/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.clt;

import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;

import jpl.gds.cfdp.clt.ampcs.properties.CfdpCltAmpcsProperties;
import jpl.gds.cfdp.common.config.CfdpCommonProperties;
import jpl.gds.cfdp.common.spring.bootstrap.CfdpCommonSpringBootstrap;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.security.options.AccessControlCommandOptions;
import jpl.gds.common.config.types.LoginEnum;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.security.cam.AccessControl;
import jpl.gds.security.cam.AccessControlException;
import jpl.gds.security.spring.bootstrap.SecuritySpringBootstrap;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.numeric.DiscreteUnsignedIntOption;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.bootstrap.SharedSpringBootstrap;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.types.UnsignedInteger;

public abstract class ACfdpClt extends AbstractCommandLineApp implements Runnable {

    protected static final Tracer trace = TraceManager.getDefaultTracer();

    protected static final String CFDP_PROCESSOR_HOST_SHORT = "ch";
    protected static final String CFDP_PROCESSOR_HOST_LONG = "restHost";
    protected static final String CFDP_PROCESSOR_HOST_LONG_ALIAS = "cfdpProcessorHost";
    protected static final String CFDP_PROCESSOR_PORT_SHORT = "cp";
    protected static final String CFDP_PROCESSOR_PORT_LONG = "restPort";
    protected static final String CFDP_PROCESSOR_PORT_LONG_ALIAS = "cfdpProcessorPort";

    // MPCS-11675 - Shakeh Brys - Add --restInsecure option
    protected static final String REST_INSECURE_LONG  = "restInsecure";
    private static final String REST_INSECURE_DESC = "Supplying this option turns off HTTPS "
        + "(SSL/TLS) encryption for the "
        + "RESTful M&C Service.\n"
        + "NOTE: The Client and "
        + "Server must be in agreement on this.";

    public static final String CLASS_SHORT = "c";
    protected static final String CLASS_LONG = "class";
    protected static final String CLASS_DESCRIPTION = "CFDP service class (1=Unreliable Transfer, 2=Reliable Transfer)";

    private final StringOption cfdpProcessorHostOption = new StringOption(null,
            CFDP_PROCESSOR_HOST_LONG, "hostname", "CFDP Processor hostname", false);

    private final UnsignedIntOption cfdpProcessorPortOption = new UnsignedIntOption(null,
            CFDP_PROCESSOR_PORT_LONG, "port", "CFDP Processor port number", false,
            UnsignedInteger.valueOfIntegerAsUnsigned(0), UnsignedInteger.valueOfIntegerAsUnsigned(65535));

    private final FlagOption insecureOption = new FlagOption(null, REST_INSECURE_LONG,
            REST_INSECURE_DESC, false);

    protected final DiscreteUnsignedIntOption classOption;
    public UnsignedInteger serviceClassUnsignedInteger;

    protected String cfdpProcessorHost;
    protected int cfdpProcessorPort;
    protected String cfdpProcessorUriRoot;
    protected String cfdpProcessorHttpType;
    protected boolean restIsSecure = false;

    protected final CfdpCommonProperties cfdpCommonProperties;
    
    private static AccessControl ACCESS_CONTROL = null;
    protected final AccessControlCommandOptions accessOptions;
	private final ApplicationContext springContext;
    private final SecurityProperties securityProps;
    protected HttpHeaders headers = null;
    
    protected ACfdpClt() {
        super();
        
        this.springContext = SpringContextFactory.getSpringContext(new String[] {
        		CommonSpringBootstrap.class.getPackage().getName(), 
        		SecuritySpringBootstrap.class.getPackage().getName(),
        		SharedSpringBootstrap.class.getPackage().getName(),
        		CfdpCommonSpringBootstrap.class.getPackage().getName()
        		});
        
        cfdpCommonProperties = springContext.getBean(CfdpCommonProperties.class);
        
        classOption = new DiscreteUnsignedIntOption(CLASS_SHORT, CLASS_LONG, "number",
                CLASS_DESCRIPTION, false,
                springContext.getBean(CfdpCommonProperties.class).getServiceClasses());
        
        final CfdpCltAmpcsProperties cfdpCltProperties = new CfdpCltAmpcsProperties();
        cfdpProcessorHost = cfdpCltProperties.getCfdpProcessorHostDefault();
        cfdpProcessorPort = cfdpCltProperties.getCfdpProcessorPortDefault();
        cfdpProcessorUriRoot = cfdpCltProperties.getCfdpProcessorUriRoot()
                + (cfdpCltProperties.getCfdpProcessorUriRoot().endsWith("/") ? "" : "/");
        cfdpProcessorHttpType = cfdpCltProperties.getCfdpProcessorHttpType();
        
        this.securityProps = springContext.getBean(SecurityProperties.class);
        accessOptions = new AccessControlCommandOptions(securityProps,
                                                        springContext.getBean(AccessControlParameters.class));
		
    }

    /**
     * Try to find mapping for the provided mnemonic or entity ID string and return the mapped entity ID number. If no
     * mapping exists, then return the numeric entity ID. If argument is not mapped nor a number, exit with code 1.
     *
     * @param mnemonicOrEntityId mnemonic or entity ID string
     * @return valid entity ID
     */
    protected long translatePossibleMnemonic(final String mnemonicOrEntityId) {
    	Long retVal = null;
    	try {
    		retVal = cfdpCommonProperties.translatePossibleMnemonic(mnemonicOrEntityId);
    	} catch (final NumberFormatException nfe) {
    		System.err.println(mnemonicOrEntityId + " is not a numerical entity ID, nor is it a mapped mnemonic");
    		System.exit(1);
    	}

    	return retVal;
    }

