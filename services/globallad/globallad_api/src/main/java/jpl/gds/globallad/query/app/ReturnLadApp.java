/*
 * Copyright 2006-2020. California Institute of Technology.
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
package jpl.gds.globallad.query.app;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.QueryType;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState;
import jpl.gds.globallad.data.utilities.BaseGlobalLadQuery;
import jpl.gds.globallad.data.utilities.GlobalLadQueryParams;
import jpl.gds.globallad.data.utilities.GlobalLadQueryParams.GlobalLadQueryParamsBuilder;
import jpl.gds.globallad.options.GlobalLadOptions;
import jpl.gds.globallad.rest.resources.QueryOutputFormat;
import jpl.gds.globallad.spring.beans.SpringPropertySourcesLoader;
import jpl.gds.security.spring.bootstrap.SecuritySpringBootstrap;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.config.GdsSpringSystemProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.bootstrap.SharedSpringBootstrap;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * The ReturnLadApp is the command line application used to retrieve latest
 * channel values from a specific venue or session from the Global LAD
 */
public class ReturnLadApp extends AbstractCommandLineApp implements Runnable {
    private static final String        REST_INSECURE_SHORT        = null;
    private static final String        REST_INSECURE_LONG         = "restInsecure";

	/**
	 * Fixed usage statements which include setting the app name properly.
	 */
	private static final String QUERY_TYPE_SYSTEM_PROPERTY = "QueryType";
	private static final String APP_NAME_BASE = "chill_return_lad_%s";
	
    /** Tracer */
    private final Tracer               logger;
    
    private boolean verifiedQuery;
    
    private DataSource source = DataSource.all;
    private RecordedState state = RecordedState.both;
    private GlobalLadPrimaryTime timeType = GlobalLadPrimaryTime.EVENT;
    
    private Collection<Long> sessionIds = Collections.emptyList();
	private final Collection<String> hostRegexes = Collections.emptyList();
	private Collection<String> venueRegexes = Collections.emptyList();
	private Collection<Integer> dssIds = Collections.emptyList();
	private Collection<Integer> vcids = Collections.emptyList(); 
	private Integer scid;
	private Integer maxResults;
	
	private String lowerBoundTimeString;
	private String upperBoundTimeString;

	// EHA specific
    private Collection<String> channelIds = Collections.emptyList();

    // EVR specific
    private Collection<String> evrLevels = Collections.emptyList();
	private Collection<String> evrNameRegexes = Collections.emptyList();
	private Collection<String> messageRegexes = Collections.emptyList();  
	
    private boolean                    showColHeaders;

    private final QueryType            qtype;
    private final String               appName;

    /** The option for setting the port on which an optional RESTful server will listen */
    protected FlagOption               restInsecureOption         = new FlagOption(REST_INSECURE_SHORT,
                                                                                   REST_INSECURE_LONG,
                                                                                   "Supplying this option turns off HTTPS "
                                                                                           + "(SSL/TLS) encryption for the "
                                                                                           + "RESTful Global LAD Service.\n"
                                                                                           + "NOTE: The Client and "
                                                                                           + "Server must be in agreement on this.",
                                                                                   false);

    /**
     * Currently the output format is only csv and JSON.  Some of the code for templating is still below but commented out. 
     * Is it necessary to template this tool?  I don't know.
     */
    protected QueryOutputFormat outputFormat;
    

    private final ApplicationContext appContext;

    /**
     * @param queryType
     *            The type of query this is.
     * @param appName
     *            the app name
     * @throws IOException
     *             on error loading config
     */
    public ReturnLadApp(final QueryType queryType, final String appName) throws IOException {
        this.qtype = queryType;
        this.appName = appName;

        this.appContext = SpringContextFactory.getSpringContext(new String[] {
                SecuritySpringBootstrap.class.getPackage().getName(),
                SharedSpringBootstrap.class.getPackage().getName() });

        this.logger = TraceManager.getTracer(appContext, Loggers.GLAD);
        GdsSpringSystemProperties.loadAMPCSProperySources(appContext.getBean(ConfigurableEnvironment.class),
                                                          logger,
                                                          SpringPropertySourcesLoader.GLAD_SPRING_PROPERTIES_FILENAME,
                                                          appContext.getBean(SseContextFlag.class));

        ApplicationConfiguration.setApplicationName(appName);
    }

    private String getUsage() {
		/**
		 * Fixed usage statements.
		 */
        final StringBuilder b = new StringBuilder(appName + " ");
    		
    		switch (qtype) {
			case eha:
				b.append(String.format("[--%s <string,...>] ", GlobalLadOptions.CHANNEL_VALUE_OPTION.getLongOpt()));
				break;
			case evr:
				b.append(String.format("[--%s <string,...>] ", GlobalLadOptions.EVR_LEVEL_OPTION.getLongOpt()));
				b.append(String.format("[--%s <string,...>] ", GlobalLadOptions.EVR_NAME_OPTION.getLongOpt()));
				b.append(String.format("[--%s <string,...>] ", GlobalLadOptions.EVR_MSG_OPTION.getLongOpt()));
				break;
			default:
				break;
    		}
    		
		b.append("[Search options - Not required]");
    		
    		return b.toString();
    }

