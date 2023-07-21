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
package jpl.gds.eha.channel.api;

import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * The IChannelSampleFactory interface is used to provide customer derivations
 * the ability to create child channel samples. This interface cannot be used
 * directly by derivations. It is used by AMPCS algorithm initialization.
 * Customer classes should use the convenience methods in IDerivationAlgorithm
 * for creating child samples. It is the implementation of those methods that
 * rely on this interface.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * 
 */
@CustomerAccessible(immutable = true)
public interface IChannelSampleFactory {

    /**
     * Creates a numeric channel value with the given channel ID and DN value.
     * Automatically looks in the channel definition map for an existing
     * definition for the channel. Will also create boolean channel value, which
     * will be false if the input DN has a 0 value, and true otherwise.
     * 
     * @param cid
     *            the Channel ID of the channel sample
     * @param dn
     *            the DN Value for the channel sample
     * @return the new IChannelValue
     * 
     * @throws InvalidChannelValueException
     *             if the data type of the DN is not consistent with the channel
     *             dictionary definition
     * @throws IllegalStateException
     *             if the channel does not exist in the dictionary or is not a
     *             numeric or boolean channel
     */
    public IChannelValue create(String cid, Number dn) throws IllegalStateException, InvalidChannelValueException;

    /**
     * Creates a String channel value with the given channel ID and DN value.
     * Automatically looks in the channel definition map for an existing
     * definition for the channel.
     * 
     * @param cid
     *            the channel ID; may not be null
     * @param value
     *            the string value; may not be null
     * 
     * @return the new IChannelValue instance
     * 
     * @throws InvalidChannelValueException
     *             if the data type of the DN is not consistent with the channel
     *             dictionary definition
     * @throws IllegalStateException
     *             if the channel does not exist in the dictionary or is not a
     *             string channel
     */
    public IChannelValue create(String cid, String value) throws IllegalStateException, InvalidChannelValueException;

    /**
     * Creates a boolean channel value with the given channel ID and DN value.
     * Automatically looks in the channel definition map for an existing
     * definition for the channel.
     * 
     * @param cid
     *            the channel ID; may not be null
     * @param value
     *            the boolean value
     * 
     * @return the new IChannelValue instance
     * 
     * @throws InvalidChannelValueException
     *             if the data type of the DN is not consistent with the channel
     *             dictionary definition
     * @throws IllegalStateException
     *             if the channel does not exist in the dictionary or is not a
     *             boolean channel
     */
    public IChannelValue create(String cid, boolean value) throws IllegalStateException, InvalidChannelValueException;
}