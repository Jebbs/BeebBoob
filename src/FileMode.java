public enum FileMode
{
	READ,
	WRITE,
	APPEND,
	READWRITE,
	READAPPEND,
	ERROR;

	public static FileMode parse(String mode)
	{
		if("r".equals(mode))
			return READ;
		else if("w".equals(mode))
			return WRITE;
		else if("a".equals(mode))
			return APPEND;
		else if("r+".equals(mode) || "w+".equals(mode))
			return READWRITE;
		else if("a+".equals(mode))
			return READAPPEND;

		return ERROR;
	}
}
