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
package jpl.gds.monitor.canvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;

import jpl.gds.monitor.canvas.support.ButtonSupport;
import jpl.gds.monitor.canvas.support.DualCoordinateSupport;
import jpl.gds.monitor.canvas.support.TextSupport;
import jpl.gds.monitor.canvas.support.TwoColorSupport;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.fields.ButtonFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ButtonFieldConfiguration.ActionType;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.process.StderrLineHandler;
import jpl.gds.shared.process.StdoutLineHandler;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillColor.ColorName;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * This fixed view Canvas Element is a button. A button can either launch 
 * another fixed view, or run a specific command line. If no end 
 * coordinate is given in the configuration, the button will be sized to 
 * fit its original title text. Buttons support foreground and background 
 * colors.
 */
public class ButtonElement extends DualCoordinateCanvasElement 
implements TwoColorSupport, DualCoordinateSupport, TextSupport, ButtonSupport{
	
	/**
	 * The default selection priority of this CanvasElement
	 */
	private static final int SELECTION_PRIORITY = 2;
	
	/**
	 * The centered text on the button
	 */
	private String title;
	
	/**
	 * The action string may be the name of another fixed layout view or a 
	 * command line to execute
	 */
	private String actionString;
	
	/**
	 * The fill color of the button
	 */
	private Color background;  // Optional
	
	/**
	 * The color of the button border and the title
	 */
	private Color foreground;  // Optional
	
	/**
	 * Enumerated type which determines if the buttons should open a fixed
	 * page or launch a script
	 */
	private ActionType actionType;
	
	/**
	 * Stores the actual background color so that the button background can 
	 * temporarily change colors when it is pressed
	 */
	private Color saveBackground;
	
	private ViewLaunchManager launchManager;
	
	
	/**
	 * Creates a ButtonElement on the given parent Canvas.
	 * 
	 * @param parent the parent Canvas widget
	 */
	public ButtonElement(final Canvas parent) {
		super(parent, FixedFieldType.BUTTON);
		this.setSelectionPriority(SELECTION_PRIORITY);
	}
	
	/**
	 * Creates a ButtonElement with the given fixed view field configuration
	 * on the given parent Canvas.
	 * 
	 * @param parent the parent Canvas widget
	 * @param config the ButtonFieldConfiguration object from the perspective
	 */
	public ButtonElement(final Canvas parent, final ButtonFieldConfiguration config) {
		super(parent, config);
		updateFieldsFromConfig();
		this.setSelectionPriority(SELECTION_PRIORITY);
	}
	
	/**
	 * Sets the ViewLauncher object to be used by buttons that launch views.
	 * 
	 * @param launcher ViewLaunchManager to set
	 */
	public void setViewLaunchManager(final ViewLaunchManager launcher) {
	    this.launchManager = launcher;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#draw(org.eclipse.swt.graphics.GC)
	 */
	@Override
    public void draw(final GC gc) {
		
	    if (!displayMe && this.getFieldConfiguration().getCondition() != null) {
            return;
        }
	    
		saveGcSettings(gc);
		
		gc.setLineWidth(2);
		gc.setLineStyle(SWT.LINE_SOLID);
		
		if (foreground != null) {
			gc.setForeground(foreground);
		}
		
		int titleLen = title.length();
		final int titleHeight = SWTUtilities.getFontCharacterHeight(gc);
		
		final int x = getXCoordinate(startPoint.getX(), gc);
		final int y = getYCoordinate(startPoint.getY(), gc);
		int tempXEnd = 0;
		int tempYEnd = 0;
		
		// If not end point is specified, compute the end point given the 
		// current font and title length
		if (endPoint.isUndefined()) {
			if (this.getCoordinateSystem().equals(
												  CoordinateSystemType.PIXEL)) {
				tempXEnd = startPoint.getX() + 
				titleLen * SWTUtilities.getFontCharacterWidth(gc) + 30;
				tempYEnd = startPoint.getY() + titleHeight + 15;
			} else {
				tempXEnd = startPoint.getX() + titleLen + 2;
				tempYEnd = startPoint.getY() + 3;
			}
			this.setEndLocation(new ChillPoint(
											   tempXEnd, 
											   tempYEnd,
											   this.getCoordinateSystem()));
		} else {
			tempXEnd = endPoint.getX();
			tempYEnd = endPoint.getY();
		}
		final int ex = getXCoordinate(tempXEnd, gc);
		final int ey = getYCoordinate(tempYEnd, gc);
		
		// If the whole title will not fit in the button size, truncate it.
		String titleForDisplay = title;
		titleLen *= SWTUtilities.getFontCharacterWidth(gc);
		
		if (!endPoint.isUndefined() && titleLen > Math.abs(ex - x)) {
			final int charWidth = SWTUtilities.getFontCharacterWidth(gc);
			final int maxChars = Math.abs(ex - x) / charWidth;
			titleForDisplay = title.substring(0, Math.max(maxChars, 0));
			titleLen = SWTUtilities.getFontCharacterWidth(gc) 
			* titleForDisplay.length();
		}
		
		// Draw the button
		gc.drawRoundRectangle(x, y, ex - x, ey - y, 8, 8);
		if (background != null) {
			gc.setBackground(background);
		}
		gc.fillRoundRectangle(x, y, ex - x, ey - y, 8, 8);
		
		setLastBounds(x, y, ex, ey);
		
		// Draw the button title
		final int titleX = ((x + ex) / 2) - (titleLen / 2);
		final int titleY = ((y + ey) / 2) - (titleHeight / 2);
		
		gc.drawText(titleForDisplay, titleX, titleY, SWT.TRANSPARENT);
		
		restoreGcSettings(gc);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#updateFieldsFromConfig()
	 */
	@Override
	protected void updateFieldsFromConfig() {
		super.updateFieldsFromConfig();
		final ButtonFieldConfiguration buttonConfig = (
												 ButtonFieldConfiguration)fieldConfig;
		title = buttonConfig.getText();
		ChillPoint end = buttonConfig.getEndCoordinate();
		if (end == null) {
			end = new ChillPoint(
								 ChillPoint.UNDEFINED, 
								 ChillPoint.UNDEFINED, 
								 getCoordinateSystem());
		}
		setEndLocation(end);
		actionString = buttonConfig.getActionString();
		final ChillColor foreground_cc = buttonConfig.getForeground();
		final ChillColor background_cc = buttonConfig.getBackground();
		if (foreground_cc != null) {
			if (foreground != null && !foreground.isDisposed()) {
				foreground.dispose();
				foreground = null;
			}
			foreground = ChillColorCreator.getColor(foreground_cc);
		}
		if (background_cc != null) {
			if (background != null && !background.isDisposed()) {
				background.dispose();
				background = null;
			}
			background = ChillColorCreator.getColor(background_cc);
		}
		actionType = buttonConfig.getActionType();
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TextSupport#setText(java.lang.String)
	 */
	@Override
    public void setText(final String title) {
		this.title = title;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TextSupport#getText()
	 */
	@Override
    public String getText() {
		return title;
	}
	
	/**
	 * Sets the button action string. The action must either be a command line
	 * to launch, or the name of another fixed view to launch, depending on 
	 * the value of getActionType().
	 * 
	 * @param as the action to take when the button is pressed
	 */
	public void setActionString(final String as) {
		actionString = as;
	}
	
	/**
	 * Gets the button action string. The action must either be a command line
	 * to launch, or the name of another fixed view to launch,  depending on 
	 * the value of getActionType().
	 * 
	 * @return the action string
	 */
	public String getActionString() {
		return actionString;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.OneColorSupport#setForeground(org.eclipse.swt.graphics.Color)
	 */
	@Override
    public void setForeground(final Color foreground) {
		this.foreground = foreground;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.OneColorSupport#getForeground()
	 */
	@Override
    public Color getForeground() {
		return foreground;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TwoColorSupport#setBackground(org.eclipse.swt.graphics.Color)
	 */
	@Override
    public void setBackground(final Color background) {
		this.background = background;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TwoColorSupport#getBackground()
	 */
	@Override
    public Color getBackground() {
		return background;
	}
	
	/**
	 * Sets the type of action to take when the button is pressed: LAUNCH_PAGE
	 * or LAUNCH_SCRIPT.
	 * 
	 * @param at the ActionType to set
	 */
	public void setActionType(final ActionType at) {
		actionType = at;
	}
	
	/**
	 * Gets the type of action to take when the button is pressed: LAUNCH_PAGE
	 * or LAUNCH_SCRIPT.
	 * 
	 * @return the ActionType to set
	 */
	public ActionType getActionType() {
		return actionType;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#isShapeMorphable()
	 */
	@Override
	public boolean isShapeMorphable() {
		return true;
	}
	
	/**
	 * Changes the drawing attributes of the button so it looks "pressed".
	 */
	public void setPressed() {
		saveBackground = background;
		setBackground(ChillColorCreator.getColor(new ChillColor(
																ColorName.DARK_GREY)));
	}
	
	/**
	 * Resets the drawing attributes of the button so it looks "released".
	 */
	public void setReleased() {
		if (saveBackground == background) {
			return;
		}
		if (getBackground() != null && !getBackground().isDisposed()) {
			getBackground().dispose();
		}
		setBackground(saveBackground);
	}
	
	/**
	 * Exercises the button action if not in edit mode.
	 */
	public void activate() {
		
		if (isEditMode || actionString == null) {
			return;
		}
		switch(actionType) {
			case LAUNCH_PAGE:
			    if (this.launchManager == null) {
			        throw new IllegalStateException("View launch manager not defined in button element");
			    }
			    launchManager.loadView(actionString, parent.getShell());

				break;
			case LAUNCH_SCRIPT:
				final ProcessLauncher pl = new ProcessLauncher();
				pl.setErrorHandler(new StderrLineHandler());
				pl.setOutputHandler(new StdoutLineHandler());
				try {
                    pl.launch(actionString);
				} catch (final Exception e) {
					e.printStackTrace();
				}
				break;
		}
	}
}
