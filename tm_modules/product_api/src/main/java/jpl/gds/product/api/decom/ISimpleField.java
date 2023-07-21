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
package jpl.gds.product.api.decom;

/**
 * An interface to be implemented by simple field decom classes.
 * 
 *
 */
public interface ISimpleField extends IPrimitiveField, IChannelSupport, IChannelTimestampSupport {
    /**
     * Gets the reference to the field that contains the length to this field, if any.
     * 
     * @return reference to simple field; may be null
     */
    public ISimpleField getLengthField();

    /**
     * Gets the value of the field that contains the length to this field, if any.
     * 
     * @return byte length of length prefix
     */
    public int getLengthValue(); 
}
