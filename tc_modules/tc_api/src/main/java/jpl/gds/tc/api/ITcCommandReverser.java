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
package jpl.gds.tc.api;

import jpl.gds.tc.api.exception.CommandParseException;

/**
 * {@code ITcCommandReverser} is a bean to reverse translate one or more command bit sequences to their mnemonic texts.
 *
 * <p>To use this reverser, you must first set the {@code ITewUtility} by calling {@code #setTewUtility(jpl.gds.tc
 * .api.ITewUtility)}</p>
 *
 * <p>You can then call {@code #reverse(java.lang.String)} repeatedly to retrieve the reverse translated mnemonic
 * texts.</p>
 *
 * @since 8.2.0
 */
public interface ITcCommandReverser {

    /**
     * Reverse translate the provided command bytes (represented as hexadecimal string).
     *
     * @param commandBytesHex command bytes to reverse translate (must be a hexadecimal string)
     * @return mnemonic resulting from reverse translation
     * @throws CommandParseException thrown when any error is encountered while reverse translating
     */
    String reverse(String commandBytesHex) throws CommandParseException;

}