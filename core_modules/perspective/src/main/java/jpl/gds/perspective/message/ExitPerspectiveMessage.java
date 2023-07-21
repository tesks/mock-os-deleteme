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
package jpl.gds.perspective.message;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * ExitPerspectiveMessage is published to both the internal MPCS and the
 * external message bus when a user elects to exit the integrated GUI. The
 * application ID indicates which instance of the GUI this message applies to.
 *
 */
public class ExitPerspectiveMessage extends Message {


    private String applicationId;

    /**
     * Creates an instance of ExitPerspectiveMessage.
     */
    public ExitPerspectiveMessage() {
        super(PerspectiveMessageType.ExitPerspective, System.currentTimeMillis());
    }

    /**
     * Gets the application ID.
     * 
     * @return Returns the application id.
     */
    public String getApplicationId() {
        return this.applicationId;
    }

    /**
     * Sets the application Id.
     * 
     * @param applicationId
     *            The id to set.
     */
    public void setApplicationId(final String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ExitPerspective";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTemplateContext(final Map<String,Object> map) {
        super.setTemplateContext(map);
        if (this.applicationId != null) {
            map.put("applicationId", this.applicationId);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
	public void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <ExitPerspectiveMessage>
        writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());
        
        super.generateStaxXml(writer);

        writer.writeStartElement("guiApplicationId"); // <guiApplicationId>
        writer.writeCharacters(this.applicationId != null ? this.applicationId
                : "");
        writer.writeEndElement(); // </guiApplicationId>

        writer.writeEndElement(); // </ExitPerspectiveMessage>
    }

    /**
     * ParseHandler is the XML SAX parsing handler for this message.
     *
     * We do not want to process session configuration because we are
     * exiting and it may not be set properly anyway.
     *
     * See comments for endElement below.
     */
    public static class XmlParseHandler extends BaseXmlMessageParseHandler {
        private ExitPerspectiveMessage message = null;

        /**
         * {@inheritDoc}
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
         * java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(final String uri, final String localName,
                final String qname, final Attributes attr) throws SAXException {
            super.startElement(uri, localName, qname, attr);

            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(PerspectiveMessageType.ExitPerspective))) {
            	setInMessage(true);
                this.message = new ExitPerspectiveMessage();
                addMessage(this.message);
                final IAccurateDateTime d = getDateFromAttr(attr, IMessage.EVENT_TIME_TAG);
                this.message.setEventTime(d);
            }
        }


        /**
         * {@inheritDoc}
         * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
         * java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(final String uri, final String localName,
                final String qname) throws SAXException
        {
            
            super.endElement(uri, localName, qname);
            
            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(PerspectiveMessageType.ExitPerspective))) {
            	setInMessage(false);
            } else if (qname.equalsIgnoreCase("guiApplicationId")) {
        		this.message.setApplicationId(getBufferText());
        	} else {
        		super.endElement(uri, localName, qname);
        	}

        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "Exit Perspective";
    }
   
}
