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
package jpl.gds.telem.down.app.mc.rest.controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import jpl.gds.common.config.ConfigurationDumpUtility;
import jpl.gds.context.api.EnableSseDownlinkContextFlag;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.session.config.SessionConfigurationUtilities;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.performance.HealthStatus;
import jpl.gds.shared.performance.HeapPerformanceData;
import jpl.gds.shared.performance.PerformanceSummaryMessage;
import jpl.gds.shared.performance.ProviderPerformanceSummary;
import jpl.gds.telem.common.app.mc.rest.resources.DownlinkStatusResource;
import jpl.gds.telem.down.IDownlinkApp;
import jpl.gds.telem.down.app.mc.rest.resources.TelemetrySummaryDelegateResource;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.IFrameSummaryMessage;
import jpl.gds.tm.service.api.packet.IPacketSummaryMessage;

/**
 * Restful Downlink Status
 *
 */
@RestController
@ResponseBody
@RequestMapping("/status")
@Api(value = "status")
public class RestfulDownlinkStatus implements MessageSubscriber {
    private final ApplicationContext               appContext;
    private final IDownlinkApp                     app;
    private final IMessagePublicationBus           bus;
    private final PerformanceProperties            perfProps;
    private final DownlinkStatusResource           downStatus;
    private final TelemetrySummaryDelegateResource telemStatus;

    /**
     * @param appContext
     *            the Spring Application Context
     */
    @Autowired
    public RestfulDownlinkStatus(final ApplicationContext appContext) {
        super();
        this.appContext = appContext;
        this.app = appContext.getBean(IDownlinkApp.class);
        this.downStatus = new DownlinkStatusResource(appContext);
        this.telemStatus = new TelemetrySummaryDelegateResource(app);
        this.perfProps = appContext.getBean(PerformanceProperties.class);
        this.bus = appContext.getBean(IMessagePublicationBus.class);
        this.bus.subscribe(TmServiceMessageType.TelemetryFrameSummary, this);
        this.bus.subscribe(TmServiceMessageType.TelemetryPacketSummary, this);
        this.bus.subscribe(CommonMessageType.PerformanceSummary, this);
        this.bus.subscribe(SessionMessageType.EndOfSession, this);
    }

