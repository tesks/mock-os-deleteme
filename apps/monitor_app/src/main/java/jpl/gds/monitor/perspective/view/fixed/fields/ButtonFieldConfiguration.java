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
package jpl.gds.monitor.perspective.view.fixed.fields;

import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.TextConfigSupport;
import jpl.gds.monitor.perspective.view.fixed.TwoColorConfigSupport;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * ButtonFieldConfiguration is a subclass of FixedFieldConfiguration that represents a
 * button on a fixed layout view; the button can be clicked to take an action.  Buttons
 * support foreground and background colors, and a button title.
 */ 
public class ButtonFieldConfiguration extends DualPointFixedFieldConfiguration 
implements TwoColorConfigSupport, TextConfigSupport {

	// XML tags and attribute names
    /**
     * XML button element name
     */
	public static final String BUTTON_TAG = "Button";
	
	/**
	 * XML title attribute name
	 */
	public static final String TITLE_TAG = "title";
    
    /**
     * XML button action string attribute name
     */
	public static final String ACTION_STRING_TAG = "actionString";
    
    /**
     * XML background color attribute name
     */
	public static final String BACKGROUND_TAG = "background";
    
    /**
     * XML foreground color attribute name
     */
	public static final String FOREGROUND_TAG = "foreground";
    
    /**
     * XML action type attribute name
     */
	public static final String ACTION_TYPE_TAG = "actionType";

	/**
	 * Types of actions that can be triggered by the button.
	 */
	public enum ActionType {
		/**
		 * Button action for launching and opening a fixed page
		 */
		LAUNCH_PAGE,
		
		/**
		 * Button action for executing a script
		 */
		LAUNCH_SCRIPT
	}

	private String title = "Button";
	private String actionString; // required
	private ChillColor background;	// Optional
	private ChillColor foreground;	// Optional
	private ActionType actionType; // required
	private boolean isDefaultColors;

	/**
	 * Creates a new ButtonFieldConfiguration.
	 */
	public ButtonFieldConfiguration() {
		super(FixedFieldType.BUTTON);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TextConfigSupport#getText()
	 */
	@Override
    public String getText() {
		return title;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TextConfigSupport#setText(java.lang.String)
	 */
	@Override
    public void setText(final String title) {
		this.title = title;
	}

	/**
	 * Gets the action string for this button. The string may be the name of another
	 * fixed layout view, or a command line to execute, etc, depending upon 
	 * the action type.
	 * 
	 * @return the button action string
	 * 
	 */
	public String getActionString() {
		return actionString;
	}

	/**
	 * Sets the action string for this button. The string may be the name of another
	 * fixed layout view, or a command line to execute, etc, depending on the action
	 * type.
	 * 
	 * @param action the button action string to set
	 * 
	 */
	public void setActionString(final String action) {
		actionString = action;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TwoColorConfigSupport#getBackground()
	 */
	@Override
    public ChillColor getBackground() {
		return background;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.TwoColorConfigSupport#setBackground(jpl.gds.shared.swt.types.ChillColor)
	 */
	@Override
    public void setBackground(final ChillColor background) {
		this.background = background;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#getForeground()
	 */
	@Override
    public ChillColor getForeground() {
		return foreground;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#setForeground(jpl.gds.shared.swt.types.ChillColor)
	 */
	@Override
    public void setForeground(final ChillColor foreground) {
		this.foreground = foreground;
	}

	/**
	 * Gets the type of action executed when this button is pressed.
	 * 
	 * @return the ActionType 
	 */
	public ActionType getActionType() {
		return actionType;
	}

	/**
	 * Sets the type of action executed when this button is pressed.
	 * 
	 * @param at the ActionType to set
	 */
	public void setActionType(final ActionType at) {
		actionType = at;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFieldTag() {
		return BUTTON_TAG;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.fields.DualPointFixedFieldConfiguration#getAttributeXML(java.lang.StringBuilder)
	 */
	@Override
	public void getAttributeXML(final StringBuilder toAppend) {
		super.getAttributeXML(toAppend);
		if (background != null) {
			toAppend.append(BACKGROUND_TAG + "=\"" + background.getRgbString() + "\" ");
		}
		if (foreground != null) {
			toAppend.append(FOREGROUND_TAG + "=\"" + foreground.getRgbString() + "\" ");
		}
		toAppend.append(TITLE_TAG + "=\"" + this.getUnicodeStringForXml(title) + "\" ");
		toAppend.append(ACTION_STRING_TAG + "=\"" + actionString + "\" ");
		toAppend.append(ACTION_TYPE_TAG + "=\"" + actionType + "\" ");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBuilderDefaults(final CoordinateSystemType coordSystem) {
		ChillPoint start = null;
		ChillPoint end = null;
		if (coordSystem.equals(CoordinateSystemType.CHARACTER)) {
			start = new ChillPoint(5,5,coordSystem);
			end = new ChillPoint(15,3, coordSystem);
		} else {
			start = new ChillPoint(20,20,coordSystem);
			end = new ChillPoint(80,50, coordSystem);
		}
		this.setStartCoordinate(start);
		this.setEndCoordinate(end);
		this.setText("Button");
		this.setActionType(ActionType.LAUNCH_SCRIPT);
		this.setActionString("/bin/ls");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void copyConfiguration(final IFixedFieldConfiguration newConfig) {
		if (!(newConfig instanceof ButtonFieldConfiguration)) {
			throw new IllegalArgumentException("Object for copy is not of type ButtonFieldConfiguration");
		}
		final ButtonFieldConfiguration buttonConfig = (ButtonFieldConfiguration)newConfig;
		super.copyConfiguration(newConfig);
		buttonConfig.title = title;
		buttonConfig.actionString = actionString;
		buttonConfig.actionType = actionType;
		if (background != null) {
			buttonConfig.background = new ChillColor(background);
		}
		if (foreground != null) {
			buttonConfig.foreground = new ChillColor(foreground);
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#usesDefaultColors()
	 */
	@Override
    public boolean usesDefaultColors() {
		isDefaultColors = foreground == null && background == null ? true : false;
		return isDefaultColors;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.perspective.view.fixed.OneColorConfigSupport#usesDefaultColors(boolean)
	 */
	@Override
    public void usesDefaultColors(final boolean usesDefaultColors) {
		this.isDefaultColors = usesDefaultColors;
	}
}

