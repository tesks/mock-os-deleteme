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

import jpl.gds.shared.cli.app.mc.UnsafeOkHttpClient;
import jpl.gds.shared.cli.options.DynamicEnumOption;
import jpl.gds.shared.cli.options.EnumOption;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.numeric.IntegerOption;
import jpl.gds.shared.cli.options.numeric.LongOption;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import static jpl.gds.context.api.options.RestCommandOptions.DEFAULT_REST_HOST;

/**
 * Interface for a Monitor & Control application
 *
 */
public interface IRestfulClientCommandLineApp extends IRestFulServerCommandLineApp {
    /**
     * Informs AbstractRestfulClientCommandLineApp what kind of option to create for RESTful call
     */
    public enum OPTION_TYPES {
        /** Generic String Value */
        STRING(StringOption.class),

        /** Boolean Value */
        BOOLEAN(FlagOption.class),
        
        /** Integer value */
        INT(IntegerOption.class),

        /** Long value */
        LONG(LongOption.class),

        /** A file that can be written to */
        WRITABLE_FILE(FileOption.class),

        /** A file that exists and be read */
        READABLE_FILE(FileOption.class),

        /** Enum */
        ENUM(EnumOption.class),

        /** Dynamic enum */
        DYNAMIC_ENUM(DynamicEnumOption.class)
        ;

        private final Class<?> optionClass;

        private OPTION_TYPES(final Class<?> optionClass) {
            this.optionClass = optionClass;
        }

        /**
         * @param longOpt
         *            the long option string to use when creating the option instance
         * @param argName
         *            the argument name used for the option
         * @param description
         *            the description of the option
         * @param required
         *            true if option is required, false if not required
         * @param parmAllowedValues
         *            allowed values for ENUM options. May be null
         * @param enumClassName
         *            for ENUM options, the Enum class being used
         * @return the instantiated option or null if a failure occurred
         */
        public ICommandLineOption<?> getOption(final String longOpt, final String argName,
                                               final String description, final boolean required,
                                               final String[] parmAllowedValues, final String enumClassName) {
            Constructor<?> ctor = null;
            ICommandLineOption<?> o = null;
            final String className = optionClass.getName();
            try {
                switch (className) {
                    case "jpl.gds.shared.cli.options.StringOption":
                    case "jpl.gds.shared.cli.options.numeric.IntegerOption":
                    case "jpl.gds.shared.cli.options.numeric.LongOption":
                        ctor = optionClass.getConstructor(String.class, String.class, String.class, String.class,
                                                          boolean.class);
                        o = (ICommandLineOption<?>) ctor.newInstance(null, longOpt, argName, description, required);
                        break;
                    case "jpl.gds.shared.cli.options.FlagOption":
                        ctor = optionClass.getConstructor(String.class, String.class, String.class, boolean.class);
                        o = (ICommandLineOption<?>) ctor.newInstance(null, longOpt, description, required);
                        break;
                    case "jpl.gds.shared.cli.options.filesystem.FileOption":
                        ctor = optionClass.getConstructor(String.class, String.class, String.class, String.class,
                                                          boolean.class, boolean.class);
                        o = (ICommandLineOption<?>) ctor.newInstance(null, longOpt, argName, description, required,
                                                                     false);
                        break;
                    case "jpl.gds.shared.cli.options.EnumOption":
                        ctor = optionClass.getConstructor(Class.class, String.class, String.class, String.class,
                                                          String.class, boolean.class, List.class);
                        o = (ICommandLineOption<?>) ctor.newInstance(Class.forName(enumClassName), null, longOpt, argName, description,
                                                                     required, Arrays.asList(parmAllowedValues));
                        break;
                    case "jpl.gds.shared.cli.options.DynamicEnumOption":
                        ctor = optionClass.getConstructor(Class.class, String.class, String.class, String.class,
                                                          String.class, boolean.class);
                        o = (ICommandLineOption<?>) ctor.newInstance(Class.forName(enumClassName), null, longOpt, argName, description,
                                                                     required);
                        break;
                    default:
                        break;
                }
            }
            catch (final Throwable t) {
                t.printStackTrace();
                // ignore -- caught later when option is null
            }
            return o;
        }
    }

    /** The Default Value for the RESTful interface's host */
    public String SECURE_REST_SCHEME   = "https";

    /** The Default Value for the RESTful interface's host */
    public String INSECURE_REST_SCHEME = "http";

    /** The Default Value for the RESTful interface's host */
    public String DEFAULT_REST_SCHEME  = SECURE_REST_SCHEME;

    /** The Default Value for the base URI path for a RESTful application */
    public String DEFAULT_REST_PATH = "/";

