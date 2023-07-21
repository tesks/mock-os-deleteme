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
package jpl.gds.tc.api.cltu;

import jpl.gds.tc.api.IBchCodeblock;
import jpl.gds.tc.api.exception.CltuEndecException;

import java.util.List;

/**
 * {@code ITcCltuBuilder} is a CLTU byte array builder, which takes custom parameters for CLTUs and builds CLTU bytes
 * using the MPSA CTS library.
 * <p>
 * To use this builder, first get an instance of this class by grabbing the bean via Spring. Then, set the data for the
 * CLTU and any other parameters to set. (If those parameters are not manually set, default values will be used.) After
 * setting all the parameters, call {@link #build()} to retrieve a CLTU byte array.
 *
 * @since 8.2.0
 */
public interface ITcCltuBuilder {

    /**
     * Build a CLTU using the parameters set in this builder (or defaults for those parameters not set) and return the
     * bytes representing the CLTU. At a minimum, the BCH codeblocks must be set.
    *
     * @return CLTU bytes from CTS/MPSA
     * @throws CltuEndecException thrown when MPSA UplinkUtils library reports an error
     */
    byte[] build() throws CltuEndecException;

    /**
     * Set the CLTU start sequence
     *
     * @param startSequence byte array
     * @return builder
     */
    ITcCltuBuilder setStartSequence(byte[] startSequence);

    /**
     * Set the CLTU start sequence
     *
     * @param startSequence hex string
     * @return builder
     */
    ITcCltuBuilder setStartSequence(String startSequence);

    /**
     * Set the CLTU tail sequence
     *
     * @param tailSequence byte array
     * @return builder
     */
    ITcCltuBuilder setTailSequence(byte[] tailSequence);

    /**
     * Set the CLTU tail sequence
     *
     * @param tailSequence hex string
     * @return builder
     */
    ITcCltuBuilder setTailSequence(String tailSequence);

    /**
     * Set the CLTU acquisition sequence
     *
     * @param acquisitionSequence byte array
     * @return builder
     */
    ITcCltuBuilder setAcquisitionSequence(byte[] acquisitionSequence);

    /**
     * Set the CLTU acquisition sequence
     *
     * @param acquisitionSequence hex string
     * @return builder
     */
    ITcCltuBuilder setAcquisitionSequence(String acquisitionSequence);

    /**
     * Set the BCH code blocks
     *
     * @param codeblocks
     * @return builder
     */
    ITcCltuBuilder setCodeblocks(List<IBchCodeblock> codeblocks);

    /**
     * Set TC frames serialized to bytes
     *
     * @param frameBytes byte array
     * @return builder
     */
    ITcCltuBuilder setFrameBytes(final byte[] frameBytes);
}