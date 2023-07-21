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
package jpl.gds.product.automation.checkers;

import jpl.gds.shared.config.DynamicEnum;

import java.util.Map;

/**
 * REFERENCE PDPP MNEMONIC
 * See TelemetryInputType and DynamicEnum classes
 */
public class ReferencePdppMnemonic extends DynamicEnum<ReferencePdppMnemonic> {

    /**
     * LOGGER
     */
    public static final ReferencePdppMnemonic LOGGER = new ReferencePdppMnemonic("LOGGER", 0);

    /**
     * Constructor
     *
     * @param name    Name
     * @param ordinal Ordinal
     */
    public ReferencePdppMnemonic(final String name, final int ordinal) {

        //will register the value in the superclass map
        //MPCS-11542 Store keys in uppercase
        super(name.toUpperCase(), ordinal);

        //overwrite with actual telemetry input type object with properties
        elements.get(getClass()).put(name, this);
    }

    /**
     * Get object from string
     *
     * @param name Name of option
     * @return PdppChecker object
     */
    public static ReferencePdppMnemonic valueOf(String name) {
        Map<String, DynamicEnum<?>> map = elements.get(ReferencePdppMnemonic.class);
        if (map != null && map.containsKey(name)) {
            return (ReferencePdppMnemonic) map.get(name);
        }

        throw new IllegalArgumentException("No enum constant " + name);
    }

    /**
     * Explicit definition of values() is needed here to trigger static initializer.
     *
     * @return array of PdppChecker
     */
    public static ReferencePdppMnemonic[] values() {
        return values(ReferencePdppMnemonic.class);
    }

}