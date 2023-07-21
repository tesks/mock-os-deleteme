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
package jpl.gds.perspective.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * ViewScanner searches the project-configured view search path for Xml files and creates 
 * a list of ViewRefrence objects that point to them.
 * 
 *
 */
public class ViewScanner {
	private final HashMap<String, ViewReference> scannedViews = new HashMap<String, ViewReference>();
	private final PerspectiveProperties perspectiveProps;
    private static SseContextFlag                sseFlag;

    /**
     * @param perspectiveProps
     *            Perspective Properties
     * @param sseFlag
     *            The SSE context flag
     */
    public ViewScanner(final PerspectiveProperties perspectiveProps, final SseContextFlag sseFlag) {
		this.perspectiveProps = perspectiveProps;
        ViewScanner.sseFlag = sseFlag;
	}
	
	/**
	 * Directs this object to scan configured view directories and create a list of references.
	 */
	public void scanViews() {
		scannedViews.clear();
        final List<String> viewList = getConfiguredViewPaths(perspectiveProps, sseFlag);
		if (viewList == null) {
			return;
		}
		for (final String path: viewList) {
			final ViewReference ref = createViewReference(path);
			if (ref != null) {
				scannedViews.put(ref.getPath(), ref);
			}
		}
	}

	/**
	 * Gets the sorted list of ViewReferences found by the file system scan.
	 * @return a list of ViewReference objects
	 */
	public List<ViewReference> getViewList() {
		if (scannedViews.size() == 0) {
			scanViews();
		}
		List<ViewReference> result = new ArrayList<ViewReference>(scannedViews.size());

		final Set<String> keys = scannedViews.keySet();
		for (final String key : keys) {
			result.add(scannedViews.get(key));
		}
		result = sortViews(result);
		return result;
	}

	private List<ViewReference> sortViews(final List<ViewReference> viewList) {
		final ViewReference[] refs = new ViewReference[viewList.size()];
		viewList.toArray(refs);
		Arrays.sort(refs);
		final ArrayList<ViewReference> result = new ArrayList<ViewReference>(refs.length);
		for (final ViewReference ref: refs) {
			result.add(ref);
		}
		return result;
	}

	/**
	 * Retrieves the list of ViewReferences created by the file system scan that also
	 * match the given ViewType
	 * @param type the type of ViewReferences to return
	 * @return the List of matching ViewReference objects
	 */
	public List<ViewReference> getViewList(final ViewType type) {
		final List<ViewReference> allViews = getViewList();
		final ArrayList<ViewReference> filteredViews = new ArrayList<ViewReference>();

		for (final ViewReference ref : allViews) {
			if (ref.getType().equals(type)) {
				filteredViews.add(ref);
			}
		}
		return filteredViews;
	}

	// This method does not fire up an SAX parser because we do not want to do
	// a full parse of every view file on the search path
	private ViewReference createViewReference(final String filename) {
		BufferedReader fr = null;

		try {
			fr = new BufferedReader(new FileReader(filename));
			String line = fr.readLine();
			if (line != null) {
				line = line.trim();
			}
			while (line != null && (line.equals("") || line.startsWith("<!--") || line.startsWith("<?xml") ||
					!line.startsWith("<"))) {
				line = fr.readLine();
			}
			if (line == null) {
				return null;
			}
			line = line.trim();

			if (!line.startsWith("<View")) {	
				return null;
			}

			int typeIndex = line.indexOf("type=\"");
			if (typeIndex == -1) {
				return null;
			}
			typeIndex += "type=\"".length();
			final int endIndex = line.indexOf("\"", typeIndex);
			if (endIndex == -1) {
				return null;
			}
			final String type = line.substring(typeIndex, endIndex);
			final ViewType viewType = new ViewType(type);

			final ViewReference ref = new ViewReference();
			ref.setName(createNameFromPath(filename));
			ref.setPath(filename);
			ref.setType(viewType);
			ref.setFoundByName(true);
			return ref;

		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (fr != null) {
					fr.close();
				}
			} catch (final IOException e) {
				// do nothing			
			}
		}
	}

	/**
	 * Gets a ViewReference name given its path. The name is the file name portion
	 * less the .xml extension.
	 * @param path the ViewReference path
	 * @return the corresponding ViewReference name
	 */
	public static String createNameFromPath(final String path) {
		final File f = new File(path);
		final String name = f.getName().substring(0, f.getName().length() - 4);
		return name;
	}

	/**
	 * Gets the list of file paths to all files with .xml extensions found on the project
	 * view search path.
	 * Uses GdsSystemProperties to get directories to be checked for views instead of a fixed
	 * set of directories
	 *
	 * @param perspectiveProps the current PerspectiveProperties, used to get the configured
	 *       view directories to be checked 
	 * @return a List of file paths as Strings
	 * 
	 */
    public static List<String> getConfiguredViewPaths(final PerspectiveProperties perspectiveProps,
                                                      final SseContextFlag sseFlag) {
		final List<String> dirs = perspectiveProps.getViewDirectories();
		
		// The config paths must
		// be added to the list of directories in REVERSE order, so that the user and project
		// view files will be found before multimission view files
        final List<String> configDirs = GdsSystemProperties.getFullConfigPathList(sseFlag.isApplicationSse());
		for (int i = configDirs.size() - 1; i >= 0; i--) {
		    dirs.add(configDirs.get(i));
		}

        final List<String> result = new ArrayList<>();
		for (final String dir: dirs) {
			final File dirFile = new File(dir);
			final File[] allFiles = dirFile.listFiles(new XmlFileFilter());
			if (allFiles == null) {
				continue;
			}
			for (final File name: allFiles) {
				result.add(name.getPath());
			}
		}
		return result;
	}

	/**
	 * Filter for .xml files
	 *
	 */
	private static class XmlFileFilter implements FilenameFilter {
		/**
		 * {@inheritDoc}
		 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
		 */
		@Override
        public boolean accept(final File dir, final String name) {
			return name.endsWith(".xml");
		}
	}
}
