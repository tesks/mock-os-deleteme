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
package jpl.gds.eha.impl.message;


import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IEhaMessageFactory;
import jpl.gds.message.api.IStreamMessage;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;

/**
 * EndChannelProcMessage is used to signal the end of a stream of related
 * channel messages.
 * 
 */
public class EndChannelProcMessage extends Message implements IStreamMessage {

    /**
     * Stream id.
     */
    protected String streamId;

    /**
     * Creates an instance of EndChannelProcMessage.
     * 
     * @param streamId
     *            a unique string used to identify this sequence of channel
     *            messages.
     */
    public EndChannelProcMessage(final String streamId) {
        super(EhaMessageType.EndChannelProcessing);
        this.streamId = streamId;
    }

    /**
     * Returns the stream id.
     * 
     * @return id
     */
    @Override
    public String getStreamId() {
        return this.streamId;
    }

    /**
     * Sets the stream id.
     * 
     * @param streamId
     *            id
     */
    @Override
    public void setStreamId(final String streamId) {
        this.streamId = streamId;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "EndChannelProcMessage [stream id = " + this.streamId + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTemplateContext(final Map<String,Object> map) {
        super.setTemplateContext(map);
        map.put("streamId", (this.streamId == null ? "" : this.streamId));
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <EndChannelProcMessage>
        writer.writeAttribute("streamId", this.streamId != null ? this.streamId
                : "");
        super.generateStaxXml(writer);
        writer.writeEndElement(); // </EndChannelProcMessage>
    }


    /**
     * ParseHandler is the message-specific SAX parse handler for creating this
     * Message from its XML representation.
     * 
     */
    protected static class XmlParseHandler extends BaseXmlMessageParseHandler {
        private IStreamMessage msg;
		private final IEhaMessageFactory ehaMessageFactory;
        
        /**
         * Constructor.
         * 
         * @param appContext
         *            the current application context
         */
        public XmlParseHandler(final ApplicationContext appContext) {
			this.ehaMessageFactory = appContext.getBean(IEhaMessageFactory.class);
        	
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(final String uri, final String localName, final String qname,
                final Attributes attr) throws SAXException {
            super.startElement(uri, localName, qname, attr);

            if (qname.equals(MessageRegistry.getDefaultInternalXmlRoot(EhaMessageType.EndChannelProcessing))) {
            	setInMessage(true);
                this.msg = ehaMessageFactory.createEndChannelProcMessage(attr.getValue("streamId"));
                addMessage(this.msg);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "End of channel processing for stream " + this.streamId;
    }
  
}