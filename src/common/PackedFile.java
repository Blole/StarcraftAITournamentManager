package common;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import common.utils.ZipTools;

public class PackedFile implements Serializable
{
	private static final long serialVersionUID = -5749964971289520236L;
	
	private static HashMap<File, PackedFile> cache = new HashMap<>();
	private final byte[] data;
	private final String fileName;
	private final boolean isDir;
	
	private PackedFile(File src) throws IOException
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
	
	static public PackedFile get(File src) throws IOException
	{
		PackedFile packedFile = cache.get(src);
		if (packedFile == null)
		{
			packedFile = new PackedFile(src);
			cache.put(src, packedFile);
		}
		return packedFile;
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
