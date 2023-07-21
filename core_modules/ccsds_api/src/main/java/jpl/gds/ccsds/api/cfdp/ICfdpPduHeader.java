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
package jpl.gds.ccsds.api.cfdp;

/**
 * An interface to be implemented by CFDP PDU headers.
 * 
 *
 * @since R8
 *
 */
public interface ICfdpPduHeader {

   /** Maximum PDU size in bytes. TODO - what is the real max, if any, for PDUs? */
   public static final int MAX_PDU_DATA_LENGTH = 65536;
   
    /**
     * The byte length of the fixed PDU header.
     */
    public static final int FIXED_PDU_HEADER_LENGTH = 4;

    /**
     * Gets the version number from the PDU header.
     * 
     * @return CCSDS version number
     */
    public int getVersion();
    
    /**
     * Gets the PDU type from the PDU header.
     * 
     * @return CfdpPduType
     */
    public CfdpPduType getType();
    
    /** 
     * Gets the direction (towards receiver, towards sender) from the PDU header.
     * 
     * @return CfdpPduDirection 
     */
    public CfdpPduDirection getDirection();
    
    /**
     * Gets the transmission mode (acknowledged, unacknowledged) from the PDU header.
     * 
     * @return CfdpTransmissionMode
     */
    public CfdpTransmissionMode getTransmissionMode();
    
    /**
     * Indicates whether the PDU has a CRC (checksum).
     * 
     * @return true if there is a CRC, false if not
     */
    public boolean hasCrc();
    
    /**
     * Gets the data length from the PDU header.
     * 
     * @return PDU data length in bytes
     */
    public int getDataLength();
    
    /**
     * Gets the byte length of the CFDP transaction number from the
     * PDU header.
     * 
     * @return length of transaction sequence number in bytes
     */
    public int getTransactionSequenceLength();
    
    /**
     * Gets the transaction sequence number from the PDU header.
     * Always a long, as the sequence number may be from 1 to 8 bytes.
     * Should be treated as unsigned.
     * 
     * @return transaction sequence number
     */
    public long getTransactionSequenceNumber();
    
    /**
     * Gets the byte length of the CFDP entity IDs from the PDU
     * header.
     * 
     * @return entity ID length in bytes
     */
    public int getEntityIdLength();
    
    /**
     * Gets the source entity ID from the PDU header. Always a long,
     * as the actual length of the ID may vary. Should be treated as
     * unsigned.
     * 
     * @return the source entity ID
     */
    public long getSourceEntityId();
    
    /**
     * Gets the destination entity ID from the PDU header. Always a long,
     * as the actual length of the ID may vary. Should be treated as
     * unsigned.
     * 
     * @return the destination entity ID
     */
    public long getDestinationEntityId();
    
    /**
     * Gets the length of the PDU header in bytes.
     * 
     * @return header length in bytes
     */
    public int getHeaderLength();
    
    /**
     * Gets the flag indicating whether the PDU header (TODO - or is it the PDU itself?) 
     * is valid.
     * 
     * @return true if valid, false if not
     */
    public boolean isValid();
    
    /**
     * Loads the fixed PDU header from a byte array.
     * 
     * @param buffer byte array containing the header
     * @param offset starting offset of the header in the array
     * @return next offset after the header in the array
     */
    public int loadFixedHeader(byte[] buffer, int offset);
    
    /**
     * Loads the entire PDU header, including the variable length portion, 
     * from a byte array.
     * 
     * @param buffer byte array containing the header
     * @param offset starting offset of the header in the array
     * @return next offset after the header in the array
     */
    public int load(byte[] buffer, int offset);
}
