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
package jpl.gds.shared.directive;

/**
 * A class represented an argument to a process control directive.
 * 
 *
 * @since R8
 */
public class DirectiveArgument implements IDirectiveArgument {

	private String name;
	private Object value;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            name of the argument
	 * @param value
	 *            current value of the argument
	 */
	public DirectiveArgument(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Object getValue() {
		return this.value;
	}

	@Override
	public String getValueAsString() {
		return this.value == null ? null : value.toString();
	}

}
