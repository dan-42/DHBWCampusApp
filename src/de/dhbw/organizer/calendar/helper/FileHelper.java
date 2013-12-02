/**
 * LICENSE: GPL v3 
 * 
 * Copyright (c) 2013 by
 * Daniel Friedrich <friedrda@dhbw-loerrach.de>
 * Simon Riedinger <riedings@dhbw-loerrach.de>
 * Patrick Strittmatter <strittpa@dhbw-loerrach.de> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3.0 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package de.dhbw.organizer.calendar.helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.content.Context;
import android.util.Log;

/**
 * @author friedrda
 * 
 */
public class FileHelper {

	private static final String TAG = "FileHelper";

	/**
	 * creates on the given Context a cache / temporary file
	 * 
	 * @param context
	 * @param filename
	 * @param fileending The file ending e.g. .txt or .ical
	 * @return cache file or null
	 */
	public static File createCacheFile(Context context, String filename, String fileending) {
		File outputDir = context.getCacheDir();
		File cacheFile = null;
		try {
			cacheFile = File.createTempFile(filename, fileending, outputDir);
		} catch (IOException e) {
			Log.e(TAG, "createCacheFile() " + e.getMessage());
		}

		return cacheFile;
	}

	/**
	 * reads data from InputStream line by line and writes it to the given file
	 * InputStreams gets closed the File f will be returned
	 * 
	 * @param is
	 *            InputStream
	 * @param f
	 *            File
	 * @return File f or null if any error
	 */
	public static File writeInputStreamToFile(InputStream is, File f) {

		// write to internal storage
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(f);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "writeImpuStreamToFile() ERROR: " + e.getMessage());
			return null;
		}
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String s = "";
		try {
			while ((s = reader.readLine()) != null) {
				writer.write(s);
			}
		} catch (IOException e) {
			Log.e(TAG, "writeImpuStreamToFile() ERROR: " + e.getMessage());
			return null;
		}
		try {
			if (reader != null)
				reader.close();
			if (writer != null)
				writer.close();

			if (fos != null) {
				fos.flush();
				fos.close();
			}
			if (is != null)
				is.close();
		} catch (IOException e) {
			Log.e(TAG, "writeImpuStreamToFile() ERROR: " + e.getMessage());
			return null;
		}

		return f;
	}

	/**
	 * Copies via bufferd reader / writer the input of the file into the
	 * FileOutputStream
	 * 
	 * @param cacheFile
	 * @param fos
	 * @throws IOException
	 */
	public static void writeFileToOutputStream(File f, FileOutputStream fos) {

		FileInputStream fis = null;
		BufferedWriter writer = null;
		BufferedReader reader = null;
		String s = null;
		try {
			fis = new FileInputStream(f);
			writer = new BufferedWriter(new OutputStreamWriter(fos));
			reader = new BufferedReader(new InputStreamReader(fis));
			while ((s = reader.readLine()) != null) {
				writer.write(s);
			}

		} catch (IOException e) {
			Log.e(TAG, "writeFileToOutputStream() ERROR: " + e.getMessage());
		}
		try {
			if (reader != null)
				reader.close();
			if (writer != null)
				writer.close();

			if (fos != null) {
				fos.flush();
				fos.close();
			}

		} catch (IOException e) {
			Log.e(TAG, "writeFileToOutputStream() ERROR: " + e.getMessage());
		}

	}
	
	/**
	 * reads the content of a file into a String 
	 * @param context
	 * @param fileName
	 * @return content of File as a String
	 * @throws IOException
	 */
	public static String readFileAsString(Context context, String fileName) throws IOException {

		BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(context.getFilesDir() + File.separator + fileName)));
		String read;
		StringBuilder builder = new StringBuilder("");

		while ((read = bufferedReader.readLine()) != null) {
			builder.append(read);
		}		
		bufferedReader.close();
		return builder.toString();

	}

	/**
	 * writes the given data as String into the File
	 * @param context
	 * @param fileName
	 * @param data
	 * @throws IOException
	 */
	public static void writeFileAsString(Context context, String fileName, String data) throws IOException {				
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(context.getFilesDir() + File.separator + fileName)));
		bufferedWriter.write(data);
		bufferedWriter.flush();
		bufferedWriter.close();
	}	

}
