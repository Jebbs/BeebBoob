public class FileTableEntry
{
    public int seekPtr;
    public final Inode inode;
    public final short iNumber;
    public int count;
    public final FileMode mode;

    public FileTableEntry(Inode i, short inumber, FileMode m)
    {
        seekPtr = 0;
        inode = i;
        iNumber = inumber;
        count = 1;
        mode = m;
        if(mode == FileMode.APPEND || mode == FileMode.READAPPEND)
            seekPtr = inode.length;
    }
}
