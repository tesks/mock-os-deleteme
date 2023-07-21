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
package jpl.gds.tm.service.api.frame;

import org.xml.sax.SAXException;

import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.context.api.filtering.IScidFilterable;
import jpl.gds.context.api.filtering.IVcidFilterable;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.serialization.frame.Proto3TelemetryFrameInfo;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.xml.stax.StaxSerializable;
import jpl.gds.station.api.InvalidFrameCode;

/**
 * The ITelemetryFrameInfo interface is to be implemented by all classes that must
 * provide frame messages to adaptation implementations.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * IFrameInfo defines methods needed by the frame extraction adapters for 
 * obtaining data about the source frames.
 * Adaptations should only access Frame Info objects via this interface.
 * Interaction with the actual Frame Info implementation classes in an adapter
 * implementation is contrary to multi-mission development standards.
 * 
 * @version 1.0 - Initial Implementation
 * @version 2.0 - Changed names of several methods, added several methods,
 *                updated javadoc. (MPCS-3923 -11/3/14)
 * @version 3.0 - Major changes related to new transfer frame dictionary and
 *                interfaces. Extended Templatable, StaxSerializable.
 *                (3/29/16 - MPCS-7993)
 * @version 3.1 - Added build and load functions for or creating and parsing
 * 				  Proto3TelemetryFrameInfo messages.
 * 				  (MPCS-4599 - 10/30/17)
 */
public interface ITelemetryFrameInfo extends IScidFilterable, IVcidFilterable, Templatable, StaxSerializable {

	/**
	 * Sets the transfer frame definition, from the transfer frame dictionary.
	 * @param format The transfer frame definition to set
	 */
	void setFrameFormat(ITransferFrameDefinition format);

	/**
	 * Gets the transfer frame definition object, which contains the dictionary 
	 * definition of the transfer frame.
	 * 
	 * @return the transfer frame format
	 */

	ITransferFrameDefinition getFrameFormat();

	/**
	 * Retrieves the reason this frame was marked bad, as an InvalidFrameCode
	 * enumeration value. Will return null if the frame was not bad.
	 * 
	 * @return the reason the frame was marked bad, as an enum value; may be
	 *         null
	 */
	InvalidFrameCode getBadReason();

	/**
	 * Retrieves the header object for this frame instance. It is important to
	 * note that the return value can be null, because IFrameInfo objects can be
	 * used to represent frame metadata pulled from a packet header, and there
	 * may have been no actual frame.
	 * 
	 * @return the header object; may be null
	 */
	ITelemetryFrameHeader getHeader();

	/**
	 * Sets the header object for this frame instance. Required only for
	 * frame header channelization.
	 * 
	 * @param header the header to set
	 */
	void setHeader(ITelemetryFrameHeader header);

	/**
	 * Sets the reason this frame was marked bad, as an InvalidFrameCode
	 * enumeration value. The value should be null if the frame was not bad.
	 * 
	 * @param badReason the reason to set
	 */
	void setBadReason(InvalidFrameCode badReason);

	/**
	 * Sets the dictionary type name of the transfer frame. This value
	 * may be null if this IFrameInfo object is created from a packet.
	 * 
	 * @param typeName the type name from the transfer_frame.xml file
	 */
	void setType(String typeName);

	/**
	 * Gets the dictionary transfer frame type as defined by transfer_frame.xml.
	 * This value may be null if this IFrameInfo object was created from a
	 * packet.
	 * 
	 * @return the type name; may be null
	 */
	String getType();

	/**
	 * Sets the UENENCODED frame size in bytes including sync mark (ASM or PN-code) 
	 * and encoding block. This is sometimes referred to as the CADU length.
	 * This value will be 0 if this IFrameInfo object is created from a
	 * packet.
	 * 
	 * @param sz the frame size in bytes
	 */
	void setCADUSize(int sz);

	/**
	 * Gets the size of the UNENCODED frame in bytes, including the sync marker (ASM or
	 * PN-code) and encoding block. This is sometimes referred to as the CADU
	 * length. This value will be 0 if this IFrameInfo object is created from a
	 * packet.
	 * 
	 * @return the size in bytes
	 */
	int getCADUSize();

