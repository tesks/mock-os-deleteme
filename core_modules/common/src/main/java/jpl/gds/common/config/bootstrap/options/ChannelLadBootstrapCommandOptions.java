/*
 * Copyright 2006-2017. California Institute of Technology.
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
package jpl.gds.common.config.bootstrap.options;

import jpl.gds.common.config.bootstrap.ChannelLadBootstrapConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.FlagOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;
import jpl.gds.shared.cli.options.numeric.CsvUnsignedIntOption;
import jpl.gds.shared.cli.options.numeric.CsvUnsignedIntOptionParser;
import jpl.gds.shared.types.UnsignedInteger;
import org.apache.commons.cli.ParseException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * Container object for ChannelLadBoostrap command options. This class creates command line option objects used for
 * parsing ChannelLad bootstrap options and automatically setting the parsed values into the
 * ChannelLadBootstrapConfiguration object.
 * 
 * @since R8
 *
 */
public class ChannelLadBootstrapCommandOptions implements ICommandLineOptionsGroup {

    /** Long opt for channellad bootstrap */
    public static final String BOOTSTRAP_CHANNEL_LAD_LONG     = "bootstrapLad";
    /** Long opt for channellad bootstrap session ids */
    public static final String BOOTSTRAP_CHANNEL_LAD_IDS_LONG = "bootstrapIds";
    /** Description for channel lad bootstrap */
    public static final String BOOTSTRAP_CHANNEL_LAD_DESC     = "Boot-strap data from Channel LAD when downlink service starts. Note: Alarm history is only retrieved from the most recent available session.";
    /** Description for channel lad bootstrap session ids */
    public static final String BOOTSTRAP_CHANNEL_LAD_IDS_DESC = "Specify session ids to populate the Channel LAD from. Note: Alarm history is only retrieved from the most recent available session.";

    /**
     * Bootstrap ChannelLAD flag option
     */
    public final FlagOption bootstrapChannelLad;

    /**
     * Bootstrap ChannelLAD sessions option
     */
    public final CsvUnsignedIntOption bootstrapChannelLadIds;

    private final ChannelLadBootstrapConfiguration config;

    /**
     * ChannelLadBoostrap Command options container
     * 
     * @param config
     *            ChannelLadBootstrapConfiguration
     */
    public ChannelLadBootstrapCommandOptions(final ChannelLadBootstrapConfiguration config) {
        this.config = config;

        bootstrapChannelLad = new FlagOption(null, BOOTSTRAP_CHANNEL_LAD_LONG,
                BOOTSTRAP_CHANNEL_LAD_DESC, false);
        bootstrapChannelLad.setParser(new BootstrapLadOptionParser());

        bootstrapChannelLadIds = new CsvUnsignedIntOption(null, BOOTSTRAP_CHANNEL_LAD_IDS_LONG, "sessionIds",
                BOOTSTRAP_CHANNEL_LAD_IDS_DESC,
        		true, true, false);
        		
        bootstrapChannelLadIds.setParser(new BootstrapLadIdsOptionParser());

    }

    /**
     * Option parser for the BootstrapChannelLad option.
     * 
     */
    protected class BootstrapLadOptionParser extends FlagOptionParser {

        @Override
        public Boolean parse(final ICommandLine commandLine, final ICommandLineOption<Boolean> opt)
                throws ParseException {
            final boolean present = super.parse(commandLine, opt);

            config.setLadBootstrapFlag(present);

            return present;
        }
    }

    /**
     * Option parser for the BootstrapChannelLadIds option. This will enable the bootstrapLad flag if bootstrap id's are
     * present
     * 
     */
    protected class BootstrapLadIdsOptionParser extends CsvUnsignedIntOptionParser {

        /**
         * Unsigned CSV Integer parser for 'bootstrapping' ID(s) from GLAD
         */
		public BootstrapLadIdsOptionParser() {
			super(true, true);
		}

		@Override
		public Collection<UnsignedInteger> parse(final ICommandLine commandLine,
				final ICommandLineOption<Collection<UnsignedInteger>> opt) throws ParseException {
			final Collection<UnsignedInteger> fetchIds = super.parse(commandLine, opt);
			
            // Keep current bootstrapLad flag when no ID(s) specified
            // and set bootstrapLad flag to true when ID(s) are present
            if (!fetchIds.isEmpty()) {
                config.setLadBootstrapFlag(true);

                final List<Long> longIds = new LinkedList<>();
                fetchIds.forEach(l -> longIds.add(Long.valueOf(l.longValue())));
                config.setLadBootstrapIds(longIds);
            }
			
					
			return fetchIds;
		}
    }

    /**
     * Gets all the ChannelLAD bootstrap configuration command line options
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getBootstrapCommandOptions() {
        final List<ICommandLineOption<?>> result = new LinkedList<>();

        result.add(bootstrapChannelLad);
        result.add(bootstrapChannelLadIds);

        return result;
    }

    /**
     * Parses all ChannelLAD bootstrap command line options
     * 
     * @param commandLine
     *            Command line to parse
     * @throws ParseException
     *             if an error occurs parsing ChannelLAD options
     */
    public void parseBootstrapOptions(final ICommandLine commandLine) throws ParseException {
        for (final ICommandLineOption<?> option : getBootstrapCommandOptions()) {
            option.parse(commandLine, false);
        }
    }

}
