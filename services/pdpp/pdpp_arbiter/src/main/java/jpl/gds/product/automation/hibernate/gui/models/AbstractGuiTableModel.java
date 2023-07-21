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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import org.apache.commons.lang.ObjectUtils.Null;

/**
 * Abstract table model for holding onto product automation objects. When
 * calling the constructor the last column should be the id for the object. This
 * will never be displayed and is used to to optimize loading and to help with
 * deleting.
 * 
 * @param <E>
 *
 * MPCS-8182 - 08/11/16 - Added to and updated for AMPCS.
 */
@SuppressWarnings("serial")
public abstract class AbstractGuiTableModel<E> extends DefaultTableModel {
	private ArrayList<Long> tableObjectIds;
	private int idColumn;
	private boolean useFullPath;
	
	/**
	 * Constructor for the AbstractGuiTableModel that requires the column header
	 * names
	 * 
	 * @param columnHeaderNames
	 *            The names of the columns in the table
	 */
	protected AbstractGuiTableModel(Object[] columnHeaderNames) {
		super(columnHeaderNames, 0);
		init();
	}
	
	private void init() {
		useFullPath = false;
		tableObjectIds = new ArrayList<Long>();
		idColumn = columnIdentifiers.size() - 1;
	}
	
	
	/**
	 * Get if the full path is being used with the product name
	 * 
	 * @return TRUE if the full path is displayed, FALSE if not
	 */
	public boolean useFullPaths() {
		return useFullPath;
	}
	
	
	/**
	 * Set the useFullPath variable and the table is updated. If useFullPath is
	 * true the full path will be shown iwth the product name. If false it is
	 * not.
	 * 
	 * @param fp
	 *            TRUE if the full path is to be displayed, FALSE if not
	 */
	public void setFullPaths(boolean fp) {
		useFullPath = fp;
		fireTableDataChanged();
	}
	
	/**
	 * Adds the row to the end of the data.  Updates the internal statusId list.  Skips
	 * the add if the status is already added. 
	 * 
	 * @param rowData The table row being added
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void addRow(Object[] rowData) {
		// First check if the row is already there.  If it is do nothing.
		Long objectId = (Long) rowData[rowData.length - 1];
		
		if (!tableObjectIds.contains(objectId)) {
			dataVector.add(DefaultTableModel.convertToVector(rowData));
			
			Integer newRowId = dataVector.size() - 1;
			tableObjectIds.add(objectId);
			fireTableRowsInserted(newRowId, newRowId);
		}
	}
	
	/**
	 * Get the the id (product ID, status ID, log ID, action ID, etc.) of the
	 * item on a particular row
	 * 
	 * @param rowId
	 *            the row being queried
	 * @return the ID of the object as a Long
	 */
	public Long getObjectId(int rowId) {
		return (Long) getValueAt(rowId, idColumn);
	}
	
	/**
	 * Just replaces the old data vector with an empty one, rebuilds the status
	 * id map and calls the fire table data changed method.
	 */
	public void clearData() {
		dataVector.clear();
		tableObjectIds.clear();
		fireTableDataChanged();
	}

	private Integer findRowWithObjectId(Long statusId) {
		for (int row = 0; row < dataVector.size(); row++) {
			if (statusId == (Long) getValueAt(row, idColumn)) {
				return row;
			}
		}
		
		return -1;
	}
	
	/**
	 * Pass in an array of automation object ids to remove.
	 * 
	 * @param objectIds
	 *            a List of Long object IDs associated with the objects to be
	 *            removed
	 */
	public void removeObjects(List<Long> objectIds) {
		// From the list get the row for the status id and build a list of actual rows to be deleted.
		List<Integer> rowsToDelete = new ArrayList<Integer>();
		
		for (int index = 0; index < objectIds.size(); index++) {
			rowsToDelete.add(findRowWithObjectId(objectIds.get(index)));
		}
		
		// Just call the delete rows method now.
		removeRows(rowsToDelete);
	}