    /** Status sub-command */
    public static final String STATUS_CMD            = "status";
    /** Shutdown sub-command */
    public static final String SHUTDOWN_CMD          = "shutdown";
    /** abort sub-command */
    public static final String ABORT_CMD             = "abort";
    /** bind sub-command */
    public static final String BIND_CMD              = "bind";
    /** unbind sub-command */
    public static final String UNBIND_CMD            = "unbind";
    /** start sub-command */
    public static final String ATTACH_CMD            = "attach";
    /** start sub-command */
    public static final String START_CMD             = "start";
    /** stop sub-command */
    public static final String STOP_CMD              = "stop";
    /** stop sub-command */
    public static final String RELEASE_CMD           = "release";
    /** resume sub-command */
    public static final String RESUME_CMD            = "resume";
    /** pause sub-command */
    public static final String PAUSE_CMD             = "pause";
    /** exit sub-command */
    public static final String EXIT_CMD              = "exit";
    /** cmdline sub-command */
    public static final String CMDLINE_CMD           = "cmdline";
    /** perf sub-command */
    public static final String PERF_CMD              = "perf";
    /** properties sub-command */
    public static final String PROP_CMD              = "properties";
    /** properties filter option */
    public static final String PROP_FILTER_OPT       = "filter";
    /** telem sub-command */
    public static final String TELEM_CMD             = "telem";
    /** Processing state sub-command */
    public static final String STATE_CMD             = "state";
    /** session sub-command */
    public static final String SESSION_CMD           = "session";
    /** context sub-command */
    public static final String CONTEXT_CMD           = "context";
    /** query sub-command */
    public static final String QUERY_CMD             = "query";
    /** workers sub-command */
    public static final String WORKERS_CMD           = "workers";
    /** create sub-command */
    public static final String CREATE_CMD            = "create";
    /** MC createSession sub-command */
    public static final String CREATE_SESSION_CMD    = "createSession";
    /** log sub-command */
    public static final String LOG_CMD               = "log";
    /** LOGS sub-command */
    public static final String LOGS_CMD              = "logs";
	/** time comparison strategy subb-command */
    public static final String TIME_COMP_CMD         = "tmcomp";
    /** session key param */
    public static final String KEY_PARAM             = "sessionKey";
    /** session host param */
    public static final String HOST_PARAM            = "sessionHost";
    /** session fragment param */
    public static final String FRAGMENT_PARAM        = "sessionFragment";
    /** Time comparison strategy param */
    public static final String TIME_COMP_PARAM       = "strategy";

    /** key param description */
    public static final String KEY_PARAM_DESC        = "The session key";
    /** host param description */
    public static final String HOST_PARAM_DESC       = "The session host";
    /** fragment param description */
    public static final String FRAGMENT_PARAM_DESC = "The session fragment";

    /**
     * Set the configured base URI into the Swagger-generated ApiClient class.
     * This class is only known by the app class itself, so it is implemented as an abstract method in the
     * AbstractRestfulClientApp
     * 
     * @param uri
     *            the base URI for the API Client to use
     * @param client
     *            an instance of OkHttpClient that ignores all SSL/TLS verification checks (to support --restInsecure on
     *            the client side)
     */
    public void setApiClientBaseURI(String uri, UnsafeOkHttpClient client);

    /**
     * Set client request headers along with the query
     * @param key
     *            the request header key
     * @param value
     *            the value to set the request header key
     */
    public void setApiClientHeader(String key, String value);
    
    /**
     * Retrieve the configured base URI from the Swagger-generated ApiClient class.
     * This class is only known by the app class itself, so it is implemented as an abstract method in the
     * AbstractRestfulClientApp
     * 
     * @return the configured base URI from the Swagger-generated ApiClient class
     */
    public String getApiClientBaseURI();

    /**
     * The HTTP scheme used for M&C RESTful Interface. Will be https if not configured.
     * 
     * @return the host at which a RESTful M&C RESTful Interface may be accessed
     */
    default String getRestScheme() {
        return DEFAULT_REST_SCHEME;
    }

    /**
     * The host at which a RESTful M&C RESTful Interface may be accessed. Will be localhost if not configured.
     * 
     * @return the host at which a RESTful M&C RESTful Interface may be accessed
     */
    default String getRestHost() {
        return DEFAULT_REST_HOST;
    }

    /**
     * The path at which a RESTful M&C RESTful Interface may be accessed. Will be '/' if not configured.
     * 
     * @return the path at which a RESTful M&C RESTful Interface may be accessed
     */
    public default String getRestPath() {
        return DEFAULT_REST_PATH;
    }
    

    /**
     * Extract the URI components fro provided URI string. The components' values are stored and retrievable through
     * their associated getters
     * 
     * @param uriString
     *            the string from which to derive its components
     */
    public void extractComponentsFromURI(String uriString);

    /**
     * Creates and returns the URI string from the currently set values.
     * 
     * @return the URI string representing the current configuration of this M&C Restful Interface
     */
    public String getBaseURI();

    /**
     * Creates and returns the URI string from the provided values.
     * 
     * @param scheme
     *            HTTP Scheme ("http", "https", etc.)
     * @param host
     *            the RESTful host
     * @param port
     *            the RESTful port
     * @param path
     *            the RESTful base path
     * @return the URI string representing the provided values for this M&C Restful Interface
     */
    public String getBaseURI(String scheme, String host, int port, String path);

    /**
     * @return the HTTP Status exit code.
     */
    public int getExitCode();

    /**
     * Call System.exit() with the exit code
     */
    public void exit();

    /**
     * @param args
     *            Perform the specified RESTful service
     * @return the reference to the application (builder pattern).
     */
    public IRestfulClientCommandLineApp process(String[] args);
}
