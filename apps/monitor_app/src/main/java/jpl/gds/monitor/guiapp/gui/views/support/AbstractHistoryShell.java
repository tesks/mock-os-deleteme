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
package jpl.gds.monitor.guiapp.gui.views.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.db.api.sql.fetch.IChannelValueFetch;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.globallad.GlobalLadException;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState;
import jpl.gds.globallad.utilities.ICoreGlobalLadQuery;
import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.guiapp.gui.views.AbstractBasicTableComposite;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.monitor.perspective.view.RealtimeRecordedSupport;
import jpl.gds.monitor.perspective.view.StationSupport;
import jpl.gds.monitor.perspective.view.channel.ChannelDisplayFormat;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.ProgressBarShell;
import jpl.gds.shared.swt.SWTUtilities;


/**
 * The AbstractHistoryShell class is the base class for channel history windows. It provides
 * common implementation of the channel history for the Alarm, Channel List, and Fixed views.
 * It is capable of displaying history from the LAD or from the database.
 */
public abstract class AbstractHistoryShell implements ChillShell{
    private final Tracer                 log;

    /**
     * Title for history windows
     */
    public static final String TITLE = "Channel History";

    // The default number of records to query with each request is in the config file
    private final int maxQueryRecords;

    private static final int MAX_HEIGHT = 600;
    private static final int MIN_HEIGHT = 400;
    private static final long PROGRESS_INTERVAL = 150;

    private Shell mainShell;
    private AbstractBasicTableComposite tableComposite;
    private final Shell parent;

    /**
     * Channel, Fixed or Alarm View configuration that was chosen to show channel history for
     */
    protected final IViewConfiguration viewConfig;

    private Label noDataLabel;
    private final Point location;
    private String channelId;
    private ChannelDisplayFormat displayItem;
    /*
     *
     * Realtime/recorded is no longer a boolean and
     * logic for deciding what to display is more 
     * complex now.
     */
    private boolean monitor;
    private Button refreshButton;
    private Button queryButton;
    private GLADQueryThread queryThread;
    private ProgressBarShell progressShell;
    private Timer progressTimer;
    private Spinner queryCountSpinner;
    private int maxQuerySize;
    private IChannelValueFetch fetch;
    private final Object lock = new Object();
    /** Current application context */
	protected ApplicationContext appContext;
	/** Channel definition provider */
	protected IChannelDefinitionProvider chanDefs;
    protected GlobalLadProperties        gladConfig;


    /**
     * Constructor.
     * @param appContext the current application context
     * @param parent parent shell for this shell
     * @param useLocation location at which to place this shell
     * @param config the ViewConfiguration object for the view for which history is to be posted
     */
    public AbstractHistoryShell(final ApplicationContext appContext, final Shell parent, final Point useLocation, final IViewConfiguration config) {
    	this.maxQueryRecords = appContext.getBean(MonitorGuiProperties.class).getHistoryMaxQueryRecords();
        log = TraceManager.getTracer(appContext, Loggers.UTIL);
    	this.chanDefs = appContext.getBean(IChannelDefinitionProvider.class);
    	this.maxQuerySize = maxQueryRecords;
    	this.appContext = appContext;
        this.parent = parent;
        viewConfig = config;
        location = useLocation;
        this.gladConfig = GlobalLadProperties.getGlobalInstance();
        createControls();
    }

