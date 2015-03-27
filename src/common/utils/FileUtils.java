package common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileUtils
{

	private static void DeleteRecursive(File f)
	{
		try
		{
			if (f.exists() && f.isDirectory())
			{
				for (File c : f.listFiles())
				{
					DeleteRecursive(c);
				}
			}
			
			f.delete();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void CleanDirectory(File f)
	{
		try
		{
			for (File c : f.listFiles())
			{
				DeleteRecursive(c);
			}
		}
		catch (Exception e)
		{
			
		}
	}
	
	public static void DeleteDirectory(File f)
	{
		DeleteRecursive(f);
	}
	
	public static void DeleteFile(File f)
	{
		DeleteRecursive(f);
	}
	
	public static void CopyFile(File source, File dest)
	{
		try
		{
			copy(new FileInputStream(source), new FileOutputStream(dest));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static boolean CopyDirectory(File src, File dst)
	{
		try
		{
			dst.mkdirs();
			
			Path destPath = dst.toPath();
			for (File sourceFile : src.listFiles())
			{
			    Path sourcePath = sourceFile.toPath();
			    File newFile = new File(sourcePath.toString());
			    
			    if (newFile.exists() && newFile.isDirectory())
			    {
			    	newFile.mkdirs();
			    	CopyDirectory(sourceFile, destPath.resolve(sourcePath.getFileName()).toFile());
			    }
			    else
			    {
			    	Files.copy(sourcePath, destPath.resolve(sourcePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			    }
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public static void CopyFilesInDirectory(File source, File dest)
	{
		try
		{
			copy(new FileInputStream(source), new FileOutputStream(dest));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void copy(InputStream in, OutputStream out)
	{
		try
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
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
}
