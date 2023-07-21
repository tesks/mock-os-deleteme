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
package jpl.gds.tcapp.icmd.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import gov.nasa.jpl.icmd.schema.UplinkRequest;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.fetch.ICommandFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.ICommandOrderByType;
import jpl.gds.db.api.sql.order.IOrderByTypeFactory;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.db.api.sql.store.ICommandUpdateStore;
import jpl.gds.db.api.types.IDbCommandUpdater;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.types.Triplet;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.IUplinkMetadata;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesPoller;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesSubscriber;
import jpl.gds.tc.api.icmd.ICpdObjectFactory;
import jpl.gds.tc.api.icmd.config.IntegratedCommandProperties;
import jpl.gds.tc.api.icmd.datastructures.CpdDmsBroadcastStatusMessages;
import jpl.gds.tc.api.icmd.datastructures.CpdIncrementalRequestStatus;
import jpl.gds.tc.api.icmd.datastructures.CpdIncrementalRequestStatus.StatusType;
import jpl.gds.tc.api.message.ICommandMessageFactory;
import jpl.gds.tc.api.message.ICpdUplinkStatusMessage;


/**
 * This class uses the CPD long polling of DMS broadcast status messages get
 * statuses, and then publishes them.
 *
 * Note: AGEOUT and EPOCH timers are disabled by setting them to zero.
 *
 * Operation:
 *
 * There is a map that relates each observed request-id to its information. The
 * information includes the history list, which is the list of accumulated
 * statuses for that request-id, the finalized status, and the master key (or
 * null if the request-id has not been seen from the database.)
 *
 * This class implements the {@link ICpdDmsBroadcastStatusMessagesSubscriber}
 * interface to receive callbacks when new statuses are available. Hence, we
 * rely on the {@link ICpdDmsBroadcastStatusMessagesPoller}.
 *
 * The local publisher thread loops reading from that blocking queue.
 *
 * The received list is examined in processRawStatuses, which weeds out statuses
 * already seen and adds new ones to the end of the history list.
 *
 * The list is examined to see if all the request-ids are known. If any are not
 * known, then queryUnknownRequestIds looks them up from the database. It may
 * not find all of them; the remainder will be looked up on the next pass.
 *
 * The master key of all rows are assigned, and the statuses are incorporated
 * into the history list. They are put at the front of the history. This is
 * primarily to get the initial entry at the top, since that rarely comes out of
 * the poller. Since we do not have reliable timestamps for the statuses, they
 * cannot be ordered properly. Note that poller statuses are always favored over
 * those from the database, because the database does not have all of the
 * information (such as bit-rates, etc.)
 *
 * The finalized status from the database is also kept.
 *
 * Then processNewStatuses is called to publish the new values. It loops over
 * all request-ids, skipping those that have no master key (which have not been
 * seen in the database yet.)
 *
 * For each request-id with a master key, we loop over the statuses looking for
 * those that have not yet been reported. Those that have not are published and
 * then marked as reported. Statuses whose origin is the database are marked to
 * suppress database insertion.
 *
 * When a finalized status is seen for an unfinalized request-id, it is marked
 * as finalized and a database update generated to set it finalized in the
 * database.
 *
 * Finally, ageOutOldRequestIds is called to remove all finalized request-ids
 * that are too old.
 *
 * We are now at the end of the polling loop.
 *
 * Notes:
 *
 * When reading requestIds from the database, we want those from all sessions.
 * But, we want only those within the configured EPOCH time. This is done to
 * eliminate entries that are too old to be relevant.
 *
 * Statuses derived from the database do not have all the fields that can be
 * populated from the polled statuses. The missing ones are set to empty values.
 *
 * When a status comes fome the poller, and is then subsequently also seen from
 * the database, the poller value is kept and the database value discarded as
 * redundant. But if the history entry has not yet been reported it is set to
 * suppress database insertion, because the status is already present there.
 *
 */
public class UplinkStatusPublisher implements ICpdDmsBroadcastStatusMessagesSubscriber
{
    /** Report status enum */
    private static enum ReportStatus {
        /** Element must be inserted and published */
        Unreported,

        /** Element must be published but not inserted */
        UnreportedSuppress,

        /** Element has been reported */
        Reported;
    }

    private static final List<Float>                           EMPTY_BITRATES  = Collections.emptyList();

    private static final List<Float>                           EMPTY_DURATIONS = Collections.emptyList();

    private static final int                                   EMPTY_CLTUS     = -1;

    private static final IAccurateDateTime                     EMPTY_DATE      = null;

    private static final String                                EMPTY           = "";

    private static final String                                REQUEST_ID      = "Request-id ";
    private static final String                                SLASH           = "/";

    /** Name for logging, thread name, etc. */
    private static final String                                NAME            = "UplinkStatusPublisher";

    private static final long                                  ONE_SECOND      = 1000L;

