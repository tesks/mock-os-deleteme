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
package jpl.gds.telem.down.gui;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jpl.gds.tm.service.api.frame.IPresyncFrameMessage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.performance.HealthStatus;
import jpl.gds.shared.performance.HeapPerformanceData;
import jpl.gds.shared.performance.PerformanceSummaryMessage;
import jpl.gds.shared.performance.ProviderPerformanceSummary;
import jpl.gds.shared.performance.gui.PerformanceShell;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.IFrameSummaryMessage;
import jpl.gds.tm.service.api.packet.IPacketSummaryMessage;


/**
 * The DownStatisticsComposite class is used to display telemetry statistics in
 * the downlink window.
 * 
 *
 * 4/2/15  Removed all logic to deal with Backlog
 *          Summary messages and replaced it with logic to handle
 *          PerformanceSummaryMessages.
 * 
 * 5/18/15hanges throughout to eliminate dependence
 *          upon TransferFrameMessage and decouple from the MessageContext.
 *          Individual changes not all marked.
 *
 */
public class DownStatisticsComposite extends Composite {

	private static final int MAX_QUEUE_LEN = 10000;

	private static final Color NONE = ChillColorCreator
			.getColor(new ChillColor(ChillColor.ColorName.BLUE));
	private static final Color GREEN = ChillColorCreator
			.getColor(new ChillColor(ChillColor.ColorName.GREEN));
    private static final Color YELLOW = ChillColorCreator
            .getColor(new ChillColor(ChillColor.ColorName.YELLOW));
    private static final Color RED = ChillColorCreator.getColor(new ChillColor(
            ChillColor.ColorName.RED));
    private static final Color WHITE = ChillColorCreator.getColor(new ChillColor(
            ChillColor.ColorName.WHITE));
    private static final Color BLACK = ChillColorCreator.getColor(new ChillColor(
            ChillColor.ColorName.BLACK));
    
    private static final String VALID_PKT_LABEL = "Valid Packets: ";
    private static final String INVALID_PKT_LABEL = "Invalid Packets: ";
    private static final String FILL_PKT_LABEL = "Fill Packets: ";

    private static final String STATION_PKT_LABEL = "Station Packets: ";

    private static final String                 CFDP_PKT_LABEL          = "CFDP Packets: ";
    private static final String VALID_FRAME_LABEL = "In Sync Frames: ";
    private static final String INVALID_FRAME_LABEL = "Invalid Frames: ";
    private static final String FILL_FRAME_LABEL = "Idle Frames: ";
    private static final String DEAD_FRAME_LABEL = "Dead Frames: ";
    private static final String OUT_OF_SYNC_COUNT_LABEL = "Out-of-Sync Byte Count: ";
    private static final String REPEAT_FRAME_LABEL = "Repeat Frames: ";
    private static final String FRAME_GAP_LABEL = "Frame Gaps: ";
    private static final String BIT_RATE_LABEL = "Bitrate: ";

    private Label validPackets;
    private Label invalidPackets;
    private Label idlePackets;

    private Label stationPackets;

    private Label                               cfdpPackets;
    private Label validFrames;
    private Label invalidFrames;
    private Label idleFrames;
    private Label deadFrames;
    private Label outOfSyncCount;
    private Label repeatFrames;
    private Label frameGaps;
    private Label bitrate;
    
    private Label healthButton;
    private PerformanceShell perfShell;
    private PerformanceSummaryMessage lastPerfMessage;
    private final IMessagePublicationBus bus;

    /* Added queue to buffer the MessageContext from GUI updates. */
    private final LinkedBlockingQueue<IMessage> messages                = new LinkedBlockingQueue<>(MAX_QUEUE_LEN);

    private final PerformanceProperties perfProps;
    
    private Thread                              updateThread;

    private static final Tracer                 log                     = TraceManager.getTracer(Loggers.DOWNLINK);

    /*  2021-07-26 - make bitrate updates more responsive at low bitrates */
    private long             lastBitrateUpdate           = 0;
    private static final int           BITRATE_UPDATE_THRESHOLD_MS = 2000;

    /**
     * Creates a new DownStatisticsComposite.
     * @param bus the internal message bus to subscribe to
     * @param perfProps the performance properties configuration object
     * 
     * @param parent the parent Composite for this composite
     */
    public DownStatisticsComposite(final IMessagePublicationBus bus, final PerformanceProperties perfProps, final Composite parent) {
        super(parent, SWT.BORDER);
        this.perfProps = perfProps;
        this.bus = bus;
        createControls();
        new StatisticsSubscriber();
    }

