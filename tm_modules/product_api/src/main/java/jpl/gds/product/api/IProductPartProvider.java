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

import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.types.ByteArraySlice;
import jpl.gds.shared.xml.stax.StaxSerializable;

/**
 * A read-only interface to be implemented by product part classes.
 * 
 *
 * @since R8
 */
public interface IProductPartProvider extends StaxSerializable, Templatable {

    /** Part grouping flag indicating packet was not in a record group. **/
    int NOT_IN_GROUP = 3;
    /** Part grouping flag indicating packet was first in a record group. **/
    int FIRST_IN_GROUP = 1;
    /** Part grouping flag indicating packet was continuing a record group. **/
    int CONTINUING_GROUP = 0;
    /** Part grouping flag indicating packet was last a record group. **/
    int LAST_IN_GROUP = 2;

    /**
     * Performs validation on this product part and throws an exception if it fails.
     * @throws ProductException if validation fails
     * @Deprecated does nothing in the code
     */
    void validate() throws ProductException;

    /**
     * Gets the product builder transaction ID for this part.
     * @return the transaction ID
     */
    String getTransactionId();

    /**
     * Gets the sub-directory name for the product that this part belongs to, relative to the
     * top-level products directory created by the product builder.
     * @return the sub-directory name
     */
    String getDirectoryName();

    /**
     * Gets the filename for the product that this part belongs to, without directory path or
     * file extension
     * @return the filename, less extension
     */
    String getFilename();

    /**
     * Gets the virtual channel ID on which this product was received.
     * @return Returns the VCID.
     */
    int getVcid();

    /**
     * Gets the record grouping flags for the packet that was used to create this product part.
     * Values are mission-specific
     * @return the grouping flags
     */
    int getGroupingFlags();

    /**
     * Gets the number of this part within the product. Note that whether the part number starts
     * at 0 or 1 is mission specific.
     * @return the part number
     */
    int getPartNumber();

    /**
     * Gets the byte offset of this part within the product.
     * @return the part offset
     */
    long getPartOffset();

    /**
     * Gets the relay spacecraft ID for this part.
     * @return the relay SCID
     */
    int getRelayScid();

    /**
     * Gets the application process ID (APID) for this part.
     * @return the APID
     */
    int getApid();

    /**
     * Gets the source packet sequence number, the sequence number of the packet
     * from which this part was created.
     * @return the sequence number
     */
    int getPacketSequenceNumber();

    /**
     * Gets the actual part data as a ByteArraySlice.
     * @return the part data, as a ByteArraySlice, positioned at the start of the part data.
     */
    ByteArraySlice getData();

    /**
     * Gets the data length in bytes of this part.
     * @return the part length
     */
    int getPartLength();

    /**
     * Returns the product metadata associated with this part.
     *  
     * @return mission-specific product metadata object
     */
    IProductMetadataProvider getMetadata();

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.xml.stax.StaxSerializable#toXml()
     */
    @Override
    String toXml();

	/**
	 * Gets the PDU type of the part.
	 * 
	 * @return IPduType
	 */
	IPduType getPartPduType();
}