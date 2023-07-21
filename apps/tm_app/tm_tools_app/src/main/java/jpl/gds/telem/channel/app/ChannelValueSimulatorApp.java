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
package jpl.gds.telem.channel.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.options.SpacecraftIdOption;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.client.alarm.IAlarmDictionaryManager;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.eha.api.channel.IChannelLad;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.message.IChannelValueMessage;
import jpl.gds.eha.api.service.alarm.IAlarmPublisherService;
import jpl.gds.eha.api.service.channel.IChannelLadService;
import jpl.gds.eha.api.service.channel.IChannelPublisherUtility;
import jpl.gds.eha.api.service.channel.IPrechannelizedPublisherService;
import jpl.gds.message.api.options.MockSessionTopicCommandOptions;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.ChannelOption;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.time.AccurateDateTimeOption;
import jpl.gds.shared.cli.options.time.DateOption;
import jpl.gds.shared.cli.options.time.SclkOption;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTime;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.util.BinOctHexUtility;

/**
 * ChannelValueSimulatorApp is a test tool to simulate data for derived channels
 * and alarm testing. Data is fed in to seed the lad and simulate incoming data.
 * Resulting LAD is printed to STDOUT and optionally published to the message service.
 * 
 * @since AMPCS R4
 */
public class ChannelValueSimulatorApp extends AbstractCommandLineApp {
	private final Tracer logger;

	/* FLAGS */

	private static final String SHORT_IS_RECORDED_OPTION = "R";
	static final String LONG_IS_RECORDED_OPTION = "isRecorded";

	private static final String SHORT_USE_TRIGGERS_OPTION = "T";
	static final String LONG_USE_TRIGGERS_OPTION = "useTriggers";

	private static final String SHORT_PUBLISH_TO_JMS_OPTION = "J";
	static final String LONG_PUBLISH_TO_JMS_OPTION = "publishToJms";

	/* TIMES OPTIONS */

	private static final String SHORT_RCT_OPTION = "r";
	static final String LONG_RCT_OPTION = "rct";

	private static final String SHORT_ERT_OPTION = "e";
	static final String LONG_ERT_OPTION = "ert";

	private static final String SHORT_SCET_OPTION = "s";
	static final String LONG_SCET_OPTION = "scet";

	private static final String SHORT_SCLK_OPTION = "S";
	static final String LONG_SCLK_OPTION = "sclk";

	/* DATA OPTIONS */

	private static final String SHORT_CHANNELS_OPTION = "c";
	static final String LONG_CHANNELS_OPTION = "channels";

	static final String LONG_VALUES_OPTION = "value";

	private static final String SHORT_LAD_CHANNELS_OPTION = "l";
	static final String LONG_LAD_CHANNELS_OPTION = "ladChannels";

	private static final String SHORT_PACKET_CHANNELS_OPTION = "p";
	static final String LONG_PACKET_CHANNELS_OPTION = "packetChannels";

	/* DEBUG OPTION */
	static final String LONG_DEBUG_MODE_OPTION = "debug";

	/* DESCRIPTIONS */

	private static final String DESCRIPTION_IS_RECORDED = "If option is provided, the channel values "
			+ "will be marked as recorded, otherwise it will be marked as realtime";

	private static final String DESCRIPTION_USE_TRIGGERS = "If option is provided, "
			+ "then channel derivations will be initiated by trigger channels";

	private static final String DESCRIPTION_PUBLISH_TO_JMS = "If option is provided, channel messages will be published to the JMS "
			+ "and the resulting LAD printed to STDOUT. Otherwise, only the resulting LAD will be printed to STDOUT.";

	private static final String DESCRIPTION_RCT = "The record creation time to mark each channel value with";
	private static final String DESCRIPTION_ERT = "The earth received time to mark each channel value with";
	private static final String DESCRIPTION_SCET = "The spacecraft event time to mark each channel value with";
	private static final String DESCRIPTION_SCLK = "The spacecraft clock to mark each channel value with";

	private static final String DESCRIPTION_CHANNELS = "A comma separated list of channel IDs to fill the LAD with. "
			+ "These channels will be filled with the value provided to the --"
			+ LONG_VALUES_OPTION
			+ " option, or with 1 if the "
			+ "--"
			+ LONG_VALUES_OPTION
			+ " is not specified. These channels are NOT treated as channels coming from an incoming "
			+ "packet (i.e. it won't trigger derivations)";