	/**
	 * Sets the size in bytes of the frame header and the ASM together. This
	 * should include the size of any secondary header. This value will be 0 if
	 * this IFrameInfo object is created from a packet.
	 * 
	 * @param sz
	 *            the header size in bytes
	 */
	void setHdrSize(int sz);

    /**
     * Sets the data area size of the frame in bytes. The data area is where
     * packet or PDU data is present in the frame, and does not include frame
     * ASM or headers (including M_PDU header), or trailing error control,
     * operational control, or encoding fields. This value will be 0 if this
     * IFrameInfo object is created from a packet.
     * 
     * @param size
     *            The data area size in bytes
     *            
     */
	void setDataAreaSize(int size);

    /**
     * Sets the data pointer byte offset. Depending on the frame format, this
     * may be a first packet pointer or a last bitstream pointer. This value will
     * be 0 if this IFrameInfo object was created from a packet.
     * 
     * @param fpp
     *            the data pointer (offset) in bytes
     *            
     */
	void setDataPointer(int fpp);

	/**
	 * Sets the frame version number. This is assumed to be the version number
	 * exactly as it is written in the frame header. For instance, the version
	 * of a CCSDS AOS version 2 transfer frame, as extracted from the header, is
	 * actually 1. If the frame format being used has no version number
	 * equivalent, then the version should be set to 0. Do not assume that this
	 * version corresponds to an AOS version.
	 * 
	 * @param version
	 *            the version value to set
	 * 
	 * MPCS-3923 - 11/3/14. Rename from setVersion1TFto
	 *          setVersion and change argument type to int. Changed the meaning
	 *          of the method.
	 * 
	 */
	void setVersion(int version);

	/**
	 * Sets the frame version number. This is assumed to be the version number
	 * exactly as it is written in the frame header. For instance, the version
	 * of a CCSDS AOS version 2 transfer frame, as extracted from the header, is
	 * actually 1. If the frame format being used has no version number
	 * equivalent, then the version should be set to 0. Do not assume that this
	 * version corresponds to an AOS version. 
	 * 
	 * @return the frame version number
	 * 
	 * MPCS-3923 - Extensive update to javadoc to explain usage
	 *          of this method, and deprecated it.
	 * 
	 * @deprecated AMPCS does not use frame version
	 */
	@Deprecated
	int getVersion();

	/**
	 * Sets the flag indicating this frame is a fill/idle frame. These frames
	 * are not processed or stored by AMPCS in the default configuration.
	 * 
	 * @param idle
	 *            true if this is an idle frame
	 */
	void setIdle(boolean idle);

	/**
	 * Sets the flag indicating whether this is a dead frame, containing data
	 * not to be processed by AMPCS. These frames are not processed or stored by
	 * AMPCS.
	 * 
	 * @param dc
	 *            true to set the deadcode flag; false to turn it off
	 */
	void setDeadCode(boolean dc);

	/**
	 * Sets the flag indicating whether this is a dead frame, containing data
	 * not to be processed by AMPCS. These frames are not processed or stored by
	 * AMPCS.
	 * 
	 * @return true if this frame is dead; false if not
	 */
	boolean isDeadCode();

	/**
	 * Gets the virtual channel ID associated with this frame. 
	 * 
	 * @return the vcid
	 */
	@Override
    Integer getVcid();

	/**
	 * Sets the virtual channel ID associated with this frame.
	 * 
	 * @param vcid the vcid to set
	 */
	void setVcid(Integer vcid);

	/**
	 * Gets the sequence number (virtual channel frame counter) for this frame.
	 * 
	 * @return the sequence number
	 */
	int getSeqCount();

	/**
	 * Sets the sequence number (virtual channel frame counter) for this frame.
	 * 
	 * @param seq the sequence number
	 */
	void setSeqCount(int seq);

	/**
	 * Gets the maximum possible value for the data pointer in this frame.
	 * @return the maximum byte offset of the data pointer
	 * 
	 */
	int getMaxDataPointer();

	/** 
	 * Sets the maximum possible sequence counter (VCFC) for this frame, i.e,
	 * the maximum sequence count before the value rolls over.
	 * 
	 * @param vcfcMax the max vcfc
	 * 
	 */
	void setMaxSeqCount(int vcfcMax);

