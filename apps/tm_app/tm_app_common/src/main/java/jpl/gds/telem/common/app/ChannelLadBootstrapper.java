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

package jpl.gds.telem.common.app;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import jpl.gds.common.config.bootstrap.ChannelLadBootstrapConfiguration;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.eha.api.channel.ChannelLadInitializedEvent;
import jpl.gds.eha.api.channel.IChannelLad;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.alarm.AlarmHistoryInitializedEvent;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.globallad.GlobalLadClientConversionException;
import jpl.gds.globallad.GlobalLadException;
import jpl.gds.globallad.data.EhaGlobalLadData;
import jpl.gds.globallad.data.GlobalLadDataIterator;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.QueryType;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState;
import jpl.gds.globallad.data.utilities.GlobalLadQueryParams;
import jpl.gds.globallad.data.utilities.GlobalLadQueryParams.GlobalLadQueryParamsBuilder;
import jpl.gds.globallad.utilities.ICoreGlobalLadQuery;
import jpl.gds.globallad.utilities.LadChannelValueConverter;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Integration point for the ChannelLAD bootstrap from GlobalLAD
 * 
 * @since R8
 *
 */
@Component(value="ChannelLadBootstrapper")
public class ChannelLadBootstrapper implements ApplicationListener<ChannelLadInitializedEvent>{

	private final TimeComparisonStrategyContextFlag timeFlag;

	private final ApplicationContext appContext;

    private final ChannelLadBootstrapConfiguration  config;

    private final Tracer                            log;

	/**
     * @param timeFlag
     *            TimeComparisonStrategy flag
     * @param appContext
     *            The current application context
     * @param bootstrapConfig
     *            The ChannelLadBootstrapConfiguration
     */
	@Autowired
	public ChannelLadBootstrapper(final TimeComparisonStrategyContextFlag timeFlag, final ApplicationContext appContext, 
			final ChannelLadBootstrapConfiguration bootstrapConfig) {
		super();
		this.timeFlag = timeFlag;
		this.appContext = appContext;
        this.config = bootstrapConfig;
        this.log = TraceManager.getTracer(appContext, Loggers.GLAD);
	}



	@Override
	public void onApplicationEvent(final ChannelLadInitializedEvent ladEvent) {
		/**
		 * Check to see if we need to do the bootstrap.
		 */
        if (!config.getLadBootstrapFlag()) {
            log.info("ChannelLAD boot-strap is not enabled");
			return;
		}

		/**
		 * Figure out the time strategy for the queries.
		 */
		GlobalLadPrimaryTime gt;
		switch(timeFlag.getTimeComparisonStrategy()){
		case LAST_RECEIVED:
			gt = GlobalLadPrimaryTime.EVENT;
			break;
		case SCET:
			gt = GlobalLadPrimaryTime.SCET;
			break;
		case SCLK:
			gt = GlobalLadPrimaryTime.SCLK;
			break;
		case ERT:
		default:
			gt = GlobalLadPrimaryTime.ERT;
			break;
		}

        final Integer vcid = appContext.getBean(IContextConfiguration.class).getFilterInformation().getVcid();
        final Integer scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
        final String host = appContext.getBean(IContextIdentification.class).getHost();
        final Collection<Long> sids = config.getLadBootstrapIds();

        log.info(Markers.GENERAL, "Attemping to fetch ChannelLAD for sessions ", sids.toString(), " on host ", host,
                 " for spacecraft id ", scid, " with vcid (", vcid, ") and timeType ", gt);

        /**
         * Set the recorded state so we get all channel types and
         */
        final GlobalLadQueryParamsBuilder queryParamsBuilder = GlobalLadQueryParams.createBuilder()
                                                                                   .setQueryType(QueryType.eha)
                                                                                   .setRecordedState(RecordedState.both)
                                                                                   .setSource(DataSource.all)
                                                                                   .setTimeType(gt)
                                                                                   .setVcids(vcid)
                                                                                   .setScid(scid)
                                                                                   .setSessionIds(sids)
                                                                                   .setHostRegex(host)
                                                                                   .setMaxResults(1);

		final LadChannelValueConverter converter = new LadChannelValueConverter(appContext);
		final ICoreGlobalLadQuery query = appContext.getBean(ICoreGlobalLadQuery.class);
		final GlobalLadDataIterator iterator =  query.getLadQueryIterator(queryParamsBuilder, false);
		
		final IChannelLad lad = ladEvent.getLad();
        int count = 0;
		try {
			while(iterator.hasNext()) {
				final IGlobalLADData d = iterator.next();

				if (d instanceof EhaGlobalLadData) {
                    final IServiceChannelValue e = converter.convert((EhaGlobalLadData) d);
                    lad.addNewValue(e);
                    log.trace("Retrieved ", e.toString(), " channel value");
                    count++;
				} else {
					throw new GlobalLadException("Global lad data object was not the expected EHA data type.");
				}
			}
		} catch (final IllegalArgumentException e) {
            log.error("Failed to boot-strap channel lad due to an EHA issue: " + e.getLocalizedMessage());
		} catch (final GlobalLadClientConversionException e) {
            log.error("Failed to boot-strap channel lad due to a conversion issue: " + e.getLocalizedMessage());
		} catch (final GlobalLadException e) {
            log.error("Failed to boot-strap channel lad due to an unknown Global LAD issue: "
                    + e.getLocalizedMessage());
		} finally {
            log.info("Bootstrapped ", count, " channel(s) from the Global LAD.", 
                     " Triggering alarm history event to bootstrap alarms.");
            /*
             * Now send an event to bootstrap the alarm history.
             */
            final IAlarmHistoryProvider alarmHistory = appContext.getBean(IAlarmHistoryProvider.class);
            appContext.publishEvent(new AlarmHistoryInitializedEvent(this, alarmHistory));

		}
	}


}
