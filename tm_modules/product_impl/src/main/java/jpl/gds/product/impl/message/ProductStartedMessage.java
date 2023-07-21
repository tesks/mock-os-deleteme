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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.product.api.message.IProductStartedMessage;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.template.FullyTemplatable;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * ProductStartedMessage is the message classes indicating that 
 * generation of a product has been started by the product builder. It is both 
 * an internal and an external message.
 * 
 */
public class ProductStartedMessage extends Message implements IProductStartedMessage, FullyTemplatable
{
    private String prodType;
    private String id;
    private int apid;
    private int totalParts;
    private int vcid;

    private static final int MTAK_FIELD_COUNT = 8;
    
    /**
     * Creates an instance of ProductPartMessage with the given event time.
     * @param time the time the event occurred
     *
     */
    protected ProductStartedMessage(final IAccurateDateTime time) {
        super(ProductMessageType.ProductStarted, time.getTime());
    }
    
    /**
     * Creates an instance of ProductStartedMessage with the 
     * given product type and transaction ID and a current event time.
     *
     * @param type the product type String
     * @param typeId product type ID (e.g., APID)
     * @param vcid product virtual channel
     * @param txId product transaction ID
     * @param totalParts total parts in data product
     */
    public ProductStartedMessage(final String type, final int typeId, final int vcid, final String txId, final int totalParts) {
        super(ProductMessageType.ProductStarted);
        this.prodType = type;
        this.id = txId;
        this.apid = typeId;
        this.vcid = vcid;
        setEventTime(new AccurateDateTime(System.currentTimeMillis()));
    }


    /**
     * {@inheritDoc}
     */
    @Override
	public int getVcid() {
        return this.vcid;
    }
  
	/**
	 * Sets the virtual channel ID. For use only in message parsing.
	 * 
	 * @param vcid virtual channel ID
	 */
	protected void setVcid(final int vcid) {
        this.vcid = vcid;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
	public int getApid() {
        return apid;
    }

    /**
     * Sets the product type ID, or APID. For use only in message parsing.
     * 
     * @param typeId ID to set
     */
	protected void setApid(final int typeId) {
        this.apid = typeId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public int getTotalParts() {
        return totalParts;
    }

    /**
     * Sets the total product parts. For use only in message parsing.
     * 
     * @param totalParts part count
     */
	protected void setTotalParts(final int totalParts) {
        this.totalParts = totalParts;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
	public String getProductType() {
        return this.prodType;
    }

    /**
     * Sets the product type name. For use only in message parsing.
     * 
     * @param type type name to set
     */
	protected void setProductType(final String type) {
        this.prodType = type;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
	public String getTransactionId() {
        return id;
    }

    /**
     * Sets the product transaction ID. For use only in message parsing.
     * 
     * @param transId the ID to set
     */
	protected void setTransactionId(final String transId) {
        this.id = transId;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "ProductStartedMessage[type=" + prodType + ",id=" + id + "]";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setTemplateContext(final Map<String,Object> map) {
        super.setTemplateContext(map);
        if (getEventTime() != null) {
        	map.put(IMessage.EVENT_TIME_TAG, getEventTimeString());  
        } else {
            map.put("eventTime", null);
        }
        if (this.prodType != null) {
            map.put("type", this.prodType);
        } else {
            map.put("type", "");
        }
        if (id != null) {
           map.put("transId", this.id);
        } else {
            map.put("transId", "");
        }
        
        //null value is checked in velocity template
		map.put("vcid", vcid);
		
        map.put("apid", Integer.valueOf(this.apid));

        map.put("totalParts", Integer.valueOf(this.totalParts));
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
    	writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <ProductStartedMessage>
    	writer.writeAttribute("eventTime",getEventTime() != null ? getEventTimeString() : "");  	
    	
    	super.generateStaxXml(writer);
    	
    	writer.writeStartElement("type"); // <type>
    	writer.writeCharacters(this.prodType != null ? this.prodType : "");
    	writer.writeEndElement(); // </type>
    	
    	writer.writeStartElement("vcid"); // <vcid>
    	writer.writeCharacters(Long.toString(this.vcid));
    	writer.writeEndElement(); // </vcid>
    	
    	writer.writeStartElement("apid"); // <apid>
    	writer.writeCharacters(Long.toString(this.apid));
    	writer.writeEndElement(); // </apid>
    	
    	writer.writeStartElement("totalParts"); // <totalParts>
    	writer.writeCharacters(Long.toString(this.totalParts));
    	writer.writeEndElement(); // </totalParts>
    	
    	writer.writeStartElement("transactionId"); // <transactionId>
    	writer.writeCharacters(this.id != null ? this.id : "");
    	writer.writeEndElement(); // </transactionId>
    	
    	writer.writeEndElement(); // </ProductStartedMessage>
    }
    
    /**
     *
     * ParseHandler is the message-specific SAX parse handler for creating this message
     * from its XML representation.
     * 
     */
    public static class XmlParseHandler extends BaseXmlMessageParseHandler {
        private ProductStartedMessage msg;

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
            
            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(ProductMessageType.ProductStarted))) {
            	setInMessage(true);
                final IAccurateDateTime d = getDateFromAttr(attr, "eventTime");
                this.msg = new ProductStartedMessage(d);
                addMessage(this.msg);
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
            
            
            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(ProductMessageType.ProductStarted))) {
            	setInMessage(false);
            } else if (qname.equals("type")) {
                this.msg.setProductType(getBufferText());
            } else if (qname.equalsIgnoreCase("transactionId")) {
                this.msg.setTransactionId(getBufferText());
            } else if (qname.equalsIgnoreCase("apid")) {
                this.msg.setApid(getIntFromBuffer());
            } else if (qname.equalsIgnoreCase("vcid")) {
                this.msg.setVcid(getIntFromBuffer());
            } else if (qname.equalsIgnoreCase("totalParts")) {
                this.msg.setTotalParts(getIntFromBuffer());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOneLineSummary() {
        return "Product builder transaction " + this.id + 
        " started for product type " + this.prodType;
    }


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.template.FullyTemplatable#getXmlRootName()
	 */
	@Override
	public String getXmlRootName() {
		return MessageRegistry.getDefaultInternalXmlRoot(getType());
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.template.FullyTemplatable#getMtakFieldCount()
	 */
	@Override
	public int getMtakFieldCount() {
		return MTAK_FIELD_COUNT;
	}
	 
}
