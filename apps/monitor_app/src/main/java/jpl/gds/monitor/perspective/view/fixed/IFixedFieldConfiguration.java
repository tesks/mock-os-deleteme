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
package jpl.gds.monitor.perspective.view.fixed;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.ChillPoint;
import jpl.gds.shared.swt.types.CoordinateSystemType;

public interface IFixedFieldConfiguration {

    // Tags used in the XML representation
    /**
     * XML x start attribute name
     */
    public static final String X_START_TAG = "xStart";
    /**
     * XML y start attribute name
     */
    public static final String Y_START_TAG = "yStart";
    /**
     * XML conditions attribute name
     */
    public static final String CONDITIONS_TAG = "conditions";

    /**
     * Gets the application context this configuration was created with.
     * 
     * @return ApplicationContext
     */
    public ApplicationContext getApplicationContext();

    /**
     * Retrieves the coordinate system used to position this field
     * in the fixed layout view.
     * 
     * @return the CoordinateSystemType
     */
    public CoordinateSystemType getCoordinateSystem();

    /**
     * Sets the coordinate system used to position this field in the
     * fixed layout view.
     * 
     * @param locationType the CoordinateSystemType (character or pixel)
     */
    public void setCoordinateSystem(CoordinateSystemType locationType);

    /**
     * Gets the type of this field configuration: line, text, image, etc.
     * 
     * @return the FixedFieldType
     */
    public FixedFieldType getType();

    /**
     * Sets the type of this field configuration: line, text, image, etc.
     * 
     * @param type the FixedFieldType to set
     */
    public void setType(FixedFieldType type);

    /**
     * Gets the X coordinate of this field in the layout. Note that this coordinate must be
     * interpreted using the proper coordinate system. For objects that are defined by two points,
     * this is the X for the first point.
     * 
     * @see #getCoordinateSystem()
     * 
     * @return the X coordinate
     */
    public ChillPoint getStartCoordinate();

    /**
     * Sets the X coordinate of this field in the layout. Note that this coordinate must be
     * interpreted using the proper coordinate system. For objects that are defined by two points,
     * this is the X for the first point.
     * 
     * @see #setCoordinateSystem(CoordinateSystemType loc)
     * 
     * @param start the X coordinate
     */
    public void setStartCoordinate(ChillPoint start);

    /**
     * Sets the X coordinate of this field in the layout. Note that this coordinate must be
     * interpreted using the proper coordinate system. For objects that are defined by two points,
     * this is the X for the first point. This method should be used only by the perspective parser.
     * 
     * @see #setCoordinateSystem(CoordinateSystemType loc)
     * 
     * @param start the X coordinate
     */
    public void setXStart(int start);

    /**
     * Sets the Y coordinate of this field in the layout. Note that this coordinate must be
     * interpreted using the proper coordinate system. For objects that are defined by two points,
     * this is the Y for the first point. This method should be used only by the perspective parser.
     * 
     * @see #setCoordinateSystem(CoordinateSystemType loc)
     * 
     * @param start the Y coordinate
     */
    public void setYStart(int start);

    /**
     * Indicates whether this field is static, in other words, does not change once initially
     * drawn on the screen.
     * 
     * @return true if field is static, false if dynamic
     */
    public boolean isStatic();

    /**
     * Sets the flag indicating whether this field is static, in other words, does not change once 
     * initially drawn on the screen.
     * 
     * @param isStatic true if field is static, false if dynamic
     */
    public void setStatic(boolean isStatic);

    /**
     * Indicates conditions for drawing this field; may be any combination 
     * of one or more conditionIds and AND, OR, XOR, NOT operators
     * 
     * @return condition for drawing this field
     */
    public ICompoundCondition getCondition();

    /**
     * Sets conditions for drawing this field; may be any combination 
     * of one or more conditionIds and AND, OR, XOR, NOT operators
     * 
     * @param condition for drawing this field
     */
    public void setCondition(ICompoundCondition condition);

    /**
     * Retrieves the XML perspective representation of this configuration object.
     * 
     * @return XML text
     */
    public String toXML();

    /**
     * Retrieves the main XML element/tag for this field configuration.
     * 
     * @return XML element name
     */
    public String getFieldTag();

    /**
     * Sets the default colors for fixed view elements
     * 
     *  @param background default background color
     *  @param foreground default foreground color
     *  @param font default font (face, size, style, reverse video flag)
     */
    public void setDefaults(ChillColor background, ChillColor foreground, ChillFont font);

    /**
     * Gets the fixed view default background color. If an element has no foreground color set,
     * this color will be used.
     * 
     * @return defaultBackground If an element has no background set, it inherits this color
     */
    public ChillColor getDefaultBackground();

    /**
     * Gets the fixed view default foreground color.  If an element has no foreground color set,
     * this color will be used.
     * 
     * @return defaultForeground If an element has no foreground set, it inherits this color
     */
    public ChillColor getDefaultForeground();

    /**
     * Gets the fixed view default font. If an element has no font set, this font will be used.
     * 
     * @return defaultFont The font that is inherited if an element has no font set
     */
    public ChillFont getDefaultFont();

    /**
     * Gets the fixed view default reverse video flag.
     * 
     * @return defaultReverse Determines if the foreground and background color should be switched
     */
    public boolean getDefaultReverse();

    /**
     * Sets the default configuration for this field as required by the fixed view 
     * interactive builder.
     * 
     * @param coordSystem the coordinate system type (character or pixel based)
     */
    public void setBuilderDefaults(CoordinateSystemType coordSystem);

    /**
     * Copies attributes of this FieldConfiguration to the input configuration object.
     * 
     * @param newConfig FixedFieldConfiguration that will be copied
     */
    public void copyConfiguration(IFixedFieldConfiguration newConfig);

    /**
     * Notifies position listeners that the field's position has changed. This method is
     * actually never invoked internally, but is used by external classes.
     */
    public void notifyPositionListeners();

    /**
     * Adds a position change listener to this field configuration.
     * 
     * @param l the listener to add
     */
    public void addPositionChangeListener(PositionChangeListener l);

    /**
     * Removes a position change listener from this field configuration.
     * 
     * @param l the listener to remove
     */
    public void removePositionChangeListener(PositionChangeListener l);

    /**
     * Converts the coordinate system used by this header configuration so that all
     * sub-fields use the proper coordinates.
     * 
     * @param coordSystem the coordinate system to change to
     * @param charWidth pixel width of a character
     * @param charHeight pixel height of a character
     */
    public void convertCoordinates(CoordinateSystemType coordSystem, int charWidth, int charHeight);

}