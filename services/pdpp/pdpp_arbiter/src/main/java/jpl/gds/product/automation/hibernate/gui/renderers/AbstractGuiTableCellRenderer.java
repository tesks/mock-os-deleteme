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
import java.awt.Component;
import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import jpl.gds.product.automation.hibernate.gui.models.AbstractGuiTableModel;

/**
 * Abstract renderer class for the pdpp gui tables.  This is parameterized to use one of the 
 * pdpp entity classes.
 * 
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public abstract class AbstractGuiTableCellRenderer<T> extends DefaultTableCellRenderer {
	// MPCS-8515 - 10/26/16  - Changed hh to HH to fix 12/24 hour display.
	private static final String DATE_FMT = "yyyy-DDD'T'HH:mm:ss";
	private static final Color SELECTED_COLOR = Color.LIGHT_GRAY;
	private static final Color FOCUS_COLOR = Color.GRAY;
	
	/** light yellow color code */
	protected static final Color LIGHT_YELLOW = Color.decode("#FFFF99");
	/** light green color code */
	protected static final Color LIGHT_GREEN = Color.decode("#B8FF94");
	/** light red color code */
	protected static final Color LIGHT_RED = Color.decode("#FFB2B2");
	/** light blue color code */
	protected static final Color LIGHT_BLUE = Color.decode("#80CCFF");
	
	/** yellow color code */
	protected static final Color LOG_YELLOW = Color.decode("#FFFF80");
	/** green color code */
	protected static final Color LOG_GREEN = Color.decode("#33D633");
	/** red color code */
	protected static final Color LOG_RED = Color.decode("#FF4719");

	/** default height of cells */
	protected static final int DEFAULT_HEIGHT = 20;
	
	private boolean focusEnabled;
	private SimpleDateFormat fmt;

	/**
	 * Default constructor. Cell cannot have focus.
	 */
	public AbstractGuiTableCellRenderer () {
		this(false);
	}

	/**
	 * Constructor that can allow a cell to have focus
	 * 
	 * @param focusEnabled
	 *            TRUE if a table cell can have focus, FALSE if not
	 */
	public AbstractGuiTableCellRenderer (boolean focusEnabled) {
		super();
		fmt = new SimpleDateFormat(DATE_FMT);
		// MPCS-8515 - 10/26/16 - Set displayed time zone to UTC.
		// Default is system time zone.
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.focusEnabled = focusEnabled;
	}
	
	/**
	 * Get the name of a file
	 * 
	 * @param fp
	 *            the String representation of a file path
	 * @return the String name of the file specified in the file path
	 */
	protected String stripFilePath(String fp) {
		if (fp == null || fp.isEmpty()) {
			return fp;
		} else {
			File f = new File(fp);
			return f.getName();
		}
	}
	
	/**
	 * Converts a Date object to a String in the format "yyyy-DDD'T'hh:mm:ss"
	 * 
	 * @param date
	 *            the Date object to be converted
	 * @return a date time String
	 */
	protected String convertDate(Date date) {
		return fmt.format(date.getTime());
	}
	
	/**
	 * Converts a Timestamp object to a String in the format
	 * "yyyy-DDD'T'hh:mm:ss"
	 * 
	 * @param ts
	 *            the Timestamp object to be converted
	 * @return a date time String
	 */
	protected String convertDate(Timestamp ts) {
		return convertDate(new Date(ts.getTime()));
	}
	
	/**
	 * Converts a date or time object of unknown type to a String in the format
	 * "yyyy-DDD'T'hh:mm:ss". Returns null if the object is not a Date or
	 * Timestamp object
	 * 
	 * @param dateObj
	 *            the object to be converted
	 * @return a date time String, or null if not supported.
	 */
	protected String convertDate(Object dateObj) {
		if (dateObj instanceof Date) {
			return convertDate((Date) dateObj);
		} else if (dateObj instanceof Timestamp) {
			return convertDate((Timestamp) dateObj);
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * Sets the value of the cell as well as the background color.  Checks the focusable member
	 * to see if focus is enabled and will use the focus color.  If the row is selected, uses 
	 * selected color.  Otherwise will call the getCellBackgroundColor that is abstract.
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		Object transformedValue = transformValue(value, row, column, table);
		
		Icon icon;
		String text;
		if (value == null) {
			icon = null;
			text = null;
		} else if (transformedValue == null) {
			icon = null;
			text = value.toString();
		} else if (transformedValue instanceof Icon) {
			icon = (Icon) transformedValue;
			text = null;
		} else {
			text = transformedValue.toString();
			icon = null;
		}
		
		setIcon(icon);
		setText(text);

		if (icon != null) {
			setHorizontalAlignment(JLabel.CENTER);
		} else {
			setHorizontalAlignment(JLabel.LEFT);
		}
		
		Color bgc;
		
		if (hasFocus && focusEnabled) {
			bgc = FOCUS_COLOR;
		} else if (isSelected) {
			bgc = SELECTED_COLOR;
		} else if (table.getModel() instanceof AbstractGuiTableModel) {
			try {
				bgc = getCellBackgroundColor(row, (AbstractGuiTableModel<T>) table.getModel());
			} catch (Exception e) {
				bgc = null;
			}
		} else {
			bgc = null;
		}
		
		if (bgc != null) {
			setBackground(bgc);
		} else {
			setBackground(Color.WHITE);
		}
		
		return this;
	}

	/**
	 * Give some scheme to set the background color.
	 * 
	 * @param row
	 *            the row index where the background color is being retrieved
	 * @param tableModel
	 *            the table model that has the cell background color being
	 *            retrieved
	 * @return a Color value
	 */
	abstract public Color getCellBackgroundColor(int row, AbstractGuiTableModel<T> tableModel);
	
	/**
	 * Do whatever needs to be done to the value and then return it. This class
	 * will check if the value is an icon and will use setIcon. If it is not,
	 * assumes it is a string and will use setText.
	 * 
	 * @param value
	 *            the Object to be transformed
	 * @param row
	 *            the row index of the cell that will transform the value
	 * @param column
	 *            the column index of the cell that will transform the value
	 * @param table
	 *            the JTable that houses the cell that will transform the value
	 * @return the transformed object
	 */
	abstract public Object transformValue(Object value, int row, int column, JTable tablel);
}
