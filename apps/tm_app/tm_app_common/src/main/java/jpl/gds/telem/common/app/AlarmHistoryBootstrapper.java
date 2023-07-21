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
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.alarm.AlarmHistoryInitializedEvent;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryFactory;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.globallad.data.AlarmHistoryGlobalLadData;
import jpl.gds.globallad.data.GlobalLadDataIterator;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.QueryType;
import jpl.gds.globallad.data.utilities.GlobalLadQueryParams;
import jpl.gds.globallad.data.utilities.GlobalLadQueryParams.GlobalLadQueryParamsBuilder;
import jpl.gds.globallad.rest.resources.ResourceUris;
import jpl.gds.globallad.utilities.CoreGlobalLadQuery;
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
@Component(value="ALARM_HISTORY_BOOTSTRAPPER")
public class AlarmHistoryBootstrapper implements ApplicationListener<AlarmHistoryInitializedEvent>{

	private final ApplicationContext appContext;
	private final ChannelLadBootstrapConfiguration  config;
	private final Tracer                            log;
	private final IChannelDefinitionProvider chanDict;
	private final IAlarmHistoryFactory historyFactory;

	/**
	 * @param timeFlag
	 *            TimeComparisonStrategy flag
	 * @param query
	 *            GlobalLAD query
	 * @param appContext
	 *            The current application context
	 * @param bootstrapConfig
	 *            The ChannelLadBootstrapConfiguration
	 */
	@Autowired
	public AlarmHistoryBootstrapper(final IAlarmHistoryFactory historyFactory, final IChannelDefinitionProvider chanDict,
			final ApplicationContext appContext, final ChannelLadBootstrapConfiguration bootstrapConfig) {
		super();
		this.historyFactory = historyFactory;
		this.chanDict = chanDict;
		this.appContext = appContext;
		this.config = bootstrapConfig;
        this.log = TraceManager.getTracer(appContext, Loggers.GLAD);
	}
	
	/**
	 * Entry point for testing.
	 * 
	 * @param queryParamsBuilder
	 * @return
	 */
	public GlobalLadDataIterator getIterator(final GlobalLadQueryParamsBuilder queryParamsBuilder) {
		final CoreGlobalLadQuery query = appContext.getBean(CoreGlobalLadQuery.class);
		return query.getLadQueryIterator(ResourceUris.alarmHistoryURI, queryParamsBuilder, false);
	}

	@Override
	public void onApplicationEvent(final AlarmHistoryInitializedEvent alarmEvent) {
		/**
		 * Check to see if we need to do the bootstrap.
		 */
		if (!config.getLadBootstrapFlag()) {
			log.info("Alarm History boot-strap is not enabled");
			return;
		}

		final String venue = appContext.getBean(IVenueConfiguration.class).getVenueType().toString();
		final String host = appContext.getBean(IContextIdentification.class).getHost();
        final Collection<Long> sids = config.getLadBootstrapIds();

        final GlobalLadQueryParamsBuilder queryParamsBuilder = GlobalLadQueryParams.createBuilder()
                                                                                   .setQueryType(QueryType.alarm)
                                                                                   .setVenueRegex(venue)
                                                                                   .setHostRegex(host)
                                                                                   .setSessionIds(sids);
        log.info(Markers.GENERAL, "Attemping to fetch Alarm history for sessions ", sids.toString(), " on host ", host,
                 " in ", venue, " venue");

		final GlobalLadDataIterator iterator = getIterator(queryParamsBuilder); 
        /**
         * For all histories we get back, merge them into the history we have.
         */
        final IAlarmHistoryProvider history = alarmEvent.getAlarmHistory();
        while (iterator.hasNext()) {
            final IGlobalLADData ladHistory = iterator.next();

            if (ladHistory instanceof AlarmHistoryGlobalLadData) {
                final IAlarmHistoryProvider ah = historyFactory.createAlarmHistory(((AlarmHistoryGlobalLadData) ladHistory).getAlarmHistory(),
                                                                                       chanDict, log);
                history.merge(ah);
                log.info(Markers.GENERAL, "Merged alarm history with identifier ", ladHistory.getIdentifier());
            }
            else {
                log.error("Expected alarm history but received a different type of global lad data: ladHistory=",
                              ladHistory, ",type=", (ladHistory != null ? ladHistory.getClass() : "UNKNOWN"));
            }
        }

	}
}
