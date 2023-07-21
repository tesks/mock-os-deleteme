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
/**
 * 
 */
package jpl.gds.monitor.guiapp.gui;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.monitor.guiapp.common.MonitorSubscriber;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.time.TimeUtility;



/**
 * The MessageStatusShell class is a small composite that contains JMS status 
 * information
 */
public class MessageStatusShell implements ChillShell {
    /**
     * Message status title
     */
    public static final String TITLE = "Message Status";
    private Shell mainShell;
    private final Shell parent;
    private SubscriberComposite subscriberTable;
    private final List<MonitorSubscriber> subscriberList;
    
    /** Default window width */
    public static final int DEFAULT_WIDTH = 700;
    /** Default window height */
    public static final int DEFAULT_HEIGHT = 350;
    /** Default table height */
    public static final int DISCARD_TABLE_HEIGHT = 190;
    
    /**
     * Creates an instance of MessageStatusShell.
     * @param subs list of MonitorSubscribers to display info for
     * @param parent the parent Shell of this widget
     */
    public MessageStatusShell(final List<MonitorSubscriber> subs, final Shell parent) {
        this.parent = parent;
        this.subscriberList = subs;
        createControls();
    }
    
    
    private void createControls() {
        try {
           this.mainShell = new Shell(this.parent, SWT.DIALOG_TRIM | SWT.RESIZE);
           this.mainShell.setText("Message Status");
           
           final FormLayout layout = new FormLayout();
           layout.spacing = 5;
           this.mainShell.setLayout(layout);
           
           final Composite subComposite = new Composite(this.mainShell, SWT.NONE);
           final RowLayout sublayout = new RowLayout(SWT.VERTICAL);
           sublayout.spacing = 5;
           sublayout.fill = true;
           subComposite.setLayout(sublayout);
           final FormData subFd = new FormData();
           subFd.top = new FormAttachment(0);
           subFd.left = new FormAttachment(0);
           subFd.right = new FormAttachment(100);
           subComposite.setLayoutData(subFd);
           
           this.subscriberTable = new SubscriberComposite(subComposite, subscriberList);

           final Composite buttonComposite = new Composite(this.mainShell, SWT.NONE);
           final FormLayout fl = new FormLayout();
           buttonComposite.setLayout(fl);
           final FormData buttonFd = new FormData();
           buttonFd.top = new FormAttachment(subComposite,0, 5);
           buttonFd.right = new FormAttachment(100);
           buttonFd.left = new FormAttachment(0, 5);
           buttonComposite.setLayoutData(buttonFd);
           
           final Button resetButton = new Button(buttonComposite, SWT.PUSH);
           resetButton.setText("Reset Discard Counts");
           
           final Button cancelButton = new Button(buttonComposite, SWT.PUSH);
           cancelButton.setText("Close");
           FormData fd = new FormData();
           fd.right = new FormAttachment(100, -5);
           cancelButton.setLayoutData(fd);
           
           final Button applyButton = new Button(buttonComposite, SWT.PUSH);
           applyButton.setText("Refresh");
           this.mainShell.setDefaultButton(applyButton);
           fd = new FormData();
           fd.right = new FormAttachment(cancelButton, -10, SWT.LEFT);
           applyButton.setLayoutData(fd);
        
           cancelButton.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                   MessageStatusShell.this.mainShell.close();
               }
           });
           
           
           applyButton.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                   subscriberTable.update();
               }
           });
           
           resetButton.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                   subscriberTable.resetCounts();
               }
           });
           
           this.mainShell.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
           
        } catch (final Exception e) {
            e.printStackTrace();
            TraceManager.getDefaultTracer().error("Unable to create Message Status Window");

        }
        this.subscriberTable.update();
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getShell()
     */
    @Override
	public Shell getShell() {
        return this.mainShell;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getTitle()
     */
    @Override
	public String getTitle() {
         return TITLE;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#open()
     */
    @Override
	public void open() {
        this.mainShell.open();
        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
     */
    @Override
	public boolean wasCanceled() {
        return false;
    }
    
    /**
     * Inner composite that contains topic, messages received count, last, 
     * message number, last receive time and lag time
     *
     */
    public static class SubscriberComposite extends Composite {
        private final List<MonitorSubscriber> subscribers;
        private Label received;
        private Label lastTime;
        private Label lag;
        private Table table;

        /**
         * Constructor: calls parent constructor, sets member variables and 
         * creates GUI components
         * @param parent parent composite
         * @param subs list of monitor subscribers
         */
        public SubscriberComposite(final Composite parent, final List<MonitorSubscriber> subs) {
            super(parent, SWT.BORDER);
            this.subscribers = subs;
            createControls();
            this.setSize(DEFAULT_WIDTH, 400);
        }

        private void createControls() {
            final FormLayout layout = new FormLayout();
            setLayout(layout);
            layout.spacing = 2;
            layout.marginLeft = 3;
            layout.marginTop = 3;

            final Composite topicComposite = new Composite(this, SWT.NONE);
            FormLayout formLayout = new FormLayout();
            topicComposite.setLayout(formLayout);

            this.received = new Label(topicComposite, SWT.NONE);
            FormData data = new FormData();
            data.left = new FormAttachment(0);
            data.top = new FormAttachment(5);
            received.setLayoutData(data);

            this.lastTime = new Label(topicComposite, SWT.NONE);
            data = new FormData();
            data.left = new FormAttachment(received, 10);
            data.top = new FormAttachment(5);
            lastTime.setLayoutData(data);

            this.lag = new Label(topicComposite, SWT.NONE);
            data = new FormData();
            data.left = new FormAttachment(lastTime, 10);
            data.top = new FormAttachment(5);
            lag.setLayoutData(data);

            data = new FormData();
            data.top = new FormAttachment(5);
            data.left = new FormAttachment(0);
            topicComposite.setLayoutData(data);

            final ScrolledComposite messageDiscardComposite = new ScrolledComposite(this, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
            formLayout = new FormLayout();
            messageDiscardComposite.setLayout(formLayout);
            
            data = new FormData(DEFAULT_WIDTH, SWT.DEFAULT);
            data.top = new FormAttachment(topicComposite, 0 , 10);
            data.left = new FormAttachment(0);
            data.right = new FormAttachment(100);
            data.bottom = new FormAttachment(90);
            messageDiscardComposite.setLayoutData(data);

            table = new Table(messageDiscardComposite, SWT.NONE);
            table.setHeaderVisible(true);
            table.setLinesVisible(true);
       
            messageDiscardComposite.setSize(DEFAULT_WIDTH, DISCARD_TABLE_HEIGHT);
            final Rectangle clientArea = messageDiscardComposite.getClientArea();

            table.setBounds(clientArea.x, clientArea.y, DEFAULT_WIDTH - 20, DISCARD_TABLE_HEIGHT);

            data = new FormData();
            data.top = new FormAttachment(0);
            data.left = new FormAttachment(0);
            data.right = new FormAttachment(100);
            data.bottom = new FormAttachment(100);
            table.setLayoutData(data);

            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText("Topic");
            column.setWidth(300);

            column = new TableColumn(table, SWT.NONE);
            column.setText("Message Type");
            column.setWidth(200);

            column = new TableColumn(table, SWT.NONE);
            column.setText("Discard Count");
            column.setWidth(150);

            final TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, "Unknown");
            item.setText(1, "None");
            item.setText(2, "0");
            update();

            messageDiscardComposite.setContent(table);
        }
        
        private long getReceiveCount() {
            long count = 0;
            for (final MonitorSubscriber subscriber: subscribers) {
                count += subscriber.getReceiveCount();
            }
            return count;    
        }
        
        private String getLastReceiveTime() {
            long last = 0;
            for (final MonitorSubscriber subscriber: subscribers) {
                last = Math.max(last, subscriber.getLastReceiveTime());
            }
            return last == 0 ?  "None" : TimeUtility.getFormatter().format(new Date(last));
        }
        
        private long getLongestLagTime() {
            long longest = 0;
            for (final MonitorSubscriber subscriber: subscribers) {
                longest = Math.max(longest, subscriber.getLagTime());
            }
            return longest / 1000;
        }
        
        @Override
        public void update() {
            this.received.setText("Received: " + getReceiveCount()); 
            FormData data = new FormData();
            data.left = new FormAttachment(0);
            received.setLayoutData(data);

            this.lastTime.setText("Last Time: " + getLastReceiveTime());
            data = new FormData();
            data.left = new FormAttachment(received, 10, SWT.RIGHT);
            lastTime.setLayoutData(data);

            this.lag.setText("Lag: " + getLongestLagTime() + " seconds"); 
            data = new FormData();
            data.left = new FormAttachment(lastTime, 10);
            lag.setLayoutData(data);

            if (this.lag.getBackground() != null && !this.lag.getBackground().isDisposed()) {
                this.lag.getBackground().dispose();
            }
            if (getLongestLagTime()  > 45000) {
                this.lag.setBackground(ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.RED)));
            } else {
                this.lag.setBackground(null);
            }
            this.layout();

            table.removeAll();

            for (final MonitorSubscriber subscriber: subscribers) {
                final Iterator<String> iterator = subscriber.getDroppedMessages().keySet().iterator();

                while(iterator.hasNext()) {
                    final String msgType = iterator.next();
                    final int count = subscriber.getDroppedMessages().get(msgType).getCount();

                    /*
                     * Note: We've split up the message types
                     * by their message source IDs (source application's PID,
                     * namely). If we see dropped messages from more than one
                     * application, for the same type, the table will show these
                     * counts in separate rows. This might confuse the users, but
                     * it's unlikely that this will occur enough to be a nuisance.
                     * If it does, we'll have to aggregate the counts by type before
                     * writing it on the GUI.
                     */
                    if(count > 0) {
                        final TableItem item = new TableItem(table, SWT.NONE);
                        item.setText(0, subscriber.getTopic());
                        item.setText(1, msgType.split("/")[0]); // because the key is "<type>/<source ID>" combo
                        item.setText(2, String.valueOf(count));
                    }
                }
            }
            //for SWT scrolling...
            table.setSelection(table.getItemCount() - 1);
            table.deselectAll();
        }

        /**
         * Resets all the message counts to 0
         */
        public void resetCounts() {
            for (final MonitorSubscriber subscriber: subscribers) {
                subscriber.reset();
            }
            this.update();
        }
    }
    
    /**
     * Method for testing without having to run chill_monitor
     * @param args command line args - none
     */
    public static void main(final String[] args) {
        // Execute the GUI application
        final List<MonitorSubscriber> subs = new LinkedList<>();
        subs.add(new MonitorSubscriber(null, "Topic A", TraceManager.getDefaultTracer()));
        subs.get(0).update(1000010L, 1000000L, 1000, 1, EvrMessageType.Evr);
        subs.get(0).update(1000200L, 1000000L, 1000, 3, EvrMessageType.Evr);
        subs.get(0).update(1000000L, 1000000L, 1000, 2, EhaMessageType.AlarmedEhaChannel);
        subs.get(0).update(1000300L, 1000000L, 1000, 5, EhaMessageType.AlarmedEhaChannel);
        subs.add(new MonitorSubscriber(null, "Topic B", TraceManager.getDefaultTracer()));
        subs.get(1).update(1000000L, 10000010L, 1000, 1, EvrMessageType.Evr);
        subs.get(1).update(1000200L, 1000000L, 1000, 3, EvrMessageType.Evr);
        subs.get(1).update(1000000L, 10000020L, 1000, 2, EhaMessageType.AlarmedEhaChannel);
        subs.get(1).update(1000300L, 1000000L, 1000, 4, EhaMessageType.AlarmedEhaChannel);
        subs.add(new MonitorSubscriber(null, "Topic C", TraceManager.getDefaultTracer()));
        subs.get(2).update(1000000L, 10000010L, 1000, 1, EvrMessageType.Evr);
        subs.get(2).update(1000200L, 1000000L, 1000, 3, EvrMessageType.Evr);
        subs.get(2).update(1000000L, 10000020L, 1000, 2, EhaMessageType.AlarmedEhaChannel);
        subs.get(2).update(1000300L, 1000000L, 1000, 4, EhaMessageType.AlarmedEhaChannel);
        subs.get(2).update(1000000L, 10000020L, 1000, 2, ProductMessageType.ProductAssembled);
        subs.get(2).update(1000500L, 1000000L, 1000, 7, ProductMessageType.ProductAssembled);
        subs.add(new MonitorSubscriber(null, "Topic D", TraceManager.getDefaultTracer()));
        subs.get(3).update(1000000L, 10000010L, 1000, 1, EvrMessageType.Evr);
        subs.get(3).update(1000200L, 1000000L, 1000, 3, EvrMessageType.Evr);
        subs.get(3).update(1000000L, 10000020L, 1000, 2, EhaMessageType.AlarmedEhaChannel);
        subs.get(3).update(1000300L, 1000000L, 1000, 4, EhaMessageType.AlarmedEhaChannel);
        subs.get(3).update(1000000L, 10000020L, 1000, 2, ProductMessageType.ProductAssembled);
        subs.get(3).update(1000500L, 1000000L, 1000, 7, ProductMessageType.ProductAssembled);
        subs.add(new MonitorSubscriber(null, "Topic E", TraceManager.getDefaultTracer()));
        subs.get(4).update(1000000L, 10000010L, 1000, 1, EvrMessageType.Evr);
        subs.get(4).update(1000200L, 1000000L, 1000, 3, EvrMessageType.Evr);
        subs.get(4).update(1000000L, 10000020L, 1000, 2, EhaMessageType.AlarmedEhaChannel);
        subs.get(4).update(1000300L, 1000000L, 1000, 4, EhaMessageType.AlarmedEhaChannel);
        subs.get(4).update(1000000L, 10000020L, 1000, 2, ProductMessageType.ProductAssembled);
        subs.get(4).update(1000500L, 1000000L, 1000, 7, ProductMessageType.ProductAssembled);
        
        final Display display = new Display();
        final Shell shell = new Shell(display);
        
        final MessageStatusShell statusShell = new MessageStatusShell(subs, shell);
        statusShell.open();

        while (!statusShell.getShell().isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }
}
