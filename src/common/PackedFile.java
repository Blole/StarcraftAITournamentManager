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
	
	public PackedFile(File src) throws IOException
	{
		fileName = src.getName();
		
		if (!src.isDirectory() && src.getName().endsWith(".zip"))
			data = ZipTools.LoadZipFileToByteArray(src);
		else
			data = ZipTools.ZipDirToByteArray(src);
	}
	
	public void writeTo(File dest) throws IOException
	{
		ZipTools.UnzipByteArrayToDir(data, dest);
	}
	
	@Override
	public String toString()
	{
		return String.format("[%s, %.0f kB]", fileName, data.length/1024.0);
	}
}
