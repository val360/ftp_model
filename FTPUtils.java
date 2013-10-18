package com.prosc.ftpeek;

import com.prosc.io.IOUtils;
import com.prosc.fmkit.PluginUtils;
import com.enterprisedt.net.ftp.FTPException;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class FTPUtils {
	public static void writeFile(InputStream io, String fileName) throws IOException {
		PluginUtils.checkWrite(new File(fileName));		
		IOUtils.writeInputToOutput(io, new FileOutputStream(fileName), 8192);
	}
	
	public static boolean existsDir(FTPInterface model, String remoteDir) throws IOException, FTPException {
		boolean result = false;
		String cwp = model.getCurrentDir();
		try {
			model.changeDir(remoteDir); //try cd into dir
			result = true;
			model.changeDir(cwp); //go back where you came from!					
		} catch (FTPException e) {
			//Ignore - dir does not exist
		}
		return result;
	}


	public static File[] scanDirectoryFiles( File directory, boolean recursive ) throws FileNotFoundException {
		if (!directory.exists()) throw new FileNotFoundException("Directory does not exist: " + directory.getAbsolutePath() );
		if (!directory.isDirectory()) throw new FileNotFoundException("This is a file, not a directory: " + directory.getAbsolutePath() );
		List<File> result = new LinkedList<File>();
		scanDirectoryFiles( directory, recursive, result );
		return result.toArray( new File[result.size()] );
	}

	private static void scanDirectoryFiles(File file, final boolean recursive, final List<File> result ) {
		FileFilter filter = new FileFilter() {
			public boolean accept(File pathname) {
				if( !pathname.isHidden() ) {
					result.add( pathname );
					if( recursive && pathname.isDirectory() ) {
						scanDirectoryFiles( pathname, true, result );
					}
				}
				return false;
			}
		};
		file.listFiles( filter );
	}

	/** @param file path to directory
	 * @param recursive scan subdirectories recursively
	 * @deprecated Use {@link #scanDirectoryFiles(java.io.File, boolean)} instead
	 * @return return separted list of absolut path to all files*/
	public static String scanDirectory(File file, final boolean recursive) {
		/*if (path == null) throw new NullPointerException("Path must not be null.");
		File file = new File(path);*/
		if (!file.exists()) return null;
		if (!file.isDirectory()) return file.getPath();
		final StringBuffer scannedFiles = new StringBuffer(1024);
		FileFilter filter = new FileFilter() {
			public boolean accept(File pathname) {
				if (!pathname.getName().startsWith(".")) {
					if (pathname.isDirectory()) {
						if (recursive) {
							pathname.listFiles(this);
						} else {
							// not recursive, ignore directories
						}
					} else {
						// this is a plain file
						if (scannedFiles.length() != 0) {
							scannedFiles.append("\r");
						}
						scannedFiles.append(pathname.getPath());
					}
				}
				return false;
			}
		};
		file.listFiles(filter);
		return scannedFiles.toString();
	}
}
