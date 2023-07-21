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
/**
 * Project:	AMMOS Mission Data Processing and Control System (MPCS)
 * Package:	jpl.gds.tc.impl.app
 * File:	CommandStatusRequestApp.java
 *
 *
 */
package jpl.gds.product.app;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.cli.legacy.app.AbstractCommandLineApp;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.builder.ProductStorageException;
import jpl.gds.shared.file.ISharedFileLock;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.spring.context.SpringContextFactory;
/**
 * This is a test tool application to test the ISharedFileLock functionality.
 * Given a lock file path, it will create and hold the lock for a user-specified
 * amount of time.
 *
 * @see jpl.gds.product.builder.ISharedFileLock
 */
public class ProductFileLockerApp extends AbstractCommandLineApp {

	/**
	 * Short option for specifying lock file
	 */
	public static final String LOCK_FILE_SHORT = "f";
	/**
	 * Long option for specifying lock file
	 */
	public static final String LOCK_FILE_LONG = "lockfile";
	/**
	 * Short option for specifying duration
	 */
	public static final String LOCK_DURATION_SHORT = "d";
	/**
	 * Long option for specifying duration
	 */
	public static final String LOCK_DURATION_LONG = "duration";
	/**
	 * Short option for specifying that application should keep running
	 */
	public static final String KEEP_RUNNING_SHORT = "r";
	/**
	 * Long option for specifying that application should keep running
	 */
	public static final String KEEP_RUNNING_LONG = "keepRunning";
	
	private String filename;
	private long millisecs;
	private boolean keepRunning;
    private final ApplicationContext appContext;
	
	public ProductFileLockerApp() {
	    appContext = SpringContextFactory.getSpringContext(true);
	}
	
	private void run() throws IOException, InterruptedException, ProductStorageException {
		final ISharedFileLock pfl = appContext.getBean(IProductBuilderObjectFactory.class).createFileLock(filename);
		System.out.println("Locking " + pfl.getLockFilePath() + " for " + millisecs + " milliseconds");
		final boolean locked = pfl.lock();

		if (!locked) {
			System.out.println("Failed to lock");
			return;
		}
			
		Thread.sleep(millisecs);
		pfl.release();
		System.out.println("Released lock on " + pfl.getLockFilePath());
		
		if (keepRunning) {
			System.out.println("Process will continue to run...");
			
			while (true) {
				Thread.sleep(5000);
			}
			
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#createOptions()
	 */
	@Override
	public Options createOptions() {
		final Options options = super.createOptions();

		options.addOption(ReservedOptions
				.createOption(
						LOCK_FILE_SHORT,
						LOCK_FILE_LONG,
						"filename",
						"Filename of the lock file"));
		options.addOption(ReservedOptions.createOption(
				LOCK_DURATION_SHORT,
				LOCK_DURATION_LONG,
				"ms",
				"Duration to hold the lock before releasing (in milliseconds)"));
		options.addOption(ReservedOptions.createOption(
				KEEP_RUNNING_SHORT,
				KEEP_RUNNING_LONG,
				null,
				"If supplied, the program will continue to run without exiting"));

		return options;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#configure(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void configure(final CommandLine commandLine) throws ParseException {
		super.configure(commandLine);

		if (commandLine.hasOption(LOCK_FILE_SHORT)) {
			filename = commandLine.getOptionValue(LOCK_FILE_SHORT, null);
		} else {
			throw new ParseException("Required option \"" + LOCK_FILE_LONG + "\" is missing");
		}
		
		if (commandLine.hasOption(LOCK_DURATION_SHORT)) {
			final String durationStr = commandLine.getOptionValue(LOCK_DURATION_SHORT, null);
			millisecs = GDR.parse_int(durationStr.trim());
		} else {
			throw new ParseException("Required option \"" + LOCK_DURATION_LONG + "\" is missing");
		}

		if (commandLine.hasOption(KEEP_RUNNING_SHORT)) {
			keepRunning = true;
		} else {
			keepRunning = false;
		}

	}

    /**
     * Main entry point for execution.
     *
     * @param args	command line arguments
     */
    public static void main(final String[] args) {
    
    	final ProductFileLockerApp app = new ProductFileLockerApp();
    
    	try {
    		final CommandLine cl = ReservedOptions.parseCommandLine(args, app);
    		app.configure(cl);
    
    	} catch (final ParseException e) {
    		System.err.println("Exception encountered while interpreting arguments: " + e.getMessage());
    		System.exit(1);
    	}
    
    	try {
    		app.run();
    		
    	} catch (final Exception e) {
    		System.err.println("Exception encountered while locking/waiting/releasing: " + e.getMessage());
    		System.exit(1);
    	}
    
    }
	
}
