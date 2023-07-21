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
import java.io.IOException;

import jpl.gds.product.api.file.IProductFilename;
import jpl.gds.product.api.file.ProductFilenameException;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Ported from MPCS4MSL, added javadoc
 *
 * 9/26/2016 - Converted / created a product file name implementation for reference.  This is a very basic object now that is
 * used by the product builder to create a product file name and reserve the metadata and data file objects.  All of the other functionality
 * that lived in these classes has been moved to the ProductFileNameBuilder implementations.
 */
public class ReferenceProductFilename implements IProductFilename {
	/** tracer */
    protected static final Tracer logger         = TraceManager.getTracer(Loggers.TLM_PRODUCT);

	private static final String				PARTIAL_STRING					= "_Partial";

	/**
	 * indicates reservation
	 */
	protected Boolean reserved;
	/** reserved datafile */
	protected File reservedDataFile;

	/**
	 * reserved metadata file.
	 */
	protected File reservedMetadataFile;

	/** partial flag */
	protected final boolean isPartial;
	/** product directory path */
	protected final String productPath;
	/** product name */
	protected final String productName;

	/** Major product version number */
	protected int majorVersion;
	/** Minor product version number */
	protected int minorVersion;

	/**
	 * Constructor for use when creating a new product with the product builder.  When
	 * using this constructor it is required that reserve is called so that the data file
	 * and metadata file paths can be constructed, versioned and reserved on the file system.
	 *
	 * @param isPartial true if product is partial
	 * @param productPath path to product
	 * @param productName name of product
	 */
	public ReferenceProductFilename(final boolean isPartial, final String productPath, final String productName) {
		super();
		this.isPartial = isPartial;
		this.productPath = productPath;
		this.productName = productName;

		reserved = false;
		reservedDataFile = null;
		reservedMetadataFile = null;
		majorVersion = -1;
		minorVersion = -1;
	}

	/**
	 * This constructor should be used as part of PDPP to create PFN objects from
	 * existing products using their product paths.  Also, this is not a private
	 * constructor so all values will be set as part of the builder before this is even invoked.
	 *
	 * @param isPartial if the product is partial
	 * @param productPath path to the product
	 * @param productName the name of the product, without version or extension
	 * @param originalProductPathNoExtention full path of the product without extenion
	 */
	public ReferenceProductFilename(final boolean isPartial, final String productPath, final String productName, final String originalProductPathNoExtention) {
		this(isPartial, productPath, productName);

		/**
		 * Must set these since we should never use this constructor in the product builder.  This constructor should be used as part of PDPP
		 * to create PFN objects from existing products using their product paths.  Also, this is not a private constructor so all values
		 * will be set as part of the builder before this is even invoked.
		 */
		if (originalProductPathNoExtention != null) {
			this.reservedDataFile = new File(originalProductPathNoExtention + getAppropriateFilenameSuffix(false));
			this.reservedMetadataFile = new File(originalProductPathNoExtention + getAppropriateFilenameSuffix(true));
		}
	}

	/**
	 * Reserves the data and metadata files
	 * @return this
	 * @throws ProductFilenameException if there is an error reserving the filenames.
	 */
	@Override
	public IProductFilename reserve() throws ProductFilenameException {
		if (!reserved) {
			synchronized (reserved) {
				if (!reserved) {
					// Find the next version string using a product file name filter.
					final ProductFilenameFileFilter filter = new ProductFilenameFileFilter(isPartial(), productName, productPath);

					try {
						/**
						 * We do a loop here in case the product was created while we are working.  We know the filter
						 * will get the highest version when it is created, so we keep incrementing if there is a collision until
						 * we are successful.
						 */
						do {
							// This will increment the version before computing it.  If we can not reserve the file it will be null.
							reservedDataFile = reserveProductFile(filter.getVersion(true));
						} while (reservedDataFile == null);

						// We got no errors so build the metadata file with the same version.  Don't increment the version in the filter.
						reservedMetadataFile = new File(buildProductMetadataFilename(filter.getVersion(false)));

						// Set the local version parameters.
						majorVersion = filter.getCompleteProductVersion();
						minorVersion = filter.getPartialProductVersion();

						if (!reservedMetadataFile.createNewFile()) {
							throw new ProductFilenameException("Cannot reserve Metadata File Pair for: " + FileUtility.createFilePathLogMessage(reservedMetadataFile));
						}
						reserved = true;
					}
					catch (final ProductFilenameException e) {
						throw e;
					}
					catch (final IOException e) {
						throw new ProductFilenameException(e.getLocalizedMessage(), e);
					}
				}
			}
		}
		return this;
	}

	/**
	 * Reserves data file name
	 * @param version product version string
	 * @return File object
	 * @throws IOException if there is an I/O problem reserving the file
	 */
	protected File reserveProductFile(final String version) throws IOException {
		File reservedFile = null;

		reservedFile = new File(buildProductDataFilename(version));

		if (!reservedFile.getParentFile().exists()) {
			reservedFile.getParentFile().mkdirs();
		}

		// Attempt to create.  If it fails, return null else the reserved file.
		return reservedFile.createNewFile() ? reservedFile : null;
	}

	/**
	 * Builds the product data filename
	 * @param version the product version string
	 * @return the product filename
	 */
	public String buildProductDataFilename(final String version) {
		return buildProductFilename(false, version);
	}

	/**
	 * Builds the product metadata filename
	 * @param version the product version string
	 * @return the product metadata filename
	 */
	public String buildProductMetadataFilename(final String version) {
		return buildProductFilename(true, version);
	}

	/**
	 * Builds the product filename using productPath, productName and getting the
	 * version with the product filename filter.
	 *
	 * @param isMetadata true if this is the metadata file
	 * @param version the product version string
	 * @return the product filename
	 */
	public String buildProductFilename(final boolean isMetadata, final String version) {
		// Add the path first.
		final StringBuilder s = new StringBuilder(productPath);
		s.append(File.separatorChar);

		// Add the product name next.
		s.append(productName);

		// If it is partial, add the partial indicator.
		if (isPartial()) {
			s.append(PARTIAL_STRING);
		}

		// Add the version string.
		s.append('-').append(version);

		// Get the appropriate suffix.
		s.append(getAppropriateFilenameSuffix(isMetadata));
		return s.toString();
	}

	/**
	 * Returns the appropriate filename suffix
	 * 09/05/2012 -  Data Compression File Extensions added.
	 * @param isMetadata indicates metadata
	 * @return the appropriate filename suffix
	 */
	public String getAppropriateFilenameSuffix(final boolean isMetadata) {
		if (isMetadata) {
			return getAppropriateMetadataFilenameSuffix();
		}
		else {
			return getAppropriateDataFilenameSuffix();
		}
	}

	@Override
	public boolean isReserved() {
		return reserved;
	}

	@Override
	public boolean isPartial() {
		return isPartial;
	}

	@Override
	public String getDataFilePath() {
		return reservedDataFile == null ? null : reservedDataFile.getAbsolutePath();
	}

	@Override
	public String getMetadataFilePath() {
		return reservedMetadataFile == null ? null : reservedMetadataFile.getAbsolutePath();
	}

	@Override
	public String getProductPath() {
		return productPath;
	}

	@Override
	public String getProductName() {
		return productName;
	}

	@Override
	public int getMajorVersion() {
		return majorVersion;
	}

	@Override
	public int getMinorVersion() {
		return minorVersion;
	}
}
