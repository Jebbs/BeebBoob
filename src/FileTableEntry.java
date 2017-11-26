public class FileTableEntry     // Each table entry should have
{
    public int seekPtr;         //  a file seek pointer
    public final Inode inode;   //  a reference to its inode
    public final short iNumber; //  this inode number
    public int count;           //  # threads sharing this entry
    public final FileMode mode;   //  "r", "w", "w+", or "a"

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
