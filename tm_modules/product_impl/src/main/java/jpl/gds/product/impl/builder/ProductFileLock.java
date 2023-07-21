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

package jpl.gds.product.impl.builder;

import java.io.File;
import java.io.IOException;

import jpl.gds.shared.file.SharedFileLock;

/**
 * This class defines a product lock file. A product lock file must have a name
 * unique to the data product it references: some combination of the product
 * transaction ID and VCID. It is also recommended that the lock be placed into
 * the permanent output directory for the specific data product it references.
 * If the lock is stored in the product builder temporary sub-directory for the
 * product itself, it will be removed when a complete product is written,
 * opening the way for a locking mistake at that exact moment in time.
 * 
 * An alternate constructor takes in an arbitrary filename. The lock file will
 * be created purely based on that.
 * 
 * @since AMPCS R5
 */
public class ProductFileLock extends SharedFileLock {
	private final boolean productLockFileDeleteOnRelease;
    /**
     * Constructs a new ProductFileLock object based on the unique identity of
     * the product. The directory in which the product lock file should be
     * created must also be specified.
     * 
     * @param vcid
     *            VCID of the product
     * @param transactionId
     *            transaction identification string of the product
     * @param directory
     *            the directory path to which the lock file should be created
     */
    public ProductFileLock(final int vcid, final String transactionId,
            final File directory) /* throws ProductStorageException */ {
    	this(vcid, transactionId, directory, true);

    }
    /**
     * Constructs a new ProductFileLock object based on the unique identity of
     * the product. The directory in which the product lock file should be
     * created must also be specified.
     * 
     * @param vcid
     *            VCID of the product
     * @param transactionId
     *            transaction identification string of the product
     * @param directory
     *            the directory path to which the lock file should be created
     * @param productLockFileDeleteOnRelease indicates if the file should be 
     *        deleted upon lock release
     */
    public ProductFileLock(final int vcid, final String transactionId,
            final File directory, final boolean productLockFileDeleteOnRelease) {
        super(directory, getLockFilename(vcid, transactionId));
        
        this.productLockFileDeleteOnRelease = productLockFileDeleteOnRelease;
    }

    /**
     * 
     * Constructs a new ProductFileLock object based on the provided path of the
     * lock file.
     * 
     * @param filename
     *            directory and filename of the lock file
     */
    public ProductFileLock(final String filename) {
    	this(filename, true);
    }
    /**
     * 
     * Constructs a new ProductFileLock object based on the provided path of the
     * lock file.
     * 
     * @param filename
     *            directory and filename of the lock file
     * @param productLockFileDeleteOnRelease indicates if the file should be 
     *        deleted upon lock release
     */
    public ProductFileLock(final String filename, final boolean productLockFileDeleteOnRelease) {
        super(filename);
        
        this.productLockFileDeleteOnRelease = productLockFileDeleteOnRelease;
    }

    /**
     * Creates a file name from VCID and Transaction ID.
     * 
     * @param vcid
     *            the
     * @param transactionId
     *            the transaction ID
     * @return a constructed filename
     */
    private static String getLockFilename(final int vcid, final String transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Cannot accept null transactionId");
        }

        /*
         * Somewhat based on ReceivedPartsTracker class's getKey(...) method.
         */
        final StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        sb.append(vcid);
        sb.append("_");
        sb.append(transactionId);
        sb.append(LOCK_FILE_EXTENSION);
        return sb.toString().replace('/', '_');
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() throws IOException {
        super.release(productLockFileDeleteOnRelease);
    }
}
