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
import jpl.gds.eha.api.message.ISuspectChannelTable;
import jpl.gds.eha.api.message.ISuspectChannelsMessage;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.template.FullyTemplatable;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * SuspectChannelsMessage is used to transport the contents of the current suspect
 * channel file, which indicates channels whose value or alarms are currently
 * questionable.
 * 
 */
public class SuspectChannelsMessage extends Message implements FullyTemplatable, ISuspectChannelsMessage {

	private ISuspectChannelTable suspectTable = new SuspectChannelTable();

	/**
	 * Creates a new instance of SuspectChannelsMessage with an empty suspect channels table
	 * and the current date/time.
	 */
	public SuspectChannelsMessage() {
		super(EhaMessageType.SuspectChannels, System.currentTimeMillis());
	}

	    /**
     * Creates a new instance of SuspectChannelsMessage.
     * 
     * @param table
     *            the suspect channel table
     */
    public SuspectChannelsMessage(final ISuspectChannelTable table) {
        this();
        setSuspectTable(table);       
    }

    
	/**
     * @{inheritDoc}
     * @see jpl.gds.eha.api.message.ISuspectChannelsMessage#getSuspectTable()
     */
	@Override
    public ISuspectChannelTable getSuspectTable() {
		return suspectTable;
	}

	/**
	 * Sets a whole new suspect channel table into this message
	 * @param suspectTable the suspect channel table to set
	 */
	private void setSuspectTable(final ISuspectChannelTable suspectTable) {
		this.suspectTable = suspectTable;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
	 */
	@Override
	public String getOneLineSummary() {
		return "Suspect DN count: " + suspectTable.getChannelsWithSuspectDN().size() + ", " +
		"suspect EU count: " + suspectTable.getChannelsWithSuspectEU().size() + ", " +
		"suspect alarm count: " + suspectTable.getSuspectAlarmCount();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.IMessage#toString()
	 */
	@Override
	public String toString() {
		return getOneLineSummary();
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
	throws XMLStreamException {
		writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); //<SuspectChannelsMessage>
		writer.writeAttribute(IMessage.EVENT_TIME_TAG,getEventTimeString());
		super.generateStaxXml(writer);
		writer.writeStartElement("fromSSE"); //<fromSSE>
        writer.writeCharacters(String.valueOf(suspectTable.getTableSseContextFlag().isApplicationSse()));
		writer.writeEndElement(); //</fromSSE>

		if (suspectTable != null) {
			suspectTable.generateStaxXml(writer);
		}

		writer.writeEndElement(); //</SuspectChannelsMessage>
	}


	/**
	 * XmlParseHandler is the message-specific SAX parse handler for creating this Message
	 * from its XML representation.
	 * 
	 */
	public static class XmlParseHandler extends BaseXmlMessageParseHandler {
		private SuspectChannelsMessage msg;
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

			if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(EhaMessageType.SuspectChannels))) {
				setInMessage(true);
                final IAccurateDateTime d = getDateFromAttr(attr, IMessage.EVENT_TIME_TAG);
				msg = (SuspectChannelsMessage) ehaMessageFactory.createSuspectChannelsMessage();
				msg.setEventTime(d);
				addMessage(msg);
			} else if (msg != null) {
				((SuspectChannelTable)msg.getSuspectTable()).startElement(uri, localName, qname, attr);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void endElement(final String uri,
				final String localName,
				final String qname)
		throws SAXException {
			super.endElement(uri, localName, qname);

			if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(EhaMessageType.SuspectChannels))) {
				setInMessage(false);
				msg = null;
			} else if (msg != null) {
				((SuspectChannelTable)msg.getSuspectTable()).endElement(uri, localName, qname);
			}
		}
	}



	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.template.FullyTemplatable#getMtakFieldCount()
	 */
	@Override
	public int getMtakFieldCount() {
	    // This message does not go to MTAK
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.template.FullyTemplatable#getXmlRootName()
	 */
	@Override
	public String getXmlRootName() {
		return "SuspectChannelsMessage";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void setTemplateContext(final Map<String,Object> map) {
		super.setTemplateContext(map);
		if (getEventTime() != null) {
			map.put(IMessage.EVENT_TIME_TAG, getEventTimeString());
		} else {
			map.put(IMessage.EVENT_TIME_TAG, null);
		}

		map.put("fromSSE", this.isFromSse());// Deprecated for R8

		if (suspectTable != null) {
			suspectTable.setTemplateContext(map);
		}
	}
}
