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
package jpl.gds.product.impl;


import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import jpl.gds.product.api.IPduType;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.IProductPartUpdater;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.builder.IProductMissionAdaptor;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.types.ByteArraySlice;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.stax.StaxSerializable;
import jpl.gds.shared.xml.stax.StaxStreamWriterFactory;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * This is the base class for product parts, which are extracted
 * from product telemetry packets.
 *
 */
public abstract class AbstractProductPart implements IProductPartUpdater, Templatable, StaxSerializable {
    protected static final Tracer          log  = TraceManager.getTracer(Loggers.TLM_PRODUCT);

    
    private int groupingFlags;
    private final ByteArraySlice data = new ByteArraySlice();
    private ByteArraySlice pktData;
    
    /*
     * Limit on the acceptable product part offset value. If we accept offset
     * values specified in the PDU blindly, it can cause product files with
     * astronomical size. That causes further problems. And that's also probably
     * a FSW bug.
     */
    private final long maxAllowedPartOffset;

    /**
     * RElay spacecraft ID for this part.
     */
    protected int relayScid;
    /**
     * Sequence number for the packet that contained this part.
     */
    protected int packetSequenceNumber;
    /**
     * Number of this part in the product as a whole.
     */
    protected int partNumber;
    /**
     * Offset of this part in the product as a whole.
     */
    protected long partOffset;
    /**
     * Byte length of this part's data.
     */
    protected int partLength;
    /**
     * Metadata associated with this part.
     */
    protected IProductMetadataUpdater metadata;

    /**
     * All of the subclasses were storing this and returning
     * it so moved it to the abstract version.  Also made a single PDU type that all missions should use.
     * The part PDU type.
     * 
     * Using the PDU interface.
     */
    protected IPduType partPduType;
    
    /** Current application context */
    protected ApplicationContext appContext;
    /** Product builder mission adapter */
    protected IProductMissionAdaptor adaptor;
    /** High volume product builder object factory */
    protected IProductBuilderObjectFactory partFactory;

    /**
     * Creates an empty instance of AbstractProductPart with the given metadata object.
     * @param appContext the current application context
     * @param md the mission specific product metadata object
     */
    public AbstractProductPart(final ApplicationContext appContext, final IProductMetadataUpdater md) {
    	this.appContext = appContext;
    	this.adaptor = appContext.getBean(IProductMissionAdaptor.class);
    	this.partFactory = appContext.getBean(IProductBuilderObjectFactory.class);
        this.metadata = partFactory.convertToMetadataUpdater(md);
        this.maxAllowedPartOffset = appContext.getBean(IProductPropertiesProvider.class).getMaximumPartOffset();
        log.setAppContext(appContext);
    }

    /**
     * Creates an instance of AbstractProductPart from a packet message.
     * 
     * @param appContext the current application context
     * @param packetMsg the IPacketMessage containing part data
     * @param md the mission specific product metadata object
     */
    public AbstractProductPart(final ApplicationContext appContext, final ITelemetryPacketMessage packetMsg, final IProductMetadataUpdater md) {
        this.metadata = md;
    	this.appContext = appContext;
    	this.adaptor = appContext.getBean(IProductMissionAdaptor.class);
    	this.maxAllowedPartOffset = appContext.getBean(IProductPropertiesProvider.class).getMaximumPartOffset();
        log.setAppContext(appContext);
    	
        /* Use header length from packet info */
        final ITelemetryPacketInfo info = packetMsg.getPacketInfo();
        final int index = info.getPrimaryHeaderLength() + info.getSecondaryHeaderLength();
        this.pktData = new ByteArraySlice(packetMsg.getPacket(), index, packetMsg.getPacket().length - index);
        /*  Get ERT from packet info rather than DSN info. */
        this.metadata.setErt(info.getErt());
        this.metadata.setScet(info.getScet());
        this.metadata.setSol(info.getLst());
        this.groupingFlags = info.getGroupingFlags();
        /* Get SCIDs from packet info rather than DSN and frame info. */
        this.metadata.setScid(info.getScid());
        this.relayScid = info.getRelayScid();
        this.metadata.setApid(info.getApid());
        this.metadata.setSclk(info.getSclk());
        this.packetSequenceNumber = info.getSeqCount();
        this.data.update(pktData.array, pktData.offset, pktData.length);
    }

