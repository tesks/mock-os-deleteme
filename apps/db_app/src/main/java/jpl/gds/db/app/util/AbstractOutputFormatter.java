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
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.types.IDbChannelSampleProvider;
import jpl.gds.db.api.types.IDbCommandProvider;
import jpl.gds.db.api.types.IDbEvrProvider;
import jpl.gds.db.api.types.IDbLog1553Provider;
import jpl.gds.db.api.types.IDbLogProvider;
import jpl.gds.db.api.types.IDbProductMetadataProvider;
import jpl.gds.db.api.types.IDbQueryable;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.types.Pair;

/**
 * This class provides a base implementation of the OutputFormatter interface
 * used to write information from GetEverythingApp.  
 *
 */
public abstract class AbstractOutputFormatter implements OutputFormatter {
    /**
     * The Spring Application Context
     */
	protected final ApplicationContext appContext;

    /**
     * The formatter to use when formatting
     */
	protected SprintfFormat formatUtil;
	
	/**
     * @param appContext
     *            the Spring Application Context
     */
	public AbstractOutputFormatter(final ApplicationContext appContext) {
		super();
		this.appContext = appContext;
		this.formatUtil = appContext.getBean(SprintfFormat.class);
	}

	/**
	 * Map that holds the data to be written for a IDbQueryable object
	 */
	protected Map<String,String> dqMap;	
	
	/**
	 * Print writers for each of the files
	 */

    /** PrintWriter for EVRs */
	protected PrintWriter pwEvr;

    /** PrintWriter for EHA */
	protected PrintWriter pwEha;

    /** PrintWriter for commands */
	protected PrintWriter pwCmd;

    /** PrintWriter for logs */
	protected PrintWriter pwLog;

    /** PrintWriter for products */
	protected PrintWriter pwProd;

    /** Maop of levels to PrintWriters */
	protected Map<String,PrintWriter> pwEvrLevelMap;

    /** PrintWriter for sorted */
	protected PrintWriter pwSorted;

    /** PrintWriter for 1553 */
	protected PrintWriter pw1553Log;

	
    /**
     * Returns a formatted string representing one line of the output text file.
     *   
     * @param widthMap is a mapping from header column name to a data/data width pair;
     *        key is the column name, and the attached pair is the data value
     *        and desired width of the column
     * @param isHeader should be true if printing a header, otherwise false
     * 
     * @return formatted string, which may be a header or data line
     */
    protected String getTextLine(final Map<String,Pair<String,Integer>> widthMap, final boolean isHeader) {
    	
		final StringBuilder sb = new StringBuilder();
		
		if (widthMap != null) {
			
    		for (final Entry<String,Pair<String,Integer>> entry : widthMap.entrySet()) {
    			final String data = isHeader ? entry.getKey() : entry.getValue().getOne();
    			sb.append(formatUtil.anCsprintf("%-" + entry.getValue().getTwo() + "s\t",data));
    		}
    		sb.append("\n");
    		
    		return sb.toString();
		}
    	
		return null;
    }
    
	 /**
     * Returns a CSV string that consists of all the keys in the given map (if a header) or
     * each string values in the map if not a header.
     * 
     * @param csvMap is a map from header column name to the associated data value
     * @param isHeader should be true if printing a header, otherwise false
     * @param isSessionReport is true if printing a session report format csv file, which
     * does not require double quotes to be added around the values on the CSV line
     * 
     * @return formatted CSV string
     */
    protected String getCsvLine(final Map<String,String> csvMap, final boolean isHeader, final boolean isSessionReport) {
    	
    	// Session report format uses , as a delimiter.  Other csv formats use "," as a delimiter.
    	
		final StringBuilder sb = new StringBuilder();
						
		if (csvMap != null) {
			
			int k = 1;
    		final int size = csvMap.size();
    		
    		if (!isSessionReport)
    			sb.append("\"");

    		for (final Entry<String,String> entry : csvMap.entrySet()) {
        		
    			final String data = isHeader ? entry.getKey() : entry.getValue();
    			sb.append(data);
    			if (k == size) {
    				if (isSessionReport) {
                		sb.append("\n");
    				}
    				else {
                		sb.append("\"\n");
    				}
    			}
    			else {
       				if (isSessionReport) {
        				sb.append(","); 
    				}
    				else {
        				sb.append("\",\""); 
        			}
    			}	
    			k++;       				
    		}
    		
    		return sb.toString();
		}
    	
		return null;
    }
    
 
    
