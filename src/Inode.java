
public class Inode {
    public final static int iNodeSize = 32;
    public final static int directSize = 11;
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

        int block = (iNumber/FileSystem.inodesPerBlock) +1;
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

        int block = (iNumber/FileSystem.inodesPerBlock) +1;
        int offset = iNumber%FileSystem.inodesPerBlock;

        SysLib.cread(block, buffer);
        saveToBytes(buffer, offset);
        SysLib.cwrite(block, buffer);

        return 0; //what is this even supposed to return?
    }

    public void loadFromBytes(byte[] buffer, int offset)
    {
        length = SysLib.bytes2int(buffer, offset);
        count = SysLib.bytes2short(buffer, offset+4);
        flag = SysLib.bytes2short(buffer, offset+6);

        for(int i = 0; i < directSize; i++)
        {
            direct[i] = SysLib.bytes2short(buffer, offset+8+(i*2));
        }

        indirect = SysLib.bytes2short(buffer, offset+8+22);
    }

    public void saveToBytes(byte[] buffer, int offset)
    {
        SysLib.int2bytes(length, buffer, offset);
        SysLib.short2bytes(count, buffer, offset+4);
        SysLib.short2bytes(flag, buffer, offset+6);

        for(int i = 0; i < directSize; i++)
        {
            SysLib.short2bytes(direct[i], buffer, offset+8+(i*2));
        }

        SysLib.short2bytes(indirect, buffer, offset+8+22);
    }

}
