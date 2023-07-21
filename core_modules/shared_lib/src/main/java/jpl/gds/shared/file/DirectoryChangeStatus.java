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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * DirectoryChangeStatus is used to encapsulate the status of files in a directory
 * as it changes over time. It is used by the DirectoryChangeMonitor.
 *
 *
 */
public class DirectoryChangeStatus {
    
    private File directory;
    private boolean exists;
    private Map<File,Long> timestamps = new HashMap<File,Long>(); // File -> Long timestamp

    private DirectoryChangeEvent newEvent;
    private Map<File,Long> newTimestamps = new HashMap<File,Long>(); // for comparisons
    private FilenameFilter filenameFilter;

    /**
     * Creates an instance of DirectoryChangeStatus.
     * @param directory the File object for the directory to monitor
     */
    public DirectoryChangeStatus(File directory) {
        this.directory = directory;
        newEvent = new DirectoryChangeEvent(directory, this);
    }

    /**
     * Resets everything in this status object to a starting state.
     */
    public void reset() {
        if (!directory.exists()) {
            exists = false;
            timestamps.clear();
            return;
        }
        exists = true;
        File[] files = listFiles();
        for (int i = 0; i < files.length; ++i) {
            timestamps.put(files[i], Long.valueOf(files[i].lastModified()));
        }
    }

    private File[] listFiles() {
        if (filenameFilter == null) {
            return directory.listFiles();
        }
        return directory.listFiles(filenameFilter);
    }

    /**
     * Retrieves a directory change event object for the latest directory changes.
     * @return DirectoryChangeEvent
     */
    public DirectoryChangeEvent getChange() {
        boolean changed = false;
        newEvent.reset();
        newTimestamps.clear();

        // check if this directory was just created
        if (directory.exists()) {
            if (!exists) {
                newEvent.setCreated(true);
                exists = true;
                changed = true;
            }
        }
        // check if this directory was just deleted
        else if (exists) {
            changed = true;
            exists = false;
            newEvent.setDeleted(true);
            newEvent.setRemovedFiles(timestamps.keySet());
            DirectoryChangeEvent event = newEvent.copy();
            newEvent.reset();
            timestamps.clear();
            return event;
        }

        // check for added or modified files
        File[] files = listFiles();
        if (files == null) {
            files = new File[0];
        }
        for (int i = 0; i < files.length; ++i) {
            long newTimestamp = files[i].lastModified();
            Long oldTimestamp = timestamps.get(files[i]);
            if (oldTimestamp == null) {
                newEvent.addAddedFile(files[i]);
                changed = true;
            }
            else if (oldTimestamp.longValue() != newTimestamp) {
                newEvent.addModifiedFile(files[i]);
                changed = true;
            }
            newTimestamps.put(files[i], Long.valueOf(newTimestamp));
        }

        // check for removed files
        for (Iterator<File> i = timestamps.keySet().iterator(); i.hasNext();) {
            File oldFile = i.next();
            if (!newTimestamps.containsKey(oldFile)) {
                newEvent.addRemovedFile(oldFile);
                changed = true;
            }
        }

        // rotate timestamp maps
        Map<File,Long> tmp = timestamps;
        timestamps = newTimestamps;
        newTimestamps = tmp;
        newTimestamps.clear();

        // make event if something changed
        DirectoryChangeEvent event = null;
        if (changed) {
            event = newEvent.copy();
        }
        newEvent.reset();
        return event;
    }

    /**
     * Sets the filter for filename to be monitored.
     * @param filenameFilter the FilenameFilter matching the files to watch
     */
    public void setFilenameFilter(FilenameFilter filenameFilter) {
        this.filenameFilter = filenameFilter;
    }
}
