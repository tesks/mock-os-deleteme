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
package jpl.gds.shared.swt;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import jpl.gds.shared.swt.types.ChillColor;

/**
 * 
 * ChillColorCreator creates SWT color objects from ChillColor objects.
 * 
 *
 */
public class ChillColorCreator {
    private static Display display = Display.getDefault();

    /**
     * Create an AWT color object according to the specified ChillColor input.
     * 
     * @param color
     *            the ChillColor to get color values from
     * @return AWT Color object
     */
    public static java.awt.Color getAwtColor(final ChillColor color) {
        return new java.awt.Color(color.getRed(), color.getGreen(), color
                .getBlue());
    }

    /**
     * Create an SWT color that is brighter than, but has the same hue as, the
     * given ChillColor.
     * 
     * @param color
     *            the original ChillColor
     * @return an SWT Color object brighter than color that was passed in
     */
    public static Color getBrighterColor(final ChillColor color) {
        final int newRed = Math.min(255, color.getRed() + 25);
        final int newGreen = Math.min(255, color.getGreen() + 25);
        final int newBlue = Math.min(255, color.getBlue() + 25);
        return new Color(display, newRed, newGreen, newBlue);
    }

    /**
     * Create a color object according to the specified ChillColor input.
     * 
     * @param color
     *            the ChillColor to get color values from
     * @return SWT Color object
     */
    public static Color getColor(final ChillColor color) {

        // if an RGB value is greater than 255, set it to 255
        if (color.getRed() > 255) {
            color.setRed(255);
        }
        if (color.getGreen() > 255) {
            color.setGreen(255);
        }
        if (color.getBlue() > 255) {
            color.setBlue(255);
        }

        // if an RGB value is less than 0, set it to 0
        if (color.getRed() < 0) {
            color.setRed(0);
        }
        if (color.getGreen() < 0) {
            color.setGreen(0);
        }
        if (color.getBlue() < 0) {
            color.setBlue(0);
        }
        return new Color(display, color.getRed(), color.getGreen(), color
                .getBlue());
    }

    /**
     * Create an SWT color that is darker than, but has the same hue as, the
     * given ChillColor.
     * 
     * @param color
     *            the original ChillColor
     * @return an SWT Color object darker than the color that was passed in
     */
    public static Color getDarkerColor(final ChillColor color) {
        final int newRed = Math.max(0, color.getRed() - 25);
        final int newGreen = Math.max(0, color.getGreen() - 25);
        final int newBlue = Math.max(0, color.getBlue() - 25);
        return new Color(display, newRed, newGreen, newBlue);
    }
}
