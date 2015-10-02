package common.file;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.yaml.snakeyaml.TypeDescription;

public class MyFile extends File implements Serializable
{
	private static final long serialVersionUID = -7563594135120571872L;
	public static final TypeDescription typeDescription = new TypeDescription(MyFile.class, "!file");
	
	
	
	private final boolean endsWithSlash;

	public MyFile(String name)
	{
		this(null, name);
	}
	
	public MyFile(File file)
	{
		this(null, file.getPath());
	}
	
	public MyFile(File base, String name)
	{
		super(base, name);
		endsWithSlash = name.endsWith("/") || name.endsWith("\\");
	}
	
	/**
	 * @return true if this file exists and is a directory, or if it does not exist and it's name ends with a '/' or '\'
	 */
	@Override
	public boolean isDirectory()
	{
		if (exists())
			return super.isDirectory();
		else
			return endsWithSlash;
	}
	
	
	
	@Override
	public String getAbsolutePath()
	{
		return super.getAbsolutePath() + (isDirectory() ? File.separatorChar : "");
	}
	
	@Override
	public String getCanonicalPath() throws IOException
	{
		return super.getCanonicalPath() + (isDirectory() ? File.separatorChar : "");
	}
	
	@Override
	public String getPath()
	{
		return super.getPath() + (isDirectory() ? File.separatorChar : "");
	}
	
	@Override
	public String getName()
	{
		return super.getName() + (isDirectory() ? File.separatorChar : "");
	}
	
	@Override
	public String toString()
	{
		return super.toString();
	}
	
	public void syncToDirectory(File destDir) throws IOException
	{
		FileUtils.forceMkdir(destDir);
		assert(isDirectory());
		
		// delete the extra files
		Iterator<File> iter = FileUtils.iterateFilesAndDirs(destDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
		while (iter.hasNext())
		{
			File fileInDestDir = iter.next();
			File srcFile = this.toPath().resolve(destDir.toPath().relativize(fileInDestDir.toPath())).toFile();
			
			if (!srcFile.exists())
				FileUtils.forceDelete(fileInDestDir);
		}
		// copy in the differing ones
		copyDiffering(destDir);
	}
	
	public void copyDiffering(File destDir) throws IOException
	{
		FileUtils.forceMkdir(destDir);
		assert(isDirectory());
		
		Iterator<File> iter = FileUtils.iterateFiles(this, null, true);
		while (iter.hasNext())
		{
			File srcFile = iter.next();
			File destFile = destDir.toPath().resolve(this.toPath().relativize(srcFile.toPath())).toFile();
			
			if (!FileUtils.contentEquals(srcFile, destFile))
				FileUtils.copyFile(srcFile, destFile);
		}
	}
}
