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
 * Encapsulates the size of a GUI item on the display.
 * Capable of generating an XML string representing the size.
 * 
 *
 */
public class ChillSize {
    private int xWidth;
    private int yHeight;

    /**
     * XML tag name for a display height element
     */
    public static final String Y_HEIGHT_TAG = "yHeight";

    /**
     * XML tag name for a display width element
     */
    public static final String X_WIDTH_TAG = "xWidth";

    /**
     * Creates an instance of ChillSize with 0 height and width.
     */
    public ChillSize() {
        // do nothing
    }

    /**
     * Creates an instance of ChillSize.
     * 
     * @param x
     *            the display width in pixels
     * @param y
     *            the display height in pixels
     */
    public ChillSize(final int x, final int y) {
        this.setXWidth(x);
        this.setYHeight(y);
    }

    /**
     * Gets the width of the display in pixels
     * 
     * @return Returns the display width in pixels.
     */
    public int getXWidth() {
        return this.xWidth;
    }

    /**
     * Sets the display width in pixels.
     * 
     * @param width
     *            The width to set.
     */
    public void setXWidth(final int width) {
        this.xWidth = width;
    }

    /**
     * Gets the height of the display in pixels
     * 
     * @return Returns the display height in pixels.
     */
    public int getYHeight() {
        return this.yHeight;
    }

    /**
     * Sets the display height in pixels.
     * 
     * @param height
     *            The height to set.
     */
    public void setYHeight(final int height) {
        this.yHeight = height;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object compare) {
        if (compare == null || !(compare instanceof ChillSize)) {
            return false;
        }
        final ChillSize compareLoc = (ChillSize) compare;
        return compareLoc.xWidth == this.xWidth
                && compareLoc.yHeight == this.yHeight;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.xWidth + this.yHeight;
    }

    /**
     * Gets an XML representation for this display's width and height
     * 
     * @return an XML string representing the current size settings
     */
    public String toXml() {
        return "   <" + X_WIDTH_TAG + ">" + this.getXWidth() + "</"
                + X_WIDTH_TAG + ">\n" + "   <" + Y_HEIGHT_TAG + ">"
                + this.getYHeight() + "</" + Y_HEIGHT_TAG + ">\n";
    }
}
