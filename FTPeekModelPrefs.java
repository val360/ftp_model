package com.prosc.ftpeek;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Handles persisting the last directory selected by the user.
 */
public class FTPeekModelPrefs {
	private static Preferences prefs = Preferences.userNodeForPackage(FTPeekModelPrefs.class);
	private static File last_upload_dir = prefs.get("last_upload_dir", null) == null ? null : new File(prefs.get("last_upload_dir", null));

	/**
	 * Gets the last directory passed to setLastDir.
	 * @return the last directory the user selected
	 */
    public static File getLastDir(){
        return last_upload_dir;
    }

	/**
	 * Sets and persists the last directory chosen by the user.
	 * @param file directory
	 */
    public static void setLastDir(File file){
	    prefs.put("last_upload_dir", file.getAbsolutePath());
	    last_upload_dir = new File(prefs.get("last_upload_dir", null));
    }
}