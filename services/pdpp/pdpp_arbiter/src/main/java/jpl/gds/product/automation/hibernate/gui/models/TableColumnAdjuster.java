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
package jpl.gds.product.automation.hibernate.gui.models;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 *	Class to manage the widths of columns in a table.
 *
 *  Various properties control how the width of the column is calculated.
 *  Another property controls whether column width calculation should be dynamic.
 *  Finally, various Actions will be added to the table to allow the user
 *  to customize the functionality.
 *
 *  This class was designed to be used with tables that use an auto resize mode
 *  of AUTO_RESIZE_OFF. With all other modes you are constrained as the width
 *  of the columns must fit inside the table. So if you increase one column, one
 *  or more of the other columns must decrease. Because of this the resize mode
 *  of RESIZE_ALL_COLUMNS will work the best.
 *  
 *
 *  MPCS-8182 - 08/11/16 - Added to and updated for AMPCS.
 */
public class TableColumnAdjuster implements PropertyChangeListener, TableModelListener
{
	private JTable table;
	private int spacing;
	private boolean isColumnHeaderIncluded;
	private boolean isColumnDataIncluded;
	private boolean isOnlyAdjustLarger;
	private boolean isDynamicAdjustment;
	private Map<TableColumn, Integer> columnSizes = new HashMap<TableColumn, Integer>();

	private int lastColumn;
	
	/**
	 * Specify the table and use default spacing
	 * 
	 * @param table
	 *            the table to be adjusted
	 */
	public TableColumnAdjuster(JTable table)
	{
		this(table, 6);
	}

	/**
	 * Specify the table and spacing. This allows each column to have a minimum
	 * amount of padding.
	 * 
	 * @param table
	 *            the table to be adjusted
	 * @param spacing
	 *            the amount of space to be given for each column
	 * 
	 */
	public TableColumnAdjuster(JTable table, int spacing)
	{
		lastColumn = table.getColumnCount() - 1;
		this.table = table;
		this.spacing = spacing;
		setColumnHeaderIncluded( true );
		setColumnDataIncluded( true );
		setOnlyAdjustLarger( true );
		setDynamicAdjustment( false );
		installActions();
	}

	/**
	 * Adjusts the widths of all of the columns in the table. If the total table
	 * width is less than the supplied minimum, then the last column of the
	 * table is given the remaining space.
	 * 
	 * @param minWidth the minimum width of the entire table
	 */
	public void adjustColumns(int minWidth) {
		adjustColumns();
		if (table.getWidth() < minWidth) {
			// Add the rest of the space to the last column.
			int tableWidth = table.getWidth();
			TableColumn lastCol = table.getColumnModel().getColumn(lastColumn);

			lastCol.setWidth(minWidth - (tableWidth - lastCol.getWidth()));
		}
	}
	
	/**
	 *  Adjust the widths of all the columns in the table so the full names and data are displayed
	 */
	public void adjustColumns()
	{
		TableColumnModel tcm = table.getColumnModel();
		
		for (int i = 0; i < tcm.getColumnCount(); i++)
		{
			adjustColumn(i);
		}
	}

	/**
	 * Adjust the width of the specified column in the table so the full name
	 * and data are displayed
	 * 
	 * @param column
	 *            the index of the column to be adjusted.
	 */
	public void adjustColumn(final int column)
	{
		TableColumn tableColumn = table.getColumnModel().getColumn(column);

		if (! tableColumn.getResizable()) return;

		int columnHeaderWidth = getColumnHeaderWidth( column );
		int columnDataWidth   = getColumnDataWidth( column );
		int preferredWidth    = Math.max(columnHeaderWidth, columnDataWidth);

		updateTableColumn(column, preferredWidth);
	}

	/**
	 *  Calculated the width based on the column name
	 *  @param column the index of the column width being retrieved
	 */
	private int getColumnHeaderWidth(int column)
	{
		if (! isColumnHeaderIncluded) return 0;

		TableColumn tableColumn = table.getColumnModel().getColumn(column);
		Object value = tableColumn.getHeaderValue();
		TableCellRenderer renderer = tableColumn.getHeaderRenderer();

		if (renderer == null)
		{
			renderer = table.getTableHeader().getDefaultRenderer();
		}

		Component c = renderer.getTableCellRendererComponent(table, value, false, false, -1, column);
		return c.getPreferredSize().width;
	}

