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

import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProcess;

/**
 * Process table model. This one is different than the other table models. Want
 * to be able to show the processId, so going to override the super
 * getColumnCount. Want to be able to see what the deal process the action is
 * assigned to.
 * 
 * MPCS-8182 - 08/11/16 - Added to and updated for AMPCS.
 */
@SuppressWarnings("serial")
public class ProcessTableModel extends AbstractGuiTableModel<ProductAutomationProcess> {
	
	private IFswToDictionaryMapper mapper;

	/**
	 * Create a ProcessTableModel for displaying ProductAutomationProcess
	 * objects
	 * 
	 * @param columnHeaderNames
	 *            names of the columns to be displayed
	 */
	public ProcessTableModel(Object[] columnHeaderNames, IFswToDictionaryMapper mapper) {
		super(columnHeaderNames);
		this.mapper = mapper;
	}
	
	/**
	 * Default ProcessTableModel constructor that sets the column names
	 * 
	 * MPCS-6671 -  9/2014 - Updating for new columns.
	 */
	public ProcessTableModel(IFswToDictionaryMapper mapper) {
		this(new Object[] {
				"Action", 
				"FSW Build ID", 
				"Dictionary", 
				"PID",
				"Start Time", 
				"Stop Time", 
				"Host",
				"Assigned Actions",
				"Completed Actions",
				"Last Completed Time", 
				"Killed By", 
				"Pause Requested", 
				"Pause Acknowlegded",
				"ProcessId"}, mapper);
		}
	
	/**
	 * Return the column count. This table will display the id column and
	 * therefore does show the full value of this count
	 * 
	 * @return the column count
	 */
	@Override
	public int getColumnCount() {
		return columnIdentifiers.size();
	}

	/**
	 * Add multiple ProductAutomationProcess objects to the table. No entries
	 * are removed.
	 * 
	 * @param objectCollection
	 *            a Collection of ProductAutomaitonProcess objects to be added
	 *            to the table
	 */
	@Override
	public void addRows(Collection<ProductAutomationProcess> objectCollection) {
		for (ProductAutomationProcess process : objectCollection) {
			addRow(process);
		}
	}

	/**
	 * Add a single ProductAutomationProcess to the table.
	 * 
	 * @param process
	 *            a ProductAutomationProcess to be added to the table
	 */
	@Override
	public void addRow(ProductAutomationProcess process) {
		/**
		 * MPCS-6671 -  9/2014 - Updating for new columns.
		 * 
		 * MPCS-7069 -  3/2015 - Adding PID column as well as looking up the dictionary version for the given build id.
		 */
		Object[] rowData = new Object[] {
			process.getAction().getMnemonic(),
			process.getFswBuildId(),
			mapper.getDictionary(process.getFswBuildId()),
			process.getPid(),
			process.getStartTime(), 
			process.getShutDownTime(), 
			process.getProcessHost(),
			process.getAssignedActions(),
			process.getCompletedActions(),
			process.getLastCompleteTimeStr(),
			process.getKiller(),
			process.getPauseAck(),
			process.getPause(),
			process.getProcessId()
		};
		
		addRow(rowData);
	}
}