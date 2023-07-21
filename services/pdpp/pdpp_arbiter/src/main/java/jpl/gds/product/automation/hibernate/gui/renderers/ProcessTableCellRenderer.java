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

import jpl.gds.product.automation.hibernate.entity.ProductAutomationProcess;
import jpl.gds.product.automation.hibernate.gui.IconFactory;
import jpl.gds.product.automation.hibernate.gui.models.AbstractGuiTableModel;

/**
 * Renderer for cells in the process table. Gets row colors, converts pause and
 * pause ack binary values to String display values, converts date values, and
 * retrieves appropriate action icons.
 * 
 */
@SuppressWarnings("serial")
public class ProcessTableCellRenderer extends AbstractGuiTableCellRenderer<ProductAutomationProcess> {
	private static final int ACTION_COLUMN = 0;
	private static final int STOP_TIME_COLUMN = 5;
	private static final int PAUSE_REQ_COLUMN = 11;
	private static final int PAUSE_ACK_COLUMN = 12;
	
	private static final String SET = "Set";
	private static final String UNSET = "Not set";
	
	@Override
	public Color getCellBackgroundColor(int row, AbstractGuiTableModel<ProductAutomationProcess> tableModel) {
		if (tableModel.getValueAt(row, STOP_TIME_COLUMN) == null) {
			return LIGHT_GREEN;
		} else {
			return LIGHT_RED;
		}
	}

	@Override
	public Object transformValue(Object value, int row, int column, JTable table) {
		Object val;

		switch(column) {
			case ACTION_COLUMN:
				val = getIcon(value.toString());
				break;
			case PAUSE_REQ_COLUMN: case PAUSE_ACK_COLUMN:
				if (value instanceof Integer) {
					val = ((Integer) value) == 0 ? UNSET : SET;
				} else {
					val = value;
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
