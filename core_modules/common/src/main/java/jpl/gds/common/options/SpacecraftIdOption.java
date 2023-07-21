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
package jpl.gds.common.options;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * A command line option class for spacecraft ID. Entered value must be a
 * defined spacecraft ID for the current mission and will default the the
 * default ID for the mission.
 * 
 *
 */
@SuppressWarnings("serial")
public class SpacecraftIdOption extends UnsignedIntOption {
    
    /**
     * The short option name.
     */
    public static final String SHORT_OPTION = "S";
    /**
     * The long option name.
     */
    public static final String LONG_OPTION = "spacecraftID";
    /**
     * Description
     */
    public static final String DESCRIPTION = "spacecraft id; must be numeric";

    /**
     * Constructor.
     * @param shortOpt short option name (to override default in this class)
     * @param longOpt long option name (to override default in this class)
     * @param missionProps the current MissionProperties object to get default and valid values from
     * @param required true if the option is required, false if not
     * @param defValue the default value
     */    
    public SpacecraftIdOption(final String shortOpt, final String longOpt, final MissionProperties missionProps,
            final boolean required, final UnsignedInteger defValue) {
        super(shortOpt, longOpt, "scid", DESCRIPTION, required);
        setParser(new SpacecraftIdOptionParser(missionProps));
        
        if (defValue != null) {
            setDefaultValue(defValue);
        } else {
            final int def = missionProps.getDefaultScid();
            setDefaultValue(def > 0 ? UnsignedInteger.valueOf(def) : UnsignedInteger.MIN_VALUE);
        }
            
    }
    
    /**
     * Constructor.
     * 
     * @param missionProps the current MissionProperties object to get default and valid values from
     * @param required true if the option is required, false if not
     * @param defValue the default value
     */    
    public SpacecraftIdOption(final MissionProperties missionProps,
            final boolean required, final UnsignedInteger defValue) {
        this(SHORT_OPTION, LONG_OPTION, missionProps, required, defValue);           
    }

    /**
     * Constructor.
     * 
     * @param missionProps the current MissionProperties object to get default and valid values from
     * @param required true if the option is required, false if not
     */
    public SpacecraftIdOption(final MissionProperties missionProps, final boolean required) {
        this(missionProps, required, null);
    }


}
