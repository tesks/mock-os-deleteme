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
package jpl.gds.shared.time;

import jpl.gds.serialization.primitives.time.Proto3Sclk;
import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * Interface Isclk
 */
@CustomerAccessible(immutable = true)
public interface ISclk extends ICoarseFineTime {

    /**
     * Formats a SCLK as a ticks string, i.e., coarse-fine where fine is
     * sub-ticks.
     * 
     * @return the formatted string
     */
    String toTicksString();

    /**
     * Formats a SCLK as a decimal string, i.e., coarse.fine where fine is
     * fractional seconds, not ticks.
     * 
     * @return the formatted string
     */
    String toDecimalString();
    
    /**
     * Convert to a protobuf message
     * 
     * @return this ISclk as a protobuf message
     */
    public Proto3Sclk buildSclk();
}