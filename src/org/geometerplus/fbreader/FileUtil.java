package org.geometerplus.fbreader;

public class FileUtil {
	static public String getExtension(String filePath)
	{
		final int index = filePath.lastIndexOf('.');
		return (index > 0) ? filePath.substring(index + 1).toLowerCase().intern() : "";
	}
	
	static public String getFileName(String filePath)
	{
		final int index = filePath.lastIndexOf('.');
		if (index > 0) {
			return filePath.substring(filePath.lastIndexOf('/') + 1, index);
		} else {
			return filePath.substring(filePath.lastIndexOf('/') + 1);
		}
	}
	
	static public String getFullFileName(String filePath)
	{
		return filePath.substring(filePath.lastIndexOf('/') + 1);
	}
}
