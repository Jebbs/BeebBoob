
public class Inode {
    public final static int iNodeSize = 32;
    private final static int directSize = 11;
    private static byte[] buffer;

    public int length;
    public short count;
    public short flag;
    public short[] direct = new short[directSize];
    public short indirect;

    public Inode(){
        length = -1;
        count = 0;
        flag = 0;//think about this
        for(int i = 0; i < directSize; i++)
            direct[i] = -1;
        indirect = -1;
    }

    /**
     * Construct an Inode from a given index.
     */
    public Inode(short iNumber){

        //make sure we have our static buffer
        if(buffer == null)
            buffer = new byte[Disk.blockSize];

        int block = iNumber/FileSystem.inodesPerBlock;
        int offset = iNumber%FileSystem.inodesPerBlock;

        SysLib.cread(block, buffer);
        loadFromBytes(buffer, offset);
    }

    /**
     * Write an Inode to a given index.
     */
    public int toDisk(short iNumber){

        //make sure we have our static buffer
        if(buffer == null)
            buffer = new byte[Disk.blockSize];

        int block = iNumber/FileSystem.inodesPerBlock;
        int offset = iNumber%FileSystem.inodesPerBlock;

        SysLib.cread(block, buffer);
        saveToBytes(buffer, offset);
        SysLib.cwrite(block, buffer);

        return 0; //what is this even supposed to return?
    }

    /**
     * This is an old function. It's going to be replaced by saveToBytes
     */
    public static void inodeToBytes(Inode node, byte[] b, int offset)
    {
        SysLib.int2bytes(node.length, b, offset);

        for(int i = 0; i < directSize; i++)
        {
            SysLib.short2bytes(node.direct[i], b, offset+4+(i*2));
        }

        SysLib.short2bytes(node.indirect, b, offset+4+22);
    }

    public void loadFromBytes(byte[] buffer, int offset)
    {
        length = SysLib.bytes2int(buffer, offset);

        for(int i = 0; i < directSize; i++)
        {
            direct[i] = SysLib.bytes2short(buffer, offset+4+(i*2));
        }

        indirect = SysLib.bytes2short(buffer, offset+4+22);
    }

    public void saveToBytes(byte[] buffer, int offset)
    {
        SysLib.int2bytes(length, buffer, offset);

        for(int i = 0; i < directSize; i++)
        {
            SysLib.short2bytes(direct[i], buffer, offset+4+(i*2));
        }

        SysLib.short2bytes(indirect, buffer, offset+4+22);
    }

}
