public class FileSystem{
    private static final int inodesPerBlock = 16;

    private SuperBlock superBlock;
    private Directory directory;
    private FileTable fileTable;
    private Scheduler scheduler;//this is to have access to the TCB's

    public FileSystem(int diskBlocks, Scheduler sched)
    {
        superBlock = new SuperBlock(diskBlocks);

        if(SuperBlock.totalInodes  == 0)
            format(64);

        directory = new Directory(superBlock.totalInodes);

        fileTable = new FileTable(directory);

        scheduler = sched;

        //directory reconstruction?
        FileTableEntry dirEntry = open("/","r");
        int dirSize = fsize(dirEntry);
    }


    /**
     * Write all blocks back to the disk?
     */
    void sync(){

    }

    /**
     * format the disk such that there are n files allowed in the system.
     */
    boolean format(int files){
        //clear free spots in super block
        superBlock.totalInodes = files;

        //get an array of bytes for a block of blank inodes

        //rewrite the inode blocks

        return true;
    }

    /**
     * Open a file.
     */
    FileTableEntry open(String filename, String mode){
        short thing = -1;
        return new FileTableEntry(new Inode(), thing, "");
    }

    boolean close(FileTableEntry entry){
        return false;
    }

    int fsize(FileTableEntry entry){
        return -1;
    }

    int read(FileTableEntry entry, byte[] buffer){
        return -1;
    }

    int write(FileTableEntry entry, byte[] buffer){
        return -1;
    }

    private boolean deallocAllBlocks(FileTableEntry entry){
        return false;
    }

    boolean delete(String filename){
        return false;
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 0;
    private final int SEEK_END = 0;

    int seek(FileTableEntry entry, int offset, int whence){
        return -1;
    }
}