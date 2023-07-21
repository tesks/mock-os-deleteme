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
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.message.IProductAssembledMessage;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.shared.interfaces.EscapedCsvSupport;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.template.FullyTemplatable;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 *
 * ProductAssembledMessage is the message class that indicates a
 * complete product has been generated to storage by the product builder. It is
 * both an internal and an external message.
 * 
 *d
 */
public class ProductAssembledMessage extends Message implements IProductAssembledMessage, FullyTemplatable, EscapedCsvSupport
{
    private String transactionId;
    private IProductMetadataUpdater metadata;

    /**
     * Creates an instance of ProductAssembledMessage with a current event time.
     * @param md the product metadata provider
     * @param txId the product transaction ID
     *
     */
    public ProductAssembledMessage(final IProductMetadataUpdater md, final String txId) {
        super(ProductMessageType.ProductAssembled, System.currentTimeMillis());
        this.metadata = md;
        this.transactionId = txId;
    }

    /**
     * Creates an instance of ProductAssembledMessage with the given event time.
     *
     * @param time the wall clock time the event occurred
     */
    protected ProductAssembledMessage(final IAccurateDateTime time) {
        super(ProductMessageType.ProductAssembled, time.getTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTransactionId() {
        return this.transactionId;
    }

	/**
	 * Sets the product transaction ID. For use in message parsing only.
	 * 
	 * @param transactionId the transaction ID to set
	 */
	protected void setTransactionId(final String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IProductMetadataProvider getMetadata() {
        return this.metadata;
    }

	/**
	 * Sets the product metadata provider. For use only in message parsing.
	 * 
	 * @param metadata product metadata provider
	 */
	protected void setMetadata(final IProductMetadataProvider metadata) {
        this.metadata = IProductMetadataUpdater.class.cast(metadata);
        this.metadata.setPartial(false);
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "ProductAssembledMessage[ID=" + getTransactionId() + "]";
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
        if (this.transactionId != null) {
            map.put("transId", this.transactionId);
        }
 
        if (this.metadata != null) {
            map.put("metadata", true);
            map.put("class", this.metadata.getClass().getName());
            this.metadata.setTemplateContext(map);
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
    	sb.append("Complete");
    	sb.append(CSV_SEPARATOR);
    	sb.append(getEventTimeString());
    	sb.append(CSV_SEPARATOR);
    	sb.append(getEventTime().getTime());
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.transactionId != null ? this.transactionId : "");
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getProductType() != null ? this.metadata.getProductType() : "null");
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getApid());
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getTotalParts());
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getSclkStr());
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getDvtCoarse());
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getDvtFine());
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getSclkExact());
    	sb.append(CSV_SEPARATOR);
    	
    	final IAccurateDateTime scet = this.metadata.getScet();
    	sb.append(scet.getFormattedScet(true));
    	sb.append(CSV_SEPARATOR);
		sb.append(scet.getTime()); 
    	sb.append(CSV_SEPARATOR);
		sb.append(scet.getNanoseconds()); 
		sb.append(CSV_SEPARATOR);
    	
    	sb.append(this.metadata.getSolStr());
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getSolExact());
        sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getErtStr());
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getErtExact());
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getErtExactFine());
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getAbsoluteDataFile());
    	sb.append(CSV_SEPARATOR);
    	sb.append(this.metadata.getProductCreationTimeStr());
    	sb.append(CSV_SEPARATOR);
    	//reason field is empty for a complete product
    	
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
    	writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <CompleteProductMessage>
    	writer.writeAttribute(IMessage.EVENT_TIME_TAG,getEventTime() != null ? getEventTimeString() : "");
    	
    	
    	super.generateStaxXml(writer);
    	
    	writer.writeStartElement("TransactionId"); // <TransactionId>
    	writer.writeCharacters(this.transactionId != null ? this.transactionId : "");
    	writer.writeEndElement(); // </TransactionId>
    	
    	if(this.metadata != null)
    	{
    		this.metadata.generateStaxXml(writer);
    	}
    	
    	writer.writeEndElement(); // </ProductAssembledMessage>
    }

    /**
     *
     * ParseHandler is the message-specific SAX parse handler for creating this Message
     * from its XML representation.
     * 
     */
    public static class XmlParseHandler extends BaseXmlMessageParseHandler {
        private ProductAssembledMessage msg;
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


        @Override
        public void startElement(final String uri,
                                 final String localName,
                                 final String qname,
                                 final Attributes attr)
        throws SAXException {
            super.startElement(uri, localName, qname, attr);

            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(ProductMessageType.ProductAssembled))) {
            	setInMessage(true);
                final IAccurateDateTime d = getDateFromAttr(attr, IMessage.EVENT_TIME_TAG);
                this.msg = new ProductAssembledMessage(d);
                addMessage(this.msg);
			} else if (qname.endsWith("ProductMetadata")) {
                metadata = partFactory.createMetadataUpdater();
                msg.setMetadata(metadata);
			}
        }

        @Override
        public void endElement(final String uri,
                               final String localName,
                               final String qname)
        throws SAXException {
            super.endElement(uri, localName, qname);

            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(ProductMessageType.ProductAssembled))) {
            	setInMessage(false);
            } else if (qname.equalsIgnoreCase("TransactionId")) {
                this.msg.setTransactionId(getBufferText());
            } else if (this.metadata != null) {
                this.metadata.parseFromElement(qname, getBufferText());
            }
        }
    }


    @Override
    public String getOneLineSummary() {
        return "Final product " +
        (this.metadata == null ? "Unknown" : this.metadata.getFullPath()) +
        " of type " +
        (this.metadata == null ? "Unknown" : this.metadata.getProductType()) +
        " generated";
    }

	@Override
	public String getXmlRootName() {
		return MessageRegistry.getDefaultInternalXmlRoot(getType());
	}



	@Override
	public int getMtakFieldCount() {
		final Map<String,String> props = this.metadata.getMissionProperties();
    	return MTAK_FIELD_COUNT + props.size() * 2;
	}
}
