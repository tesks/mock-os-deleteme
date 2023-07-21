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
package jpl.gds.product.utilities.file;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpl.gds.shared.file.ISharedFileLock;

/**
 * The was split out of the ProductFilename class.  It it used as a file filter and will find the highest complete and partial
 * version of existing products in an output directory.  From that it will create the version string for the product based
 * on the highest complete and partial version numbers found and the partial status of the product being created.
 *
 *
 */
public class ProductFilenameFileFilter implements FilenameFilter {
	private static final Pattern VERSION_NUMBER_PATTERN	= Pattern.compile("-(\\d+)\\.(\\d*)\\.*");

	/** next complete product version */
	protected int nextCompleteProductVersion;
	/** next partial product version */
	protected int nextPartialProductVersion;
	/** highest complete product version */
	protected int highestCompleteProductVersion;

    /** Flag indicating file is partial */
	protected final boolean isPartial;
	/** Product name portion of path */
	protected final String productName;
	/** Path to search for files */
	protected final File searchPath;


	/**
	 * @param isPartial if the product is partial
	 * @param productName the base product file name
	 * @param searchPath the output search path
	 */
	public ProductFilenameFileFilter(final boolean isPartial, final String productName, final String searchPath) {
		this(isPartial, productName, new File(searchPath));
	}

	/**
	 * @param isPartial if the product is partial
	 * @param productName the base product file name
	 * @param searchPath the output search path
	 */
	public ProductFilenameFileFilter(final boolean isPartial, final String productName, final File searchPath) {
		super();
		this.isPartial = isPartial;
		this.searchPath = searchPath;

		this.productName = productName;

		init();
	}

	private void init() {
		// This will cause this to be used as the file name filter, and will set up the
		// highest and next versions parameters.  We don't actually care about the returned
		// file list.
		searchPath.list(this);
	}

	/**
	 * Builds the version number.  If it is complete will be an integer, like 1 or 2.  If it is
	 * partial, will be a float, like 1.1, 1.2.
	 *
	 * @param incrementVersion if true will increment the version before returning the result.
	 * @return versions string.
	 */
	public String getVersion(final boolean incrementVersion) {
		if (incrementVersion) {
			incVersion();
		}

		final StringBuilder s = new StringBuilder().append(nextCompleteProductVersion);

		if (isPartial) {
			s.append('.').append(nextPartialProductVersion);
		}

		return s.toString();
	}

	/**
	 * Returns the complete product version
	 * @return the completeProductVersion
	 */
	public int getCompleteProductVersion() {
		return nextCompleteProductVersion;
	}

	/**
	 * Returns the partial product version
	 * @return the partialProductVersion
	 */
	public int getPartialProductVersion() {
		return isPartial ? nextPartialProductVersion : 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean accept(final File dir, final String name) {
		/**
		 * Must filter out the lock files.
		 */
		final boolean accept = name.contains(productName) && !name.endsWith(ISharedFileLock.LOCK_FILE_EXTENSION);

		if (accept) {
			final Matcher m = VERSION_NUMBER_PATTERN.matcher(name);
			m.find();

			try {
				/*
				 * Check for existence of a PARTIAL file
				 */
				nextPartialProductVersion = Math.max(nextPartialProductVersion, Integer.valueOf(name.substring(m.start(2), m.end(2))));
			}
			catch (final Exception e) {
				/*
				 * Partial file does not exist.
				 * Check for existence of COMPLETE file.
				 */
				try {
					nextCompleteProductVersion = Math.max(nextCompleteProductVersion, Integer.valueOf(name.substring(m.start(1), m.end(1))));

					/**
					 * 6/26/2013 - Keeping track of the highest version number we come
					 * across.  This will tell use when we are incrementing the complete version if the last
					 * complete product that was found was complete.  If it was not, then we need to use the
					 * current version number for the partial.
					 */
					if (nextCompleteProductVersion > highestCompleteProductVersion) {
						highestCompleteProductVersion = nextCompleteProductVersion;
					}
				}
				catch (final Exception e1) {
					// ignore
				}
			}
		}
		return accept;
	}
	
	/**
	 * Increments the product version number.
	 */
	public void incVersion() {
		if (isPartial) {
			/**
			 * The issue is we need to increment the partial number if there is NOT a complete for a version.  But the
			 * the problem is the logic sets the completeProductExists flag if ANY complete is found, and then if
			 * other versions are created, it always increments the complete version and sets the partial to 1.
			 */

			/**
			 * 6/26/2013 - Changed the check for a complete version increment.  To see if
			 * the highest complete number equals the next number, IE, is the last version complete or was the
			 * last product partial, and we need to increment another partial.
			 */
			if (highestCompleteProductVersion == nextCompleteProductVersion) {
				nextCompleteProductVersion++;
				nextPartialProductVersion = 1;
			}
			else {
				/**
				 * 6/26/2013 - If this product is the first, partial or complete, we need
				 * to set the version to 1 but the default is 0.  So we increment it in that case.  Otherwise, we
				 * leave it as the most current version number.
				 */
				if (nextCompleteProductVersion == 0) {
					nextCompleteProductVersion++;
				}
				nextPartialProductVersion++;
			}
		}
		else {
			nextCompleteProductVersion++;
		}
	}
}