    /*
     * (non-Javadoc)
     *
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#createOptions()
     */
    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }
        super.createOptions();

        // MPCS-11675 - Shakeh Brys - 04/01/2020 - Add cfdp old port/host as alias for backwards compatibility
        cfdpProcessorHostOption.addAlias(CFDP_PROCESSOR_HOST_LONG_ALIAS);
        cfdpProcessorHostOption.addAlias(CFDP_PROCESSOR_HOST_SHORT);
        cfdpProcessorPortOption.addAlias(CFDP_PROCESSOR_PORT_LONG_ALIAS);
        cfdpProcessorPortOption.addAlias(CFDP_PROCESSOR_PORT_SHORT);

        options.addOption(cfdpProcessorHostOption);
        options.addOption(cfdpProcessorPortOption);
        options.addOption(insecureOption);
        options.addOption(this.accessOptions.KEYTAB_FILE);
        options.addOption(this.accessOptions.LOGIN_METHOD_NON_GUI);
        options.addOption(this.accessOptions.USER_ID);
        
        return options;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.
     * cmdline.ICommandLine)
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        final String cfdpProcessorHostCLOption = cfdpProcessorHostOption.parse(commandLine);

        if (cfdpProcessorHostCLOption != null) {
            cfdpProcessorHost = cfdpProcessorHostCLOption;
        }

        final UnsignedInteger cfdpProcessorPortCLOption = cfdpProcessorPortOption.parse(commandLine);

        if (cfdpProcessorPortCLOption != null) {
            cfdpProcessorPort = cfdpProcessorPortCLOption.intValue();
        }

        // MPCS-11675 - Shakeh Brys - 04/01/2020 - parse secure option and set correct http type
        restIsSecure = commandLine.hasOption(cfdpProcessorPortOption.getLongOpt()) &&
            !insecureOption.parse(commandLine);
        if (restIsSecure) {
            cfdpProcessorHttpType = "https";
        }

        serviceClassUnsignedInteger = classOption.parse(commandLine);
        if (serviceClassUnsignedInteger != null && !(serviceClassUnsignedInteger.intValue() == 1 || serviceClassUnsignedInteger.intValue() == 2)) {
            throw new ParseException("Provide valid class. Options available: " + CLASS_DESCRIPTION);
        }

        if (commandLine.getOptionValue(this.accessOptions.LOGIN_METHOD_NON_GUI.getLongOrShort()) == null) {
            // No authentication requested, continue without setting cookie.
            trace.trace("No loginMethod specified on command line. Skipping authentication");
            return;
        }

        this.accessOptions.LOGIN_METHOD_NON_GUI.parseWithDefault(commandLine, true,true);
        this.accessOptions.KEYTAB_FILE.parseWithDefault(commandLine, false, true);
        this.accessOptions.USER_ID.parse(commandLine);
 
        final AccessControlParameters acParams = this.accessOptions.getAccessControlParameters();

        if ((acParams.getLoginMethod() == LoginEnum.KEYTAB_FILE) && acParams.getKeytabFile().isEmpty())
        {
            throw new ParseException("No keytab file provided");
        }

        if ((acParams.getLoginMethod() == LoginEnum.KEYTAB_FILE) && acParams.getUserId() == "") {
            throw new ParseException("No username specified");
        }
        verifySecurityAccess();
        final String cookie = ACCESS_CONTROL.getSsoCookie().toString();
        headers = new HttpHeaders();
        headers.add("Cookie", cookie);
    }

    protected String getAbsoluteUri(final String relativeUri) {
        return cfdpProcessorHttpType + "://" + cfdpProcessorHost + ":" + cfdpProcessorPort
                + cfdpProcessorUriRoot + relativeUri;
    }

    @Override
    protected void showHelp(final String cltCommandStr) {

        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final OptionSet options = createOptions().getOptions();
        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + " " + cltCommandStr
                + " [options]");
        pw.println();
        options.printOptions(pw);
        printTemplateStylesAndDirectories(pw);
        pw.flush();
    }
	/**
	 * Security check.
	 */
	public void verifySecurityAccess() {

		try {
			final AccessControlParameters acParams = this.accessOptions.getAccessControlParameters();

            final String user = (acParams.getUserId() == null || acParams.getUserId() == "")
                    ? GdsSystemProperties.getSystemUserName()
                    : acParams.getUserId();

			ACCESS_CONTROL = AccessControl.createAccessControl(
					securityProps,
					user,
					securityProps.getDefaultRole(),
					acParams.getLoginMethod(),
					acParams.getKeytabFile(), false, null, trace);

		} catch (final AccessControlException ace) {
			throw new IllegalArgumentException("Could not start access "
					+ "control, unable to " + "run", ace);
		}

		try {
			ACCESS_CONTROL.requestSsoToken();
		} catch (final AccessControlException ace) {
			throw new IllegalArgumentException("Could not get initial "
					+ "token, unable to " + "run", ace);
		}

	}
}