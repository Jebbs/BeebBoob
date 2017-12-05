
import java.nio.charset.*;

public class FileSystem
{
    public static int inodesPerBlock;

    private SuperBlock superBlock;
    private Directory directory;
    private FileTable fileTable;
    public boolean skipSync;

    public FileSystem(int diskBlocks)
    {
        inodesPerBlock = Disk.blockSize / Inode.iNodeSize;
        superBlock = new SuperBlock(diskBlocks);

        if(superBlock.totalInodes == 0)
            diskFormat(64);

        directory = new Directory(superBlock.totalInodes);
        fileTable = new FileTable(directory, superBlock.totalInodes);

        //directory reconstruction?
        FileTableEntry dirEntry = open("/", "r");
        int dirSize = fsize(dirEntry);

        if(dirSize>0) {
            byte[] dirData = new byte[dirSize];
            read(dirEntry, dirData);
            directory.bytes2directory(dirData);
        }

        close(dirEntry);

        skipSync = false;
    }

    /**
     * Write all blocks back to the disk
     */
    void sync()
    {
        //prevents an infinite recursive loop caused by csync and flush
        if(skipSync)
            return;
        skipSync = true;

        //we don't need to handle most information here due to the use of the
        //disk cache.
        byte[] dir = directory.directory2bytes();
        FileTableEntry dirEntry = open("/", "w");
        write(dirEntry, dir);
        close(dirEntry);

        //finish with making sure the disk cache is written back
        SysLib.csync();
    }

    /**
     * format the disk such that there are n files allowed in the system.
     */
    boolean format(int files)
    {
        boolean ret;
        synchronized(fileTable)
        {
            skipSync = true;

            //if there are no open files, then we can do stuff
            if(ret = fileTable.fempty()) {
                SysLib.flush();
                diskFormat(files);
            }

            skipSync = false;
        }

        return ret;
    }

    FileTableEntry open(String filename, String mode)
    {
        short i = directory.namei(filename);
        FileTableEntry r = fileTable.falloc(filename, mode);

        if(i == -1 && r != null) {
            short n = superBlock.findFirstFreeBlock();
            r.inode.direct[0] = n;
            superBlock.setFreeList(n, true);
        }

        return r;
    }

    boolean close(FileTableEntry entry)
    {
        if(entry == null)
            return false;

        synchronized(fileTable){
            if(--entry.inode.count == 0 &&
               entry.inode.flag == Inode.DELETE) {
                //dealoc all blocks associated for file if this is the last one
                deallocAllBlocks(entry.inode, entry.iNumber);
            }
        }

        return fileTable.ffree(entry);
    }

    int fsize(FileTableEntry entry)
    {
        return (entry == null)?-1:entry.inode.length;
    }

    int read(FileTableEntry entry, byte[] buffer)
    {

        if(entry == null)
            return -1;

        if(entry.mode == FileMode.WRITE || entry.mode == FileMode.APPEND)
            return -1;

        int i = 0;
        int currentFrame = -1;
        byte currentBuffer[] = new byte[Disk.blockSize];

        for(; i < buffer.length && entry.seekPtr < entry.inode.length; ++i,
            ++entry.seekPtr) {
            int indexInFrame = entry.seekPtr % Disk.blockSize;
            int newFrame = entry.fetchFrame();

            if(newFrame != currentFrame) {
                currentFrame = newFrame;
                SysLib.cread(currentFrame, currentBuffer);
            }

            buffer[i] = currentBuffer[indexInFrame];
        }

        return i;
    }