    /** Wake up every so often and check to see if we are stopping */
    private static final long                                  TIMEOUT         = 5L * ONE_SECOND;

    /** Interval to keep finalized or saved entries (in milliseconds) */
    private static long                                  AGEOUT;

    /**
     * Interval to keep unfinalized entries (in milliseconds).
     * This is used to purge entries that for some reason hang around.
     * It's to prevent memory growth.
     */
    private static long                                  EXTREME_AGEOUT;

    /** Ignore database entries older than this (in milliseconds) */
    private static long                                  EPOCH;

    /** Message publishing context */
    private final IMessagePublicationBus                       bus;

    /** Database store controller */
    private final IDbSqlArchiveController                      archiveController;
    
    /** Database fetch instance factory
     *  Use fetch factory rather than archive controller to get fetch instances.
     */
    private final IDbSqlFetchFactory                     fetchFactory;

    /** Uplink status lists from poller */
    private final BlockingQueue<CpdDmsBroadcastStatusMessages> _dmsStatusMsgs  = new LinkedBlockingQueue<CpdDmsBroadcastStatusMessages>();

    /** Runnable to do real work */
    private final UplinkStatusPublisherRun                     _runner;

    /** Thread with runnable */
    private final Thread                                       _thread;

    /** Logger */
    private final Tracer                                       _log;

    /** Local session configuration */
    private final IContextConfiguration                         sessionConfig;

    /**
     * Configured spacecraft.
     *
     * Note: probably not the best solution.
     * Should probably come from Session.spacecraftId when reading from DB.
     */
    private final int                                          scid;

    private final ApplicationContext                           appContext;

    private final IDbSessionInfoFactory                        dbSessionInfoFactory;

    /** Session info for fetching, note all sessions */
    private final IDbSessionInfoProvider                       _dsi;

    private final IOrderByTypeFactory                          orderByTypeFactory;

    static
    {
    }


    /**
     * Constructor.
     * 
     * @param appContext
     *            the Spring Application Context
     *
     * @param log
     *            Logger
     */
    public UplinkStatusPublisher(final ApplicationContext appContext, final Tracer log)
    {
        super();
        
        this.appContext = appContext;
        
        final IntegratedCommandProperties config = appContext.getBean(IntegratedCommandProperties.class);

        // Convert timers to milliseconds
        AGEOUT = config.getPublisherAgeout() * ONE_SECOND;
        EPOCH  = config.getPublisherEpoch()  * ONE_SECOND;

        // If there is an extreme ageout, make sure it is not less than ageout.
        final long temp = config.getPublisherExtremeAgeout() * ONE_SECOND;
        EXTREME_AGEOUT = (((temp > 0L) && (temp < AGEOUT)) ? AGEOUT : temp);
        
        bus = appContext.getBean(IMessagePublicationBus.class);
        archiveController = appContext.getBean(IDbSqlArchiveController.class);
        fetchFactory = appContext.getBean(IDbSqlFetchFactory.class);
        dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);
        _dsi = dbSessionInfoFactory.createQueryableProvider();
        sessionConfig = appContext.getBean(IContextConfiguration.class);
        scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
        orderByTypeFactory = appContext.getBean(IOrderByTypeFactory.class);

        _log    = log;
        _runner = new UplinkStatusPublisherRun();
        _thread = new Thread(_runner, NAME);

