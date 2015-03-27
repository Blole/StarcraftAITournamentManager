package common;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import common.utils.ZipTools;

@SuppressWarnings("serial")
public class PackedFile implements Serializable
{
	private final byte[] data;
	public final String fileName;
	private final boolean isDir;
	
	public PackedFile(File src) throws IOException
	{
		fileName = src.getName();
		
		if (fileName.endsWith(".zip") && !src.isDirectory())
		{
			data = ZipTools.LoadZipFileToByteArray(src);
			isDir = true;
		}
		else
		{
			data = ZipTools.ZipDirToByteArray(src);
			isDir = src.isDirectory();
		}
	}
	
	public void writeTo(File dest) throws IOException
	{
		if (dest.isDirectory() && !isDir)
			dest = new File(dest, fileName);
		ZipTools.UnzipByteArrayToDir(data, dest);
	}
	
	@Override
	public String toString()
	{
		return String.format("[%s, %.0f kB]", fileName, data.length/1024.0);
	}
}
