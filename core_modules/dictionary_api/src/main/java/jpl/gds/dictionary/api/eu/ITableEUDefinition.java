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
 * The ITableEUDefinition interface is to be implemented by all classes that
 * represent the dictionary definition of an EU (engineering unit) calculation
 * that uses a Table interpolation.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * ITableEUDefinition defines the methods that must be implemented by all EU
 * table interpolation classes. A table interpolation has a series of DN/EU
 * pairs. Calculation of an EU from a DN means finding where the DN fits into
 * the table (between two elements, for instance) and then interpolates the EU
 * from the EU entries in the table at the same location(s).
 * 
 *
 */
public interface ITableEUDefinition extends IEUDefinition {

    /**
     * Gets the length (number of entries) in the interpolation table.
     * 
     * @return Returns the number of entries in the table.
     */
    public int getLength();

    /**
     * Sets the number of entries in the interpolation table.
     * 
     * @param len
     *            the length to set
     */
    public void setLength(int len);

    /**
     * Sets the DN at a specific table index.
     * 
     * @param index
     *            the table index
     * @param dn
     *            the DN to set
     */
    public void setDn(int index, double dn);

    /**
     * Sets the EU at a specific table index.
     * 
     * @param index
     *            the table index
     * @param eu
     *            the EU to set
     */
    public void setEu(int index, double eu);

    /**
     * Gets the DN at a specific table index, which must be less than the return
     * value from getLength().
     * 
     * @param index
     *            the table index
     * @return the DN at the given index
     */
    public double getDn(int index);

    /**
     * Gets the EU at a specific table index, which must be less than the return
     * value from getLength().
     * 
     * @param index
     *            the table index
     * @return the EU at the given index
     */
    public abstract double getEu(int index);

}