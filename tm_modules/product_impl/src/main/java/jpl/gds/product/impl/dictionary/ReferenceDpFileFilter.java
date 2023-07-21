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
package jpl.gds.product.impl.dictionary;

import java.io.File;
import java.io.FileFilter;

import jpl.gds.shared.formatting.SprintfFormat;

/**
 * ReferenceDpFileFilter is a file filter specifically for listing product dictionary files
 * for the REFERENCE project.
 *
 *
 */
public class ReferenceDpFileFilter implements FileFilter {
	  private File latestFile;
	    private char highestChar = (char) 0;
	    
	    /**
	     * The prefix string that applies to all DP XML filenames.
	     */
	    protected String prefix;
	    /**
	     * The length of the prefix string.
	     */
	    protected int prefixLength;

	    /**
	     * Creates a CommonDpFileFilter.
	     * 
	     * @param key the product key for the definitions we want to filter for
	     */
	    public ReferenceDpFileFilter(final ReferenceProductDefinitionKey key) {
	        init(key.getApid(), String.valueOf(key.getVersion()));
	    }
	    
	    /**
	     * Creates an instance of GenericDpFileFilter.
	     * @param apid the application process ID of the product
	     * @param version the version of the product
	     */
	    public ReferenceDpFileFilter(final int apid, final String version) {
	        init(apid, version);
	    }
	    
	    /**
	     * Initializes the file prefix and prefix length for this filter.
	     * @param apid the product APID
	     * @param version the product version
	     */
		protected void init(final int apid, final String version) {
	        final Object[] args = new Object[2];

	        args[0] = Integer.valueOf( apid );
	        args[1] = Integer.valueOf(version);
        final SprintfFormat formatter = new SprintfFormat();
        prefix = formatter.sprintf("dp_%d_v%d", args);
	        prefixLength = prefix.length();
	    }

	    /**
	     * {@inheritDoc}
	     * @see java.io.FileFilter#accept(java.io.File)
	     */
		@Override
		public boolean accept(final File file) {
	        final String name = file.getName();

	        if (!name.startsWith(prefix)) {
	            return false;
	        }
	        if (!name.endsWith(".xml")) {
	            return false;
	        }
	        if (name.length() > (prefixLength + 5)) {
	            return false;
	        }
	        
	        if (latestFile == null) {
	            latestFile = file;
	        }

	        if (name.length() == (prefixLength + 5)) {
	            final char c = name.charAt(prefixLength);
	            if (!Character.isLetter(c)) {
	                return false;
	            }
	            if (c > highestChar) {
	                highestChar = c;
	                latestFile = file;
	            }
	            return true;
	        }

	        return true;
	    }

	    /**
	     * Retrieves the File object for the latest version of the product
	     * dictionary file that matches this filter. Works only after a call to accept().
	     * 
	     * @return the File object for the dictionary file, or null if no such file is found
	     */
		public File getLatestFile() {
	        return latestFile;
	    }
}
