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
package jpl.gds.product.api;

import org.xml.sax.SAXException;

import jpl.gds.shared.types.ByteArraySlice;

/**
 * A writer interface to be implemented by product part classes.
 * 
 *
 * @since R8
 */
public interface IProductPartUpdater extends IProductPartProvider {

	/**
	 * Sets the part virtual channel ID.
	 * 
	 * @param vcid the VCID to set
	 */
	void setVcid(int vcid);

	/**
	 * Sets the part grouping (record control) flags.
	 * @param flags the flags to set
	 */
	void setGroupingFlags(int flags);

	/**
	 * Sets the product part number.
	 * 
	 * @param partNumber part number to set
	 */
	void setPartNumber(int partNumber);

	/**
	 * Sets the part data offset in the overall product file.
	 * @param partOffset the offset to set
	 */
	void setPartOffset(long partOffset);

	/**
	 * Sets the part relay spacecraft ID.
	 * @param relayScid SCID to set
	 */
	void setRelayScid(int relayScid);

	/**
	 * Sets the product type identifier (APID).
	 * @param apid the APID to set
	 */
	void setApid(int apid);

	/**
	 * Sets the packet sequence number for the source packet for this product part.
	 * 
	 * @param packetSequenceNumber sequence to set
	 */
	void setPacketSequenceNumber(int packetSequenceNumber);

	/**
	 * Sets the product part data bytes.
	 * 
	 * @param data the bytes to set
	 */
	void setData(ByteArraySlice data);

	/**
	 * Sets the data length of the part.
	 * 
	 * @param partLength length to set
	 */
	void setPartLength(int partLength);

	/**
	 * Sets the part metadata.
	 * 
	 * @param metadata
	 */
	void setMetadata(IProductMetadataProvider metadata);

    /**
     * Sets the part PDU type.
     *
     * @param partPduType The PDU type to set.
     */
    void setPartPduType(IPduType partPduType);
    
    /**
     * Parses a field value in this product part from an XML element name and value.
     * @param elementName the XML element name
     * @param text the value of the element
     * @throws SAXException if there is a parsing issue
     */
    public void parseFromElement(final String elementName, final String text) throws SAXException;
}