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

import java.util.Collection;

import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationLog;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationStatus;

/**
 * Table model for holding onto ProductAutomationLog, ProductAutomationStatus,
 * and ProductAutomationAciton objects. This table is used to show the history
 * of a product in the Lineage View.
 * 
 * MPCS-8182 - 08/11/16 - Added to and updated for AMPCS.
 */
@SuppressWarnings("serial")
public class HistoryTableModel extends AbstractGuiTableModel<Object> {
	private static final String STATUS = "Status";
	private static final String ACTION = "Action";
	private static final String LOG = "Log";
	private static final String NOT_APP = "N/A";
	
	/**
	 * Create a HistoryTableModel for displaying automation objects that
	 * represent the history of a product
	 * 
	 * @param columnHeaderNames
	 *            names of the columns to be displayed
	 */
	public HistoryTableModel(Object[] columnHeaderNames) {
		super(columnHeaderNames);
	}
	
	/**
	 * Default HistoryTableModel constructor that sets the column names
	 */
	public HistoryTableModel() {
		this(new Object[] {
				"Event Typesffs",
				"Name / Level", 
				"Event Time",
				"Message",
				"id"
		});
	}

	/**
	 * Add all of the supplied items to the history table.
	 * 
	 * @param objectCollection
	 *            a Collection of action, status, and log objects to be added
	 */
	@Override
	public void addRows(Collection<Object> objectCollection) {
		for (Object obj : objectCollection) {
			addRow(obj);
		}
	}

	private void addLog(ProductAutomationLog log) {
		addRow(new Object[] {
				LOG,
				log.getLevel(),
				log.getEventTime(),
				log.getMessage(),
				log.getLogId()
		});
	}
	
	private void addStatus(ProductAutomationStatus status) {
		addRow(new Object[] {
			STATUS,
			status.getStatusName(),
			status.getStatusTime(),
			NOT_APP,
			status.getStatusId()
		});
	}
	
	private void addAction(ProductAutomationAction action) {
		addRow(new Object[] {
			ACTION,
			action.getActionName().getMnemonic(),
			action.getAssignedTime(),
			NOT_APP,
			action.getActionId()
		});
	}
	
	
	/**
	 * Add a single automation action, status, or log as a row to the table. If
	 * it is of any other type it will be ignored.
	 * 
	 * @param obj the ProductAutomationAction, Status, or Log to be added to the table
	 */
	@Override
	public void addRow(Object obj) {
		if (obj instanceof ProductAutomationAction) {
			addAction((ProductAutomationAction) obj);
		} else if (obj instanceof ProductAutomationStatus) {
			addStatus((ProductAutomationStatus) obj);
		} else if (obj instanceof ProductAutomationLog) {
			addLog((ProductAutomationLog) obj);
		} else {
			// Bogus object, just skip.
		}
	}
}
