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
package jpl.gds.jms.message;

import jpl.gds.jms.util.JmsMessageServiceExceptionFactory;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.MessageServiceException;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.openwire.OpenWireFormat;
import org.apache.activemq.openwire.v12.ActiveMQBytesMessageMarshaller;
import org.apache.activemq.openwire.v12.ActiveMQMessageMarshaller;
import org.apache.activemq.openwire.v12.ActiveMQTextMessageMarshaller;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A wrapper class for JMS message service messages, to hide the actual message objects used by the current message
 * service implementation from publishers and subscribers.
 * <p>
 * Added serialization. See https://docs.oracle.com/javase/8/docs/platform/serialization/spec/serialTOC.html
 */
public class JmsExternalMessage implements IExternalMessage {

    private static final long                  serialVersionUID = -2659767955876009372L;
    // if we need another implementation of this down the road, make an interface out of this and add setter
    private static       ExternalMessageSerDes serDes           = new ExternalMessageSerDes();

    private final transient Message innerMessage;

    /**
     * Constructor.
     *
     * @param wrappedMessage the message object to wrap
     */
    public JmsExternalMessage(final Message wrappedMessage) {
        if (wrappedMessage == null) {
            throw new IllegalArgumentException("inner message object cannot be null");
        }
        innerMessage = wrappedMessage;
    }

    @Override
    public Message getMessageObject() {
        return innerMessage;
    }

    @Override
    public boolean isBinary() {
        return innerMessage instanceof BytesMessage;
    }

    @Override
    public byte[] getBinaryContent() throws MessageServiceException {
        if (!isBinary()) {
            return null;
        } else {
            byte[] result;
            try {
                final BytesMessage bmsg = (BytesMessage) innerMessage;
                bmsg.reset();
                result = new byte[(int) bmsg.getBodyLength()];
                bmsg.readBytes(result);
            } catch (final JMSException e) {
                throw JmsMessageServiceExceptionFactory
                        .createException("Cannot extract binary content from message", e);
            }
            return result;
        }
    }

    @Override
    public String getTextContent() throws MessageServiceException {
        if (isBinary()) {
            return null;
        } else {
            try {
                return ((TextMessage) innerMessage).getText();
            } catch (final JMSException e) {
                throw JmsMessageServiceExceptionFactory.createException("Cannot extract text content from message", e);
            }
        }
    }

    /**
     * Temporary field for setting the final Message via #readResolve()
     */
    private transient Message messageFromRead;

    /**
     * Java serialization. See https://docs.oracle.com/javase/8/docs/platform/serialization/spec/output.html#a861
     *
     * @param out object output stream
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        serDes.serialize(innerMessage, out, isBinary());
    }

    /**
     * Java deserialization. See https://docs.oracle.com/javase/8/docs/platform/serialization/spec/input.html#a2971
     *
     * @param in object input stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        this.messageFromRead = serDes.deserialize(in);
    }

    /**
     * Java deserialization; controls the object returned. Necessary to set the final innerMessage field. See
     * https://docs.oracle.com/javase/8/docs/platform/serialization/spec/input.html#a5903
     *
     * @return IExternalMessage
     */
    private Object readResolve() {
        return new JmsExternalMessage(this.messageFromRead);
    }

    /**
     * Serializer/deserializer for external JMS messages
     */
    private static class ExternalMessageSerDes {

        private static final ActiveMQBytesMessageMarshaller bytesMarshaller = new ActiveMQBytesMessageMarshaller();
        private static final ActiveMQTextMessageMarshaller  textMarshaller  = new ActiveMQTextMessageMarshaller();
        private static final OpenWireFormat                 wireFormat      = new OpenWireFormat();

        void serialize(Message message, ObjectOutputStream out, boolean isBinary) throws IOException {
            out.defaultWriteObject();
            out.writeBoolean(isBinary);
            ByteArrayOutputStream     data       = new ByteArrayOutputStream();
            DataOutput                dataOutput = new DataOutputStream(data);
            ActiveMQMessageMarshaller marshaller;
            if (isBinary) {
                marshaller = bytesMarshaller;
            } else {
                marshaller = textMarshaller;
            }
            marshaller.looseMarshal(wireFormat, message, dataOutput);
            out.writeObject(data.toByteArray());
        }

        Message deserialize(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            boolean                   isBinary  = in.readBoolean();
            ByteArrayInputStream      bais      = new ByteArrayInputStream((byte[]) in.readObject());
            DataInput                 dataInput = new DataInputStream(bais);
            ActiveMQMessageMarshaller marshaller;
            ActiveMQMessage           unmarshal;
            if (isBinary) {
                marshaller = bytesMarshaller;
                unmarshal = new ActiveMQBytesMessage();
            } else {
                marshaller = textMarshaller;
                unmarshal = new ActiveMQTextMessage();
            }
            marshaller.looseUnmarshal(wireFormat, unmarshal, dataInput);
            return unmarshal;
        }
    }

}
