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

import jpl.gds.perspective.config.ViewType;

/**
 * ViewTriple is used to uniquely identify a view cell in a grid view in the perspective. 
 * It identifies the location of the cell in the grid and the type of view it contains.
 *
 */
public class ViewTriple {
	/**
	 * XML view triple element name
	 */
	public static final String VIEW_TRIPLE_CONFIG = "ViewTriple";
	
    private ViewType viewType;
    private int xLoc;
    private int yLoc;
    private int number;
    
    /**
     * Creates a new ViewTriple object.
     * @param type the ViewType contained in the grid view cell this object represents
     * @param xCoord the X coordinate of the cell in the grid
     * @param yCoord the Y coordinate of the cell in the grid
     * @param num the unique number of the cell in the grid view
     */
    public ViewTriple(ViewType type, int xCoord, int yCoord, int num) {
    	this.viewType = type;
    	this.xLoc = xCoord;
    	this.yLoc = yCoord;
    	this.number = num;
    }
    
	/**
	 * Gets the ViewType for this ViewTriple.
	 * @return the ViewType
	 */
	public ViewType getViewType() {
		return this.viewType;
	}
	
	/**
	 * Sets the ViewType for this ViewTriple.
	 * @param viewType the ViewType to set
	 */
	public void setViewType(ViewType viewType) {
		this.viewType = viewType;
	}
	
	/**
	 * Gets the X location in the grid.
	 * @return X index
	 */
	public int getXLoc() {
		return this.xLoc;
	}
	/**
	 * Sets the X location in the grid.
	 * @param loc X index
	 */
	public void setXLoc(int loc) {
		this.xLoc = loc;
	}
	
	/**
	 * Gets the Y location in the grid.
	 * @return the Y index
	 */
	public int getYLoc() {
		return this.yLoc;
	}
	
	/**
	 * Sets the Y location in the grid.
	 * @param loc the Y index
	 */
	public void setYLoc(int loc) {
		this.yLoc = loc;
	}
    
	/**
	 * Returns an XML string representation of this ViewTriple object.
	 * @return XML text
	 */
	public String toXML() {
		return "<ViewTriple" + this.number + ">" + 
		    this.viewType.getValueAsString() + "," +
		    this.xLoc + "," +
		    this.yLoc + "</ViewTriple" + this.number + ">";
	}

	/**
	 * Gets the unique number of this ViewTriple (cell) in the grid view.
	 * @return the cell number
	 */
	public int getNumber() {
		return this.number;
	}

	/**
	 * Sets the unique number of this ViewTriple (cell) in the grid view.
	 * @param number the cell number
	 */
	public void setNumber(int number) {
		this.number = number;
	}
    
	/**
     * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
    public boolean equals(Object compare) {
		if (compare == null) {
			return false;
		}
		if (!(compare instanceof ViewTriple)) {
			return false;
		}
		ViewTriple compareTriple = (ViewTriple)compare;
		if (compareTriple.viewType.equals(this.viewType) &&
				compareTriple.xLoc == this.xLoc &&
				compareTriple.yLoc == this.yLoc &&
				compareTriple.number == this.number) {
			return true;
		}
		return false;
	}
	
	/**
     * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
    public int hashCode() {
	    return this.viewType.getValueAsInt() + this.xLoc +
	    this.yLoc + this.number;
	}
}
