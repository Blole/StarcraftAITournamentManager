package objects;

import java.io.File;
import java.io.IOException;

import utility.ZipTools;

public class FileMessage implements Message
{
	private static final long serialVersionUID = -8193083217816970522L;
	
	public final byte[] data;
	public final String dest;
	
	public FileMessage(byte[] data, String dest)
	{
		this.data = data;
		this.dest = dest;
	}
	
	public FileMessage(File file, String dest)
	{
		this(read(file), dest);
	}
	
	public void write(Environment env)
	{
		try
		{
			System.err.printf("unzipping to %s (%s)\n", dest, env.lookup(dest));
			ZipTools.UnzipByteArrayToDir(data, env.lookup(dest));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private static byte[] read(File file)
	{
		try
		{
			if (file.isDirectory())
				return ZipTools.ZipDirToByteArray(file);
			else
				return ZipTools.LoadZipFileToByteArray(file);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}
	
	@Override
	public String toString()
	{
		return String.format("[%s, %.0f kB]", dest, data.length/1024.0);
	}
}