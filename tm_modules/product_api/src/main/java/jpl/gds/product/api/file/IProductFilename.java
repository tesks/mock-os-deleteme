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
package jpl.gds.product.api.file;

import java.io.File;

import jpl.gds.product.api.builder.ProductStorageConstants;

/**
 *
 * Product file name interface.
 *
 *
 */
public interface IProductFilename {

	/**
	 * @return the major version otherwise known as the completed version.
	 */
	public int getMajorVersion();

	/**
	 * @return the minor version, otherwise known as the partial version.
	 */
	public int getMinorVersion();

	/**
	 * Reserves the dat and emd files if they do not already exist.  When this method returns,
	 * both the dat and emd file will exist on the file system.
	 *
	 * @return the reserved product file name.
	 *
	 * @throws ProductFilenameException if there is a problem reserving the file name
	 */
	public IProductFilename reserve() throws ProductFilenameException;

	/**
	 *
	 * @return the full path to the data file including file name and extension.
	 */
	public String getDataFilePath();

	/**
	 * @return the full path to the emd file including file name and extension.
	 */
	public String getMetadataFilePath();

	/**
	 * @return the product path without product name.
	 */
	public String getProductPath();

	/**
	 * @return the product file name without path or extension
	 */
	public String getProductName();

	/**
	 * @return if the product is partial.
	 */
	public boolean isPartial();

	/**
	 * @return true if this product file has been reserved.
	 */
	public boolean isReserved();

	/**
	 * Return the product version string
	 * @param zeroPad true of the version should be padded with zeros to 5 digits
	 * @return the product version string
	 */
	public default String getProductVersionString(final boolean zeroPad) {
		final StringBuilder s = new StringBuilder(Integer.toString(getMajorVersion()));
		if (zeroPad || isPartial()) {
			s.append('.');
			s.append(getMinorVersion());
			if (zeroPad) {
				while (s.length() < 5) {
					s.append('0');
				}
			}
		}
		return s.toString();
	}

	/**
	 * Return the reserved data file.  This will throw if this product has not been reserved.  This should
	 * only be called by the product builder or PDPP when creating a new product file.  If you just want
	 * to get the full path of the product, use getDataFilePath.

	 * @return the reserved data file
	 * @throws ProductFilenameException if there is a problem getting the reserved data file object
	 */
	public default File getReservedDataFile() throws ProductFilenameException {
		if (isReserved()) {
			return new File(getDataFilePath());
		} else {
			throw new ProductFilenameException("Attempted to retrieve Product Data File before reserving it.");
		}
	}

	/**
	 * Return the metadata file.  This will throw if this product has not been reserved.  This should
	 * only be called by the product builder or PDPP when creating a new product file.  If you just want
	 * to get the full path of the product, use getMetadataFilePath.
	 * @return the reserved metadata file
	 * @throws ProductFilenameException if there is a problem getting the reserved data file object
	 */
	public default File getReservedMetadataFile() throws ProductFilenameException {
		if (isReserved()) {
			return new File(getMetadataFilePath());
		} else {
			throw new ProductFilenameException("Attempted to retrieve Product Metadata File before reserving it.");
		}
	}

	/**
	 * Uses getReservedDataFile and getReservedMetadataFile to get the reserved files and deletes them.
	 *
	 * @throws ProductFilenameException Attempting to delete unreserved file or an unknown exception.
	 */
	public default void delete() throws ProductFilenameException {
		// Rely on the individual methods to throw if they are not reserved.
		try {
			getReservedDataFile().delete();
			getReservedMetadataFile().delete();
		} catch (final ProductFilenameException e) {
			throw new ProductFilenameException("Attempted to delete Product Files before reserving them.");
		} catch (final Exception e) {
			throw new ProductFilenameException("Unknown excpetion attempting to delete product file.", e);
		}
	}

	/**
	 * Uses the ProductStorageConstants partial and complete data suffix.
	 *
	 * @return data file suffix based on partial status.
	 */
	public default String getAppropriateDataFilenameSuffix() {
		return isPartial() ? ProductStorageConstants.PARTIAL_DATA_SUFFIX : ProductStorageConstants.DATA_SUFFIX;
	}

	/**
	 * Uses the ProductStorageConstants partial and complete metadata suffix.
	 *
	 * @return data file suffix based on partial status.
	 */
	public default String getAppropriateMetadataFilenameSuffix() {
		return isPartial() ? ProductStorageConstants.PARTIAL_METADATA_SUFFIX : ProductStorageConstants.METADATA_SUFFIX;
	}
}
