public class Directory {
    private static int maxChars = 30; // max characters of each file name

    private FileBlock files[];

    public Directory(int maxInumber) {
        files = new FileBlock[maxInumber];
        for (int i = 0; i < files.length; i++)
            files[i] = new FileBlock(maxChars);

        String root = "/"; // entry(inode) 0 is "/"
        files[0].length = root.length();
        root.getChars(0, files[0].length, files[0].filename, 0);
    }

    public int bytes2directory(byte data[]) {
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]
        return -1;
    }

    public byte[] directory2bytes() {
        // converts and return Directory information into a plain byte array
        // this byte array will be written back to disk
        // note: only meaningfull directory information should be converted
        // into bytes.
        return new byte[0];
    }

    /**
     * Allocate a new iNode for a given file.
     *
     * Returns the index of the inode allocated to the file, or -1 if there are
     * no available inodes.
     */
    public short ialloc(String filename) {
        for (int i = 0; i < files.length; i++) {
            if (files[i].length == 0) {
                files[i].length = filename.length();
                filename.getChars(0, files[i].length, files[i].filename, 0);
                return (short) i;
            }
        }

        return -1;
    }

    /**
     * Removes the associaton between an inode and a filename.
     *
     * Returns true on a succeed, and false if there is no file associated with
     * the inode.
     */
    public boolean ifree(short iNumber) {
        if (files[iNumber].length == 0)
            return false;

        files[iNumber].length = 0;
        return true;
    }

    /**
     * Get the iNumber corresponding to a file.
     *
     * Returns -1 if filename doesn't refer to a file.
     */
    public short namei(String filename) {
        for (int i = 0; i < files.length; i++) {
            if (stringCharArrCmp(filename, files[i].filename, files[i].length))
                return (short) i;
        }

        return -1;
    }

    private boolean stringCharArrCmp(String str, char[] charArr, int cLen) {
        if (str.length() != cLen)
            return false;

        for (int i = 0; i < cLen; i++) {
            if (str.charAt(i) != charArr[i])
                return false;
        }
        return true;
    }
}

class FileBlock {
    char[] filename;
    int length;

    FileBlock(int size) {
        filename = new char[size];
    }
}