    @Override
    public BaseCommandOptions createOptions() {
        final GlobalLadOptions options = new GlobalLadOptions(this);

		options.addOption(GlobalLadOptions.SESSION_KEY_OPTION);
		options.addOption(GlobalLadOptions.SESSION_HOST_OPTION);
		options.addOption(GlobalLadOptions.BEGIN_TIME_OPTION);
		options.addOption(GlobalLadOptions.END_TIME_OPTION);
		options.addOption(GlobalLadOptions.SCID_OPTION);
		options.addOption(GlobalLadOptions.VENUE_OPTION);
		options.addOption(GlobalLadOptions.VCID_OPTION);
		options.addOption(GlobalLadOptions.DSSID_OPTION);
		options.addOption(GlobalLadOptions.MAX_RESULTS_OPTION);
		options.addOption(GlobalLadOptions.REST_SERVER_PORT_OPTION);
		options.addOption(GlobalLadOptions.REST_SEVER_HOST_OPTION);
		options.addOption(GlobalLadOptions.SHOW_COLUMN_OPTION);
		options.addOption(GlobalLadOptions.VERIFIED_QUERY_OPTION);
        
		options.addOption(GlobalLadOptions.RECORDED_STATE_OPTION);
		options.addOption(GlobalLadOptions.DATA_SOURCE_OPTION);
		options.addOption(GlobalLadOptions.TIME_TYPE_OPTION);
		options.addOption(GlobalLadOptions.OUTPUT_FORMAT_OPTION);

		
        // Channel query values.
        if (qtype == QueryType.eha || qtype == QueryType.alarm) {
        		options.addOption(GlobalLadOptions.CHANNEL_VALUE_OPTION);
        }
        
        // EVR query values.
        if (qtype == QueryType.evr) {
	        	options.addOption(GlobalLadOptions.EVR_LEVEL_OPTION);
	        	options.addOption(GlobalLadOptions.EVR_MSG_OPTION);
	        	options.addOption(GlobalLadOptions.EVR_NAME_OPTION);
        }
        options.addOption(restInsecureOption);
        return options;
    }

    private Collection<Long> longConvert(final Collection<UnsignedInteger> vals) {
    		return vals.stream()
    			.map(UnsignedInteger::longValue)
    			.collect(Collectors.toSet());
    }

    private Collection<Integer> integerConvert(final Collection<UnsignedInteger> vals) {
    		return vals.stream()
    			.map(UnsignedInteger::intValue)
    			.collect(Collectors.toSet());
    }
    
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
    		super.configure(commandLine);

    		final GlobalLadProperties config = GlobalLadProperties.getGlobalInstance();
    		
    		this.verifiedQuery = GlobalLadOptions.VERIFIED_QUERY_OPTION.parse(commandLine);
    		
    		final Integer portStr = GlobalLadOptions.REST_SERVER_PORT_OPTION.parse(commandLine);
    		
    		if (portStr != null && portStr > 0) {
    			config.setGlobalLadRestServerPort(portStr);
    		}

        final String serverHost = GlobalLadOptions.REST_SEVER_HOST_OPTION.parse(commandLine);

        // Override globallad host configuration with command-line host
        if (serverHost != null) {
            config.setGlobalLadHost(serverHost);
        }

    		/* HTTPS must be enabled in the configuration and not disabled on the command line for it to be active */
    		config.setHttpsEnabled(config.isHttpsEnabled() && !restInsecureOption.parse(commandLine)); // allow disabling HTTPS

    		this.lowerBoundTimeString = GlobalLadOptions.BEGIN_TIME_OPTION.parse(commandLine);
    		this.upperBoundTimeString = GlobalLadOptions.END_TIME_OPTION.parse(commandLine);

    		this.venueRegexes = GlobalLadOptions.VENUE_OPTION.parse(commandLine);
    		this.dssIds = integerConvert(GlobalLadOptions.DSSID_OPTION.parse(commandLine));
    		this.vcids = integerConvert(GlobalLadOptions.VCID_OPTION.parse(commandLine));
    		this.sessionIds = longConvert(GlobalLadOptions.SESSION_KEY_OPTION.parse(commandLine));
    		this.scid = GlobalLadOptions.SCID_OPTION.parse(commandLine);
    		
    		this.maxResults = GlobalLadOptions.MAX_RESULTS_OPTION.parse(commandLine);
    		if (this.maxResults == null) {
    			this.maxResults = 1;
    		}

    		this.source = GlobalLadOptions.DATA_SOURCE_OPTION.parse(commandLine);
    		this.source = source == null ? DataSource.all : source;
    		
    		this.timeType = GlobalLadOptions.TIME_TYPE_OPTION.parse(commandLine);
    		this.timeType = timeType == null ? GlobalLadPrimaryTime.EVENT : this.timeType;
    		
    		this.state = GlobalLadOptions.RECORDED_STATE_OPTION.parse(commandLine);
    		this.state = state == null ? RecordedState.both : state;