	private static final String DESCRIPTION_VALUE = "The value to use to fill the channels specified by the --"
			+ LONG_CHANNELS_OPTION + " option";

	private static final String DESCRIPTION_LAD_CHANNELS = "The path to a file containing a list of [channel ID]=[channel value] "
			+ "pairs to use to seed the LAD. Each entry must be on a new line.  These values will overwrite any values specified by the --"
			+ LONG_CHANNELS_OPTION + " option";

	private static final String DESCRIPTION_PACKET_CHANNELS = "The path to a file containing a list of [channel ID]=[channel value] "
			+ "pairs to use to simulate channels in an incoming packet. Each entry must be on a new line.";

	private static final String DESCRIPTION_DEBUG_MODE = "Debug mode";
	
	private static final String SCID_LONG = SpacecraftIdOption.LONG_OPTION;

	private boolean isRecorded;
	private boolean useTriggers;
	private boolean publishToJms;

	private IAccurateDateTime rct;
	private IAccurateDateTime ert;
	private IAccurateDateTime scet;
	private ISclk sclk;

	private final Map<String, String> ladChannelMap;
	private final Map<String, String> packetChannelMap;

    private final MockSessionTopicCommandOptions topicOptions;
    private String pubTopic;
    private UnsignedInteger vcid;
    private UnsignedInteger dssId;
    private UnsignedInteger scid;	
	private final DictionaryProperties dictConfig;
	private final DictionaryCommandOptions dictOptions;
    private final ContextCommandOptions contextOptions;
    private SpacecraftIdOption scidOption;

	private boolean debugMode;

	private final IChannelUtilityDictionaryManager chanDefTable;
	private final IAlarmDictionaryManager alarmDefTable;
    
    private IChannelLad channelLad;
	private IService channelLadService;
    private IService ehaPubService;
    private IChannelPublisherUtility pubUtil;
    private IService alarmPubService;
    private IMessagePortal msgPortal;
    private final ApplicationContext commonContext;
    private final IChannelValueFactory chanFactory;
   

	/**
	 * Constructor.
	 */
	public ChannelValueSimulatorApp() {
		
		commonContext =  SpringContextFactory.getSpringContext(true);
		
		topicOptions = new MockSessionTopicCommandOptions(commonContext.getBean(MissionProperties.class));
		contextOptions = new ContextCommandOptions(commonContext.getBean(IContextConfiguration.class));
	    
		this.ladChannelMap = new HashMap<>();
		this.packetChannelMap = new HashMap<>();

		this.isRecorded = true;
		this.useTriggers = false;
		this.publishToJms = false;
		
        logger = TraceManager.getTracer(commonContext, Loggers.TLM_EHA);
		dictConfig = commonContext.getBean(DictionaryProperties.class);
		dictOptions = new DictionaryCommandOptions(dictConfig);

		this.chanDefTable = commonContext.getBean(IChannelUtilityDictionaryManager.class);
		this.alarmDefTable = commonContext.getBean(IAlarmDictionaryManager.class);
		this.chanFactory = commonContext.getBean(IChannelValueFactory.class);
	}

	@Override
	public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

	    options = createOptions();

        final PrintWriter pw = new PrintWriter(System.out);
        
        pw.println("Usage: "
                + ApplicationConfiguration.getApplicationName()
                + " -p <packet-channels-data-file> |\n"
                + "-l <lad-channels-data-file> |\n"
                + "-c <channel-ids> [--value <fill-value>] "
                + "<options>");
        pw.println("                   ");

        options.getOptions().printOptions(pw);
        