    private void createControls() {
        final FormLayout fl = new FormLayout();
        fl.spacing = 5;
        setLayout(fl);

        final Composite telemComposite = new Composite(this, SWT.BORDER);
        /* Changed layout parameters to allow for display of larger bitrates. */
        final GridLayout gl = new GridLayout(4, false);
        gl.horizontalSpacing = 15;
        telemComposite.setLayout(gl);
        final FormData fdt = new FormData();
        fdt.top = new FormAttachment(0);
        fdt.left = new FormAttachment(0);
        fdt.bottom = new FormAttachment(100);
        fdt.right = new FormAttachment(85);
        telemComposite.setLayoutData(fdt);

        validFrames = new Label(telemComposite, SWT.NONE);
        validFrames.setText(VALID_FRAME_LABEL + "0              ");
        outOfSyncCount = new Label(telemComposite,SWT.BOLD);
        outOfSyncCount.setText(OUT_OF_SYNC_COUNT_LABEL + "0      ");
        invalidFrames = new Label(telemComposite, SWT.NONE);
        invalidFrames.setText(INVALID_FRAME_LABEL + "0          ");
        idleFrames = new Label(telemComposite, SWT.BOLD);
        idleFrames.setText(FILL_FRAME_LABEL + "0          ");
        deadFrames = new Label(telemComposite, SWT.BOLD);
        deadFrames.setText(DEAD_FRAME_LABEL + "0          ");
        repeatFrames = new Label(telemComposite,SWT.BOLD);
        repeatFrames.setText(REPEAT_FRAME_LABEL + "0          ");
        frameGaps = new Label(telemComposite,SWT.BOLD);
        frameGaps.setText(FRAME_GAP_LABEL + "0          ");
        bitrate = new Label(telemComposite, SWT.BOLD);
        bitrate.setText(BIT_RATE_LABEL + "0              ");
        validPackets = new Label(telemComposite, SWT.NONE);
        validPackets.setText(VALID_PKT_LABEL + "0              ");
        invalidPackets = new Label(telemComposite, SWT.NONE);
        invalidPackets.setText(INVALID_PKT_LABEL + "0          ");
        idlePackets = new Label(telemComposite, SWT.BOLD);
        idlePackets.setText(FILL_PKT_LABEL + "0          ");

        stationPackets = new Label(telemComposite, SWT.BOLD);
        stationPackets.setText(STATION_PKT_LABEL + "0        ");

        cfdpPackets = new Label(telemComposite, SWT.BOLD);
        cfdpPackets.setText(CFDP_PKT_LABEL + "0        ");

        /*
         * Replace previous queue length report block
         * with performance indicator and details button.
         */
        final Composite healthComposite = new Composite(this, SWT.BORDER);
        final RowLayout rl = new RowLayout(SWT.VERTICAL);
        rl.pack = true;
        rl.justify = true;
        healthComposite.setLayout(rl);
        final FormData fdq = new FormData();
        fdq.top = new FormAttachment(0);
        fdq.right = new FormAttachment(100);
        fdq.bottom = new FormAttachment(100);
        fdq.left = new FormAttachment(telemComposite);
        healthComposite.setLayoutData(fdq);
        
        fdt.right = new FormAttachment(healthComposite);
        
        final Composite buttonGroup = new Group(healthComposite, SWT.BORDER);
        buttonGroup.setLayout(new RowLayout());
        
        /* Note this would be a button, but SWT will not set the background color
         * of a button, apparently because it is just lame. I have to use a 
         * label and add a mouse listener so it acts like a button.
         */
        this.healthButton = new Label(buttonGroup, SWT.CENTER);
        
        // This will set the initial color of the health button.
        handlePerformanceSumMessage(null);
        
        this.healthButton.setText("Click for\nDetails");

        final Label healthLabel = new Label(healthComposite, SWT.CENTER);
        healthLabel.setText("Performance");
     
        this.healthButton.addMouseListener(new MouseListener() {

            /**
             * {@inheritDoc}
             * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
             */
            @Override
            public void mouseDoubleClick(final MouseEvent arg0) {
                // do nothing
                
            }

            /**
             * {@inheritDoc}
             * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
             */
            @Override
            public void mouseUp(final MouseEvent arg0) {
                // do nothing
                
            }

            /**
             * {@inheritDoc}
             * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
             */
            @Override
            public void mouseDown(final MouseEvent arg0) {
                try {
                    if (perfShell == null) {
                        perfShell = new PerformanceShell(getDisplay(), log);
                        perfShell.open();
                        if (lastPerfMessage != null) {
                            handlePerformanceSumMessage(lastPerfMessage);
                        }
                        perfShell.getShell().addDisposeListener(new DisposeListener() {

                            @Override
                            public void widgetDisposed(final DisposeEvent arg0) {
                                perfShell = null;

                            }
                        });
                    } else {
                    	perfShell.getShell().forceActive();
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });

        layout(true);
        
        /* Added thread to work off message queue. */
        /* Made thread global and added stop method for interrupting it */
        updateThread = new Thread(new StatisticsUpdater(), "Downlink GUI Stats Updater");
        updateThread.start();
    }

    /**
     * Handles interrupting the statistics updater thread
     */
    public void stopUpdateThread() {
        if (updateThread != null && !updateThread.isInterrupted()) {
            updateThread.interrupt();
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

    	/* If we have no message (at startup) then
    	 * set health status indicator based upon heap status only.
    	 */
    	HealthStatus status = HealthStatus.GREEN;
    	if (msg == null) {
    		status = new HeapPerformanceData(perfProps).getHealthStatus();
    	} else {
    		status = msg.getOverallHealth();
    	}
    	if (!this.isDisposed()) {
    		switch (status) {
    		case GREEN:
                this.healthButton.setBackground(GREEN);
                this.healthButton.setForeground(BLACK);
                break;
            case NONE:
                this.healthButton.setBackground(NONE);
                this.healthButton.setForeground(WHITE);
                break;
            case RED:
                this.healthButton.setBackground(RED);
                this.healthButton.setForeground(WHITE);
                break;
            case YELLOW:
                this.healthButton.setBackground(YELLOW);
                this.healthButton.setForeground(BLACK);
                break;
            default:
                break;

            }

            if (msg != null && this.perfShell != null) {
            	HeapPerformanceData hd = msg.getHeapStatus();
            	if (hd == null) {
            		hd = new HeapPerformanceData(perfProps);
            	}
                this.perfShell.setHeapPerformance(hd);
                final Map<String, ProviderPerformanceSummary> provMap = msg
                        .getPerformanceData();
                for (final ProviderPerformanceSummary sum : provMap.values()) {
                    this.perfShell.setPerformanceData(sum);
                }
            }
        }

        this.lastPerfMessage = msg;
    }

    private void handleFrameSumMessage(final IFrameSummaryMessage msg) {
        if (!this.isDisposed()) {
            String temp = String.format("%-10d", msg.getNumFrames());
            validFrames.setText(VALID_FRAME_LABEL + temp);
            temp = String.format("%-10d", msg.getBadFrames());
            invalidFrames.setText(INVALID_FRAME_LABEL + temp);
            temp = String.format("%-10d", msg.getIdleFrames());
            idleFrames.setText(FILL_FRAME_LABEL + temp);
            temp = String.format("%-10d", msg.getDeadFrames());
            deadFrames.setText(DEAD_FRAME_LABEL + temp);
            temp = String.format("%-3d", msg.getOutOfSyncCount());
            outOfSyncCount.setText(OUT_OF_SYNC_COUNT_LABEL + temp);
            /* Bitrate now gotten from summary message */
            updateBitRate(msg.getBitrate());
            /* Do not call pack/layout. Too performance intense. */
        }
    }

    private void handlePacketSumMessage(final IPacketSummaryMessage msg) {
        if (!this.isDisposed()) {
            String temp = String.format("%-10d", msg.getNumValidPackets());
            validPackets.setText(VALID_PKT_LABEL + temp);
            temp = String.format("%-10d", msg.getNumInvalidPackets());
            invalidPackets.setText(INVALID_PKT_LABEL + temp);
            temp = String.format("%-10d", msg.getNumFillPackets());
            idlePackets.setText(FILL_PKT_LABEL + temp);
            temp = String.format("%-10d", msg.getNumFrameRepeats());
            repeatFrames.setText(REPEAT_FRAME_LABEL + temp);
            temp = String.format("%-10d", msg.getNumFrameGaps());
            frameGaps.setText(FRAME_GAP_LABEL + temp);

            temp = String.format("%-10d", msg.getNumStationPackets());
            stationPackets.setText(STATION_PKT_LABEL + temp);

            temp = String.format("%-10d", msg.getNumCfdpPackets());
            cfdpPackets.setText(CFDP_PKT_LABEL + temp);

            /* Do not call pack/layout. Too performance intense. */
            
        }
    }

    /**
     * Update bitrate from frame presync messages, in addition to frame summary msgs.
     *
     * @param msg presync frame message
     */
    private void handleFramePresyncMessage(final IPresyncFrameMessage msg) {
        updateBitRate(msg.getStationInfo().getBitRate());
    }

    /**
     * Update the displayed bitrate
     * @param bitRate
     */
    private void updateBitRate(double bitRate) {
        long start = System.currentTimeMillis();
        if (start - lastBitrateUpdate > BITRATE_UPDATE_THRESHOLD_MS && !this.isDisposed()) {
            final String rate = String.format("%-13d", (long) bitRate);
            log.debug("Updating bitrate to ", rate);
            bitrate.setText(BIT_RATE_LABEL + rate);
            lastBitrateUpdate = start;
        }
    }
    

    /**
     * StatisticsSubscriber is the listener for internal messages that are
     * used to populate this composite.
     * 
     *
     */
    private class StatisticsSubscriber implements MessageSubscriber {

    	public StatisticsSubscriber() {
    		/* No longer listens for TransferFrameMessage */
    		bus.subscribe(TmServiceMessageType.TelemetryFrameSummary, this);
    		bus.subscribe(TmServiceMessageType.TelemetryPacketSummary, this);
            bus.subscribe(CommonMessageType.PerformanceSummary, this);
    		/* Subscribe to end of test message */
            bus.subscribe(SessionMessageType.EndOfSession, this);
            /* Sub to presync frame data message */
            bus.subscribe(TmServiceMessageType.PresyncFrameData, this);
    	}

    	/**
    	 * {@inheritDoc}
    	 */
    	@Override
    	public void handleMessage(final IMessage message) {
    		/* No longer handle message directly. Queue it. */
    		if (!messages.offer(message))  {
                log.error("Unable to offer message to the downlink GUI");
    		}

    	}
    }

    /**
     * Resets all graphical counter displays.
     */
    public void resetCounters() {
        validFrames.setText(VALID_FRAME_LABEL + "0            ");
        outOfSyncCount.setText(OUT_OF_SYNC_COUNT_LABEL + "0    ");
        invalidFrames.setText(INVALID_FRAME_LABEL + "0        ");
        idleFrames.setText(FILL_FRAME_LABEL + "0        ");
        deadFrames.setText(DEAD_FRAME_LABEL + "0        ");
        repeatFrames.setText(REPEAT_FRAME_LABEL + "0        ");
        frameGaps.setText(FRAME_GAP_LABEL + "0        ");
        bitrate.setText(BIT_RATE_LABEL + "0            ");
        validPackets.setText(VALID_PKT_LABEL + "0            ");
        invalidPackets.setText(INVALID_PKT_LABEL + "0        ");
        idlePackets.setText(FILL_PKT_LABEL + "0        ");
        stationPackets.setText(STATION_PKT_LABEL + "0        ");
        cfdpPackets.setText(CFDP_PKT_LABEL + "0        ");
        /* Clear health indicator. */
        healthButton.setBackground(NONE);
        healthButton.setForeground(WHITE);

        layout(true);
    }
    
	/**
	 * Thread worker class for the working off the message queue by posting
	 * async exec tasks.
	 * 
	 */
    private class StatisticsUpdater implements Runnable {

    	/**
    	 * {@inheritDoc}
    	 */
    	@Override
    	public void run() {

    		while (!Thread.interrupted()) {

    			try {
    				final IMessage message = messages.poll(2000,
    						TimeUnit.MILLISECONDS);

    				if (message != null) {

    					if (!DownStatisticsComposite.this.isDisposed()) {
    						DownStatisticsComposite.this.getDisplay()
    						.asyncExec(new Runnable() {
    							@Override
    							public void run() {
    								try {
    									if (message instanceof IFrameSummaryMessage) {
    										handleFrameSumMessage((IFrameSummaryMessage) message);
    									} else if (message instanceof IPacketSummaryMessage) {
    										handlePacketSumMessage((IPacketSummaryMessage) message);
    									} else if (message instanceof PerformanceSummaryMessage) {
                                            /*
                                             * Now handles performance
                                             * summary rather than
                                             * backlog summary.
                                             */
                                            handlePerformanceSumMessage((PerformanceSummaryMessage) message);
    									} else if (message instanceof IPresyncFrameMessage) {
    									    /* Add presync frame message as source for bitrate */
    									    handleFramePresyncMessage((IPresyncFrameMessage) message);
                                        }
    									/*
    									 * Leave the health status button alone at end of session.
    									 */
    								} catch (final Exception e) {
    									e.printStackTrace();
    								}
    							}
    						});

    					}
    				}

                }
                catch (final InterruptedException e1) {
                    // This thread is interrupted when
                    // Select the 'X' in chill_down gui
                    // Select chill_down gui's File -> Exit application drop-down
                    // Ctrl+C was detected
                    Thread.currentThread().interrupt();
                }
                catch (final Exception e) {
                    log.error("Unknown exception in Down Statistics Summary Publisher run()");
    				e.printStackTrace();
    			}

    		}
    	}

    }

}
