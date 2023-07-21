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
package jpl.gds.dictionary.api.channel;


/**
 * PacketHeaderFieldName enumerates the different fields found in packet
 * headers that can be used to populate header channels, as defined in 
 * a header channel dictionary.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * PacketHeaderFieldName enumerates the different fields found in packet
 * headers. Two of the fields that relate to spacecraft time are not found in the
 * standard packet primary header, but are part of the secondary header. The
 * enumeration names must not be carelessly modified, as the header channel
 * dictionary relies on these names. If modification is needed,
 * header_channel.rnc and all header-channel dictionaries that depend on it must
 * subsequently be changed, as well.
 * <p>
 * For further information on space packet headers and field definitions, refer
 * to the CCSDS AOS Space Packet Protocol 133.0-B-1 Blue Book.
 * 
 *
 */
public enum PacketHeaderFieldName {

	/** Packet version number. */
	PACKET_VERSION_NUMBER,

	/** Packet type. */
	PACKET_TYPE,

	/** Secondary header presence flag. */
	SECONDARY_HEADER_FLAG,

	/** Application process identifier. */
	APID,

	/** Sequence, or grouping, flags. */
	SEQUENCE_FLAGS,

	/** Packet sequence counter. */
	PACKET_SEQUENCE_COUNT,

	/** Packet data length. */
	PACKET_DATA_LENGTH,

	/** 
	 * Coarse spacecraft clock. This is JPL-interpretation of the first
	 * 1-4 bytes of the time code field in the secondary packet header.
	 */
	COARSE_SPACECRAFT_TIME,

	/** 
	 * Fine spacecraft clock. This is JPL-interpretation of the second
	 * 1-4 bytes of the time code field in the secondary packet header.
	 */
	FINE_SPACECRAFT_TIME
}
