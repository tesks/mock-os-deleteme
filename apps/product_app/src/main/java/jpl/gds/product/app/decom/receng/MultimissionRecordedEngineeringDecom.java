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
package jpl.gds.product.app.decom.receng;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.config.CcsdsProperties;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.mission.RealtimeRecordedConfiguration;
import jpl.gds.common.config.mission.RealtimeRecordedConfiguration.StrategyEnum;
import jpl.gds.common.notify.NotificationProperties;
import jpl.gds.common.types.RecordedBool;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.store.ldi.IEvrLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IChannelAggregateLDIStore;
import jpl.gds.db.api.types.IDbSessionUpdater;
import jpl.gds.decom.DecomEngine;
import jpl.gds.decom.exception.DecomException;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.apid.ApidContentType;
import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.client.alarm.IAlarmDictionaryManager;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.evr.IEvrUtilityDictionaryManager;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.decom.IChannelDecomDefinitionProvider;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.service.alarm.IAlarmNotifierService;
import jpl.gds.eha.api.service.alarm.IAlarmPublisherService;
import jpl.gds.eha.api.service.channel.IChannelLadService;
import jpl.gds.eha.api.service.channel.IChannelPublisherUtility;
import jpl.gds.eha.api.service.channel.IChannelizationListener;
import jpl.gds.eha.api.service.channel.IDecomListenerFactory;
import jpl.gds.eha.api.service.channel.IGroupedChannelAggregationService;
import jpl.gds.eha.api.service.channel.IPrechannelizedAdapter;
import jpl.gds.eha.api.service.channel.PrechannelizedAdapterException;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.evr.api.message.IEvrMessageFactory;
import jpl.gds.evr.api.service.IEvrNotifierService;
import jpl.gds.evr.api.service.extractor.EvrExtractorException;
import jpl.gds.evr.api.service.extractor.IEvrExtractor;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.ProductOutputFormat;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.config.RecordedProductProperties;
import jpl.gds.product.api.decom.IStoredProductInput;
import jpl.gds.product.api.decom.formatter.IProductDecomOutputFormatter;
import jpl.gds.product.api.decom.receng.IRecordedEngProductDecom;
import jpl.gds.product.app.decom.AbstractProductDecom;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.shared.types.ByteStream;
import jpl.gds.shared.types.FileByteStream;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory;


/**
 * MultimissionRecordedEngineeringDecom is the  class for decommutating
 * engineering product data (EVRS and channels) downlinked as the content
 * of data products.  The format of the data product content is a stream of
 * CCSDS packets.
 *
 */
public class MultimissionRecordedEngineeringDecom extends AbstractProductDecom implements IRecordedEngProductDecom
{

	private final RecordedBool markingState;

	/*
	 * Removed the NO_STATION constant.
	 * Should be using UNSPECIFIED station ID.
	 */

	// Behavioral parameters set at instantiation time
	private boolean useDatabase         = false;
	private boolean useJms              = false;
	private String  dirOverride         = null;
	private String  versionOverride     = null;

	// Some initialization must occur before processing the first product
	private boolean readyToHandleProducts = false;
	// Indicates whether to process version 1 files only
	private boolean useVersionOneOnly;
	private IContextConfiguration sessionConfig;

	// Before processing the first product, the following must be initialized
	// using the metadata of the first product to identify session and
	// dictionary information. Note this means we cannot process a
	// product with an incompatible dictionary!
	private IMessagePortal       msgPortal       = null;
	private IPrechannelizedAdapter           ehaAdapter      = null;
	private IEvrExtractor          evrAdapter      = null;
	private IDbSqlArchiveController   archiveController = null;
	private  IAlarmPublisherService      alarmPublisher  = null;
	private IChannelPublisherUtility  pubUtil;
	private IService    ladService = null;

	// Tracks session number from product to product
	private long                 previousSessionNumber = 0;

	// Regular expressions for matching recorded product types
	private final List<Integer>         ehaProductTypeApidList = new ArrayList<Integer>();
	private final List<Integer>         evrProductTypeApidList = new ArrayList<Integer>();

