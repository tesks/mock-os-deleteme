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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * DirectoryChangeEvent is the event object used to notify DirectoryChangeListeners of directory
 * changes detected by the DirectoryChangeMonitor.
 *
 *
 */
public class DirectoryChangeEvent
{
    private File directory;
    private boolean created = false;
    private boolean deleted = false;
    private List<File> added;
    private List<File> removed;
    private List<File> modified;
    private Object eventSource;


    /**
     * Creates an instance of DirectoryChangeEvent.
     * @param directory the File object for the directory being monitored
     * @param source source Object of the event
     */
    public DirectoryChangeEvent(File directory, Object source) {
        this.directory = directory;
        this.eventSource = source;
    }

    /**
     * Resets all attributes of this object to a starting state.
     */
    public void reset() {
        created = false;
        deleted = false;
        added = null;
        removed = null;
        modified = null;
    }
    
    /**
     * Sets the source object for this event.
     * 
     * @param source the object to set
     */
    public void setSource(Object source) {
        this.eventSource = source;
    }
    
    /**
     * Gets the source object for this event
     * @return source object
     */
    public Object getSource() {
        return this.eventSource;
    }

    /**
     * Get monitored directory.
     *
     * @return the File object for the directory being monitored.
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Sets the flag indicating the directory has just been created.
     * @param flag true if the directory has been created
     */
    public void setCreated(boolean flag) {
        created = flag;
    }

    /**
     * Get is-created state.
     * 
     * @return flag indicating the directory has just been corrected
     */
    public boolean isCreated() {
        return created;
    }

    /**
     * Sets the flag indicating the directory has just been deleted.
     * @param flag true if the directory has been deleted
     */
    public void setDeleted(boolean flag) {
        deleted = flag;
    }

    /**
     * Get is-deleted state.
     * 
     * @return the flag indicating the directory has just been deleted
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Sets the list of files added to the directory.
     * @param c a Collection of File objects
     */
    public void setAddedFiles(Collection<File> c) {
        added = (c == null) ? null : new ArrayList<File>(c);
    }

    /**
     * Adds one file to the list of files added to the directory
     * @param file the File object to add
     */
    public void addAddedFile(File file) {
        if (added == null) {
            added = new ArrayList<File>();
        }
        added.add(file);
    }

    /**
     * Get has-added-files state.
     *
     * @return a flag indicating whether any files have been added to the directory
     */
    public boolean hasAddedFiles() {
        return ((added != null) && (added.size() > 0));
    }

    /**
     * Get list of added files.
     * 
     * @return the List of File objects for files added to the directory
     */
    public List<File> getAddedFiles() {
        if (added == null) {
            added = new ArrayList<File>();
        }
        return added;
    }

    /**
     * Sets the list of files removed from the directory.
     * @param c a Collection of File objects
     */
    public void setRemovedFiles(Collection<File> c) {
        removed = (c == null) ? null : new ArrayList<File>(c);
    }

    /**
     * Adds one file to the list of files removed from the directory
     * @param file the File object to add to the remove list
     */
    public void addRemovedFile(File file) {
        if (removed == null) {
            removed = new ArrayList<File>();
        }
        removed.add(file);
    }

    /**
     * Get has-removed-files state.
     *
     * @return a flag indicating whether files have been removed from the directory
     */
    public boolean hasRemovedFiles() {
        return ((removed != null) && (removed.size() > 0));
    }

    /**
     * Get the List of File objects for files removed from the directory.
     * 
     * @return the List of File objects for files removed from the directory
     */
    public List<File> getRemovedFiles() {
        if (removed == null) {
            removed = new ArrayList<File>();
        }
        return removed;
    }

    /**
     * Sets the list of files modified in the directory.
     * @param c a Collection of File objects
     */
    public void setModifiedFiles(Collection<File> c) {
        modified = (c == null) ? null : new ArrayList<File>(c);
    }


    /**
     * Adds one file to the list of files modified in the directory
     * @param file the File object to add to the modified list
     */
    public void addModifiedFile(File file) {
        if (modified == null) {
            modified = new ArrayList<File>();
        }
        modified.add(file);
    }

    /**
     * Get has-modified-files atate.
     *
     * @return a flag indicating whether files have been modified in the directory
     */
    public boolean hasModifiedFiles() {
        return ((modified != null) && (modified.size() > 0));
    }

    /**
     * Get the List of File objects for files modified in the directory.
     * 
     * @return the List of File objects for files modified in the directory
     */
    public List<File> getModifiedFiles() {
        if (modified == null) {
            modified = new ArrayList<File>();
        }
        return modified;
    }

    /**
     * Clones this object, including the lists. Does not deep clone the File objects.
     * @return a new DirectoryChangeEvent that is a copy of this one
     */
    public DirectoryChangeEvent copy() {
        DirectoryChangeEvent event = new DirectoryChangeEvent(directory, eventSource);
        event.setCreated(created);
        event.setDeleted(deleted);
        event.setAddedFiles(added);
        event.setRemovedFiles(removed);
        event.setModifiedFiles(modified);
        return event;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (isDeleted()) {
            buf.append(directory + " has been deleted");
        }
        else if (isCreated()) {
            buf.append(directory + " has been created");
        }
        else {
            buf.append(directory + " has been updated");
        }

        if (hasAddedFiles()) {
            buf.append("\n\tAdded files:");
            Iterator<File> i = getAddedFiles().iterator();
            while (i.hasNext()) {
                buf.append("\n\t\t" + i.next());
            }
        }

        if (hasRemovedFiles()) {
            buf.append("\n\tRemoved files:");
            Iterator<File> i = getRemovedFiles().iterator();
            while (i.hasNext()) {
                buf.append("\n\t\t" + i.next());
            }
        }

        if (hasModifiedFiles()) {
            buf.append("\n\tModified files:");
            Iterator<File> i = getModifiedFiles().iterator();
            while (i.hasNext()) {
                buf.append("\n\t\t" + i.next());
            }
        }

        return buf.toString();
    }
}
