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
package jpl.gds.tc.api.options;

import java.util.Arrays;
import java.util.List;

import jpl.gds.common.config.connection.options.ConnectionCommandOptions;
import jpl.gds.shared.cli.options.CsvStringOption;

/**
 * Class UplinkBitRateCommandOption
 *
 */
public class UplinkBitRateCommandOption extends CsvStringOption {
    private static final long  serialVersionUID  = 5550156224613857757L;

    /** Short command option for uplink rate **/
    public static final String UPLINK_RATE_SHORT = "u";

    /** Long command option for uplink rate **/
    public static final String UPLINK_RATE_LONG  = "uplinkRate";

    /**
     * @param validRates
     *            a list of valid values against which to verify
     * @param sort
     *            true if list should be sorted, false if not
     */
    public UplinkBitRateCommandOption(final List<String> validRates, final boolean sort) {
        super(UPLINK_RATE_SHORT, UPLINK_RATE_LONG, "uplinkRate(s)",
                "Specify an uplink rate (or a comma-separated list of rates) in bits per"
                        + " second that the request may be radiated with. Only applicable with "
                        + ConnectionCommandOptions.UPLINK_CONNECTION_LONG + "=COMMAND_SERVICE. Valid rates are: "
                        + Arrays.toString(validRates.toArray(new String[] {})) + ". Defaults to ANY if not specified.",
                true, true, false, validRates);
    }
}