    /**
     * @param regExOrNull
     *            a regular expression with which to filter results
     * @param includeDescriptionsOrNull
     *            if true, show descriptions, if false or null, do not show descriptions
     * @param includeSystemOrNull
     *            if true, show System properties, if false or null, do not show System properties
     * @param includeTemplateDirsOrNull
     *            if true, show Template Directories, if false or null, do not show Template Directories
     * @return a Map of property objects that satisfy the request
     */
    @GetMapping(value = "properties", produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Displays an optionally filtered list of currently active downlink properties.", tags = "properties")
    @ApiResponse(code = 200, message = "Status Query Successfully")
    public Map<String, String> getPropertiesStatus(@RequestParam(value = "filter", required = false) final String regExOrNull,
                                                   @RequestParam(value = "includeDescriptions", required = false) final Boolean includeDescriptionsOrNull,
                                                   @RequestParam(value = "includeSystem", required = false) final Boolean includeSystemOrNull,
                                                   @RequestParam(value = "includeTemplateDirs", required = false) final Boolean includeTemplateDirsOrNull) {

        final boolean includeDescriptions = (includeDescriptionsOrNull != null) ? includeDescriptionsOrNull.booleanValue() : false;
        final boolean includeSystem = (includeSystemOrNull != null) ? includeSystemOrNull.booleanValue() : false;
        final boolean includeTemplateDirs = (includeTemplateDirsOrNull != null) ? includeTemplateDirsOrNull.booleanValue() : false;
        return new ConfigurationDumpUtility(appContext).collectProperties(regExOrNull, includeSystem,
                                                                          includeTemplateDirs,
                                                                          includeDescriptions
                                                                                  ? GdsHierarchicalProperties.PropertySet.INCLUDE_DESCRIPTIVES
                                                                                  : GdsHierarchicalProperties.PropertySet.NO_DESCRIPTIVES);
    }

    /**
     * @return the Current state of the Local Downlink LAD
     */
    @GetMapping(value = "dumplad", produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Displays the contents of the internal downlink Latest Active Data (LAD).", tags = "dumplad")
    @ApiResponse(code = 200, message = "Status Query Successfully")
    public Object dumpLad() {
        return new Object() {
            @SuppressWarnings("unused")
            public String getLadCsv() {
                return app.getLadContentsAsString();
            }
        };
    }

    /**
     * @return the Current Telemetry Summary POJO
     */
    @GetMapping(value = "cmdline", produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Displays the command line used to invoke the currently running downlink process.", tags = "cmdline")
    @ApiResponse(code = 200, message = "Status Query Successfully")
    public Object getCommandLine() {
        final StringBuilder sb = new StringBuilder();
        if (appContext.getBean(EnableSseDownlinkContextFlag.class).isSseDownlinkEnabled()) {
            sb.append("sse_");
        }
        sb.append(app.getAppName());
        for (final String arg : appContext.getBean(ApplicationArguments.class).getSourceArgs()) {
            sb.append(' ').append(arg);
        }
        return new Object() {
            @SuppressWarnings("unused")
            public String getCommandLine() {
                return sb.toString();
            }
        };
    }

    /**
     * @return the Current Telemetry Summary POJO
     */
    @GetMapping(value = "telem", produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Displays a summary of telemetry processing statistics.", tags = "telem")
    @ApiResponse(code = 200, message = "Status Query Successfully")
    public TelemetrySummaryDelegateResource getTelemetryStatus() {
        telemStatus.setTelemetrySummary(app.getSessionSummary());
        return telemStatus;
    }

    /**
     * @return the Current Downlink Status POJO
     */
    @GetMapping(value = "perf", produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Displays a summary of performance and memory usage data, along with warnings when low resources threaten to cause failure.", tags = "perf")
    @ApiResponse(code = 200, message = "Status Query Successfully")
    public DownlinkStatusResource getPerformanceStatus() {
        return this.downStatus;
    }

    /**
     * @return the Current Session Status POJO
     */
    @GetMapping(value = "session", produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Displays a summary of current session parameters", tags = "session")
    @ApiResponse(code = 200, message = "Status Query Successfully")
    public Map<String, String> getSessionConfiguration() {
        final IContextConfiguration contextConfig = app.getContextConfiguration();
        final SessionConfigurationUtilities sessionConfigUtil = new SessionConfigurationUtilities(appContext);
        final Map<String,Object> objectMap = sessionConfigUtil.assembleSessionConfigData(contextConfig);

        // transform to Map<String,String>
        final Map<String, String> stringMap = new LinkedHashMap<>();
        for(Map.Entry<String, Object> entry : objectMap.entrySet()){
            if(entry.getValue() != null) {
                stringMap.put(entry.getKey(), entry.getValue().toString());
            }
        }

        return stringMap;
    }
    
    @Override
    public void handleMessage(final IMessage message) {
        try {
            if (message instanceof IFrameSummaryMessage) {
                handleFrameSumMessage((IFrameSummaryMessage) message);
            }
            else if (message instanceof IPacketSummaryMessage) {
                handlePacketSumMessage((IPacketSummaryMessage) message);
            }
            else if (message instanceof PerformanceSummaryMessage) {
                handlePerformanceSumMessage((PerformanceSummaryMessage) message);
            }
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles a performance summary message.
     * 
     * @param msg
     *            the message to handle
     * 
     */
    private void handlePerformanceSumMessage(final PerformanceSummaryMessage msg) {
        /*
         * If we have no message (at startup) then
         * set health status indicator based upon heap status only.
         */
        HealthStatus status = HealthStatus.GREEN;
        if (msg == null) {
            status = new HeapPerformanceData(perfProps).getHealthStatus();
        }
        else {
            status = msg.getOverallHealth();
        }
        this.downStatus.setHeapHealth(status);
        if (msg != null) {
            HeapPerformanceData heapPerfData = msg.getHeapStatus();
            if (heapPerfData == null) {
                heapPerfData = new HeapPerformanceData(perfProps);
            }
            this.downStatus.setHeapPerfData(heapPerfData);
            final Map<String, ProviderPerformanceSummary> provMap = msg.getPerformanceData();
            for (final ProviderPerformanceSummary sum : provMap.values()) {
                this.downStatus.setPerformanceData(sum);
            }
        }
    }

    private void handleFrameSumMessage(final IFrameSummaryMessage msg) {
        this.telemStatus.setNumberOfFrames(msg.getNumFrames());
        this.telemStatus.setNumberOfOutOfSyncFrames(msg.getOutOfSyncCount());
        this.telemStatus.setBitrate(msg.getBitrate());
    }

    private void handlePacketSumMessage(final IPacketSummaryMessage msg) {
        this.telemStatus.setNumberOfValidPackets(msg.getNumValidPackets());
        this.telemStatus.setNumberOfInvalidPackets(msg.getNumInvalidPackets());
        this.telemStatus.setNumberOfFillPackets(msg.getNumFillPackets());
        this.telemStatus.setNumberOfFrameRepeats(msg.getNumFrameRepeats());
        this.telemStatus.setNumberOfFrameGaps(msg.getNumFrameGaps());
        this.telemStatus.setNumberOfStationPackets(msg.getNumStationPackets());
        this.telemStatus.setNumberOfCfdpPackets(msg.getNumCfdpPackets());
    }
}
