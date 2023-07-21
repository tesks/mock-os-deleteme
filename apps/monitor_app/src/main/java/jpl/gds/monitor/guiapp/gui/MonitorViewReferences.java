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
package jpl.gds.monitor.guiapp.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpl.gds.monitor.guiapp.common.ViewReferenceListener;
import jpl.gds.perspective.view.ViewReference;

/**
 * MonitorViewReferences is a singleton that tracks ViewReferences open in the current
 * perspective and notifies listeners when open view references change.
 */
public final class MonitorViewReferences {

	private final Map<ViewReference, Integer> viewReferences = new HashMap<ViewReference, Integer>();
	private final List<ViewReferenceListener> viewReferenceListeners = new ArrayList<ViewReferenceListener>();
	private WindowManager windowManager;
	
	/**
	 * The one static instance of this object.
	 */
	private static volatile MonitorViewReferences instance;

	/**
	 * Constructor.  Private for Singleton pattern.
	 */
	private MonitorViewReferences() {}

	/**
	 * Gets the one static instance of this class.
	 * 
	 * @return MonitorViewReferences
	 */
	public synchronized static MonitorViewReferences getInstance() {
		if (instance == null) {
			instance = new MonitorViewReferences();
		}
		return instance;
	}

	/**
	 * This method is part of a proper singleton class. It prevents using
	 * cloning as a hack around the singleton.
	 * 
	 * @return It never returns
	 * @throws CloneNotSupportedException
	 *             This function always throws this exception
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	/**
	 * Add a ViewReference to the table of view references and notifies listeners 
	 * of the change.
	 * 
	 * @param toAdd ViewReference to add
	 */
	public synchronized void addViewReference(final ViewReference toAdd) {
		if (!viewReferences.containsKey(toAdd)) {
			viewReferences.put(toAdd, 1);
		} else {
			Integer count = viewReferences.get(toAdd);
			viewReferences.put(toAdd, count + 1);
		}
		notifyListeners();
	}
	
	/**
	 * Adds all the view references on the given list.
	 * @param references the List of ViewReferences to add; if null, does nothing
	 */
	public synchronized void addViewReferences(List<ViewReference> references) {
		if (references == null) {
			return;
		}
		for (ViewReference r: references) {
			addViewReference(r);
		}		
	}

	/**
	 * Removes a ViewReference from the table of view references and notifies listeners 
	 * of the change.
	 * 
	 * @param toRemove the ViewReference to remove
	 */
	public synchronized void removeViewReference(final ViewReference toRemove) {
		if (toRemove == null) {
			return;
		}
		if (viewReferences.containsKey(toRemove)) {
			Integer count = viewReferences.get(toRemove);
			if (count.equals(1)) {
    			viewReferences.remove(toRemove);
			} else {
				viewReferences.put(toRemove, count - 1);
			}
			notifyListeners();
		}
	}
	
	/**
	 * Removes all the view references on the given list.
	 * @param references the List of ViewReferences to remove; if null, does nothing
	 */
	public synchronized void removeViewReferences(List<ViewReference> references) {
		if (references == null) {
			return;
		}
		for (ViewReference r: references) {
			removeViewReference(r);
		}		
	}

	/**
	 * Adds a ViewReferenceListener to this object.
	 * 
	 * @param toAdd the listener to add
	 */
	public synchronized void addViewReferenceListener(final ViewReferenceListener toAdd) {
		if (!viewReferenceListeners.contains(toAdd)) {
			viewReferenceListeners.add(toAdd);
		}
	}

	/**
	 * Removes a ViewReferenceListener from this object.
	 * 
	 * @param toRemove the listener to remove
	 */
	public synchronized void removeViewReferenceListener(final ViewReferenceListener toRemove) {
		viewReferenceListeners.remove(toRemove);
	}

	/**
	 * Notifies listeners that there has been a change to this object.
	 */
	private synchronized void notifyListeners() {
		for (ViewReferenceListener l : viewReferenceListeners) {
			l.referencesChanged();
		}
	}

	/**
	 * Indicates whether the given ViewReference is loaded in the monitor (i.e., present 
	 * in this table).
	 * 
	 * @param toFind the ViewReference to look for
	 * 
	 * @return true if the ViewReference is found in the reference table, false if not
	 */
	public synchronized boolean isViewReferenceLoaded(final ViewReference toFind) {
		for (ViewReference ref: viewReferences.keySet()) {
			if (toFind.getPath() != null && ref.getPath() != null && 
					toFind.getPath().equals(ref.getPath())) {
			    return true;
			}
			if (toFind.getName().equalsIgnoreCase(ref.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Clears all ViewReferences from the table and notifies listeners of the change.
	 */
	public synchronized void clearViewReferences() {
		viewReferences.clear();
		notifyListeners();
	}

	/**
	 * Gets the global WindowManager object.
	 * 
	 * @return WindowManager object, or null if none set
	 */
	public WindowManager getWindowManager() {
		return windowManager;
	}

	/**
	 * Sets the global WindowManager object.
	 * 
	 * @param windowManager the WindowManager to set
	 */
	public void setWindowManager(final WindowManager windowManager) {
		this.windowManager = windowManager;
	}
}
