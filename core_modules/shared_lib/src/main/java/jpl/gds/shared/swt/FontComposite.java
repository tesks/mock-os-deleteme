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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * Font Composite that allows the user to select only font size from a
 * pre-defined list of sizes. ChillFont is used to represent font.
 * 
 *
 */
public class FontComposite {
    
    private static String CUSTOM_NAME = "CUSTOM";

    private ChillFont currentFont;
    private Composite mainComposite;
    private final Composite parent;
    private Combo dataFontCombo;
    private final String label;
    private ChillFont customFont;
    private Tracer          trace;

    /**
     * Creates a FontComposite with the given parent and display label.
     * 
     * @param parent
     *            parent Composite widget
     * @param label
     *            String to label font prompt with
     * @param trace
     *            The Tracer logger
     */
    public FontComposite(final Composite parent, final String label, Tracer trace) {
        this.parent = parent;
        this.label = label;
        this.trace = trace;
        createControls();
    }

    private void createControls() {

        mainComposite = new Composite(parent, SWT.NONE);
        mainComposite.setLayout(new GridLayout(2, false));

        final Label dataFontLabel = new Label(mainComposite, SWT.NONE);
        dataFontLabel.setText(label + ": ");
        dataFontCombo = new Combo(mainComposite, SWT.BORDER);
        final ChillFont.FontSize[] sizes = ChillFont.FontSize.values();
        for (int i = 0; i < sizes.length; i++) {
            dataFontCombo.add(sizes[i].name());
        }

        dataFontCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final String sizeStr = dataFontCombo.getText();
                    /* Fix issue with fonts not of standard size.
                     * If there is a custom size selection, restore the saved custom font. */
                    if (!sizeStr.equals(CUSTOM_NAME)) {
                        final ChillFont.FontSize size = Enum.valueOf(
                                ChillFont.FontSize.class, sizeStr);
                        final ChillFont newFont = new ChillFont();
                        newFont.setFace(currentFont.getFace());
                        newFont.setStyle(currentFont.getStyle());
                        newFont.setSize(size);
                        currentFont = newFont;
                    } else {
                        currentFont = customFont;
                    }
                } catch (final Exception eE) {
                    trace.error(
                                    "dataFontCombo listener handled unknown " +
                                    "and unexpected exception in " +
                                    "FontComposite.java");
                    eE.printStackTrace();
                }
            }
        });
    }

    /**
     * Gets the main composite object.
     * 
     * @return Composite object
     */
    public Composite getComposite() {
        return mainComposite;
    }

    /**
     * Gets the currently selected font
     * @return Returns the current font selected by the user.
     */
    public ChillFont getCurrentFont() {
        return currentFont;
    }

    /**
     * Sets the current font
     * 
     * @param font
     *            The current font to set.
     */
    public void setCurrentFont(final ChillFont font) {
        currentFont = font;
        String sizeName = ChillFont.getSizeName(font
                .getSize());
        if (sizeName == null) {
            /* This font does not match the standard sizes. Someone 
             * changed the font config. Save this "custom" font and
             * add a "CUSTOM" entry to the font list.
             */
            sizeName = CUSTOM_NAME;
            dataFontCombo.add("CUSTOM");
            customFont = font;
        }
        dataFontCombo.setText(sizeName);
    }
}
