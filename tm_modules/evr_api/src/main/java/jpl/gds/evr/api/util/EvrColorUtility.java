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
package jpl.gds.evr.api.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.graphics.Color;

import jpl.gds.evr.api.config.EvrProperties;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * Utility class for formatting EVR content with proper color designations.
 * 
 */
public final class EvrColorUtility {

	
	private Map<String, Color> foreColors;
	private Map<String, Color> backColors;
	private Map<String, ChillColor> foreChillColors;
	private Map<String, ChillColor> backChillColors;
	private final EvrProperties evrProps;
	
	
    /**
     * The one static instance of this class.
     */
	
	/**
	 * Constructor.
	 * 
	 * @param props the current EVR properties object
	 */
	public EvrColorUtility(final EvrProperties props) {
		this.evrProps = props;
	}
	
	/**
	 * Loads ChillColor objects for EVR backgrounds and foregrounds by creating them from
	 * the colors specified in the configuration file.
	 */
	private void loadColors() {

		if (backColors != null) {
			return;
		}
		
		final Set<String> allLevels = evrProps.getCombinedEvrLevels();
		if (allLevels == null) {
		    return;
		}
		
		backChillColors = new HashMap<String, ChillColor>();
		for (final String level : allLevels) {
			
			try {
				final ChillColor chillColor = new ChillColor(evrProps.getBackgroundColor(level));
				backChillColors.put(level, chillColor);
				
			} catch (final Exception e) {
				backChillColors.put(level, new ChillColor(ChillColor.ColorName.WHITE));
			}
		}
		
		foreChillColors = new HashMap<String,ChillColor>();
		for (final String level: allLevels) {
			
			try {
				final ChillColor chillColor = new ChillColor(evrProps.getForegroundColor(level));
				foreChillColors.put(level, chillColor);
				
			} catch (final Exception e) {
				foreChillColors.put(level, new ChillColor(ChillColor.ColorName.BLACK));
			}
		}
	}
	
	/**
	 * Loads SWT Color objects for EVR backgrounds and foregrounds by creating
	 * them from the colors specified in the configuration file.
	 */
	private void loadSwtColors() {

		if (backChillColors != null) {
			return;
		}
		
		loadColors();
		
		backColors = new HashMap<String, Color>();
		
		for (final String newKey: backChillColors.keySet()) {
			final ChillColor chillColor = backChillColors.get(newKey);
			try {
				final Color c = ChillColorCreator.getColor(chillColor);
				backColors.put(newKey, c);
				
			} catch (final Exception e) {
				backColors.put(newKey, ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.WHITE)));
			}
			
		}
		
		foreColors = new HashMap<String, Color>();
		
		for (final String newKey: foreChillColors.keySet()) {
			final ChillColor chillColor = foreChillColors.get(newKey);
			try {
				final Color c = ChillColorCreator.getColor(chillColor);
				foreColors.put(newKey, c);
				
			} catch (final Exception e) {
				foreColors.put(newKey, ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.BLACK)));
			}
			
		}

	}

	/**
	 * Gets the SWT color object for the background of EVRs with the given level.
	 * @param level EVR level string
	 * @return SWT Color object, or null if none defined
	 */
	public Color getSwtBackgroundColorForLevel(final String level) {
		loadSwtColors();

		if (backColors == null) {
			return null;
		}
		final Color c = backColors.get(level);
		if (c == null) {
			return null;
		}
		return c;
	}

	/**
	 * Gets the SWT color object for the foreground of EVRs with the given level.
	 * @param level EVR level string
	 * @return SWT Color object, or null if none defined
	 */
	public Color getSwtForegroundColorForLevel(final String level) {

		loadSwtColors();
		
		if (foreColors == null) {
			return null;
		}
		final Color c = foreColors.get(level);
		if (c == null) {
			return null;
		}
		return c;       
	}
	
	/**
	 * Gets the ChillColor object for the background of EVRs with the given level.
	 * @param level EVR level string
	 * @return ChillColor object, or null if none defined
	 */
	public ChillColor getBackgroundColorForLevel(final String level) {
		loadColors();
		
		if (backChillColors == null) {
			return null;
		}
		final ChillColor c = backChillColors.get(level);
		if (c == null) {
			return null;
		}
		return c; 
	}
	
	/**
	 * Gets the ChillColor object for the foreground of EVRs with the given level.
	 * @param level EVR level string
	 * @return ChillColor object, or null if none defined
	 */
    public ChillColor getForegroundColorForLevel(final String level) {
    	loadColors();
		
		if (foreChillColors == null) {
			return null;
		}
		final ChillColor c = foreChillColors.get(level);
		if (c == null) {
			return null;
		}
		return c; 
	}
    
    /**
     * Gets a copy of the entire hash map of EVR level versus foreground ChillColor objects.
     * @return HashMap of EVR level versus ChillColor, or null if no table defined
     */
    public Map<String, ChillColor> getForegroundColorsForAllLevels() {
    	loadColors();
    	if (foreChillColors == null) {
    		return null;
    	}
    	final HashMap<String,ChillColor> newMap = new HashMap<String,ChillColor>(foreChillColors.size());
    	newMap.putAll(foreChillColors);
    	return newMap;
    }
    
    
    /**
     * Gets a copy of the entire hash map of EVR level versus background ChillColor objects.
     * @return HashMap of EVR level versus ChillColor, or null if no table defined
     */
    public Map<String, ChillColor> getBackgroundColorsForAllLevels() {
    	loadColors();
    	if (backChillColors == null) {
    		return null;
    	}
    	final HashMap<String,ChillColor> newMap = new HashMap<String,ChillColor>(backChillColors.size());
    	newMap.putAll(backChillColors);
    	return newMap;
    }

}
