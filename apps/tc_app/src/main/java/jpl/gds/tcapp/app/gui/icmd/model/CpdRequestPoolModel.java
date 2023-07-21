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

import gov.nasa.jpl.icmd.schema.UplinkRequest;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.icmd.CpdDmsBroadcastStatusMessagesUtil;
import jpl.gds.tc.api.icmd.CpdStatusChange;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesPoller;
import jpl.gds.tc.api.icmd.ICpdObjectFactory;
import jpl.gds.tc.api.icmd.datastructures.CpdDmsBroadcastStatusMessages;
import jpl.gds.tc.api.message.ICommandMessageFactory;
import org.eclipse.jface.viewers.TableViewer;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class holds the state of a CPD request pool
 *
 * @since AMPCS R3
 */
public class CpdRequestPoolModel extends AbstractCpdRequestTableModel {
    /** The message context to use to publish messages to */
    private final IMessagePublicationBus context;

    /** The items obtained in the previous poll */
    private Map<String, ICpdUplinkStatus> previousPoll;

    /** The items that changed since the previous poll */
    private List<ICpdUplinkStatus> deltas;

    /*
	 * logStale flag is no longer used to
	 * determine if CPD data staleness should be logged or not. Instead, we use
	 * a separate logger
	 * (jpl.gds.tcapp.icmd.config.IntegratedCommandConfiguration
	 * .STALE_DATA_LOGGER_NAME) to turn on and off these warnings.
	 *
     * Default constructor
     *
     */
    public CpdRequestPoolModel(final ApplicationContext appContext) {
    	super(appContext);
    	
    	this.context = appContext.getBean(IMessagePublicationBus.class);
    	
        if (this.previousPoll == null) {
            this.previousPoll = new LinkedHashMap<String, ICpdUplinkStatus>();
        }

        if (this.deltas == null) {
            this.deltas = new LinkedList<ICpdUplinkStatus>();
        }

		// Notify the subscription model that this model is now subscribed
		// (subscription done by the superclass constructor)
		CpdDmsBroadcastStatusMessagesSubscriptionModel.INSTANCE
				.subscribed(appContext.getBean(ICpdDmsBroadcastStatusMessagesPoller.class),
						CpdRequestPoolModel.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see jpl.gds.tcapp.icmd.AbstractCpdRequestTableModel#getName()
     */
    @Override
    protected String getName() {
        return "Request Pool";
    }


    /*
     * (non-Javadoc)
     *
     * @see jpl.gds.tcapp.icmd.AbstractCpdRequestTableModel#refreshViewers()
     */
    @Override
    protected void refreshViewers() {
    	super.refreshViewers();

        context.publish(appContext.getBean(ICommandMessageFactory.class).createInternalCpdUplinkStatusMessage(this.requests,
                this.deltas));
        this.deltas.clear();
    }

    /**
     * Get a request from this data model
     *
     * @param requestId the CPD ID of the request to get
     * @return the CPD request with the specified CPD request ID
     */
    public ICpdUplinkStatus getRequest(final String requestId) {
        if (this.previousPoll.containsKey(requestId)) {
            return this.previousPoll.get(requestId);
        } else {
            return null;
        }
    }

    /**
     * Get the status of a request by request ID
     *
     * @param requestId the CPD request ID of the request
     * @return the CPD status of the request
     */
    public CommandStatusType getStatus(final String requestId) {
        final ICpdUplinkStatus request = this.getRequest(requestId);

        if (request != null) {
            return request.getStatus();
        } else {
            return null;
        }
    }

	/**
	 * {@inheritDoc}
	 *
	 * @see jpl.gds.tcapp.app.gui.icmd.model.AbstractCpdRequestTableModel#handleNewMessages(jpl.gds.tcapp.icmd.datastructures.CpdDmsBroadcastStatusMessages)
	 */
	@Override
	public synchronized void handleNewMessages(
			final CpdDmsBroadcastStatusMessages msgs) {

		final List<CpdStatusChange> statusChanges = new ArrayList<>();
		/*
		 * Changed the logic here to fit the
		 * new CPD's long-poll response data structure (which separates list and
		 * incremental updates, and we need to handle both).
		 * 
		 */
		if (msgs.getRadiationRequests() != null
				|| msgs.getIncrementalRequestStatusList() != null) {

			if (msgs.getRadiationRequests() != null) {

				for (final UplinkRequest ur : msgs.getRadiationRequests()) {
					final ICpdUplinkStatus status = appContext.getBean(ICpdObjectFactory.class).createCpdUplinkStatus(appContext.getBean(MissionProperties.class).getStationMapper(), ur);
					this.requests.add(status);
					statusChanges.add(CpdStatusChange.newAddedStatus(status));
				}

			}

			/*
			 * Merge any incremental status updates
			 *  Call the method with "in execution list?" filtering turned off
			 */
			statusChanges.addAll(CpdDmsBroadcastStatusMessagesUtil
					.applyIncrementalStatusUpdates(this.appContext.getBean(MissionProperties.class).getStationMapper(),
					        appContext.getBean(ICpdObjectFactory.class),
					        this.requests,
							msgs.getIncrementalRequestStatusList(), false));

			final Map<String, ICpdUplinkStatus> currentPoll = new LinkedHashMap<String, ICpdUplinkStatus>();

			if (this.previousPoll == null) {
				this.previousPoll = new LinkedHashMap<String, ICpdUplinkStatus>();
			}

			for (final ICpdUplinkStatus status : this.requests) {
				currentPoll.put(status.getId(), status);
			}

			if (this.deltas == null) {
				this.deltas = new LinkedList<ICpdUplinkStatus>();
			}

			for (final ICpdUplinkStatus status : this.requests) {
				if (this.previousPoll.containsKey(status.getId())) {
					if (!this.previousPoll.get(status.getId()).equals(status)) {
						this.deltas.add(status);
					}
				} else {
					this.deltas.add(status);
				}
			}

			this.previousPoll = currentPoll;

			if (!statusChanges.isEmpty()) {
				refreshTableViewer(statusChanges, true);
			}

		}

		this.stale = false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
