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
package jpl.gds.station.api.dsn.chdo;

import java.util.List;

/**
 * An interface to be implemented by classes that store conditionals for comparison
 * of CHDO SFDU fields.
 * 
 *
 * @since R8
 */
public interface IChdoCondition {

    /**
     * Adds an equality comparison on a field to this condition object.
     * 
     * @param name the name of the equality condition
     * @param value value to compare the CHDO field against
     * @param equalityValue true if we want the value to be equal to what's in the CHDO, false if not equal
     */
    public void addEqualityCondition(String name, String value,
            boolean equalityValue);

    /**
     * Gets the CHDO type this condition applies to.
     * @return CHDO type number
     */
    public int getChdoType();

    /**
     * Gets the list of equality comparisons that make up this compound condition.
     * @return list of EqualityConditions; empty list if no comparisons defined
     */
    public List<IEqualityCondition> getEqualityConditions();

}