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
package jpl.gds.sleproxy.server.sleinterface.rtn.action;

/**
 * Enumerates the different types of actions that can be performed on the SLE
 * return service.
 * 
 */
public enum EReturnActionType {

	/**
	 * The SLE BIND operation.
	 */
	BIND("bind"),

	/**
	 * The SLE START operation.
	 */
	START("start"),

	/**
	 * The SLE STOP operation.
	 */
	STOP("stop"),

	/**
	 * The SLE UNBIND operation.
	 */
	UNBIND("unbind"),

	/**
	 * The SLE ABORT operation.
	 */
	ABORT("abort");

    private final String val;

    private EReturnActionType(final String val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return val;
    }
	
}
