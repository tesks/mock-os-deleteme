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
package jpl.gds.dictionary.api.eu;

/**
 * Interface specifies the information describing a DN-to-EU conversion. It is
 * implemented by a variety of classes that support EU conversion, such as 
 * channel and product field definitions.
 * 
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 */
public interface IImmutableEUSupport {

    /**
     * Gets the DN to EU conversion object for this object, or null if
     * none defined.
     * 
     * @return Returns the IEUCalculation object.
     */
    public IEUDefinition getDnToEu();

    /**
     * Indicates whether the object has an EU conversion.
     * 
     * @return true if there is a defined conversion, false if not
     */
    public boolean hasEu();

    /**
     * Gets the unit specifier for the EU value; may be null
     * 
     * @return unit string
     */
    public String getEuUnits();

    /**
     * Gets the output formatter for the EU value; may be null
     * 
     * @return the output formatter (a C printf format specifier)
     */
    public String getEuFormat();

}
