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
package jpl.gds.shared.cli.options;

import java.util.List;

/**
 * An interface to be implemented by ICommandLineOption classes that can
 * optionally check to see that the entered value is a member of a
 * restricted list of possible values.
 * 
 *
 * @param <T>
 *            the data type of the ICommandLineOption
 */
public interface IListCheckingOptionParser<T extends Object> {
    
    /**
     * Sets the list of restriction values. The value of the option
     * must be in this set to be valid.
     * 
     * @param listToSet List of T
     */
    public void setRestrictionList(List<T> listToSet);
    
    /**
     * Gets a non-modifiable copy of the list of restriction values.
     * 
     * @return List of T; may be null
     */
    public List<T> getRestrictionList();

}
