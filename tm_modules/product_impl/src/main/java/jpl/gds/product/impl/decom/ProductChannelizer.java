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
package jpl.gds.product.impl.decom;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.RealtimeRecordedConfiguration;
import jpl.gds.common.config.mission.RealtimeRecordedConfiguration.StrategyEnum;
import jpl.gds.common.types.RecordedBool;
import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.service.channel.IChannelPublisherUtility;
import jpl.gds.product.api.decom.ProductDecomTimestampType;
import jpl.gds.product.api.message.InternalProductMessageType;
import jpl.gds.product.impl.message.ProductChannelTimeMessage;
import jpl.gds.product.impl.message.ProductChannelValueMessage;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.CoarseFineEncoding;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUnit;

/**
 * The ProductChannelizer class is a base class for use in developing mission-specific product
 * channelization capability.  It listens for Product Channel messages and keeps a list of them,
 * and then sends out generic channel messages for each value when triggered to do so.

 *
 */
public abstract class ProductChannelizer implements MessageSubscriber
{
    private final RecordedBool markingState;

	private List<TaggedChannelValue> channelList;
	private List<ProductChannelTimeMessage> timeMessages;
	private final CoarseFineEncoding sclkEncoding;
	/** spacecraft ID */
	protected final int scid;
	/** Flag for setting LST times */
	protected boolean setSolTimes = false;
	/** Channel publisher utility instance */
	protected IChannelPublisherUtility pubUtil;
	/** Current publication context */
	protected ApplicationContext appContext;
	/** INternal publication bus to use */
	protected IMessagePublicationBus bus;
    /** Product decom Tracer to log with */
    protected final Tracer                  tracer;


    /**
	 * Constructs a new ProductChannelizer.
     * @param appContext the current application context
	 */
	public ProductChannelizer(final ApplicationContext appContext) {
	    this.appContext = appContext;
	    bus = appContext.getBean(IMessagePublicationBus.class);
        tracer = TraceManager.getTracer(appContext, Loggers.PRODUCT_DECOM);
	    sclkEncoding = TimeProperties.getInstance().getCanonicalEncoding();
	    
    	scid =  appContext.getBean(IContextIdentification.class).getSpacecraftId();
	  	setSolTimes = appContext.getBean(EnableLstContextFlag.class).isLstEnabled();
		pubUtil = appContext.getBean(IChannelPublisherUtility.class);
	  	

        boolean rt = false;

        try
        {
            final RealtimeRecordedConfiguration rtRecConfig = 
            		appContext.getBean(RealtimeRecordedConfiguration.class);

            // It's real-time only if UNCONDITIONAL and REALTIME
            /** 02/11/14 Always enabled */

            rt = ((rtRecConfig.getTelemetryMarkingStrategy() == StrategyEnum.UNCONDITIONAL) &&
                  ! rtRecConfig.getTelemetryUnconditionalMarking().get());
        }
        catch (final Exception de)
        {
            tracer.error(
                    "ProductChannelizer Unable to get marking state, assuming recorded: ", de.getLocalizedMessage(),
                    de.getCause());

            rt = false;
        }

        markingState = RecordedBool.valueOf(! rt);
	}


	/**
	 *  Starts receipt of product channel messages.
	 */
	public void startMessageReceipt() {
		bus.subscribe(InternalProductMessageType.ProductChannelValue, this);
		bus.subscribe(InternalProductMessageType.ProductChannelTime, this);
	}