	private final ISecondaryPacketHeaderLookup secondaryPacketHeaderLookup;
	private IChannelUtilityDictionaryManager channelDictUtil;
	private IAlarmDictionaryManager  alarmDictUtil;
	private IEvrUtilityDictionaryManager evrDictUtil;
	private IService _evrNotifier;
	private IService _alarmNotifier;
	private IEvrMessageFactory evrMsgFactory;
	private IApidDefinitionProvider apidDefs;
	private IChannelDecomDefinitionProvider decomDict;
	private final ITelemetryPacketInfoFactory pktInfoFactory;
	private final CcsdsProperties ccsdsProps;

	private IGroupedChannelAggregationService channelAggregationService;

	/**
	 * Constructor.
	 *
	 * @param context
	 *            the Spring ApplicationContext
	 * @throws ProductException
	 *             If problem in constructor
	 */
	public MultimissionRecordedEngineeringDecom(final ApplicationContext context) throws ProductException {
		super(context);
		this.setShowProductViewer(false);
		this.setShowDpoViewer(false);
		this.setIgnoreChecksum(false);
		this.setShowLaunchInfo(false);
		this.setSupressText(true);
		this.ccsdsProps = context.getBean(CcsdsProperties.class);
		this.outf = appContext.getBean(IProductDecomOutputFormatter.class, ProductOutputFormat.NO_PRODUCT_OUTPUT, null);
		this.setMessageContext(context.getBean(IMessagePublicationBus.class));
		secondaryPacketHeaderLookup = context.getBean(ISecondaryPacketHeaderLookup.class);

		// Get properties from the context
		final RecordedProductProperties rpc = appContext.getBean(RecordedProductProperties.class);
		useVersionOneOnly = rpc.isUseOnlyVersionOneProducts();
		final String[] ehaTypes = rpc.getEhaProductApids();
		final String[] evrTypes = rpc.getEvrProductApids();

		if (ehaTypes != null) {
			for (final String exp: ehaTypes) {
				this.ehaProductTypeApidList.add(Integer.valueOf(exp));
			}
		}

		if (evrTypes != null) {
			for (final String exp: evrTypes) {
				this.evrProductTypeApidList.add(Integer.valueOf(exp));
			}
		}


		boolean rt = false;

		try
		{
			final RealtimeRecordedConfiguration rtRecConfig = context.getBean(RealtimeRecordedConfiguration.class);

			// It's real-time only if UNCONDITIONAL and REALTIME

			rt = ((rtRecConfig.getTelemetryMarkingStrategy() == StrategyEnum.UNCONDITIONAL) &&
					! rtRecConfig.getTelemetryUnconditionalMarking().get());
		}
		catch (final Exception de)
		{
			log.error("MultimissionRecordedEngineeringDecom " + "Unable to get marking state, assuming recorded: "
					          + de.getLocalizedMessage(), de.getCause());

			rt = false;
		}

		markingState = RecordedBool.valueOf(! rt);

		pktInfoFactory = context.getBean(ITelemetryPacketInfoFactory.class);
	}


	/**
	 * Set version-one-only state.
	 *
	 * @param enable True if only version one allowed
	 */
	public void setVersionOneOnly(final boolean enable) {
		this.useVersionOneOnly = enable;
	}


	/**
	 * Constructor.
	 *
	 * @param context
	 *            the Spring Application Context
	 * @param useDatabase
	 *            True if database to be used
	 * @param useGlobalLad
	 *            True if global LAD to be used
	 * @param useJms
	 *            True if the message service is to be used
	 * @param dictDirOverride
	 *            True if directory override
	 * @param dictVersionOverride
	 *            True if version override
	 *
	 * @throws ProductException
	 *             If error setting up constructor
	 */
	public MultimissionRecordedEngineeringDecom(final ApplicationContext context, final boolean useDatabase, final boolean useGlobalLad, final boolean useJms, final String dictDirOverride, final String dictVersionOverride)
			throws ProductException {
		this(context);
		this.useDatabase = useDatabase;
		this.useJms = useJms;
		this.dirOverride = dictDirOverride;
		this.versionOverride = dictVersionOverride;
	}

