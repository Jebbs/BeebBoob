import java.util.Vector;

public class FileTable
{
    private Vector<FileTableEntry> table;
    private Inode[] Inodes;
    private Directory dir;

    public FileTable(Directory directory, int fileCount)
    {
        table = new Vector<FileTableEntry>();
        dir = directory;
        Inodes = new Inode[fileCount];
    }

    public synchronized FileTableEntry falloc(String filename, String mode)
    {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry

        FileMode fmode = FileMode.parse(mode);
        short iNum = dir.namei(filename);
        if(iNum < 0 && fmode != FileMode.READ) {
            dir.ialloc(filename);
            iNum = dir.namei(filename);
        } else if(iNum < 0) {
            return null;
        }

        if(Inodes[iNum] == null)
        {
            Inodes[iNum] = new Inode(iNum);

            //this is a new file
            if(Inodes[iNum].flag == Inode.UNUSED_INODE)
            {
                Inodes[iNum].flag = Inode.UNOPEN_INODE;
                Inodes[iNum].length = 0;
            }
        }

        Inode node = Inodes[iNum];

        if(node.flag == Inode.UNOPEN_INODE) {

            //opening a previously unopen file
            if(fmode == FileMode.READ)
                node.flag = Inode.OPEN_INODE_R;
            else if(fmode == FileMode.WRITE)
                node.flag = Inode.OPEN_INODE_W;
            else if(fmode == FileMode.READWRITE)
                node.flag = Inode.OPEN_INODE_RW;
        }
        else if(node.flag == Inode.OPEN_INODE_R && fmode != FileMode.READ) {
            //can't open a file to read when it is open for writing
            return null;
        }
        else{
            //can't open a file for writing/appending when open for anything else
            return null;
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

        if(e == null)
            return false;

        if(--e.inode.count == 0) {
            e.inode.flag = Inode.UNOPEN_INODE;
            e.inode.toDisk(e.iNumber);
            Inodes[e.iNumber] = null;
        }

        //remove this FTE since they are all unique
        table.remove(e);

        return true;
    }

    public synchronized boolean fempty()
    {
        return table.isEmpty();
    }
}
