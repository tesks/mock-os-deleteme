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
package jpl.gds.dictionary.api.decom.types;

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.ILookupSupport;

/**
 * Represents enum data within a decom map. An enum is extracted
 * as a signed integer, but is associated / displayed with an enum format.
 * This should not be confused with a {@link EnumerationDefinition}, which
 * defines a lookup table for translating integral enum values to their
 * symbolic mapping. Instances of this class identify how to extract
 * data with a decom map, and how to later display the enum in viewers.
 *
 */
public interface IEnumDataDefinition extends INumericDataDefinition, ILookupSupport {

	/**
	 * Get the format string to display this enum value with in viewing
	 * applications.
	 * @return the enum format string.
	 */
	public String getEnumFormat();
	
	/**
	 * Get the name of the enum mapping of numeric values to symbolic values.
	 * @return the name of the enum mapping
	 */
	public String getEnumName();

}
