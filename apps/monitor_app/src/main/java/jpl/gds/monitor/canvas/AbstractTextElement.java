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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;

import jpl.gds.monitor.canvas.support.FontSupport;
import jpl.gds.monitor.canvas.support.FormatSupport;
import jpl.gds.monitor.canvas.support.StaleSupport;
import jpl.gds.monitor.canvas.support.TransparencySupport;
import jpl.gds.monitor.canvas.support.TwoColorSupport;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.AbstractTextFieldConfiguration;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * This fixed view Canvas Element is an abstract simple text field. It can be
 * extended to represent more special purpose text fields. It supports
 * foreground and background colors, font, a formatter, and transparency.
 * 
 */
public abstract class AbstractTextElement extends SingleCoordinateCanvasElement
implements TwoColorSupport, FontSupport, TransparencySupport, FormatSupport {
	
	/** On screen NO_DATA indicator for text elements **/
	protected static final String NO_DATA = "---";
	/** Default time formatter for time elements **/
	protected static final String DEFAULT_TIME_FORMAT = "yyyy-DDD'T'HH:mm:ss";
	/** Default time formatter for LST elements **/
	protected static final String DEFAULT_LST_FORMAT = "SOL-xxxx'M'HH:mm:ss";
	/** Default time formatter for SCLK elements **/
	protected static final String DEFAULT_SCLK_FORMAT = "%s";
	
	/**
	 * The default selection priority for this CanvasElement
	 */
	private static final int SELECTION_PRIORITY = 1;
	
	/** Text foreground color. **/
	protected Color foreground;
	/** Text background color **/
	protected Color background;
	/** Text font **/
	protected Font font;
	/** Actual text to display **/
	protected String text;
	/** Formatter for the displayed text **/
	protected String format;
	/** Indicates whether text has transparent background **/
	protected boolean transparent;
	/** Indicates whether text is displayed in reverse video. **/
	protected boolean reverse;
	/** Indicates whether Font has been changed. **/
	protected boolean usesDefaultFont = true;
	/** On screen NO_DATA indicator current value. **/
	protected String noDataIndicator = NO_DATA;
	/** Print formatter */
    protected SprintfFormat       printFormatter      = new SprintfFormat();
	
	/**
	 * Creates an AbstractTextElement with the given parent Canvas.
	 * 
	 * @param parent the parent Canvas widget
	 */
	public AbstractTextElement(final Canvas parent) {
		super(parent, FixedFieldType.TEXT);
		this.setSelectionPriority(SELECTION_PRIORITY);
	}
	
	/**
	 * Creates an AbstractTextElement with the given parent Canvas and 
	 * field type.
	 * 
	 * @param parent the parent Canvas widget
	 * @param type the enumeration field type (box, line, text, image,
	 * channel, button, time, header)
	 */
	public AbstractTextElement(final Canvas parent, final FixedFieldType type) {
		super(parent, type);
		this.setSelectionPriority(SELECTION_PRIORITY);
	}
	
	/**
	 * Creates an AbstractTextElement with the given fixed view field
	 * configuration and parent canvas.
	 * 
	 * @param parent the parent Canvas widget
	 * 
	 * @param textConfig the FixedFieldConfiguration object for this element
	 *            from the perspective
	 */
	public AbstractTextElement(
							   final Canvas parent, final IFixedFieldConfiguration textConfig) {
		super(parent, textConfig);
		updateFieldsFromConfig();
		this.setSelectionPriority(SELECTION_PRIORITY);
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
	 * @see jpl.gds.monitor.canvas.support.OneColorSupport#setForeground(org.eclipse.swt.graphics.Color)
	 */
	@Override
	public void setForeground(final Color foreground) {
		this.foreground = foreground;
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
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TwoColorSupport#setBackground(org.eclipse.swt.graphics.Color)
	 */
	@Override
	public void setBackground(final Color background) {
		this.background = background;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.FontSupport#getFont()
	 */
	@Override
	public Font getFont() {
		return font;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.FontSupport#setFont(org.eclipse.swt.graphics.Font)
	 */
	@Override
	public void setFont(final Font font) {
		this.font = font;
	}
	
	/**
	 * Gets the text to be drawn by this text object.
	 * 
	 * @return text string
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * Gets the text drawn by this text object, formatted per the
	 * configuration.
	 * 
	 * @return text string
	 */
	public String getFormattedText() {
		if (format == null) {
			return text;
		} else {
			return printFormatter.anCsprintf(format, text);
		}
	}
	
	/**
	 * Sets the text drawn by this text object.
	 * 
	 * @param text the text string to set
	 */
	public void setText(final String text) {
		this.text = text;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.FormatSupport#getFormat()
	 */
	@Override
	public String getFormat() {
		return format;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.FormatSupport#setFormat(java.lang.String)
	 */
	@Override
	public void setFormat(final String format) {
		this.format = format;
		noDataIndicator = null;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TransparencySupport#setTransparent(boolean)
	 */
	@Override
	public void setTransparent(final boolean transparent) {
		this.transparent = transparent;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.TransparencySupport#isTransparent()
	 */
	@Override
	public boolean isTransparent() {
		return transparent;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.CanvasElement#draw(org.eclipse.swt.graphics.GC)
	 */
	@Override
	public void draw(final GC gc) {
		
	    if(!displayMe && this.getFieldConfiguration().getCondition() != null) {
            return;
        }
	    
		saveGcSettings(gc);
		
		if (getText() == null) {
			return;
		}
		final int x = getXCoordinate(startPoint.getX(), gc);
		final int y = getYCoordinate(startPoint.getY(), gc);
		
		if (font != null) {
			gc.setFont(font);
		}	
		if (foreground != null) {
			gc.setForeground(foreground);
		}
		if (background != null) {
			gc.setBackground(background);
		}
		
		
		if(reverse) {
			final Color save = gc.getBackground();
			gc.setBackground(gc.getForeground());
			gc.setForeground(save);
		} 
		
		final String newText = getFormattedText();
		
		if (this instanceof StaleSupport) {
			if (((StaleSupport)this).isStale()) {	
				gc.setAlpha(100);
			}
		}
		
		//textExtent fixes Linux bug in which background color wouldn't 
		//fill same area as text
		gc.textExtent(text);
		
		gc.drawText(newText, x, y, (transparent ? SWT.DRAW_TRANSPARENT : 0));
		final int wx = SWTUtilities.getFontCharacterWidth(gc) * newText.length();
		final int wy = SWTUtilities.getFontCharacterHeight(gc);
		setLastBounds(x, y, x + wx, y + wy);
		
		restoreGcSettings(gc);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.SingleCoordinateCanvasElement#updateFieldsFromConfig()
	 */
	@Override
	protected void updateFieldsFromConfig() {
		super.updateFieldsFromConfig();
		
		final AbstractTextFieldConfiguration textConfig = (
													 AbstractTextFieldConfiguration)fieldConfig;
		
		if(textConfig.getForeground() != null) {
			if (foreground != null && !foreground.isDisposed()) {
				foreground.dispose();
				foreground = null;
			}
			foreground = ChillColorCreator.getColor(
													textConfig.getForeground());
		}
		if(textConfig.getBackground() != null) {
			if (background != null && !background.isDisposed()) {
				background.dispose();
				background = null;
			}
			background = ChillColorCreator.getColor(
													textConfig.getBackground());
		}
		if(textConfig.getFont() != null)
		{
			//if font size is negative, set it to absolute value
		    if(textConfig.getFont().getSize()<0) {
			    textConfig.setFont(new ChillFont(
												 textConfig.getFont().getFace(), 
												 Math.abs(textConfig.getFont().getSize()), 
												 textConfig.getFont().getStyle()));
			}
		    if (this.font != null && !this.font.isDisposed()) {
		    	this.font.dispose();
		    	this.font = null;
		    }
		    this.setFont(ChillFontCreator.getFont(textConfig.getFont()));
			reverse = textConfig.getFont().getReverseFlag();
		}
		if(textConfig.getFormat() != null)
		{
			this.setFormat(textConfig.getFormat());
		}
		this.setTransparent(textConfig.isTransparent());
	}
	
	/**
	 * Gets the proper NO DATA indicator for this field.
	 * @return the text that represents "no data" for this field
	 */
	public String getNoDataIndicator() {
		return NO_DATA;
	}
}
