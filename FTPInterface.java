package com.prosc.ftpeek;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Date;

public interface FTPInterface {
	public void connect( String host, String username, String password ) throws FTPException, IOException;

	String[] getFileList() throws FTPException, IOException;

	String[] getFileList(String dirName) throws FTPException, IOException;

	void downloadFile(OutputStream localFile, String remoteFilePath, FTPTransferType transferType, boolean showProgress ) throws IOException, FTPException, ParseException;
	
	void uploadFile(String remoteFile, InputStream localFile, long fileSize, FTPTransferType transferType, boolean showProgress ) throws FTPException, IOException;

	void disconnect() throws FTPException, IOException;

	void rename(String from, String to) throws FTPException, IOException;

	String getCurrentDir() throws FTPException, IOException;

	void changeDir(String remoteDir) throws FTPException, IOException;

	void changeDirUp() throws FTPException, IOException;

	void changeDirRoot() throws FTPException, IOException;

	void deleteFile(String remoteFile, boolean useWildcards) throws FTPException, IOException, ParseException;

	void deleteDir(String remoteDir, boolean recursive) throws FTPException, IOException, ParseException;

	void makeDir(String remoteDir) throws FTPException, IOException;

	int isConnected();

	int exists(String path) throws FTPException, IOException;

	long getSize( String path ) throws FTPException, IOException;

	Date getModDate( String path ) throws FTPException, IOException;

	String executeCommand( String command ) throws FTPException, IOException;
}