        _thread.setDaemon(true);
    }


    /**
     * See if we have a database connection. This is used to see if
     * we are connecting to the database at all.
     *
     * @return True if connected
     */
    public boolean isConnected()
    {
        return ((_runner != null) && _runner.isConnected());
    }

    /**
     * Begin operation. Start our thread.
     */
    public void start()
    {

		/*
		 * Removed starting of uplink status
		 * poller here because the new CpdDmsBroadcastStatusMessagesPoller will
		 * start automatically.
		 */

		_thread.start();

		/*
		 * Now that the thread handling the
		 * incoming CPD messages is started, add the message handler here.
		 */
		appContext.getBean(ICpdDmsBroadcastStatusMessagesPoller.class).subscribe(this);
		// Start the poller now, since I'm the only subscriber and I just
		// subscribed to the poller.
		appContext.getBean(ICpdDmsBroadcastStatusMessagesPoller.class).start();
	}

    /**
     * Cease operation by stopping status poller and then asking our thread to
     * exit. Thread is daemon, just in case.
     */
    public void stop()
    {

		/*
		 * Removed stopping of uplink status
		 * poller here because the new CpdDmsBroadcastStatusMessagesPoller will
		 * start automatically.
		 */

        _runner.stopRunning();
    }

    /**
     * Runs continuously, publishing uplink status.
     */
    private class UplinkStatusPublisherRun extends Object implements Runnable
    {
        /** Database fetch object */
        private ICommandFetch _fetch = null;

        /** Database update object */
        private ICommandUpdateStore _update = null;

        /** Set true if we are to stop */
        private final AtomicBoolean _stopped = new AtomicBoolean(false);

        /** Request-ids => status data */
        private final Map<String, RequestItem> _requestIds =
            new HashMap<String, RequestItem>();

        /** Worker time-range */
        private final DatabaseTimeRange _range =
            new DatabaseTimeRange(DatabaseTimeType.EVENT_TIME);


        /**
         * Constructor.
         */
        public UplinkStatusPublisherRun()
        {
            super();
        }


        /**
         * Called to ask thread to stop.
         */
        public void stopRunning()
        {
            _stopped.set(true);
        }


        /**
         * See if we have a database connection. This is used to see if
         * we are connecting to the database at all.
         *
         * @return True if connected
         */
        public boolean isConnected()
        {
            return ((_fetch != null) && _fetch.isConnected());
        }


        /**
         * Run method of thread. We create and free some needed objects, but
         * the main purpose is to catch exceptions thrown by working method.
         *
         * {@inheritDoc}
         */
        @Override
        public void run()
        {

        	if (AGEOUT > 0L)
            {
                _log.debug("Using ageout of " + AGEOUT);
            }
            else
            {
                _log.debug("Not using ageout");
            }

            if (EXTREME_AGEOUT > 0L)
            {
                _log.debug("Using extreme ageout of " + EXTREME_AGEOUT);
            }
            else
            {
                _log.debug("Not using extreme ageout");
            }

            if (EPOCH > 0L)
            {
                _log.debug("Using epoch of " + EPOCH);
            }
            else
            {
                _log.debug("Not using epoch");
            }

            try
            {
                _fetch  = fetchFactory.getCommandFetch(false);
                _update = archiveController.getCommandUpdateStore();

                internalRun();
            }
            catch (final RuntimeException re)
            {
                _log.error("Thread " + NAME + " dies: ", re);

                throw re;
            }
            catch (final Exception e)
            {
                _log.error("Thread " + NAME + " dies: ", e);
            }
            finally
            {
                if (_fetch != null)
                {
                    _fetch.close();

                    _fetch = null;
                }

                if (_update != null)
                {
                    _update.stopUpdateStore();

                    _update.close();

                    _update = null;
                }
                
                if (archiveController != null) {
                    archiveController.shutDown();
                }
            }
        }


        /**
         * Worker run method.
         *
         * @throws DatabaseException If database trouble
         */
        private void internalRun() throws DatabaseException, DictionaryException
        {

            /** Worker set of request-ids */
            final Set<String> worker = new TreeSet<String>();

            while (! _stopped.get())
            {
                CpdDmsBroadcastStatusMessages statusMsgs = null;

                try
                {
                    statusMsgs = _dmsStatusMsgs.poll(TIMEOUT,
                                                       TimeUnit.MILLISECONDS);
                }
                catch (final InterruptedException ie)
                {
                    // We don't care if we wake up early
                    statusMsgs = null;
                }

                if (_stopped.get())
                {
                    break;
                }

                final long now = System.currentTimeMillis();

                // Assign any polled statuses to history
                processRawStatuses(statusMsgs);

                // If there are any unknown request-ids, query them from
                // the database. (May not get all.)

                queryUnknownRequestIds(now, worker);

                // Publish and/or insert new statuses

                processNewStatuses();

                // Delete old request-ids

                ageOutOldRequestIds(now);
            }
        }


        /**
         * Process raw statuses and put them where they belong.
         *
         * @param msgs List of DMS broadcast status messages
         */
        private void processRawStatuses(final CpdDmsBroadcastStatusMessages msgs)
        {
        	if (msgs == null) {
        		_log.log(TraceSeverity.DEBUG, "Null CPD DMS broadcast status messages");
                return;
        	}

        	final List<ICpdUplinkStatus> statuses = msgs.getRadiationList();

			if (statuses == null || statuses.isEmpty()) {
				_log.log(TraceSeverity.TRACE, "DMS broacast status messages includes no radiation list");

			} else {

                _log.trace("Radiation list has " + statuses.size() + " CPD statuses");

				for (final ICpdUplinkStatus cus : statuses) {
					/*
					 * Refactored by moving
					 * the code here to the new method processSingleStatus. This
					 * method is called here and also below, when handling the
					 * incremental updates.
					 */
					processSingleStatus(cus);
				}

			}

			/*
			 * Now that we can also get
			 * incremental updates of the requests, handle it here.
			 */
            _log.trace("CPD incremental update status messages has " , msgs.getIncrementalRequestStatusList().size()
                    , " items");

			for (final CpdIncrementalRequestStatus inc : msgs
					.getIncrementalRequestStatusList()) {
				List<UplinkRequest> req = null;

				switch (inc.getType()) {
				case ADDED:
				case UPDATED:
					// In this case, we have new "added" or "updated"
					// incremental updates.

					if (inc.getType() == StatusType.ADDED) {
						req = inc.getAsUplinkReqAdded().getREQUESTLIST()
								.getREQUESTINFO();
					} else {
						req = inc.getAsUplinkReqUpdated().getREQUESTLIST()
								.getREQUESTINFO();
					}

					_log.log(TraceSeverity.TRACE, "Process UPLINK_REQ_" + inc.getType());

					for (final UplinkRequest ur : req) {
						final ICpdUplinkStatus cus = appContext.getBean(ICpdObjectFactory.class).
						        createCpdUplinkStatus(appContext.getBean(MissionProperties.class).getStationMapper(), ur);
						processSingleStatus(cus);
					}

					break;

				case DELETED:
					/*
					 * Actually, we shouldn't
					 * remove the existing request info from the list, although
					 * the status update is that of "deleted". The removing is
					 * taken care of by jpl.gds
					 * .core.cmd.icmd.UplinkStatusPublisher
					 * .UplinkStatusPublisherRun.ageOutOldRequestIds(long). The
					 * proper thing to do is append to the list with the new
					 * status update.
					 */
					final List<String> idsToDelete = inc.getAsUplinkReqDeleted()
							.getREQUESTIDLIST().getREQUESTID();

					_log.log(TraceSeverity.TRACE, "Process UPLINK_REQ_DELETED");

					for (final String id : idsToDelete) {
						/*
						 * Unfortunately, with
						 * the CPD long polling interface, the deleted
						 * information is only provided to us by the request
						 * ID--we don't get an UplinkRequest info. This means
						 * that we have to create a faux UplinkRequest object so
						 * that we can maintain harmony in the way our request
						 * history is being kept.
						 */

						_log.log(TraceSeverity.DEBUG, "Starting UPLINK_REQ_DELETED processing of request ID " + id);

						/*
						 * First see if it's a request we have a history of.
						 */
						final RequestItem item = _requestIds.get(id);

						if (item != null) {
							_log.log(TraceSeverity.DEBUG, id + " found in _requestIds");

							// Get the last item from history
							final StatusItem si = item.getHistory().get(item.getHistory().size() - 1);

							if (si != null) {
								_log.log(TraceSeverity.DEBUG, "Found a StatusItem in " + id + "'s history");
								final ICpdUplinkStatus newCus = appContext.getBean(ICpdObjectFactory.class).
								        createCpdUplinkStatus(appContext.getBean(MissionProperties.class).getStationMapper(), si.getCpdStatus());

								if (inc.getTimestamp() == null) {
									_log.log(TraceSeverity.ERROR, "UPLINK_REQ_DELETED " + id + " timestamp is null!");
								}

								newCus.setTimestamp(inc.getTimestamp());
								processSingleStatus(newCus);

							} else {
								_log.log(TraceSeverity.DEBUG, "Did not find a StatusItem in "
										+ id
										+ "'s history. Nothing more to do with this request.");

								/*
								 * Do nothing
								 */
							}

						} else {
							_log.log(TraceSeverity.DEBUG, id + " not found in _requestIds. Nothing more to do with this request.");

							/*
							 * Do nothing
							 */
						}

						/*
						 * Got rid of the
						 * _requestIds.remove(id) here, because the removing is
						 * done by the age-out method.
						 */
                        _log.log(TraceSeverity.DEBUG, "Finishing UPLINK_REQ_DELETED processing of request ID " + id);
					}

					break;

				default:
					throw new IllegalStateException("StatusType " + inc.getType()
							+ " is not handled!");

				}

			}

		}

        /*
		 * This is a new method. The logic
		 * within was extracted verbatim from processRawStatuses method. The
		 * reason for the extraction is that, previously, only CpdUplinkStatus
		 * seen in the radiation list from CPD was handled with this logic. But
		 * now we also get UPLINK_REQ_ADDED and UPLINK_REQ_UPDATED incremental
		 * update status messages, which require the same handling. So we reuse
		 * the code for all these cases via this new method.
		 *
		 * Now UPLINK_REQ_DELETED also gets
		 * handled via this method, so removed the "new" and "updated" words
		 * from the method name.
		 */
        /**
		 * Process a CpdUplinkStatus object encountered in either the radiation
		 * list or the incremental added/updated message.
		 *
		 * @param cus CpdUplinkStatus object to process
		 */
        private void processSingleStatus(final ICpdUplinkStatus cus) {

			final String requestId = cus.getId();
			final CommandStatusType cst = cus.getStatus();
			final IUplinkMetadata metadata = cus.getUplinkMetadata();

			if (metadata == null) {
                _log.debug(REQUEST_ID + requestId + SLASH + cst + " is foreign, ignored");
				return;
			}

			final long sessionId = metadata.getSessionId();

			if (sessionId <= 0L) {
                _log.debug(REQUEST_ID + requestId + SLASH + cst + " is foreign, ignored");
				return;
			}

			final RequestItem item = _requestIds.get(requestId);

			if (item == null) {
				// Unknown request-id

				final RequestItem newItem = new RequestItem();

				newItem.addHistory(cus);

				_requestIds.put(requestId, newItem);

				final String topic = newItem.getTopic();

				if (topic != null) {
                    _log.debug(REQUEST_ID + requestId + SLASH + cst +
							" is new unknown on topic '" + topic +
							"' and master key " + metadata.getHostId() +
							SLASH + sessionId + " for S/C " +
                            metadata.getScid());
				} else {
                    _log.debug(REQUEST_ID + requestId + SLASH + cst +
							" is new unknown on unknown topic" +
							" and master key " + metadata.getHostId() +
							SLASH + sessionId + " for S/C " +
                            metadata.getScid());
				}

				return;
			}

			final StatusItem si = item.hasHistory(cst);

			if (item.getMasterKey() == null) {
				// Request-id seen before, but not from database

				if (si == null) {
					// Not found, so add
					item.addHistory(cus);

                    _log.debug(REQUEST_ID + requestId + SLASH + cst + " is added unknown");
				} else {
                    _log.trace(REQUEST_ID + requestId + SLASH + cst + " is redundant unknown");
				}

				return;
			}

			// Fully known

			if (si == null) {
				// Not found, so add
				item.addHistory(cus);

                _log.debug(REQUEST_ID + requestId + SLASH + cst + " is added");
			} else if (si.getFromDatabase()
					&& (si.getReportStatus() != ReportStatus.Reported)) {
				// Found, but from database and not yet reported;
				// replace CPD status but do not change report status.
				// Will be marked as polled but will still not be
				// inserted.
				// May not ever happen.

				si.setCpdStatus(cus);

                _log.debug(REQUEST_ID + requestId + SLASH + cst + " is updated");
			} else {
                _log.debug(REQUEST_ID + requestId + SLASH + cst + " is redundant");
			}

        }

        /**
         * Get entries for unknown request-ids from database.
         *
         * @param now    Current time in milliseconds
         * @param worker Worker set
         *
         * @throws DatabaseException If trouble with database
         */
        @SuppressWarnings("unchecked")
        private void queryUnknownRequestIds(final long        now,
                                            final Set<String> worker)
            throws DatabaseException, DictionaryException
        {
            worker.clear();

            if (_requestIds.isEmpty())
            {
                return;
            }

            // Put in worker all unknown request-ids

            for (final Map.Entry<String, RequestItem> entry :
                     _requestIds.entrySet())
            {
                if (entry.getValue().getMasterKey() == null)
                {
                    worker.add(entry.getKey());
                }
            }

            if (worker.isEmpty())
            {
                return;
            }

            // Fetch particulars of request-ids from database
            // Ignore entries that are too old

            if (EPOCH > 0L)
            {
            	// Updated due to updated DatabaseTimeRange
                _range.setStartTime(new AccurateDateTime(now - EPOCH));
            }

            List<IDbCommandUpdater> results =
                (List<IDbCommandUpdater>) _fetch.get(_dsi,
			           (EPOCH > 0L) ? _range : null,
			           1000,    // Batch size
			           null,    // Command string
			           null,    // Command type
			           orderByTypeFactory.getOrderByType(OrderByType.COMMAND_ORDER_BY, ICommandOrderByType.REQUEST_ID_TYPE),
			           null,    // Finalized
			           null,    // Status type
			           worker,  // Request ids
			           false);  // Last status only

            _log.trace("Query: " + _fetch.getLastQuery());

            while ((results != null) && ! results.isEmpty())
            {
                processResults(results);

                results = (List<IDbCommandUpdater>) _fetch.getNextResultBatch();
            }

            worker.clear();
        }


        /**
         * Process rows returned from database.
         *
         * @param results List of database rows
         */
        private void processResults(final List<IDbCommandUpdater> results)
        {
            for (final IDbCommandUpdater dc : results)
            {
                final String            requestId = dc.getRequestId();
                final CommandStatusType status    = dc.getStatus();
                final RequestItem       item      = _requestIds.get(requestId);
                final boolean           finalized = dc.getFinalized();

                // Associate the master key and check for trouble
                setMasterKey(item, dc);

                if (finalized && ! item.getFinalized())
                {
                    item.setFinalized();

                    _log.debug(REQUEST_ID + requestId + " is read from database as finalized");
                }

                final StatusItem si = item.hasHistory(status);

                if (si != null)
                {
                    // Duplicate, but we want to make sure it does not get
                    // inserted, since the database has it already

                    si.setSuppress();

                    if (finalized)
                    {
                        si.setReported();
                    }

                    _log.debug(REQUEST_ID + requestId + SLASH + status + " is redundant from database"
                            + (finalized ? " and will not be reported" : ""));
                    continue;
                }

                // Fill in as best we can. We can get our user and role, but
                // that's not necessarily the user who generated the command.

                final ICpdUplinkStatus cus = appContext.getBean(ICpdObjectFactory.class).
                    createCpdUplinkStatus(appContext.getBean(MissionProperties.class).getStationMapper(),
                    		            requestId,
                                        status,
                                        dc.getEventTime(),
                                        StringUtil.safeTrim(
                                            dc.getOriginalFile()),
                                        EMPTY_BITRATES,
                                        EMPTY_DURATIONS,
                                        EMPTY,
                                        EMPTY,
                                        EMPTY,
                                        EMPTY,
                                        EMPTY,
                                        EMPTY,
                                        EMPTY_CLTUS,
                                        EMPTY_DATE,
                                        EMPTY_DATE);

                cus.setUplinkMetadata(appContext.getBean(ICpdObjectFactory.class).createUplinkMetadata(dc.getSessionId(),
                                                         dc.getSessionHostId(),
                                                         item.getTopic(),
                                                         scid));

                item.addHistoryFromDatabase(cus, finalized);

                _log.debug(REQUEST_ID + requestId + SLASH + status + SLASH + scid + " is added from database"
                        + (finalized ? " but will not be reported" : ""));
            }
        }


        /**
         * Make sure that the master key is set.
         *
         * @param ri Request item
         * @param dc Database row
         */
        private void setMasterKey(final RequestItem     ri,
                                  final IDbCommandUpdater dc)
        {
            final MasterKey             masterKey       = ri.getMasterKey();
            final long                  sessionId       = dc.getSessionId();
            final SessionFragmentHolder sessionFragment =
                                            dc.getSessionFragment();
            final int                   hostId          =
                                            dc.getSessionHostId();

            if (masterKey == null)
            {
                // Now have info from database, update entry

                ri.setMasterKey(new MasterKey(sessionId,
                                              sessionFragment,
                                              hostId));
                return;
            }

            final long                  oldSessionId =
                                            masterKey.getSessionId();
            final SessionFragmentHolder oldFragment  =
                                            masterKey.getSessionFragment();
            final int                   oldHostId    = masterKey.getHostId();

            if ((sessionId != oldSessionId)           ||
                ! sessionFragment.equals(oldFragment) ||
                (hostId    != oldHostId))
            {
                _log.warn(REQUEST_ID, dc.getRequestId(), " has multiple sessions: ", "(", sessionId, ",",
                        sessionFragment, ",", hostId, ") and (", oldSessionId, ",", oldFragment, ",", oldHostId, ")");
            }
        }


        /**
         * Insert and/or publish any new statuses.
         */
        private void processNewStatuses()
        {
            if (_requestIds.isEmpty())
            {
                return;
            }

            for (final Map.Entry<String, RequestItem> entry :
                     _requestIds.entrySet())
            {
                final String      requestId = entry.getKey();
                final RequestItem item      = entry.getValue();
                final MasterKey   masterKey = item.getMasterKey();

                if ((masterKey == null) || ! item.hasTopic())
                {
                    // Not ready
                    continue;
                }

                for (final StatusItem si : item.getHistory())
                {
                    final ReportStatus rs = si.getReportStatus();

                    if (rs == ReportStatus.Reported)
                    {
                        continue;
                    }

                    // Publish, and insert into database if necessary

                    final ICpdUplinkStatus cus = si.getCpdStatus();

                    makeSureTopicIsSet(cus, item.getTopic());

                    publish(cus,
                            masterKey,
                            (rs == ReportStatus.UnreportedSuppress));

                    si.setReported();

                    final CommandStatusType cst = cus.getStatus();

                    if (! item.getFinalized() && cst.isFinal())
                    {
                        // Transition to finalized

                        if (masterKey != null)
                        {
                            try
                            {
                                _update.finalize(
                                    masterKey.getHostId(),
                                    masterKey.getSessionId(),
                                    masterKey.getSessionFragment().getValue(),
                                    requestId);
                            }
                            catch (final DatabaseException sqle)
                            {
                                // We don't bail on this, since it is not
                                // critical

                                _log.error("Unable to finalize " + "CommandMessage: " + sqle);
                            }
                        }

                        item.setFinalized();

                        _log.debug(REQUEST_ID + requestId + SLASH + cst + " is accepted as final");
                    }
                }
            }
        }


        /**
         * Publish status; may or may not insert to database.
         *
         * @param cus       CPD status
         * @param masterKey Master key of CPD status
         * @param suppress  True, suppress insert to database
         */
        private void publish(final ICpdUplinkStatus cus,
                             final MasterKey       masterKey,
                             final boolean         suppress)
        {
            // Publish new message using proper session and host ids

            _log.debug("Publishing " + (suppress ? "without inserting " : EMPTY) +
                    "request-id " + cus.getId() + SLASH + cus.getStatus());

            final ICpdUplinkStatusMessage cusm = appContext.getBean(ICommandMessageFactory.class).createCpdUplinkStatusMessage(cus);

            final IContextConfiguration sc = new SessionConfiguration(appContext.getBean(MissionProperties.class),
                    appContext.getBean(ConnectionProperties.class), false);

            if (sessionConfig != null)
            {
                sc.copyValuesFrom(sessionConfig);
            }

            sc.getContextId().setNumber(masterKey.getSessionId());
            sc.getContextId().setFragment(masterKey.getSessionFragment().getValue());
            sc.getContextId().setHostId(masterKey.getHostId());

            cusm.setContextHeader(sc.getContextId().getMetadataHeader());

            if (suppress)
            {
                // Do not insert into database
                cusm.setDoNotInsert();
            }

            bus.publish(cusm);
        }


        /**
         * Get rid of too-old request-ids.
         *
         * @param now Current time
         */
        private void ageOutOldRequestIds(final long now)
        {
            if (_requestIds.isEmpty())
            {
                return;
            }

            for (final Iterator<Map.Entry<String, RequestItem>> it =
                     _requestIds.entrySet().iterator(); it.hasNext();)
            {
                final Map.Entry<String, RequestItem> entry     = it.next();
                final String                         requestId =
                    entry.getKey();
                final RequestItem                    item      =
                    entry.getValue();

                if (item.getDropDead() <= now)
                {
                    if (item.getFinalized())
                    {
                        it.remove();

                        _log.debug("Finalized request-id " + requestId + " is purged");
                    }

                    if (item.getMasterKey() == null)
                    {
                        it.remove();

                        _log.debug("Foreign request-id " + requestId + " is purged");
                    }
                }
                else if (item.getDropReallyDead() <= now)
                {
                    it.remove();

                    _log.warn("Hung request-id " + requestId + " is purged");
                }
            }
        }
    }


    /**
     * Set topic if not set already.
     *
     * @param cus   Status object
     * @param topic Topic
     */
    private static void makeSureTopicIsSet(final ICpdUplinkStatus cus,
                                           final String          topic)
    {
        final IUplinkMetadata um = cus.getUplinkMetadata();

        if (StringUtil.emptyAsNull(um.getMessageServiceTopicName()) == null)
        {
            um.setMessageServiceTopicName(topic);
        }
    }


    /** Holder class for session id, session fragment, and host ids */
    private static class MasterKey extends Triplet<Long,
                                                   SessionFragmentHolder,
                                                   Integer>
    {
        private static final long serialVersionUID = 0L;


        /**
         * Constructor.
         *
         * @param sessionId       Session id
         * @param sessionFragment Session fragment
         * @param hostId          Host id
         */
        public MasterKey(final long                  sessionId,
                         final SessionFragmentHolder sessionFragment,
                         final int                   hostId)
        {
            super(sessionId, sessionFragment, hostId);
        }


        /**
         * Get session id.
         *
         * @return Session id
         */
        public long getSessionId()
        {
            return getOne();
        }


        /**
         * Get session fragment.
         *
         * @return Session fragment
         */
        public SessionFragmentHolder getSessionFragment()
        {
            return getTwo();
        }


        /**
         * Get host id.
         *
         * @return Host id
         */
        public int getHostId()
        {
            return getThree();
        }
    }


    /** Holder class for CPD status and report status */
    private static class StatusItem extends Object
    {
        private ICpdUplinkStatus _cus    = null;
        private ReportStatus    _rs     = null;
        private boolean         _fromDb = false;


        /**
         * Constructor.
         *
         * @param cpdStatus    CPD status
         * @param reportStatus Report status
         * @param fromDb       True if from database
         */
        public StatusItem(final ICpdUplinkStatus cpdStatus,
                          final ReportStatus    reportStatus,
                          final boolean         fromDb)
        {
            super();

            _cus    = cpdStatus;
            _rs     = reportStatus;
            _fromDb = fromDb;
        }


        /**
         * Get CPD status.
         *
         * @return CPD status
         */
        public ICpdUplinkStatus getCpdStatus()
        {
            return _cus;
        }


        /**
         * Get from-database status.
         *
         * @return True if from database
         */
        public boolean getFromDatabase()
        {
            return _fromDb;
        }


        /**
         * Set CPD report status. Used to replace values read from database
         * with polled version.
         *
         * @param cus CPD report status
         */
        public void setCpdStatus(final ICpdUplinkStatus cus)
        {
            _cus    = cus;
            _fromDb = false;
        }


        /**
         * Get report status.
         *
         * @return Report status
         */
        public ReportStatus getReportStatus()
        {
            return _rs;
        }


        /**
         * Set report status to reported.
         */
        public void setReported()
        {
            _rs = ReportStatus.Reported;
        }


        /**
         * Set report status to suppress insertion.
         */
        public void setSuppress()
        {
            if (_rs == ReportStatus.Unreported)
            {
                _rs = ReportStatus.UnreportedSuppress;
            }
        }
    }


    /** Class with all knowledge about a request-id */
    private class RequestItem extends Object
    {
        private final List<StatusItem> _history = new ArrayList<StatusItem>();
        private final long             _dropReallyDead;


        private MasterKey   _masterKey = null;
        private String      _topic     = null;
        private boolean     _finalized = false;
        private CommandUserRole _role      = null;
        private long        _dropDead  = Long.MAX_VALUE;


        /**
         * Constructor.
         */
        public RequestItem()
        {
            super();

            _dropReallyDead = (EXTREME_AGEOUT > 0L)
                                  ? System.currentTimeMillis() + EXTREME_AGEOUT
                                  : Long.MAX_VALUE;
        }


        /**
         * Get history.
         *
         * @return History list
         */
        public List<StatusItem> getHistory()
        {
            return _history;
        }


        /**
         * Get topic.
         *
         * @return Topic
         */
        public String getTopic()
        {
            return _topic;
        }


        /**
         * Get topic state.
         *
         * @return True if have topic
         */
        public boolean hasTopic()
        {
            return (_topic != null);
        }


        /**
         * Get master key.
         *
         * @return Master key
         */
        public MasterKey getMasterKey()
        {
            return _masterKey;
        }


        /**
         * Set master key.
         *
         * @param masterKey Master key
         */
        public void setMasterKey(final MasterKey masterKey)
        {
            if (_masterKey != null)
            {
                _log.log(TraceSeverity.ERROR, "Attempt to set master key twice");

                return;
            }

            _masterKey = masterKey;
        }


        /**
         * Get finalized.
         *
         * @return Finalized state
         */
        public boolean getFinalized()
        {
            return _finalized;
        }


        /**
         * Set finalized if not already. Also update drop-dead time.
         */
        public void setFinalized()
        {
            if (! _finalized)
            {
                _finalized = true;

                if (AGEOUT > 0L)
                {
                    _dropDead = System.currentTimeMillis() + AGEOUT;
                }
            }
        }


        /**
         * Get drop-dead time.
         *
         * @return Drop-dead time
         */
        public long getDropDead()
        {
            return _dropDead;
        }


        /**
         * Get final drop-dead time.
         *
         * @return Drop-dead time
         */
        public long getDropReallyDead()
        {
            return _dropReallyDead;
        }


        /**
         * Get role.
         *
         * @return Role
         */
        @SuppressWarnings("unused")
		public CommandUserRole getRole()
        {
            return _role;
        }


        /**
         * Add a new status from the database to the history list.
         * Database entries are in front of polled entries.
         *
         * If it is already finalized in the database, there is no need to
         * report it. In any case, we do not want to insert again.
         * This check is important if the status is still around after we
         * purge the finalized statuses.
         *
         * @param cpdStatus CPD status
         * @param finalized True if finalized
         */
        public void addHistoryFromDatabase(final ICpdUplinkStatus cpdStatus,
                                           final boolean         finalized)
        {
            final StatusItem newSi =
                new StatusItem(cpdStatus,
                               finalized ? ReportStatus.Reported
                                         : ReportStatus.UnreportedSuppress,
                               true);
            int index = 0;

            for (final StatusItem si : _history)
            {
                if (si.getFromDatabase())
                {
                    ++index;
                }
                else
                {
                    break;
                }
            }

            _history.add(index, newSi);
        }


        /**
         * Add a new polled status to the history list. Place at end.
         *
         * @param cpdStatus CPD status
         */
        public void addHistory(final ICpdUplinkStatus cpdStatus)
        {
            final String role = cpdStatus.getRoleId();

            if ((role != null) && (_role == null))
            {
                _role = CommandUserRole.valueOf(role);
            }

            _history.add(new StatusItem(cpdStatus,
                                        ReportStatus.Unreported,
                                        false));

            // Get topic from metadata as necessary

            final IUplinkMetadata metadata = cpdStatus.getUplinkMetadata();
            final String         jmsTopic =
                StringUtil.emptyAsNull(metadata.getMessageServiceTopicName());

            if (jmsTopic == null)
            {
                return;
            }

            if (_topic == null)
            {
                _topic = jmsTopic;
            }
            else if (! jmsTopic.equalsIgnoreCase(_topic))
            {
                _log.warn(REQUEST_ID + " attempt to change topic from '" + _topic + "' to '" + jmsTopic + "'");
            }
        }


        /**
         * See if a status is already present in history.
         *
         * @param cst Status to check for
         *
         * @return Status item if found, else null
         */
        public StatusItem hasHistory(final CommandStatusType cst)
        {
            for (final StatusItem si : _history)
            {
                if (si.getCpdStatus().getStatus() == cst)
                {
                    return si;
                }
            }

            return null;
        }
    }


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleNewMessages(final CpdDmsBroadcastStatusMessages msgs) {
		_dmsStatusMsgs.add(msgs);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dataNowStale() {
		// Don't do anything about stale polls
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
