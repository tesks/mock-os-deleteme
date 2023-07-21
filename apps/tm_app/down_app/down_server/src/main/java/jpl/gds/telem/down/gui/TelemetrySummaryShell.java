
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

import java.text.DateFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jpl.gds.common.service.telem.IDownlinkSummary;
import jpl.gds.common.service.telem.ITelemetryIngestorSummary;
import jpl.gds.common.service.telem.ITelemetryProcessorSummary;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.time.TimeUtility;

/**
 * This class implements a Shell for viewing telemetry summary information.
 * 
 */
public class TelemetrySummaryShell implements ChillShell {
    private static final String TITLE = "Downlink Summary";
    private Shell mainShell = null;
    private Shell parent = null;
    private final ITelemetrySummary summary;
    private final SseContextFlag    sseFlag;

	/**
     * Creates a TelemetrySummaryShell to display the given summary.
     * 
     * @param summ
     *            the TelemetrySummary to view
     * @param parent
     *            the parent Shell widget
     * @param databaseName
     *            the current database name
     * @param sseFlag
     *            The SSE context flag
     */
    public TelemetrySummaryShell(final ITelemetrySummary summ, final Shell parent, final String databaseName,
            final SseContextFlag sseFlag) {
        summary = summ;
        this.parent = parent;
        this.sseFlag = sseFlag;
        createGui(databaseName);
    }

    private void addLabelIfNonZero(final Composite comp, final long value,
            final String labelText) {
        final Label label = new Label(comp, SWT.CENTER);
        label.setText(value + " " + labelText);
    }

    /**
     * Closes the shell.
     */
    public void close() {
        mainShell.close();
    }

    private Composite createFrameComposite(final Composite mainComp) {
        final Composite frameComp = new Composite(mainComp, SWT.BORDER);
        final RowLayout shellLayout = new RowLayout(SWT.VERTICAL);
        shellLayout.spacing = 5;
        shellLayout.marginHeight = 2;
        shellLayout.marginWidth = 2;
        frameComp.setLayout(shellLayout);
        
        if (summary != null) {
            if (summary instanceof IDownlinkSummary) {
                final IDownlinkSummary sum = (IDownlinkSummary) summary;

                addLabelIfNonZero(frameComp, sum.getInSyncFrames(), "In sync frames");
                addLabelIfNonZero(frameComp, sum.getOutOfSyncData(), "Bytes out of sync");
                addLabelIfNonZero(frameComp, sum.getOutOfSyncCount(), "Times out of sync");
                addLabelIfNonZero(frameComp, sum.getIdleFrames(), "Idle frames");
                addLabelIfNonZero(frameComp, sum.getBadFrames(), "Bad frames");
                addLabelIfNonZero(frameComp, sum.getDeadFrames(), "Dead frames");
                if (sum.getFrameGaps() + sum.getFrameRegressions() != 0) {
                    final Label label = new Label(frameComp, SWT.CENTER);
                    label.setText(sum.getFrameGaps() + "/" + sum.getFrameRegressions() + " Frame gaps/regressions");
                }
                addLabelIfNonZero(frameComp, sum.getFrameRepeats(), "Repeated frames");
            }
            else if (summary instanceof ITelemetryIngestorSummary) {
                final ITelemetryIngestorSummary sum = (ITelemetryIngestorSummary) summary;

                addLabelIfNonZero(frameComp, sum.getInSyncFrames(), "In sync frames");
                addLabelIfNonZero(frameComp, sum.getOutOfSyncData(), "Bytes out of sync");
                addLabelIfNonZero(frameComp, sum.getOutOfSyncCount(), "Times out of sync");
                addLabelIfNonZero(frameComp, sum.getIdleFrames(), "Idle frames");
                addLabelIfNonZero(frameComp, sum.getBadFrames(), "Bad frames");
                addLabelIfNonZero(frameComp, sum.getDeadFrames(), "Dead frames");
                if (sum.getFrameGaps() + sum.getFrameRegressions() != 0) {
                    final Label label = new Label(frameComp, SWT.CENTER);
                    label.setText(sum.getFrameGaps() + "/" + sum.getFrameRegressions() + " Frame gaps/regressions");
                }
                addLabelIfNonZero(frameComp, sum.getFrameRepeats(), "Repeated frames");
            }
        }
        

        return frameComp;
    }

