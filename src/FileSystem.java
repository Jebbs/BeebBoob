
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
                SysLib.cread(newFrame, currentBuffer);
            }

            buffer[i] = currentBuffer[indexInFrame];
        }

        return i;
    }

    int write(FileTableEntry entry, byte[] buffer)
    {
        if(entry.mode == FileMode.READ)
            return -1;

        return -1;
    }

    private boolean deallocAllBlocks(FileTableEntry entry)
    {
        return false;
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
        SysLib.rawwrite(1, inodeBlock);//1 is the first block after superblock

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
