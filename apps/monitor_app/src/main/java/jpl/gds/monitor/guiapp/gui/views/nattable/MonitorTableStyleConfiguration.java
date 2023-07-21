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
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDisplayConverter;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.LineBorderDecorator;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.Style;

import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;

/**
 * A custom NAT Table style configuration that sets table font and color changes
 * from a ViewConfiguration, and left aligns text in table cells. A new instance
 * must be created if the view configuration changes to see the effects.
 *
 */
public class MonitorTableStyleConfiguration extends DefaultNatTableStyleConfiguration {
    
    
    /**
     * Constructor.
     * 
     * @param viewConfig the ViewConfiguration for the NAT table view
     */
    public MonitorTableStyleConfiguration(IViewConfiguration viewConfig) {
        super();
        
        /* Just override the default font and colors */
        this.font = ChillFontCreator.getFont(viewConfig.getDataFont());
        this.fgColor = ChillColorCreator.getColor(viewConfig.getForegroundColor());
        this.bgColor = ChillColorCreator.getColor(viewConfig.getBackgroundColor());
    }
    

    /**
     * {@inheritDoc}
     * @see org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration#configureRegistry(org.eclipse.nebula.widgets.nattable.config.IConfigRegistry)
     */
    @Override
    public void configureRegistry(IConfigRegistry configRegistry) {
        ICellPainter painter = new LineBorderDecorator(new TextPainter(false, true, 2, false, true));

        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_PAINTER, painter);
        Style cellStyle = new Style();
        cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, bgColor);
        cellStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, fgColor);
        cellStyle.setAttributeValue(CellStyleAttributes.FONT, font);
        
        /* Override default cell text alignment, which is CENTER */
        cellStyle.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
        
        cellStyle.setAttributeValue(CellStyleAttributes.VERTICAL_ALIGNMENT, vAlign);
        cellStyle.setAttributeValue(CellStyleAttributes.BORDER_STYLE, borderStyle);
        configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle);
        configRegistry.registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultDisplayConverter());
        
    }
}