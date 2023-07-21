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
package jpl.gds.tc.api;

/**
 * The TcApiBeans is a class that holds the names of the TcSpringBootstrap bean names
 *
 * 06/30/19 - MPCS-10745 - Added LEGACY_COMMAND_TRANSLATOR and MPS_COMMAND_TRANSLATOR
 * 04/09/19 - MPCS-10813 - Added UPLINK_RESPONSE_FACTORY and COMMAND_MESSAGE_UTILITY
 */
public class TcApiBeans {
    public static final String INTEGRATED_COMMAND_PROPERTIES   = "INTEGRATED_COMMAND_PROPERTIES";
    public static final String COMMAND_PROPERTIES              = "COMMAND_PROPERTIES";
    public static final String CLTU_PROPERTIES                 = "CLTU_PROPERTIES";
    public static final String FRAME_PROPERTIES                = "FRAME_PROPERTIES";
    public static final String PLOP_PROPERTIES                 = "PLOP_PROPERTIES";
    public static final String SCMF_PROPERTIES                 = "SCMF_PROPERTIES";
    public static final String CPD_CLIENT                      = "CPD_CLIENT";
    public static final String CPD_STATUS_POLLER               = "CPD_STATUS_POLLER";
    public static final String OUTPUT_ADAPTER_FACTORY          = "OUTPUT_ADAPTER_FACTORY";
    public static final String SCMF_FACTORY                    = "SCMF_FACTORY";
    public static final String SCMF_SFDU_HEADER_SERIALIZER     = "SCMF_SFDU_HEADER_SERIALIZER";
    public static final String COMMAND_MESSAGE_FACTORY         = "COMMAND_MESSAGE_FACTORY";
    public static final String COMMAND_OBJECT_FACTORY          = "COMMAND_OBJECT_FACTORY";
    public static final String UPLINK_RESPONSE_FACTORY         = "UPLINK_RESPONSE_FACTORY";
    public static final String SSE_COMMAND_SOCKET              = "SSE_COMMAND_SOCKET";
    public static final String COMMAND_WRITE_UTILITY           = "COMMAND_WRITE_UTILITY";
    public static final String COMMAND_MESSAGE_UTILITY         = "COMMAND_MESSAGE_UTILITY";
    public static final String CPD_OBJECT_FACTORY              = "CPD_OBJECT_FACTORY";
    public static final String COMMAND_ECHO_INPUT_FACTORY      = "COMMAND_ECHO_INPUT_FACTORY";
    public static final String COMMAND_ECHO_DECOM_SERVICE      = "COMMAND_ECHO_DECOM_SERVICE";
    public static final String CLTU_BUILDER_FACTORY            = "CLTU_BUILDER_FACTORY";
    public static final String TELECOMMAND_FRAME_FACTORY       = "TELECOMMAND_FRAME_FACTORY";
    public static final String MPS_COMMAND_FRAME_SERIALIZER    = "MPS_COMMAND_FRAME_SERIALIZER";
    public static final String LEGACY_COMMAND_FRAME_SERIALIZER = "LEGACY_COMMAND_FRAME_SERIALIZER";
    public static final String MPS_COMMAND_FRAME_PARSER        = "MPS_COMMAND_FRAME_PARSER";
    public static final String MPS_CLTU_PARSER                 = "MPS_CLTU_PARSER";
    public static final String TELECOMMAND_PACKET_FACTORY      = "TELECOMMAND_PACKET_FACTORY";
    public static final String COMMAND_LOAD_BUILDER            = "COMMAND_LOAD_BUILDER";
    public static final String SESSION_BUILDER                 = "SESSION_BUILDER";
    public static final String LEGACY_TEW_UTILITY              = "LEGACY_TEW_UTILITY";
    public static final String MPS_TEW_UTILITY                 = "MPS_TEW_UTILITY";
    public static final String TC_TRANSFER_FRAMES_BUILDER      = "TC_TRANSFER_FRAMES_BUILDER";
    public static final String TC_TRANSFER_FRAME_BUILDER       = "TC_TRANSFER_FRAME_BUILDER";
    public static final String MPS_TC_TRANSFER_FRAME_BUILDER   = "MPS_TC_TRANSFER_FRAMES_BUILDER";
    public static final String TC_SCMF_WRITER                  = "TC_SCMF_WRITER";
    public static final String TC_COMMAND_REVERSER             = "TC_COMMAND_REVERSER";
    public static final String LEGACY_SCMF_BUILDER             = "LEGACY_SCMF_BUILDER";
    public static final String MPS_SCMF_BUILDER                = "MPS_SCMF_BUILDER";
    public static final String SCMF_INTERNAL_MESSAGE_FACTORY   = "SCMF_INTERNAL_MESSAGE_FACTORY";
    public static final String LEGACY_COMMAND_TRANSLATOR       = "LEGACY_COMMAND_TRANSLATOR";
    public static final String LEGACY_CLTU_FACTORY = "LEGACY_CLTU_FACTORY";
    public static final String CLTU_BUILDER        = "MPS_CLTU_BUILDER";
}