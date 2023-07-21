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
package jpl.gds.tc.api.icmd.datastructures;

import gov.nasa.jpl.icmd.schema.AggregationMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMode;
import gov.nasa.jpl.icmd.schema.ExecutionState;
import gov.nasa.jpl.icmd.schema.ListConfiguration;
import gov.nasa.jpl.icmd.schema.ListConfigurationResponseType;
import gov.nasa.jpl.icmd.schema.ListPreparationStateEnum;
import gov.nasa.jpl.icmd.schema.ResponseStatus;
import jpl.gds.tc.api.icmd.CpdResponse;

/**
 * This class wraps a CPD response to a request for the CPD configuration
 *
 * @since AMPCS R5
 */
public class CpdConfiguration extends CpdResponse {
	/** The CPD configuration response */
	private final ListPreparationStateEnum preparationState;
	private final ExecutionMode executionMode;
	private final ExecutionMethod executionMethod;
	private final ExecutionState executionState;
	private final AggregationMethod aggregationMethod;

	/**
	 * Constructor for ListConfigurationResponseType
	 *
	 * @param config the CPD configuration response
	 */
	public CpdConfiguration(ListConfigurationResponseType config) {
		super(config.getRESPONSE());
		this.preparationState = config.getLISTCONFIG().getPREPARATIONSTATE();
		this.executionMode = config.getLISTCONFIG().getEXECUTIONMODE();
		this.executionMethod = config.getLISTCONFIG().getEXECUTIONMETHOD();
		this.executionState = config.getLISTCONFIG().getEXECUTIONSTATE();
		this.aggregationMethod = config.getLISTCONFIG().getAGGREGATIONMETHOD();
	}

	/*
	 * MPCS-7327 - Josh Choi - 5/12/2015: Handle ListConfiguration types (comes
	 * with LIST_CONFIGURATION_INFO)
	 */
	/**
	 * Constructor for ListConfiguration
	 *
	 * @param listconfigurationinfo the CPD configuration
	 */
	public CpdConfiguration(ListConfiguration listconfigurationinfo) {
		super(new ResponseStatus());
		this.preparationState = listconfigurationinfo.getLISTCONFIG().getPREPARATIONSTATE();
		this.executionMode = listconfigurationinfo.getLISTCONFIG().getEXECUTIONMODE();
		this.executionMethod = listconfigurationinfo.getLISTCONFIG().getEXECUTIONMETHOD();
		this.executionState = listconfigurationinfo.getLISTCONFIG().getEXECUTIONSTATE();
		this.aggregationMethod = listconfigurationinfo.getLISTCONFIG().getAGGREGATIONMETHOD();
	}

	/**
	 * Get the preparation state
	 *
	 * @return the preparation state
	 * @see gov.nasa.jpl.icmd.schema.ListConfigurationType#getPREPARATIONSTATE()
	 */
	public ListPreparationStateEnum getPreparationState() {
		return this.preparationState;
	}

	/**
	 * Get the execution mode
	 *
	 * @return the execution mode
	 * @see gov.nasa.jpl.icmd.schema.ListConfigurationType#getEXECUTIONMODE()
	 */
	public ExecutionMode getExecutionMode() {
		return this.executionMode;
	}

	/**
	 * Get the execution method
	 *
	 * @return the execution method
	 * @see gov.nasa.jpl.icmd.schema.ListConfigurationType#getEXECUTIONMETHOD()
	 */
	public ExecutionMethod getExecutionMethod() {
		return this.executionMethod;
	}

	/**
	 * Get the execution state
	 *
	 * @return the execution state
	 * @see gov.nasa.jpl.icmd.schema.ListConfigurationType#getEXECUTIONSTATE()
	 */
	public ExecutionState getExecutionState() {
		return this.executionState;
	}

	/**
	 * Get the aggregation method
	 *
	 * @return the aggregation method
	 * @see gov.nasa.jpl.icmd.schema.ListConfigurationType#getAGGREGATIONMETHOD()
	 */
	public AggregationMethod getAggregationMethod() {
		return this.aggregationMethod;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * jpl.gds.tc.impl.icmd.datastructures.CpdDataStructure#toKeyValueCsv()
	 */
	@Override
	public String toKeyValueCsv() {
		StringBuilder builder = new StringBuilder(super.toKeyValueCsv());

		builder.append(",");
		builder.append("preparation_state");
		builder.append("=");
		builder.append(this.getPreparationState().toString());
		builder.append(",");
		builder.append("execution_mode");
		builder.append("=");
		builder.append(this.getExecutionMode().toString());
		builder.append(",");
		builder.append("execution_method");
		builder.append("=");
		builder.append(this.getExecutionMethod().toString());
		builder.append(",");
		builder.append("execution_state");
		builder.append("=");
		builder.append(this.getExecutionState().toString());
		builder.append(",");
		builder.append("aggregation_method");
		builder.append("=");
		builder.append(this.getAggregationMethod().toString());

		return builder.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((aggregationMethod == null) ? 0 : aggregationMethod
						.hashCode());
		result = prime * result
				+ ((executionMethod == null) ? 0 : executionMethod.hashCode());
		result = prime * result
				+ ((executionMode == null) ? 0 : executionMode.hashCode());
		result = prime * result
				+ ((executionState == null) ? 0 : executionState.hashCode());
		result = prime
				* result
				+ ((preparationState == null) ? 0 : preparationState.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!super.equals(obj)) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		CpdConfiguration other = (CpdConfiguration) obj;

		if (aggregationMethod != other.aggregationMethod) {
			return false;
		}

		if (executionMethod != other.executionMethod) {
			return false;
		}

		if (executionMode != other.executionMode) {
			return false;
		}

		if (executionState != other.executionState) {
			return false;
		}

		if (preparationState != other.preparationState) {
			return false;
		}

		return true;
	}

}
