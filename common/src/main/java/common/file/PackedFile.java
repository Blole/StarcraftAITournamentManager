package common.file;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
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
		this.data = ZipTools.zip(fileData);
		this.isDir = false;
	}

	public PackedFile(File src) throws IOException
	{
		name = src.getName();
		
		if (name.endsWith(".zip") && !src.isDirectory())
		{
			data = FileUtils.readFileToByteArray(src);
			isDir = true;
		}
		else
		{
			data = ZipTools.zip(src);
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
		ZipTools.unzip(data, dest);
	}
	
	public void syncTo(File destDir) throws IOException
	{
		assert(isDir);
		File srcDir = Files.createTempDirectory("scaitm_sync").toFile();
		writeTo(srcDir);
		new MyFile(srcDir).syncToDirectory(destDir);;
	}
	
	@Override
	public String toString()
	{
		return String.format("{%s, %s}", name, FileUtils.byteCountToDisplaySize(data.length));
	}
}