    private void createGui(final String databaseName) {
        mainShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE);
        if (sseFlag.isApplicationSse()) {
        	mainShell.setText("SSE " + TITLE);
        } else {
        	mainShell.setText("FSW " + TITLE);
        }
        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 5;
        shellLayout.marginHeight = 2;
        shellLayout.marginWidth = 2;
        mainShell.setLayout(shellLayout);

        final Composite mainComp = new Composite(mainShell, SWT.BORDER);

        final FormLayout compLayout = new FormLayout();
        compLayout.spacing = 5;
        compLayout.marginHeight = 2;
        compLayout.marginWidth = 2;
        mainComp.setLayout(compLayout);
        final FormData fd = new FormData();
        fd.left = new FormAttachment(2);
        fd.right = new FormAttachment(98);
        fd.top = new FormAttachment(2);
        mainComp.setLayoutData(fd);

        final Label idLabel = new Label(mainComp, SWT.LEFT | SWT.WRAP);
        idLabel.setText("Test ID: " + summary.getFullName());
        final FormData rd1 = new FormData();
        rd1.width = 470;
        rd1.top = new FormAttachment(0);
        rd1.left = new FormAttachment(0);
        idLabel.setLayoutData(rd1);

        final Label dirLabel = new Label(mainComp, SWT.LEFT | SWT.WRAP);
        dirLabel.setText("Output Directory: " + summary.getOutputDirectory());
        final FormData rd = new FormData();
        rd.width = 470;
        rd.left = new FormAttachment(0);
        rd.top = new FormAttachment(idLabel);
        dirLabel.setLayoutData(rd);
        dirLabel.setBounds(dirLabel.getBounds().x, dirLabel.getBounds().y, 600,
                150);

        final Label dbKeyLabel = new Label(mainComp, SWT.LEFT);
        dbKeyLabel.setText("Database Key: " + summary.getContextKey());
        final FormData rd9 = new FormData();
        rd9.left = new FormAttachment(0);
        rd9.top = new FormAttachment(dirLabel);
        dbKeyLabel.setLayoutData(rd9);

        final Label dbLabel = new Label(mainComp, SWT.LEFT);
        dbLabel.setText("Database Name: " + databaseName);
        final FormData rd10 = new FormData();
        rd10.left = new FormAttachment(0);
        rd10.top = new FormAttachment(dbKeyLabel);
        dbLabel.setLayoutData(rd10);

        final DateFormat df = TimeUtility.getFormatterFromPool();

        final Label startLabel = new Label(mainComp, SWT.LEFT);
        startLabel.setText("Start Time: " + df.format(summary.getStartTime()));
        final FormData rd2 = new FormData();
        rd2.left = new FormAttachment(0);
        rd2.top = new FormAttachment(dbLabel);
        startLabel.setLayoutData(rd2);

        final Label stopLabel = new Label(mainComp, SWT.LEFT);
        stopLabel.setText("Stop Time: " + df.format(summary.getStopTime()));
        final FormData rd3 = new FormData();
        rd3.left = new FormAttachment(0);
        rd3.top = new FormAttachment(startLabel);
        stopLabel.setLayoutData(rd3);

        TimeUtility.releaseFormatterToPool(df);

        final Composite frameComp = createFrameComposite(mainComp);
        final FormData rd4 = new FormData();
        rd4.left = new FormAttachment(0);
        rd4.top = new FormAttachment(stopLabel);
        rd4.right = new FormAttachment(49);
        frameComp.setLayoutData(rd4);

        final Composite packetComp = createPacketComposite(mainComp);
        final FormData rd5 = new FormData();
        rd5.left = new FormAttachment(frameComp, 5);
        rd5.top = new FormAttachment(stopLabel);
        rd5.right = new FormAttachment(100);
        packetComp.setLayoutData(rd5);

        final Composite productComp = createProductComposite(mainComp);
        final FormData rd6 = new FormData();
        rd6.left = new FormAttachment(0);
        rd6.bottom = new FormAttachment(100);
        rd6.right = new FormAttachment(100);
        productComp.setLayoutData(rd6);

        rd4.bottom = new FormAttachment(productComp);
        rd5.bottom = new FormAttachment(productComp);

