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
package jpl.gds.monitor.fixedbuilder;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jpl.gds.shared.swt.SWTUtilities;

/**
 * This class implements a status bar for the fixed view builder. The status bar
 * includes the fixed view name, size, and last reported message. The message
 * will scroll in a ticker like fashion, or this scrolling can be disabled.
 * 
 */
public class FixedBuilderStatusComposite {

    private static final String MESSAGE_NONE = "None  ";
    private static final String UNDEFINED = "Undefined  ";
    private static final String NAME_NONE = "None                        ";
    private static final String MESSAGE_TAG = "Message:";
    private static final String NAME_TAG = "Name:";
    private static final String SIZE_TAG = "Size:";
    private static final int TICKER_INTERVAL = 100;
    private static final int TICKER_LENGTH = 85;

    // Timer that controls the scrolling ticker
    private Timer ticker;

    // Data used by the composite to store state
    private boolean characterMode;
    private String lastMessage;
    private final StringBuilder tickerMessage = new StringBuilder(TICKER_LENGTH);
    private boolean useTicker;

    // GUI components
    private final Shell parent;
    private Composite mainComp;
    private Label messageLabel;
    private Label nameLabel;
    private Label sizeLabel;
    private Button tickerButton;

    /**
     * Creates a FixedBuilderStatusComposite with the given parent shell.
     * 
     * @param parent
     *            the parent Shell widget
     */
    public FixedBuilderStatusComposite(final Shell parent) {
        this.parent = parent;
        createGui();
    }

    /**
     * Turns on and off the scrolling message ticker.
     * 
     * @param en
     *            true to enable the ticker, false to stop it
     */
    public void enableTicker(final boolean en) {
        this.useTicker = en;
        this.tickerButton.setSelection(en);
        stopTickerTimer();
        if (en == false) {
            setMessage(this.lastMessage);
        } else {
            initTickerMessage();
            startTickerTimer();
        }
    }

    /**
     * Gets the main Composite widget for this class.
     * 
     * @return Composite object
     */
    public Composite getComposite() {
        return this.mainComp;
    }

    /**
     * Resets the status bar contents to all "undefined."
     */
    public void reset() {
        this.nameLabel.setText(NAME_NONE);
        this.sizeLabel.setText(UNDEFINED);
        setMessage(MESSAGE_NONE);
    }

    /**
     * Sets the flag indicating whether the builder is in character-coordinate
     * mode.
     * 
     * @param enable
     *            true to enable character mode; false to disable
     */
    public void setCharacterMode(final boolean enable) {
        this.characterMode = enable;
    }

    /**
     * Changes the status message to the given text.
     * 
     * @param message
     *            the message text to set
     */
    public void setMessage(final String message) {
        if (this.useTicker) {
            stopTickerTimer();
        }
        this.lastMessage = message + "  ";
        this.messageLabel.setText(this.lastMessage);
        this.mainComp.layout();

        if (this.useTicker) {
            initTickerMessage();
            startTickerTimer();
        }
    }

    /**
     * Sets the name of the current fixed layout.
     * 
     * @param name
     *            name of the view
     */
    public void setName(final String name) {
        this.nameLabel.setText(name + "  ");
        this.mainComp.layout();
    }

    /**
     * Sets the size of the current fixed layout. Coordinates are assumed to
     * match the builder's current coordinate system.
     * 
     * @param width
     *            width of view, in pixels or characters
     * @param height
     *            height of view, in pixels or characters
     */
    public void setSize(final int width, final int height) {
        this.sizeLabel.setText("(" + String.valueOf(width) + "x"
                + String.valueOf(height) + (this.characterMode ? "c" : "p")
                + ")  ");
        this.mainComp.layout();
    }

