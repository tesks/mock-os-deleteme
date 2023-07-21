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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.session.config.gui.SessionConfigShell;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.telem.common.app.mc.DownlinkProcessingState;
import jpl.gds.telem.down.IDownlinkApp;
import jpl.gds.telem.down.gui.AbstractDownShell;

/**
 * Restful Downlink Controller
 *
 */
@RestController
@ResponseBody
@RequestMapping("/control")
@Api(value = "control")
public class RestfulDownlinkControl {
    private static final String DOWNLINK_PROCESSING_STARTED  = "DOWNLINK PROCESSING STARTED";
    private static final String DOWNLINK_PROCESSING_STOPPED  = "DOWNLINK PROCESSING STOPPED";
    private static final String DOWNLINK_PROCESSING_PAUSED   = "DOWNLINK PROCESSING PAUSED";
    private static final String DOWNLINK_PROCESSING_RESUMED  = "DOWNLINK PROCESSING RESUMED";
    private static final String DOWNLINK_ABORTED             = "DOWNLINK ABORTED";
    private static final String DOWNLINK_STOPPED_AND_EXITING = "DOWNLINK STOPPED AND EXITING";
    private static final String NOT_IMPLEMENTED              = "NOT IMPLEMENTED";
    private static final String ALREADY_STARTED              = "ALREADY STARTED";
    private static final String CANNOT_RESTART               = "CANNOT RESTART";
    private static final String ALREADY_STOPPED              = "ALREADY STOPPED";
    private static final String ALREADY_PAUSED               = "ALREADY PAUSED";
    private static final String NOT_RUNNING                  = "NOT RUNNING";
    private static final String NOT_PAUSED                   = "NOT PAUSED";
    private static final String LAD_CLEARED                  = "LAD CLEARED";
    private static final String LAD_NOT_CLEARED              = "LAD NOT CLEARED";
    private static final String LAD_SAVED                    = "LAD SAVED";
    private static final String LAD_NOT_SAVED                = "LAD NOT SAVED";
    private static final String BIND_SUCCESSFUL              = "BIND SUCCESSFUL";
    private static final String BIND_FAILED                  = "BIND FAILED";
    private static final String DOWN_SHELL_TIMED_OUT         = "DOWN_SHELL_TIMED_OUT";
    private static final String SWT_FAILURE                  = "SWT_FAILURE";
    private static final String INTERNAL_SERVER_ERROR        = "Internal Server Error";

    private static final int    MAX_RETRIES                  = 20;
    private static final int    RETRY_DELAY                  = 400;                                                                                                                                                                                                                                                                                                                // milliseoconds

    private final IDownlinkApp  app;

    /**
     * @param appContext
     *            the Spring Application Context
     */
    @Autowired
    public RestfulDownlinkControl(final ApplicationContext appContext) {
        super();
        this.app = appContext.getBean(IDownlinkApp.class);
    }

