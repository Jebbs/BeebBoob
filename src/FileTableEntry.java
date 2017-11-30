public class FileTableEntry
{
    public int seekPtr;
    public final Inode inode;
    public final short iNumber;
    public final FileMode mode;

    public FileTableEntry(Inode i, short inumber, FileMode m)
    {
        seekPtr = 0;
        inode = i;
        iNumber = inumber;
        mode = m;
        if(mode == FileMode.APPEND || mode == FileMode.READAPPEND)
            seekPtr = inode.length;
    }

    public int fetchFrame()
    {
        int x = seekPtr / inode.length;

        if(x >= 0 && x < inode.directSize)
            return inode.direct[x];

        byte indirectBlocks[] = new byte[Disk.blockSize];
        x -= inode.directSize;
        SysLib.cread(inode.indirect, indirectBlocks);
        return (x >= 0 && x < Disk.blockSize) ? indirectBlocks[x] : -1;
    }
}
