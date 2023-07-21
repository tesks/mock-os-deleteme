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
package jpl.gds.tcapp.app.gui.icmd.model;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.icmd.CpdStatusChange;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesPoller;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesSubscriber;
import jpl.gds.tc.api.icmd.datastructures.CpdDmsBroadcastStatusMessages;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.springframework.context.ApplicationContext;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is an abstract class for a CPD request table data model. It provides
 * common functionality such as handling bounded Viewers.
 *
 * @since AMPCS R3
 */
public abstract class AbstractCpdRequestTableModel implements
		IStructuredContentProvider, ICpdDmsBroadcastStatusMessagesSubscriber {
	/** Logger */
    protected final Tracer logger;      

    protected final Tracer staleDataLogger; 


	/*
	 * ScheduledExectorService is no longer
	 * needed since the model does not poll CPD anymore. Much code in this class
	 * have now been moved to CpdDmsBroadcastStatusMessagesPoller.
	 */

	/** The list of CPD requests in this data model */
	protected List<ICpdUplinkStatus> requests;

	/** The Viewers bound to this model */
	protected List<Viewer> viewers;

	/** Flag indicating whether or not this model is stale */
	protected boolean stale;
	
	protected final ApplicationContext appContext;

	protected final AtomicLong lastUpdated = new AtomicLong();

	protected final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
			new ThreadFactoryBuilder().setNameFormat("cpd-request-table-update-timer-%d").build());

	/**
	 * Constructor
	 */
	public AbstractCpdRequestTableModel(final ApplicationContext appContext) {
		this.appContext = appContext;
        this.logger = TraceManager.getTracer(appContext, Loggers.CPD_UPLINK);
        this.staleDataLogger = TraceManager.getTracer(appContext, Loggers.CPD_UPLINK);
        this.viewers = new LinkedList<>();
        this.requests = new LinkedList<>();

		/*
		 * Handle CPD long poll messages by
		 * registering this object as a subscriber.
		 */
		appContext.getBean(ICpdDmsBroadcastStatusMessagesPoller.class).subscribe(this);

		launchGuiUpdateTimer();
	}

	private void launchGuiUpdateTimer() {
		executorService.scheduleAtFixedRate(() -> {
			final long currentTime = System.currentTimeMillis();
			if (currentTime - lastUpdated.get() > 10000) {
				refreshViewers();
			}
		}, 10, 10, TimeUnit.SECONDS);
	}

	/**
	 * Get the name of this data model
	 *
	 * @return String representing the name of this data model
	 */
	protected abstract String getName();


	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java
	 * .lang.Object)
	 */
	@Override
	public Object[] getElements(final Object input) {
		return this.requests.toArray();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface
	 * .viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		if (!this.viewers.contains(viewer)) {
			this.viewers.add(viewer);
		}
	}

	/**
	 * Refresh bound viewers
	 */
	protected void refreshViewers() {
		lastUpdated.set(System.currentTimeMillis());
		if (Display.getDefault() != null && !Display.getDefault().isDisposed()) {

			SWTUtilities.safeAsyncExec(Display.getDefault(),
					 "CpdRequestTable", new Runnable() {
				@Override
				public void run() {
					for (final Viewer v : AbstractCpdRequestTableModel.this.viewers) {
						if (v.getControl() != null
								&& !v.getControl().isDisposed()) {
							v.refresh();
						}
					}
				}

			});
		}
	}

	void refreshTableViewer(final List<CpdStatusChange> statusChanges, final boolean scrollTop) {
		lastUpdated.set(System.currentTimeMillis());
		this.viewers.stream().filter(viewer -> viewer instanceof TableViewer).map(viewer -> (TableViewer) viewer)
				.forEach(viewer -> SWTUtilities.safeAsyncExec(Display.getDefault(), "CpdRequestTable", () -> {
					if (viewer.getControl() != null && !viewer.getControl().isDisposed()) {
						statusChanges.forEach(cpdStatusChange -> cpdStatusChange.performChange(viewer));
						if (scrollTop) {
							viewer.getTable().setTopIndex(0);
						} else {
							viewer.getTable().setTopIndex(viewer.getTable().getItemCount() - 1);
						}
					}
				}));
	}

	/**
	 * Indicates if the data in this model is stale. Staleness occurs after a
	 * consecutive number of failed polls. This number is configurable.
	 *
	 * @return true if data is stale, false otherwise
	 */
	public boolean isStale() {
		return this.stale;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		/*
		 * Nothing to do now since
		 * ScheduledExecutorService is no longer used, after introduction of CPD
		 * long polling.
		 *
		 * We should unsubscribe from the
		 * poller here. Found during code review.
		 */
		appContext.getBean(ICpdDmsBroadcastStatusMessagesPoller.class).unsubscribe(this);

		executorService.shutdownNow();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesSubscriber#dataNowStale()
	 */
	@Override
    public synchronized void dataNowStale() {
		this.stale = true;

		// In old code, it seemed that when data turns stale he did a
		// refreshViewers() call. Is it needed? I don't think so, so not adding
		// the call here.
	}

	/*
	 * Converted this method into an abstract
	 * one because both of its subclasses handle them quite differently.
	 */
	/**
	 * {@inheritDoc}
	 *
	 * @see jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesSubscriber#handleNewMessages(jpl.gds.tc.api.icmd.datastructures.CpdDmsBroadcastStatusMessages)
	 */
	@Override
    public abstract void handleNewMessages(
			final CpdDmsBroadcastStatusMessages msgs);

}
