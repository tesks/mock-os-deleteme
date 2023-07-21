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
 * The IPolynomialEUDefinition interface is to be implemented by all classes that
 * represent the dictionary definition of an EU (engineering unit) calculation
 * that uses a polynomial calculation.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * IPolynomialEUDefinition defines the methods that must be implemented by all EU
 * polynomial classes. A polynomial EU is computed from the DN and a series of 
 * coefficients: coeff0 * dn^0 + coeff1 * dn^1 + coeff2 * dn^2 etc. 
 * 
 *
 */
public interface IPolynomialEUDefinition extends IEUDefinition {

    /**
     * Gets the number of coefficients in the polynomial (the polynomial length).
     * 
     * @return Returns the polynomial length.
     */
    public int getLength();

    /**
     * Sets the number of coefficients in the polynomial (the polynomial length).
     * 
     * @param len The length to set.
     */
    public void setLength(int len);

    /**
     * Sets the polynomial coefficient at the given index.
     * 
     * @param index the 0-based index of the coefficient to set
     * @param coeff the coefficient
     */
    public void setCoefficient(int index, double coeff);

    /**
     * Gets the coefficient for the given index, which must be less than the
     * value returned by getLength().
     * 
     * @param index the index of the coefficient to get
     * @return the coefficient
     */
    public double getCoefficient(int index);

}