	/**
	 * Stops receipt of product channel messages.
	 */
	public void stopMessageReceipt() {
		bus.unsubscribeAll(this);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleMessage(final IMessage m) {
		if (m.isType(InternalProductMessageType.ProductChannelValue)) {
			if (channelList == null) {
				channelList = new ArrayList<TaggedChannelValue>();
			}
			final TaggedChannelValue taggedVal = new TaggedChannelValue(((ProductChannelValueMessage)m).getChannelValue());
			channelList.add(taggedVal);
			timetagChannel(taggedVal);
		} else {
			if (timeMessages == null) {
				timeMessages = new ArrayList<ProductChannelTimeMessage>(1);
			}
			timeMessages.add((ProductChannelTimeMessage)m);
		}
	}

	private void timetagChannel(final TaggedChannelValue val) {
		if (val.isTagged()) {
			return;
		}
		if (timeMessages != null) {
			ISclk sclk = null;
			IAccurateDateTime ert = null;
			IAccurateDateTime scet = null;
			ILocalSolarTime sol = null;

			ProductChannelTimeMessage baseTimeMessage = null;

			// Find the last BASE or ABSOLUTE time message
			for (int i = timeMessages.size() - 1; i >= 0; i--) {
				if (timeMessages.get(i).getTimeType().equals(ProductDecomTimestampType.BASE) ||
						timeMessages.get(i).getTimeType().equals(ProductDecomTimestampType.ABSOLUTE)) {
					baseTimeMessage = timeMessages.get(i);
					break;
				}
			}

			if (baseTimeMessage == null) {
				tracer.warn("No ABSOLUTE or BASE time field found to match DELTA field in channelized product");
				return;
			}

			ProductChannelTimeMessage deltaMessage = null;

			// If we have a BASE time and have more than one time, look for the latest DELTA time
			if (baseTimeMessage.getTimeType().equals(ProductDecomTimestampType.BASE) && timeMessages.size() > 1) {
				// Find the last BASE or ABSOLUTE time message
				for (int i = timeMessages.size() - 1; i >= 0; i--) {
					if (timeMessages.get(i).getTimeType().equals(ProductDecomTimestampType.DELTA)) {
						deltaMessage = timeMessages.get(i);
						break;
					}
				}	
			}

			if (baseTimeMessage.isSclk()) {
				sclk = new Sclk(timeMessages.get(0).getSclkTime());
			} else {
				ert = timeMessages.get(0).getIsoTime();
			}

			if (deltaMessage != null) {

				if (baseTimeMessage.isSclk() && deltaMessage.isSclk()) {
					final ISclk deltaSclk = deltaMessage.getSclkTime();
					sclk = sclk.increment(deltaSclk.getCoarse(), deltaSclk.getFine());
				} else if (baseTimeMessage.isSclk() && !deltaMessage.isSclk() && deltaMessage.getUnit().equals(TimeUnit.MS)) {
					final IAccurateDateTime deltaMs = deltaMessage.getIsoTime();
					final ISclk deltaSclk = Sclk.sclkFromMillis(deltaMs.getTime());
					sclk = sclk.increment(deltaSclk.getCoarse(), deltaSclk.getFine());
				} else if (!baseTimeMessage.isSclk() && !deltaMessage.isSclk() && deltaMessage.getUnit().equals(TimeUnit.MS)) {
					final IAccurateDateTime deltaErt = deltaMessage.getIsoTime();
					ert = new AccurateDateTime(ert.getTime() + deltaErt.getTime());
				} else {
					tracer.warn("BASE time field units do not match DELTA field units in channelized product");
					return;
				}
			}

			if (ert != null) {
				val.getValue().setErt(ert);
			}
			if (sclk != null) {
				// SCLK is bigger than standard Mission SCLK. Must be rounded for use in channel messages.
				if (sclk.getByteLength() > sclkEncoding.getByteLength()) {
					// 05/31/2016: Don't use setCoarse / setFine methods.
					// TODO the logic shouldn't be here, but I can't unwind this stuff right now
					final ISclk smallerSclk = new Sclk();
					long fine = sclk.getFine();
					if (fine <= smallerSclk.getFineUpperLimit()) {
					} else {
						while (fine >= smallerSclk.getFineUpperLimit()) {
							final double doubleFine = fine / 10.0;
							fine = Math.round(doubleFine);
						}
					}
					sclk = new Sclk(sclk.getCoarse(), fine);
				}
                final IAccurateDateTime newScet = SclkScetUtility.getScet(sclk, val.getValue().getErt(), scid, tracer);
				if (newScet != null) {
					scet = newScet;
					if (setSolTimes) {
						sol = LocalSolarTimeFactory.getNewLst(scet, scid);
					}
				}
				val.getValue().setSclk(sclk);
				val.getValue().setScet(scet);
				val.getValue().setLst(sol);
			}
			if (ert != null) {
				val.getValue().setErt(ert);
			}
			val.setTagged(true);
		} 
	}

	/**
	 * Sends out generic channel value messages for every product channel value on the current list.
	 * @param sclk SCLK to attach to channel values
	 * @param ert ERT to attach to channel values
	 * @param scet SCET to attach to channel values
	 * @param sol LST to attach to channel values
	 * @param productId unique ID of the source product
	 * @param scid the current spacecraft ID
	 * @param dssId the ID of the station on which the product was received
	 * @param vcid the ID of the virtual channel on which the product was received
	 */
	protected void processChannelValues(final ISclk sclk, final IAccurateDateTime ert, IAccurateDateTime scet, ILocalSolarTime sol, 
			final String productId, final int scid, final int dssId, final int vcid) {
		if (channelList == null) {
			return;
		}
        final IAccurateDateTime rct = new AccurateDateTime();

		final ArrayList<IServiceChannelValue> updatedChannelValues = new ArrayList<IServiceChannelValue>(channelList.size());
		for (final TaggedChannelValue val: channelList) {
			if (!val.isTagged()) {
				timetagChannel(val);
				if (!val.isTagged()) {
					val.getValue().setSclk(sclk);
					val.getValue().setErt(ert);
				}
			}
            final IAccurateDateTime newScet = SclkScetUtility.getScet(val.getValue().getSclk(), val.getValue().getErt(),
                                                                      scid, tracer);
			if (newScet != null) {
				scet = newScet;
				if (setSolTimes) {
					sol = LocalSolarTimeFactory.getNewLst(scet, scid);
				}
			}

            val.getValue().setRealtime(! markingState.get());

			updatedChannelValues.add(val.getValue());
		}

		/*
		 * Using the new, wrapped publishing API.
		 */
		pubUtil.publishFlightAndDerivedChannels(false,
				updatedChannelValues, rct, ert, scet, sclk, sol, productId,
				false, dssId, vcid, null);
	}


	/**
	 * This class tracks a channel value generated by the channelizer and a flag that indicates 
	 * whether it has been time-tagged.
	 * 
	 */
	private static class TaggedChannelValue {
		private boolean tagged = false;
		IServiceChannelValue value;

		public TaggedChannelValue(final IServiceChannelValue val) {
			value = val;
		}

		public boolean isTagged() {
			return tagged;
		}

		public void setTagged(final boolean tagged) {
			this.tagged = tagged;
		}

		public IServiceChannelValue getValue() {
			return value;
		}



	}
}
