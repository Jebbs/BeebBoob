
public class SuperBlock {
    public int totalBlocks;
    public int totalInodes;
    public int freeList;

    public SuperBlock(int diskSize)
    {
        //read the first block

        byte[] block = new byte[Disk.blockSize];

        SysLib.rawread(0, block);

        //get the information
        totalBlocks = SysLib.bytes2int(block, 0);
        totalBlocks = SysLib.bytes2int(block, 4);
        totalBlocks = SysLib.bytes2int(block, 8);
    }
}