	/** 
	 * Gets the maximum possible sequence counter (VCFC) for this frame, , i.e,
	 * the maximum sequence count before the value rolls over.
	 * @return the max vcfc
	 */
	int getMaxSeqCount();

    /**
     * Gets the size of all the frame header fields (including primary,
     * secondary, and M/B_PDU header) and the ASM together, in bytes. This value
     * will be 0 if this IFrameInfo object is populated from a packet.
     * 
     * @return the frame header size in bytes
     */
	int getHdrSize();

	/**
     * Gets the data area size of the frame in bytes. The data area is where
     * packet or PDU data is present in the frame, and does not include frame
     * ASM or headers (including M_PDU header), or trailing error control,
     * operational control, or encoding fields. This value will be 0 if this
     * IFrameInfo object is created from a packet.
  
	 * 
	 * @return the data area size in bytes
	 * 
	 */
	int getDataAreaSize();

	/**
	 * Gets the numeric spacecraft ID associated with this frame.
	 * 
	 * @return the scid
	 */
	@Override
    Integer getScid();

	/**
	 * Sets the numeric spacecraft ID associated with this frame.
	 * 
	 * @param scid The scid
	 */
	void setScid(Integer scid);

	/**
	 * Gets the byte offset of the first packet within this frame. This value
	 * will be 0 if this IFrameInfo object is populated from a packet.
	 * 
	 * @return the packet offset in bytes
	 * 
	 */
	int getDataPointer();

	/**
	 * Gets the flag indicating if this is an idle/fill frame.
	 * 
	 * @return true if the frame is idle; false if not
	 */
	boolean isIdle();

	/**
	 * Sets the flag indicating if this is a bad frame. This may mean it has a
	 * bad header, or that it failed a checksum check, or is a reed-solomon
	 * encoded frame that could not be corrected.
	 * 
	 * @param bad
	 *            a boolean to indicate if it is bad frame or not.
	 */
	void setBad(boolean bad);

	/**
	 * Gets the flag indicating if this is a bad frame. This may mean it has a
	 * bad header, or that it failed a checksum check, or is a reed-solomon
	 * encoded frame that could not be corrected.
	 * 
	 * @return true if the frame is bad
	 */
	boolean isBad();

	/**
	 * Parses and sets a field value from the XML element value with the given
	 * name.
	 * 
	 * @param elementName the name of the XML element in the XML message
	 * @param text the value of the element
	 * @throws SAXException If there is an issue parsing.
	 */
	void parseFromElement(String elementName, String text)
			throws SAXException;
	
    /**
     * Returns the binary size of the current instance, i.e., the length of the
     * array that would be returned by the next toBinary(). call Note that if
     * any attribute in the object changes, the resulting binary length may
     * change, so do not cache this value.
     * 
     * @return byte length required to serialize this instance
     */
	int getBinarySize();

    /**
     * Serializes the entire content of the ITelemetryFrameInfo object to a byte
     * array.
     * 
     * @return byte array representing object content
     */
	byte[] toBinary();

    /**
     * Serializes the entire content of the ITelemetryFrameInfo object to the
     * given byte array, starting at the given offset, and return the ending
     * offset.
     * 
     * @param buff
     *            byte array to write content to
     * @param startOff
     *            starting offset in the array
     * @return new offset after the write
     */
	int toBinary(byte[] buff, int startOff);

    /**
     * De-serializes the entire content of the ITelemetryFrameInfo object from a
     * byte array and sets all object attributes.
     * 
     * @param buff
     *            byte array containing object content
     * @param startOff
     *            starting offset of the ITelemetryFrameInfo data in the given
     *            byte array.
     * @return the next offset in the byte array after parsing
     */
	int parseFromBinary(byte[] buff, int startOff);

	/**
	 * Transforms the content of the ITelemetryFrameInfo object into a Protobuf
	 * message
	 * 
	 * @return the Protobuf message representing this ITelemetryFrameInfo object
	 */
	Proto3TelemetryFrameInfo build();
	
	/**
	 * Transforms this ITelemetryFrameInfo object into the object represented in
	 * the contents of the supplied Protobuf message
	 * 
	 * @param msg a Protobuf message representing an ITelemetryFrameInfo object
	 */
	void load(Proto3TelemetryFrameInfo msg);

}
