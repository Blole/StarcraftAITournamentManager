package common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class ZipTools
{
	public static byte[] zip(byte[] bytes) throws IOException
	{
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipOutputStream zout = new ZipOutputStream(out);
		zout.putNextEntry(new ZipEntry(""));
		IOUtils.copy(in, zout);
		in.close();
		zout.closeEntry();
		return out.toByteArray();
	}
	
	public static byte[] zip(File fileOrDir) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		zip(fileOrDir, out);
		return out.toByteArray();
	}
	
	public static void zip(File fileOrDir, OutputStream out) throws IOException
	{
		System.out.println("zipping: " + fileOrDir);
		
		Deque<File> queue = new LinkedList<File>();
		queue.add(fileOrDir);
		
		ZipOutputStream zout = new ZipOutputStream(out);

		while (!queue.isEmpty())
		{
			File file = queue.pop();
			String name = fileOrDir.toURI().relativize(file.toURI()).getPath();
			ZipEntry entry = new ZipEntry(name + (file.isDirectory()?"/":""));
			entry.setTime(file.lastModified());
			zout.putNextEntry(entry);
			
			if (file.isDirectory())
			{
				if (file.exists())
					queue.addAll(Arrays.asList(file.listFiles()));
			}
			else
				FileUtils.copyFile(file, zout);
			
			zout.closeEntry();
		}
	}
	
	public static void unzip(InputStream is, File destFileOrDir) throws IOException
	{
		ZipInputStream zis = new ZipInputStream(is);
		
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null)
		{
			File file = new File(destFileOrDir, entry.getName());
			
			if (entry.isDirectory())
				file.mkdirs();
			else
			{
				file.getAbsoluteFile().getParentFile().mkdirs();
				FileOutputStream out = FileUtils.openOutputStream(file);
				IOUtils.copy(zis, out);
				out.close();
				file.setLastModified(entry.getTime());
			}
		}
	}
	
	public static void unzip(byte[] zipdata, File destFileOrDir) throws IOException
	{
		unzip(new ByteArrayInputStream(zipdata), destFileOrDir);
	}
}