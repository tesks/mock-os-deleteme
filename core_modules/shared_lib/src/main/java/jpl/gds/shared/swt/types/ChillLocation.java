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
package jpl.gds.shared.swt.types;

/**
 * Represents the physical location of a GUI item on a display, assumed in units
 * of pixels. What this location is relative to is left to the class that
 * employs it.
 * 
 */
public class ChillLocation {
    private int xPosition;
    private int yPosition;

    /**
     * XML tag name for a y coordinate element
     */
    public static final String Y_LOCATION_TAG = "yLocation";

    /**
     * XML tag name for an x coordinate element
     */
    public static final String X_LOCATION_TAG = "xLocation";

    /**
     * 
     * Creates an instance of ChillLocation at pixel location 0,0.
     */
    public ChillLocation() {
        // do nothing
    }

    /**
     * Creates an instance of ChillLocation at pixel location x,y, relative to
     * the top left corner of the screen.
     * 
     * @param x the x position of the display
     * @param y the y position of the display
     */
    public ChillLocation(final int x, final int y) {
        this.setXPosition(x);
        this.setYPosition(y);
    }

    /**
     * Gets the x coordinate
     * @return the x coordinate of the location
     */
    public int getXPosition() {
        return this.xPosition;
    }

    /**
     * Sets the x coordinate of the location, in pixels from the upper left
     * corner of the screen.
     * 
     * @param position
     *            The x position to set.
     */
    public void setXPosition(final int position) {
        this.xPosition = position;
    }

    /**
     * Gets the y coordinate
     * @return the y coordinate of the location
     */
    public int getYPosition() {
        return this.yPosition;
    }

    /**
     * Sets the y coordinate of the location, in pixels from the upper left
     * corner of the screen.
     * 
     * @param position
     *            The y position to set.
     */
    public void setYPosition(final int position) {
        this.yPosition = position;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object compare) {
        if (compare == null || !(compare instanceof ChillLocation)) {
            return false;
        }
        final ChillLocation compareLoc = (ChillLocation) compare;
        return compareLoc.xPosition == this.xPosition
                && compareLoc.yPosition == this.yPosition;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.xPosition + this.xPosition;
    }

    /**
     * Gets an XML representation of the location
     * 
     * @return an XML string representing the current position settings
     */
    public String toXml() {
        return "   <" + X_LOCATION_TAG + ">" + this.getXPosition() + "</"
                + X_LOCATION_TAG + ">\n" + "   <" + Y_LOCATION_TAG + ">"
                + this.getYPosition() + "</" + Y_LOCATION_TAG + ">\n";
    }

}
