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
package jpl.gds.tc.api.cltu;

import jpl.gds.tc.api.IBchCodeblock;
import jpl.gds.tc.api.ITcTransferFrame;

import java.util.List;

/**
 * The interface for any object that's purpose is to collect all of the necessary data for an ICltu and only allow
 * the construction of one when all of hte appropriate information has been collected.
 *
 */
public interface ICltuBuilder {

    /**
     * Return a CLTU from the builder
     *
     * @return MPS CLTU
     */
    ICltu build();

    /**
     * Set the order ID
     *
     * @param orderId the order ID
     * @return this ICltuBuilder
     */
    ICltuBuilder setOrderId(final int orderId);

    /**
     * Set the acquisition sequence
     *
     * @param acquisitionSequence the acquisition sequence
     * @return this ICltuBuilder
     */
    ICltuBuilder setAcquisitionSequence(final byte[] acquisitionSequence);

    /**
     * Set the start sequence
     *
     * @param startSequence start sequence
     * @return this ICltuBuilder
     */
    ICltuBuilder setStartSequence(final byte[] startSequence);

    /**
     * Set the data
     *
     * @param data the data
     * @return this ICltuBuilder
     */
    ICltuBuilder setData(final byte[] data);

    /**
     * Set the tail sequence
     *
     * @param tailSequence the tail sequence
     * @return this ICltuBuilder
     */
    ICltuBuilder setTailSequence(final byte[] tailSequence);

    /**
     * Set the idle sequence
     *
     * @param idleSequence the idle sequence
     * @return this ICltuBuilder
     */
    ICltuBuilder setIdleSequence(final byte[] idleSequence);

    /**
     * Set the BCH code blocks
     *
     * @param codeblocks the BCH code blocks
     * @return this ICltuBuilder
     */
    ICltuBuilder setCodeblocks(final List<IBchCodeblock> codeblocks);


    /**
     * Set the CLTU frames
     * @param frames the frames that were parsed out from the CLTU being built
     * @return this ICltuBuilder
     */
    ICltuBuilder setFrames(final List<ITcTransferFrame> frames);
}
