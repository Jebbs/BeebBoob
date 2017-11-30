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

        //TODO: test for reading is someone is writing
        FileMode fmode = FileMode.parse(mode);
        short iNum = dir.namei(filename);
        if(iNum < 0 && fmode != FileMode.READ) {
            dir.ialloc(filename);
            iNum = dir.namei(filename);
        } else if(iNum < 0) {
            return null;
        }

        Inode node = new Inode(iNum);
        if(node.flag == 0) {
            node.flag = 1;
            node.length = 0;
        }

        node.count++;
        node.toDisk(iNum);

        FileTableEntry newEntry = new FileTableEntry(node, iNum, fmode);
        table.add(newEntry);
        return newEntry;
    }

    public synchronized boolean ffree(FileTableEntry e)
    {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table

        //assumes count has already been decreased to 0

        table.remove(e);
        e.inode.toDisk(e.iNumber);

        return true;
    }

    public synchronized boolean fempty()
    {
        return table.isEmpty();
    }
}
