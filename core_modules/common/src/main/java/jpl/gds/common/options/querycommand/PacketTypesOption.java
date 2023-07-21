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
package jpl.gds.common.options.querycommand;

import jpl.gds.shared.cli.options.CommandLineOption;

/**
 * A command line option class representing the "--packetTypes" Packet type selector
 * option.
 * 
 *
 * @since R8
 */
@SuppressWarnings("serial")
public class PacketTypesOption extends CommandLineOption<PacketTypeSelect> {
	
	/** Long option name for Packet types */
    public static final String PACKET_TYPES_LONG = "packetTypes";
    
    /**
     * Constructor.
     * 
     * @param required true if the option is required
     */
    public PacketTypesOption(final boolean required) {
        this(null, required);
    }
    
    /**
     * Constructor.
     * 
     * @param defValue default value (as string)
     * @param required true if the option is required
     */
    public PacketTypesOption(final String defValue, final boolean required) {
        super(null, PACKET_TYPES_LONG, true, "string",
                "Allowed types: " +
                    "s=SSE"                +
                    "f=FSW",
                    required,
                    new PacketTypesOptionParser());
        if (defValue != null) {
            setDefaultValue(new PacketTypeSelect(defValue));
        }
    }
}
