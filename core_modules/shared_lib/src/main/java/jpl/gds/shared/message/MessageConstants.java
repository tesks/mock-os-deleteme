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
package jpl.gds.shared.message;

/**
 * Constants used by the messaging subsystem.
 * 
 * @since R8
 *
 */
public class MessageConstants  {

    /**
     * The DSMS namespace used on external XML messages.
     */
    public static final String DSMS_NAMESPACE = "http://dsms.jpl.nasa.gov/globalschema";
    /**
     * The DSMS element prefix used in external XML messages.
     */
    public static final String DSMS_PREFIX = "dsms";
    /**
     * The DSMS XML namespace.
     */
    public static final String PROJECT_NAMESPACE = "http://dsms.jpl.nasa.gov/mpcs";
    /**
     * The prefix used when automatically creating external XML root element names for
     * messages from the internal message type name.
     */
    public static final String EXTERNAL_MESSAGE_ROOT_PREFIX = "MPCS";
    /**
     * The prefix used when automatically creating external message type name for
     * messages from the internal message type name.
     */
    public static final String EXTERNAL_MESSAGE_TYPE_PREFIX = "MPCS";
    /**
     * The prefix used when automatically creating external XML root element names for
     * messages from the internal message type name.
     */
    public static final String MESSAGE_ROOT_SUFFIX = "Message";
    /**
     * Suffix added to the internal message type name to produce the message schema name,
     * unless overridden.
     */
    public static final String SCHEMA_SUFFIX = "Message.rnc";
}
