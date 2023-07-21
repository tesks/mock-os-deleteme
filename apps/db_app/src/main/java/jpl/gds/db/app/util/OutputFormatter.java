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
package jpl.gds.db.app.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import jpl.gds.db.api.types.IDbQueryable;

/**
 * Interface used by GetEverythingApp to write files using a certain output
 * format. The output format types are specified by OutputFormatType.java. Note
 * that GetEverythingApp writes single files, containing one database record
 * type, and a merged, sorted file containing everything. It also creates one
 * file per EVR level. These aspects are reflected in this interface.
 * 
 */
public interface OutputFormatter {
    /**
     * Writes a string to the sorted output file.
     * 
     * @param data
     *            the String to write.
     * @param isHeader
     *            should be true if printing a header, otherwise false
     */
    public void writeObjectSorted(String data, boolean isHeader);

    /**
     * Writes an object to the writer associated with the IDbQueryable object
     * passed.
     * 
     * @param dq
     *            the IDbQueryable object to write
     * @param isHeader
     *            should be true if printing a header, otherwise false
     */
    public void writeObject(IDbQueryable dq, boolean isHeader);

	/**
	 * Writes an object to the writer corresponding to the given EVR level.  
	 * 
	 * @param dq the IDbQueryable object to write
     * @param level EVR level
     * @param isHeader should be true if printing a header, otherwise false	  
	 */
	public void writeObjectEL(IDbQueryable dq, String level, boolean isHeader);

    /**
     * Writes to the PrintWriter associated with the IDbQueryable object passed.
     * This method is used to implement specialized output formats such as
     * Excel, etc.
     * 
     * @param dq
     *            the IDbQueryable object to write
     * @param isHeader
     *            should be true if printing a table header, otherwise false
     * @throws IOException
     *             I/O error
     */
    void writeObjectCustom(IDbQueryable dq, boolean isHeader) throws IOException;

    /**
     * Sets up a single file writer with the passed filename, for the given type
     * of IDbQueryable object.
     * 
     * @param dq
     *            the IDbQueryable object to set up writing for
     * @param filename
     *            the name of the file to write to
     * 
     * @throws FileNotFoundException
     *             File not found
     */
    public void setUp(IDbQueryable dq, String filename) throws FileNotFoundException;
	
	/**
     * Creates a writer with the passed filename for the specified EVR level.
     * Used for printing EVR level files.
     * 
     * @param level
     *            the EVR level
     * @param filename
     *            the name of the EVR level file to write to
     * 
     * @throws FileNotFoundException
     *             if file cannot be found
     */
	public void setUpEL(String level, String filename) throws FileNotFoundException;

	/**
     * Sets up the writer for the sorted output file, using the given filename.
     * 
     * @param filename
     *            file path of the sorted file
     * 
     * @throws FileNotFoundException
     *             if file cannot be found
     */
	public void setUpSorted(String filename) throws FileNotFoundException;
	
    /**
     * Print formatter specific file
     * 
     * @throws IOException
     *             on IOError
     */
    public void printFormatterSpecificFile() throws IOException;

    /**
     * Closes all of the files but the sorted file.
     */
	public void closeSingleFiles();

	/**
	 * Closes the sorted file.
	 */
	public void closeSortedFile();
	
	/**
	 * Returns the writers that map to EVR levels.
	 * 
	 * @return Map of EVR level string to Writer 
	 */
	public Map<String,PrintWriter> getLevelMap();
}
