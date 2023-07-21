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

import java.io.IOException;

/**
 * An interface for file lock classes.
 * 
 *
 */
public interface ISharedFileLock {
    /**
     * Default extension for lock files
     */
    static final String LOCK_FILE_EXTENSION = ".lock";

    /**
     * Locks the product lock file. The method will attempt to create the lock
     * file if it does not exist. The locking mechanism uses
     * java.nio.channels.FileChannel.tryLock().
     * 
     * @return true if successfully locked, false if locking was unsuccessful
     * @throws IOException
     *             thrown if there is an error creating the product lock file or
     *             any other I/O error occurs
     */
    boolean lock() throws IOException;

    /**
     * Releases the product lock.
     * 
     * @throws IOException
     *             thrown when I/O error occurs while release the SharedFileLock
     *             or closing the RandomAccessFile object of the lock file
     */
    void release() throws IOException;

    /**
     * Releases the product lock, and optioally deletes the lock file
     * 
     * @param deleteOnRelease
     * 
     * @throws IOException
     *             thrown when I/O error occurs while release the SharedFileLock
     *             or closing the RandomAccessFile object of the lock file
     */
    void release(boolean deleteOnRelease) throws IOException;

    /**
     * Returns the absolute path of the lock file (which may or may not have
     * been created yet).
     * 
     * @return absolute path of the product lock file
     */
    String getLockFilePath();
}