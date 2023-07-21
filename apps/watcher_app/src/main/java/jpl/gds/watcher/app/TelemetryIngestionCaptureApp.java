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
package jpl.gds.watcher.app;

import org.apache.commons.cli.ParseException;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.filtering.DssIdFilter;
import jpl.gds.common.filtering.VcidFilter;
import jpl.gds.common.options.DssIdListOption;
import jpl.gds.common.options.VcidListOption;
import jpl.gds.message.api.util.MessageFilterMaker;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.watcher.app.handler.ITelemetryIngestionCaptureHandler;

/**
 * Shared abstract capture application for telemetry ingestion product (eg: packet and frame) messages
 * messages
 */
public abstract class TelemetryIngestionCaptureApp extends AbstractCaptureApp {
    /** restoreBodies option */
    public static final String RESTORE_BODIES        = "restoreBodies";

    private final FlagOption   includeStationInfoOpt = new FlagOption(null, RESTORE_BODIES,
                                                                      "Restore headers and trailers to bodies for stored data");
    private VcidListOption     vcidsOption;
    private DssIdListOption    dssIdsOption;

    /** The vcid to be used as part of the message filtering */
    protected VcidFilter       vcFilter;
    /** The dssId to be used as part of the message filtering */
    protected DssIdFilter      dssFilter;

    private boolean            restoreBodies;

    /**
     * Constructor
     * 
     * @param msgType
     *            the type of messages the capture client will be handling
     * @param captureType
     *            The name of the message type, and subtopic, to be received.
     * @param useOutputFormatOption
     *            enable the output format option if the message type to be captured has a template
     *            directory.
     */
    public TelemetryIngestionCaptureApp(final TmServiceMessageType msgType, final String captureType,
            final boolean useOutputFormatOption) {
        super(msgType, captureType, useOutputFormatOption);
    }

    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }

        super.createOptions();

        options.addOption(includeStationInfoOpt);
        vcidsOption = new VcidListOption(false, false, appContext.getBean(MissionProperties.class));
        options.addOption(vcidsOption);

        dssIdsOption = new DssIdListOption(false, true, appContext.getBean(MissionProperties.class));
        options.addOption(dssIdsOption);

        return options;
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {

        super.configure(commandLine);

        final ITelemetryIngestionCaptureHandler castedCapture = (ITelemetryIngestionCaptureHandler) capture;

        restoreBodies = includeStationInfoOpt.parse(commandLine);
        castedCapture.setRestoreBodies(restoreBodies);

        if (capture.isCaptureMessages() && castedCapture.isRestoreBodies()) {
            log.warn("Both " + captureMessagesOpt.getLongOpt() + " and " + includeStationInfoOpt.getLongOpt()
                    + " have been specified. " + captureMessagesOpt.getLongOpt()
                    + " will be used as captured messages contain all header and trailer information.");
        }

        vcFilter = vcidsOption.parse(commandLine);

        dssFilter = dssIdsOption.parse(commandLine);

        if (vcFilter != null || dssFilter != null) {
            final String internalFilter = MessageFilterMaker.createSubscriptionFilter(null, null, dssFilter, vcFilter);
            log.info("Creating internal message filter '" + internalFilter + "'");
            castedCapture.setVcidFilter(vcFilter);
            // The dssId filter also goes to the TelemetryIngestionCaptureHandler
            castedCapture.setDssIdFilter(dssFilter);
        }
    }

    // package private getters to use for tests

    boolean isRestoreBodies() {
        return restoreBodies;
    }

}
