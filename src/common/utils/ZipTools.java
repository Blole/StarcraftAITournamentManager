package common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Deque;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipTools
{
	public static void main(String[] args) throws Exception
	{
		//byte[] b = ZipToByteArray(new File(args[0]));
		//UnzipByteArrayToDir(b, new File(args[0] + "_test"));
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		System.out.println("Loading Zip File: " + args[0]);
		LoadZipFileToStream(new File(args[0]), bos);
		
		System.out.println("File loaded " + bos.size() + ", Unzipping File");
		UnzipStreamToDir(new ByteArrayInputStream(bos.toByteArray()), new File("test"));
	}
	
	// Loads a zip file into an output stream
	public static void LoadZipFileToStream(File file, OutputStream out) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		
		copy(fis, out);
		
		fis.close();
		out.close();
	}
	
	// Loads a zip file into an output stream
	public static byte[] LoadZipFileToByteArray(String filename) throws IOException
	{
		return LoadZipFileToByteArray(new File(filename));
	}
	
	// Loads a zip file into an output stream
	public static byte[] LoadZipFileToByteArray(File file) throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		LoadZipFileToStream(file, bos);
		return bos.toByteArray();
	}
	
	// Zips a file to an output stream
	// If the file is a directory, all files in the directory are zipped
	public static void ZipDirToStream(File file, OutputStream out) throws IOException
	{
		System.out.println("Zipping Dir: " + file.getPath());
			
		URI base = file.toURI();
		Deque<File> queue = new LinkedList<File>();
		queue.add(file);
		Closeable res = out;
		
		try
		{
			ZipOutputStream zout = new ZipOutputStream(out);
			res = zout;

			// if it's a directory
			while (!queue.isEmpty())
			{
				file = queue.pop();
				
				// if the file is a directory
				if (file.isDirectory())
				{
					for (File kid : file.listFiles())
					{
						String name = base.relativize(kid.toURI()).getPath();
						if (kid.isDirectory())
						{
							queue.push(kid);
							name = name.endsWith("/") ? name : name + "/";
							zout.putNextEntry(new ZipEntry(name));
						}
						else
						{
							zout.putNextEntry(new ZipEntry(name));
							FileInputStream fis = new FileInputStream(kid);
							copy(fis, zout);
							fis.close();
							zout.closeEntry();
							System.out.println("Zipped: " + name);
						}
					}
				}
				else
				{
					String name = base.relativize(file.toURI()).getPath();
					zout.putNextEntry(new ZipEntry(name));
					FileInputStream fis = new FileInputStream(file);
					copy(fis, zout);
					fis.close();
					zout.closeEntry();
				}
			}
		}
		finally
		{
			res.close();
		}
	}
	
	// Zips a file to a given filename
	// If the file is a directory, all files in the directory are zipped
	public static void ZipDirToFile(File file, File zipfile) throws IOException
	{
		ZipDirToStream(file, new FileOutputStream(zipfile));
	}
	
	// Zips a file to a byte[]
	// If the file is a directory, all files in the directory are zipped
	public static byte[] ZipDirToByteArray(String filename) throws IOException
	{
		return ZipDirToByteArray(new File(filename));
	}
	
	// Zips a file to a byte[]
	// If the file is a directory, all files in the directory are zipped
	public static byte[] ZipDirToByteArray(File file) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipDirToStream(file, out);
		return out.toByteArray();
	}
	
	// Unzip all files from a given stream to the given directory
	public static void UnzipStreamToDir(InputStream is, File directory) throws IOException
	{
		ZipInputStream zis = new ZipInputStream(is);
		
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null)
		{
			File file = new File(directory, entry.getName());
			//System.err.printf("entry %s (%s), file: %s\n", entry.getName(), entry.isDirectory()?"dir":"file", file.toString());
			if (entry.isDirectory())
			{
				file.mkdirs();
			}
			else
			{
				file.getParentFile().mkdirs();
				FileOutputStream fos = new FileOutputStream(file);
				copy(zis, fos);
				fos.close();
			}
		}
		
		zis.close();
	}
	
	public static void UnzipByteArrayToDir(byte[] zipdata, String directory) throws IOException
	{
		UnzipStreamToDir(new ByteArrayInputStream(zipdata), new File(directory));
	}
	
	public static void UnzipFileToDir(File zipfile, File directory) throws IOException
	{
		UnzipStreamToDir(new FileInputStream(zipfile), directory);
	}

	private static void copy(InputStream in, OutputStream out) throws IOException
	{
		byte[] buffer = new byte[1024];
		while (true)
		{
			int readCount = in.read(buffer);
			if (readCount < 0)
			{
				break;
			}
			out.write(buffer, 0, readCount);
		}
	}
}