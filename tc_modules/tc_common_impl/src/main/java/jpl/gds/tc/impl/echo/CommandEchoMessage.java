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
package jpl.gds.tc.impl.echo;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.tc.api.echo.ICommandEchoMessage;
import jpl.gds.tc.api.message.CommandMessageType;

/**
 * CommandEchoMessage is used to send data read from the Command
 * Echo interface in the downlink processor on the internal bus. 
 *
 */
public class CommandEchoMessage extends Message implements ICommandEchoMessage
{
    
    private final byte data[];

    /**
     * Creates an instance of CommandEchoMessage.
     * 
     * @param buff
     *            a buffer containing raw command echo data
     * @param offset
     *            the offset of the command echo data in the buffer
     * @param byteLen
     *            the length of the command echo data in bytes
     */
    public CommandEchoMessage(final byte buff[], final int offset, final int byteLen) {
        super(CommandMessageType.CommandEcho, System.currentTimeMillis());
        data = new byte[byteLen];
        java.lang.System.arraycopy(buff, offset, data, 0, byteLen);
    }

    /**
     * Retrieves the command echo data buffer.
     * 
     * @return a byte array containing the data
     */
    @Override
    public byte[] getData() {
        return data;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "MSG:" + getType() + " " + getEventTimeString() + " data length="
                + data.length;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
        writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));

    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "Command Echo bytes=" + data.length;
    }
}