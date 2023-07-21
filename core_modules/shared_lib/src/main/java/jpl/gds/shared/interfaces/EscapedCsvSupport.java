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
package jpl.gds.shared.interfaces;

/**
 * This interface is to be implemented by classes that provide an escaped
 * CSV string representation of their content, in which the commas a escaped
 * by backslashes.
 *
 *
 */
public interface EscapedCsvSupport
{
	/**
	 * The CSV separator string
	 */
	public static final String CSV_SEPARATOR = "\\,";

	/**
	 * Gets the escaped CSV text form of the object.
	 *
	 * @return CSV string
	 */
	public String getEscapedCsv();
}
