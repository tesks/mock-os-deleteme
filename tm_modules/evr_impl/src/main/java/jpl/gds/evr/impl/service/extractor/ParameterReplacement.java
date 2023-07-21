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
package jpl.gds.evr.impl.service.extractor;

/**
 * A ParameterReplacement is used by the EVR dictionary to replace message parameters 
 * with other values, like table lookups or mnemonic stem names for command opcodes.
 * Any type of replacement must implement this interface.
 * 
 */
public interface ParameterReplacement {

    /**
     * Returns the replacement object for the given object.
     * 
     * @param original the Object to replace
     * @return the replacement Object
     */
    public Object replace(Object original);

}
