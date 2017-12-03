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
            if(Inodes[iNum].flag == Inode.UNUSED)
            {
                Inodes[iNum].flag = Inode.UNOPEN;
                Inodes[iNum].length = 0;
            }
        }

        Inode node = Inodes[iNum];

        if(node.flag == Inode.UNOPEN) {

            //opening a previously unopen file
            if(fmode == FileMode.READ)
                node.flag = Inode.OPEN_R;
            else if(fmode == FileMode.WRITE)
                node.flag = Inode.OPEN_W;
            else if(fmode == FileMode.READWRITE)
                node.flag = Inode.OPEN_RW;
            else if(fmode == FileMode.APPEND)
                node.flag = Inode.OPEN_A;

        }
        else if(node.flag == Inode.OPEN_R && fmode != FileMode.READ) {
            //can't open a file to read when it is open for writing
            return null;
        }
        else{//wrong
            //can't open a file for writing/appending when open for anything else
            //or this file is marked for deletion
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

        if(e.inode.count == 0) {
            if(e.inode.flag == Inode.DELETE) {
                dir.ifree(e.iNumber);
                e.inode.flag = Inode.UNUSED;
            }
            else
                e.inode.flag = Inode.UNOPEN;

            e.inode.toDisk(e.iNumber);
            Inodes[e.iNumber] = null;
        }
        //remove this FTE since they are all unique
        table.remove(e);

        return true;
    }

    /**
     * Synchronized way of reducing the inode's use count by one.
     *
     * This method returns the current inode count after the reduction.
     */
    public synchronized int reduceInodeCount(FileTableEntry e) {
        return --e.inode.count;

    }

    public synchronized boolean isFileOpen(short iNum) {
        return (Inodes[iNum] == null)?false:true;
    }

    /**
     * This method goes through the process of marking an inode/file for
     * deletion.
     *
     * This method will return true if the file is in a state acceptible for
     * immediate deletion, otherwise it returns false but marks the file to be
     * deleted when all references close.
     *
     * This method assumes that a file mapped to this inode exists in the
     * directory.
     */
    public synchronized boolean markForDeletion(short iNum) {

        //if the file is currently not open, we are safe to delete it now
        boolean okForDeletion = (Inodes[iNum] == null)?true:false;

        Inode node = (Inodes[iNum] == null)? new Inode(iNum):Inodes[iNum];

        node.flag = Inode.DELETE;

        return okForDeletion;

    }


    public synchronized boolean fempty()
    {
        return table.isEmpty();
    }
}
