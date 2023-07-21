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

import java.io.IOException;
import java.util.List;


/**
 * An interface to be implemented by binary message parsers.
 * 
 *
 * @since R8
 *
 */
public interface IBinaryMessageParseHandler {

    /**
     * Retrieves the array of Message objects resulting from the parse; must be
     * set by the subclass.
     * @return the Message object, or null if no parsing has taken place
     */
    public abstract IMessage[] getMessages();

    /**
     * Parses the content of the given byte arrays to a series of messages
     * 
     * @param content
     *            the binary data of each message
     * @return array of parsed message objects
     * @throws IOException
     *             Thrown if an Exception is encountered
     */
    public abstract IMessage[] parse(List<byte[]> content) throws IOException;

}
