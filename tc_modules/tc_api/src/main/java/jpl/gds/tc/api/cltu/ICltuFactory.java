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
package jpl.gds.tc.api.cltu;

import java.util.List;

import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.exception.CltuEndecException;

/**
 * Interface for the CLTU Builder
 * 
 *
 */
public interface ICltuFactory {
    /**
     * Create a list of CLTU's from frames
     *
     * @param frames
     *            List<ITcTransferFrame>
     * @return List<ICltu>
     */
    public List<ICltu> createCltusFromFrames(final List<ITcTransferFrame> frames) throws CltuEndecException;

    /**
     * Create a list of PLOP CLTU's from frames
     *
     * @param frames
     *            List<ITcTransferFrame>
     * @return List<ICltu>
     */
    public List<ICltu> createPlopCltusFromFrames(final List<ITcTransferFrame> frames) throws CltuEndecException;
    
    /**
     * Create a single CLTU from byte data 
     * 
     * @param data raw data to extract as CLTU 
     * @return ICltu
     * @throws CltuEndecException if the data cannot be parsed as a CLTU
     */
    public ICltu parseCltuFromBytes(final byte[] data) throws CltuEndecException;

    /**
     * Create a list of CLTU's from byte data
     * 
     * @param data
     *            raw data to extract as CLTU
     * @return List<ICltu>
     * @throws CltuEndecException
     *             if the data cannot be parsed as a CLTU
     */
    public List<ICltu> parseCltusFromBytes(final byte[] data) throws CltuEndecException;

}
