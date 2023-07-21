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
package jpl.gds.product.api;

/**
 * Product PDU type interface.
 * 
 *
 */
public interface IPduType {
	
	/**
	 * @return true if the pdu type is a metadata type.
	 */
	public boolean isMetadata();
	
	/**
	 * @return true if the pdu type is an end of data type.
	 */
	public boolean isEndOfData();

	/**
	 * @return true if the pdu type is a data type.
	 */
	public boolean isData();
	
	/**
	 * @return true if the pdu type is an end type.
	 */
	public boolean isEnd();
}
