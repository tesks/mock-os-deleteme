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

import jpl.gds.product.automation.hibernate.gui.IconFactory;
import jpl.gds.product.automation.hibernate.gui.models.AbstractGuiTableModel;

/**
 * Renderer for cells in the history table.
 * Gets row colors and retrieves appropriate action icons.
 * 
 *
 */
@SuppressWarnings("serial")
public class HistoryTableCellRenderer extends AbstractGuiTableCellRenderer<Object> {
	private static final int TYPE_COLUMN = 0;
	private static final int LEVEL_NAME = 1;
	
	private static enum TYPES {
		Status, 
		Action, 
		Log, 
		NONE
	}
	
	private static enum LOG_LEVELS {
		FATAL, 
		ERROR, 
		WARN, 
		INFO, 
		OTHER
	}
	
	private static enum STATUSES {
		failed,
		completed,
		completed_pre,
		unknown_complete,
		pending
	}
	
	private Color getStatusColor(Object value) {
		STATUSES status;
		
		try {
			status = STATUSES.valueOf(value.toString());
		} catch (Exception e) {
			status = STATUSES.pending;
		}
		
		switch(status) {
			case completed: case completed_pre: case unknown_complete:
				return LIGHT_GREEN;
			case failed:
				return LIGHT_RED;
			default:
				return LIGHT_YELLOW;
		}
	}
	
	private Color getLogColor(Object value) {
		LOG_LEVELS log;
		
		try {
			log = LOG_LEVELS.valueOf(value.toString());
		} catch (Exception e) {
			log = LOG_LEVELS.OTHER;
		}
		
		switch(log) {
			case ERROR: case FATAL: case WARN:
				return LOG_RED;
			case INFO:
				return LOG_GREEN;
			default:
				return LOG_YELLOW;
		}
	}
	
	@Override
	public Color getCellBackgroundColor(int row, AbstractGuiTableModel<Object> tableModel) {
		TYPES type;
		
		try {
			type = TYPES.valueOf(tableModel.getValueAt(row, TYPE_COLUMN).toString());
		} catch (Exception e) {
			type = TYPES.NONE;
		}

		Object name = tableModel.getValueAt(row, LEVEL_NAME);
		
		if (name == null) {
			return Color.WHITE;
		} else {
			switch(type) {
				case Status:
					return getStatusColor(name);
				case Action:
					return LIGHT_BLUE;
				case Log:
					return getLogColor(name);
				default:
					return Color.WHITE;
			}
		}
	}

	@Override
	public Object transformValue(Object value, int row, int column, JTable tablel) {
		Object val;

		if (column == LEVEL_NAME) {
			val = getIcon(value.toString());
		} else {
			val = convertDate(value);
		}
		
		return val == null ? value : val;
		
	}
	
	private Icon getIcon(String value) {
		return IconFactory.getActionIcon(value);
	}
}
