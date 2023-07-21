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
package jpl.gds.monitor.guiapp.gui.views.nattable;

import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.selection.config.DefaultSelectionStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.SelectionStyleLabels;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.swt.graphics.Color;

import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillColor.ColorName;

/**
 * A custom selection style configuration for a NAT table, which controls
 * how selected rows are rendered..
 */
public class MonitorSelectionStyleConfiguration extends
        DefaultSelectionStyleConfiguration {

    private static final Color BLUE = ChillColorCreator
            .getColor(new ChillColor(ColorName.BLUE));
    private static final Color WHITE = ChillColorCreator
            .getColor(new ChillColor(ColorName.WHITE));

    /**
     * {@inheritDoc}
     * @see org.eclipse.nebula.widgets.nattable.selection.config.DefaultSelectionStyleConfiguration#configureSelectionStyle(org.eclipse.nebula.widgets.nattable.config.IConfigRegistry)
     */
    @Override
    protected void configureSelectionStyle(IConfigRegistry configRegistry) {
        /* This establishes the style for selected rows in the body region,
         * other than the cell specifically clicked upon, which is the anchor.
         */
        Style cellStyle = new Style();
        cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, BLUE);
        cellStyle
                .setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, WHITE);
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
                cellStyle, DisplayMode.SELECT);
    }

    /**
     * {@inheritDoc}
     * @see org.eclipse.nebula.widgets.nattable.selection.config.DefaultSelectionStyleConfiguration#configureHeaderFullySelectedStyle(org.eclipse.nebula.widgets.nattable.config.IConfigRegistry)
     */
    @Override
    protected void configureHeaderFullySelectedStyle(
            IConfigRegistry configRegistry) {
        /* This establishes the style for selected rows in row header region.
         */
        Style cellStyle = new Style();
        cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, BLUE);
        cellStyle
                .setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, WHITE);
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
                cellStyle, DisplayMode.SELECT,
                SelectionStyleLabels.COLUMN_FULLY_SELECTED_STYLE);
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
                cellStyle, DisplayMode.SELECT,
                SelectionStyleLabels.ROW_FULLY_SELECTED_STYLE);
    }

    /**
     * {@inheritDoc}
     * @see org.eclipse.nebula.widgets.nattable.selection.config.DefaultSelectionStyleConfiguration#configureSelectionAnchorStyle(org.eclipse.nebula.widgets.nattable.config.IConfigRegistry)
     */
    @Override
    protected void configureSelectionAnchorStyle(IConfigRegistry configRegistry) {
        /* This establishes the style for selected rows in the anchor cell, i.e.,
         * the cell clicked upon when highlighting the row. I have no idea why
         * this must be set for both NORMAL and SELECT display modes.
         */

        Style cellStyle = new Style();
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
                cellStyle, DisplayMode.NORMAL,
                SelectionStyleLabels.SELECTION_ANCHOR_STYLE);

        cellStyle = new Style();
        cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, BLUE);
        cellStyle
                .setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, WHITE);

        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE,
                cellStyle, DisplayMode.SELECT,
                SelectionStyleLabels.SELECTION_ANCHOR_STYLE);
    }
}