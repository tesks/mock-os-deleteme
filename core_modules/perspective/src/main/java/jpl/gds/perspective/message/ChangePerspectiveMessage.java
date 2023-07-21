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
 * ChangePerspectiveMessage is published to both the internal MPCS and the
 * external message bus when a user elects to load another GUI perspective. The
 * application ID member identifies which instance of the GUI the message
 * applies to.
 * 
 */
public class ChangePerspectiveMessage extends Message {

    private String location = null;
    private String applicationId = null;

    /**
     * Creates an instance of ChangePerspectiveMessage.
     */
    public ChangePerspectiveMessage() {
        super(PerspectiveMessageType.ChangePerspective, System.currentTimeMillis());
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ChangePerspective" + "location";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTemplateContext(final Map<String,Object> map) {
        super.setTemplateContext(map);
        if (this.location != null) {
            map.put("location", this.location);
        } else {
            map.put("location", "");
        }
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
        writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <ChangePerspectiveMessage>
        writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());
        super.generateStaxXml(writer);

        writer.writeStartElement("location"); // <location>
        writer.writeCharacters(this.location != null ? this.location : "");
        writer.writeEndElement(); // </location>

        writer.writeStartElement("guiApplicationId"); // <guiApplicationId>
        writer.writeCharacters(this.applicationId != null ? this.applicationId
                : "");
        writer.writeEndElement(); // </guiApplicationId>

        writer.writeEndElement(); // </ChangePerspectiveMessage>
    }

    /**
     * Sets the application ID.
     * 
     * @param applicationId
     *            id to set.
     */
    public void setApplicationId(final String applicationId) {
        this.applicationId = applicationId;
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
     * ParseHandler is the XML SAX parsing handler for this message.
     */
    public static class XmlParseHandler extends BaseXmlMessageParseHandler {
        private ChangePerspectiveMessage message = null;

        /**
         * {@inheritDoc}
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
         * java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(final String uri, final String localName,
                final String qname, final Attributes attr) throws SAXException {
            super.startElement(uri, localName, qname, attr);

            if (qname.equals(MessageRegistry.getDefaultInternalXmlRoot(PerspectiveMessageType.ChangePerspective))) {
            	setInMessage(true);
                this.message = new ChangePerspectiveMessage();
                addMessage(this.message);
                final IAccurateDateTime d = this.getDateFromAttr(attr, IMessage.EVENT_TIME_TAG);
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
                final String qname) throws SAXException {
            super.endElement(uri, localName, qname);

            if (qname.equals(MessageRegistry.getDefaultInternalXmlRoot(PerspectiveMessageType.ChangePerspective))) {
            	setInMessage(false);
            } else if (qname.equalsIgnoreCase("location")) {
                this.message.setLocation(getBufferText());
            } else if (qname.equalsIgnoreCase("guiApplicationId")) {
                 this.message.setApplicationId(getBufferText());
            }
        }
    }

    /**
     * Gets the directory path for the new perspective
     * 
     * @return Returns the new location (directory path).
     */
    public String getLocation() {
        return this.location;
    }

    /**
     * Sets the new location (directory path).
     * 
     * @param location
     *            The location to set.
     */
    public void setLocation(final String location) {
        this.location = location;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "Change Perspective to " + this.location;
    }
    
}