    /**
     * @return result of start operation
     */
    @PostMapping(value = "start", produces = MediaType.TEXT_PLAIN)
    @ApiOperation(value = "If not currently processing telemetry, causes the current downlink process to begin processing telemetry. Includes an implicit 'bind' operation.", tags = "start")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Downlink Telemetry Processing Successfully Started"),
            @ApiResponse(code = 412, message = ALREADY_STARTED + " or " + NOT_RUNNING),
            @ApiResponse(code = 417, message = DOWN_SHELL_TIMED_OUT),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR) })
    public ResponseEntity<String> start() {
        final StringBuilder sb = new StringBuilder();
        try {
            if (null != app) {
                if (app.useGui()) {
                    final SessionConfigShell configShell = app.getConfigShell();
                    if ((configShell != null) && !configShell.pushRunSessionButton()) {
                        return new ResponseEntity<>(SWT_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    /*
                     * Wait for a downlink shell to come up...
                     */
                    for (int i = 0; i < MAX_RETRIES; i++) {
                        final AbstractDownShell ads = app.getDownlinkShell();
                        if (ads != null) {
                            break;
                        }
                        Thread.sleep(RETRY_DELAY);
                    }

                    final AbstractDownShell downShell = app.getDownlinkShell();
                    if (downShell != null) {
                        if (!downShell.isStarted()) {
                            final SWTDeferredSynchronousExecutor r = new SWTDeferredSynchronousExecutor(new Runnable() {
                                @Override
                                public void run() {
                                    downShell.startApp();
                                }
                            });
                            r.run();
                            if (!r.isOK()) {
                                throw r.getThrowable();
                            }
                            return new ResponseEntity<>(DOWNLINK_PROCESSING_STARTED + " (" + app.getProcessingState()
                                    + ")", HttpStatus.OK);
                        }
                        else {
                            return new ResponseEntity<>(ALREADY_STARTED + " (" + app.getProcessingState() + ")",
                                                        HttpStatus.PRECONDITION_FAILED);
                        }
                    }
                    else {
                        return new ResponseEntity<>(DOWN_SHELL_TIMED_OUT, HttpStatus.EXPECTATION_FAILED);
                    }
                }
                else {
                    if (!app.hasBeenStarted()) {
                        /* start downlink data processing */
                        app.startDownlink();
                        return new ResponseEntity<>(DOWNLINK_PROCESSING_STARTED + " (" + app.getProcessingState() + ")",
                                                    HttpStatus.OK);
                    }
                    else if (app.getProcessingState() == DownlinkProcessingState.STARTED) {
                        return new ResponseEntity<>(ALREADY_STARTED + " (" + app.getProcessingState() + ")",
                                                    HttpStatus.PRECONDITION_FAILED);
                    }
                    else {
                        return new ResponseEntity<>(CANNOT_RESTART + " (" + app.getProcessingState() + ")",
                                                    HttpStatus.PRECONDITION_FAILED);
                    }
                }
            }
            else {
                return new ResponseEntity<>(NOT_RUNNING, HttpStatus.PRECONDITION_FAILED);
            }
        }
        catch (final Throwable t) {
            t.printStackTrace();
            sb.append(t.getClass().getSimpleName()).append(": ").append(ExceptionTools.getMessage(t));
        }
        return new ResponseEntity<>(sb.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * @return stop status
     */
    @PostMapping(value = "stop", produces = MediaType.TEXT_PLAIN)
    @ApiOperation(value = "If currently processing telemetry, causes the current downlink process to stop processing telemetry.", tags = "stop")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Downlink Telemetry Processing Successfully Stopped"),
            @ApiResponse(code = 412, message = ALREADY_STOPPED + " or " + NOT_RUNNING),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR) })
    public ResponseEntity<String> stop() {
        final StringBuilder sb = new StringBuilder();
        final AtomicBoolean successful = new AtomicBoolean(false);
        AbstractDownShell theDownShell = null;
        try {
            if (null != app) {
                if (app.useGui()) {
                    final AbstractDownShell downShell = app.getDownlinkShell();
                    if (downShell != null) {
                        theDownShell = downShell;
                        if (!downShell.isStopped()) {
                            final SWTDeferredSynchronousExecutor r = new SWTDeferredSynchronousExecutor(new Runnable() {
                                @Override
                                public void run() {
                                    successful.set(downShell.stopApp(false));
                                }
                            });
                            r.run();
                            if (!r.isOK()) {
                                throw r.getThrowable();
                            }
                        }
                    }
                }
                else if (app.getProcessingState() == DownlinkProcessingState.STARTED
                        || app.getProcessingState() == DownlinkProcessingState.PAUSED) {
                    app.stop();
                    successful.set(true);
                }
            }
            if (successful.get()) {
                return new ResponseEntity<>(DOWNLINK_PROCESSING_STOPPED + " (" + app.getProcessingState() + ")",
                                            HttpStatus.OK);
            }
            else {
                return new ResponseEntity<>(((theDownShell != null) ? theDownShell.isNeverStarted() : false)
                        ? NOT_RUNNING
                        : ALREADY_STOPPED + " (" + app.getProcessingState() + ")", HttpStatus.PRECONDITION_FAILED);
            }
        }
        catch (final Throwable t) {
            t.printStackTrace();
            sb.append(t.getClass().getSimpleName()).append(": ").append(ExceptionTools.getMessage(t));
        }
        return new ResponseEntity<>(sb.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * @return start status
     */
    @PostMapping(value = "pause", produces = MediaType.TEXT_PLAIN)
    @ApiOperation(value = "If currently processing telemetry, causes the current downlink process to pause the processing of telemetry.", tags = "pause")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Downlink Telemetry Processing Successfully Paused"),
            @ApiResponse(code = 412, message = ALREADY_PAUSED + " or " + NOT_RUNNING),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR) })
    public ResponseEntity<String> pause() {
        final StringBuilder sb = new StringBuilder();
        try {
            if (null != app) {
                if (app.useGui()) {
                    final AbstractDownShell downShell = app.getDownlinkShell();
                    if (downShell != null) {
                        if (downShell.isStarted() && !downShell.isPaused() && !downShell.isStopped()) {
                            final SWTDeferredSynchronousExecutor r = new SWTDeferredSynchronousExecutor(new Runnable() {
                                @Override
                                public void run() {
                                    downShell.pauseApp();
                                }
                            });
                            r.run();
                            if (!r.isOK()) {
                                throw r.getThrowable();
                            }
                        }
                        else {
                            return new ResponseEntity<>(downShell.isStopped() ? NOT_RUNNING
                                    : ALREADY_PAUSED + " (" + app.getProcessingState() + ")",
                                                        HttpStatus.PRECONDITION_FAILED);
                        }
                    }
                }
                else {
                    if (app.getProcessingState() == DownlinkProcessingState.STARTED) {
                        app.pause();
                        return new ResponseEntity<>(DOWNLINK_PROCESSING_PAUSED + " (" + app.getProcessingState() + ")",
                                                    HttpStatus.OK);
                    }
                    else {
                        return new ResponseEntity<>(((app.getProcessingState() == DownlinkProcessingState.PAUSED)
                                ? ALREADY_PAUSED
                                : NOT_RUNNING) + " (" + app.getProcessingState() + ")", HttpStatus.PRECONDITION_FAILED);
                    }
                }
            }
            else {
                return new ResponseEntity<>(NOT_RUNNING, HttpStatus.PRECONDITION_FAILED);
            }
        }
        catch (final Throwable t) {
            t.printStackTrace();
            sb.append(t.getClass().getSimpleName()).append(": ").append(ExceptionTools.getMessage(t));
        }
        return new ResponseEntity<>(sb.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * @return start status
     */
    @PostMapping(value = "resume", produces = MediaType.TEXT_PLAIN)
    @ApiOperation(value = "If telemetry processing is currently paused, causes the current downlink process to resume the processing of telemetry.", tags = "resume")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Downlink Telemetry Processing Successfully Resumed"),
            @ApiResponse(code = 412, message = NOT_PAUSED + " or " + NOT_RUNNING),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR) })
    public ResponseEntity<String> resume() {
        final StringBuilder sb = new StringBuilder();
        try {
            if (null != app) {
                if (app.useGui()) {
                    final AbstractDownShell downShell = app.getDownlinkShell();
                    if (downShell != null) {
                        if (downShell.isPaused()) {
                            final SWTDeferredSynchronousExecutor r = new SWTDeferredSynchronousExecutor(new Runnable() {
                                @Override
                                public void run() {
                                    app.getDownlinkShell().resumeApp();
                                }
                            });
                            r.run();
                            if (!r.isOK()) {
                                throw r.getThrowable();
                            }
                        }
                        else {
                            return new ResponseEntity<>(NOT_PAUSED + " (" + app.getProcessingState() + ")",
                                                        HttpStatus.PRECONDITION_FAILED);
                        }
                    }
                }
                else {
                    if (app.getProcessingState() == DownlinkProcessingState.PAUSED) {
                        app.resume();
                        return new ResponseEntity<>(DOWNLINK_PROCESSING_RESUMED + " (" + app.getProcessingState() + ")",
                                                    HttpStatus.OK);

                    }
                    else {
                        return new ResponseEntity<>(NOT_PAUSED + " (" + app.getProcessingState() + ")",
                                                    HttpStatus.PRECONDITION_FAILED);
                    }
                }
            }
            else {
                return new ResponseEntity<>(NOT_RUNNING, HttpStatus.PRECONDITION_FAILED);
            }
        }
        catch (final Throwable t) {
            t.printStackTrace();
            sb.append(t.getClass().getSimpleName()).append(": ").append(ExceptionTools.getMessage(t));
        }
        return new ResponseEntity<>(sb.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * @return abort status
     */
    @PostMapping(value = "abort", produces = MediaType.TEXT_PLAIN)
    @ApiOperation(value = "If currently processing telemetry, causes the current downlink process to abort the processing of telemetry.", tags = "abort")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Downlink Telemetry Processing Successfully Aborted"),
            @ApiResponse(code = 412, message = NOT_RUNNING),
            @ApiResponse(code = 500, message = SWT_FAILURE + " or " + INTERNAL_SERVER_ERROR) })
    public ResponseEntity<String> abort() {
        final StringBuilder sb = new StringBuilder();
        try {
            if (null != app) {
                if (app.useGui()) {
                    final SessionConfigShell configShell = app.getConfigShell();
                    if (configShell != null) {
                        if (!configShell.pushExitButton()) {
                            return new ResponseEntity<>(SWT_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                }
                app.abort();
                return new ResponseEntity<>(DOWNLINK_ABORTED + " (" + app.getProcessingState() + ")", HttpStatus.OK);
            }
            else {
                return new ResponseEntity<>(NOT_RUNNING, HttpStatus.PRECONDITION_FAILED);
            }
        }
        catch (final Throwable t) {
            t.printStackTrace();
            sb.append(t.getClass().getSimpleName()).append(": ").append(ExceptionTools.getMessage(t));
        }
        return new ResponseEntity<>(sb.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * @return abort status
     */
    @PostMapping(value = "exit", produces = MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Causes the current downlink process stop processing and exit. An attempt is made to shut down cleanly, but the exit is guaranteed even if an orderly shutdown is not possible.", tags = "exit")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Downlink Telemetry Processing Successfully Stopped -- Exiting..."),
            @ApiResponse(code = 500, message = SWT_FAILURE + " or " + INTERNAL_SERVER_ERROR) })
    public ResponseEntity<String> exit() {
        final StringBuilder sb = new StringBuilder();
        try {
            if ((null != app) && (app.useGui())) {
                final SessionConfigShell configShell = app.getConfigShell();
                if (configShell != null) {
                    if (!configShell.pushExitButton()) {
                        return new ResponseEntity<>(SWT_FAILURE, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                final AbstractDownShell downlinkShell = app.getDownlinkShell();
                if (downlinkShell != null) {
                    try {
                        downlinkShell.getShell().getDisplay().syncExec(new Runnable() {
                            @Override
                            public void run() {
                                downlinkShell.stopApp(false);
                                downlinkShell.getShell().dispose();
                            }
                        });
                    }
                    catch (final Exception e) {
                        // ignore
                    }
                }
            }
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    }
                    catch (final InterruptedException e) {
                        // ignore
                    }
                    System.exit(0);
                }
            });
            t.setDaemon(true);
            t.start();
            return new ResponseEntity<>(DOWNLINK_STOPPED_AND_EXITING + " (" + app.getProcessingState() + ")",
                                        HttpStatus.OK);
        }
        catch (final Throwable t) {
            t.printStackTrace();
            sb.append(t.getClass().getSimpleName()).append(": ").append(ExceptionTools.getMessage(t));
        }
        return new ResponseEntity<>(sb.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Saves the current contents of the downlink local channel LAD to a specified, fully qualified file on the
     * downlink's file system
     * 
     * @param filename
     *            the fully qualified file name on the downlink's file system to which to save the current contents of
     *            the downlink local channel LAD
     * @return savelad status
     */
    @PostMapping(value = "savelad", produces = MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Saves the current contents of the downlink local channel LAD to a specified, fully qualified file on the downlink's file system.", tags = "savelad")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Global LAD Successfully Saved"),
            @ApiResponse(code = 500, message = LAD_NOT_SAVED) })
    public ResponseEntity<String> saveLad(@RequestParam(value = "filename", required = true) final String filename) {
        try {
            if (app.saveLadToFile(filename)) {
            	return new ResponseEntity<>(LAD_SAVED, HttpStatus.OK);
            }
            return new ResponseEntity<>(LAD_NOT_SAVED + ": Global LAD reported failure when saving.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch (final Throwable t) {
            return new ResponseEntity<>(LAD_NOT_SAVED + ": " + ExceptionTools.getMessage(t),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @return clearlad status
     */
    @PostMapping(value = "clearlad", produces = MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Clears the contents of the current downlink LAD.", tags = "clearlad")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Global LAD Successfully Cleared"),
            @ApiResponse(code = 500, message = LAD_NOT_CLEARED) })
    public ResponseEntity<String> clearLad() {
        try {
            app.clearChannelState();
            return new ResponseEntity<>(LAD_CLEARED, HttpStatus.OK);
        }
        catch (final Throwable t) {
            return new ResponseEntity<>(LAD_NOT_CLEARED + ": " + ExceptionTools.getMessage(t),
                                        HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @param strategy
     *            the desired TimeComparisonStrategy
     * @return timeComparisonStrategy status
     */
    @PostMapping(value = "timeCompStrategys", produces = MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Sets the current Time Comparison Strategy.", tags = "timeCompStrategy")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Time Comparison Strategy Successfully Set"),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR) })
    public ResponseEntity<String> setTimeComparisonStrategy(@RequestParam(value = "strategy", required = true) @ApiParam(allowableValues = "LAST_RECEIVED,SCLK,SCET,ERT") final String strategy) {
        try {
            final TimeComparisonStrategy timeStrategy = TimeComparisonStrategy.valueOf(strategy.toUpperCase());
            app.setTimeComparisonStrategy(timeStrategy);

            /*
             * TODO: Need to somehow update the GUI with the results of this operation.
             * Not sure how yet.
             */
            return new ResponseEntity<>("TIME_COMPARISON_STRATEGY=" + app.getTimeComparisonStrategy(), HttpStatus.OK);
        }
        catch (final Throwable t) {
            return new ResponseEntity<>("TIME_COMPARISON_STRATEGY NOT SET. STILL: " + app.getTimeComparisonStrategy()
                    + ". (" + ExceptionTools.getMessage(t) + ")", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @return bind status
     */
    @PostMapping(value = "bind", produces = MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Causes the current downlink process to connect to the data source without beginning telemetry processing.", tags = "bind")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Downlink Successfully Bound to Data Source"),
            @ApiResponse(code = 500, message = BIND_FAILED) })
    public ResponseEntity<String> bind() {
        try {
            if (null != app) {
                if (app.useGui()) {
                    final AbstractDownShell downShell = app.getDownlinkShell();
                    if ((downShell != null) && !downShell.isStarted()) {
                        final SessionConfigShell configShell = app.getConfigShell();
                        if (configShell != null) {
                            configShell.getShell().close();
                        }
                    }
                }
            }
            return new ResponseEntity<>(BIND_SUCCESSFUL, HttpStatus.OK);
        }
        catch (final Throwable t) {
            return new ResponseEntity<>(BIND_FAILED + ": " + ExceptionTools.getMessage(t),
                                        HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @return unbind status
     */
    @PostMapping(value = "unbind", produces = MediaType.TEXT_PLAIN)
    @ApiOperation(value = "If bound and telemetry processing has not begun or is paused, causes the current downlink process to disconnect from the data source.", tags = "unbind")
    @ApiResponses(value = { @ApiResponse(code = 500, message = NOT_IMPLEMENTED) })
    public ResponseEntity<String> unbind() {
        return new ResponseEntity<>(NOT_IMPLEMENTED, HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * A class wrapper for synchronously executing SWT deferred tasks on the SWT display thread
     * 
     */
    private class SWTDeferredSynchronousExecutor implements Runnable {
        private final Runnable runnable;

        private Throwable      t;

        public SWTDeferredSynchronousExecutor(final Runnable runnable) {
            super();
            this.runnable = runnable;
            this.t = null;
        }

        @Override
        public void run() {
            try {
                app.getDownlinkShell().getShell().getDisplay().syncExec(runnable);
            }
            catch (final Throwable t) {
                this.t = t;
            }
        }

        public boolean isOK() {
            return t == null;
        }

        public Throwable getThrowable() {
            return t;
        }
    }
}
