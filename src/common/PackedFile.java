package common;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import common.utils.ZipTools;

public class PackedFile implements Serializable
{
	private static final long serialVersionUID = -5749964971289520236L;
	
	private static HashMap<File, PackedFile> cache = new HashMap<>();
	private final byte[] data;
	public final String name;
	private final boolean isDir;
	
	public PackedFile(String displayName, byte[] fileData) throws IOException
	{
		this.name = displayName;
		this.data = ZipTools.Zip(fileData);
		this.isDir = false;
	}

	private PackedFile(File src) throws IOException
	{
		name = src.getName();
		
		if (name.endsWith(".zip") && !src.isDirectory())
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
			dest = new File(dest, name);
		ZipTools.UnzipByteArrayToDir(data, dest);
	}
	
	@Override
	public String toString()
	{
		
		return String.format("{%s, %s}", name, FileUtils.byteCountToDisplaySize(data.length));
	}
}
