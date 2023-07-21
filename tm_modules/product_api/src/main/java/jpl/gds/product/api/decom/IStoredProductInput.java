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
package jpl.gds.product.api.decom;

import java.io.File;
import java.io.IOException;

import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.builder.ProductStorageConstants;
import jpl.gds.shared.message.IMessagePublicationBus;

/**
 * An interface to be implemented by stored product input classes for
 * product decom.
 * 
 *
 */
public interface IStoredProductInput {

    /**
     * Sets the message context for use in message publication.
     * @param messageContext the MessageContext to set
     */
    void setMessageContext(IMessagePublicationBus messageContext);

    /**
     * Reads a product and its metadata from product storage.
     * @param filename the full path name to either the product data or product 
     * metadata file.
     * @throws IOException if there is a I/O problem reading the data product files
     * @throws ProductException if there is a problem parsing or interpreting the data product files
     */
    void read(String filename) throws IOException, ProductException;
    
    /**
     * Given either the product metadata file path, or the product data path,
     * returns an array of two file paths, the first of which is the product 
     * data file, and the second of which is the product metadata file.
     * @param filename the full path name to either the product data or product 
     * metadata file
     * @return an array of two Strings: the data file path and the metadata file path
     * @throws ProductException if a matching set of data and metadata files cannot be found
     */
    public static String[] getFilenames(final String filename) throws ProductException {
        final String[] filenames = new String[2];

        if (filename.endsWith(ProductStorageConstants.DATA_SUFFIX)) {
            if (filename.equals(ProductStorageConstants.DATA_SUFFIX)) {
                throw new ProductException("Invalid product file name: "
                                           + filename);
            }
            final int suffixLen = ProductStorageConstants.DATA_SUFFIX.length();
            filenames[0] = filename;
            filenames[1] = filename.substring(0, filename.length() - suffixLen)
                         + ProductStorageConstants.METADATA_SUFFIX;
            return filenames;
        }

        if (filename.endsWith(ProductStorageConstants.METADATA_SUFFIX)) {
            if (filename.equals(ProductStorageConstants.METADATA_SUFFIX)) {
                throw new ProductException("Invalid product file name: "
                                           + filename);
            }
            final int suffixLen = ProductStorageConstants.METADATA_SUFFIX.length();
            filenames[0] = filename.substring(0, filename.length() - suffixLen)
                         + ProductStorageConstants.DATA_SUFFIX;
            filenames[1] = filename;
            return filenames;
        }

        if (filename.endsWith(ProductStorageConstants.PARTIAL_DATA_SUFFIX)) {
            if (filename.equals(ProductStorageConstants.PARTIAL_DATA_SUFFIX)) {
                throw new ProductException("Invalid product file name: "
                                           + filename);
            }
            final int suffixLen = ProductStorageConstants.PARTIAL_DATA_SUFFIX.length();
            filenames[0] = filename;
            filenames[1] = filename.substring(0, filename.length() - suffixLen)
                         + ProductStorageConstants.PARTIAL_METADATA_SUFFIX;
            return filenames;
        }

        if (filename.endsWith(ProductStorageConstants.PARTIAL_METADATA_SUFFIX)) {
            if (filename.equals(ProductStorageConstants.PARTIAL_DATA_SUFFIX)) {
                throw new ProductException("Invalid product file name: "
                                           + filename);
            }
            final int suffixLen = ProductStorageConstants.PARTIAL_METADATA_SUFFIX.length();
            filenames[0] = filename.substring(0, filename.length() - suffixLen)
                         + ProductStorageConstants.PARTIAL_DATA_SUFFIX;
            filenames[1] = filename;
            return filenames;
        }

        File f;
        f = new File(filename + ProductStorageConstants.DATA_SUFFIX);
        if (f.exists()) {
            filenames[0] = filename + ProductStorageConstants.DATA_SUFFIX;
            filenames[1] = filename + ProductStorageConstants.METADATA_SUFFIX;
            return filenames;
        }
        f = new File(filename + ProductStorageConstants.PARTIAL_DATA_SUFFIX);
        if (f.exists()) {
            filenames[0] = filename + ProductStorageConstants.PARTIAL_DATA_SUFFIX;
            filenames[1] = filename + ProductStorageConstants.PARTIAL_METADATA_SUFFIX;
            return filenames;
        }

        throw new ProductException("Could not use product file " + filename + ". It must have a data or meta data suffix.");
    }

}