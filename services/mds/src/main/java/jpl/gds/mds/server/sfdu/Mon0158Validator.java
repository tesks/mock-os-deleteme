/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.mds.server.sfdu;

import jpl.gds.mds.server.config.MdsProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * MON-0158 Validator. Discovery and validation of SFDU label in byte array.
 * <p>
 * See 820-013 0158-Monitor rev J for MON-0158 specification,
 * <p>
 * See 820-013 0164-Comm for the Stadnard DSN Block (SDB) structure, including DSN Data Delivery (DDD) specifications.
 * <p>
 * See 820-013 0171-Telecomm-NJPL for SFDU structure
 */
public class Mon0158Validator implements ISfduValidator {

    protected static final String      DDP_ID_ASCII                  = "0093";
    protected static final int         MAX_BLOCK_LENGTH              = 4480;
    protected static final char        CLASS_ID                      = 'I';
    protected static final char        VERSION_ID                    = '2';
    protected static final int         CONTROL_AUTHORITY_BYTE_LENGTH = 4;
    protected static final int         DDP_ID_BYTE_LENGTH            = 4;
    protected static final int         BLOCK_LENGTH_BYTE_LENGTH      = 8;
    protected static final int         SFDU_HEADER_BYTE_LENGTHLENGTH = 20;
    protected static final int         IDX_CA_START                  = 0;
    protected static final int         IDX_CA_END                    = 4;
    protected static final int         IDX_VERSION_ID                = 4;
    protected static final int         IDX_CLASS_ID                  = 5;
    protected static final int         IDX_DDP_ID_START              = 8;
    protected static final int         IDX_DDP_ID_END                = 12;
    protected static final int         IDX_BLOCK_LENGTH_START        = 12;
    protected static final int         IDX_BLOCK_LENGTH_END          = 20;
    private static final   Tracer      LOG                           = TraceManager.getTracer(Loggers.MDS);
    // default control authority
    private                Set<String> controlAuthorities            = new HashSet<>(Collections.singletonList("NJPL"));

    /**
     * Default constructor
     */
    public Mon0158Validator() {
    }

    /**
     * Constructor with MDS Properties
     *
     * @param mdsProperties
     */
    public Mon0158Validator(final MdsProperties mdsProperties) {
        this.controlAuthorities = mdsProperties.getControlAuthorities();
    }

    @Override
    public boolean validate(final byte[] data) {
        final int found = findSfduLabel(data);

        // SFDU header is 20 bytes in length
        if (found < 0 || found + SFDU_HEADER_BYTE_LENGTHLENGTH > data.length) {
            LOG.debug("Invalid message, no SFDU label found");
            return false;
        }

        return validateSfduLabel(Arrays.copyOfRange(data, found, found + SFDU_HEADER_BYTE_LENGTHLENGTH));
    }

    /**
     * Get valid control authorities
     *
     * @return
     */
    public Set<String> getControlAuthorities() {
        return new HashSet<>(controlAuthorities);
    }

    /**
     * Set valid control authorities
     *
     * @param controlAuthorities
     */
    public void setControlAuthorities(final Set<String> controlAuthorities) {
        this.controlAuthorities = controlAuthorities;
    }

    /**
     * Find the SFDU label, return the byte index.
     *
     * @param data
     * @return The byte index, or -1 if not found
     */
    int findSfduLabel(final byte[] data) {
        // loop through bytes in data up to (length - 3), to account for size of control authority field (4 bytes)
        for (int i = 0; i < data.length - 3; i++) {
            for (String controlAuthorityStr : controlAuthorities) {
                final byte[] controlAuthorityBytes = controlAuthorityStr.getBytes(StandardCharsets.US_ASCII);
                if (data[i] == controlAuthorityBytes[0]) {
                    final byte[] match = Arrays.copyOfRange(data, i, i + 4);
                    if (Arrays.equals(match, controlAuthorityBytes)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Validate the SFDU label
     *
     * @param sfdu
     * @return
     */
    boolean validateSfduLabel(final byte[] sfdu) {
        // control authority id is bytes (0,4]
        final byte[]  controlAuthorityId = Arrays.copyOfRange(sfdu, IDX_CA_START, IDX_CA_END);
        final boolean caValid            = validateControlAuthorityId(controlAuthorityId);

        // version id is byte 4
        final byte    versionId    = sfdu[IDX_VERSION_ID];
        final boolean versionValid = validateVersionId(versionId);

        // class id is byte 5
        final byte    classId      = sfdu[IDX_CLASS_ID];
        final boolean classIdValid = validateClassId(classId);

        // ddp id is bytes (8,12]
        final byte[]  ddpId      = Arrays.copyOfRange(sfdu, IDX_DDP_ID_START, IDX_DDP_ID_END);
        final boolean ddpIdValid = validateDdpId(ddpId);

        // block length is bytes 13-20
        final byte[]  blockLength      = Arrays.copyOfRange(sfdu, IDX_BLOCK_LENGTH_START, IDX_BLOCK_LENGTH_END);
        final boolean blockLengthValid = validateBlockLength(blockLength);

        final boolean valid = caValid && versionValid && classIdValid && ddpIdValid && blockLengthValid;

        if (!valid) {
            LOG.debug("Invalid SFDU label: [ca: ", caValid, ", versionId: ", versionValid, ", classId: ",
                    classIdValid, ", ddpId: ", ddpIdValid, ", blockLength: ", blockLengthValid, "]");
        }

        return valid;

    }

    /**
     * Validate control authority ID field, must == "NJPL" (ASCII), check against configured CAs
     *
     * @param controlAuthorityId
     * @return
     */
    boolean validateControlAuthorityId(final byte[] controlAuthorityId) {
        if (controlAuthorityId.length > CONTROL_AUTHORITY_BYTE_LENGTH) {
            return false;
        }
        final String controlAuthorityIdAscii = new String(controlAuthorityId, StandardCharsets.US_ASCII);
        return controlAuthorities.contains(controlAuthorityIdAscii);
    }

    /**
     * Validate version ID field, must be == "2" (ASCII)
     *
     * @param versionId
     * @return
     */
    boolean validateVersionId(final byte versionId) {
        final char versionIdChar = (char) versionId;
        return versionIdChar == VERSION_ID;
    }

    /**
     * Validate class ID field, must == 'I' (ASCII)
     *
     * @param classId
     * @return
     */
    boolean validateClassId(final byte classId) {
        final char classIdAscii = (char) classId;
        return classIdAscii == CLASS_ID;
    }

    /**
     * Validate DDP ID field, must == "0093" (ASCII)
     *
     * @param ddpId
     * @return
     */
    boolean validateDdpId(final byte[] ddpId) {
        if (ddpId.length > DDP_ID_BYTE_LENGTH) {
            return false;
        }
        final String ddpIdAscii = new String(ddpId, StandardCharsets.US_ASCII);
        return ddpIdAscii.equals(DDP_ID_ASCII);
    }

    /**
     * Validate block length field, must be <= 4480
     *
     * @param blockLength
     * @return
     */
    boolean validateBlockLength(final byte[] blockLength) {
        if (blockLength.length > BLOCK_LENGTH_BYTE_LENGTH) {
            return false;
        }
        final long blockLengthLong = ByteBuffer.wrap(blockLength).getLong();
        return blockLengthLong <= MAX_BLOCK_LENGTH;
    }
}
