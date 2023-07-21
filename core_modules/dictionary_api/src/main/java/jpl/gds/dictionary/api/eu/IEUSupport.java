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
 * Interface specifies the mutators needed to build a DN-to-EU conversion. It is
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
public interface IEUSupport extends IImmutableEUSupport {

    /**
     * Sets the DN to EU conversion object for this object, or null if
     * none defined.
     * 
     * @param dnToEu
     *            The IEUCalculation to set.
     */
    public void setDnToEu(final IEUDefinition dnToEu);
    
    /**
     * Sets the flag indicating whether this object has an EU (engineering unit) 
     * conversion. An engineering unit conversion is computed on a raw data 
     * value by the ground software.
     * 
     * @param yes true if there is an EU conversion; false if there is not
     */
    public abstract void setHasEu(boolean yes);

    /**
     * Sets the unit specifier for the EU value; may be null
     * 
     * @param unitStr
     *            unit string to set
     */
    public void setEuUnits(String unitStr);

    /**
     * Sets the output formatter for the EU value; may be null
     * 
     * @param formatStr
     *            the output formatter (a C printf format specifier) to set
     */
    public void setEuFormat(String formatStr);

}