    /**
     * Perform mission specific part validation. 
     * 
     * @throws ProductException validation of part fails.
     */
    @Override
	public void validate() throws ProductException {
    		/**
    		 * Moved this code from the disk product storage
    		 * handle message method.  Makes more sense to be part of the validation check
    		 * for the part and allows for overrides in the future.
    		 */
        boolean invalid = false;
        String invalidReason = null;

        if (getPartNumber() < 0) {
            invalidReason = "Product part number is " + getPartNumber()
                    + " which is less than 0.";
            invalid = true;
        } else if (getMetadata().getTotalParts() != 0
                && getPartNumber() > getMetadata().getTotalParts()) {
            invalidReason = "Metadata indicates "
                    + getMetadata().getTotalParts()
                    + " but received part number is " + getPartNumber()
                    + ".";
            invalid = true;
        } else if (getMetadata().getErt() == null) {
            invalidReason = "Product part ERT is null.";
            invalid = true;
        } else if (getMetadata().getScet() == null) {
            invalidReason = "Product part SCET is null.";
            invalid = true;
        } else if (getData() == null) {
            invalidReason = "Product part does not contain data.";
            invalid = true;
        }

        if (invalid) {
        		final String m = "Found invalid product part for transaction "
                            + getFilename() + ", part number "
                            + getPartNumber() + ". Reason: "
                            + invalidReason;

            throw new ProductException(m);
        }
    } 
    

    /**
     * Gets the product builder transaction ID for this part.
     * @return the transaction ID
     */
    @Override
    public abstract String getTransactionId();

    /**
     * Gets the sub-directory name for the product that this part belongs to, relative to the
     * top-level products directory created by the product builder.
     * @return the sub-directory name
     */
    @Override
    public String getDirectoryName() {
        return this.metadata.getDirectoryName();
    }

    /**
     * Gets the filename for the product that this part belongs to, without directory path or
     * file extension
     * @return the filename, less extension
     */
    @Override
    public String getFilename() {
        return this.metadata.getFilename();
    }

    /**
     * Gets the virtual channel ID on which this product was received.
     * @return Returns the VCID.
     */
    @Override
    public int getVcid() {
        return this.metadata.getVcid();
    }

    /**
     * Sets the virtual channel ID on which this product was received.
     *
     * @param vcid The VCID to set.
     */
    @Override
	public void setVcid(final int vcid) {
        this.metadata.setVcid(vcid);
    }

    /**
     * Gets the record grouping flags for the packet that was used to create this product part.
     * Values are mission-specific
     * @return the grouping flags
     */
    @Override
    public int getGroupingFlags() {
        return this.groupingFlags;
    }

    /**
     * Sets the record grouping flags for the packet that was used to create this product part.
     * Values are mission-specific
     * @param flags the grouping flags to set
     */
	@Override
    public void setGroupingFlags(final int flags) {
        this.groupingFlags = flags;
    }


    /**
     * Gets the number of this part within the product. Note that whether the part number starts
     * at 0 or 1 is mission specific.
     * @return the part number
     */
    @Override
    public int getPartNumber() {
        return this.partNumber;
    }

    /**
     * Sets the number of this part within the product. Note that whether the part number starts
     * at 0 or 1 is mission specific.
     * @param partNumber the part number to set
     */
    @Override
	public void setPartNumber(final int partNumber) {
        this.partNumber = partNumber;
    }

    /**
     * Gets the byte offset of this part within the product.
     * @return the part offset
     */
    @Override
    public long getPartOffset() {
        return this.partOffset;
    }

    /**
     * Sets the byte offset of this part within the product.
     * @param partOffset the part offset to set, in bytes
     */
    @Override
	public void setPartOffset(final long partOffset) {
        this.partOffset = partOffset;
    }

    /**
     * Sets the relay spacecraft ID for this part.
     * @param relayScid the scid to set
     */
    @Override
	public void setRelayScid(final int relayScid) {
        this.relayScid = relayScid;
    }

    /**
     * Gets the relay spacecraft ID for this part.
     * @return the relay SCID
     */
    @Override
    public int getRelayScid() {
        return this.relayScid;
    }

    /**
     * Sets the application process ID (APID) for this part.
     * @param apid the ID to set
     */
    @Override
	public void setApid(final int apid) {
        this.metadata.setApid(apid);
    }

    /**
     * Gets the application process ID (APID) for this part.
     * @return the APID
     */
    @Override
    public int getApid() {
        return this.metadata.getApid();
    }

    /**
     * Sets the source packet sequence number, the sequence number of the packet
     * from which this part was created.
     * @param packetSequenceNumber the sequence number to set
     */
    @Override
	public void setPacketSequenceNumber(final int packetSequenceNumber) {
        this.packetSequenceNumber = packetSequenceNumber;
    }

    /**
     * Gets the source packet sequence number, the sequence number of the packet
     * from which this part was created.
     * @return the sequence number
     */
    @Override
    public int getPacketSequenceNumber() {
        return this.packetSequenceNumber;
    }

    /**
     * Sets the actual part data bytes from a ByteArraySlice.
     * @param data the source ByteArraySlice, positioned at the start of the part data.
     */
    @Override
	public void setData(final ByteArraySlice data) {
        this.data.update(data.array, data.offset, data.length);
    }

    /**
     * Gets the actual part data as a ByteArraySlice.
     * @return the part data, as a ByteArraySlice, positioned at the start of the part data.
     */
    @Override
    public ByteArraySlice getData() {
        return this.data;
    }

