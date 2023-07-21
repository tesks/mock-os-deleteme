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
 * A command line option class representing the "--channelTypes" option.
 * 
 *
 * @since R8
 */
@SuppressWarnings("serial")
public class ChannelTypesOption extends CommandLineOption<ChannelTypeSelect> {
	
	/** Long option name for Channel types */
    public static final String CHANNEL_TYPES_LONG = "channelTypes";

    /** All channel types constant */
    public static final String ALL_CHANNEL_TYPES = "frhmsg";
    
    /**
     * Constructor.
     * 
     * @param required true if the option is required
     */
    public ChannelTypesOption(final boolean required) {
        this(null, required);
    }
    
    /**
     * Constructor.
     * 
     * @param defValue default value (as string)
     * @param required true if the option is required
     */
    public ChannelTypesOption(final String defValue, final boolean required) {
        super(null, CHANNEL_TYPES_LONG, true, "string",
                "Allowed types: "	+
                    "f=FSW-realtime"	+
                	"r=FSW-recorded"	+
                    "h=FSW-header"	+
                	"m=Monitor"	+
                    "s=SSE"		+
                	"g=SSE-header",
                    required,
                    new ChannelTypesOptionParser());
        if (defValue != null) {
            setDefaultValue(new ChannelTypeSelect(defValue));
        }
    }
}

