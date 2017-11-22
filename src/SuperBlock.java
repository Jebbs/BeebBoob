public class SuperBlock {
    public int totalBlocks;
    public int totalInodes;
    public byte freeList[];

    public SuperBlock(int diskSize)
    {
        byte[] block = new byte[Disk.blockSize];
        SysLib.rawread(0, block);

        totalBlocks = SysLib.bytes2int(block, 0);
        totalInodes = SysLib.bytes2int(block, 4);

        freeList = new byte[125];
        for(int i = 0; i < 125; ++i)
            freeList[i] = block[i + 8];
    }

    public boolean getFreeList(int i)
    {
        byte x = 1 << (p & 7);
        return (freeList[i / 8] & x) == x;
    }

    public void setFreeList(int i, boolean b)
    {
        byte x = 1 << (p & 7);

        if(b)
            freeList[i / 8] |= x;
        else
            freeList[i / 8] &= ~x;
    }

    public void sync()
    {
        byte[] block = new byte[Disk.blockSize];
        SysLib.int2bytes(totalBlocks, block, 0);
        SysLib.int2bytes(totalInodes, block, 4);

        for(int i = 0; i < 125; ++i)
            block[i + 8] = freeList[i];

        SysLib.rawwrite(0, block);
    }
}