	/**
	 * Invoked by ProductHandler shutdown method, or by the RecordedEngProductDecomApp.
	 */
	@Override
	public void shutdown() {
		try {
			/*
			 * Stop Alarm Publisher if running
			 */
			if (null != alarmPublisher) {
				log.debug("Shutting down alarm publisher service");
				alarmPublisher.stopService();
				log.debug("Shutting down alarm publisher completed");
			}

			if (null != _evrNotifier) {
				log.debug("Shutting down evr notifier service");
				_evrNotifier.stopService();
				log.debug("Shutting down evr notifier completed");
			}


			if (null != _alarmNotifier) {
				log.debug("Shutting down alarm notifier service");
				_alarmNotifier.stopService();
				log.debug("Shutting down alarm notifier completed");
			}

			/*
			 * Shutdown sequence is important for EHA Aggregation. Need to stop the
			 * Aggregation Service before the Archive Controllers so that all channel
			 * samples are stored into the database.
			 */

			// EHA Aggregation Integration
			if (channelAggregationService != null) {
				log.debug("Shutting down EHA aggregation service");
				channelAggregationService.stopService();
				log.debug("Shutting down EHA aggregation service completed");
			}

			// LAD shutdown
			if (ladService != null) {
				log.debug("Shutting down GlobalLAD service");
				ladService.stopService();
				log.debug("Shutting down GlobalLAD service completed");
			}

			messageContext.unsubscribeAll();
			log.debug("Unsubscribed from messages");

			if (null != msgPortal) {
				log.debug("Shutting down message portal service ");
				msgPortal.stopService();
				log.debug("Shutting down message portal completed");
			}

			/*
			 * Stop all LDI Stores.
			 */
			if (null != archiveController) {
				log.debug("Shutting down archive controller service");
				archiveController.shutDown();
				/*
				 * Set to null so we won't try to stop again
				 */
				archiveController = null;
				log.debug("Shutdown archive controller completed");
			}

		} catch (final Exception e) {
			log.error("Could not cleanly shutdown MultimissionRecordedEngineeringDecom: " + e.toString(), e.getCause());
		}
	}

	/**
	 * Checks the product version of the given data or emd file.
	 * @param fileName name of product .dat or .emd file
	 * @return true if product version okl false if not
	 * @throws ProductException
	 */
	private boolean checkVersionNumber( final String fileName ) throws ProductException {
		if (useVersionOneOnly) {
			try {
				final int dot = fileName.lastIndexOf('.');
				final String fileNameWithoutExtension = fileName.substring(0,dot);
				final int dash = fileNameWithoutExtension.lastIndexOf('-');
				final String versionString = fileNameWithoutExtension.substring(dash+1);
				final int versionNumber = Integer.parseInt(versionString);
				if ( 1 != versionNumber ) {
					return false;
				}
			} catch (final Exception e) {
				e.printStackTrace();
				throw new ProductException("Cannot determine version number of data product file "
						                           + fileName + " (" + e + ").");
			}
		}
		return true;
	}

	/**
	 * Execute the extraction Process. Invoked by RecordedEngProductDecomApp.
	 * Filename of the data product may be a .dat or a .emd file.
	 *
	 * @param fileName
	 *            Data product name
	 *
	 * @throws ProductException
	 *             if the product file cannot be read
	 *
	 */
	@Override
	public void execute(final String fileName) throws ProductException {

		// convert filename argument into both the .dat and .emd names
		final String[] filenames = IStoredProductInput.getFilenames(fileName);
		final String dataFileName = filenames[0];

		if (!checkVersionNumber(dataFileName)) {
			throw new ProductException(new File(dataFileName).getName() + " is not a version 1 product");
		}

		// The only member required to be set for handleProduct is the full path.
		final IProductMetadataUpdater metadata = appContext.getBean(IProductBuilderObjectFactory.class).createMetadataUpdater();
		metadata.setFullPath(dataFileName);

		ByteStream bstream;
		try {
			bstream = new FileByteStream(dataFileName, log);
		}
		catch (final IOException e) {
			throw new ProductException(e.getClass().getSimpleName() + " opening Product Data File: " + dataFileName + " (" + e.toString()
					                           + ")");
		}

		// Check return value from handleProduct()
		if (!handleProduct(metadata, bstream)) {
			throw new ProductException("Data product content could not be extracted");
		}
	}