	/**
	 * Pass in an array of row indices to be removed
	 * 
	 * @param rows the indices for the rows to be removed
	 */
	public void removeRows(int[] rows) {
		List<Integer> rs = new ArrayList<Integer>();
		
		for (int r : rows) {
			rs.add(r);
		}
		
		removeRows(rs);
	}
	
	/**
	 * Pass in an array of ROW indices to be removed. This will sort the list in
	 * place. If the order matters to you pass in a copy of the array.
	 * 
	 * @param rows
	 *            the indices of the rows to be removed
	 */
	public void removeRows(List<Integer> rows) {
		// Sort the list and then reverse.
		Collections.sort(rows);
		Collections.reverse(rows);
		
		for (int row : rows) {
			removeRow(row);
		}
	}
	
	/**
	 * Gets the name of a single column
	 * 
	 * @param col
	 *            the column being queried
	 * 
	 * @return a String name for this column
	 */
	@Override
	public String getColumnName(int col) {
		try {
			String colName = (String) columnIdentifiers.get(col);
			return colName;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Returns the number of columns -1 since the last column is not to be displayed and is only
	 * there as identifiers to the object id.
	 * 
	 * @return the number of columns used
	 */
	@Override
	public int getColumnCount() {
		return columnIdentifiers.size() - 1;
	}

	/**
	 * Returns the number of rows
	 * 
	 * @return the number of rows
	 */
	@Override
	public int getRowCount() {
		return dataVector.size();
	}

	/**
	 * Get the value of an individual cell in the table
	 * 
	 * @param row
	 *            the horizontal row the value is being retrieved from
	 * @param column
	 *            the vertical column the value is being retrieved from
	 * @return the value stored in the unique row and column location.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object getValueAt(int row, int column) {
		// Make sure there is data before trying to get values.
		
		Object value;
		
		if (!dataVector.isEmpty()){
			value = ((Vector<Object>) dataVector.get(row)).get(column);
		} else {
			value = null;
		}
		
		return value;
	}
	
	
	/**
	 * Removes an individual row from the table
	 * 
	 * @param row
	 *            the row number to be removed
	 */
	@Override
	public void removeRow(int row) {
		tableObjectIds.remove(getValueAt(row, columnIdentifiers.size() - 1));
		super.removeRow(row);
	}
	
	
	/**
	 * Returns false regardless of parameter values. No cell in the results is
	 * editable by the user
	 *
	 * @param row
	 *            the row whose value is to be queried
	 * @param column
	 *            the column whose value is to be queried
	 * @return false
	 */
	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}
	
	/**
	 * Return the class for the values stored in a column
	 * 
	 * @param c
	 *            the column whose values' class is to be queried
	 * 
	 * @return the class for the values stored in this column
	 */
	@Override
	public Class<?> getColumnClass(int c) {
		Object v = getValueAt(0, c);
		return v == null ? Null.class : v.getClass();
	}
	
	/**
	 * Removes all of the "stale" objects from the table. Stale objects are any
	 * that are those not supplied in the argument to this function
	 * 
	 * @param validIds
	 *            the automation object IDs that are to NOT be deleted
	 */
	protected void removeStaleObjects(Collection<Long> validIds) {
		List<Long> staleIds = new ArrayList<Long>(tableObjectIds.size());
		
		// Collections.copy would not work.  So just copying them here. 
		for (Long id : tableObjectIds) {
			if (!validIds.contains(id)) {
				staleIds.add(id);
			}
		}
		
		removeObjects(staleIds);
	}

	/**
	 * Generic method. Need to take in a collection of objects. Note, this class
	 * is not able to remove data that should not be there after the query is
	 * done. That part will have to be taken care of in the implementation of
	 * this method.
	 * 
	 * @param objectCollection
	 *            the objects to be added to the table as new row
	 */
	public abstract void addRows(Collection<E> objectCollection);
	
	/**
	 * Single method to add an automation object to the model.
	 * 
	 * @param automationObject the single object to be added to the table as a row
	 */
	public abstract void addRow(E automationObject);
}
