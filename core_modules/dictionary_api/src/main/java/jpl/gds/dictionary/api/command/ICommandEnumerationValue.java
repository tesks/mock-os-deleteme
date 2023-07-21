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
package jpl.gds.dictionary.api.command;


/**
 * The ICommandEnumerationValue interface is to be implemented by all
 * command dictionary argument enumeration value classes, as stored in the 
 * CommandEnumerationDefinition class.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 *
 *
 * @see CommandEnumerationDefinition
 */
public interface ICommandEnumerationValue extends Comparable<ICommandEnumerationValue> {

    /**
     * Accessor for the bit value.
     * 
     * @return The current bit value
     */
    public abstract String getBitValue();

    /**
     * Mutator for the bit value.
     * 
     * @param bitValue
     *            The new bit value
     */
    public abstract void setBitValue(final String bitValue);

    /**
     * Accessor for the dictionary value.
     * 
     * @return The current dictionary value
     */
    public abstract String getDictionaryValue();

    /**
     * Mutator for the dictionary value.
     * 
     * @param dictionaryValue
     *            The new dictionary value
     */
    public abstract void setDictionaryValue(final String dictionaryValue);

    /**
     * Accessor for the FSW value.
     * 
     * @return The current FSW value
     */
    public abstract String getFswValue();

    /**
     * Mutator for the FSW value.
     * 
     * @param fswValue
     *            The new FSW value
     * 
     */
    public abstract void setFswValue(final String fswValue);

}