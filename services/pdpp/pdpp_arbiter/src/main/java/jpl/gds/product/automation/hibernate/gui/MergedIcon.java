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
package jpl.gds.product.automation.hibernate.gui;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * Merges two icons together.  The front icon should be bigger than back icon.  Back icon
 * will be set to display half way down at gridx = 0.
 * 
 * MPCS-8182 - 08/11/16 - Added to and updated for AMPCS.
 */
public class MergedIcon implements Icon {
	private Icon back;
	private Icon front;
	
	/**
	 * Merges the two supplied icons into a single icon
	 * @param back the icon to be the base image
	 * @param front The icon to be layered on top
	 */
	public MergedIcon(Icon back, Icon front) {
		this.back = back;
		this.front = front;
	}

	/**
	 * Get the merged icon's height
	 * 
	 * @return an int specifying the fixed height of the icon.
	 */
	@Override
	public int getIconHeight() {
		return back.getIconHeight();
	}

	/**
	 * Get the merged icon's width
	 * 
	 * @return an int specifying the fixed width of the icon.
	 */
	@Override
	public int getIconWidth() {
		return back.getIconWidth() + front.getIconWidth();
	}

	
	/**
	 * The front Icon will be painted normally, and the back Icon will be put at
	 * the bottom right side where the middle is at the end of the first.
	 * 
	 * @param c
	 *            the Component that the icon will be drawn on
	 * @param g
	 *            the Graphics that the icon will be drawn on
	 * @param x
	 *            the horizontal position of the icon
	 * @param y
	 *            the vertical position of the icon
	 */
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		int yFront = y + back.getIconHeight() - front.getIconHeight();
		
		back.paintIcon(c, g, x, y);
		front.paintIcon(c, g, x, yFront);
	}
}