    		this.outputFormat = GlobalLadOptions.OUTPUT_FORMAT_OPTION.parse(commandLine);

    		/**
    		 * Must have channel or evr and added error msgs and exit statements.
    		 */
        if (qtype == QueryType.eha) {
        		this.channelIds = GlobalLadOptions.CHANNEL_VALUE_OPTION.parse(commandLine);
        		if (this.channelIds == null) {
                logger.warn("Missing required option: --channelIds.");
        			this.showHelp();
        			System.exit(1);
        		}
        		
        } else if (qtype == QueryType.evr) {
        		this.evrLevels = GlobalLadOptions.EVR_LEVEL_OPTION.parse(commandLine);
        		this.evrNameRegexes = GlobalLadOptions.EVR_NAME_OPTION.parse(commandLine);
        		this.messageRegexes = GlobalLadOptions.EVR_MSG_OPTION.parse(commandLine);
        		
        		//CSV Options
        		if (this.evrLevels.isEmpty() && this.evrNameRegexes.isEmpty() && this.messageRegexes.isEmpty()) {
                logger.warn("Missing filter criteria.");
        			this.showHelp();
        			System.exit(1);
        		}
        }
        else if(qtype == QueryType.alarm){
        	//option is not required
	        this.channelIds = GlobalLadOptions.CHANNEL_VALUE_OPTION.parse(commandLine);
        }
        
        this.showColHeaders = GlobalLadOptions.SHOW_COLUMN_OPTION.parse(commandLine);
    }


    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        /* Get nested Options object */
        final OptionSet options = createOptions().getOptions();

        final PrintWriter pw = new PrintWriter(System.out);
        
        pw.println(getUsage());
        pw.println("                   ");
        options.printOptions(pw);
        pw.flush();
    }
    

    @Override
    public void run() {
    		BaseGlobalLadQuery query;
    		try {
            query = new BaseGlobalLadQuery(appContext);
		} catch (final Exception e) {
            logger.error("Exception encountered querying: ", ExceptionUtils.getMessage(e),
                         ". See log file for more details");
            logger.error(Markers.SUPPRESS, ExceptionUtils.getMessage(e), e);
			return;
		}

		GlobalLadQueryParamsBuilder builder = GlobalLadQueryParams.createBuilder()
				.setQueryType(qtype)
				.setSource(source)
				.setRecordedState(state)
				.setTimeType(timeType)
				.setOutputFormat(outputFormat)
				.setSessionIds(sessionIds)
				.setHostRegexes(hostRegexes)
				.setVenueRegexes(venueRegexes)
				.setDssIds(dssIds)
				.setVcids(vcids)
				.setScid(scid)
				.setMaxResults(maxResults)
				.setLowerBoundTimeString(lowerBoundTimeString)
				.setUpperBoundTimeString(upperBoundTimeString)
				.setVerified(verifiedQuery)
				.setShowColumnHeaders(showColHeaders);

		
		switch(qtype) {
			case eha:
			case alarm:
				builder = builder.setChannelIds(channelIds);
				break;
			case evr:
				builder.setEvrLevelRegexes(evrLevels)
						.setEvrNameRegexes(evrNameRegexes)
						.setMessageRegexes(messageRegexes);
				break;
			default:
				break;
		}

		try (BufferedInputStream is = new BufferedInputStream(query.getResponseStream(builder.build(), false))) {
			int bytesRead = 0;
			final byte[] buffer = new byte[4096];
			
			do {
				bytesRead = is.read(buffer);
				
				if (bytesRead > 0) {
					System.out.write(buffer, 0, bytesRead);
				}
			} while (bytesRead > 0);
		} catch (final IOException e) {
            logger.error(ExceptionTools.getMessage(e), e);
		}
    }

    /**
     * Main method to start the app
     * 
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        try {
	    		/**
	    		 * Get the query type system property to figure out what type of query this run is.
	    		 */
            final QueryType queryType = QueryType.valueOf(GdsSystemProperties.getSystemProperty(QUERY_TYPE_SYSTEM_PROPERTY));
        		
        		String appNameEnd = "";
        		switch(queryType) {
					case eha:
						appNameEnd = "chanvals"; break;
					case evr:
						appNameEnd = "evr"; break;
					case alarm:
						appNameEnd = "alarm"; break;
			        default:
			        	break;
        		}

        		final String appName = String.format(APP_NAME_BASE, appNameEnd);
        		
            final ReturnLadApp returnLad = new ReturnLadApp(queryType, appName);
            final BaseCommandOptions options = returnLad.createOptions();
            final ICommandLine commandLine = options.parseCommandLine(args, true);

            returnLad.configure(commandLine);
			
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.ERROR);

            returnLad.run();
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.INFO);

        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error("Parse error: ", ExceptionTools.getMessage(e));
        } catch (final Exception e) {
            TraceManager.getDefaultTracer().error("Unexpected error: ", ExceptionTools.getMessage(e));
            TraceManager.getDefaultTracer().error(Markers.SUPPRESS, "Unexpected error: ", ExceptionTools.getMessage(e), e);
        }
    }
}
