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

import java.util.List;

/**
 * An interface to be implemented by classes that support a basic 
 * CSV representation.
 * 
 *
 */
public interface ICsvSupport {
    /**
     * Format header as a CSV.
     * 
     * @param csvColumns list of CSV column names
     *
     * @return CSV header string
     */
	public String getCsvHeader(final List<String> csvColumns);
	
    /**
     * Format as a CSV.
     *
     * @param csvColumns list of CSV column names
     * @return CSV string
     *
     *
     */
    public abstract String toCsv(final List<String> csvColumns);

    /**
     * Parses a CSV line in the format specified by toCsv() and sets the
     * appropriate member variables. This should be modified accordingly each
     * time toCsv() is modified.
     *
     * @param csvStr     String to parse
     * @param csvColumns Columns expected
     *
     */
    public abstract void parseCsv(final String       csvStr,
                                  final List<String> csvColumns);
}
