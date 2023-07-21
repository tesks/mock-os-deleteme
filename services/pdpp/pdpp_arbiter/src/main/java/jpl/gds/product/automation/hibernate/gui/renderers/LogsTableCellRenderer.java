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

import jpl.gds.product.automation.hibernate.entity.ProductAutomationLog;
import jpl.gds.product.automation.hibernate.gui.models.AbstractGuiTableModel;

/**
 * Renderer for cells in the logs table. Does not do anything special really.
 * Converts dates to string format, gets row colors, and retrieves appropriate
 * action icons.
 * 
 */
@SuppressWarnings("serial")
public class LogsTableCellRenderer extends AbstractGuiTableCellRenderer<ProductAutomationLog> {
	private static final int LEVEL_COLUMN = 0;
	private static final int REPORTER_COLUMN = 2;
	private static final int PRODUCT_COLUMN = 4;
	private static final int TIME_COLUMN = 3;
	private static final int ARBITER_LOGS = 0;
	
	private static enum VALUE_ENUMS {
		Arbiter,
		Process,
	}
	
	private static enum LEVELS {
		FATAL, 
		ERROR, 
		WARN, 
		USER, 
		INFO, 
		DEBUG, 
		TRACE,
		UNKNOWN, 
		NO_LEVEL_FOUND
	}
	
	@Override
	public Color getCellBackgroundColor(int row,
			AbstractGuiTableModel<ProductAutomationLog> tableModel) {
		LEVELS level;
		
		try {
			level = LEVELS.valueOf((String) tableModel.getValueAt(row, LEVEL_COLUMN));
		} catch (Exception e) {
			level = LEVELS.NO_LEVEL_FOUND;
		}
		
		Color bgc;
		
		switch(level) {
			case FATAL: case ERROR: case WARN:
				bgc = LOG_RED;
				break;
			case INFO:
				bgc = LOG_GREEN;
				break;
			case NO_LEVEL_FOUND:
				bgc = Color.WHITE;
				break;
			default:
				bgc = LOG_YELLOW;
				break;
		}
		
		return bgc;
	}

	@Override
	public Object transformValue(Object value, int row, int column, JTable table) {
		Object val;

		switch(column) {
			case REPORTER_COLUMN:
				Long procId = (Long) value;
				val = procId > ARBITER_LOGS ? 
						VALUE_ENUMS.Process.toString():
						VALUE_ENUMS.Arbiter.toString();

				val = val + " - " + procId;
				break;
			case TIME_COLUMN:
				val = convertDate(value);
				break;
			case PRODUCT_COLUMN:
				if (value != null) {
					val = stripFilePath((String) value);
				} else {
					val = value;
				}
				
				break;
			default:
				val = value;
				break;
		}
		
		return val;
	}
}
