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
package jpl.gds.automation.mtak;

import jpl.gds.cfdp.data.api.ECfdpIndicationType;
import jpl.gds.cfdp.message.api.CfdpMessageType;
import jpl.gds.cfdp.message.api.ICfdpIndicationMessage;
import jpl.gds.cli.legacy.app.AbstractCommandLineApp;
import jpl.gds.cli.legacy.options.MpcsOption;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.service.ServiceConfiguration;
import jpl.gds.common.config.service.ServiceConfiguration.ServiceType;
import jpl.gds.context.api.*;
import jpl.gds.context.api.message.IContextHeartbeatMessage;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.eha.api.channel.ChannelValueFilter;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.api.message.IEhaMessageFactory;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState;
import jpl.gds.globallad.data.utilities.GlobalLadQueryParams;
import jpl.gds.globallad.data.utilities.GlobalLadQueryParams.GlobalLadQueryParamsBuilder;
import jpl.gds.globallad.utilities.CoreGlobalLadQuery;
import jpl.gds.globallad.utilities.ICoreGlobalLadQuery;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.*;
import jpl.gds.message.api.util.MessageFilterMaker;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.session.message.StartOfSessionMessage;
import jpl.gds.shared.channel.ChannelListRangeException;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.interfaces.EscapedCsvSupport;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.util.HostPortUtility;
import org.apache.commons.cli.*;
import org.springframework.context.ApplicationContext;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static jpl.gds.cfdp.data.api.ECfdpIndicationType.*;