	/*
	 *  Calculate the width based on the widest cell renderer for the
	 *  given column.
	 */
	private int getColumnDataWidth(int column)
	{
		if (! isColumnDataIncluded) return 0;

		int preferredWidth = 0;
		int maxWidth = table.getColumnModel().getColumn(column).getMaxWidth();

		for (int row = 0; row < table.getRowCount(); row++)
		{
    		preferredWidth = Math.max(preferredWidth, getCellDataWidth(row, column));

			//  We've exceeded the maximum width, no need to check other rows

			if (preferredWidth >= maxWidth)
			    break;
		}

		return preferredWidth;
	}

	/*
	 *  Get the preferred width for the specified cell
	 */
	private int getCellDataWidth(int row, int column)
	{
		//  Inovke the renderer for the cell to calculate the preferred width

		TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
		Component c = table.prepareRenderer(cellRenderer, row, column);
		int width = c.getPreferredSize().width + table.getIntercellSpacing().width;

		return width;
	}

	/*
	 *  Update the TableColumn with the newly calculated width
	 */
	private void updateTableColumn(int column, int width)
	{
		final TableColumn tableColumn = table.getColumnModel().getColumn(column);

		if (! tableColumn.getResizable()) return;

		width += spacing;

		//  Don't shrink the column width

		if (isOnlyAdjustLarger)
		{
			width = Math.max(width, tableColumn.getPreferredWidth());
		}

		columnSizes.put(tableColumn, new Integer(tableColumn.getWidth()));
		table.getTableHeader().setResizingColumn(tableColumn);
		tableColumn.setWidth(width);
	}

	/**
	 *  Restore the widths of the columns in the table to its previous width
	 */
	public void restoreColumns()
	{
		TableColumnModel tcm = table.getColumnModel();

		for (int i = 0; i < tcm.getColumnCount(); i++)
		{
			restoreColumn(i);
		}
	}

	/*
	 *  Restore the width of the specified column to its previous width
	 */
	private void restoreColumn(int column)
	{
		TableColumn tableColumn = table.getColumnModel().getColumn(column);
		Integer width = columnSizes.get(tableColumn);

		if (width != null)
		{
			table.getTableHeader().setResizingColumn(tableColumn);
			tableColumn.setWidth( width.intValue() );
		}
	}

	/**
	 * Indicate whether to include the header in the width calculation
	 * 
	 * @param isColumnHeaderIncluded
	 *            TRUE if the header's width is to be considered in the width
	 *            calculation, FALSE if not
	 *
	 */
	public void setColumnHeaderIncluded(boolean isColumnHeaderIncluded)
	{
		this.isColumnHeaderIncluded = isColumnHeaderIncluded;
	}

	/**
	 * Indicate whether to include the model data in the width calculation
	 * 
	 * @param isColumnDataIncluded
	 *            TRUE if the column's width is to be considered in the width
	 *            calculation, FALSE if not
	 */
	public void setColumnDataIncluded(boolean isColumnDataIncluded)
	{
		this.isColumnDataIncluded = isColumnDataIncluded;
	}

	/**
	 * Indicate whether columns can only be increased in size
	 * 
	 * @param isOnlyAdjustLarger
	 *            TRUE if the column width can only be made larger, FALSE if not
	 */
	public void setOnlyAdjustLarger(boolean isOnlyAdjustLarger)
	{
		this.isOnlyAdjustLarger = isOnlyAdjustLarger;
	}

