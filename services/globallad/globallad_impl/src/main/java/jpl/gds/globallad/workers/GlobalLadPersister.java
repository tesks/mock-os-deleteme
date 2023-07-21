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
package jpl.gds.globallad.workers;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.AbstractGlobalLadData;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.container.IGlobalLadContainer;
import jpl.gds.globallad.data.container.search.query.BasicQuerySearchAlgorithm;
import jpl.gds.serialization.globallad.data.Proto3GlobalLadTransport;
import jpl.gds.shared.log.Tracer;

/**
 * Runnable to persist the global lad.  
 */
public class GlobalLadPersister implements Runnable {
	private static final int BYTES_IN_GIGA = 1024*1024*1024;
	private static final String INPROGRESS = ".inprogress";
	
	private IGlobalLadContainer persistTarget;
	private File outputDirectory;
	private int maxBackups;
	private double maxTotalSize;
	private String backupFileBaseName;
	
	private boolean hasCountRestraint;
	private boolean hasSizeRestraint;
	
	private boolean wasInitialized;
	
	private final Comparator<File> oldestFileComparator;
	private final Comparator<File> newestFileComparator;
	
	private File lastBackupFile;
	
	private GlobalLadProperties config;
	private Tracer log;

	/**
	 * @param persistTarget - The container that will be used create the backup files.
	 * @pram log the Tracer logger
	 */
	public GlobalLadPersister(final IGlobalLadContainer persistTarget, final GlobalLadProperties config, final Tracer log) {
		super();
		this.persistTarget = persistTarget;
		this.config = config;
		this.log = log;
		
		this.wasInitialized = false;
		
		this.oldestFileComparator = new Comparator<File>() {
			
		    /**
		     * {@inheritDoc}
		     */
		    @Override
            public int compare(final File f1, final File f2) {
		    		/**
		    		 * We want them to be in reverse order where the oldest is at the front so compare f2 to f1.
		    		 */
		    		return Long.compare(f1.lastModified(), f2.lastModified());
		    } 
		};
		
		this.newestFileComparator = new Comparator<File>() {
			
		    /**
		     * {@inheritDoc}
		     */
		    @Override
            public int compare(final File f1, final File f2) {
		    		return Long.compare(f2.lastModified(), f1.lastModified());
		    } 
		};
		
		this.lastBackupFile = null;
	}
	
	/**
	 * Initializes the persister with configured values.  If this is not called prior to running exceptions will be thrown.
	 * 
	 * @return - True if initialization was successful.
	 */
	public boolean init() {
		final File persistenceOutput = config.getPersistenceDirectory();
		
		if (persistenceOutput == null) {
			log.error("Global LAD configured to persist but the persistence output directory is null.");
			return false;
		} else if (!persistenceOutput.isDirectory() && !persistenceOutput.mkdirs()) {
			log.error("Global LAD failed to create the required backup directories for persistence.");
			return false;
		}

		this.outputDirectory = config.getPersistenceDirectory();
		this.backupFileBaseName = config.getPersistenceBackupBaseName();
		this.maxBackups = config.getPersistenceMaxNumberBackups();
		this.hasCountRestraint = this.maxBackups > 0;
		
		this.maxTotalSize = config.getPersistenceMaxSize() * BYTES_IN_GIGA;
		this.hasSizeRestraint = this.maxTotalSize > 0;
		
		this.wasInitialized = true;
		
		
		return true;
	}
	
	/**
	 * Removes backups if required.
	 */
	private void cleanBackups() {
		while (tooManyFiles() || overSize()) {
			final boolean wasDeleted = removeOldest();
			
			if (!wasDeleted) {
				log.warn("Tried to delete a backup file but the attempt was not successful.");
				break;
			}
		}
	}

	/**
	 * @return - If anything was removed.
	 */
	private boolean removeOldest() {
		final File oldest = getOldestBackup();
		
		if (oldest == null) {
			return false;
		} else {
			return oldest.delete();
		}
	}

	/**
	 * Returns a list of files in the backup directory that start with the configured backup base name.
	 * 
	 * @return - Array of all the backup files found in the configured output directory.
	 */
	public File[] getBackupFiles() {
		if (outputDirectory == null) {
			return new File[0];
		} else {
			return outputDirectory.listFiles(new FileFilter() {
				
				@Override
				public boolean accept(final File pathname) {
					return !pathname.equals(getLastBackupFile()) && 
						    pathname.getName().startsWith(backupFileBaseName) &&
						   !pathname.getName().endsWith(INPROGRESS);
				}
			});
		}
	}
	
	/**
	 * Returns the last backup file that was created.  If no backup was created yet returns null;
	 * 
	 * @return - The last backup file that was created. 
	 */
	public File getLastBackupFile() {
		return this.lastBackupFile;
	}
	
	/**
	 * Finds gets the newest backup in the output directory.  If there are no files in the output directory returns null.
	 * 
	 * @return - The most recent backup file in the configured output directory.
	 */
	public File getNewestBackup() {
		final File[] files = getBackupFiles();
		
		if (files == null || files.length > 0) {
			Arrays.sort(files, newestFileComparator);
			return files[0];
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the oldest backup file in the output directory.  If there are no files in the output directory returns null.
	 * 
	 * @return - The oldest backup file in the configured output directory.
	 */
	public File getOldestBackup() {
		final File[] files = getBackupFiles();
		
		if (files == null || files.length > 0) {
			Arrays.sort(files, this.oldestFileComparator);
			return files[0];
		} else {
			return null;
		}
	}
	
	private boolean overSize() {
		return hasSizeRestraint && getOutputDirectorySize() > maxTotalSize;
	}
	
	private boolean tooManyFiles() {
		return hasCountRestraint && getDirectoryCount() > maxBackups;
	}
	
	private long getOutputDirectorySize() {
		long size = 0;
		for (final File bu : getBackupFiles()) {
			size += bu.length();
		}
		
		return size;
	}
	
	private String generateBackupFileName() {
		final String backup = String.format("%s_%d.backup", backupFileBaseName, System.currentTimeMillis());
		return new File(outputDirectory, backup).getAbsolutePath();
	}
	
	private int getDirectoryCount() {
		return this.getBackupFiles().length;
	}
	
	@Override
	public void run() {
		if (!wasInitialized) {
			throw new IllegalStateException("Global LAD persister was not initialzed before run method.");
		}
		
		/**
		 * Gets all the data.
		 * 
		 * MPCS-8189 triviski 5/11/2016 - Must set the time type to ALL to ensure everything is backed up. 
		 */
		final Map<Object, Collection<IGlobalLADData>> results = persistTarget.getAll(BasicQuerySearchAlgorithm
				.createBuilder()
				.setTimeType(GlobalLadPrimaryTime.ALL)
				.build());
		
		if (results.isEmpty()) {
			return;
		}
		
		/**
		 * MPCS-8189 triviski 5/10/2016 - Generate the backup file name and append temp 
		 * to write to it.  Once it is finished, move the file to the final name. 
		 */
		final File finalBackupFile = new File(generateBackupFileName());
		final File inProgressFile = new File(finalBackupFile+INPROGRESS);
		
		try (DataOutputStream op = new DataOutputStream(new FileOutputStream(inProgressFile))) {
			for (final Collection<IGlobalLADData> dataSet : results.values()) {
				for (final IGlobalLADData data : dataSet) {
					op.write(data.toPacketByteArray());
				}
			}
			inProgressFile.renameTo(finalBackupFile);
			this.lastBackupFile = finalBackupFile;
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		
        log.info("Created backup file: ", finalBackupFile);
		
		cleanBackups();
	}
}
