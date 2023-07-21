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
import jpl.gds.shared.time.DataValidityTime;
import jpl.gds.shared.time.ICoarseFineTime;

/**
 * Table model for holding onto ProductAutomationAction objects.
 * 
 * MPCS-8182 - 08/11/16 - Added to and updated for AMPCS.
 */
@SuppressWarnings("serial")
public class ActionTableModel extends AbstractGuiTableModel<ProductAutomationAction> {
	
	/**
	 * Create an ActionTableModel for displaying ProductAutomationAction objects
	 * 
	 * @param columnHeaderNames
	 *            names of the columns to be displayed
	 */
	public ActionTableModel(Object[] columnHeaderNames) {
		super(columnHeaderNames);
		setFullPaths(false);
	}
	
	/**
	 * Default ActionTableModel constructor that sets the column names
	 */
	public ActionTableModel() {
		this(new Object[] {
				"Action",
				"Pass Number", 
				"Process ID",
				"Reassign",
				"Assigned Time", 
				"Accepted Time", 
				"Completed Time", 
				"Session ID",
				"Session Host",
				"APID",
				"SCLK",
				"Product", 
				"id"
		});
	}

	/**
	 * This is a special case because the data coming back could change even though it is 
	 * already displayed.  For that reason, going to remove all data each time and do a full update.
	 * 
	 * @param objectCollection the ProductAutomationAction objects to be added to the table
	 */
	@Override
	public void addRows(Collection<ProductAutomationAction> objectCollection) {
		clearData();
		
		// There is no need to remove stale so just add everything.
		for (ProductAutomationAction action : objectCollection) {
			addRow(action);
		}
	}

	/**
	 * Add a single ProductAutomationAction to the table
	 * 
	 * @param action
	 *            the ProductAutomationAction to be added to the table
	 */
	@Override
	public void addRow(ProductAutomationAction action) {
		// MPCS-11678 - 11/03/21 : PDPP Admin GUI stack traces when data has SCLK fine value
		// that exceeds the configured SCLK fine bits
		// Products use DVT (Data Validity Time) or just plain On-board Product Creation Time which
		// uses 20 bits for the sub-seconds compared to the 16 bits used for SCLK
		// Looking at the FGICDs for both Psyche and Europa it seems that both missions
		// allocate 20 bits for the sub-seconds.
		//
		// Psyche FGICD D-102323 Rev B 20210501 Signed
		// 3.2.1 PDU Header
		// Table 3-7:
		// |---------------------------------------------------------------------------------------------------------|
		// |            Field                        | Length (bits) |          Comments                             |
		// | Data Validity Time 32 (DVT) seconds     |     32        | The on-board second product creation time     |
		// | Data Validity Time 20 (DVT) sub-seconds |     20        | The on-board sub-second product creation time |
		// |---------------------------------------------------------------------------------------------------------|
		//
		// Europa FGICD D-56521 Working Version 2019-02-19
		// 4.5.2.5.1.2 Transaction Sequence Number
		// Table 4-13:
		// |--------------------------------------------------------------|
		// |            Field                             | Length (bits) |
		// | On-board Product Creation Time (seconds)     |      32       |
		// | On-board Product Creation Time (sub-seconds) |      20       |
		// |--------------------------------------------------------------|
		ICoarseFineTime dvt = new DataValidityTime(action.getProduct().getSclkCoarse(), action.getProduct().getSclkFine());


		// Get the process id for the product.  Need to check if it is null first.
		Long processId;
		
		if (action.getProcess() == null) {
			processId = null;
		} else {
			processId = action.getProcess().getProcessId();
		}
		
		/**
		 * MPCS-6671 -  9/2014 - Updating for new columns.
		 */
		
		addRow(new Object[] {
				action.getActionName().getMnemonic(), // Action
				action.getPassNumber(), // pass
				processId,
				action.getReassignBool(), // Reassign flag.
				action.getAssignedTime(), 
				action.getAcceptedTime(), 
				action.getCompletedTime(), 
				action.getProduct().getSessionId(),
				action.getProduct().getSessionHost(),
				action.getProduct().getApid(),
				dvt.toString(),
				action.getProduct().getProductPath(),
				action.getActionId()
				}
		);
	}
}