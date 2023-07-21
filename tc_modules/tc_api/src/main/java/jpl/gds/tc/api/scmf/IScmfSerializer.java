/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tc.api.scmf;

import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.message.IScmfCommandMessage;

/**
 * IScmfSerializer is the interface to be utilized by a class that intends to
 * create a byte array of data the represents an IScmf.
 *
 */
public interface IScmfSerializer {

    /** The byte length of the file name field */
    short  FILE_NAME_BYTE_LENGTH     = 8;
    /** The byte length of the preparer field */
    short PREPARER_BYTE_LENGTH = 60;

    /** The byte length of the file size field */
    short FILE_SIZE_BYTE_LENGTH = 4;
    /** The total byte length of the SCMF header */
    /** The byte length of the file header size field */
    short FILE_HEADER_SIZE_BYTE_LENGTH = 4;
    /** The byte length of the mission ID field */
    short MISSION_ID_BYTE_LENGTH = 2;
    /** The byte length of the spacecraft ID field */
    short SPACECRAFT_ID_BYTE_LENGTH = 2;
    /** The byte length of the reference number field */
    short REFERENCE_NUMBER_BYTE_LENGTH = 4;
    /** The byte length of the bit one radiation time field */
    short BIT_ONE_RADIATION_TIME_BYTE_LENGTH = 24;
    /** The byte length of the bit rate field */
    short BIT_RATE_INDEX_BYTE_LENGTH = 4;
    /** The byte length of the comment field */
    short COMMENT_FIELD_BYTE_LENGTH = 66;
    /** The byte length of the creation time field */
    short CREATION_TIME_BYTE_LENGTH = 24;
    /** The byte length of the title field */
    short TITLE_BYTE_LENGTH = 60;
    /** The byte length of the seqtran version field */
    short SEQTRAN_VERSION_BYTE_LENGTH = 60;
    /** The byte length of the macro version field */
    short MACRO_VERSION_BYTE_LENGTH = 60;

    /** The byte length of the file checksum field */
    short FILE_CHECKSUM_BYTE_LENGTH = 2;

    /** The size of the entire SCMF header in bytes */
    int    SCMF_HEADER_BYTE_LENGTH   = FILE_NAME_BYTE_LENGTH + PREPARER_BYTE_LENGTH + FILE_SIZE_BYTE_LENGTH
            + FILE_HEADER_SIZE_BYTE_LENGTH + MISSION_ID_BYTE_LENGTH + SPACECRAFT_ID_BYTE_LENGTH
            + REFERENCE_NUMBER_BYTE_LENGTH + BIT_ONE_RADIATION_TIME_BYTE_LENGTH + BIT_RATE_INDEX_BYTE_LENGTH
            + COMMENT_FIELD_BYTE_LENGTH + CREATION_TIME_BYTE_LENGTH + TITLE_BYTE_LENGTH + SEQTRAN_VERSION_BYTE_LENGTH
            + MACRO_VERSION_BYTE_LENGTH + FILE_CHECKSUM_BYTE_LENGTH;


    /** The byte length of the message number */
    short MESSAGE_NUMBER_BYTE_LENGTH = 4;
    /** The byte length of the message length field */
    short MESSAGE_BIT_LENGTH_BYTE_LENGTH = 4;
    /** The byte length of a time field */
    short TIME_BYTE_LENGTH = 24;
    /** The byte length for the message comment field */
    short MESSAGE_COMMENT_BYTE_LENGTH = 66;
    /** The byte length of the message checksum */
    short MESSAGE_CHECKSUM_BYTE_LENGTH = 2;
    /** The defined size of the entire command message header */
    int MESSAGE_HEADER_BYTE_LENGTH = MESSAGE_NUMBER_BYTE_LENGTH +
            MESSAGE_BIT_LENGTH_BYTE_LENGTH +
            (3 * TIME_BYTE_LENGTH) +
            MESSAGE_COMMENT_BYTE_LENGTH +
            MESSAGE_CHECKSUM_BYTE_LENGTH;

    /**
     * Get the byte serlialized version of an IScmf's header
     * @param scmf the IScmf to have its header serialized
     * @return the byte serialized version of the IScmf's header
     */
    byte[] getHeaderBytes(final IScmf scmf);

    /**
     * Get they byte serialized version of a single IScmfCommandMessage
     * @param msg the IScmfCommandMessage to be serialized
     * @return the byte serialized version of an IScmfCommandMessage
     */
    byte[] getCommandMessageBytes(final IScmfCommandMessage msg);
}
