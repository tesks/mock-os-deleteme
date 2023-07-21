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
package jpl.gds.dictionary.api.apid;



/**
 * This enumeration defines the valid classifications for the data contained
 * within packets with a specific APID. <br>
 * <br>
 * <b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br>
 * <br>
 * 
 * ApidContentType is an enumeration that defines all the valid classifications
 * for the data contained within packets labeled with an APID. As such, the
 * ApidContentType must be set on all IApidDefinition objects created by an
 * ApidDictionary implementation.
 * 
 *
 * 
 * @see IApidDefinition
 * @see ApidDefinitionFactory
 */
public enum ApidContentType {
	/**
	 * APID type is unset or content type is unrecognized by the GDS.
	 */
	UNKNOWN,
	/**
	 * APID content type is pre-channelized telemetry.
	 */
	PRE_CHANNELIZED,
	/**
	 * APID content type is event records (EVRs)
	 */
	EVR,
	/**
	 * APID content type is time correlation information.
	 */
	TIME_CORRELATION,
	/**
	 * APID content type is data product information.
	 */
	DATA_PRODUCT,
	/**
	 * APID content type is channelized telemetry to be extracted using a
	 * decommutation map.
	 */
	DECOM_FROM_MAP,
	/**
	 * APID content type is fill or data to be discarded.
	 */
	FILL,
	/**
	 * APID content type is CFDP PDU
	 */
	CFDP_DATA,
	/**
	 * APID content type is CFDP PROTOCOL
	 */
	CFDP_PROTOCOL,
    /**
     * APID content type is user defined and requires custom processing.
     */
	OTHER
}
