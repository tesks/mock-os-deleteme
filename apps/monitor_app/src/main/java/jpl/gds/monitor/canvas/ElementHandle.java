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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * ElementHandle is an object used to represent the little selection "handles"
 * that appear on objects when they are selected in the fixed view canvas.
 *
 */
public class ElementHandle {

	/**
	 * The actual drawn size of the handles
	 */
	public static final int HANDLE_PIXEL_SIZE = 8;

	/**
	 * The larger size used to determine when the mouse is in range
	 */
	public static final int HANDLE_GRAB_SIZE = 12;

	/**
	 * Color of the handles.
	 */
	private static Color selectionColor	= 
	    ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.BLUE));

	private int centerX = -1;
	private int centerY = -1;

	/**
	 * Creates an ElementHandle centered on the given absolute x,y pixel 
	 * coordinate.
	 * 
	 * @param x the x coordinate of the handle
	 * @param y the y coordinate of the handle
	 */
	public ElementHandle(int x, int y) {
		centerX = x;
		centerY = y;
	}

	/**
	 * Gets the x coordinate of this handle.
	 * 
	 * @return the x coordinate
	 */
	public int getCenterX() {
		return centerX;
	}

	/**
	 * Sets the x coordinate of this handle.
	 * 
	 * @param centerX the x coordinate to set
	 */
	public void setCenterX(int centerX) {
		this.centerX = centerX;
	}

	/**
	 * Gets the y coordinate of this handle.
	 * 
	 * @return the y coordinate
	 */
	public int getCenterY() {
		return centerY;
	}

	/**
	 * Sets the y coordinate of this handle.
	 * 
	 * @param centerY the y coordinate
	 */
	public void setCenterY(int centerY) {
		this.centerY = centerY;
	}

	/**
	 * Draws the actual selection handle on the given Graphics Context (GC) 
	 * object.
	 * 
	 * @param gc the GC to draw on
	 */
	public void drawHandle(GC gc) {
		if (centerX == -1) {
			return;
		}
		Color saveFore = gc.getForeground();
		Color saveBack = gc.getBackground();
		gc.setForeground(selectionColor);
		gc.setBackground(selectionColor);
		int half = HANDLE_PIXEL_SIZE / 2;
		gc.fillRectangle(Math.max(0, centerX - half), 
		        Math.max(0, centerY - half), 
		        HANDLE_PIXEL_SIZE, HANDLE_PIXEL_SIZE);
		gc.setForeground(saveFore);
		gc.setBackground(saveBack);
	} 

	/**
	 * Determines if the given absolute x,y coordinate falls on this handle.
	 * 
	 * @param pt the x,y point to check
	 * 
	 * @return true if the point falls on or very near this handle; false if 
	 * not
	 */
	public boolean containsPoint(Point pt) {
		if (centerX == -1) {
			return false;
		}
		int half = HANDLE_GRAB_SIZE / 2;
		return (pt.x >= Math.max(0,centerX - half) && 
		        pt.x <= Math.max(0,centerX + half)) &&
		(pt.y >= centerY - half && pt.y <= centerY + half); 
	}
}
