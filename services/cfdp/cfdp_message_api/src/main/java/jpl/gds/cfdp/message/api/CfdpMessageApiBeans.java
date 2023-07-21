/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.cfdp.message.api;

/**
 * An interface that defines bean names for the Spring configuration in the CFDP
 * message projects.
 * 
 *
 * @since R8
 */
public interface CfdpMessageApiBeans {

	/**
	 * Bean name for ICfdpIndicationMessage object.
	 */
	public static final String CFDP_INDICATION_MESSAGE = "CFDP_INDICATION_MESSAGE";

	/**
	 * Bean name for ICfdpFileGenerationMessage object.
	 */
	public static final String CFDP_FILE_GENERATION_MESSAGE = "CFDP_FILE_GENERATION_MESSAGE";

	/**
	 * Bean name for ICfdpFileUplinkFinishedMessage object.
	 */
	public static final String CFDP_FILE_UPLINK_FINISHED_MESSAGE = "CFDP_FILE_UPLINK_FINISHED_MESSAGE";

	/**
	 * Bean name for ICfdpRequestReceivedMessage object.
	 */
	public static final String CFDP_REQUEST_RECEIVED_MESSAGE = "CFDP_REQUEST_RECEIVED_MESSAGE";

	/**
	 * Bean name for ICfdpRequestResultMessage object.
	 */
	public static final String CFDP_REQUEST_RESULT_MESSAGE = "CFDP_REQUEST_RESULT_MESSAGE";

	/**
	 * Bean name for ICfdpPduReceivedMessage object.
	 */
	public static final String CFDP_PDU_RECEIVED_MESSAGE = "CFDP_PDU_RECEIVED_MESSAGE";

	/**
	 * Bean name for ICfdpPduSentMessage object.
	 */
	public static final String CFDP_PDU_SENT_MESSAGE = "CFDP_PDU_SENT_MESSAGE";

	/**
	 * Bean name for ICfdpMessageHeader object.
	 */
	public static final String CFDP_MESSAGE_HEADER = "CFDP_MESSAGE_HEADER";

}
