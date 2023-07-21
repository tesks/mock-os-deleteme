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
package jpl.gds.product.automation.hibernate.gui;

import java.sql.Timestamp;
import java.util.Collection;

import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationLog;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProcess;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationStatus;

/**
 * Interface that will give user input for quering for the PDPP automation. 
 * 
 */
public interface UserInputInterface {
	
	/**
	 * Returns the product string. Will be null if field is empty.
	 * 
	 * @return the text in the product field as a String
	 */
	public String getProduct();
	
	/**
	 * Return the session ID. Will be null if field is empty.
	 * 
	 * @return the text in the session ID field as a Long
	 */
	public Long getSessionId();
	
	/**
	 * Return the pass number. Will be null if field is empty.
	 * 
	 * @return the text in the pass number field as a Long
	 */
	public Long getPassNumber();

	/**
	 * Return the host. Will be null if the field is empty.
	 * 
	 * @return the text in the host field as a String
	 */
	public String getHost();
	
	/**
	 * Return the apid. Will be null if the field is empty
	 * 
	 * @return the text in the APID field as an Integer.
	 */
	public Integer getApids();
	
	/**
	 * Returns an array of the selected statuses.
	 * 
	 * @return a collection of the Strings representing the selected statuses
	 */
	public Collection<String> getSelectedStatuses();

	/**
	 * Converts lower bound time to a time stamp and returns. If the field is
	 * empty, returns null. If the time from now field has data will get the
	 * current time minus the delta.
	 * 
	 * @return a Timestamp object representing the value places in the
	 *         "Start Time" field
	 */
	public Timestamp getLowerBound();
	
	/**
	 * Converts upper bound time to a time stamp and returns. If the field is
	 * empty, returns null. If the time from now field has valid data will
	 * return the current time.
	 * 
	 * @return a Timestamp object representing the value places in the
	 *         "End Time" field
	 */
	public Timestamp getUpperBound();
	
	/**
	 * Get if only the latest option is selected or not.
	 * 
	 * @return TRUE if the "Latest" option is selected, FALSE otherwise
	 */
	public boolean latestOnly();
	
	/**
	 * Returns if the "Event Time" option is selected under the "Time Type" configuration option
	 * 
	 * @return TRUE if "Event Time" is selected, FALSE if "Product Time" is selected
	 */
	public boolean isEventTime();
	
	/**
	 * Enables all of the status options and disables the other options.
	 */
	public void enableStatusOptions();
	
	/**
	 * Disables the action only options.
	 */
	public void disableStatusOptions();
	
	/**
	 * Enables all of the logs options and disables the other options
	 */
	public void enableLogsOptions();
	
	/**
	 * Disables the logs only options.
	 */
	public void disableLogsOptions();
	
	/**
	 * Enables all of the actions options and disables the other options
	 */
	public void enableActionOptions();
	
	/**
	 * Disables the action only options.
	 */
	public void disableActionOptions();
	
	/**
	 * Enables all of the process options and disables the other options
	 */
	public void enableProcessOptions();
	
	/**
	 * Disables the process only options.
	 */
	public void disableProcessOptions();
	
	/**
	 * Queries for products with the inputs from the panel.
	 * 
	 * @param bottomUp
	 *            TRUE if the results are to be returned starting with the child
	 *            followed by parent, FALSE if parent to child
	 * 
	 * @return a collection of ProductAutomationProduct objects matching the
	 *         user query parameters
	 */
	public Collection<ProductAutomationProduct> getProducts(boolean bottomUp);
	
	/**
	 * Queries for statuses using the user input values.
	 * 
	 * @return a collection of ProductAutomationStatus objects matching the user
	 *         query parameters
	 */
	public Collection<ProductAutomationStatus> getStatuses();
	
	/**
	 * Queries for processes using the user input values
	 * 
	 * @return a collection of ProductAutomationProcess objects matching the
	 *         user query parameters
	 */
	public Collection<ProductAutomationProcess> getProcesses();
	
	/**
	 * Queries for actions using the user input values
	 * 
	 * @return a collection of ProductAutomationAction objects matching the user
	 *         query parameters
	 */
	public Collection<ProductAutomationAction> getActions();
	
	/**
	 * Queries for logs using the user input values
	 * 
	 * @return a collection of ProductAtuomationLogs objects matching the user
	 *         query parameters
	 */
	public Collection<ProductAutomationLog> getLogs();
}