    /**
     * Sets the data length in bytes of this part.
     * @param partLength the length to set
     */
    @Override
	public void setPartLength(final int partLength) {
        this.partLength = partLength;
    }

    /**
     * Gets the data length in bytes of this part.
     * @return the part length
     */
    @Override
    public int getPartLength() {
        return this.partLength;
    }

    /**
     * Used for verifying that sufficient data remains in a product packet for continued
     * processing.
     * @param off the starting offset into the packet data buffer
     * @param inc the incremental offset (number of bytes) we must support for extracting 
     * the next field from the buffer
     * @param buf the packet data buffer
     * @param message the name of the next field we are attempting to extract from the product data 
     * (for logging)
     * @throws ProductException if there is insufficient data remaining in the packet
     * buffer to process the next field
     */
    protected void checkOffset(final int off, final int inc, final byte[] buf, final String message) throws ProductException {
        if ( (off + inc) > buf.length) {
            throw new ProductException("Ran out of product packet bytes getting " + message + ": filename " + getFilename() + " part number= " + getPartNumber() + ";");
        } 
    }
    
    /**
     * Used for verifying that the offset value specified in the product part is
     * reasonable/acceptable for processing.
     * 
     * @param offset
     *            offset value in DPDU
     * @throws ProductException
     *             if the offset is above the acceptable value
     */
    protected void checkValidPduOffset(final long offset) throws ProductException {
        
        if (offset > maxAllowedPartOffset) {
            throw new ProductException("PDU offset of " + offset
                    + " is above configured limit of "
                    + maxAllowedPartOffset + ": filename "
                    + getFilename() + " part number= " + getPartNumber() + ";");
        }
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTemplateContext(final Map<String,Object> map) {
        this.metadata.setTemplateContext(map);
        map.put("seq", Integer.valueOf(this.packetSequenceNumber));
        map.put("relay", Integer.valueOf(this.relayScid));
        if (getTransactionId() != null) {
            map.put("transId", getTransactionId());
        } else {
            map.put("transId", "");
        }
        map.put("partOffset", Long.valueOf(this.partOffset));
        map.put("partNumber", Long.valueOf(this.partNumber));
        map.put("partLength", Integer.valueOf(this.partLength));
    }

    /**
     * Parses an attribute of this object from the given XML element name and value.
     * @param elementName the name of the XML element being parsed
     * @param text the value of the element
     * @throws SAXException if parsing of the attribute fails
     */
    @Override
    public void parseFromElement(final String elementName, final String text) throws SAXException {
        this.metadata.parseFromElement(elementName, text);
        final String newElem = elementName.toLowerCase();
        if (newElem.equals("sourcepacketsequencecount")) {
            setPacketSequenceNumber(XmlUtility.getIntFromText(text));
        } else if (newElem.equals("partlength")) {
            setPartLength(XmlUtility.getIntFromText(text));
        } else if (newElem.equals("partnumber")) {
            setPartNumber(XmlUtility.getIntFromText(text));
        } else if (newElem.equals("partoffset")) {
            setPartOffset(XmlUtility.getIntFromText(text));
        } else if (newElem.equals("relayscid")) {
            setRelayScid(XmlUtility.getIntFromText(text));
        } 
    }

    /**
     * Creates a 3 digit character string representing the given number,
     * left padded with 0s.
     * @param n the number to represent
     * @return the padded string representation of the number
     */
    protected String zeroPad(final int n) {
        if (n > 999) {
            return Integer.toString(n);
        }
        if (n > 99) {
            return "0" + n;
        }
        if (n > 9) {
            return "00" + n;
        }
        return "000" + n;
    }

    /**
     * Returns the product metadata associated with this part.
     *  
     * @return mission-specific product metadata object
     */
    @Override
    public IProductMetadataProvider getMetadata() {
        return this.metadata;
    }

    /**
     * Sets the product metadata associated with this part.
     *
     * @param metadata The metadata to set.
     */
    @Override
	public void setMetadata(final IProductMetadataProvider metadata) {
        this.metadata = partFactory.convertToMetadataUpdater(metadata);
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.xml.stax.StaxSerializable#toXml()
     */
    @Override
    public String toXml()
    {
        String output = "";
        try
        {
            output = StaxStreamWriterFactory.toXml(this);
        }
        catch(final XMLStreamException e)
        {
            e.printStackTrace();
            log.error("Could not transform ProductPart object to XML: ", ExceptionTools.getMessage(e));
        }
        
        return(output);
	}
    
    /**
     * Returns the part PDU type.
     * @return the part PDU type.
     */
    @Override
    public IPduType getPartPduType() {
        return partPduType;
    }
    
    /**
     * Sets the part PDU type.
     *
     * @param partPduType The PDU type to set.
     */
    @Override
    public void setPartPduType(final IPduType partPduType) {
        this.partPduType = partPduType;
    }
}
