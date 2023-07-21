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
 * StartChannelProcMessage is used to group related channel messages. If it is
 * published, it will be followed by some number of channel messages, and
 * terminated by an EndChannelProcMessage. All messages will have the same
 * stream ID.
 * 
 */
public class StartChannelProcMessage extends Message implements IStreamMessage {

    /**
     * Message stream id.
     */
    protected String streamId;
    
    /**
     * Creates an instance of StartChannelProcMessage.
     *
     * @param streamId a unique string used to identify this sequence of channel messages.
     */
    public StartChannelProcMessage(final String streamId) {
        super(EhaMessageType.StartChannelProcessing);
        this.streamId = streamId;
    }
   
    /**
     * Returns the streamId.
     * @return stream id
     */
    @Override
    public String getStreamId() {
        return this.streamId;
    }

    /**
     * Sets the streamId.
     * @param streamId stream id
     */
    @Override
    public void setStreamId(final String streamId) {
        this.streamId = streamId;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "StartChannelProcMessage [stream id = " + streamId + "]";
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
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
    	writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <StartChannelProcMessage>
    	writer.writeAttribute("streamId",this.streamId != null ? this.streamId : "");
    	super.generateStaxXml(writer);
    	writer.writeEndElement(); // </StartChannelProcMessage>
    }
    
    /**
     * ParseHandler is the message-specific SAX parse handler for creating this Message
     * from its XML representation.
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
        public void startElement(final String uri,
                                 final String localName,
                                 final String qname,
                                 final Attributes attr)
        throws SAXException {
            super.startElement(uri, localName, qname, attr);
            
            if (qname.equals(MessageRegistry.getDefaultInternalXmlRoot(EhaMessageType.StartChannelProcessing))) {
            	setInMessage(true);
                this.msg = ehaMessageFactory.createStartChannelProcMessage(attr.getValue("streamId"));
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
        return "Started channel processing for stream " + this.streamId;
    }
}