	/**
     * {@inheritDoc}
     *
	 * @see jpl.gds.db.app.util.OutputFormatter#closeSingleFiles()
	 */
    @Override
	public void closeSingleFiles() {
		
        // Close all of the evr level print writers:
		if (pwEvrLevelMap != null) {
	        for (final Entry<String, PrintWriter> entry: pwEvrLevelMap.entrySet()) {
	        	entry.getValue().close();
	        }
		}
        
        // Close and flush all the other print writers:
        if (pwEha != null) {
        	pwEha.flush();
        	pwEha.close();
        	pwEha = null;
        }
        if (pwEvr != null) {
        	pwEvr.flush();
        	pwEvr.close();
        	pwEvr = null;
        }
        if (pwProd != null) {
        	pwProd.flush();
        	pwProd.close();
        	pwProd = null;
        }
        if (pwCmd != null) {
        	pwCmd.flush();
        	pwCmd.close();
        	pwCmd = null;
        }
        if (pwLog != null) {
        	pwLog.flush();
        	pwLog.close();
        	pwLog = null;
        }
        if (pw1553Log != null) {
        	pw1553Log.flush();
        	pw1553Log.close();
        	pw1553Log = null;
        }
	}
	
	/**
     * {@inheritDoc}
     *
	 * @see jpl.gds.db.app.util.OutputFormatter#closeSortedFile()
	 */
    @Override
	public void closeSortedFile() {
		
		if (pwSorted != null) {
			pwSorted.close();
		}
	}
	
	/**
     * {@inheritDoc}
     *
	 * @see jpl.gds.db.app.util.OutputFormatter#setUpEL(java.lang.String, java.lang.String)
	 */
    @Override
	public void setUpEL(final String level, final String filename) throws FileNotFoundException {
		
		pwEvrLevelMap.put(level, new PrintWriter(filename));
	}
	
	/**
     * {@inheritDoc}
     *
	 * @see jpl.gds.db.app.util.OutputFormatter#setUp(jpl.gds.db.api.types.IDbQueryable, java.lang.String)
	 */
    @Override
	public void setUp(final IDbQueryable dq, final String filename) throws FileNotFoundException {
		
		pwEvrLevelMap = new HashMap<String,PrintWriter>();

        if (dq instanceof IDbChannelSampleProvider) {
			pwEha = new PrintWriter(filename);
		}
        else if (dq instanceof IDbEvrProvider) {
			pwEvr = new PrintWriter(filename);
		}
        else if (dq instanceof IDbCommandProvider) {
			pwCmd = new PrintWriter(filename);
		}
        else if (dq instanceof IDbProductMetadataProvider) {
			pwProd = new PrintWriter(filename);
		}
        else if (dq instanceof IDbLogProvider) {
			pwLog = new PrintWriter(filename);
		}
        else if (dq instanceof IDbLog1553Provider) {
			pw1553Log = new PrintWriter(filename);
		}
	}

	/**
     * {@inheritDoc}
     *
	 * @see jpl.gds.db.app.util.OutputFormatter#setUpSorted(java.lang.String)
	 */
    @Override
	public void setUpSorted(final String filename) throws FileNotFoundException {
		
		setUpDataMapSorted();
		pwSorted = new PrintWriter(filename);
	}
	
	/**
	 * Sets up the data map for the sorted file.
	 */
	public abstract void setUpDataMapSorted();
	
	/**
     * {@inheritDoc}
     *
	 * @see jpl.gds.db.app.util.OutputFormatter#getLevelMap()
	 */
    @Override
	public Map<String,PrintWriter> getLevelMap() {
		return pwEvrLevelMap;
	}
    
}
