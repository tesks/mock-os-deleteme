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
package jpl.gds.dictionary.api;

/**
 * The ILookupSupport interface is to be implemented by all dictionary
 * definition classes that require a lookup table, or enumeration, that maps
 * integral values to corresponding symbolic strings. This interface allows one
 * to set or get the lookup table from an object. The class that should be used
 * for the actual enumeration of values is EnumerationDefinition. <br>
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 *
 * @see EnumerationDefinition
 */
public interface ILookupSupport {

    /**
     * Sets the lookup table/enumeration into the current object.
     * 
     * @param table the EnumerationDefinition to set
     */
    public void setLookupTable(EnumerationDefinition table);

    /**
     * Gets the lookup table/enumeration from the current object.
     * 
     * @return the Object mapped to the key, or null if no mapping found
     */
    public EnumerationDefinition getLookupTable();
}
