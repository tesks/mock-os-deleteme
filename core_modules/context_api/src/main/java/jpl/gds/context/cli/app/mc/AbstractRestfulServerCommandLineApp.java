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
package jpl.gds.context.cli.app.mc;

import org.apache.commons.cli.ParseException;

import jpl.gds.context.api.options.RestCommandOptions;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;

import static jpl.gds.context.api.options.RestCommandOptions.DEFAULT_REST_PORT;

/**
 * Class AbstractRestfulServerCommandLineApp
 */
public abstract class AbstractRestfulServerCommandLineApp extends AbstractCommandLineApp
        implements IRestFulServerCommandLineApp {

    /** the default port for the RESTful service */
    protected int                 restPort            = DEFAULT_REST_PORT;

    /** the default state of security for the RESTful service */
    /** Make HTTP the default instead of HTTPS */
    protected boolean           restIsSecure        = false;

    /** REST Options object; has to be created here as Global LAD does not call super.createOptions() */
    protected RestCommandOptions restOptions = new RestCommandOptions();

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     */
    public AbstractRestfulServerCommandLineApp() {
        this(true);
    }

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     * 
     * @param addHook
     *            whether or not to add the shutdown hook
     */
    public AbstractRestfulServerCommandLineApp(final boolean addHook) {
        super(addHook);
    }

    /**
     * The RESTful M&C --restPort option has different functionality for a Server vs. a Client applications.
     * In the Server, it sets the listening port, and has certain behavior associated with SpringBoot/SpringMVC.
     * In the Client, it sets the port on which the client will communication with the server. There are no
     * SpringBoot/SpringMVC behaviors to document.
     * 
     * This method should be overloaded to return the appropriate description to use for the --restPort option, allowing
     * the creation of the option with different descriptions appropriate for the individual use-case.
     * 
     * @return the correctly described --restPort option
     */
    protected String getRestPortOptionDescription() {
        return getTruncatedRestPortDescription() + "Specifying -1 disables the RESTful Service entirely.\n"
                + "NOTE: Not specifying this option defaults to -1 (Disabled)";
    }

    /**
     * Truncated part of the Rest Port description for applications that to now allow the REST interface to be disabled
     * 
     * @return Truncated description
     */
    protected String getTruncatedRestPortDescription() {
        return RestCommandOptions.REST_PORT_DESC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseCommandOptions createOptions() {

        return createOptions(new BaseCommandOptions(this, true));

    }
    
    @Override
    protected BaseCommandOptions createOptions(final BaseCommandOptions opts) {
        
        if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions(opts);

        restOptions.REST_PORT_OPTION.setDescription(getRestPortOptionDescription());

        options.addOptions(restOptions.getAllRestOptionsNoHost());
        return options;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        /* Set the RESTful port */
        restPort = restOptions.REST_PORT_OPTION.parseWithDefault(commandLine, false, true);
        /* Turn on HTTPS when restPort is present AND restInsecure is NOT */
        restIsSecure = commandLine.hasOption(restOptions.REST_PORT_OPTION.getLongOpt()) &&
                        !restOptions.REST_INSECURE_OPTION.parse(commandLine);
    }

    @Override
    public int getRestPort() {
        return restPort;
    }

    @Override
    public boolean isRestSecure() {
        return restIsSecure;
    }

    @Override
    public void setRestPort(final Integer restPort) {
        this.restPort = restPort;
    }
}
