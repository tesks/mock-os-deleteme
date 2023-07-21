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

package jpl.gds.tc.mps.impl.session;

import gov.nasa.jpl.uplinkutils.CtsReturn;
import gov.nasa.jpl.uplinkutils.SWIGTYPE_p_unsigned_char;
import gov.nasa.jpl.uplinkutils.SWIGTYPE_p_void;
import gov.nasa.jpl.uplinkutils.UplinkUtils;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.exception.CommandParseException;
import jpl.gds.tc.mps.impl.ctt.CommandTranslationTable;

/**
 * Reverse translation MPS session.
 *
 * Provides command reversal from bytes or hex string.
 *
 * @since R8.2
 */
public class ReverseTranslationMpsSession {

    private final CtsReturn       ctsReturn;
    private final SWIGTYPE_p_void reverseTranslationSession;

    /**
     * Constructor
     *
     * @param ctt command translation table
     */
    public ReverseTranslationMpsSession(final CommandTranslationTable ctt) {
        ctsReturn = new CtsReturn();
        reverseTranslationSession = UplinkUtils
                .revxlt_session_begin_w(ctt.getReverseTranslationTablePointer(), ctsReturn);
    }

    /**
     * Reverse command bytes from a byte array
     *
     * @param commandBytes
     * @return reverse translation command mnemonic
     * @throws CommandParseException
     */
    public String reverseCommandBytes(final byte[] commandBytes) throws CommandParseException {
        final String commandBytesHex = BinOctHexUtility.toHexFromBytes(commandBytes);
        return reverseCommandBytesHex(commandBytesHex);
    }

    /**
     * Reverse command bytes from a hex string
     *
     * @param commandBytesHex
     * @return reverse translated command mnemonic
     * @throws CommandParseException
     */
    public String reverseCommandBytesHex(final String commandBytesHex) throws CommandParseException {
        if (!BinOctHexUtility.isValidHex(commandBytesHex)) {
            throw new IllegalArgumentException("Invalid hex string.");
        }

        final String commandHex = BinOctHexUtility.stripHexPrefix(commandBytesHex);
        final SWIGTYPE_p_unsigned_char commandBytes = UplinkUtils
                .xlt_hexstring_to_bin_w(commandHex);

        final String mnemonic = UplinkUtils.revxlt_getNextCommand_w(reverseTranslationSession, commandBytes,
                commandHex.length() / 2, ctsReturn);

        if (mnemonic == null) {
            throw new CommandParseException("Reverse translation failed with error code " + ctsReturn.getErrcode());
        }

        return mnemonic;
    }
}
