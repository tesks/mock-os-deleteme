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
/**
 * 
 */
package jpl.gds.evr.api.service.extractor;

import jpl.gds.evr.api.IEvr;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * The <code>IEvrAdapter</code> interface is to be implemented by all EVR
 * extraction adapters.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * <code>IEvrAdapter</code> defines methods needed by the EVR extraction
 * adapters, so that the EVR processing system can use the adapter to perform
 * mission-specific extraction of EVRs from packets. The primary job of such an
 * adapter is to extract <code>IEvr</code> objects from an
 * <code>IPacketMessage</code> and return them. <code>IEvrAdapter</code> objects
 * should only be created through the <code>EvrAdapterFactory</code> class, and
 * should interact with EVRs and packets only through the <code>IEvrValue</code>
 * and <code>IPacketMessage</code> interfaces. Interaction with the actual
 * implementation classes in an <code>IEvrAdapter</code> implementation is
 * contrary to multi-mission development standards.
 * <p>
 * It is also important to note than the class that implements this interface
 * should create <code>IEvr</code> objects using the <code>EvrFactory</code>.
 * 
 *
 * @see ITelemetryPacketMessage
 * @see IEvr
 */
public interface IEvrExtractor {

	/**
	 * Extracts and formats (i.e. deserializes) the <code>IEvr</code> object
	 * from the given CCSDS <code>IPacketMessage</code>. The passed
	 * <code>IPacketMessage</code> is guaranteed to contain a valid packet with
	 * an EVR APID, including the primary and secondary packet headers. The
	 * packet may contain either recorded or real-time content.
	 * 
	 * In the case of a fatal EVR with some corruption, attempt to return the
	 * best possible EVR without throwing an exception.
	 * 
	 * @param pm
	 *            the <code>IPacketMessage</code> containg the EVR packet to
	 *            process
	 * @return an <code>IEvr</code> object; if no EVR is extracted from the
	 *         packet, a null value must be returned
	 * 
	 * @throws EvrExtractorException
	 *             if any error occurs during the extraction of the EVR, such as
	 *             due to corruption (e.g. parameter count or lengths are
	 *             invalid)
	 */
	public IEvr extractEvr(ITelemetryPacketMessage pm) throws EvrExtractorException;
	
	/**
	 * Create an EVR object from the bytes in a CCSDS packet. Return
	 * <code>null</code> if the packet does not hold an EVR. Throw an exception
	 * if there is some corruption, such as when the parameter count or lengths
	 * are invalid. In the case of a fatal EVR with some corruption, attempt to
	 * return the best possible EVR without throwing an exception.
	 * 
	 * @param buff
	 *            byte array buffer of a packet that may contain an EVR
	 * @param startOffset
	 *            starting address of buffer memory to process
	 * @param length
	 *            length of the buffer memory to process, starting from
	 *            <code>startOffset</code>
	 * @param apid
	 *            APID number of the packet, to which <code>buff</code> data
	 *            belongs to
	 * @param vcid
	 *            virtual channel ID number of the source frame
	 * @param dssId
	 *            receiving station for this EVR           
	 * @param seqCount
	 *            sequence count of the packet
	 * @return <code>IEvr</code> object, with contents processed from
	 *         <code>buff</code>
	 * @throws EvrExtractorException
	 *             thrown if problem is encountered while processing the EVR
	 */
	public IEvr extractEvr(final byte[] buff, final int startOffset, final int length,
			final int apid, final Integer vcid, final int dssId, final int seqCount) throws EvrExtractorException;

	/**
	 * Return the current offset position
	 * @return offset position
	 */
	int getCurrentOffset();

}