        final Label line = new Label(mainShell, SWT.SEPARATOR | SWT.HORIZONTAL
                | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.left = new FormAttachment(0, 3);
        formData6.right = new FormAttachment(100, 3);
        formData6.top = new FormAttachment(mainComp, 5);
        line.setLayoutData(formData6);

        final Button okButton = new Button(mainShell, SWT.PUSH);
        okButton.setText("Ok");
        final FormData okFd = new FormData();
        okFd.top = new FormAttachment(line);
        okFd.right = new FormAttachment(100);
        okButton.setLayoutData(okFd);

        okButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                mainShell.close();
            }
        });
        mainShell.pack();
    }

    private Composite createPacketComposite(final Composite mainComp) {
        final Composite packetComp = new Composite(mainComp, SWT.BORDER);
        final RowLayout shellLayout = new RowLayout(SWT.VERTICAL);
        shellLayout.spacing = 5;
        shellLayout.marginHeight = 2;
        shellLayout.marginWidth = 2;
        packetComp.setLayout(shellLayout);

        if (summary != null) {
            if (summary instanceof IDownlinkSummary) {
                final IDownlinkSummary sum = (IDownlinkSummary) summary;

                addLabelIfNonZero(packetComp, sum.getPackets(), "Valid packets");
                addLabelIfNonZero(packetComp, sum.getBadPackets(), "Bad packets");
                addLabelIfNonZero(packetComp, sum.getProductPackets(), "Product packets");

                addLabelIfNonZero(packetComp, sum.getStationPackets(), "Station Monitor packets");

                addLabelIfNonZero(packetComp, sum.getCfdpPackets(), "CFDP packets");

                addLabelIfNonZero(packetComp, sum.getEvrPackets(), "EVR packets");
                addLabelIfNonZero(packetComp, sum.getEhaPackets(), "EHA packets");
                addLabelIfNonZero(packetComp, sum.getFillPackets(), "Fill packets");
            }
            else if (summary instanceof ITelemetryIngestorSummary) {
                final ITelemetryIngestorSummary sum = (ITelemetryIngestorSummary) summary;

                addLabelIfNonZero(packetComp, sum.getPackets(), "Valid packets");
                addLabelIfNonZero(packetComp, sum.getBadPackets(), "Bad packets");
                addLabelIfNonZero(packetComp, sum.getFillPackets(), "Fill packets");
                addLabelIfNonZero(packetComp, sum.getProductPackets(), "Product packets");
                addLabelIfNonZero(packetComp, sum.getStationPackets(),
                                  "Station Monitor packets");

                addLabelIfNonZero(packetComp, sum.getCfdpPackets(), "CFDP packets");
            }
            else if (summary instanceof ITelemetryProcessorSummary) {

                final ITelemetryProcessorSummary sum = (ITelemetryProcessorSummary) summary;
                addLabelIfNonZero(packetComp, sum.getEvrPackets(), "EVR packets");
                addLabelIfNonZero(packetComp, sum.getEhaPackets(), "EHA packets");
            }

        }

        return packetComp;
    }

    private Composite createProductComposite(final Composite mainComp) {
        final Composite prodComp = new Composite(mainComp, SWT.BORDER);
        final RowLayout shellLayout = new RowLayout(SWT.VERTICAL);
        shellLayout.spacing = 5;
        shellLayout.marginHeight = 2;
        shellLayout.marginWidth = 2;
        prodComp.setLayout(shellLayout);

        if (summary != null) {
            if (summary instanceof IDownlinkSummary) {
                final IDownlinkSummary sum = (IDownlinkSummary) summary;

                addLabelIfNonZero(prodComp, sum.getPartialProducts(), "Partial products");
                addLabelIfNonZero(prodComp, sum.getProducts(), "Complete products");
                addLabelIfNonZero(prodComp, sum.getProductDataBytes(), "Bytes of product data");
            }
            else if (summary instanceof ITelemetryProcessorSummary) {
                final ITelemetryProcessorSummary sum = (ITelemetryProcessorSummary) summary;

                addLabelIfNonZero(prodComp, sum.getPartialProducts(), "Partial products");
                addLabelIfNonZero(prodComp, sum.getProducts(), "Complete products");
                addLabelIfNonZero(prodComp, sum.getProductDataBytes(), "Bytes of product data");
            }
        }
        return prodComp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public Shell getShell() {
        return mainShell;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public String getTitle() {
        return TITLE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void open() {
        mainShell.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public boolean wasCanceled() {
        return false;
    }
}
