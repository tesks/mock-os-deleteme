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
 * An interface to be implemented by classes that represent CHDO SFDU properties
 * (CHDO SFDU data fields that support evaluation as part of a CHDO condition.
 * 
 *
 * @since R8
 */
public interface IChdoProperty {

    /**
     * Adds a condition to this property.
     * 
     * @param condition the IChdoCondition to add.
     */
    public void addCondition(IChdoCondition condition);

    /**
     * Retrieves all the conditions for this property.
     * 
     * @return List of IChdoCondition, or an empty list if no conditions added.
     */
    public List<IChdoCondition> getChdoConditions();

    /**
     * Gets the name of this property.
     * 
     * @return the name
     */
    public String getName();

}