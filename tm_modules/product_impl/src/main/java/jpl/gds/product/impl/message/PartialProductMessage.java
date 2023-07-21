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
package jpl.gds.product.impl.message;

import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.builder.AssemblyTrigger;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.message.IPartialProductMessage;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.shared.interfaces.EscapedCsvSupport;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.template.FullyTemplatable;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 *
 * PartialProductMessage is the message class that indicates a
 * partial product has been generated to the output filesystem by the product
 * builder. This is both an internal and external message.
 * 
 */
public class PartialProductMessage extends Message implements IPartialProductMessage, FullyTemplatable, EscapedCsvSupport
{
	private String transactionId;
	private String transactionLog;
	private AssemblyTrigger reason;
	private IProductMetadataUpdater metadata;
	
	/**
	 * Constructor for subclasses and the message parser.
	 * @param time message event time
	 */
    protected PartialProductMessage(final IAccurateDateTime time) {
	    super(ProductMessageType.PartialProduct, time.getTime());
	}
	
	/**
	 * Creates an instance of PartialProductMessage with a current
	 * event time.
	 * @param txId product transaction ID
	 * @param txLog product transaction log file path
	 * @param why reason for the partial generation
	 * @param md product metadata
	 */
	public PartialProductMessage(final String txId, final String txLog, final AssemblyTrigger why, final IProductMetadataUpdater md) {
        this(new AccurateDateTime(System.currentTimeMillis()), txId, txLog, why, md);
	}

