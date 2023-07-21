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
package jpl.gds.telem.common.feature;

import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.config.CcsdsProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.service.telem.AbstractTelemetryFeatureManager;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.dictionary.api.client.frame.ITransferFrameUtilityDictionaryManager;
import jpl.gds.dictionary.api.config.IFrameFormatDefinition.TypeName;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.tm.service.api.cfdp.IPduExtractService;

/**
 * Class PduExtractionFeatureManager
 */
public class PduExtractionFeatureManager extends AbstractTelemetryFeatureManager {

    IContextConfiguration contextConfig;

    @Override
    public boolean init(final ApplicationContext springContext) {

        contextConfig = springContext.getBean(IContextConfiguration.class);

        log = TraceManager.getTracer(springContext, Loggers.PDU_EXTRACTOR);
        setValid(false);
        if (!this.isEnabled()) {
            return true;
        }

        setValid(true);

        String type = null;

        final TelemetryInputType inputType = contextConfig.getConnectionConfiguration().getDownlinkConnection()
                                                          .getInputType();
        if (inputType.hasFrames()) {
            for (final ITransferFrameDefinition frameDef : springContext.getBean(ITransferFrameUtilityDictionaryManager.class)
                                                                        .getFrameDefinitions()) {
                final TypeName tn = frameDef.getFormat().getType();
                // missions don't mix V1 and V2 frames, but check to make sure they don't use V2_BPDU and any future
                // frame types that aren't supported yet
                if (tn.equals(TypeName.CCSDS_TM_1) || tn.equals(TypeName.CCSDS_AOS_2_MPDU)) {
                    type = tn.toString();
                    break;
                }
            }
        }
        else {
            type = springContext.getBean(CcsdsProperties.class).getPacketHeaderFormat().getType().toString();
        }
        final List<Integer> vcids = springContext.getBean(MissionProperties.class).getCfdpPduExtractVcids();

        for (final int vcid : vcids) {
            addService(springContext.getBean(IPduExtractService.class, type, vcid));
        }

        setValid(startAllServices());

        if (this.isValid()) {
            log.debug("Pdu Extraction feature susccessfully initialized.");
        }

        return isValid();
    }

}