    // Entry point for GUI creation
    private void createGui() {
        try {
            this.mainComp = new Composite(this.parent, SWT.BORDER);
            this.mainComp.setLayout(new FormLayout());

            final Composite labelComp = new Composite(this.mainComp, SWT.NONE);
            final RowLayout rl = new RowLayout(SWT.HORIZONTAL);
            rl.spacing = 5;
            rl.wrap = false;
            labelComp.setLayout(rl);

            final FormData lcfd = new FormData();
            lcfd.left = new FormAttachment(0);
            lcfd.top = new FormAttachment(0);
            lcfd.right = new FormAttachment(100);
            labelComp.setLayoutData(lcfd);

            final Color blue = this.mainComp.getDisplay().getSystemColor(
                    SWT.COLOR_BLUE);

            final Label staticNameLabel = new Label(labelComp, SWT.NONE);
            staticNameLabel.setText(NAME_TAG);

            this.nameLabel = new Label(labelComp, SWT.NONE);
            this.nameLabel.setText(NAME_NONE);
            this.nameLabel.setForeground(blue);

            final Label staticSizeLabel = new Label(labelComp, SWT.NONE);
            staticSizeLabel.setText(SIZE_TAG);

            this.sizeLabel = new Label(labelComp, SWT.NONE);
            this.sizeLabel.setText("Undefined   ");
            this.sizeLabel.setForeground(blue);

            final Label staticMessageLabel = new Label(labelComp, SWT.NONE);
            staticMessageLabel.setText(MESSAGE_TAG);

            this.messageLabel = new Label(labelComp, SWT.NONE);
            this.messageLabel.setText(MESSAGE_NONE);
            this.messageLabel.setForeground(blue);

            this.tickerButton = new Button(this.mainComp, SWT.CHECK);
            this.tickerButton.setSelection(true);
            final FormData tbfd = new FormData();
            tbfd.right = new FormAttachment(100, -8);
            tbfd.top = new FormAttachment(0);
            this.tickerButton.setLayoutData(tbfd);
            lcfd.right = new FormAttachment(this.tickerButton);

            this.tickerButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(
                        final org.eclipse.swt.events.SelectionEvent e) {
                    try {
                        FixedBuilderStatusComposite.this.useTicker = FixedBuilderStatusComposite.this.tickerButton
                                .getSelection();
                        if (FixedBuilderStatusComposite.this.useTicker) {
                            setMessage(FixedBuilderStatusComposite.this.lastMessage);
                        } else {
                            stopTickerTimer();
                            setMessage(FixedBuilderStatusComposite.this.lastMessage);
                        }
                    } catch (final Exception e1) {
                        e1.printStackTrace();
                    }
                }
            });

            this.parent.addDisposeListener(new DisposeListener() {

                @Override
                public void widgetDisposed(final DisposeEvent arg0) {
                    stopTickerTimer();
                }
            });
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void initTickerMessage() {
        this.tickerMessage.setLength(0);
        this.tickerMessage.append(this.lastMessage);
        while (this.tickerMessage.length() < TICKER_LENGTH) {
            this.tickerMessage.append(this.lastMessage);
        }
    }

    private synchronized void rotateTickerText() {
        if (this.tickerMessage.length() == 0) {
            return;
        }
        final char firstChar = this.tickerMessage.charAt(0);
        this.tickerMessage.deleteCharAt(0);
        this.tickerMessage.append(firstChar);
    }

    private void startTickerTimer() {
        if (this.ticker != null) {
            this.ticker.cancel();
            this.ticker = null;
        }

        this.ticker = new Timer();
        this.ticker.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                if (FixedBuilderStatusComposite.this.parent.isDisposed()) {
                    return;
                }
                SWTUtilities.safeAsyncExec(
                        FixedBuilderStatusComposite.this.parent.getDisplay(),
                        "startTicker", new Runnable() {
                            @Override
                            public void run() {
                                if (FixedBuilderStatusComposite.this.messageLabel
                                        .isDisposed()) {
                                    return;
                                }
                                try {
                                    rotateTickerText();
                                    if (FixedBuilderStatusComposite.this.tickerMessage
                                            .length() != 0) {
                                        FixedBuilderStatusComposite.this.messageLabel
                                                .setText(FixedBuilderStatusComposite.this.tickerMessage
                                                        .substring(0,
                                                                TICKER_LENGTH));
                                    }
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                    return;
                                }
                            }
                        });
            }
        }, TICKER_INTERVAL, TICKER_INTERVAL);
    }

    private void stopTickerTimer() {
        if (this.ticker != null) {
            this.ticker.cancel();
            this.ticker = null;
        }
    }
}
