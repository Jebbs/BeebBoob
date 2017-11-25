
public class Inode {
    public final static int iNodeSize = 32;
    private final static int directSize = 11;

    public int length;
    public short count;
    public short flag;
    public short[] direct = new short[directSize];
    public short indirect;

    public Inode(){
        length = 0;
        count = 0;
        flag = 0;//think about this
        for(int i = 0; i < directSize; i++)
            direct[i] = -1;
        indirect = -1;
    }

    public Inode(short iNumber){

    }

    public int toDisk(short iNumber){
        return 0;
    }

    public static void inodeToBytes(Inode node, byte[] b, int offset)
    {
        SysLib.int2bytes(node.length, b, offset);

        for(int i = 0; i < directSize; i++)
        {
            SysLib.short2bytes(node.direct[i], b, offset+4+(i*2));
        }

        SysLib.short2bytes(node.indirect, b, offset+4+22);
    }

}
