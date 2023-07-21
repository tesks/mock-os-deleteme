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

import jpl.gds.product.automation.hibernate.entity.ProductAutomationStatus;
import jpl.gds.shared.time.DataValidityTime;
import jpl.gds.shared.time.ICoarseFineTime;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The table model for the status table.  Deals with holding onto statuses and is optimized
 * to not load statuses that already exist. 
 * 
 * MPCS-8182 - 08/11/16 - Added to and updated for AMPCS.
 */
@SuppressWarnings("serial")
public class StatusTableModel extends AbstractGuiTableModel<ProductAutomationStatus> {
	/**
	 * Create an StatusTableModel for displaying ProductAutomationStatus objects
	 * 
	 * @param columnHeaderNames
	 *            names of the columns to be displayed
	 */
	public StatusTableModel(Object[] columnHeaderNames) {
		super(columnHeaderNames);
	}
	
	/**
	 * Default StatusTableModel constructor that sets the column names
	 */
	public StatusTableModel() {
		this(new Object[] {
				"StatusType",
				"Status Time",
				"Session ID", 
				"Session Host",
				"Pass Number",
				"Apid",
				"Sclk",
				"Product Name",
				"id"
		});		
	}
	
	/**
	 * Add a single ProductAutomationStatus to the table
	 * 
	 *  @param status the ProductAutomationStatus to be added to the table
	 */
	@Override
	public void addRow(ProductAutomationStatus status) {
		// MPCS-11678  - 11/03/21 : PDPP Admin GUI stack traces when data has SCLK fine value
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
		// |---------------------------------------------------------------------------------------------------------|
		// | Data Validity Time 32 (DVT) seconds     |     32        | The on-board second product creation time     |
		// | Data Validity Time 20 (DVT) sub-seconds |     20        | The on-board sub-second product creation time |
		// |---------------------------------------------------------------------------------------------------------|
		//
		// Europa FGICD D-56521 Working Version 2019-02-19
		// 4.5.2.5.1.2 Transaction Sequence Number
		// Table 4-13:
		// |--------------------------------------------------------------|
		// |            Field                             | Length (bits) |
		// |--------------------------------------------------------------|
		// | On-board Product Creation Time (seconds)     |      32       |
		// | On-board Product Creation Time (sub-seconds) |      20       |
		// |--------------------------------------------------------------|
		ICoarseFineTime dvt = new DataValidityTime(status.getProduct().getSclkCoarse() , status.getProduct().getSclkFine());


		addRow(new Object[] {
			status.getStatusName(),
			status.getStatusTime(),
			status.getProduct().getSessionId(),
			status.getProduct().getSessionHost(),
			status.getPassNumber(),
			status.getProduct().getApid(),
			dvt.toString(),
			status.getProduct().getProductPath(),
			status.getStatusId()
		});
	}
	
	/**
	 * Add all of the supplied ProductAutomationtatus objects to the table. All
	 * status objects that are "stale" (not represented in the added set) are
	 * removed.
	 * 
	 * @param statuses
	 *            a Collection of ProductAutomationStatus objects to add to the
	 *            table
	 */
	@Override
	public void addRows(Collection<ProductAutomationStatus> statuses) {
		ArrayList<Long> inputIds = new ArrayList<Long>();
		
		for (ProductAutomationStatus status : statuses) {
			addRow(status);
			
			inputIds.add(status.getStatusId());
		}

		removeStaleObjects(inputIds);		
	} 
}