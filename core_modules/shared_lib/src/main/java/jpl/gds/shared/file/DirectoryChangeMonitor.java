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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.thread.SleepUtilities;


/**
 * DirectoryChangeMonitor is used to asynchronously monitor a directory for file changes.
 * It can detect files added, removed, or modified. It can be limited to watching
 * for specific file names of patterns.  A listener mechanism is used to notify 
 * other objects of the changes.
 *
 *
 */
public class DirectoryChangeMonitor implements Runnable {

    /** The directory being monitored. **/
    private File directory;

    /** The current directory status object. **/
    private DirectoryChangeStatus status;
    
    /** Filename filter. Matches any file by default. **/
    private FilenameFilter filenameFilter = new AnyFileFilter();

    /** Time in milliseconds to sleep between checking for changes **/
    private long pollInterval = 5000;

    /** Listeners to this monitor **/
    private final List<DirectoryChangeListener> listeners = new ArrayList<DirectoryChangeListener>();
    
    /** Thread that instance is running in **/
    private Thread thread;

    /** flag to indicate that thread should stop **/
    private boolean isStopping;

    private final Tracer log;

    /**
     * Creates an instance of DirectoryChangeMonitor. The setDirectory() method
     * must be called before starting this monitor.
     * 
     */
    public DirectoryChangeMonitor() {
        this(TraceManager.getDefaultTracer());
    }

    /**
     * Creates an instance of DirectoryChangeMonitor. The setDirectory() method
     * must be called before starting this monitor.
     * 
     * @param trace
     *            the application context logger
     */
    public DirectoryChangeMonitor(final Tracer trace)
    {
        this.thread = new Thread(this, "Directory Monitor");
        log = trace;
        thread.setDaemon(true);
    }


    /**
     * Creates an instance of DirectoryChangeMonitor top monitor the given
     * directory.
     *
     * @param directory
     *            File object for the directory to watch
     * @param trace
     *            The Tracer context logger
     * @param Tracer
     *            The application context logger
     */
    public DirectoryChangeMonitor(final File directory, final Tracer trace)
    {
    	log = trace;
        setDirectory(directory);
    }


    /**
     * Starts polling the directory.
     */
    public void start() {
        this.thread.start();
    }


    /**
     * Wake up periodically and check status.
     *
     * To avoid interrupts, we sleep for no more than a short time so that we
     * can detect that we must stop. We don't check status that often, though.
     *
     * The poll interval might change, we presume.
     *
     * The poll interval will usually be several seconds.
     *
     */
    @Override
	public void run()
    {
        isStopping = false;

        long interval = getPollInterval();
        long target   = System.currentTimeMillis() + interval;

        while (true)
        {
            // Keep sleep out of the try, because if there is an error
            // in sleeping we could get a fast infinite loop. Sleep must work.
            SleepUtilities.checkedSleep(Math.min(interval, 1000L));

            if (isStopping)
            {
                break;
            }

            final long now = System.currentTimeMillis();

            if (now < target)
            {
                interval = target - now; // Must be > zero
                continue;
            }

            try
            {
                // Do real work


                final DirectoryChangeEvent event = status.getChange();
             
                if (event != null)
                {
                    event.setSource(this);
                    fireDirectoryChangeEvent(event);
                }
            }
            catch (final Exception e)
            {
                // Do NOT rethrow RuntimeExceptions, handle those as well

                log.error("Unexpected exception in DirectoryChangeMonitor: " +
                          e);
            }

            // Set up next check time

            interval = getPollInterval();
            target   = now + interval;
        }
    }


    /**
     * Stops polling the directory.
     */
    public void stop()
    {
        isStopping = true;
    }


    /**
     * Gets the File object for the directory being monitored.
     * @return the directory File
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Sets the directory being monitored and resets the current status.
     * @param directory the File object for the directory to be monitored
     */
    public void setDirectory(final File directory) {
        this.directory = directory;
        status = new DirectoryChangeStatus(directory);
        status.setFilenameFilter(filenameFilter);
        status.reset();
    }

    /**
     * Sets the filename filter for this monitor. Only files that match the filter
     * will be monitored.
     * @param filenameFilter the FilenameFilter to set
     */
    public void setFilenameFilter(final FilenameFilter filenameFilter) {
        this.filenameFilter = filenameFilter;
        if (status != null) {
            status.setFilenameFilter(filenameFilter);
        }
    }

    /**
     * Returns the time in milliseconds between checking for changes.
     * @return the poll interval in milliseconds
     */
    public long getPollInterval() {
        return pollInterval;
    }

    /**
     * Sets the time in milliseconds between checking for directory changes.
     * @param milliseconds interval in milliseconds
     */
    public void setPollInterval(final long milliseconds) {
        this.pollInterval = milliseconds;
    }

    /**
     * Adds a listener to this monitor.
     * @param l the DirectoryChangeListener to add
     */
    public void addDirectoryChangeListener(final DirectoryChangeListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    /**
     * Removes a listener from this monitor.
     * @param l the DirectoryChangeListener to remove
     */
    public void removeDirectoryChangeListener(final DirectoryChangeListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    /**
     * Notifies listeners of a directory change.
     * @param event the DirectoryChangeEvent describing what changed
     */
    private void fireDirectoryChangeEvent(final DirectoryChangeEvent event) {
        List<DirectoryChangeListener> safeList = null;
        synchronized (listeners) {
            safeList = new ArrayList<>(listeners);
        }
        DirectoryChangeListener listener = null;
        final Iterator<DirectoryChangeListener> i = safeList.iterator();
        while (i.hasNext()) {
            listener = i.next();
            listener.directoryChanged(event);
        }
    }


   /**
     * AnyFileFilter is a FilenameFilter that matches any file.
     *
     *
     */
    public static class AnyFileFilter implements FilenameFilter
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept(final File dir, final String name) {
            return true;
        }
    }
}
