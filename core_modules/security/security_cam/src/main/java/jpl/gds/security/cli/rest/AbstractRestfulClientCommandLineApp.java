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
package jpl.gds.security.cli.rest;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.ParseException;
import org.apache.commons.httpclient.HttpStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.security.options.AccessControlCommandOptions;
import jpl.gds.common.config.types.LoginEnum;
import jpl.gds.security.cam.AccessControl;
import jpl.gds.security.cam.AccessControlException;
import jpl.gds.shared.annotation.Operation;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.app.HelpOrVersionDisplayedException;
import jpl.gds.context.cli.app.mc.AbstractRestfulServerCommandLineApp;
import jpl.gds.context.cli.app.mc.IRestfulClientCommandLineApp;
import jpl.gds.shared.cli.app.mc.UnsafeOkHttpClient;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;

import static jpl.gds.context.api.options.RestCommandOptions.DEFAULT_REST_HOST;

/**
 * Class AbstractRestfulClientCommandLineApp
 */
public abstract class AbstractRestfulClientCommandLineApp extends AbstractRestfulServerCommandLineApp
        implements IRestfulClientCommandLineApp {

    /** Shared Tracer to use */
    protected final Tracer                        log;


    private static final String                   API_EXCEPTION_NAME                     = "ApiException";
    private static final String                   API_EXCEPTION_GET_CODE_METHOD          = "getCode";
    private static final String                   API_EXCEPTION_GET_RESPONSE_BODY_METHOD = "getResponseBody";
    private static final String                   API_EXCEPTION_GET_MESSAGE              = "getLocalizedMessage";
    private static final String                   API_EXCEPTION_GET_RESPONSE_HEADERS     = "getResponseHeaders";
    public static final String                   UNAUTHENTICATED_RESPONSE_HEADER_KEYWORD= "AMAuthCookie";

    private String                                restScheme                             = DEFAULT_REST_SCHEME;
    private String                                restHost                               = DEFAULT_REST_HOST;
    private String                                restPath                               = DEFAULT_REST_PATH;




    private static AccessControl ACCESS_CONTROL = null;
    private final AccessControlCommandOptions accessOptions;

    /** Spring context */
	protected final ApplicationContext springContext;
    private final SecurityProperties securityProps;
    
     /**
     * If true, will attempt to retry in the event of an invalid SSL/TLS certificiate or protocol error.
     * If false, will not attempt this retry.
     */
    private final boolean                         attemptInsecureRetries;

    /**
     * Calculated list of sub-commands supported for this RESTful App.
     */
    private final Map<String, SubCommandMetadata> subCommands;
    private SubCommandMetadata                    subCommand                             = null;
    private int                                   exitCode                               = 0;

    /**
     * TP stop times out when product assembly has not finished
     * For --restInsecure option, see related timeout setting in class {@link jpl.gds.shared.cli.app.mc.UnsafeOkHttpClient}
     */
    protected static final int MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES = 10;

    /**
     * This class encapsulates all of the data derived from the @Operation annotation as well as the methods they
     * annotate. Also provides an invocation interface for calling the RESTful services.
     */
    public static class SubCommandMetadata {
        private final IRestfulClientCommandLineApp object;
        private final Method                       method;
        private final Class<?>[]                   argTypes;
        private final String                       subCmd;
        private final String                       subCmdDesc;
        private final ICommandLineOption<?>[]      options;
        private final Gson                         gson;

        /**
         * @param object
         *            the IRestfulClientCommandLineApp object containing the methods to call
         * @param method
         *            the specific method being annotated
         * @param operation
         *            the class representing the operation annotation
         */
        protected SubCommandMetadata(final IRestfulClientCommandLineApp object, final Method method,
                final Operation operation) {
            super();
            final int argCount = operation.parmTypes().length;
            final OPTION_TYPES[] optionTypes = new OPTION_TYPES[argCount];

            this.object = object;
            this.method = method;
            this.argTypes = method.getParameterTypes();
            this.options = new ICommandLineOption<?>[argCount];
            this.subCmd = operation.subCmd();
            this.subCmdDesc = operation.subCmdDesc();

            /*
             * Set up for JSON and Pretty-Printed JSON.
             */
            if (operation.returnsJSON()) {
                final GsonBuilder gsonBuilder = new GsonBuilder();
                if (operation.prettyJSON()) {
                    gsonBuilder.setPrettyPrinting();
                }
                this.gson = gsonBuilder.create();
            }
            else {
                this.gson = null;
            }

            for (int i = 0; i < argCount; i++) {
                optionTypes[i] = OPTION_TYPES.valueOf(operation.parmTypes()[i]);

                String optName;
                try {
                    optName = operation.optNames()[i];
                }
                catch (final IndexOutOfBoundsException e) {
                    optName = "";
                }

                String parmName;
                try {
                    parmName = operation.parmNames()[i];
                }
                catch (final IndexOutOfBoundsException e) {
                    parmName = "";
                }

                String parmDesc;
                try {
                    parmDesc = operation.parmDesc()[i];
                }
                catch (final IndexOutOfBoundsException e) {
                    parmDesc = "";
                }

                String[] parmAllowedValues;
                try {
                    parmAllowedValues = operation.parmAllowedValues()[i].split("[\\s,:;]");
                }
                catch (final IndexOutOfBoundsException e) {
                    parmAllowedValues = new String[0];
                }

                String enumClassName;
                try {
                    enumClassName = operation.enumClassName()[i];
                }
                catch (final IndexOutOfBoundsException e) {
                    enumClassName = null;
                }
                options[i] = optionTypes[i].getOption(optName, parmName, parmDesc, false, parmAllowedValues,
                                                      enumClassName);
            }
        }

        /**
         * @return the Sub-Command for which this metadata object applies
         */
        public String getName() {
            return subCmd;
        }

        /**
         * @return the sub command's description
         */
        public String getSubCmdDesc() {
            return subCmdDesc;
        }

        /**
         * @return the Options created to satisfy the arguments required to call this sub-command's method
         */
        public ICommandLineOption<?>[] getOptions() {
            return options;
        }

        /**
         * @param args
         *            the arguemnts to pass to this sub-command's method
         * @return the output from the RESTful call
         * @throws IllegalAccessException
         *             if a Security Access violation occurs
         * @throws IllegalArgumentException
         *             if an argument is not appropriate for the method call
         * @throws InvocationTargetException
         *             if the method being called throws
         */
        public Object invoke(final Object... args)
                throws IllegalAccessException, InvocationTargetException {
            final Object o = method.invoke(object, args);
            return (gson == null) ? o : gson.toJson(o);
        }

        @Override
        public String toString() {
            final StringBuilder argTypeStrings = new StringBuilder();
            final StringBuilder optionsStrings = new StringBuilder();

            for (int i = 0; i < argTypes.length; i++) {
                if (i > 0) {
                    argTypeStrings.append(", ");
                }
                argTypeStrings.append(argTypes[i].getSimpleName());
            }
            for (int i = 0; i < options.length; i++) {
                if (i > 0) {
                    optionsStrings.append(", ");
                }
                optionsStrings.append(options[i]);
            }
            return "SubCommandMetadata [object=" + object.getClass().getSimpleName() + ", method=" + method.getName()
                    + ", argTypes=[" + argTypeStrings.toString() + "], options=[" + optionsStrings.toString() + "]]";
        }
    }

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     */
    public AbstractRestfulClientCommandLineApp() {
        this(true, false);
    }

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     * 
     * @param addHook
     *            whether or not to add the shutdown hook
     * @param attemptInsecureRetries
     *            if true, will attempt to retry in the event of an invalid SSL/TLS certificate or protocol error.
     *            if false, will not attempt this retry.
     * 
     */
    public AbstractRestfulClientCommandLineApp(final boolean addHook, final boolean attemptInsecureRetries) {
        super(addHook);
        this.springContext = SpringContextFactory.getSpringContext(true);
        this.securityProps = springContext.getBean(SecurityProperties.class);
        accessOptions = new AccessControlCommandOptions(securityProps,
                                                        springContext.getBean(AccessControlParameters.class));
        log = TraceManager.getTracer(springContext, Loggers.UTIL);
		
        this.attemptInsecureRetries = attemptInsecureRetries;

        /*
         * Calculate possible command modifiers
         */
        final Map<String, SubCommandMetadata> subCommands = new HashMap<>();
        for (final Method method : this.getClass().getMethods()) {
            final Annotation annotation = method.getDeclaredAnnotation(Operation.class);
            if (annotation instanceof Operation) {
                final Operation operation = (Operation) annotation;
                try {
                    subCommands.put(operation.subCmd(), new SubCommandMetadata(this, method, operation));
                }
                catch (final Exception e) {
                    e.printStackTrace();
                    System.err.println(e.getLocalizedMessage());
                    showHelp();
                    System.exit(-1);
                }
            }
        }
        this.subCommands = subCommands;
    }

    @Override
    protected String getRestPortOptionDescription() {
        return "The port on which the RESTful M&C Client communicates with the server.";
    }

    /**
     * @param args
     *            the command line arguments passed into the main() method
     */
    @Override
    public IRestfulClientCommandLineApp process(final String[] args) {
        final StringBuilder stdoutSb = new StringBuilder();
        final StringBuilder stderrSb = new StringBuilder();

        subCommand = getSubCommand(args);
        
        try {
            ICommandLine commandLine = null;
            final AtomicInteger retry = new AtomicInteger(0);
            do {
                try {
                    commandLine = createOptions().parseCommandLine(args, false, false, true);
                    if (commandLine != null) {
                        if (retry.get() == 0) {
                            configure(commandLine);
                        }
                        final String result = process(commandLine);
                        if (stdoutSb.length() > 0) {
                            stdoutSb.append('\n');
                        }
                        stdoutSb.append(result);
                    }
                }
                catch (final HelpOrVersionDisplayedException e) {
                    if (commandLine != null) {
                        if (!e.isHelpDisplayed()) {
                            showHelp();
                        }
                    }
                }
                catch (final IllegalArgumentException | ParseException e) {
                    this.exitCode = -2;
                    stderrSb.append("Client Error: " + e.getLocalizedMessage()).append('\n');
                    this.subCommand = null;
                    showHelp();
                }
                catch (final InvocationTargetException e) {
                    this.exitCode = -3;
                    if (retry.get() == 0) {
                        stderrSb.append("WARNING: Server Error: ");
                    }
                    final Throwable cause = e.getCause();
                    if (cause == null) {
                        stderrSb.append(e.getClass().getSimpleName()).append(": ").append(e.getLocalizedMessage());
                    }
                    else {
                        final Throwable causeCause = cause.getCause();
                        final Class<? extends Throwable> theExceptionClass = cause.getClass();
                        if (theExceptionClass.getSimpleName().equals(API_EXCEPTION_NAME)) {
                            try {
                                final Method getCode = theExceptionClass.getMethod(API_EXCEPTION_GET_CODE_METHOD);
                                final Method getResponseBody = theExceptionClass.getMethod(API_EXCEPTION_GET_RESPONSE_BODY_METHOD);
                                final Method getLocalizedMessage = theExceptionClass.getMethod(API_EXCEPTION_GET_MESSAGE);
                                final Method getResponseHeaders = theExceptionClass.getMethod(API_EXCEPTION_GET_RESPONSE_HEADERS);
                                final Integer code = (Integer) getCode.invoke(cause);
                                final Object responseBody = getResponseBody.invoke(cause);
                                if ((code == Integer.valueOf(0)) || (getResponseBody.invoke(cause) == null)) {
                                    final Object message = getLocalizedMessage.invoke(cause);
                                    if ((message != null) && (message.toString().length() > 0)) {
                                        stderrSb.append(cause.getClass().getSimpleName()).append(": ").append(message);
                                    }
                                    else {
                                        stderrSb.append(cause.getClass().getSimpleName()).append(": ")
                                                .append(e.getLocalizedMessage()).append(" caused by: ")
                                                .append(cause.getLocalizedMessage());
                                    }
                                }
                                else {
                                    if (getResponseHeaders.invoke(cause).toString().contains(UNAUTHENTICATED_RESPONSE_HEADER_KEYWORD)) {
                                    	this.exitCode = HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED;
                                        stderrSb.append("Server requires authentication. Please supply \""+this.accessOptions.LOGIN_METHOD_NON_GUI.getLongOrShort()+"\" option.");
                                    }
                                    else {
                                    	this.exitCode = code;
                                        stderrSb.append(cause.getClass().getSimpleName()).append(": ").append(responseBody)
                                        .append(" (").append(this.exitCode).append(")");
                                    }
                                }
                            }
                            catch (final NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                                    | InvocationTargetException e1) {
                                stderrSb.append(cause.getClass().getSimpleName()).append(": ")
                                        .append(cause.getLocalizedMessage());
                            }
                        }
                        else {
                            stderrSb.append(cause.getClass().getSimpleName()).append(": ")
                                    .append(cause.getLocalizedMessage());
                        }
                        if (attemptInsecureRetries && (retry.get() == 0) && (causeCause != null)) {
                            toggleSsl();
                            retry.incrementAndGet();
                            final int idx = stderrSb.indexOf(", plaintext connection?");
                            if (idx >= 0) {
                                stderrSb.replace(idx, stderrSb.length(), " -- ");
                            }
                            else {
                                stderrSb.append(" -- ");
                            }
                            stderrSb.append("Retrying with plaintext connection...");
                            continue;
                        }
                    }
                }
                catch (final Throwable t) {
                    this.exitCode = -4;
                    t.printStackTrace();
                }
            } while (retry.getAndIncrement() == 1);
        }
        finally {
            if (stderrSb.length() > 0) {
                System.err.println(stderrSb.toString());
                System.err.flush();
            }
            if (stdoutSb.length() > 0) {
                System.out.println(stdoutSb.toString());
                System.out.flush();
            }
        }
        return this;
    }


    /**
     * Get sub command
     *
     * @param args
     *            this application's command line
     * @return SubCommandMetadata object
     */
    public SubCommandMetadata getSubCommand(final String[] args) {
        SubCommandMetadata sc = null;
        /* Pre-process command line to determine what subcommand has been specified */
        /* TODO: 01/08/2018: Allow multiple subcommand processing */
        for (final String arg : args) {
            if (subCommands.keySet().contains(arg)) {
                sc = subCommands.get(arg);
                break;
            }
        }
        return sc;
    }

    private void toggleSsl() {
        if (restScheme.equals(SECURE_REST_SCHEME)) {
            restScheme = INSECURE_REST_SCHEME;
            restIsSecure = false;
        }
        else if (restScheme.equals(INSECURE_REST_SCHEME)) {
            restScheme = SECURE_REST_SCHEME;
            restIsSecure = true;
        }

        /* Install certificate ignoring http client if not secure */
        setApiClientBaseURI(getBaseURI(), restIsSecure ? null : new UnsafeOkHttpClient());
    }

    /**
     * @param commandLine
     *            this application's command line
     * @throws IllegalAccessException
     *             if a Security Access violation occurs
     * @throws IllegalArgumentException
     *             if an argument is not appropriate for the method call
     * @throws InvocationTargetException
     *             if the method being called throws
     * @throws ParseException
     *             if the command line cannot be parsed
     */
    private String process(final ICommandLine commandLine)
            throws IllegalAccessException, InvocationTargetException, ParseException {
        if (null == this.subCommand) {
            throw new IllegalArgumentException("*** Illegal Sub-Command Specified: " + subCommand);
        }
        final ICommandLineOption<?>[] options = this.subCommand.getOptions();
        final int argCount = options.length;
        final Object[] args = new Object[argCount];
        for (int i = 0; i < argCount; i++) {
            args[i] = options[i].parse(commandLine);
        }
        return this.subCommand.invoke(args).toString();
    }

    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }
        super.createOptions();
        options.addOption(restOptions.REST_HOST_OPTION);
        
        options.addOption(this.accessOptions.KEYTAB_FILE);
        options.addOption(this.accessOptions.LOGIN_METHOD_NON_GUI);
        options.addOption(this.accessOptions.USER_ID);
        
        if (this.subCommand != null) {
            options.addOptions(Arrays.asList(subCommand.getOptions()));
        }
        return options;
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        /* get the current URI paramters from the ApiClient class in the main app class */
        extractComponentsFromURI(getApiClientBaseURI());
        /* Process command line optins */
        super.configure(commandLine);

        /* Set the RESTful host */
        restHost = restOptions.REST_HOST_OPTION.parseWithDefault(commandLine, false, true);
        if (restHost == null) {
            try {
                restHost = InetAddress.getLocalHost().getCanonicalHostName();
            }
            catch (final UnknownHostException e) {
                restHost = DEFAULT_REST_HOST;
            }
        }

        /* Set the RESTful scheme */
        restScheme = restIsSecure ? SECURE_REST_SCHEME : INSECURE_REST_SCHEME;

        /* Set correct URI into ApiClient class in main app class. */
        setApiClientBaseURI(getBaseURI(), restIsSecure ? null : new UnsafeOkHttpClient());

        if (commandLine.getOptionValue(this.accessOptions.LOGIN_METHOD_NON_GUI.getLongOrShort()) == null) {
            // No authentication requested, continue without setting cookie.
            log.trace("No loginMethod specified on command line. Skipping authentiation");
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
        setApiClientHeader("Cookie", cookie.trim());

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
					acParams.getKeytabFile(), false, null, log);

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

    @Override
    public String getRestScheme() {
        return restScheme;
    }

    @Override
    public String getRestPath() {
        return restPath;
    }

    @Override
    public String getRestHost() {
        return restHost;
    }

    @Override
    public String getBaseURI() {
        final UriComponents uriComponents = UriComponentsBuilder.newInstance().scheme(this.restScheme)
                                                                .host(this.restHost).port(this.restPort)
                                                                .path(this.restPath).build();
        return uriComponents.toUriString();
    }

    @Override
    public String getBaseURI(final String scheme, final String host, final int port, final String path) {
        final UriComponents uriComponents = UriComponentsBuilder.newInstance().scheme(scheme).host(host).port(port)
                                                                .path(path).build();
        return uriComponents.toUriString();
    }

    @Override
    public void extractComponentsFromURI(final String uriString) {
        final UriComponents uri = UriComponentsBuilder.fromUriString(uriString).build();
        this.restScheme = uri.getScheme();
        this.restHost = uri.getHost();
        this.restPort = uri.getPort();
        this.restPath = uri.getPath();
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    @Override
    public void exit() {
        System.exit(exitCode);
    }

    @Override
    public void showHelp() {
        createOptions().getOptions();
        super.showHelp("Usage: " + ApplicationConfiguration.getApplicationName()
                + " [options] [[SubCommand] [SubCommand options]]");

        final PrintWriter pw = new PrintWriter(System.out);
        pw.println();

        final SortedSet<String> keySet = new TreeSet<>();
        if (this.subCommand != null) {
            keySet.add(subCommand.subCmd);
        }
        else {
            keySet.addAll(subCommands.keySet());
        }
        pw.printf("   %-10s   %s%n", "SubCommand", "Description");
        pw.printf("   %-10s   %s%n", "==========", "=============================================================");
        for (final String key : keySet) {
            final SubCommandMetadata scm = subCommands.get(key);
            if (null == scm) {
                if (subCommand != null) {
                    System.err.println("*** Illegal Sub-Command Specified: " + subCommand);
                    System.out.println();
                    this.subCommand = null;
                }
                showHelp();
                System.exit(-1);
            }
            pw.printf("   %-10s - %s%n", key, scm.getSubCmdDesc());
            final OptionSet os = new OptionSet();
            for (final ICommandLineOption<?> option : scm.getOptions()) {
                os.addOption(option);
            }
            os.printOptions(pw);
        }
        pw.flush();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [restScheme=" + restScheme + ", restHost=" + restHost + ", restPort="
                + restPort + ", restPath=" + restPath + ", getBaseURI()=" + getBaseURI() + ", restIsSecure="
                + restIsSecure + "]";
    }
}
