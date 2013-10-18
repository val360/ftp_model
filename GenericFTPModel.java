package com.prosc.ftpeek;

import com.enterprisedt.net.ftp.*;
import com.enterprisedt.net.ftp.pro.ProFTPClient;
import com.enterprisedt.net.ftp.pro.ProFTPClientInterface;
import com.enterprisedt.net.ftp.ssh.SSHFTPClient;
import com.enterprisedt.net.ftp.ssh.SSHFTPPublicKey;
import com.enterprisedt.net.ftp.ssl.SSLFTPCertificateException;
import com.enterprisedt.net.ftp.ssl.SSLFTPClient;
import com.enterprisedt.net.j2ssh.transport.publickey.InvalidSshKeyException;
import com.prosc.format.ByteLengthFormat;
import com.prosc.io.IOUtils;
import com.prosc.swing.InputStreamRangeModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.text.Format;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class GenericFTPModel implements FTPInterface {
	private static final Logger log = Logger.getLogger(GenericFTPModel.class.getName());

	ProFTPClientInterface client;
	private FTPFile[] lastDirListing;
	private Boolean escapeSpacesWithQuestionMarks = null;
	private static Preferences prefs = Preferences.userNodeForPackage(GenericFTPModel.class);
	private static File last_upload_dir = prefs.get("last_upload_dir", null) == null ? null : new File(prefs.get("last_upload_dir", null));

	private static final String HOST_VALIDATION_DISABLED = "HOST_VALIDATION_DISABLED";
	
	private String currentPath;

	public static File getLastDir(){
		return last_upload_dir;
	}

	public static void setLastDir(File file){
		prefs.put("last_upload_dir", file.getAbsolutePath());
		last_upload_dir = new File(prefs.get("last_upload_dir", null));
	}

	public GenericFTPModel(ProFTPClientInterface client, com.enterprisedt.util.debug.Level level) throws FTPException {
		com.enterprisedt.util.debug.Logger.setLevel(level);
		this.client = client;
	}

	public String getHostKey(String host) throws FTPException, IOException {
		if (client instanceof SSHFTPClient) {
			log.info("Getting SFTP host key");

			String[] params = host.split(":");
			host = params[0];
			if (params.length > 1) {
				String port = params[1];
				client.setRemotePort(Integer.parseInt(port));
			}
			client.setRemoteHost(host);

			SSHFTPPublicKey key = SSHFTPClient.getHostPublicKey(host, client.getRemotePort() );
			if( key == null ) throw new FTPException("Couldn't contact host " + host + " on port " + client.getRemotePort() + ". Make sure that you have the hostname correct, and that the SSH service is running on that port." );
			ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
			key.write(byteOutStream, SSHFTPPublicKey.OPENSSH_FORMAT);
			return new String(byteOutStream.toByteArray());
		}
		return null;
	}

/*	public void setKnownHost(String host) throws FTPException, IOException {
		if (client instanceof SSHFTPClient) {
			log.info("Setting known SFTP host");
			SSHFTPPublicKey key = SSHFTPClient.getHostPublicKey(host);
			ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
			key.write(byteOutStream, SSHFTPPublicKey.IETF_SECSH_FORMAT);
			byte[] bytes = byteOutStream.toByteArray();
			ByteArrayInputStream byteInStream = new ByteArrayInputStream(bytes);
			key.addKnownHost(host, byteInStream);
			((SSHFTPClient)client).setValidator(key);
		}
	}*/

	public void setKnownHost(String host, String publicKey) throws FTPException, IOException {
		if (client instanceof SSHFTPClient) {
			log.info("Setting known SFTP host with host key");

			if( publicKey.equals( HOST_VALIDATION_DISABLED )) {
				((SSHFTPClient) client).getValidator().setHostValidationEnabled(false);
				return;
			}

			//parse the host
			String[] params = host.split(":");
			host = params[0];
			if (params.length > 1) {
				String port = params[1];
				try {
					client.setRemotePort(Integer.parseInt(port));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid port number: " + port);
				}
			}
			client.setRemoteHost(host);

			//Add public key to known hosts
			if( client.getRemotePort() != 22) {
				host = "[" + host + "]:" + client.getRemotePort();
			}
			try {
				ByteArrayInputStream byteInStream = new ByteArrayInputStream(publicKey.getBytes());
				try {
					((SSHFTPClient) client).getValidator().addKnownHost(host, byteInStream);
				} finally {
					byteInStream.close();
				}
			} catch ( InvalidSshKeyException e ) {
				throw new InvalidSshKeyException( "The public key: " + publicKey + " is not in a supported format" );
			}
		}
	}

	public void setConfigFlag(int configFlag) throws FTPException {
		if ( client instanceof SSLFTPClient) {
			((SSLFTPClient)client).setConfigFlags(configFlag);
		}
	}

	public void setValidateServerCert(Boolean flag) throws FTPException {
		if (client instanceof SSLFTPClient) {
			((SSLFTPClient) client).setValidateServer(flag.booleanValue());
		}
	}

	public void setStartWithClearDataChannels(Boolean flag) throws FTPException {
		if (client instanceof SSLFTPClient) {
			if ( flag ) ((SSLFTPClient) client).setConfigFlags(SSLFTPClient.ConfigFlags.START_WITH_CLEAR_DATA_CHANNELS);
		}
	}

	public void setImplicitFTPS(boolean implicitFTPS) {
		if(client instanceof SSLFTPClient) {
			((SSLFTPClient) client).setImplicitFTPS(implicitFTPS);
		}
	}

	public void setEncoding(String encoding) throws FTPException {
		client.setControlEncoding( encoding );
	}

	public void setConnectMode( boolean activeMode ) {
		FTPConnectMode cMode;
		if (activeMode) {
			cMode = FTPConnectMode.ACTIVE;
		} else { //if active passed in, switch to active, otherwise default to passive
			cMode = FTPConnectMode.PASV;
		}
		if (client instanceof SSLFTPClient) {
			((SSLFTPClient) client).setConnectMode( cMode );
		}
		if (client instanceof ProFTPClient) {
			((ProFTPClient) client).setConnectMode( cMode );
		}
	}

	public void connect( String host, String username, String password ) throws FTPException, IOException {
		String[] params = host.split(":");
		host = params[0];
		if (params.length > 1) {
			String port = params[1];
			client.setRemotePort(Integer.parseInt(port));
		}
		client.setRemoteHost(host);
		if (client instanceof SSHFTPClient)  //SSH handshake
			((SSHFTPClient) client).setAuthentication(username, password);
		client.connect();
		if (client instanceof SSLFTPClient) {  //USE SSL
			try {
				if ( !((SSLFTPClient) client).isImplicitFTPS() ) {
					((SSLFTPClient) client).auth(SSLFTPClient.AUTH_SSL);
				}
				((SSLFTPClient) client).login(username, password);
			} catch (SSLFTPCertificateException e) {
				String message = e.getMessage();
				message = message.substring( 0, message.indexOf("(") ) + "(use ValidateServerCertificate=false flag to ignore invalid certificates)";
				throw new SSLFTPCertificateException( message );
			}
		} else if (!(client instanceof SSHFTPClient)) //USE Plain
			((ProFTPClient) client).login(username, password);
		log.fine("Setting transfer type to BINARY");
		client.setType(FTPTransferType.BINARY);
		log.info("Connected to server " + host);
		
		currentPath = client.pwd() + "/";
	}

	public String[] getFileList() throws FTPException, IOException {
		return getFileList(null);
	}

	/**
	 * Convenience method to show a file chooser dialog
	 * @param nativeFrame  TODO
	 * @return the selected file
	 */
	public static File chooseFile(final Component nativeFrame, FileChooserOptions options) {

		if (options == null) options = new FileChooserOptions();
		log.log(Level.FINE, "Showing file chooser dialog");
		//final File[] result = new File[1];
		// do some examination of the system to determine whether to allow native dialogs.
		// on OS X prior to Leopard, native dialogs crash filemaker.  Do not use.
		// on windows, native dialogs are OK, but they cannot be used to select directories
		// on leopard, native dialogs can be used to select files, or select directories, but not both.
		final String os = System.getProperty("os.version");
		boolean isMac = System.getProperty("os.name").startsWith("Mac");
		if (options.isUseNative()) {
			if ( isMac &&  (os == null || os.startsWith("10.4") || os.startsWith("10.3") || os.startsWith("10.2") || os.startsWith("10.1"))) {
				log.log(Level.WARNING, "Native file dialog disabled for OS X prior to 10.5 due to stability problems");
				options.setUseNative(false);
			} else if ( options.getFileSelectionMode() == JFileChooser.FILES_AND_DIRECTORIES) {
				log.log(Level.WARNING, "Native file dialog disabled, as it does not support selecting both files and directories");
				options.setUseNative(false);
			} else if ( !isMac && options.getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY) {
				log.log(Level.WARNING, "Native file dialog disabled, as windows native dialog does not support selecting directories");
				options.setUseNative(false);
			}
		}

		if (options.isUseNative()) {
			// use the native file chooser dialog
			// this crashes tiger, but seems to work on leopard.
			//
			// optionally set the apple system property which determines whether file dialogs are for directories only
			final String propKey = "apple.awt.fileDialogForDirectories";
			String oldPropValue = null;
			FileDialog dialog;
			try {
				oldPropValue = null;
				if (isMac) {
					oldPropValue = System.getProperty(propKey);
					System.setProperty(propKey, String.valueOf(options.getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY));
				}
				Frame frame = JOptionPane.getFrameForComponent(nativeFrame);
				dialog = options.getTitle() == null ? new FileDialog(frame) : new FileDialog(frame, options.getTitle());
				if( options.getInitialDir() != null ) {
					dialog.setDirectory( options.getInitialDir().getAbsolutePath() );
				}
				dialog.setLocationRelativeTo(null);
				dialog.setVisible( true );
			} finally {
				// restore the apple system property
				if (isMac) {
					if (oldPropValue != null) {
						System.setProperty(propKey, oldPropValue);
					} else {
						System.getProperties().remove(propKey);
					}
				}
			}
			if (dialog.getFile() == null) {
				return null;
			} else {
				return new File(dialog.getDirectory() + dialog.getFile());
			}
		} else {
			// non-native swing dialog
			JFileChooser chooser;
			if (options.getInitialDir() != null) {
				chooser = new CenteredFileChooser(options);
			} else {
				chooser = new CenteredFileChooser();
			}
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileSelectionMode(options.getFileSelectionMode());
			if ( options.getTitle() != null ) {
				chooser.setDialogTitle( options.getTitle() );
			}
			int choice = chooser.showDialog(nativeFrame, null);
			if(choice == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null ) {
				return chooser.getSelectedFile();
			} else {
				return null;
			}
		}
	}

	public String[] getFileList(String dirName) throws FTPException, IOException {
		log.info("Getting FTP file list from specified directory");
		log.info("pwd: " + client.pwd());

		if( dirName == null ) {
			dirName = client.pwd();
		}
		try {
			getDirDetails(dirName);
		} catch( ParseException e ) {
			lastDirListing = null;
			log.log( Level.WARNING, "Could not get detailed information for this directory, just returning names", e );
			return client.dir( dirName );
		}
		String[] result = new String[ lastDirListing.length ];
		for( int n=0; n<lastDirListing.length; n++ ) {
			result[n] = lastDirListing[n].getName();
			if( lastDirListing[n].isDir() ) result[n] += "/"; //Append a trailing slash to directories
		}
		return result;
	}

	private void getDirDetails(String dirName) throws IOException, FTPException, ParseException {
		try {
			lastDirListing = dirDetails( dirName );
		} catch( ParseException e ) {
			if( client instanceof FTPClient ) {
				((FTPClient)client).setParserLocale( Locale.US ); //If the client is international, FTPClient will try their locale first, which will fail if going to a US server
				lastDirListing = dirDetails( dirName );
			} else {
				throw e;
			}
		}
	}

	public long getSize( String path ) throws FTPException, IOException {
		if( path.endsWith("/") ) path = path.substring( 0, path.length() - 1 ); //If path is a directory, then take off the trailing slash

		String parentFolder = path.lastIndexOf("/") == -1 ? "/" : path.substring(0, path.lastIndexOf("/"));
		if( lastDirListing == null ) {
			refetchDirListing(client.pwd());
		}

		//Look for the file in the current list of files
		int n;
		for( n=0; n<lastDirListing.length; n++ ) {
			if( lastDirListing[n].getName().equals( path ) ) break;
		}
		if( n == lastDirListing.length ) {
			refetchDirListing(parentFolder);
			for( n=0; n<lastDirListing.length; n++ ) {
				if( lastDirListing[n].getName().equals( path ) ) break;
			}
		}
		return lastDirListing[n].size();
	}

	private void refetchDirListing(String parentFolder) throws IOException, FTPException {
		try {
			getDirDetails(parentFolder);
		} catch (ParseException e) {
			lastDirListing = null;
			log.log( Level.SEVERE, "Could not get the detailed dir listing", e );
		}
	}

	public Date getModDate( String path ) throws FTPException, IOException {
		if( path.endsWith("/") ) path = path.substring( 0, path.length() - 1 ); //If path is a directory, then take off the trailing slash

		String parentFolder = path.lastIndexOf("/") == -1 ? "/" : path.substring(0, path.lastIndexOf("/"));
		if( lastDirListing == null ) {
			refetchDirListing(parentFolder);
		}

		//Look for the file in the current list of files
		int n;
		for( n=0; n<lastDirListing.length; n++ ) {
			if( lastDirListing[n].getName().equals( path ) ) break;
		}
		if( n == lastDirListing.length ) {
			refetchDirListing(parentFolder);
			for( n=0; n<lastDirListing.length; n++ ) {
				if( lastDirListing[n].getName().equals( path ) ) break;
			}
		}
		return lastDirListing[n].lastModified();

	}

	/** Downloads the remoteFilePath to the localFile OutputStream. The calling code is responsible for closing the OutputStream. */
	public void downloadFile(final OutputStream localFile, final String remoteFilePath, final FTPTransferType transferType, boolean showProgress ) throws IOException, FTPException {
		//FIX! Should we preserve the original mod date of the file on the FTP server as the mod date for the downloaded file?
		log.info("Downloading " + remoteFilePath + " to " + localFile);
		if( ! showProgress ) {
			if (transferType.equals(FTPTransferType.ASCII)) { //Check if need to use text
				client.setType(transferType);
			}
			client.get(localFile, remoteFilePath);
		} else {
			final TransferStatus dialog = new TransferStatus(JOptionPane.getRootFrame());
			PipedInputStream pis = new PipedInputStream();
			final PipedOutputStream pos = new PipedOutputStream(pis);
			final InputStreamRangeModel localRangeInputStream = new InputStreamRangeModel(pis);

			dialog.getProgressBar().setModel(localRangeInputStream);
			dialog.pack();
			dialog.setLocationRelativeTo(null);
			log.info("created dialog, creating transfer thread");

			final FTPException[] ftpException = new FTPException[1];
			final IOException[] ioException = new IOException[1];
			final Throwable[] throwable = new Throwable[1];

			Thread threadDownload = new Thread() {
				public void run() {
					try {
						log.fine( "Starting IOUtils thread" );
						IOUtils.writeInputToOutput(localRangeInputStream, localFile, 65536);
					} catch (IOException e) {
						ioException[0] = e;
					} catch (Throwable t) {
						throwable[0] = t;
					}
					log.fine( "download thread is finished" );
				}
			};

			Thread thread = new Thread() {
				public void run() {
					try {
						try {
							log.info("starting transfer");

							if (transferType.equals(FTPTransferType.ASCII)) { //Check if need to use text
								client.setType(transferType);
							}

							log.info( "getting size" );
							FTPFile theFile;
							long size;
							try {  //Attempt to get the size of the file directly
								size = client.size(remoteFilePath);
							} catch (Throwable e) {
								log.log(Level.INFO, "problem getting file size", e);
								theFile = getFileDetails( remoteFilePath );
								size = theFile.size();
							}
							//final long fileSize = client.size(remoteFilePath); //We switched to using dirDetails instead for two reasons:
							//1) size is not supported by all FTP servers
							//2) size does not work with leading slashes
							final long fileSize = size;
							log.info( "size is " + fileSize );


							localRangeInputStream.setMaximum((int) fileSize);
							EventQueue.invokeLater(new Runnable() {
								public void run() {
									dialog.getStatusLabel().setText("Downloading " + remoteFilePath + ": ");
								}
							});
							final Format formatter = new ByteLengthFormat(new BigDecimal(50), new BigDecimal(999));
							final String totalSizeString = formatter.format(Long.valueOf(fileSize));
							localRangeInputStream.addChangeListener(new ChangeListener() {
								public void stateChanged(ChangeEvent e) {
									String currentSizeString = formatter.format(Integer.valueOf(localRangeInputStream.getValue()));
									dialog.getStatusLabel().setText("Downloading " + remoteFilePath + ": " + currentSizeString + "/" + totalSizeString);
									int percent = (int) Math.round( (double)localRangeInputStream.getValue() / (double)fileSize * 100.0 );
									dialog.getPercentLabel().setText(String.valueOf(percent) + "%");
								}
							});

							client.get(pos, remoteFilePath);

							log.fine("finished transfer");
						} finally {
							pos.close();
							EventQueue.invokeLater(new Runnable() {
								public void run() {
									log.fine("disposing dialog");
									Window owner = dialog.getOwner();
									dialog.dispose();
									owner.dispose(); //Hack for PluginBridge memory leak
									log.fine("disposed dialog");
								}
							});
						}
					} catch (FTPException e) {
						ftpException[0] = e;
					} catch (IOException e) {
						ioException[0] = e;
					} catch (Throwable t) {
						throwable[0] = t;
					}
					log.fine( "Main thread is finished" );
				}
			};
			threadDownload.start();
			log.fine("starting transfer thread");
			thread.start();
			log.fine("making dialog visible");
			dialog.setVisible(true);
			log.fine( "Dialog has been disposed" );
			try {
				log.fine( "Waiting for thread1 to finish" );
				thread.join();
				log.fine( "Waiting for thread2 to finish" );
				threadDownload.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			if (ioException[0] != null) {
				throw ioException[0];
			}
			if (ftpException[0] != null) {
				throw ftpException[0];
			}
			if (throwable[0] != null) {
				if (throwable[0] instanceof RuntimeException) throw (RuntimeException) throwable[0];
				if (throwable[0] instanceof Error) throw (Error) throwable[0];
				throw new RuntimeException(throwable[0]);
			}

		}


		if (transferType.equals(FTPTransferType.ASCII)) { // check if used ASCII and switch back to Binary
			client.setType(FTPTransferType.BINARY);
		}
		log.info("Downloaded " + remoteFilePath );
	}

	private FTPFile getFileDetails( String remoteFilePath ) throws IOException, FTPException, ParseException {
		String dirPath = "";
		if( remoteFilePath.lastIndexOf('/') > -1 ) {
			dirPath = remoteFilePath.substring( 0, remoteFilePath.lastIndexOf( '/' ) );
		}
		String fileName = remoteFilePath.substring( remoteFilePath.lastIndexOf( '/' ) + 1 );

		FTPFile[] allFiles = dirDetails( dirPath );

		FTPFile theFile = null;
		for( int n=0; n<allFiles.length; n++ ) {
			if( allFiles[n].getName().equals( fileName ) ) {
				theFile = allFiles[n];
				break;
			}
		}
		if( allFiles.length == 0 || theFile == null ) {
			throw new FTPException("Cannot get file information at remote path " + remoteFilePath + " ( server name: " + client.getRemoteHost() + "; current dir: " + client.pwd() + ")" );
		}
		return theFile;
	}

	private FTPFile[] dirDetails( String dirPath ) throws FTPException, IOException, ParseException {
		FTPFile[] allFiles;
		//dirPath = dirPath.replace(" ", "?");
		//dirPath = "\'" + dirPath + "\'";
		log.info("dirPath used for getting files:" + dirPath);
		if( dirPath.indexOf( ' ' ) == -1 ) { //There are no spaces
			allFiles = _dirDetails( dirPath, false );
		} else if( escapeSpacesWithQuestionMarks != null ) { //There are spaces, but we know how to handle them
			allFiles = _dirDetails( dirPath, escapeSpacesWithQuestionMarks.booleanValue() );
		} else { //There are spaces and we don't know how to handle them
			try {
				allFiles = _dirDetails( dirPath, true );
				escapeSpacesWithQuestionMarks = Boolean.TRUE;
				if ( allFiles.length == 1 )  //some servers do not support wild cards, maybe b/c they quote the paths that contain spaces, and return the folder dirDetails was called on.  The code below deals with that.
				{
					String name = "";
					if(dirPath.endsWith("/"))
					{
						dirPath = dirPath.substring(0, dirPath.lastIndexOf("/"));

					}
					name = dirPath.substring(dirPath.lastIndexOf("/") + 1);
					if ( allFiles[0].getName().equals(name) )
					{
						throw new FTPException("dirDetails returned a result that's is the same path that was passed in.");
					}
				}
			} catch( FTPException e ) {
				allFiles = _dirDetails( dirPath, false );
				escapeSpacesWithQuestionMarks = Boolean.FALSE;
			}
		}
		return allFiles;
	}

	private FTPFile[] _dirDetails( String dirPath, boolean escapeSpaces ) throws FTPException, IOException, ParseException {
		if ( dirPath.equals( currentPath ) ) {
		 dirPath = ".";
		}

		if( escapeSpaces ) {
			return client.dirDetails( dirPath.replace(' ', '?' ) );
		} else {
			return client.dirDetails( dirPath );
		}
	}

	public void uploadFile(final String remotePath, InputStream localFileStream, final long fileSize, FTPTransferType transferType, boolean showProgress ) throws FTPException, IOException {
		String folderPath= (!remotePath.contains("/")) ? "" : remotePath.substring(0, remotePath.lastIndexOf("/"));

		// throw exception when the user is uploading a folder

		//FIX! Check to see if the underlying client library really streams, or if it takes it all into memory
		//FIX! library does not stream using what we do below, look into FTPOutputStream class
		//FIX! also, the FTPOutputStream is not supported for sftp -val
		if ( folderPath.length() != 0 && !FTPUtils.existsDir(this, folderPath) ) {
			makeDir(folderPath);
		}
		log.info("Uploading file to remote path " + remotePath);
		if (transferType.equals(FTPTransferType.ASCII)) {			// Check if need to use text transfer
			client.setType(transferType);
		}

		//if( ! EventQueue.isDispatchThread() || ! showProgress ) { // this is hanging on FileMaker server
		if( ! showProgress ) {
			try {
				client.put(localFileStream, remotePath);
			} finally {
				localFileStream.close();
			}
		} else {
			final InputStreamRangeModel localRangeInputStream = new InputStreamRangeModel(localFileStream);
			localRangeInputStream.setMaximum((int) fileSize);
			final TransferStatus dialog = new TransferStatus(JOptionPane.getRootFrame());
			//final TransferStatus dialog = new TransferStatus((Frame)null);
			dialog.getStatusLabel().setText("Uploading to " + remotePath + ": ");
			final Format formatter = new ByteLengthFormat(new BigDecimal(50), new BigDecimal(999));
			final String totalSizeString = formatter.format(Long.valueOf(fileSize));
			localRangeInputStream.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					String currentSizeString = formatter.format(Integer.valueOf(localRangeInputStream.getValue()));
					dialog.getStatusLabel().setText("Uploading to " + remotePath + ": " + currentSizeString + "/" + totalSizeString);
					int percent = (int) Math.round( (double)localRangeInputStream.getValue() / (double)fileSize * 100.0 );
					dialog.getPercentLabel().setText(String.valueOf(percent) + "%");
				}
			});
			dialog.getProgressBar().setModel(localRangeInputStream);
			dialog.pack();
			dialog.setLocationRelativeTo(null);
			log.info("created dialog, creating transfer thread");

			final FTPException[] ftpException = new FTPException[1];
			final IOException[] ioException = new IOException[1];
			final Throwable[] throwable = new Throwable[1];
			Thread thread = new Thread() {
				public void run() {
					try {
						log.info("starting transfer");
						client.put(localRangeInputStream, remotePath);
						log.info("finished transfer");
					} catch (FTPException e) {
						ftpException[0] = e;
					} catch (IOException e) {
						ioException[0] = e;
					} catch (Throwable t) {
						throwable[0] = t;
					} finally {
						try {
							localRangeInputStream.close();
						} catch (IOException e) {
							log.log(Level.INFO, "problem closing upload stream", e);
						}

						EventQueue.invokeLater(new Runnable() {
							public void run() {
								log.info("disposing dialog");
								Window owner = dialog.getOwner();
								dialog.dispose();
								owner.dispose();
								log.info("disposed dialog");
							}
						});
					}
				}
			};
			log.info("starting transfer thread");
			thread.start();
			log.info("making dialog visible");
			dialog.setVisible(true);
			try {
				thread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			if (ioException[0] != null) {
				throw ioException[0];
			}
			if (ftpException[0] != null) {
				throw ftpException[0];
			}
			if (throwable[0] != null) {
				if (throwable[0] instanceof RuntimeException) throw (RuntimeException) throwable[0];
				if (throwable[0] instanceof Error) throw (Error) throwable[0];
				throw new RuntimeException(throwable[0]);
			}
		}


		if (transferType.equals(FTPTransferType.ASCII)) { // check if used ASCII and switch back to Binary
			client.setType(FTPTransferType.BINARY);
		}
		System.gc(); //There seems to be a memory leak when progress dialog is visible... ?
		log.info("Uploaded file to remote path " + remotePath);
	}

	//backup for progress bar experimentation
