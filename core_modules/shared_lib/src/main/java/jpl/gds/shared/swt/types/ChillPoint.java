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
 * This class abstracts the notion of an X,Y coordinate on a display using
 * either a character or pixel coordinate system. What this point is relative
 * to is left to the class that employs it.
 * 
 *
 */
public class ChillPoint {

    /**
     * Negative integer represents an undefined coordinate point
     */
    public static final int UNDEFINED = -1;
    
    private int x = UNDEFINED;
    private int y = UNDEFINED;
    private CoordinateSystemType coordinateSystem;

    /**
     * Creates a new ChillPoint object.
     * 
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param coordType
     *            the coordinate system
     */
    public ChillPoint(final int x, final int y,
            final CoordinateSystemType coordType) {
        this.x = x;
        this.y = y;
        this.coordinateSystem = coordType;
    }

    /**
     * Indicates that this data point has undefined coordinates.
     * 
     * @return true if the point is undefined
     */
    public boolean isUndefined() {
        return this.x == UNDEFINED;
    }

    /**
     * Gets the X coordinate, which must be interpreted using the proper
     * coordinate system.
     * 
     * @return X coordinate, or UNDEFINED if it is undefined
     */
    public int getX() {
        return this.x;
    }

    /**
     * Sets the X coordinate, which must match the established coordinate
     * system.
     * 
     * @param x
     *            the X coordinate, or UNDEFINED to make it undefined
     */
    public void setX(final int x) {
        this.x = x;
    }

    /**
     * Gets the Y coordinate, which must be interpreted using the proper
     * coordinate system.
     * 
     * @return Y coordinate, or UNDEFINED if it is undefined
     */
    public int getY() {
        return this.y;
    }

    /**
     * Sets the Y coordinate, which must match the established coordinate
     * system.
     * 
     * @param y
     *            the Y coordinate, or UNDEFINED to make it undefined
     */
    public void setY(final int y) {
        this.y = y;
    }

    /**
     * Gets the coordinate system for this point.
     * 
     * @return CoordinateSystemType
     */
    public CoordinateSystemType getCoordinateSystem() {
        return this.coordinateSystem;
    }

    /**
     * Sets the coordinate system for this point.
     * 
     * @param coordinateSystem
     *            the CoordinateSystemType to set
     */
    public void setCoordinateSystem(final CoordinateSystemType coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
    }
}
