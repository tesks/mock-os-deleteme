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
package jpl.gds.tc.api.plop;

import java.util.List;

import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.config.PlopProperties;

/**
 * An interface for the builder for command file loads.
 * 
 *
 * @since R8
 * MPCS-9390 - 1/24/18 - Added interface
 */
public interface ICommandLoadBuilder {

    /**
     * Clear out the internal list of CLTUs.
     */
    void clear();

    /**
     * Add all of the input CLTUs to this command load
     * 
     * @param cltus The CLTUs to be added to the command load
     */
    void addCltus(List<ICltu> cltus);

    /**
     * Using the internal list of CLTUs as a starting point, return a new list of CLTUs that
     * has all of the acquisition and idle sequences properly attached according to the user's preferences.
     * 
     * It's strange that this method returns a list of CLTUs, but that's because the acquisition/idle sequence
     * stuff has just been implemented as part of the CLTU class.  It is assumed that the CLTUs input to the command
     * load builder do not have any acquisition or idle sequences.  It is the job of this method to add those acquisition
     * and idle sequences as needed.
     * 
     * @param plopConfig the current Plop Configuration
     * 
     * @return A list of CLTUs, identical to those originally input, but with acquisition and idle sequences added according
     * to configuration and user input values.
     */
    List<ICltu> getPlopCltus(PlopProperties plopConfig);

}