/*	public void uploadFile(String remotePath, InputStream localFileStream, FTPTransferType transferType) throws FTPException, IOException {
		String folderPath = (remotePath.indexOf("/") < 0) ? "" : remotePath.substring(0, remotePath.lastIndexOf("/"));
		//String filename = remotePath.substring(remotePath.lastIndexOf("/") + 1, remotePath.length());
		//FIX Check to see if the underlying client library really streams, or if it takes it all into memory
		makeDir(folderPath); //create full path if necessary
		log.info("Uploading file to remote path " + remotePath);
		if (transferType.equals(FTPTransferType.ASCII)) { //Check if need to use text
			client.setType(transferType);
		}
		client.put(localFileStream, remotePath);
		if (transferType.equals(FTPTransferType.ASCII)) { // check if used ASCII and switch back to Binary
			client.setType(FTPTransferType.BINARY);
		}
		log.info("Uploaded file to remote path " + remotePath);
	}*/

	public void disconnect() throws FTPException, IOException {
		client.quit();
		log.info("Disconnected from server " + client.getRemoteHost());
	}

	public void rename(String from, String to) throws FTPException, IOException {
		if ( !client.exists(from) ) throw new FTPException("The file to be renamed does not exist.");

		FTPFile fromFile = new FTPFile(from);
		FTPFile toFile = new FTPFile(to);
		if ( fromFile.isDir() ) throw new IOException("Renaming directories is not supported.");
		//if ( fromFile.getPath().equals(toFile.getPath()) )  //FIX!!! finish this check -val
		client.rename(from, to);
		log.info("Changed name from " + from + " to " + to);
	}

	// FIX for FTP-6

	// moving a directory
	public void moveDir(String prevDirLocation, String newDirLocation) throws IOException, FTPException {
		// does the new location of transfer already exist??
		client.rename(prevDirLocation, newDirLocation + "/" + prevDirLocation + "/");
		client.rmdir(prevDirLocation);
	}

	public String getCurrentDir() throws FTPException, IOException {
		String currentDir = client.pwd();
		log.info("Currently in " + currentDir);
		return currentDir;
	}

	public void changeDir(String remoteDir) throws FTPException, IOException {
		log.info("remoteDir used in chDir: " + remoteDir);
		client.chdir(remoteDir);

		currentPath = client.pwd() + "/";
		log.info( "chdir into " + currentPath );
	}

	public void changeDirUp() throws FTPException, IOException {
		try {
			if (!client.pwd().equals("/"))
				client.cdup(); //cdup only if not in root, otherwise problematic for some servers (crushftp, etc)
		} catch (FTPException e) {
			if (!e.toString().contains("You are already at the root")) //Fix find a better way < downgraded this one b/c I don't think we end up throwing this ever. -val
				throw new RuntimeException(e);
			else
				throw e;
		}
		
		currentPath = client.pwd() + "/";
		log.info("chdir into " + currentPath);
	}

	public void changeDirRoot() throws FTPException, IOException {
		changeDir("/");
	}

	public void deleteFile(String remoteFile, boolean useWildcards) throws FTPException, IOException, ParseException {
		if(useWildcards) {
			client.mdelete(remoteFile);
			log.info("Deleted file(s) matching " + remoteFile);
		}
		else {
			client.delete(remoteFile);
			log.info("Deleted file " + remoteFile);
		}
	}

	public void deleteDir(String remoteDir, boolean recursive) throws FTPException, IOException, ParseException {
		client.rmdir(remoteDir, recursive);
		if( recursive ) log.info("Deleted dir " + remoteDir + " and it's contents");
		else log.info("Deleted dir " + remoteDir);
	}

	public void makeDir(String remoteDir) throws FTPException, IOException {
		String currentDir = client.pwd();
		String[] dirs = remoteDir.split("/");
		for (int i = 0; i < dirs.length; i++) {
			String dir = (dirs[i].equals("")) ? "/" : dirs[i];
			if (dir.equals("/") || FTPUtils.existsDir(this, dir)) {  //Using FTPUtils.existsDir() instead of client.exists() b/c exists() is only for files not for directories -val
				client.chdir(dir);
			} else {
				client.mkdir(dir);
				client.chdir(dir);
				log.info("mkdir " + client.pwd());
			}
		}
		client.chdir(currentDir); //go back to where you started
	}

	public int isConnected() {
		return client.connected() ? 1 : 0;
	}

	public int exists(String path) throws FTPException, IOException {
		return client.exists(path) ? 1 : 0;
	}

	public String executeCommand(String command) throws IOException, FTPException {
		return client.executeCommand(command);
	}

	private static class CenteredFileChooser extends JFileChooser {
		public CenteredFileChooser() {
		}

		public CenteredFileChooser(final FileChooserOptions options) {
			super(options.getInitialDir().getAbsolutePath());
		}

		protected JDialog createDialog(final Component parent) throws HeadlessException {
			final JDialog result = super.createDialog(parent);
			if (parent.getWidth() == 0 || parent.getHeight() == 0) {
				result.setLocationRelativeTo(null);// center on-screen
			}
			return result;
		}
	}
}

