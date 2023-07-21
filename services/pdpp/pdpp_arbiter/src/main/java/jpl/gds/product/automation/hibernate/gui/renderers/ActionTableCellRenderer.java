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
package jpl.gds.product.automation.hibernate.gui.renderers;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JTable;

import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;
import jpl.gds.product.automation.hibernate.gui.IconFactory;
import jpl.gds.product.automation.hibernate.gui.models.AbstractGuiTableModel;

/**
 * Renderer for cells in the action table. Gets row colors, retrieves
 * appropriate action icons, and can toggle products to show either the full
 * file path or just the file name.
 * 
 *
 */
@SuppressWarnings("serial")
public class ActionTableCellRenderer extends AbstractGuiTableCellRenderer<ProductAutomationAction> {
	private static final int ACTION_COLUMN = 0;
	private static final int COMPLETED_TIME_COLUMN = 5;
	private static final int PRODUCT_COLUMN = 10;
	
	@Override
	/**
	 * Colors them based on completed or pending.  Checks the completed time.  
	 */
	public Color getCellBackgroundColor(int row, AbstractGuiTableModel<ProductAutomationAction> tableModel) {
		if (tableModel.getValueAt(row, COMPLETED_TIME_COLUMN) == null) {
			return LIGHT_YELLOW;
		} else {
			return LIGHT_GREEN;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object transformValue(Object value, int row, int column, JTable table) {
		Object val;
		
		switch(column) {
			case ACTION_COLUMN:
				val = getIcon(value.toString());
				break;
			case PRODUCT_COLUMN:
				if (((AbstractGuiTableModel<ProductAutomationAction>) table.getModel()).useFullPaths()) {
					val = value;
				} else {
					val = stripFilePath(value.toString());
				}
				break;
			default:
				val = convertDate(value); 
				break;
		}
		
		return val == null ? value : val;
	}

	private Icon getIcon(String value) {
		return IconFactory.getActionIcon(value);
	}
}