    /**
     * Creates GUI widgets and controls.
     */
    private void createControls() {
        mainShell = new Shell(parent, SWT.SHELL_TRIM);
        mainShell.setLayout(new FormLayout());

        final Composite shellComposite = new Composite(mainShell, SWT.NONE);
        final FormLayout fl = new FormLayout();
        fl.spacing = 10;
        shellComposite.setLayout(fl);
        final FormData shellFd = new FormData();
        shellFd.left = new FormAttachment(1);
        shellFd.right = new FormAttachment(99);
        shellFd.top = new FormAttachment(1);
        shellFd.bottom = new FormAttachment(99);
        shellComposite.setLayoutData(shellFd);

        noDataLabel = new Label(shellComposite, SWT.NONE);
        final FormData labelFd = new FormData();
        labelFd.top = new FormAttachment(0,5);
        labelFd.left = new FormAttachment(0);
        noDataLabel.setLayoutData(labelFd);        

        final Composite queryComposite = new Composite(shellComposite, SWT.NONE);
        queryComposite.setLayout(new FormLayout());
        final FormData queryFd = new FormData();
        queryFd.left = new FormAttachment(0);
        queryFd.top = new FormAttachment(noDataLabel, 0, 10);
        queryComposite.setLayoutData(queryFd);

        final Label queryCountLabel = new Label(queryComposite, SWT.NONE);
        queryCountLabel.setText("Max Number of Records to Query:");
        final FormData qlFd = new FormData();
        qlFd.left = new FormAttachment(0);
        qlFd.top = new FormAttachment(0,5);
        queryCountLabel.setLayoutData(qlFd);
        queryCountSpinner = new Spinner(queryComposite, SWT.BORDER);
        queryCountSpinner.setMinimum(5);
        queryCountSpinner.setMaximum(maxQueryRecords);
        queryCountSpinner.setIncrement(5);
        queryCountSpinner.setDigits(0);
        queryCountSpinner.setSelection(maxQueryRecords);
        final FormData qtFd = new FormData();
        qtFd.left = new FormAttachment(queryCountLabel);
        qtFd.top = new FormAttachment(queryCountLabel,0, SWT.CENTER);
        queryCountSpinner.setLayoutData(qtFd);

        queryCountLabel.setVisible(false);
        queryCountSpinner.setVisible(false);

        tableComposite = createTableComposite(shellComposite, viewConfig);
        final FormData tableFd = new FormData();
        tableFd.left = new FormAttachment(0);
        tableFd.right = new FormAttachment(100);
        tableFd.top = new FormAttachment(queryComposite, 0, 12);
        tableComposite.setLayoutData(tableFd);

        final Composite composite = new Composite(shellComposite, SWT.NONE);
        final GridLayout rl = new GridLayout(3, false);
        composite.setLayout(rl);
        final FormData formData8 = new FormData();
        formData8.right = new FormAttachment(100);
        formData8.bottom = new FormAttachment(100);
        composite.setLayoutData(formData8);
        tableFd.bottom = new FormAttachment(composite);

        queryButton = new Button (composite, SWT.PUSH);
        queryButton.setText("Query from Global LAD");

        refreshButton = new Button(composite, SWT.PUSH);
        refreshButton.setText("Refresh from Local LAD");
        mainShell.setDefaultButton(refreshButton);

        final Button applyButton = new Button(composite, SWT.PUSH);
        applyButton.setText("Close");
        
        final Shell finalMainShell = mainShell;

        applyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                mainShell.close();
            }
        });
        refreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    noDataLabel.setText("Current data is from the Local LAD");
                    queryCountSpinner.setVisible(false);
                    queryCountLabel.setVisible(false);

                    refreshFromLad();
                } catch (final RuntimeException e1) {
                    e1.printStackTrace();
                }
            }
        });
        queryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {             	
                	if(GlobalLadProperties.getGlobalInstance().isEnabled()) {
                    noDataLabel.setText("Current data is from the Global LAD");
                      queryCountSpinner.setVisible(true);
                      queryCountLabel.setVisible(true);
                      queryButton.setEnabled(false);
                      refreshButton.setEnabled(false);
                      queryFromDatabase();                   
                	} else {
                		SWTUtilities.showMessageDialog(finalMainShell, 
                				"Global LAD is Disabled", 
                				"Cannot query from database because the global LAD is disabled.");
                	}                    
                } catch (final RuntimeException e1) {
                    e1.printStackTrace();
                }
            }
        });

        mainShell.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(final DisposeEvent arg0) {
                synchronized(lock) {
                    if (fetch != null) {
                        log.info("History query aborted by user request.");
                        fetch.abortQuery();
                        fetch.close();
                    }
                }
            }
        });

        noDataLabel.setText("Current data is from the Local LAD");
        mainShell.setLocation(location);
    }

    /**
     * Creates the BasicTableComposite used to display channel values. Overridden by specific
     * history classes.
     * 
     * @param parent parent Composite for the table
     * @param config the ViewConfiguration object for the view posting this history
     * 
     * @return new BasicTableComposite
     */
    public abstract AbstractBasicTableComposite createTableComposite(Composite parent, IViewConfiguration config);

    /**
     * Sets shell title, adds channels from LAD to the table and sets window size
     * 
     * @param dataList Latest Available Data for selected channel
     * @param displayStuff display characteristics including channel ID
     * @param pack true if shell contents should be compacted, false otherwise
     */
    public void setChannelValues(final List<MonitorChannelSample> dataList, final ChannelDisplayFormat displayStuff, final boolean pack) {
        if (mainShell.isDisposed()) {
            return;
        }
        mainShell.setText("LAD Values for Channel " + displayStuff.getChanId());
        channelId = displayStuff.getChanId();
        final IChannelDefinition def = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(channelId);
        if (def == null) {
            return;
        }
        displayItem = displayStuff;
        final ChannelDefinitionType type = def == null ? ChannelDefinitionType.FSW: def.getDefinitionType();		
        monitor = type.equals(ChannelDefinitionType.M);
        
        if (dataList == null || dataList.isEmpty()) {
            setNoChannelValues(displayStuff.getChanId(), pack);
            return;
        }

        for (final MonitorChannelSample data: dataList) {
            tableComposite.addChannelValue(def, displayStuff, data);
        }
        if (pack) {
            mainShell.pack();
            final Point size = mainShell.getSize();
            if (size.y > MAX_HEIGHT) {
                mainShell.setSize(size.x, MAX_HEIGHT);
            }
            if (size.y < MIN_HEIGHT) {
                mainShell.setSize(size.x, MIN_HEIGHT);
            }
        }
    }

    private void setNoChannelValues(final String id, final boolean pack) {
        noDataLabel.setText("There is currently no history for channel " + id);
        tableComposite.getTable().setItemCount(1);
        if (pack) {
            mainShell.pack();
        }
    }

    private void refreshFromLad() {
        /*
         * Realtime/recorded filter is now
         * an enum rather than a boolean, and station ID is required for
         * LAD access.
         */
        final List<MonitorChannelSample> list = appContext.getBean(MonitorChannelLad.class).getValueHistory(channelId, 
                ((RealtimeRecordedSupport)this.viewConfig).getRealtimeRecordedFilterType(), 
                ((StationSupport)this.viewConfig).getStationId());
        tableComposite.clearRows();
        setChannelValues(list, displayItem, true);
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getShell()
     */
    @Override
    public Shell getShell() {
        return mainShell;
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
        mainShell.open();
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
     * Starts a query to the database on another thread.
     */
    private void queryFromDatabase() {
        if (appContext.getBean(IContextIdentification.class).getNumber() == null) {
            SWTUtilities.showMessageDialog(mainShell, "Can't Query Yet", "You cannot query at this time because the session ID is still unknown");
            refreshButton.setEnabled(true);
            queryButton.setEnabled(true);
            return;
        }
        tableComposite.clearRows();

        maxQuerySize = queryCountSpinner.getSelection();
        //queryCountSpinner.setSelection(queryBatchSize);

        progressShell = null;
        progressShell = new ProgressBarShell(mainShell);

        progressShell.getShell().setText("Querying...");
        progressShell.getProgressLabel().setText("Querying values for channel " + channelId + "...");
        progressShell.getProgressBar().setMinimum(0);
        progressShell.getProgressBar().setMaximum(100);
        progressShell.getProgressBar().setSelection(0);
        final Point p = mainShell.getLocation();
        progressShell.getShell().setLocation(p);

        progressShell.open();
        progressShell.getShell().setSize(400,50);
        if (progressTimer != null) {
            progressTimer.cancel();
        }

        progressTimer = new Timer();
        SWTUtilities.startProgressBarUpdate(progressShell, progressTimer, PROGRESS_INTERVAL, 5);

        queryThread = new GLADQueryThread();
        new Thread(queryThread).start();
    }

    /**
     * Called by the query thread when the query is complete.
     * 
     * @param channelRecords List of ChannelSample objects resulting from the query
     */
    private void notifyQueryDone(final List<MonitorChannelSample> channelRecords) {

        SWTUtilities.runInDisplayThread(new Runnable()  {
            @Override
            public String toString() {
                return "ChannelHistoryShell.notifyQueryDone.Runnable";
            }

            @Override
            public void run() {
                try {
                    if (progressTimer != null) {
                        progressTimer.cancel();
                        progressTimer = null;
                    }
                    if (progressShell != null && !progressShell.getShell().isDisposed()) {
                        progressShell.dispose();
                        progressShell = null;
                    }
                    if (mainShell == null || mainShell.isDisposed()) {
                        return;
                    }
                    if (channelRecords == null) {
                        SWTUtilities.showErrorDialog(mainShell, "Query Error", "There was a problem querying the Global LAD for channel " + channelId + 
                                ": " + queryThread.getGlobalLadError());
                        setNoChannelValues(channelId, true);
                    } else if (channelRecords.isEmpty()) {
                        SWTUtilities.showMessageDialog(mainShell, "No Records Found", "No records for channel " + channelId + 
                                " were found in the database for session " + appContext.getBean(IContextIdentification.class).getNumber());
                        setNoChannelValues(channelId, true);
                    } else {
                        setChannelValues(channelRecords, displayItem, true);
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                } finally {
                    if (!mainShell.isDisposed()) {
                        refreshButton.setEnabled(true);
                        queryButton.setEnabled(true);

                    }
                }
            }
        });
    }

    /**
     * This class implements the channel query thread.
     *
     */
    private class GLADQueryThread implements Runnable {
        private String globalLadError;

        /**
         * Gets the latest SQL error string, or null if no error occurred.
         * 
         * @return error string or null
         */
        public String getGlobalLadError() {
            return globalLadError;
        }

        /**
         * {@inheritDoc}
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run () {

        		ICoreGlobalLadQuery ladQuery = null;

            try {
            		ladQuery = appContext.getBean(ICoreGlobalLadQuery.class);
            	} catch (final Exception e) {
                globalLadError = "Failed to create global lad data factory: " + e.getMessage();
                notifyQueryDone(null);
                return;
            }

            final int scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
            final VenueType venue = appContext.getBean(IVenueConfiguration.class).getVenueType();
            final RealtimeRecordedFilterType realtimeRecordedType = ((RealtimeRecordedSupport)viewConfig).getRealtimeRecordedFilterType();
            final Long sessionId = appContext.getBean(IContextIdentification.class).getNumber();
            final String sessionHost = appContext.getBean(IContextIdentification.class).getHost();

            RecordedState recordedState;

            switch(realtimeRecordedType) {
            case REALTIME:
            		recordedState = RecordedState.realtime;
                break;
            case RECORDED:
        			recordedState = RecordedState.recorded;
                break;
            case BOTH:
        			recordedState = RecordedState.both;
                break;
            default:
                globalLadError = "Unrecognized realtime/recorded filter";
                notifyQueryDone(null);
                return;
            }

            /**
             * Map the time strategy to the global lad time value to use for the query.
             */
            GlobalLadPrimaryTime timeType;
            
            switch (appContext.getBean(TimeComparisonStrategyContextFlag.class).getTimeComparisonStrategy()) {
			case ERT:
				timeType = GlobalLadPrimaryTime.ERT;
				break;
			case SCET:
				timeType = GlobalLadPrimaryTime.SCET;
				break;
			case SCLK:
				timeType = GlobalLadPrimaryTime.SCLK;
				break;
			case LAST_RECEIVED:
			default:
				timeType = GlobalLadPrimaryTime.EVENT;
				break;
            }
            
            final Collection<Integer> stationIds = monitor ? 
            		Arrays.asList(((StationSupport)viewConfig).getStationId()) :
            		Collections.<Integer>emptyList();
            		
            	Collection<IClientChannelValue> history;

			try {
				history = ladQuery.getChannelHistory(
						channelId, 
						DataSource.all, 
						recordedState, 
						timeType, 
						scid, 
						venue, 
						sessionHost, 
						sessionId, 
						stationIds, 
						maxQuerySize);

	            final List<MonitorChannelSample> channelRecords = new ArrayList<MonitorChannelSample>();
	            
	            for (final IClientChannelValue val : history) {
	                channelRecords.add(MonitorChannelSample.create(chanDefs, val));
	            }

	            notifyQueryDone(channelRecords);
			} catch (final GlobalLadException e) {
				globalLadError = "Failed to query Global LAD for channel history: " + e.getMessage();
				notifyQueryDone(null);
			} catch (final Exception e2) {
				globalLadError = "Failed to query Global LAD: " + e2.getMessage();
				notifyQueryDone(null);
			}
        }
    }
}