        pw.flush();
   
	}


	@Override
	public void configure(final ICommandLine commandLine) throws ParseException {
	    
	    super.configure(commandLine);
	    	    
		this.debugMode = commandLine.hasOption(LONG_DEBUG_MODE_OPTION);

		this.dictOptions.parseAllOptionsAsOptionalWithDefaults(commandLine);

		final PrintStream origOut = System.out;

		if (this.debugMode) {
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.DEBUG);

		} else {
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.WARN);
			// suppress system.out so the loadDictionaryJarFiles stays quiet
			System.setOut(new PrintStream(new OutputStream() {
				@Override
				public void write(final int b) {
				    // do nothing
				}
			}));
		}

		final AmpcsUriPluginClassLoader secureLoader = this.commonContext.getBean(AmpcsUriPluginClassLoader.class);
        dictOptions.getDictionaryConfiguration()
                   .loadDictionaryJarFiles(true, secureLoader, logger);

		System.setOut(origOut);
		
		dssId = topicOptions.FILTER_DSSID.parse(commandLine);
        vcid = topicOptions.FILTER_VCID.parse(commandLine);
        scid = scidOption.parseWithDefault(commandLine, false, true);
       
		this.publishToJms = commandLine.hasOption(SHORT_PUBLISH_TO_JMS_OPTION);

		if (this.publishToJms) {
		    pubTopic = contextOptions.PUBLISH_TOPIC.parse(commandLine);
		    if (pubTopic == null) {
		        pubTopic = topicOptions.parseForFlightTopic(commandLine);
		        commonContext.getBean(IGeneralContextInformation.class).setRootPublicationTopic(pubTopic);
		        commonContext.getBean(IGeneralContextInformation.class).setTopicIsOverridden(true);
		    }		   
		}

		// isRealtime option
		this.isRecorded = commandLine.hasOption(SHORT_IS_RECORDED_OPTION);

		// useTriggers option
		this.useTriggers = commandLine.hasOption(SHORT_USE_TRIGGERS_OPTION);

        this.rct = (IAccurateDateTime) options.getOption(SHORT_RCT_OPTION).parse(commandLine);
		if (this.rct == null) {
            this.rct = new AccurateDateTime();
		}
		
		this.ert = (IAccurateDateTime) options.getOption(SHORT_ERT_OPTION).parse(commandLine);
		if (this.ert == null) {
		    this.ert = new AccurateDateTime();
		}
		
		this.sclk = (ISclk) options.getOption(SHORT_SCLK_OPTION).parse(commandLine);
		this.scet = (IAccurateDateTime) options.getOption(SHORT_SCET_OPTION).parse(commandLine);
		
		// if both sclk and scet were not provided, use default scet to get
        // sclk
        if (this.sclk == null && this.scet == null) {
        	this.scet = new AccurateDateTime();
        	this.sclk = SclkScetUtility.getSclk(this.scet, this.ert, this.scid.intValue());
        }
        // scet was provided but no sclk, so do scet->sclk conversion
        else if (this.sclk == null && this.scet != null) {
        	this.sclk = SclkScetUtility.getSclk(this.scet, this.ert, this.scid.intValue());
        }
        // sclk was provided but no scet, so do sclk->scet conversion
        else if (this.scet == null && this.sclk != null) {
        	this.scet = SclkScetUtility.getScet(this.sclk, this.ert, this.scid.intValue());
        }

		// channels option
		if (commandLine.hasOption(SHORT_CHANNELS_OPTION)) {
			String value = (String) options.getOption(LONG_VALUES_OPTION).parse(commandLine);
			if (value == null) {
			    value = "1";
			}

			@java.lang.SuppressWarnings("unchecked")
			final
            Collection<String> chanIds = (Collection<String>) options.getOption(SHORT_CHANNELS_OPTION).parse(commandLine);

			for (final String c : chanIds) {
				this.ladChannelMap.put(c, value);
			}
		}
		
		// LAD channels option
		final String ladChannelsFile = (String) options.getOption(SHORT_LAD_CHANNELS_OPTION).parse(commandLine);
		if (ladChannelsFile != null) {
			addChannels(ladChannelsFile, this.ladChannelMap);
		}

		// Packet channels option
		final String packetChannelsFile = (String) options.getOption(SHORT_PACKET_CHANNELS_OPTION).parse(commandLine);
		if (packetChannelsFile != null) {
			addChannels(packetChannelsFile, this.packetChannelMap);
		}

		if (this.ladChannelMap.size() == 0 && this.packetChannelMap.size() == 0) {
			throw new ParseException("No input channels were provided");
		}
	}

	private void addChannels(final String filePath,
			final Map<String, String> channelMap) throws ParseException {
		final File dataFile = new File(filePath.trim());

		BufferedReader br = null;

		if (dataFile.exists()) {
			try {
				br = new BufferedReader(new FileReader(dataFile));
			} catch (final FileNotFoundException e) {
				throw new ParseException(e.getMessage());
			}
		} else {
			logger.warn(String.format("File %s does not exist...ignoring",
					filePath));
			return;
		}

		String line;
		try {
			line = br.readLine();
			while (line != null && line.length() > 0) {
				if (line.charAt(0) == '#') {
					line = br.readLine();
					continue;
				}

				final String[] keyValue = line.split("=");

				if (keyValue.length < 2) {
					throw new ParseException("Invalid entry provided: " + line
							+ ". Must be in 'Channel-ID=ChannelValue' format");
				} else {
					channelMap.put(keyValue[0].trim(), keyValue[1].trim());
				}

				line = br.readLine();
			}
		} catch (final IOException e) {
			throw new ParseException(e.getMessage());
		} finally {
			try {
				br.close();
			} catch (final IOException e) {
				throw new ParseException(e.getMessage());
			}
		}
	}

	@Override
	public BaseCommandOptions createOptions() {
	    
	    if (optionsCreated.get()) {
            return options;
        }
	    
	    super.createOptions(commonContext.getBean(BaseCommandOptions.class, this));
        options.addOptions(dictOptions.getFswOptions());
        options.addOptions(topicOptions.getAllOptionsWithoutScid());
        options.addOption(contextOptions.PUBLISH_TOPIC);
        
        /* R8 Refactor - Emulate previous defaults, which made no sense before,
         * and really make no sense now.
         */
        topicOptions.FILTER_DSSID.setDefaultValue(UnsignedInteger.valueOf(1));
        topicOptions.FILTER_VCID.setDefaultValue(UnsignedInteger.valueOf(1));
        
       	options.addOption(new FlagOption(SHORT_IS_RECORDED_OPTION, LONG_IS_RECORDED_OPTION,
				DESCRIPTION_IS_RECORDED, false));
		options.addOption(new FlagOption(SHORT_USE_TRIGGERS_OPTION, LONG_USE_TRIGGERS_OPTION,
				DESCRIPTION_USE_TRIGGERS, false));
		options.addOption(new FlagOption(SHORT_PUBLISH_TO_JMS_OPTION,
				LONG_PUBLISH_TO_JMS_OPTION, DESCRIPTION_PUBLISH_TO_JMS, false));

		options.addOption(new DateOption(SHORT_RCT_OPTION, LONG_RCT_OPTION, "rct",
				DESCRIPTION_RCT, false));
		options.addOption(new AccurateDateTimeOption(SHORT_ERT_OPTION, LONG_ERT_OPTION, "ert",
				DESCRIPTION_ERT, false));
		options.addOption(new AccurateDateTimeOption(SHORT_SCET_OPTION, LONG_SCET_OPTION, "scet",
				DESCRIPTION_SCET, false));
		options.addOption(new SclkOption(TimeProperties.getInstance(), SHORT_SCLK_OPTION, LONG_SCLK_OPTION, "sclk",
				DESCRIPTION_SCLK, false));

		options.addOption(new ChannelOption(SHORT_CHANNELS_OPTION, LONG_CHANNELS_OPTION, "channel list",
				DESCRIPTION_CHANNELS, false));
		options.addOption(new StringOption(null, LONG_VALUES_OPTION, "value", DESCRIPTION_VALUE, false));
		options.addOption(new FileOption(SHORT_LAD_CHANNELS_OPTION, LONG_LAD_CHANNELS_OPTION,
				"filename", DESCRIPTION_LAD_CHANNELS, false, true));
		options.addOption(new FileOption(SHORT_PACKET_CHANNELS_OPTION,
				LONG_PACKET_CHANNELS_OPTION, "filename", DESCRIPTION_PACKET_CHANNELS, false, true));

		options.addOption(new FlagOption(null, LONG_DEBUG_MODE_OPTION,
				DESCRIPTION_DEBUG_MODE, false));
		
		scidOption = new SpacecraftIdOption(null, SCID_LONG, commonContext.getBean(MissionProperties.class), false, null);
		options.addOption(scidOption);

		return options;
	}

	/**
	 * Executes the main application logic
	 * 
	 * @throws Exception if an unrecoverable error occurs
	 */
	public void run() throws Exception {

		List<IServiceChannelValue> ehaList = new ArrayList<>();

		if (this.ladChannelMap.size() == 0 && this.packetChannelMap.size() == 0) {
			throw new IllegalStateException("No input channels provided");
		}

		Set<Entry<String, String>> iterItems = this.ladChannelMap.entrySet();

		for (final Entry<String, String> keyValue : iterItems) {
			final String key = keyValue.getKey();
			final String value = keyValue.getValue();

			ehaList.add(getChannelValue(key, value));
		}

		String streamId = pubUtil.genStreamId("");

		if (!ehaList.isEmpty()) {
			logger.info(String.format("Seeding LAD with %d channel%s...",
					ehaList.size(), ehaList.size() > 1 ? "s" : ""));
			/*
			 * Using the new, wrapped publishing API.
			 */
			pubUtil.publishFlightAndDerivedChannels(true, ehaList,
					this.rct, this.ert, this.scet, this.sclk,
					new LocalSolarTime(true), streamId, !this.isRecorded,
					dssId.intValue(), vcid.intValue(), null);
		}

		ehaList = new ArrayList<>();

		iterItems = this.packetChannelMap.entrySet();

		for (final Entry<String, String> keyValue : iterItems) {
			final String key = keyValue.getKey();
			final String value = keyValue.getValue();

			ehaList.add(getChannelValue(key, value));
		}

		if (!ehaList.isEmpty()) {
			streamId = pubUtil.genStreamId("");

			logger.info(String.format("Publishing %d packet channel%s...",
					ehaList.size(), ehaList.size() > 1 ? "s" : ""));
			logger.info(String.format(
					"Initiating channel derivations (%susing triggers)...",
					this.useTriggers ? "" : "NOT "));
			pubUtil.publishFlightAndDerivedChannels(false, ehaList, this.rct, this.ert, this.scet, this.sclk, new LocalSolarTime(true),
					streamId, !this.isRecorded, dssId.intValue(),
					vcid.intValue(), Boolean.valueOf(this.useTriggers));
		}

		logger.info("DONE!");

		channelLad.writeCsv(new PrintWriter(System.out));
	}


	private IServiceChannelValue getChannelValue(final String channelId, String value) {

		final IChannelDefinition chanDef = chanDefTable
				.getDefinitionFromChannelId(channelId);

		if (chanDef == null) {
			throw new IllegalArgumentException(
					"Unable to find channel definition in channel dictionary for channel: "
							+ channelId);
		}

		final IServiceChannelValue chanVal = chanFactory.createServiceChannelValue(
				chanDef, channelId);

		Object dn = value;

		final ChannelType type = chanDef.getChannelType();
		if (BinOctHexUtility.hasHexPrefix(value)
				&& !type.equals(ChannelType.DIGITAL)) {
			if (type.equals(ChannelType.SIGNED_INT)) {
				dn = GDR.parse_int(value);

			} else if (type.equals(ChannelType.FLOAT)) {
				/*
				 * Workaround for bug in GDR. GDR includes the hex prefix in the
				 * length when determining hex overflow, we don't get into this
				 * block unless there is a hex prefix, so strip it off every
				 * time
				 */
				value = BinOctHexUtility.stripHexPrefix(value);

				/* 
				 * Note that the DOUBLE case was removed above. Now handle
				 * both FLOAT and DOUBLE here.  These GDR methods do not work unless the value is
				 * of the correct length anyway, so I will use that to decide which method to call.
				 */
				if (value.length() == Double.SIZE / 4) {
					dn = GDR.getDoubleFromHex(value);
				} else if (value.length() == Float.SIZE / 4) {				    
					dn = GDR.getFloatFromHex(value); 
				}
			} else if (type.equals(ChannelType.UNSIGNED_INT) || type.equals(ChannelType.TIME)) {
				/* Added TIME case above. */
				dn = GDR.parse_unsigned(value);
			} else {
				throw new IllegalArgumentException("Channel, " + channelId
						+ ", has a type of " + type
						+ ", which is not compatible with hex values.");
			}

			chanVal.setDn(dn);
		} else {
			if (chanVal instanceof IChannelValueMessage) {
				chanVal.setDnFromString(value,
						value.length());
			} else {
				chanVal.setDn(dn);
			}
		}

		/*
		 * No longer compute EU here. EU computation is now done
		 * by the EhaPublisherUtility.
		 */

		return chanVal;
	}

	private void startServices() throws IOException {
	    	
		commonContext.getBean(IGeneralContextInformation.class).setOutputDir(Files.createTempDirectory("spill").toString());

		channelLad = commonContext.getBean(IChannelLad.class);

	    initEhaFeatures();

		if (this.publishToJms) {
		    this.msgPortal = commonContext.getBean(IMessagePortal.class);
		    this.msgPortal.startService();
		}
	}

	private void stopServices() {
		if (this.msgPortal != null) {
			this.msgPortal.stopService();
		}

		if (this.ehaPubService != null) {
			this.ehaPubService.stopService();
		}
		
		if (this.alarmPubService != null) {
		    this.alarmPubService.stopService();
		}

		if (this.channelLadService != null) {
			this.channelLadService.stopService();
		}
	}


	private boolean initEhaFeatures()  {
       
        try {
            loadDictionaries();

        } catch (final DictionaryException e) {
            logger.error(e.getMessage() + e.getCause());
            logger.error("Unable to load channel and/or alarm definitions");
            return false;
        }
        
                  
        //Setup the channel publisher, which listens for packet messages and publishes ChannelValueMessages
        ehaPubService = commonContext.getBean(IPrechannelizedPublisherService.class);
		pubUtil = commonContext.getBean(IChannelPublisherUtility.class);
        
        //Setup the LAD (Latest Available Data) table to listen for channel values
        this.channelLadService = commonContext.getBean(IChannelLadService.class);

        
        //Setup the alarm publisher, which listens for ChannelValueMessages and publishes EHA channel messages
        //(we always instantiate this...if alarm processing is turned off, it's up to the alarm publisher to
        //act as a pass-through for channel value messages)
        alarmPubService = commonContext.getBean(IAlarmPublisherService.class);
         
        return (channelLadService.startService() && ehaPubService.startService() && alarmPubService.startService());
    }
	
	  /**
     * Loads the mission-specific channel definitions and alarm definitions.
     * 
     * @param channelFilename the channel definition file to load
     * @param alarmFilename the alarm definition file to load
     * @param userAlarmFilename the user alarm file to load
     * @throws DictionaryException
     */
    private void loadDictionaries()
            throws DictionaryException {
        
        chanDefTable.loadFsw(true);
        final Map<String, IChannelDefinition> chanMap = chanDefTable.getChannelDefinitionMap();
        alarmDefTable.loadFsw(chanMap);
       
    }
    

	/**
	 * Main method for this application.
	 * 
	 * @param args list of user supplied command line arguments
	 * @throws Exception if an unrecoverable error occurs
	 */
	public static void main(final String[] args) throws Exception {
		final ChannelValueSimulatorApp theApp = new ChannelValueSimulatorApp();

		try {
		    final ICommandLine cl = theApp.createOptions().parseCommandLine(args, true);
            theApp.configure(cl);
			theApp.startServices();
			theApp.run();
		} catch (final Exception e) {
			if (e.getMessage() == null) {
				TraceManager.getTracer(Loggers.DEFAULT).error(e.toString());
			} else {
				TraceManager.getTracer(Loggers.DEFAULT).error(e.getMessage());
			}

			if (theApp.debugMode) {
				e.printStackTrace();
			}

			System.exit(1);
		} finally {
			theApp.stopServices();
		}

		System.exit(0);
	}

    // package private getters for tests

    ApplicationContext getCommonContext() {
        return commonContext;
    }

    Map<String, String> getLadChannelMap() {
        return Collections.unmodifiableMap(ladChannelMap);
    }

    Map<String, String> getPacketChannelMap() {
        return Collections.unmodifiableMap(packetChannelMap);
    }

    boolean isPublishToJms() {
        return publishToJms;
    }

    boolean isDebugMode() {
        return debugMode;
    }

    boolean isRecorded() {
        return isRecorded;
    }

    boolean isUseTriggers() {
        return useTriggers;
    }

    String getPubTopic() {
        return pubTopic;
    }

    UnsignedInteger getVcid() {
        return vcid;
    }

    UnsignedInteger getDssId() {
        return dssId;
    }

    UnsignedInteger getScid() {
        return scid;
    }

    IAccurateDateTime getRct() {
        return rct;
    }

    IAccurateDateTime getErt() {
        return ert;
    }

    IAccurateDateTime getScet() {
        return scet;
    }

    ISclk getSclk() {
        return sclk;
    }

}
