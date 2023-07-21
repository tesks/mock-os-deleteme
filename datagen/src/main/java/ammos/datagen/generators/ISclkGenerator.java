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
package ammos.datagen.generators;

/**
 * This interface is implemented by SCLK generators.
 * 
 *
 */
public interface ISclkGenerator extends ISeededGenerator {
    /**
     * Indicates whether all values produced by a SCLK generator have been used
     * at least once.
     * 
     * @return true if all values used, false if not
     */
    public boolean isExhausted();
}