    int write(FileTableEntry entry, byte[] buffer)
    {
        if(entry == null)
            return -1;

        if(entry.mode == FileMode.READ)
            return -1;

        if((entry.seekPtr + buffer.length) / Disk.blockSize > 265)
            return -1;

        int requiredBlocks = (entry.seekPtr + buffer.length) / Disk.blockSize
            - entry.inode.length / Disk.blockSize;

        if(superBlock.areXFreeBlocksUnavailable(requiredBlocks))
            return -1;

    block_allocation:
        for(int i = 0; i < requiredBlocks; i++) {
            for(int j = 0; j < Inode.directSize; ++j) {
                if(entry.inode.direct[j] == -1) {
                    short next = superBlock.findFirstFreeBlock();
                    entry.inode.direct[j] = next;
                    superBlock.setFreeList(next, true);
                    continue block_allocation;
                }
            }

            byte indirect_content[] = new byte[Disk.blockSize];

            if(entry.inode.indirect == -1) {
                short indie = superBlock.findFirstFreeBlock();
                entry.inode.indirect = indie;
                superBlock.setFreeList(indie, true);

                for(int j = 0; j < indirect_content.length; ++j)
                    indirect_content[j] = (byte)(-1);

                SysLib.cwrite(indie, indirect_content);
            }

            SysLib.cread(entry.inode.indirect, indirect_content);

            for(int j = 0; j < indirect_content.length; j += 2) {
                if(SysLib.bytes2short(indirect_content, j) < 0) {
                    short next = superBlock.findFirstFreeBlock();
                    SysLib.short2bytes(next, indirect_content, j);
                    superBlock.setFreeList(next, true);
                    break;
                }
            }

            SysLib.cwrite(entry.inode.indirect, indirect_content);
        }

        if(entry.seekPtr + buffer.length > entry.inode.length)
            entry.inode.length = entry.seekPtr + buffer.length;

        int currentFrame = -1;
        byte currentBuffer[] = new byte[Disk.blockSize];

        for(int i = 0; i < buffer.length; ++i, ++entry.seekPtr) {
            int indexInFrame = entry.seekPtr % Disk.blockSize;
            int newFrame = entry.fetchFrame();

            if(newFrame != currentFrame) {
                if(currentFrame != -1)
                    SysLib.cwrite(currentFrame, currentBuffer);

                currentFrame = newFrame;
                SysLib.cread(currentFrame, currentBuffer);
            }

            currentBuffer[indexInFrame] = buffer[i];
        }

        SysLib.cwrite(currentFrame, currentBuffer);
        return buffer.length;
    }

    private void deallocAllBlocks(Inode entry, short inum)
    {
        for(int i = 0; i < Inode.directSize; ++i)
            if(entry.direct[i] > -1)
                superBlock.setFreeList(entry.direct[i], false);

        if(entry.indirect > -1) {
            byte indirect_content[] = new byte[Disk.blockSize];
            SysLib.cread(entry.indirect, indirect_content);

            for(int i = 0; i < Disk.blockSize; i += 2) {
                short j = SysLib.bytes2short(indirect_content, i);

                if(j > -1)
                    superBlock.setFreeList(j, false);
            }

            superBlock.setFreeList(entry.indirect, false);
        }

        entry.clear();
        entry.toDisk(inum);
    }

    /**
     * returns false if the file does not exist
     */
    boolean delete(String filename)
    {
        short i = directory.namei(filename);
        if(i == -1)
            return false;

        Inode node = new Inode(i);
        //if markforDeletion returns true, we are ok to delete right away
        if(fileTable.markForDeletion(i)) {
            synchronized(fileTable){
            directory.ifree(i);
            deallocAllBlocks(node, i);
            }
        }
        return true;
    }

    public final int SEEK_SET = 0;
    public final int SEEK_CUR = 1;
    public final int SEEK_END = 2;

    int seek(FileTableEntry entry, int offset, int whence)
    {
        if(whence == SEEK_SET)
            entry.seekPtr = offset;
        else if(whence == SEEK_CUR)
            entry.seekPtr += offset;
        else if(whence == SEEK_END)
            entry.seekPtr = entry.inode.length + offset;

        if(entry.seekPtr < 0)
            entry.seekPtr = 0;
        else if(entry.seekPtr > entry.inode.length)
            entry.seekPtr = entry.inode.length;

        //I think this is actually supposed to return 0 or -1
        return entry.seekPtr;
    }

    private boolean diskFormat(int files)
    {
        superBlock.clearFreeList();
        superBlock.totalInodes = files;

        Inode blankInode = new Inode();

        //reserve blocks
        int neededBlocks = superBlock.totalInodes/inodesPerBlock;
        for(int i = 0; i < neededBlocks; i++) {
            short block = superBlock.findFirstFreeBlock();
            superBlock.setFreeList(block, true);
        }

        //rewrite the inode blocks
        for(short i = 0; i < files; i++)
            blankInode.toDisk(i);

        //set up first inode to refer to a directory
        Inode dirNode = new Inode();
        dirNode.length = 32;
        dirNode.direct[0] = superBlock.findFirstFreeBlock();
        superBlock.setFreeList(dirNode.direct[0], true);
        dirNode.toDisk((short)0);//0 is the directory inode

        //write the directory block to disk
        byte[] directoryBlock = new byte[Disk.blockSize];
        char[] name = new char[30];
        name[0] = '/';
        name[1] = '\0';
        SysLib.short2bytes((short)0, directoryBlock, 0);//inode 0 refers to root
        byte[] nameInBytes = toBytes(name);

        for(int i = 0, j = 2; i < name.length; i++, j++)
            directoryBlock[j] = nameInBytes[i];

        SysLib.cwrite(dirNode.direct[0], directoryBlock);
        superBlock.sync();
        SysLib.csync();
        return true;
    }

    //taken from stack overflow
    // https://stackoverflow.com/questions/5513144/converting-char-to-byte
    private byte[] toBytes(char[] chars)
    {
        return new String(chars).getBytes(Charset.forName("UTF-8"));
    }
}
