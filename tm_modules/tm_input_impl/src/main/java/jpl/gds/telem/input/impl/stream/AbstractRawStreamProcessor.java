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
package jpl.gds.telem.input.impl.stream;

import java.util.Date;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.station.api.IStationHeaderFactory;
import jpl.gds.station.api.IStationInfoFactory;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import jpl.gds.telem.input.api.data.IRawDataProcessor;
import jpl.gds.telem.input.api.stream.IRawStreamProcessor;
import jpl.gds.telem.input.impl.message.RawInputMessenger;

/**
 * This is an abstract implementation of <code>IRawStreamProcessor</code>,
 * providing common implementations for stream processing methods
 * 
 *
 */
public abstract class AbstractRawStreamProcessor implements IRawStreamProcessor
{
    /** Tracer */
    protected static Tracer          logger;

    /** Count of discarded bytes */
	protected final long bytesDiscardedWhilePausedThreshold;

    /** Message context */
	protected IMessagePublicationBus context;

    /** Raw input type */
	protected TelemetryInputType inputType;

    /** Configuration object */
	protected TelemetryInputProperties rawConfig;

    /** Messenger object */
	protected RawInputMessenger messenger;

    /** True if data is first seen */
	protected boolean firstData = true;

    /** True if out -of-sync */
	protected boolean isOutOfSync = true;

    /** True if stopped */
	protected boolean stopped = false;

    /** True if paused */
	protected boolean paused = false;

    /** Count of discarded bytes */
	protected long bytesDiscarded;

    /** Time at which paused */
	protected Date pauseTime;

    /** True if remote mode */
	protected boolean isRemoteMode;

    /** True if connection lost */
	protected boolean connectionLost;

    /**
     * Station configured at startup, if any.
     * MPCS-5013 07/03/13
     */
    private final Integer configuredStation;

    /**
     * Set and clear via methods always
     *
     * MPCS-5013 07/23/13
     */
    private boolean eofOnStream = false;
    
    /** The meter interval */
    private long meterInterval;
    
    /** The current spacecraft ID */
    protected int scid;
    
    /** The current application context */
    protected ApplicationContext appContext;
    
    /** The raw data processor in use */
    protected IRawDataProcessor rawDataProc;

    /** Station info object factory */
    protected IStationInfoFactory stationInfoFactory;

    /** Station header factory  */
    protected IStationHeaderFactory stationHeaderFactory;
    

	/**
	 * Default constructor.
	 * 
	 * @param serviceContext the current application context
	 */
	protected AbstractRawStreamProcessor(final ApplicationContext serviceContext) {
        super();

        appContext = serviceContext;
		rawConfig = serviceContext.getBean(TelemetryInputProperties.class);
		this.messenger = appContext.getBean(RawInputMessenger.class);
		context = serviceContext.getBean(IMessagePublicationBus.class);

        // MPCS-9947 6/29/18: removed getBean(IRawDataProcessor).
        // The bean is NOT a singleton and it was causing IRawStreamProcessors
        // to have a different IRawDataProcessor leading to last received ERT issues

		bytesDiscardedWhilePausedThreshold = rawConfig.getDiscardedBytesThreshold();
		meterInterval = rawConfig.getMeterInterval();
		scid = serviceContext.getBean(IContextIdentification.class).getSpacecraftId();
		
        logger = TraceManager.getTracer(serviceContext, Loggers.TLM_INPUT);

        // BEGIN MPCS-5013 07/03/13

        configuredStation = serviceContext.getBean(IContextFilterInformation.class).getDssId(); 

        // END MPCS-5013
        
        stationInfoFactory = serviceContext.getBean(IStationInfoFactory.class);
        stationHeaderFactory = serviceContext.getBean(IStationHeaderFactory.class);
	}


    @Override
    public void setDataProcessor(final IRawDataProcessor dataProc) {
        if (this.rawDataProc == null) {
            this.rawDataProc = dataProc;
        }
        else {
            logger.debug("Unable to set IRawDataProcessor ", dataProc.getClass().toString(),
                         " because it already exists as ", rawDataProc.getClass().toString());
        }
    }

    /**
     * Get configured station.
     *
     * @return Station id
     */
    public int getConfiguredStation()
    {
        // MPCS-5013 07/03/13 New method

        return configuredStation == null ? StationIdHolder.UNSPECIFIED_VALUE : configuredStation;
    }


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#init(jpl.gds.common.config.types.TelemetryInputType, boolean)
	 */
	@Override
	public void init(final TelemetryInputType inputType, final boolean isRemoteMode) {
		this.inputType = inputType;
		this.isRemoteMode = isRemoteMode;
		this.stopped = false;
		setAwaitingFirstData(true);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#isPaused()
	 */
	@Override
	public boolean isPaused() {
		return this.paused;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#pause()
	 */
	@Override
	public void pause() {
		if (this.paused) {
			return;
		}

		this.paused = true;
		pauseTime = new Date(System.currentTimeMillis());
		bytesDiscarded = 0;

		messenger.sendPauseMessage(pauseTime);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#resume()
	 */
	@Override
	public void resume() {
		if (paused == false) {
			return;
		}

		logger.warn("Processing of raw input is paused: " + bytesDiscarded
		        + " bytes discarded.");
		bytesDiscarded = 0;
		this.paused = false;

		messenger.sendResumeMessage(pauseTime);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#isStopped()
	 */
	@Override
	public boolean isStopped() {
		return this.stopped;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#stop()
	 */
	@Override
	public void stop() {
		if (this.isStopped()) {
			return;
		}

		if (paused == true) {
			logger.warn("Processing of raw input is paused: " + bytesDiscarded
			        + " bytes discarded.");
			bytesDiscarded = 0;
		}

		this.stopped = true;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#awaitingFirstData()
	 */
	@Override
	public boolean awaitingFirstData() {
		return firstData;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#setAwaitingFirstData(boolean)
	 */
	@Override
	public void setAwaitingFirstData(final boolean isFirst) {
		firstData = isFirst;
	}

	/**
	 * Sleep for the length of time specified by the current meter interval.
	 */
	protected void doMetering() {
		// If input is being metered, sleep for the delay
		if (getMeterInterval() > 0) {
			SleepUtilities.fullSleep(getMeterInterval(), logger, "AbstractRawInput.doMetering "
			        + "Error while metering");
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#getMeterInterval()
	 */
	@Override
	public long getMeterInterval() {
		return meterInterval;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#setMeterInterval(long)
	 */
	@Override
	public void setMeterInterval(final long meterInterval) {
		if (meterInterval < 0L) {
			logger.error("Negative input meter interval, using zero");
		}
		this.meterInterval = (Math.max(meterInterval, 0L));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#
	 * setConnectionLost(boolean)
	 */
	@Override
	public void setConnectionLost(final boolean connectionLost) {
		this.connectionLost = connectionLost;
	}


    /**
     * Getter for EOF on stream status.
     *
     * @return Status
     */
    @Override
    public boolean getEofOnStreamStatus()
    {
        // MPCS-5013 07/23/13

        return eofOnStream;
    }


    /**
     * Setter for EOF on stream status.
     *
     * @param status True if EOF detected
     */
    @Override
    public void setEofOnStreamStatus(final boolean status)
    {
        // MPCS-5013 07/23/13

        eofOnStream = status;
    }
}
