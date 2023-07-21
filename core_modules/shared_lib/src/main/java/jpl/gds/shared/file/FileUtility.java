/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.shared.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.util.BinOctHexUtility;

/**
 * Utilities for file copying.
 *
 */
public class FileUtility
{
	private static final int BUFFER_SIZE = 800;


	/**
	 * Copies a file.
	 * 
	 * @param source
	 *            the source file
	 * @param destination
	 *            the destination file
	 * @throws IOException I/O exception
	 */
	public static void copyFile(File source, File destination) throws IOException
	{
		FileChannel srcChannel = null;
		FileChannel dstChannel = null;
	
		try {
			srcChannel = new FileInputStream(source).getChannel();
			dstChannel = new FileOutputStream(destination).getChannel();
			dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } finally {
			srcChannel.close();
			dstChannel.close();
		}
	}
	
	/**
	 * Copies a file.
	 * 
	 * @param source
	 *            the name of the source file
	 * @param destination
	 *            the name of the destination file
	 * @throws IOException I/O error
	 */
	public static void copyFile(String source, String destination) throws IOException
	{
		copyFile(new File(source),new File(destination));
	}


    /**
     * Copy directory, file by file.
     *
     * @param source      Source directory	
     * @param destination Destination directory	
     *
     * @throws IOException I/O error
     */
	public static void copyDirectory(String source, String destination) throws IOException
	{
		File sourceDir = new File(source);
		File destDir = new File(destination);
		
		if (!sourceDir.exists() || !sourceDir.isDirectory()) {
			throw new IOException("Source directory " + source + " does not exist or is not a directory");
		}
		
		if (destDir.exists() && !destDir.isDirectory()) {
			throw new IOException("Destination directory " + destination + " does not exist or is not a directory");
		}
		
		if (!destDir.exists()) {
			if (!destDir.mkdirs()) {
				throw new IOException("Directory " + destination + " cannot be created");
			}
		}
		
		File[] files = sourceDir.listFiles();
		for (File file : files) {
			if (file.isFile()) {
			    copyFile(file.getPath(), destination + File.separator
			    		+ file.getName());	
			} else if (!file.getAbsolutePath().equals(destDir.getAbsolutePath())) {
				copyDirectory(file.getPath(), destination + File.separator + file.getName());
			}
		}	
	}
	
    /**
     * Write binary data from file to stream. Binary data is in file in hex form.
     *
     * @param os        Output stream
     * @param inputFile File to copy
     *
     * @throws IOException I/O error
     */	
	public static void writeFileToOutputStream(final OutputStream os, final File inputFile) throws IOException
	{
		writeFileToOutputStream(os,inputFile,false);
	}
	

    /**
     * Write binary data from file to stream. Binary data is in file in hex form.
     *
     * @param os        Output stream
     * @param inputFile File to copy
     * @param isHexFile If true, write hex data as characters
     *
     * @throws IOException I/O error
     */	
	public static void writeFileToOutputStream(final OutputStream os, final File inputFile, final boolean isHexFile) throws IOException
    {
		if(isHexFile == true)
		{
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(inputFile));
				String line = reader.readLine();
				while(line != null)
				{
					byte[] bytes = null;
					String charString = GDR.removeWhitespaceFromString(line.trim());
					try
					{
						bytes = BinOctHexUtility.toBytesFromHex(charString);
					}
					catch(IllegalArgumentException iae)
					{
						throw new IOException("The input hex raw data file contains illegal hex characters: \"" + charString + "\"");
					}
					os.write(bytes);
					line = reader.readLine();
				}
			} finally {
				reader.close();
			}
		}
		else
		{
			byte[] inputBytes = new byte[BUFFER_SIZE];
			byte[] tempBytes = null;
			FileInputStream fis = new FileInputStream(inputFile);

			int result = fis.read(inputBytes);
			while(result != -1)
			{
				if(result != inputBytes.length)
				{
					tempBytes = new byte[result];
					System.arraycopy(inputBytes,0,tempBytes,0,result);
					inputBytes = tempBytes;
				}
				os.write(inputBytes);
				result = fis.read(inputBytes);
			}
			fis.close();
		}

		os.flush();
		os.close();
    }
	

	/**
	 * Copy file to session directory using original name.
	 * 
	 * @param sessionOutputDir directory path of the session output
     * @param source Original file
     * @return New file
     * @throws IOException I/O error
	 */
	public static File copyFileToSessionDirectory(final String sessionOutputDir, final File source) throws IOException
	{
		File destination = new File(sessionOutputDir + File.separator + source.getName());
		copyFile(source,destination);
		return(destination);
	}


    /**
     * Write binary data to file. Binary data is in a string in hex form.
     *
     * @param filename  File name
     * @param hexData   Hex data in string
     * @param isHexFile If true, write hex data as characters
     *
     * @return File object written to
     *
     * @throws IOException I/O error
     */	
	public static File writeRawDataFileFromHex(final String filename, final String hexData,final boolean isHexFile) throws IOException
	{
		File outputFile = new File(filename);
		
		if(isHexFile == true)
		{
			FileWriter fw = new FileWriter(outputFile);
			fw.write(hexData);
			fw.flush();
			fw.close();
		}
		else
		{
			byte[] totalRawBytes = BinOctHexUtility.toBytesFromHex(hexData);

			FileOutputStream fos = new FileOutputStream(outputFile);
			fos.write(totalRawBytes);
			fos.flush();
			fos.close();
		}
		
		return(outputFile);
	}
	
	/**
     * Creates a message string indicating absolute path to a file.
	 * Output Absolute Path
	 * 
	 * @param path path to the file
	 * @return message string containing absolute path
	 */
	public static String createFilePathLogMessage(String path) {
		return createFilePathLogMessage((null == path) ? null : new File(path));
	}
	
	/**
	 * Creates a message string indicating absolute path to a file.
	 *  
	 * Output Absolute Path
	 * 
	 * @param path File object to get path for
	 * @return message string containing absolute path
	 */
	public static String createFilePathLogMessage(File path) {
		if (null == path) {
			return "<<< CANNOT DE-REFERENCE due to: NULL PATH >>>";
		}

		StringBuilder sb = new StringBuilder();
		final String absolutePath = path.getAbsolutePath();
		String canonicalPath;
		
		try {
			canonicalPath = path.getCanonicalPath();
			if (absolutePath.equals(canonicalPath)) {
				canonicalPath = null;
			}
		}
		catch (IOException e) {
			canonicalPath = "<<< CANNOT DE-REFERENCE due to: " + e + " >>>";
		}
		
		sb.append("absolute path: ");
		sb.append(absolutePath);
		if (null != canonicalPath) {
			sb.append(" >>>---> which points to canonical path >>>--->: ");
			sb.append(canonicalPath);
		}
		return sb.toString();
	}
}