	/**
	 * Creates an instance of PartialProductMessage with the given
	 * event time
	 * @param time the wall clock time that the event occurred
	 * @param txId product transaction ID
     * @param txLog product transaction log file path
     * @param why reason for the partial generation
     * @param md product metadata
	 */
    public PartialProductMessage(final IAccurateDateTime time, final String txId, final String txLog,
            final AssemblyTrigger why, final IProductMetadataUpdater md) {
		super(ProductMessageType.PartialProduct);
		setEventTime(time);
		this.metadata = md;
		if (this.metadata != null) {
		    this.metadata.setPartial(true);
		}
		this.transactionId = txId;
		this.transactionLog = txLog;
		this.reason = why;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public String getTransactionId() {
		return transactionId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public String getTransactionLog() {
		return transactionLog;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public AssemblyTrigger getReason() {
		return reason;
	}

	/**
	 * Sets the transaction log file name. For message parsing only.
	 * @param log transaction log file name
	 */
	protected void setTransactionLog(final String log) {
	    this.transactionLog = log;
	}
	
	/**
     * Sets the transaction ID. For message parsing only.
     * @param id transaction ID
     */
	protected void setTransactionId(final String id) {
	    this.transactionId = id;
	}
	
	/**
	 * Sets the assembly reason. For message parsing only.
	 * 
	 * @param reason assembly trigger
	 */
	protected void setReason(final AssemblyTrigger reason) {
		this.reason = reason;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public IProductMetadataProvider getMetadata() {
		return metadata;
	}

	/**
	 * Sets the product metadata. For message parsing only.
	 * 
	 * @param md product metadata
	 */
	protected void setMetadata(final IProductMetadataUpdater md) {
	    md.setPartial(true);
		this.metadata = md;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.IMessage#toString()
	 */
	@Override
	public String toString() {
		return "PartialProductMessage [ID=" + getTransactionId() + "]";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTemplateContext(final Map<String,Object> map) {
		super.setTemplateContext(map);
		if (getEventTimeString() != null) {
			map.put(IMessage.EVENT_TIME_TAG, getEventTimeString());
		}
		if (transactionId != null) {
			map.put("transId", transactionId);
		}
		if (transactionLog != null) {
			map.put("transLog", transactionLog);
		}
		if (reason != null) {
			map.put("reason", reason);
		}
		if (metadata != null) {
			map.put("metadata", true);
			map.put("class", metadata.getClass().getName());
			metadata.setTemplateContext(map);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getEscapedCsv()
	{
		final StringBuilder sb = new StringBuilder(256);

		sb.append("prod");
		sb.append(CSV_SEPARATOR);
		sb.append("Partial");
		sb.append(CSV_SEPARATOR);
		sb.append(getEventTimeString());
		sb.append(CSV_SEPARATOR);
		sb.append(getEventTime().getTime());
		sb.append(CSV_SEPARATOR);
		sb.append(transactionId != null ? transactionId : "");
		sb.append(CSV_SEPARATOR);
		sb.append(metadata.getProductType() != null ? metadata.getProductType() : "null");
		sb.append(CSV_SEPARATOR);
		sb.append(metadata.getApid());
		sb.append(CSV_SEPARATOR);
		sb.append(metadata.getTotalParts());
		sb.append(CSV_SEPARATOR);
		sb.append(metadata.getSclkStr());
		sb.append(CSV_SEPARATOR);
		sb.append(metadata.getDvtCoarse());
		sb.append(CSV_SEPARATOR);
		sb.append(metadata.getDvtFine());
		sb.append(CSV_SEPARATOR);
		sb.append(metadata.getSclkExact());
		sb.append(CSV_SEPARATOR);
		
    	final IAccurateDateTime scet = this.metadata.getScet();
    	sb.append(scet.getFormattedScet(true));
		sb.append(CSV_SEPARATOR);
		sb.append(scet.getTime()); 
		sb.append(CSV_SEPARATOR);
		sb.append(scet.getNanoseconds()); 
		sb.append(CSV_SEPARATOR);
		
		sb.append(metadata.getSolStr());
		sb.append(CSV_SEPARATOR);
		sb.append(metadata.getSolExact());
		sb.append(CSV_SEPARATOR);
		
        sb.append(metadata.getErtStr());
		sb.append(CSV_SEPARATOR);
		sb.append(metadata.getErtExact());
		sb.append(CSV_SEPARATOR);
		sb.append(metadata.getErtExactFine());
		sb.append(CSV_SEPARATOR);
		sb.append(metadata.getAbsoluteDataFile());
		sb.append(CSV_SEPARATOR);
		sb.append(metadata.getProductCreationTimeStr());
		sb.append(CSV_SEPARATOR);
		sb.append(reason != null ? reason : "");
    	
    	final Map<String,String> props = this.metadata.getMissionProperties();
    	for(final Entry<String, String> s : props.entrySet())
    	{
    		sb.append(CSV_SEPARATOR);
    		sb.append(s.getKey());
    		sb.append(CSV_SEPARATOR);
    		sb.append(props.get(s.getValue()));
    	}

		return(sb.toString());
	}

	@Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
	{
		writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <PartialProductMessage>
		writer.writeAttribute(IMessage.EVENT_TIME_TAG,getEventTime() != null ? getEventTimeString() : "");

		super.generateStaxXml(writer);
		
		writer.writeStartElement("TransactionId"); // <TransactionId>
		writer.writeCharacters(transactionId != null ? transactionId : "");
		writer.writeEndElement(); // </TransactionId>

		writer.writeStartElement("TransactionLog"); // <TransactionLog>
		writer.writeCharacters(transactionLog != null ? transactionLog : "");
		writer.writeEndElement(); // </TransactionLog>

		writer.writeStartElement("Reason"); // <Reason>
		writer.writeCharacters(reason != null ? reason.toString() : "");
		writer.writeEndElement(); // </Reason>

		if(metadata != null)
		{
			metadata.generateStaxXml(writer);
		}

		writer.writeEndElement(); // </PartialProductMessage>
	}

	/**
	 * ParseHandler is the message-specific SAX parse handler for creating this message
	 * from its XML representation.
	 */
	public static class XmlParseHandler extends BaseXmlMessageParseHandler {
		private PartialProductMessage msg;
		private IProductMetadataUpdater metadata;
		private final IProductBuilderObjectFactory partFactory;

		/**
		 * Constructor.
		 * 
		 * @param appContext the current application context
		 */
		public XmlParseHandler(final ApplicationContext appContext) {
			super();
			this.partFactory = appContext.getBean(IProductBuilderObjectFactory.class);
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

			if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(ProductMessageType.PartialProduct))) {
				setInMessage(true);
                final IAccurateDateTime d = getDateFromAttr(attr, IMessage.EVENT_TIME_TAG);
				msg = new PartialProductMessage(d);
				addMessage(msg);
			} else if (qname.endsWith("ProductMetadata")) {
			    metadata = partFactory.createMetadataUpdater();
			    msg.setMetadata(metadata);
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


			if (qname.equalsIgnoreCase("PartialProductMessage")) {
				setInMessage(false);
			} else if (qname.equalsIgnoreCase("TransactionLog")) {
				msg.setTransactionLog(getBufferText());
			} else if (qname.equalsIgnoreCase("TransactionId")) {
				msg.setTransactionId(getBufferText());
			} else if (qname.equalsIgnoreCase("reason")) {
				msg.setReason(Enum.valueOf(AssemblyTrigger.class, getBufferText()));
			} else if (metadata != null) {
				metadata.parseFromElement(qname, getBufferText());
			}
		}
	}


	@Override
	public String getOneLineSummary() {
		return "Partial Product " +
		(metadata == null ? "Unknown" : metadata.getFullPath()) +
		" of type " +
		(metadata == null ? "Unknown" : metadata.getProductType()) +
		" generated by transaction " +
		(transactionId == null ? "Unknown" : transactionId) +
		(reason == null ? "" : " due to " + reason);
	}

	@Override
	public String getXmlRootName() {
		return MessageRegistry.getDefaultInternalXmlRoot(getType());
	}


	@Override
	public int getMtakFieldCount() {
		return MTAK_FIELD_COUNT;
	}
	
}