	/**
	 * Indicate whether changes to the model should cause the width to be
	 * dynamically recalculated.
	 * 
	 * @param isDynamicAdjustment
	 *            TRUE if the column width can be changed at any time, FALSE if
	 *            not
	 */
	public void setDynamicAdjustment(boolean isDynamicAdjustment)
	{
		//  May need to add or remove the TableModelListener when changed

		if (this.isDynamicAdjustment != isDynamicAdjustment)
		{
			if (isDynamicAdjustment)
			{
				table.addPropertyChangeListener( this );
				table.getModel().addTableModelListener( this );
			}
			else
			{
				table.removePropertyChangeListener( this );
				table.getModel().removeTableModelListener( this );
			}
		}

		this.isDynamicAdjustment = isDynamicAdjustment;
	}
//
//  Implement the PropertyChangeListener
//
	/**
	 * If there is any property change in a table model the event is propagated
	 * through this method
	 * 
	 * @param e
	 *            the PropertyChangeEvent that is causing the update
	 */
	public void propertyChange(PropertyChangeEvent e)
	{
		//  When the TableModel changes we need to update the listeners
		//  and column widths

		if ("model".equals(e.getPropertyName()))
		{
			TableModel model = (TableModel)e.getOldValue();
			model.removeTableModelListener( this );

			model = (TableModel)e.getNewValue();
			model.addTableModelListener( this );
			adjustColumns();
		}
	}
//
//  Implement the TableModelListener
//
	/**
	 * If there is any alteration to a table model the event is propagated through this method
	 * 
	 * @param e the PropertyChangeEvent that has an update
	 */
	public void tableChanged(TableModelEvent e)
	{
		if (! isColumnDataIncluded) return;

		//  A cell has been updated

		if (e.getType() == TableModelEvent.UPDATE)
		{
			int column = table.convertColumnIndexToView(e.getColumn());

			//  Only need to worry about an increase in width for this cell

			if (isOnlyAdjustLarger)
			{
				int	row = e.getFirstRow();
				TableColumn tableColumn = table.getColumnModel().getColumn(column);

				if (tableColumn.getResizable())
				{
					int width =	getCellDataWidth(row, column);
					updateTableColumn(column, width);
				}
			}

			//	Could be an increase of decrease so check all rows

			else
			{
				adjustColumn( column );
			}
		}

		//  The update affected more than one column so adjust all columns

		else
		{
			adjustColumns();
		}
	}

	/*
	 *  Install Actions to give user control of certain functionality.
	 */
	private void installActions()
	{
		installColumnAction(true,  true,  "adjustColumn",   "control ADD");
		installColumnAction(false, true,  "adjustColumns",  "control shift ADD");
		installColumnAction(true,  false, "restoreColumn",  "control SUBTRACT");
		installColumnAction(false, false, "restoreColumns", "control shift SUBTRACT");

		installToggleAction(true,  false, "toggleDynamic",  "control MULTIPLY");
		installToggleAction(false, true,  "toggleLarger",   "control DIVIDE");
	}

	/*
	 *  Update the input and action maps with a new ColumnAction
	 */
	private void installColumnAction(
		boolean isSelectedColumn, boolean isAdjust, String key, String keyStroke)
	{
		Action action = new ColumnAction(isSelectedColumn, isAdjust);
		KeyStroke ks = KeyStroke.getKeyStroke( keyStroke );
		table.getInputMap().put(ks, key);
		table.getActionMap().put(key, action);
	}

	/*
	 *  Update the input and action maps with new ToggleAction
	 */
	private void installToggleAction(
		boolean isToggleDynamic, boolean isToggleLarger, String key, String keyStroke)
	{
		Action action = new ToggleAction(isToggleDynamic, isToggleLarger);
		KeyStroke ks = KeyStroke.getKeyStroke( keyStroke );
		table.getInputMap().put(ks, key);
		table.getActionMap().put(key, action);
	}

	/*
	 *  Action to adjust or restore the width of a single column or all columns
	 */
	@SuppressWarnings("serial")
	class ColumnAction extends AbstractAction
	{
    	private boolean isSelectedColumn;
    	private boolean isAdjust;

		public ColumnAction(boolean isSelectedColumn, boolean isAdjust)
		{
			this.isSelectedColumn = isSelectedColumn;
			this.isAdjust = isAdjust;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			//  Handle selected column(s) width change actions

			if (isSelectedColumn)
			{
				int[] columns = table.getSelectedColumns();

				for (int i = 0; i < columns.length; i++)
				{
					if (isAdjust)
						adjustColumn(columns[i]);
					else
						restoreColumn(columns[i]);
				}
			}
			else
			{
				if (isAdjust)
					adjustColumns();
				else
					restoreColumns();
			}
		}
	}

	/*
	 *  Toggle properties of the TableColumnAdjuster so the user can
	 *  customize the functionality to their preferences
	 */
	@SuppressWarnings("serial")
	class ToggleAction extends AbstractAction
	{
		private boolean isToggleDynamic;
		private boolean isToggleLarger;

		public ToggleAction(boolean isToggleDynamic, boolean isToggleLarger)
		{
			this.isToggleDynamic = isToggleDynamic;
			this.isToggleLarger = isToggleLarger;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (isToggleDynamic)
			{
				setDynamicAdjustment(! isDynamicAdjustment);
				return;
			}

			if (isToggleLarger)
			{
				setOnlyAdjustLarger(! isOnlyAdjustLarger);
				return;
			}
		}
	}
}
