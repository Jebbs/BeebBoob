public class FileSystem{
    private SuperBlock superBlock;
    private Directory directory;
    private FileTable fileTable;

    public FileSystem(int diskBlocks)
    {
        superBlock = new SuperBlock(diskBlocks);

        //detect if we have an unformated disk?

        directory = new Directory(superBlock.totalInodes);

        fileTable = new FileTable(directory);

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
        return false;
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