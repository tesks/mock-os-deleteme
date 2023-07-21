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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

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
 * @since AMPCS R8
 */
public class SharedFileLock implements ISharedFileLock {
	
	/**
	 * The File object for the lock file.
	 */
    protected final File file;

    private RandomAccessFile raf;
    private FileLock fileLock;

    /**
     * 
     * Constructs a new SharedFileLock object based on the provided path of the
     * lock file.
     * 
     * @param filename
     *            directory and filename of the lock file
     */
    public SharedFileLock(final String filename) {
        file = new File(filename);
        String directory = file.getParent();
        if (null != directory) {
            new File(directory).mkdirs();
        }
    }

    /**
     * 
     * Constructs a new SharedFileLock object based on the provided path of the
     * lock file.
     * 
     * @param directory
     *            directory of the lock file
     * @param fileName
     *            the name of the lock file
     */
    public SharedFileLock(final String directory, final String fileName) {
        if ((null == directory) || (null == fileName)) {
            throw new IllegalArgumentException("Cannot accept null lock file directory or null lock filename");
        }
        new File(directory).mkdirs();
        file = new File(directory, fileName);
    }

    /**
     * Constructs a new SharedFileLock object based on the unique identity of
     * the product. The directory in which the product lock file should be
     * created must also be specified.
     * 
     * @param directory
     *            the directory path to which the lock file should be created
     * @param fileName
     *            the name of the lock file to create
     */
    public SharedFileLock(final File directory, final String fileName) {
        if ((null == directory) || (null == fileName)) {
            throw new IllegalArgumentException("Cannot accept null lock file directory or null lock filename");
        }
        directory.mkdirs();
        file = new File(directory, fileName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.shared.file.ISharedFileLock#lock()
     */
    @Override
    public boolean lock() throws IOException {
        try {
            raf = new RandomAccessFile(file, "rws");
            FileChannel channel = raf.getChannel();
            fileLock = channel.tryLock();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    
        // return true if locking was successful, false if lock not obtained
        return fileLock != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.shared.file.ISharedFileLock#release()
     */
    @Override
    public void release() throws IOException {
        release(false);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.shared.file.ISharedFileLock#release()
     */
    @Override
    public void release(final boolean deleteOnRelease) throws IOException {
        // Release the lock
        if (fileLock != null && fileLock.isValid()) {
            fileLock.release();
        }

        // Closing the RandomAccessFile also closes its FileChannel
        if (raf != null) {
            raf.close();
        }

        // Optionally delete the lock file
        if (deleteOnRelease && file.exists() && !file.delete()) {
            throw new IOException("Lock file " + getLockFilePath() + " deletion returned false");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.shared.file.ISharedFileLock#getLockFilePath()
     */
    @Override
    public String getLockFilePath() {
        return file.getAbsolutePath();
    }
}
