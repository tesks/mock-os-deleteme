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
package jpl.gds.eha.impl.service.channel;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.types.EhaBool;
import jpl.gds.common.types.RecordedBool;
import jpl.gds.decom.DecomEngine;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDefinitionProvider;
import jpl.gds.eha.api.service.channel.IDecomListenerFactory;
import jpl.gds.eha.api.service.channel.IHybridGenericPacketDecomService;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.evr.api.message.IEvrMessageFactory;
import jpl.gds.evr.api.service.extractor.IEvrExtractorUtility;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * This extension of the GenericPacketDecomService publishes EVRs in addition to
 * channel values. EVR handling is kept in a separate class from channel handling
 * in order to separate the two capabilities as much as possible. 
 *
 */
public class HybridGenericPacketDecomService extends GenericPacketDecomService implements IHybridGenericPacketDecomService {
	
    private boolean badListenerDetected = false;

    /** Map of EVR ID to definition. */
    protected Map<Long, IEvrDefinition> evrDefinitionMap;
    /** EVR extractor utility instance */
    protected IEvrExtractorUtility extractorUtil;

    private final IEvrMessageFactory    evrMessageFactory;
    

    /**
     * Create a service instance.
     * 
     * @param context
     *            the current application context
     */
    public HybridGenericPacketDecomService(final ApplicationContext context) {
		super(context);
		
		evrDefinitionMap = context.getBean(IEvrDefinitionProvider.class).getEvrDefinitionMap();
		extractorUtil = appContext.getBean(IEvrExtractorUtility.class);
        evrMessageFactory = appContext.getBean(IEvrMessageFactory.class);
	}

	@Override
	public void initializeDecom() {
	    final IDecomListenerFactory listenFactory = appContext.getBean(IDecomListenerFactory.class);
        decomListener = listenFactory.createHybridEvrChannelizationListener(appContext);
		decomEngine = new DecomEngine(appContext, decomListener);
        decomEngine.addListener(decomListener);
	}

	@Override
	protected void handlePacketMessage(final ITelemetryPacketMessage pm) {
		super.handlePacketMessage(pm);
		if (badListenerDetected) {
			return;
		}
		if (decomListener instanceof HybridChannelEvrListener) {
			
			final List<IEvr> evrList = ((HybridChannelEvrListener) decomListener).collectEvrs();

			if (!evrList.isEmpty()) {
				for (final IEvr evr : evrList) {
                    final IEvrMessage evrMessage = evrMessageFactory.createEvrMessage(evr);
					evr.setFromSse(pm.getPacketInfo().isFromSse());
                    evr.setRct(new AccurateDateTime());
					evr.setErt(pm.getPacketInfo().getErt());
					evr.setVcid(pm.getPacketInfo().getVcid());

                    evr.setPacketId(pm.getPacketId());

                    final ITelemetryPacketInfo pi = pm.getPacketInfo();
					final RecordedBool state = rtRec.getState(EhaBool.EVR,  pi.getApid(), pi.getVcid(), pi.isFromSse());

					evrMessage.getEvr().setRealtime(! state.get());

					//if (trace.isEnabledFor(TraceSeverity.TRACE)) {
					trace.trace("EVR in publisher is " + evr.toString());
					//}
					messageContext.publish(evrMessage);    
				}
			}
		} else {
			trace.warn("Generic packet decom service can publish EVRs, but is configured to only publish channels");
			badListenerDetected = false;

		}
	}
}
