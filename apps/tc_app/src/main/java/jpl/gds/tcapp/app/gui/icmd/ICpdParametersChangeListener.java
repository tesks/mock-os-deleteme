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
package jpl.gds.tcapp.app.gui.icmd;

import gov.nasa.jpl.icmd.schema.AggregationMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMode;
import gov.nasa.jpl.icmd.schema.ExecutionStateRequest;
import gov.nasa.jpl.icmd.schema.ListPreparationStateEnum;

/**
 * This interface provides notification methods when various CPD parameters are modified.
 *
 * @since AMPCS R3
 */
public interface ICpdParametersChangeListener {
	/**
	 * Called when the user issues a change to the Execution Mode
	 * @param newMode the new Execution Mode
	 */
	public void onExecutionModeChange(ExecutionMode newMode);
	
	/**
	 * Called when the user issues a change to the Execution State
	 * @param newState the new Execution State
	 */
	public void onExecutionStateChange(ExecutionStateRequest newState);
	
	/**
	 * Called when the user issues a change to the Execution Method
	 * @param newMethod the new Execution Method
	 */
	public void onExecutionMethodChange(ExecutionMethod newMethod);
	
	/**
	 * Called when the user issues a change to the Preparation State
	 * @param newState the new preparation state
	 */
	public void onPreparationStateChange(ListPreparationStateEnum newState);
	
	/**
	 * Called when the user issues a change to the Aggregation Method
	 * @param newAggregationMethod the new Aggregation Method
	 */
	public void onAggregationMethodChange(AggregationMethod newAggregationMethod);
	
	/**
	 * Called when the user issues a change to the Connection Status
	 * @param connect whether or not there is a connection
	 * @param stationId the station ID that the connection refers to, if applicable
	 */
	public void onConnectionStatusChange(boolean connect, String stationId);
	
	/**
	 * Called when the user issues a change to the station bit rate
	 * @param newBitRate the new bit rate
	 */
	public void onBitRateChange(double newBitRate);
}
