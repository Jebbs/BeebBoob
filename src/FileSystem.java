
import java.nio.charset.*;

public class FileSystem
{
    public static int inodesPerBlock;

    private SuperBlock superBlock;
    private Directory directory;
    private FileTable fileTable;

    public FileSystem(int diskBlocks)
    {
        inodesPerBlock = Disk.blockSize / Inode.iNodeSize;
        superBlock = new SuperBlock(diskBlocks);

        if(superBlock.totalInodes == 0)
            diskFormat(72);

        directory = new Directory(superBlock.totalInodes);

        fileTable = new FileTable(directory);

        //directory reconstruction?
        FileTableEntry dirEntry = open("/", "r");
        int dirSize = fsize(dirEntry);
        if(dirSize>0)
        {
            byte[] dirData = new byte[dirSize];
            read(dirEntry, dirData);
            directory.bytes2directory(dirData);
        }
        close(dirEntry);
    }


    /**
     * Write all blocks back to the disk?
     */
    void sync()
    {

    }

    /**
     * format the disk such that there are n files allowed in the system.
     */
    boolean format(int files)
    {
        if(!diskFormat(files))
            return false;

        //do some other stuff here later
        return true;
    }

    FileTableEntry open(String filename, String mode)
    {
        return fileTable.falloc(filename, mode);
    }

    boolean close(FileTableEntry entry)
    {
        if(--entry.inode.count == 0)
            return fileTable.ffree(entry);

        return true;
    }

    int fsize(FileTableEntry entry)
    {
        return entry.inode.length;
    }

    int read(FileTableEntry entry, byte[] buffer)
    {
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
        if(entry.mode == FileMode.READ)
            return -1;

        if((entry.seekPtr + buffer.length) / Disk.blockSize > 265)
            return -1;

        if(superBlock.areXFreeBlocksUnavailable((entry.seekPtr + buffer.length)
            / Disk.blockSize - entry.inode.length / Disk.blockSize))
            return -1;

    block_allocation:
        for(int i = (entry.seekPtr + buffer.length) / Disk.blockSize;
            i > entry.inode.length / Disk.blockSize; --i) {

            for(int j = 0; j <= Inode.directSize; ++j) {
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
                SysLib.cwrite(currentFrame, currentBuffer);
                currentFrame = newFrame;
                SysLib.cread(currentFrame, currentBuffer);
            }

            currentBuffer[indexInFrame] = buffer[i];
        }

        SysLib.cwrite(currentFrame, currentBuffer);
        return buffer.length;
    }

    private void deallocAllBlocks(FileTableEntry entry)
    {
        for(int i = 0; i < Inode.directSize; ++i)
            if(entry.inode.direct[i] > -1)
                superBlock.setFreeList(entry.inode.direct[i], false);

        if(entry.inode.indirect > -1) {
            byte indirect_content[] = new byte[Disk.blockSize];
            SysLib.cread(entry.inode.indirect, indirect_content);

            for(int i = 0; i < Disk.blockSize; i += 2) {
                short j = SysLib.bytes2short(indirect_content, i);

                if(j > -1)
                    superBlock.setFreeList(j, false);
            }

            superBlock.setFreeList(entry.inode.indirect, false);
        }
    }

    boolean delete(String filename)
    {
        return false;
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

        return 0;
    }

    private boolean diskFormat(int files)
    {
        superBlock.clearFreeList();
        superBlock.totalInodes = files;

        //get an array of bytes for a block of blank inodes
        byte[] inodeBlock = new byte[Disk.blockSize];
        Inode blankInode = new Inode();
        for(int i = 0; i < inodesPerBlock; i++)
            blankInode.saveToBytes(inodeBlock, i * Inode.iNodeSize);

        //rewrite the inode blocks
        for(int i = files, j = 0; i > 0; i -= inodesPerBlock, j = 0) {
            //get next free block (though it will be early)
            short block = superBlock.findFirstFreeBlock();
            superBlock.setFreeList(block, true);
            SysLib.rawwrite(block, inodeBlock);
        }

        //set up first inode to refer to a directory
        Inode dirNode = new Inode();
        dirNode.length = 32;
        dirNode.direct[0] = superBlock.findFirstFreeBlock();
        dirNode.saveToBytes(inodeBlock, 0);
        SysLib.rawwrite(1, inodeBlock);//1 is the first block after superBlock

        //write the directory block to disk
        byte[] directoryBlock = new byte[Disk.blockSize];
        char[] name = new char[30];
        name[0] = '/';
        name[1] = '\0';
        SysLib.short2bytes((short)0, directoryBlock, 0);//inode 0 refers to root
        byte[] nameInBytes = toBytes(name);

        for(int i = 0, j = 2; i < name.length; i++, j++)
            directoryBlock[j] = nameInBytes[i];

        SysLib.rawwrite(dirNode.direct[0], directoryBlock);
        superBlock.sync();
        return true;
    }

    //taken from stack overflow
    // https://stackoverflow.com/questions/5513144/converting-char-to-byte
    private byte[] toBytes(char[] chars)
    {
        return new String(chars).getBytes(Charset.forName("UTF-8"));
    }
}