	/**
	 * Subscribes to EVR and EHA channel messages.
	 * @param subscriber the object wishing to subscribe
	 */
	@Override
	public void subscribe(final MessageSubscriber subscriber) {
		this.messageContext.subscribe(EvrMessageType.Evr, subscriber);
		this.messageContext.subscribe(EhaMessageType.AlarmedEhaChannel, subscriber);
	}

	/**
	 * Un-subscribes from EVR and EHA channel messages.
	 * @param subscriber the object wishing to un-subscribe
	 */
	@Override
	public void unSubscribe(final MessageSubscriber subscriber) {
		this.messageContext.unsubscribe(EvrMessageType.Evr, subscriber);
		this.messageContext.unsubscribe(EhaMessageType.AlarmedEhaChannel, subscriber);
	}

	/**
	 * Load dictionaries, create adapters, and other responsibilities that need
	 * to be performed once per run prior to being able to process any recorded
	 * data product.
	 *
	 * @param productMeta
	 *            the product metadata of the product to be processed
	 *
	 */
	private boolean prepareToHandleProducts( final IProductMetadataProvider productMeta ) {

		//    IOldApplicationContext commonContext = null;

		final String fileName = productMeta.getFullPath();
		final String shortFileName = new File(fileName).getName();

		/*
		 * If everything in this method has succeeded before,
		 * just make sure that the session number on the new product is the same.
		 * We cannot process products from multiple sessions.
		 */
		if (this.readyToHandleProducts) {
			if (this.previousSessionNumber != productMeta.getSessionId()) {
				log.error("Data product " + shortFileName + " does not come from the same session as the previously processed product.");
				return false;
			} else {
				return true;
			}
		} else {
			this.previousSessionNumber = productMeta.getSessionId();
		}

		/*
		 * BEGIN: Even if we have not successfully prepared to process
		 * products, we only want to do this  ONCE.
		 */
		if (sessionConfig == null) {
			/*
			 * Retrieve original session during which the product was actually created.
			 * This required productMeta.loadFile has already been invoked.
			 */
			try {
				int fragment = appContext.getBean(IContextKey.class).getFragment();

				/* Can no longer create session straight
				 * from database object. Replace with multiple steps.
				 */
			    final IDbSessionUpdater dbSession = RecordedEngProductDecomUtility.retrieveSessionById(appContext,
			         productMeta.getSessionId(), productMeta.getSessionHost(), fragment);
				sessionConfig = appContext.getBean(IContextConfiguration.class);
				dbSession.setIntoContextConfiguration(sessionConfig);
				IContextConfiguration.resetTypeToApplication(sessionConfig);
			}
			catch (final ProductException e) {
				log.error("Cannot load session with id=" + productMeta.getSessionId()
						          + "from the database. Unable to process recorded EVR/EHA product " + shortFileName);
				return false;
			}

			// Make sure message subtopic is set appropriately
			setMessageServiceSubtopic();

			if (dirOverride != null) {
				appContext.getBean(DictionaryProperties.class).setFswDictionaryDir(dirOverride);
			}

			if (versionOverride != null) {
				appContext.getBean(DictionaryProperties.class).setFswVersion(versionOverride);
			}

			channelDictUtil = appContext.getBean(IChannelUtilityDictionaryManager.class);
			alarmDictUtil =  appContext.getBean(IAlarmDictionaryManager.class);
			evrDictUtil = appContext.getBean(IEvrUtilityDictionaryManager.class);

			if ( useJms ) {

				msgPortal = appContext.getBean(IMessagePortal.class);
				msgPortal.startService();
			}

			try {
				ladService = appContext.getBean(IChannelLadService.class);
			} catch (final Exception e) {
				e.printStackTrace();
				log.error("Channel LAD service could not be initialized: " + e.getMessage());
				return false;
			}
			ladService.startService();

			try {
				pubUtil = appContext.getBean(IChannelPublisherUtility.class);
			} catch (final Exception e) {
				e.printStackTrace();
				log.error("EHA publisher utility could not be initialized: " + e.getMessage());
				return false;
			}

			/*
			 * Load User JAR files for derived channels, etc.
			 */
			sessionConfig.getDictionaryConfig().loadDictionaryJarFiles(false, secureLoader, log);
		}

		/*
		 * Initialize dictionaries
		 */
		try {

			loadDictionaries(sessionConfig.getDictionaryConfig(), appContext);
		}
		catch (final DictionaryException e1) {
			e1.printStackTrace();
			log.error("Cannot load dictionaries for session with id=" + productMeta.getSessionId()
					          + ". Unable to extract EVR/EHA data from product " + shortFileName, e1);
			log.error(e1.toString());
			return false;
		}

		/*
		 * Initialize EHA Adapter: creates an object that knows how to parse channel packets
		 */
		try {
			ehaAdapter = appContext.getBean(IPrechannelizedAdapter.class);
		}
		catch (final Exception e) {
			log.error("EHA adapter configuration error: " + e.getMessage());
			return false;
		}


		evrMsgFactory = appContext.getBean(IEvrMessageFactory.class);


		/*
		 * Initialize EVR Adapter: creates an object that knows how to parse EVR packets
		 */
		try {
			evrAdapter = appContext.getBean(IEvrExtractor.class);
		}
		catch (final Exception e) {
			log.error("EVR extractor configuration error: " + e.getMessage());
			return false;
		}

		//  EHA Aggregation Integration
		try {
			channelAggregationService = appContext.getBean(IGroupedChannelAggregationService.class);
		}
		catch (final Exception e) {
			log.error("Grouped Channel Aggregation Service configuration error: " + e.getMessage());
			return false;
		}
		channelAggregationService.startService();

		/*
		 * Start up required LDI Stores for the channels and EVRs generated.
		 */
		if (useDatabase) {
			log.info("Starting database stores");
			archiveController = appContext.getBean(IDbSqlArchiveController.class);
			// Use IEvrLDIStore interface rather than the store directly
			archiveController.addNeededStore(IEvrLDIStore.STORE_IDENTIFIER);

			// EHA Aggregation Integration
			archiveController.addNeededStore(IChannelAggregateLDIStore.STORE_IDENTIFIER);

			archiveController.startAllStores();
		}

		/*
		 * Start up Alarm Publisher
		 */
		try {
			alarmPublisher = appContext.getBean(IAlarmPublisherService.class);
		} catch (final Exception e) {
			e.printStackTrace();
			log.error("Alarm publisher configuration error: " + e.getMessage());
			return false;
		}
		alarmPublisher.enableCalculation(true);
		alarmPublisher.startService();

		/* Moved start of notification services here form RecordedEngineeringProductHandler,
		 * because all services must share common context.
		 */

		final NotificationProperties nc = appContext.getBean(NotificationProperties.class);

		IService evrNotifierHolder = null;
		try {
			evrNotifierHolder = (nc.isRecordedEvrNotificationEnabled()
					? appContext.getBean(IEvrNotifierService.class)
					: null);
		} catch (final Exception e1) {
			e1.printStackTrace();
			log.error("No EVR notification will be done in MultimissionRecordedEngineeringDecom");
		}

		_evrNotifier = evrNotifierHolder;

		IService alarmNotifierHolder = null;

		try
		{
			alarmNotifierHolder = (nc.isRecordedAlarmNotificationEnabled() ?
					appContext.getBean(IAlarmNotifierService.class)
					: null);

		}
		catch (final Exception e)
		{
			e.printStackTrace();
			log.error("No alarm notification will be done in MultimissionRecordedEngineeringDecom");
		}

		_alarmNotifier = alarmNotifierHolder;

		if (_evrNotifier != null)
		{
			_evrNotifier.startService();
		}

		if (_alarmNotifier != null)
		{
			_alarmNotifier.startService();
		}

		readyToHandleProducts = true;

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean handleProduct(final IProductMetadataProvider abstractMeta, final ByteStream bytestream) {

		final String fileName = abstractMeta.getFullPath();
		final String shortFileName = new File(fileName).getName();

		// convert filename argument into both the .dat and .emd names
		String[] filenames;
		try {
			filenames = IStoredProductInput.getFilenames(fileName);
		} catch (final ProductException e) {
			log.error(e.getMessage() + ". Cannot understand product filename extracted from metadata argument.");
			return false;
		}
		final String dataFileName = filenames[0];
		final String metaFileName = filenames[1];

		try {
			if (!checkVersionNumber(dataFileName)) {
				log.error("Product " + shortFileName + " is not a version 1 EHA/EVR product");
				return false;
			}
		} catch (final ProductException e1) {
			log.error(e1.getMessage());
			return false;
		}

		// The incoming metadata argument only has the fullPath member set.
		// It also may be the wrong type if called from the watcher (RecordedEngineeringProductHandler).
		// We need the right kind of metadata, and we need to load it up from the .emd file of the
		// recorded data product.
		final IProductMetadataUpdater productMeta =
				appContext.getBean(IProductBuilderObjectFactory.class).createMetadataUpdater();
		try {
			productMeta.loadFile(metaFileName);
		} catch (final ProductException e) {
			log.error(e + ". Cannot load metadata file for EHA/EVR product " + shortFileName);
			return false;
		}

		if (!this.evrProductTypeApidList.contains(productMeta.getApid()) && !this.ehaProductTypeApidList.contains(productMeta.getApid())) {
			log.warn("APID number " + productMeta.getApid() + " is not recorded EHA or EVR,"
					         + " unable to process " + fileName );
			return false;
		}

		if (!prepareToHandleProducts(productMeta)) {
			log.error("Unable to extract data from EHA/EVR product " + shortFileName );
			return false;
		}

		log.info("Extracting recorded data from product " + shortFileName);

		/*
		 * Iterate through all packets in the data product and channelize or extract EVRs.
		 */
		try {
			int packetNumber = 0; // for warning messages

			// Use packet header factory to get right ISpacePacketHeader object object
			final ISpacePacketHeader header = PacketHeaderFactory.create(ccsdsProps.getPacketHeaderFormat());

			while ( bytestream.hasMore() ) {
				packetNumber++;

				// From the bytestream, load packet primary header.
				final byte priHdrBytes[] = bytestream.readIntoByteArray(header.getPrimaryHeaderLength());
				header.setPrimaryValuesFromBytes(priHdrBytes,0);

				// No secondary header handling allowed in processors.
				// Removed logic to read exact bytes.

				// Get the data from packet. Packet data length is ONE LESS THAN packet data length,
				// per odd, overly clever CCSDS standard.
				final int dataLen = header.getPacketDataLength() + 1;
				final byte[] dataBytes = bytestream.readIntoByteArray(dataLen);

				if (null == dataBytes) {
					log.warn("Packet " + packetNumber + " contains no data");
					continue;
				}

				final ISecondaryPacketHeader secHeader = secondaryPacketHeaderLookup.lookupExtractor(header).extract(dataBytes, 0);

				final ITelemetryPacketInfo packetInfo = pktInfoFactory.create(header, header.getPacketDataLength() + 1 + header.getPrimaryHeaderLength(), secHeader);

				final int apid = header.getApid();
				final IApidDefinition apidDef = apidDefs.getApidDefinition(apid);
				if (apidDef == null) {
					log.warn("APID number " + apid + " is not defined in the APID dictionary");
					continue;
				}

				final ApidContentType apidType = apidDefs.getApidDefinition(apid).getContentType();
				if (apidType == ApidContentType.PRE_CHANNELIZED) {
					handleRecordedEha( productMeta, packetInfo, packetNumber, dataBytes );
				} else if (apidType == ApidContentType.EVR) {
					handleRecordedEvr( productMeta, packetInfo, packetNumber, dataBytes, header );
				} else if (apidType == ApidContentType.DECOM_FROM_MAP) {
					handleDecom(productMeta, packetInfo, packetNumber, dataBytes, header);
				} else {
					log.warn("APID number " + apid + " is not recorded EHA or EVR,"
							         + " unable to process " + shortFileName );
					return false;
				}

			}
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private void handleDecom(final IProductMetadataProvider productMeta, final ITelemetryPacketInfo packetInfo,
	                         final int packetNumber, final byte[] dataBytes, final ISpacePacketHeader header) {

		final IDecomListenerFactory listenFactory = appContext.getBean(IDecomListenerFactory.class);
		final IChannelizationListener decomListener = listenFactory.createHybridEvrChannelizationListener(appContext);
		final DecomEngine decomEngine = new DecomEngine(appContext, decomListener);
		decomEngine.addListener(decomListener);

		final IDecomMapDefinition map = this.decomDict.getDecomMapByApid(header.getApid());
		if (map == null) {
			log.warn("Could not find decom map to process packet APID " + header.getApid() + ", number: " + packetNumber);
			return;
		}

		decomListener.setPacketInfo(packetInfo);
		try {
			// headerAndData - we need to pass both header and data to decom
			byte[] headerAndData = new byte[header.getBytes().length + dataBytes.length];
			System.arraycopy(header.getBytes(), 0, headerAndData, 0, header.getBytes().length);
			System.arraycopy(dataBytes, 0, headerAndData, header.getBytes().length, dataBytes.length);
			decomEngine.decom(map, headerAndData, 0, packetInfo.getSize() * Byte.SIZE);
		}
		catch (final DecomException e) {
			log.error(String.format("Generic decom failure occured while processing packet number %d, %s; cause: %s",
			                        packetNumber, packetInfo.getIdentifierString(), e.getMessage()));
		}

		final List<IServiceChannelValue> ehaList = decomListener.collectChannelValues();

		if (!ehaList.isEmpty()) {

			final String streamID = pubUtil.genStreamId("");
			final IAccurateDateTime rct = new AccurateDateTime();

			// calculate scet
			final ISclk sclk = packetInfo.getSclk();
			IAccurateDateTime scet = SclkScetUtility.getScet(sclk, productMeta.getErt(), productMeta.getScid());
			if (scet == null) {
				scet = new AccurateDateTime(0);
			}

			pubUtil.publishFlightAndDerivedChannels(false, ehaList, rct, productMeta.getErt(), scet,
			                                        sclk, null, streamID, !markingState.get(),
			                                        StationIdHolder.UNSPECIFIED_VALUE, productMeta.getVcid(), null);

			ehaList.clear();
		}
	}

	private void handleRecordedEha( final IProductMetadataProvider productMeta, final ITelemetryPacketInfo packetInfo, final int packetNumber, final byte[] dataBytes ) {

		if ( dataBytes.length < 3 ) {
			log.warn("Packet " + packetNumber + " is too short (" + dataBytes.length + " bytes)"
					         + " to contain a channel ID and channel value.");
			return;
		}

		// convert packet data bytes to list of flight channel values

		List<IServiceChannelValue> ehaList;
		try {
			ehaList = ehaAdapter.extractEha(dataBytes, packetInfo.getSecondaryHeaderLength(), dataBytes.length - packetInfo.getSecondaryHeaderLength());
		} catch (final PrechannelizedAdapterException e) {
			e.printStackTrace();
			return;
		}

		if (ehaList.isEmpty()) {
			// Can do this safely only because already checked length of dataBytes more than 2
			log.warn("Packet " + packetNumber + "in product " + productMeta.getAbsoluteDataFile()
					         + " had no recognizable channels.");
			return;
		}

		final String streamID = pubUtil.genStreamId("");

		final ISclk sclk = packetInfo.getSclk();
		IAccurateDateTime scet = SclkScetUtility.getScet(sclk, productMeta.getErt(), productMeta.getScid());
		if (scet == null) {
			scet = new AccurateDateTime(0);
		}

		final IAccurateDateTime rct = new AccurateDateTime();

		for (final IServiceChannelValue icv : ehaList)
		{
			icv.setRealtime(! markingState.get());
		}

		/*
		 * Using UNSPECIFIED_DSSID as station ID.
		 */
		pubUtil.publishFlightAndDerivedChannels(false,
		                                        ehaList,
		                                        rct,
		                                        productMeta.getErt(),
		                                        scet,
		                                        sclk,
		                                        null,
		                                        streamID,
		                                        ! markingState.get(),
		                                        StationIdHolder.UNSPECIFIED_VALUE,
		                                        productMeta.getVcid(), null);
	}


	private void handleRecordedEvr(final IProductMetadataUpdater productMeta,
	                               final ITelemetryPacketInfo             packetInfo,
	                               final int                      packetNumber,
	                               final byte[]                   dataBytes,
	                               final ISpacePacketHeader            header)
	{
		try
		{
			final Integer vcid = productMeta.getVcid();

			/*
			 * Using UNSPECIFIED_DSSID as station ID.
			 */
			final IEvr evr = this.evrAdapter.extractEvr(dataBytes,
			                                            packetInfo.getSecondaryHeaderLength(),
			                                            dataBytes.length - packetInfo.getSecondaryHeaderLength(),
			                                            packetInfo.getApid(),
			                                            vcid,
			                                            StationIdHolder.UNSPECIFIED_VALUE,
			                                            header.getSourceSequenceCount());
			evr.setVcid(vcid);
			evr.setDssId((byte) 0);  // No station

			final ISclk sclk = packetInfo.getSclk();
			evr.setSclk(sclk);

			IAccurateDateTime scet = SclkScetUtility.getScet(sclk, productMeta.getErt(), productMeta.getScid());

			if (scet == null) {
				scet = new AccurateDateTime(0);
			}
			evr.setScet(scet);
			evr.setErt(productMeta.getErt());
			final IEvrMessage message = evrMsgFactory.createEvrMessage(evr);
			evr.setRct(new AccurateDateTime());

			evr.setRealtime(! markingState.get());

			messageContext.publish(message);
		} catch (final EvrExtractorException e) {
			log.error("Problem extracting EVR from packet " + packetNumber + " in product " + productMeta.getFullPath());
			e.printStackTrace();
		}
	}


	/**
	 * Loads the mission-specific channel, alarm, and EVR dictionaries
	 *
	 * @param dictConfig Dictionary config
	 * @param springContext Spring App context
	 *
	 * @throws DictionaryException
	 */
	private void loadDictionaries(final DictionaryProperties dictConfig, final ApplicationContext springContext)
			throws DictionaryException {

		apidDefs = appContext.getBean(IApidDefinitionProvider.class);

		/**
		 * Add monitor channels. Necessary since decom
		 * maps are now loaded.
		 */
		channelDictUtil.loadFsw(true);
		channelDictUtil.loadMonitor(false);

		alarmDictUtil.loadFsw(channelDictUtil.getChannelDefinitionMap());

		evrDictUtil.loadFsw();

		decomDict = this.appContext.getBean(IChannelDecomDefinitionProvider.class);
	}


	/**
	 * Make sure JMS subtopic is set appropriately. For an OPS
	 * venue, we need the subtopic of the session to compute the
	 * topic name properly. So we extract it from the topic of
	 * the session.
	 *
	 * If anything is wrong, or we cannot match the subtopic
	 * with the list of allowed subtopics, do not set the subtopic
	 * in the configuration.
	 *
	 */
	private void setMessageServiceSubtopic()
	{
		if (! appContext.getBean(IVenueConfiguration.class).getVenueType().isOpsVenue() ||
				(appContext.getBean(IGeneralContextInformation.class).getSubtopic() != null))
		{
			// Not OPS venue, or subtopic already set, bail
			return;
		}

		final String topic = StringUtil.safeTrim(appContext.getBean(IGeneralContextInformation.class).getRootPublicationTopic());
		final int    dot   = topic.lastIndexOf('.');

		if (dot < 0)
		{
			// Not a properly formatted topic, bail
			return;
		}

		// Extract subtopic from topic
		final String subtopic = topic.substring(dot + 1);

		// See if it was a valid subtopic for this mission

		for (final String st : this.appContext.getBean(MissionProperties.class).getAllowedSubtopics())
		{
			if (subtopic.equalsIgnoreCase(StringUtil.safeTrim(st)))
			{
				// Found it, set it in configuration

				appContext.getBean(IGeneralContextInformation.class).setSubtopic(subtopic.toUpperCase());
				break;
			}
		}
	}
}