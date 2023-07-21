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
package jpl.gds.tc.api.icmd;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jpl.gds.tc.api.icmd.exception.ICmdException;

/**
 * CPD directives that AMPCS can send to the CPD server
 * 
 * @since AMPCS R5
 */
public enum CpdDirective {
	/** Connect to a station or accept a connection from a station */
	CONNECT_TO_STATION(CpdDirectiveArgument.STATION_ID),

	/** Disconnect from the currently connected station */
	DISCONNECT_FROM_STATION(),

	/** Query the CPD station connection status */
	QUERY_CONNECTION_STATUS(),

	/** Set the CPD execution state */
	SET_EXECUTION_STATE(CpdDirectiveArgument.EXECUTION_STATE),

	/** Query the CPD configuration parameters */
	QUERY_CONFIGURATION();

	/** The arguments associated with this directive */
	private Map<CpdDirectiveArgument, String> args;

	/** Flag that indicates whether or not this directive require arguments */
	private boolean requireArgument;

	/**
	 * Constructor
	 * 
	 * @param requireArgument true if the directive requires arguments
	 * @param returnType the type that will be returned after this CPD directive
	 *            is issued
	 */
	private CpdDirective(final CpdDirectiveArgument... arguments) {
		this.requireArgument = arguments != null && arguments.length > 0;

		this.args = new HashMap<CpdDirectiveArgument, String>();

		for (final CpdDirectiveArgument cda : arguments) {
			this.args.put(cda, null);
		}
	}

	/**
	 * Add an argument to this directive that will be sent to CPD when this
	 * directive is issued
	 * 
	 * @param arg the argument name
	 * @param value the argument value
	 * @throws ICmdException if the argument is invalid
	 */
	public void addArgument(final String arg, final String value) throws ICmdException {
		if (arg == null || arg.length() == 0) {
			throw new ICmdException(
					"Null or empty argument value supplied as input for CPD Directive: "
							+ this.toString());
		}

		CpdDirectiveArgument cpdArg = null;

		try {
			cpdArg = CpdDirectiveArgument.valueOf(arg.toUpperCase().trim());
		} catch (final Exception e) {
			throw new ICmdException("Unrecognized argument \"" + arg
					+ "\" for CPD Directive: " + this.toString());
		}

		if (cpdArg == null) {
			throw new ICmdException("Invalid argument provided: " + arg);
		}

		if (value == null || value.length() == 0) {
			throw new ICmdException(
					"Null or empty value supplied as input for CPD Directive: "
							+ this.toString() + ", arg: " + arg);
		}

		this.args.put(cpdArg, value);
	}

	/**
	 * Get the arguments (name and value) that have been added to this directive
	 * 
	 * @return the arguments (name and value) that have been added to this
	 *         directive
	 */
	public Map<CpdDirectiveArgument, String> getArguments() {
		return this.args;
	}

	/**
	 * Get the set of required arguments for this directive
	 * 
	 * @return the set of required arguments for this directive
	 */
	public Set<CpdDirectiveArgument> getRequiredArguments() {
		return this.args.keySet();
	}

	/**
	 * Indicates whether or not this directive had argument(s) added to it
	 * 
	 * @return true if this directive had argument(s) added to it, false
	 *         otherwise
	 */
	public boolean hasArguments() {
		return this.args.size() > 0;
	}

	/**
	 * Get all the arguments added for this directive as a comma-separted
	 * key=value string
	 * 
	 * @return all the arguments added for this directive as a comma-separted
	 *         key=value string
	 */
	public String getArgumentsString() {
		if (!hasArguments()) {
			return "";
		}

		final StringBuilder builder = new StringBuilder();

		for (final Entry<CpdDirectiveArgument, String> keyValue : this.args
				.entrySet()) {
			builder.append(",");
			builder.append(keyValue.getKey().toString());
			builder.append("=");
			builder.append(keyValue.getValue());
		}

		return builder.substring(1);
	}

	/**
	 * Indicates whether or not this directive require arguments
	 * 
	 * @return true if this directive require arguments, false otherwise
	 */
	public boolean requireArgument() {
		return this.requireArgument;
	}
}