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
 * The IValidationRange interface is to be implemented by all
 * command argument range validation classes. 
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 *
 * @see ICommandArgumentDefinition
 */
public interface IValidationRange {

    /**
     * Accessor for the minimum value of the range. Range values
     * are represented as strings in order to support all types 
     * of command arguments, but the minimum value should be of
     * appropriate data type for the argument it applies to.
     * 
     * @return The current minimum
     */
    public abstract String getMinimum();

    /**
     * Accessor for the maximum value of the range.Range values
     * are represented as strings in order to support all types 
     * of command arguments, but the maximum value should be of
     * appropriate data type for the argument it applies to.
     * 
     * @return The current maximum
     */
    public abstract String getMaximum();

    /**
     * Mutator for the minimum value of the range. Range values
     * are represented as strings in order to support all types 
     * of command arguments, but the minimum value should be of
     * appropriate data type for the argument it applies to.
     * 
     * @param mi
     *            The new minimum value
     */
    public abstract void setMinimum(final String mi);

    /**
     * Mutator for the maximum value of the range. Range values
     * are represented as strings in order to support all types 
     * of command arguments, but the maximum value should be of
     * appropriate data type for the argument it applies to.
     * 
     * @param ma
     *            The new maximum value
     */
    public abstract void setMaximum(final String ma);

}