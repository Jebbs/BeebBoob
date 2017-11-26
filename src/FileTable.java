import java.util.Vector;

public class FileTable
{
    private Vector<FileTableEntry> table;
    private Directory dir;

    public FileTable(Directory directory)
    {
        table = new Vector<FileTableEntry>();
        dir = directory;
    }

    public synchronized FileTableEntry falloc(String filename, String mode)
    {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry
        short thing = -1;
        return new FileTableEntry(new Inode(), thing, FileMode.parse(mode));
    }

    public synchronized boolean ffree(FileTableEntry e)
    {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table
        return false;
    }

    public synchronized boolean fempty()
    {
        return table.isEmpty();
    }
}
