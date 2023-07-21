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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.swt.types.ChillFont.FontSize;

/**
 * 
 * ChillFontCreator creates SWT and AWT font objects from ChillFont objects.
 * 
 *
 */
public class ChillFontCreator {
    private static Display display = Display.getDefault();

    /**
     * Creates an AWT font object from ChillFont input.
     * 
     * @param fontConfig
     *            the ChillFont object containing font settings
     * @return an AWT font object
     */
    public static java.awt.Font getAwtFont(final ChillFont fontConfig) {
        int style = java.awt.Font.PLAIN;
        switch (fontConfig.getStyle()) {
        case SWT.NONE:
            style = java.awt.Font.PLAIN;
            break;
        case SWT.BOLD:
            style = java.awt.Font.BOLD;
            break;
        case SWT.ITALIC:
            style = java.awt.Font.ITALIC;
            break;
        }
        return new java.awt.Font(fontConfig.getFace(), style, fontConfig
                .getSize());
    }

    /**
     * Creates an SWT Font object with the given point size and default
     * face/style.
     * 
     * @param size
     *            a FontSize object
     * @return an SWT Font object
     */
    public static Font getDefaultFont(final FontSize size) {
        final FontData groupFontData = new FontData(ChillFont.DEFAULT_FACE,
                size.getPointSize(), ChillFont.DEFAULT_STYLE);
        return new Font(display, groupFontData);
    }
    
    /**
     * Creates an SWT Font object with the given point size and default
     * monospace face/style.
     * 
     * @param size
     *            a FontSize object
     * @return an SWT Font object
     * 
     */
    public static Font getDefaultMonospaceFont(final FontSize size) {
        final FontData groupFontData = new FontData(ChillFont.MONOSPACE_FACE,
                size.getPointSize(), ChillFont.DEFAULT_STYLE);
        return new Font(display, groupFontData);
    }

    /**
     * Creates an SWT font object from ChillFont input.
     * 
     * @param fontConfig
     *            the ChillFont object containing font settings
     * @return an SWT font object
     */
    public static Font getFont(final ChillFont fontConfig) {
        final FontData groupFontData = new FontData(fontConfig.getFace(),
                fontConfig.getSize(), fontConfig.getStyle());
        return new Font(display, groupFontData);
    }

    /**
     * Maps an AWT font style to an SWT font style.
     * 
     * @param awtStyle
     *            the AWT style to map
     * @return the SWT style constant
     */
    public static int mapAwtStyleToSwtStyle(final int awtStyle) {
        switch (awtStyle) {
        case java.awt.Font.PLAIN:
            return SWT.NONE;
        case java.awt.Font.BOLD:
            return SWT.BOLD;
        case java.awt.Font.ITALIC:
            return SWT.ITALIC;
        default:
            return SWT.None;
        }
    }
}