/** Implements an application that sends filtered telemetry from the JMS bus over a socket. */
public class MtakDownlinkServerApp extends AbstractCommandLineApp
        implements IMessageServiceListener, IQuitSignalHandler
{
    
    public static final String FETCH_LAD_OPTION_LONG = "fetchLad";
    public static final String IGNORE_EHA_OPTION_LONG = "ignoreEha";
    public static final String IGNORE_EVRS_OPTION_LONG = "ignoreEvrs";
    public static final String IGNORE_PRODUCTS_OPTION_LONG = "ignoreProducts";
    public static final String IGNORE_FSW_OPTION_LONG = "ignoreFsw";
    public static final String IGNORE_SSE_OPTION_LONG = "ignoreSse";
    public static final String IGNORE_CFDP_INDICATION_OPTION_LONG = "ignoreCfdpIndications";
    public static final String MTAK_PORT_OPTION_LONG = "mtakDownlinkPort";
    public static final String CHANNEL_IDS_OPTION_LONG = "channelIds";
    public static final String MODULES_OPTION_LONG = "modules";
    public static final String SUBSYSTEMS_OPTION_LONG = "subsystems";
    public static final String OPS_CATEGORIES_OPTION_LONG = "opsCategories";
    
    private final Tracer log;  
    /*
	 * The reserved options for database host/port and jms host/port currently conflict, so to prevent problems
	 * we need to make our own jms host/port options locally that don't conflict.
	 * 
	 */
	public static final Option JMS_HOST = new MpcsOption(null,"jmsHost",true,"hostname","Host where the JMS message server is running.");
	public static final Option JMS_PORT = new MpcsOption(null,"jmsPort",true,"port","Port on which the JMS message server is listening.");
    
    private static final StringBuilder messageText = new StringBuilder(4096);
    
    private final List<ITopicSubscriber> fswSubscribers = new LinkedList<>();
    private final List<ITopicSubscriber> sseSubscribers = new LinkedList<>();    
    
    protected boolean fetchLad;
    
    protected boolean receiveEha;
    protected boolean receiveEvrs;
    protected boolean receiveProducts;
    protected boolean receiveFsw;
    protected boolean receiveSse;
    protected boolean receiveCfdpIndications;
    private final Set<IMessageType> messageFilters;
    
    private final ChannelValueFilter filter;
    private Socket clientSocket;
    private OutputStream outputStream;
    protected int downlinkPort;
    
    private final ApplicationContext appContext;

    
    private volatile boolean messageHandlerInterrupted = false;
    
    /** The ladFetcher thread is used to asynchronously query the global LAD server once the host and port
     * are known.
     */
    // Use optional because this is lazily initialized
    private volatile Optional<Thread> ladFetcher = Optional.empty();
   
    /** The telemetrySender consumes from the pending messages queue and forwards to the client of this proxy */
    // Use optional because this is lazily initialized
    private volatile Optional<Thread> telemetrySender = Optional.empty();
    
    // Messages arrays put on this queue will be sent as a batch to this application's client
    protected final BlockingQueue<Collection<? extends EscapedCsvSupport>> pendingMessages
    			= new LinkedBlockingQueue<>(100);

    // DSS-ID list based on session DSS-ID to use
    // to query the Global LAD
    private Set<Integer> dssIdSet;
    private final IExternalMessageUtility externalMessageUtil;
    
    public MtakDownlinkServerApp()
    {
    	appContext = SpringContextFactory.getSpringContext(true);
        log = TraceManager.getDefaultTracer(appContext);
    	ReservedOptions.setApplicationContext(appContext);
    	
    	fetchLad = false;
    	receiveEha = true;
    	receiveEvrs = true;
    	receiveProducts = true;
    	receiveFsw = true;
    	receiveSse = true;
        receiveCfdpIndications = true;

        messageFilters = new HashSet<>();
        messageFilters.add(SessionMessageType.SessionHeartbeat);
        messageFilters.add(SessionMessageType.StartOfSession);
        messageFilters.add(SessionMessageType.EndOfSession);

        filter = new ChannelValueFilter();
        
        clientSocket = null;
        outputStream = null;
        downlinkPort = HostPortUtility.UNDEFINED_PORT;
        
        externalMessageUtil = appContext.getBean(IExternalMessageUtility.class);

    }
    
    @Override
    public void exitCleanly() {
        shutdown();
    }

    public boolean init()
    {
    	       
        try
        {
            if(receiveFsw)
            {
                addFswSubscribers(ContextTopicNameFactory.getMissionSessionTopic(appContext));           	
            }

            if (receiveSse && (appContext.getBean(SseContextFlag.class).isApplicationSse()
                    || appContext.getBean(MissionProperties.class).missionHasSse()))
            {
                addSseSubscribers(ContextTopicNameFactory.getSseSessionTopic(appContext));
            }
        } 
        catch (final Exception e)
        {
            e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    private void addSseSubscribers(final String rootTopic) throws MessageServiceException {
        
        final IMessageClientFactory clientFactory = appContext.getBean(IMessageClientFactory.class);
        
        final String topicFilter = MessageFilterMaker.createFilterForSpacecraft(appContext.getBean(IContextIdentification.class).getSpacecraftId());
        ITopicSubscriber sseSubscriber = clientFactory.getTopicSubscriber(rootTopic, topicFilter, true);
        this.sseSubscribers.add(sseSubscriber);
        
        if (this.receiveEha) {
            sseSubscriber = clientFactory.getTopicSubscriber(TopicNameToken.APPLICATION_SSE_EHA.getApplicationDataTopic(rootTopic), topicFilter, true);
            this.sseSubscribers.add(sseSubscriber);
        }
        if (this.receiveEvrs) {
            sseSubscriber = clientFactory.getTopicSubscriber(TopicNameToken.APPLICATION_SSE_EVR.getApplicationDataTopic(rootTopic), topicFilter, true);
            this.sseSubscribers.add(sseSubscriber);
        }
        
        for (final ITopicSubscriber sub : sseSubscribers) {
            sub.setMessageListener(this);
        }
    }
    
    private void addFswSubscribers(final String rootTopic) throws MessageServiceException {
        
        final IMessageClientFactory clientFactory = appContext.getBean(IMessageClientFactory.class);
        
        final String topicFilter = MessageFilterMaker.createFilterFromContext(appContext);
        ITopicSubscriber fswSubscriber = clientFactory.getTopicSubscriber(rootTopic, topicFilter, true);
        this.fswSubscribers.add(fswSubscriber);
        
        if (this.receiveEha) {
            fswSubscriber = clientFactory.getTopicSubscriber(TopicNameToken.APPLICATION_EHA.getApplicationDataTopic(rootTopic), topicFilter, true);
            this.fswSubscribers.add(fswSubscriber);
        }
        if (this.receiveEvrs) {
            fswSubscriber = clientFactory.getTopicSubscriber(TopicNameToken.APPLICATION_EVR.getApplicationDataTopic(rootTopic), topicFilter, true);
            this.fswSubscribers.add(fswSubscriber);
        }
        
        if (this.receiveProducts) {
            fswSubscriber = clientFactory.getTopicSubscriber(TopicNameToken.APPLICATION_PRODUCT.getApplicationDataTopic(rootTopic), topicFilter, true);
            this.fswSubscribers.add(fswSubscriber);
        }

        if (this.receiveCfdpIndications) {
            fswSubscriber = clientFactory.getTopicSubscriber(TopicNameToken.APPLICATION_CFDP.getApplicationDataTopic(rootTopic), topicFilter, true);
            this.fswSubscribers.add(fswSubscriber);
        }

        for (final ITopicSubscriber sub : fswSubscribers) {
            sub.setMessageListener(this);
        }
    }

    @Override
    public void configure(final CommandLine commandLine) throws ParseException
    {
    	super.configure(commandLine);
    	
    	ReservedOptions.parseDatabaseHost(commandLine,false);
        ReservedOptions.parseDatabasePort(commandLine,false);
        ReservedOptions.parseDatabaseUsername(commandLine,false);
        ReservedOptions.parseDatabasePassword(commandLine,false);
        
    	ReservedOptions.parseSessionKey(commandLine,true);
    	
        parseJmsHost(commandLine,false);
        parseJmsPort(commandLine,false);
        
        if(!commandLine.hasOption(MTAK_PORT_OPTION_LONG))
        {
        	throw new ParseException("The required option --" + MTAK_PORT_OPTION_LONG + " was not specified.");
        }
        
        final String portStr = commandLine.getOptionValue(MTAK_PORT_OPTION_LONG);
        try
        {
        	downlinkPort = GDR.parse_int(portStr);
        }
        catch(final Exception e)
        {
        	throw new ParseException("Invalid input value \"" + portStr + "\" specified for the MTAK downlink port." + 
        			" Make sure this value is a valid numeric port number.");
        }
        
        receiveFsw = !commandLine.hasOption(IGNORE_FSW_OPTION_LONG);
        receiveSse = !commandLine.hasOption(IGNORE_SSE_OPTION_LONG);
        receiveEha = !commandLine.hasOption(IGNORE_EHA_OPTION_LONG);
        receiveEvrs = !commandLine.hasOption(IGNORE_EVRS_OPTION_LONG);
        receiveProducts = !commandLine.hasOption(IGNORE_PRODUCTS_OPTION_LONG);
        receiveCfdpIndications = !commandLine.hasOption(IGNORE_CFDP_INDICATION_OPTION_LONG);
        fetchLad = commandLine.hasOption(FETCH_LAD_OPTION_LONG);
      
        if(receiveEha)
        {
        	messageFilters.add(EhaMessageType.AlarmedEhaChannel);
        	filter.addSource(ChannelDefinitionType.FSW);
        }
        if(receiveEvrs)
        {
        	messageFilters.add(EvrMessageType.Evr);
        }
        if(receiveProducts)
        {
        	messageFilters.add(ProductMessageType.PartialProduct);
        	messageFilters.add(ProductMessageType.ProductAssembled);
        }
        if(receiveCfdpIndications)
        {
            messageFilters.add(CfdpMessageType.CfdpIndication);
        }

        // There is no command line for sources of telemetry other than
        // fsw and sse.  Add the unimplemented sources, since a non-empty
        // source filter will reject everything not in it.
        filter.addSource(ChannelDefinitionType.H);
        filter.addSource(ChannelDefinitionType.M);
        if (receiveFsw) {
        	filter.addSource(ChannelDefinitionType.FSW);
        }
        
        if (receiveSse) {
        	filter.addSource(ChannelDefinitionType.SSE);
        }
        
        // parse and store the channel id list from the command line (if it exists)
        if(commandLine.hasOption(CHANNEL_IDS_OPTION_LONG))
        {        	
            final String channelIdCsv = commandLine.getOptionValue(CHANNEL_IDS_OPTION_LONG);
            if (channelIdCsv == null)
            {
                throw new MissingArgumentException("--" + CHANNEL_IDS_OPTION_LONG + " requires a command line value");
            }
            try
			{
				filter.addChannelIds(channelIdCsv);
			}
			catch(final ChannelListRangeException e)
			{
				throw new ParseException("Malformed Channel ID list: " + e.getMessage());
			}
        }
        
        // parse and store the modules list from the command line (if it exists)
        if(commandLine.hasOption(MODULES_OPTION_LONG))
        {
            final String modulesCsv = commandLine.getOptionValue(MODULES_OPTION_LONG);
            if (modulesCsv == null)
            {
                throw new MissingArgumentException("--" + MODULES_OPTION_LONG + " requires a command line value");
            }
            filter.addModules(modulesCsv);
        }
        
        // parse and store the subsystems list from the command line (if it exists)
        if(commandLine.hasOption(SUBSYSTEMS_OPTION_LONG))
        {
            final String subsystemsCsv = commandLine.getOptionValue(SUBSYSTEMS_OPTION_LONG);
            if (subsystemsCsv == null)
            {
                throw new MissingArgumentException("--" + SUBSYSTEMS_OPTION_LONG + " requires a command line value");
            }
            filter.addSubsystems(subsystemsCsv);
        }
        
        // parse and store the ops categories list from the command line (if it exists)
        if(commandLine.hasOption(OPS_CATEGORIES_OPTION_LONG))
        {
            final String opsCategoriesCsv = commandLine.getOptionValue(OPS_CATEGORIES_OPTION_LONG);
            if (opsCategoriesCsv == null)
            {
                throw new MissingArgumentException("--" + OPS_CATEGORIES_OPTION_LONG + " requires a command line value");
            }
            filter.addOpsCategories(opsCategoriesCsv);
        } 
        
        final IVenueConfiguration venueConfig = appContext.getBean(IVenueConfiguration.class);
        
        appContext.getBean(IConnectionMap.class).setDefaultNetworkValuesForVenue(venueConfig.getVenueType(), 
                                                                                 venueConfig.getTestbedName(),
                                                                                 venueConfig.getDownlinkStreamId(),
                                                                                 false, true);
        
        TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.ERROR);
        

        try
		{
        	final boolean doSse = !appContext.getBean(IVenueConfiguration.class).getVenueType().isOpsVenue() && 
        			appContext.getBean(MissionProperties.class).missionHasSse() && 
                    !appContext.getBean(SseContextFlag.class).isApplicationSse();

        	if (doSse) {
        		appContext.getBean(SseDictionaryLoadingStrategy.class)
        		.enableChannel()
        		.enableAlarm()
        		.enableEvr();
        	}

        	appContext.getBean(FlightDictionaryLoadingStrategy.class)
        	.enableChannel()
        	.enableAlarm()
        	.enableEvr()
        	.loadAllEnabled(appContext, false);
		}
		catch(final DictionaryException e)
		{
			throw new ParseException("Could not parse EHA dictionary: " + e.getMessage());
		}
        TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.INFO);

		final Integer dssId = appContext.getBean(IContextFilterInformation.class).getDssId();

		if (dssId != null) {
		    dssIdSet = new HashSet<>();
		    dssIdSet.add(dssId);
		}
    }

    @Override
    public Options createOptions()
    {
        final Options options = super.createOptions();

        options.addOption(null, FETCH_LAD_OPTION_LONG, false,"Force MTAK to fetch the LAD and dump it upon startup.");
        options.addOption(null, IGNORE_EHA_OPTION_LONG, false,"Force MTAK to ignore EHA channel messages.");
        options.addOption(null, IGNORE_EVRS_OPTION_LONG, false,"Force MTAK to ignore EVR messages.");
        options.addOption(null, IGNORE_PRODUCTS_OPTION_LONG, false,"Force MTAK to ignore product messages.");
        options.addOption(null, IGNORE_CFDP_INDICATION_OPTION_LONG, false,"Force MTAK to ignore CFDP Indication messages.");
        options.addOption(null, IGNORE_FSW_OPTION_LONG, false, "Force MTAK to ignore FSW telemetry.");
        options.addOption(null, IGNORE_SSE_OPTION_LONG, false, "Force MTAK to ignore SSE telemetry.");
        options.addOption(null, MTAK_PORT_OPTION_LONG, true, "Set the port that this app will attempt to connect to in order to deliver telemetry.");
        options.addOption(null, CHANNEL_IDS_OPTION_LONG, true, "Force MTAK to only process the channels in the supplied list.");
        options.addOption(null, SUBSYSTEMS_OPTION_LONG, true, "Force MTAK to only process telemtry belonging to the supplied list of subsystems.");
        options.addOption(null, MODULES_OPTION_LONG, true, "Force MTAK to only process telemtry belonging to the supplied list of modules.");
        options.addOption(null, OPS_CATEGORIES_OPTION_LONG, true, "Force MTAK to only process telemtry belonging to the supplied list of operational categories.");

        options.addOption(ReservedOptions.SESSION_KEY);
        options.addOption(JMS_HOST);
        options.addOption(JMS_PORT);
        options.addOption(ReservedOptions.DATABASE_HOST);
        options.addOption(ReservedOptions.DATABASE_PORT);
        options.addOption(ReservedOptions.DATABASE_USERNAME);
        options.addOption(ReservedOptions.DATABASE_PASSWORD);

        return options;
    }
    
    public void start()
    {
    	boolean connected = false;
    	while(!connected)
    	{
    		try
			{
				clientSocket = new Socket(HostPortUtility.LOCALHOST,downlinkPort);
				outputStream = new DataOutputStream(clientSocket.getOutputStream());
				connected = true;
				
			}
			catch(final Exception e)
			{
				SleepUtilities.checkedSleep(500);
			}
    	}
    	
    	final MtakTelemetrySender senderTask = new MtakTelemetrySender(pendingMessages, outputStream);
    	telemetrySender = Optional.of(new Thread(senderTask, "MTAK-telemetry-sender"));
    	telemetrySender.get().start();
    	
    	for (final ITopicSubscriber sub : fswSubscribers) {
    		try {
                sub.start();
            } catch (final MessageServiceException e) {
                System.err.println("Unexpected problem starting MTAK message subscriber for topic " + sub.getTopic() +
                        " " + ExceptionTools.getMessage(e));
            }
    	}
    	
    	for (final ITopicSubscriber sub : sseSubscribers) {
            try {
                sub.start();
            } catch (final MessageServiceException e) {
                System.err.println("Unexpected problem starting MTAK message subscriber for topic " + sub.getTopic() +
                        " " + ExceptionTools.getMessage(e));
            }
        }
    }
    
    @Override
	public synchronized void onMessage(final IExternalMessage m)
    {
        try
        {       	
        	final IMessageType type = externalMessageUtil.getInternalType(m);          
        	if(messageHandlerInterrupted || !messageFilters.contains(type)) {
                return;
            }

        	final jpl.gds.shared.message.IMessage[] messages = externalMessageUtil.instantiateMessages(m);

        	if (fetchLad) {
        		ServiceConfiguration serviceConfig = null;

        		if (messages.length > 0) {
                    if (messages[0].isType(SessionMessageType.StartOfSession)) {
        				serviceConfig =((StartOfSessionMessage) messages[0]).getServiceConfiguration();
                    }
                    else if (messages[0].isType(SessionMessageType.SessionHeartbeat)) {
                        serviceConfig = ((IContextHeartbeatMessage) messages[0]).getServiceConfiguration();
        			}

        			 if (serviceConfig != null) {
        				 GlobalLadProperties.getGlobalInstance().setGlobalLadHost(serviceConfig.getService(ServiceType.GLAD).getHost());
        				 GlobalLadProperties.getGlobalInstance().setGlobalLadRestServerPort(serviceConfig.getService(ServiceType.GLAD).getPort());
        			 }

        			 fetchLad = false; 
        			 triggerLadFetch();
        		}
        	}

            final List<EscapedCsvSupport> messagesToSend = new ArrayList<>(messages.length);
        	// If there is a need to apply a message filter (Currently only applying it to EHA channel messages):
            if (IMessageType.matches(EhaMessageType.AlarmedEhaChannel, type))
            {
            	for(final jpl.gds.shared.message.IMessage msg : messages)
            	{            		                		
        			if(filter.accept(((IAlarmedChannelValueMessage)msg).getChannelValue()))
        			{
        				messagesToSend.add((IAlarmedChannelValueMessage) msg);
        			}
            	}
        	} else if (IMessageType.matches(CfdpMessageType.CfdpIndication, type)) {
                for(final jpl.gds.shared.message.IMessage msg : messages)
                {
                    final ECfdpIndicationType indicationType = ((ICfdpIndicationMessage) msg).getIndicationType();

                    if(indicationType == TRANSACTION
                            || indicationType == NEW_TRANSACTION_DETECTED
                            || indicationType == FAULT
                            || indicationType == TRANSACTION_FINISHED
                            || indicationType == ABANDONED)
                    {
                        messagesToSend.add((EscapedCsvSupport) msg);
                    }
                }

            } else {
            	// The only subscribed messages must be EscapedCsvSupports
            	for (int i = 0; i < messages.length; i++) {
            		messagesToSend.add((EscapedCsvSupport) messages[i]);
            	}
        	}
            
            if (!messagesToSend.isEmpty()) {
            	try {
            		// This thread is controlled by the JMS library, so it is not clear that an interrupt can
            		// be used.  To be safe, timeout and check if the handler should stop operations
            		boolean success = false;
            		while (!messageHandlerInterrupted && !success) {
            			success = pendingMessages.offer(messagesToSend, 100, TimeUnit.MILLISECONDS);
            		}
            	} catch (final InterruptedException e) {
            		messageHandlerInterrupted = true;
            		Thread.currentThread().interrupt();
            	}
            }
        }
        catch (final Exception e)
        {
 		   System.err.println("MTAK downlink error: " + e.getMessage());
        }
    }
    

    private class LadFetcher implements Runnable {
    	/** This class encapsulates the task of querying the global LAD server for both channels and EVRs.
    	 * This task can be cancelled if run on a separate thread by calling interrupt() on the LadFetcher's thread.
    	 * The LAD query will be initialized from the global SessionConfiguration, so make sure it is set before calling run().
    	 */
    	private final BlockingQueue<Collection<? extends EscapedCsvSupport>> outputQueue;
    	private final boolean fetchEha;
    	private final boolean fetchEvrs;
    	private final Set<Integer> dssIdSet;
    	private final ChannelValueFilter filter;
		private final ApplicationContext appContext;
    	
    	/**
    	 * Construct a new LadFetcher task.
    	 * @param outputQueue to publish telemetry to
    	 * @param fetchEvrs if true, query EVRs from the LAD server
    	 * @param fetchEha if true, query EHA from the LAD server
    	 * @param dssIdSet if dssIds to filter for.  If empty, accepts telemetry for all dssIds
    	 * @param filter filter controlling what channel values to exclude from output.
    	 */
    	public LadFetcher(final BlockingQueue<Collection<? extends EscapedCsvSupport>> outputQueue, final boolean fetchEvrs,
    					  final boolean fetchEha, final Set<Integer> dssIdSet, final ChannelValueFilter filter, final ApplicationContext appContext) {
    		this.outputQueue = outputQueue;
    		this.fetchEvrs = fetchEvrs;
    		this.fetchEha = fetchEha;
    		this.dssIdSet = dssIdSet;
    		this.filter = filter;
    		this.appContext = appContext;
    	}
    	
    	/** Execute query against the global LAD as configured in the singleton GdsConfiguration object.
    	 *  Results are published to the outputQueue for this object.
    	 */
    	@Override
    	public void run()
    	{

    		Collection<IAlarmedChannelValueMessage> rtLadChannels = Collections.emptyList();
    		Collection<IAlarmedChannelValueMessage> recLadChannels = Collections.emptyList();
    		Collection<IEvrMessage> rtLadEvrs = Collections.emptyList();
    		Collection<IEvrMessage> recLadEvrs = Collections.emptyList();
    		try
    		{
    			final IContextKey idObj = appContext.getBean(IContextKey.class);
    			
    			final CoreGlobalLadQuery query = (CoreGlobalLadQuery) appContext.getBean(ICoreGlobalLadQuery.class); 
    			final GlobalLadQueryParamsBuilder builder = GlobalLadQueryParams.createBuilder()
    					.setSource(DataSource.all)
    					.setRecordedState(RecordedState.realtime)
    					.setTimeType(GlobalLadPrimaryTime.EVENT)
    					.setSessionId(idObj.getNumber())
    					.setHostRegex(idObj.getHost())
    					.setVenueRegex(appContext.getBean(IVenueConfiguration.class).getVenueType().toString())
    					.setDssIds(dssIdSet)
    					.setScid(appContext.getBean(IContextIdentification.class).getSpacecraftId());

    			if (fetchEvrs) {
                    log.trace("LAD Fetching EVR...");
    				rtLadEvrs = query.convertEvrsToMessages(query.evrLadQuery(builder));
    				builder.setRecordedState(RecordedState.recorded);
    				recLadEvrs = query.convertEvrsToMessages(query.evrLadQuery(builder));
    				// Reset the original recorded state since we are using the same builder for EHA
    				builder.setRecordedState(RecordedState.realtime);

                    if (log.isEnabledFor(TraceSeverity.TRACE)) {
                        log.trace("Realtime LAD EVR ", rtLadEvrs);
                        log.trace("Recorded LAD EVR ", recLadEvrs);
                    }
    			}

    			if (fetchEha) {
                    log.trace("LAD Fetching EHA...");
    				// Limit the number of channel values coming back - otherwise, this process can be
    				// overwhelmed
    				final IEhaMessageFactory mf = appContext.getBean(IEhaMessageFactory.class);
    				builder.setMaxResults(1);
    				rtLadChannels = query.convertEhaToMessages(query.ehaLadQuery(builder), mf);
    				builder.setRecordedState(RecordedState.recorded);
    				recLadChannels = query.convertEhaToMessages(query.ehaLadQuery(builder), mf);
                    if (log.isEnabledFor(TraceSeverity.TRACE)) {
                        log.trace("Realtime LAD EHA ", rtLadChannels);
                        log.trace("Recorded LAD EHA ", recLadChannels);
                    }

    			}
    		}
    		catch (final Exception e) {
    			log.error("MTAK could not fetch from the Global LAD: " + e.toString());
    			return;
    		}

    		try {
    			sendChanValsToMtak(rtLadChannels);
    			sendChanValsToMtak(recLadChannels);
    			sendEvrsToMtak(rtLadEvrs);
    			sendEvrsToMtak(recLadEvrs);
    		} catch(final InterruptedException e) {
    			System.err.println("Interrupted while sending LAD data to MTAK client. Not all data may be available");
    		}
    	}


    	private void sendChanValsToMtak(final Collection<IAlarmedChannelValueMessage> rtLadChannels) throws InterruptedException {
            //We have to make sure the retrieval of the LAD goes through the same filtering process that
            //all the realtime channels go through (don't deliver the user what they don't want/need). The
            //easiest way to do this is to just put all the desired values into fake EhaChannelMessages and
            //just reuse the code used during the normal message flow.
    		final List<IAlarmedChannelValueMessage> filteredValues = new ArrayList<>(rtLadChannels.size());
    		for(final IAlarmedChannelValueMessage message : rtLadChannels)
    		{
    			if(filter.accept(message.getChannelValue())) {
    				filteredValues.add(message);
    			}
    		}

    		outputQueue.put(filteredValues);
    	}


    	private void sendEvrsToMtak(final Collection<IEvrMessage> evrMessages) throws InterruptedException {
    		outputQueue.put(evrMessages);
    	}

    }


    private static class MtakTelemetrySender implements Runnable {

    	/** This class implements a long running task that will consume telemetry as Messages and will
    	 * write them to a socket.  This task can be cancelled if run on a separate thread by
    	 * interrupting its thread and closing its underlying OutputStream.
    	 */
    	
    	private final BlockingQueue<Collection<? extends EscapedCsvSupport>> inputQueue;
    	private final OutputStream outputStream;
    	
    	/** Initialize a task that will send MTAK output CSVs to the provided output stream on behalf of some producer.
    	 * 
    	 * @param inputQueue - queue of messages that will be converted and written to the output stream.
    	 * @param outputStream to send MTAK CSVs to.  Opening and closing assumed to be managed by the caller.
    	 */
    	public MtakTelemetrySender(final BlockingQueue<Collection<? extends EscapedCsvSupport>> inputQueue, final OutputStream outputStream) {
    		this.inputQueue = inputQueue;
    		this.outputStream = outputStream;
    	}

    	/** Execution loop, in which the sender polls its inputQueue and publishes messages to its outputStream. */
    	@Override
        public void run() {
    		boolean cancelled = false;
    		while(!cancelled) {
    			try {
    				final Collection<? extends EscapedCsvSupport> nextBatch = inputQueue.take();
    				sendMessagesToMtak(nextBatch);
    			} catch (final InterruptedException e) {
    				cancelled = true;
    				Thread.currentThread().interrupt();
    			} catch (final IOException e) {
    				System.err.println("MTAK output stream closed.  No more telemetry will be sent.");
    				cancelled = true;
    			}
    		}
    	}

    	private void sendMessagesToMtak(final Collection<? extends EscapedCsvSupport> nextBatch) throws IOException {
    		if(nextBatch.isEmpty())
    		{
    			return;
    		}

    		messageText.setLength(0);

    		try {
    			for (final EscapedCsvSupport m : nextBatch) {
    				messageText.append(m.getEscapedCsv());
    				messageText.append("\n");
    			}
    			final byte[] bytes = messageText.toString().getBytes();
    			outputStream.write(bytes,0,bytes.length);
    		} catch (final IOException e) {
    			// Rethrow so caller knows
    			throw e;
    		} catch (final Exception e) {
    			System.err.println("MTAK downlink error: " + e.getMessage());
    		}
    	}

    }
    
   private void shutdown()
   {
       // Intrinsic lock should not be held when setting messageHandlerInterrupted.
       // Split synchronization into two separate blocks
	   ladFetcher.ifPresent(x -> x.interrupt());
	   telemetrySender.ifPresent(x -> x.interrupt());

	   // Don't even bother checking if these are null...the NullPointerException will just get
	   // caught by the "catch" statements anyway (brn)
	   
	   try {
		   outputStream.close();
	   }
	   catch(final Exception e)
	   {
		   //don't care
	   }

	   try
	   {
		   clientSocket.close();
	   }
	   catch(final Exception e)
	   {
		   //don't care
	   }

	   messageHandlerInterrupted = true;
	   synchronized(this) {
		   try
		   {
		       for (final ITopicSubscriber sub: fswSubscribers) {
		           sub.close();
		       }
		   }
		   catch(final Exception e)
		   {
			   //don't care
		   }

		   try
		   {
		       for (final ITopicSubscriber sub: sseSubscribers) {
                   sub.close();
               }
		   }
		   catch(final Exception e)
		   {
			   //don't care
		   }
	   }
   }
    
   public void parseJmsHost(final CommandLine commandLine,final boolean required) throws ParseException
   {
	   	if(commandLine.hasOption(JMS_HOST.getLongOpt()))
	   	{
			final String jmsHost = commandLine.getOptionValue(JMS_HOST.getLongOpt());
			if(jmsHost == null)
			{
				throw new ParseException("You must supply a value for the JMS host option.");
			}
			
			appContext.getBean(MessageServiceConfiguration.class).setMessageServerHost(jmsHost);
   		}
   		else if(required)
   		{
   			throw new MissingOptionException("You must supply the JMS host.");
   		}
   }
   
   public void parseJmsPort(final CommandLine commandLine,final boolean required) throws ParseException
   {
	   if (commandLine.hasOption(JMS_PORT.getLongOpt()))
	   {
		   final String jmsPortStr = commandLine.getOptionValue(JMS_PORT.getLongOpt());
		   if(jmsPortStr == null)
		   {
		   	throw new ParseException("You must supply a value for the JMS port option.");
		   }
		   
		   try
		   {
		       final int jmsPort = Integer.parseInt(jmsPortStr);
		       appContext.getBean(MessageServiceConfiguration.class).setMessageServerPort(jmsPort);
		   }
		   catch (final NumberFormatException e)
		   {
		       throw new ParseException("JMS port number must be an integer");
		   }
       }
	   else if(required)
	   {
		   throw new MissingOptionException("You must supply the JMS port.");
       }
   }

   public void triggerLadFetch()
   {
	   final LadFetcher fetchTask = new LadFetcher(pendingMessages, receiveEvrs, receiveEha, dssIdSet, filter, appContext);
	   ladFetcher = Optional.of(new Thread(fetchTask, "MTAK-LAD-fetcher"));
       ladFetcher.get().start();
   }

   public static void main(final String[] args)
   {
       final MtakDownlinkServerApp monitor = new MtakDownlinkServerApp();
       try
       {
           final CommandLine commandLine = ReservedOptions.parseCommandLine(args, monitor);
           monitor.configure(commandLine);
       }
        catch (final Exception e)
       {
           System.err.println(ExceptionTools.getMessage(e));
           System.exit(1);
       }
       
       final boolean ok = monitor.init();
       if (!ok)
       {
           System.exit(1);
       }
       
       monitor.start();
   }
 
}
