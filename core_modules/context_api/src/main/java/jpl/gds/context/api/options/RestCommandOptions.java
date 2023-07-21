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

package jpl.gds.context.api.options;

import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.context.cli.app.mc.IRestFulServerCommandLineApp;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.numeric.IntegerOption;
import jpl.gds.shared.cli.options.numeric.IntegerOptionParser;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.util.HostPortUtility;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.ParseException;

/**
 *
 * The RestCommandOptions are used for parsing REST command line  options (e.g. restPort, restHost, restInsecure)
 *
 */
public class RestCommandOptions implements ICommandLineOptionsGroup {
    /** The Default Value for the RESTful interface's host */
    public static final String DEFAULT_REST_HOST = "localhost";

    /** The Default Value for the RESTful interface's port */
    public static final int DEFAULT_REST_PORT = IRestFulServerCommandLineApp.SPECIFIED_DISABLE_REST_PORT;

    /** This value will cause the RESTful interface to be allocated from a random, unused port */
    public static final int SPECIFIED_RANDOM_REST_PORT  = 0;


    /** REST port description */
    public static final String REST_PORT_DESC = "The port on which the RESTful M&C Service listens. "
              + "Specifying 0 will scan for and select a random available port and "
              + "notify what port was selected in log output. ";

    /** Long option name for rest port */
    public static final String  REST_PORT_LONG      = "restPort";

    /** Long option name for rest insecure */
    public static final String  REST_INSECURE_LONG  = "restInsecure";

    /** Long option name for rest host */
    public static final String REST_HOST_LONG  = "restHost";

    private static final String REST_INSECURE_DESC = "Supplying this option turns off HTTPS "
            + "(SSL/TLS) encryption for the "
            + "RESTful M&C Service.\n"
            + "NOTE: The Client and "
            + "Server must be in agreement on this.";

    static final String REST_HOST_DESC = "The host on which the RESTful M&C Service resides";

    /** The option for setting the port on which a RESTful server will listen */
    public final IntegerOption REST_PORT_OPTION;

    /** The option for setting the port on which an optional RESTful server will listen */
    public final FlagOption REST_INSECURE_OPTION;

    /** The option for setting the port on which an optional RESTful server will listen */
    public StringOption REST_HOST_OPTION;

    private ISimpleContextConfiguration simpleContext;


    /**
     * RestCommandOptions constructor
     */
    public RestCommandOptions(){
        REST_PORT_OPTION = new IntegerOption(null, REST_PORT_LONG,
                                             REST_PORT_LONG,
                                             REST_PORT_DESC, false);
        REST_PORT_OPTION.setParser(new RestPortOptionParser());

        REST_INSECURE_OPTION = new FlagOption(null, REST_INSECURE_LONG, REST_INSECURE_DESC,
                                              false);

        REST_HOST_OPTION = new StringOption(null,
                                            REST_HOST_LONG,
                                            REST_HOST_LONG,
                                            REST_HOST_DESC,
                                            false);
        REST_HOST_OPTION.setDefaultValue(DEFAULT_REST_HOST);
    }

    /**
     * RestCommandOptions constructor that takes a SimpleContextConfiguration
     */
    public RestCommandOptions(final ISimpleContextConfiguration simpleContext) {
        this();
        this.simpleContext = simpleContext;
    }

    /**
     * Gets all the REST  command line options
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllRestOptionsNoHost() {
        final List<ICommandLineOption<?>> result = new LinkedList<>();

        result.add(REST_PORT_OPTION);
        result.add(REST_INSECURE_OPTION);

        return result;
    }

    /**
     * Gets all the REST  command line options
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllRestOptions() {
        final Collection<ICommandLineOption<?>> result = getAllRestOptionsNoHost();
        result.add(REST_HOST_OPTION);
        return result;
    }


    /**
     * An options parser for the REST port command option.
     * Generates random port id parsed port is zero
     */
    public class RestPortOptionParser extends IntegerOptionParser {
        /**
         * Constructor.
         */
        public RestPortOptionParser() {
            super();
            setDefaultValue(DEFAULT_REST_PORT);
        }

        @Override
        public Integer parse(final ICommandLine commandLine, final ICommandLineOption<Integer> opt,
                             final boolean required) throws ParseException {
            int restPort = super.parse(commandLine, opt, required);
            //generate random port
            if(restPort == SPECIFIED_RANDOM_REST_PORT){
                restPort = HostPortUtility.getRandomRestPort();
            }
            if(simpleContext != null){
                simpleContext.setRestPort(UnsignedInteger.valueOf(restPort));
            }
            return restPort;
        }

        @Override
        public Integer parseWithDefault(final ICommandLine commandLine, final ICommandLineOption<Integer> opt,
                                        final boolean required, final boolean doSetDefault) throws ParseException {
            int restPort = super.parseWithDefault(commandLine, opt, required, doSetDefault);
            //generate random port
            if(restPort == SPECIFIED_RANDOM_REST_PORT){
                restPort = HostPortUtility.getRandomRestPort();
            }
            if(simpleContext != null){
                simpleContext.setRestPort(UnsignedInteger.valueOf(restPort));
            }
            return restPort;
        }
    }
}
