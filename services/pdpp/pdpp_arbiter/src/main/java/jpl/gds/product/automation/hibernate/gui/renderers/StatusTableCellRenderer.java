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

import javax.swing.JTable;

import jpl.gds.product.automation.hibernate.dao.ProductAutomationStatusDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationStatus;
import jpl.gds.product.automation.hibernate.gui.models.AbstractGuiTableModel;

/**
 * Renderer for cells in the status table. gets row colors and can toggle
 * products to show either the full file path or just the file name.
 * 
 */
@SuppressWarnings("serial")
public class StatusTableCellRenderer extends AbstractGuiTableCellRenderer<ProductAutomationStatus> {
	private static final int STATUS_COLUMN = 0;
	private static final int PRODUCT_COLUMN = 7;
	
	@Override
	public Color getCellBackgroundColor(int row, AbstractGuiTableModel<ProductAutomationStatus> tableModel) {
		Object value = tableModel.getValueAt(row, STATUS_COLUMN);

		if (ProductAutomationStatusDAO.Status.FAILED.toString().equals(value)) {
			return LIGHT_RED;
		} else if (ProductAutomationStatusDAO.Status.COMPLETED.toString().equals(value) ||
			ProductAutomationStatusDAO.Status.COMPLETE_PRE_PB.toString().equals(value) || 
			ProductAutomationStatusDAO.Status.UNKNOWN_COMPLETE.toString().equals(value)) {
			return LIGHT_GREEN;
		} else {
			return LIGHT_YELLOW;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object transformValue(Object value, int row, int column, JTable table) {
		Object val;
		
		if (column == PRODUCT_COLUMN) {
			if (((AbstractGuiTableModel<ProductAutomationAction>) table.getModel()).useFullPaths()) {
				val = value;
			} else {
				val = stripFilePath(value.toString());
			}		
		} else {
			val = convertDate(value);
		}
		
		return val == null ? value : val;
	}
}
