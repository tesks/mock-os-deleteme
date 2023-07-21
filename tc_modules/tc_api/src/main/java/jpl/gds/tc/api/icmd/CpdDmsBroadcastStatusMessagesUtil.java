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

package jpl.gds.tc.api.icmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gov.nasa.jpl.icmd.schema.UplinkRequest;
import jpl.gds.common.config.mission.StationMapper;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.icmd.datastructures.CpdIncrementalRequestStatus;

/**
 * A utility class that implements useful (i.e. code-saving) operations that can
 * be performed on @CpdDmsBroadcastStatusMessages.
 *
 * @since AMPCS 7.1.0
 */
public class CpdDmsBroadcastStatusMessagesUtil {

	/** Logger */
    private static final Tracer logger = TraceManager.getTracer(Loggers.CPD_UPLINK);


	private CpdDmsBroadcastStatusMessagesUtil() {
		// Empty
	}

	/*
	 * MPCS-7354 - Josh Choi - 6/8/2015: Modified the method to allow filtering
	 * out of those incremental updates that should be ignored, based on whether
	 * or not the update is for a request in the Execution List (or Radiation
	 * List).
	 */
	/**
	 * Takes in a radiation/request list and applies any incremental request
	 * status contained in <code>incStatuses</code>, which consists of
	 * UPLINK_REQ_ADDED, UPLINK_REQ_UPDATED, and UPLINK_REQ_DELETED types. This
	 * method modifies the original list, hence the argument name is named such
	 * way: <code>listToModify</code>. If
	 * <code>filterOutIncUpdatesNotInExeList</code> is set to true, only those
	 * incremental updates that have the INCLUDED_IN_EXE_LIST flag set to TRUE
	 * will be applied and others are ignored. If
	 * <code>filterOutIncUpdatesNotInExeList</code> is set to false, all
	 * incremental statuses in the list will be applied.
	 *
	 * Expected behavior:
	 *
	 * If <code>listToModify</code> or <code>incStatuses</code> is null, an
	 * empty list is returned.
	 *
	 * If <code>incStatuses</code> is not null, any incremental request status
	 * included will be reflected on the returned list.
	 * Note that the <code>filterOutIncUpdatesNotInExeList</code> will affect
	 * the behavior of which incremental update will be applied.
	 *
	 * Note that this method right now is not thread-safe, with regard to
	 * <code>listToModify</code> or <code>incStatuses</code>.
	 *
	 * @param listToModify
	 *            the original list and also the list that will contain the
	 *            updated changes
	 * @param incStatuses
	 *            list of incremental status updates to apply on
	 *            <code>listToModify</code>
	 * @param filterOutIncUpdatesNotInExeList
	 *            if true, only those incremental status updates with
	 *            INCLUDED_IN_EXE_LIST value of TRUE will be applied to
	 *            <code>listToModify</code>; if false, all updates are applied
	 * @return list of CPD request status changes, to be fed back into a GUI update loop
	 */
	public static List<CpdStatusChange> applyIncrementalStatusUpdates(
			final StationMapper stationMap,
			final ICpdObjectFactory statusFact,
			final List<ICpdUplinkStatus> listToModify,
			final List<CpdIncrementalRequestStatus> incStatuses,
			final boolean filterOutIncUpdatesNotInExeList) {

		if (listToModify == null || incStatuses == null) {
			return Collections.emptyList();
		}

		final List<CpdStatusChange> statusChanges = new ArrayList<>();

		for (final CpdIncrementalRequestStatus inc : incStatuses) {
			List<UplinkRequest> req = null;

			switch (inc.getType()) {
			case ADDED:
				// In this case, we simply add the new request info to the end
				// of the list we have.

				req = inc.getAsUplinkReqAdded().getREQUESTLIST()
						.getREQUESTINFO();

				for (final UplinkRequest ur : req) {
					final ICpdUplinkStatus newStatus = statusFact.createCpdUplinkStatus(stationMap, ur);
					statusChanges.add(CpdStatusChange.newAddedStatus(newStatus));

					if (!filterOutIncUpdatesNotInExeList || "TRUE".equalsIgnoreCase(newStatus.getIncludedInExeList())) {
						listToModify.add(newStatus);
						logger.debug("UPLINK_REQ_ADDED: Added: "
								+ ur.getREQUESTID());
					} else {
						logger.debug("UPLINK_REQ_ADDED: Not added because not in execution list: "
								+ ur.getREQUESTID());
					}

				}

				break;

			case UPDATED:
				// In this case, we need to find the existing request info in
				// our list, and then replace it.

				req = inc.getAsUplinkReqUpdated().getREQUESTLIST()
						.getREQUESTINFO();

				for (final UplinkRequest ur : req) {
					// This index variable will be set to the actual index of
					// the matching request in our list, if found.
					int ix = -1;

					for (final ICpdUplinkStatus cus : listToModify) {

						if (ur.getREQUESTID().equals(cus.getId())) {
							ix = listToModify.indexOf(cus);

							// Found the matching request, so exit this search
							// loop.
							break;
						}

					}

					// Replace the matching item in our request list with the
					// new, updated request info.
					if (ix >= 0) {
						logger.debug("UPLINK_REQ_UPDATED: Replaced: "
								+ ur.getREQUESTID());
						final ICpdUplinkStatus oldStatus = listToModify.get(ix);
						final ICpdUplinkStatus newStatus = statusFact.createCpdUplinkStatus(stationMap, ur);
						statusChanges.add(CpdStatusChange.newUpdatedStatus(newStatus, oldStatus));

						listToModify.set(ix, newStatus);
					} else {
						final ICpdUplinkStatus newStatus = statusFact.createCpdUplinkStatus(stationMap, ur);

						if (!filterOutIncUpdatesNotInExeList || "TRUE".equalsIgnoreCase(newStatus.getIncludedInExeList())) {
							listToModify.add(newStatus);
							statusChanges.add(CpdStatusChange.newAddedStatus(newStatus));
							logger.warn("Received UPLINK_REQ_UPDATED from CPD but no existing request info in provided list. So added to the list: "
									+ ur.getREQUESTID());
						} else {
							logger.warn("Received UPLINK_REQ_UPDATED from CPD but no existing request info in provided list. Not adding to the list either because not in execution list: "
									+ ur.getREQUESTID());
						}

					}

				}

				break;

			case DELETED:
				// In this case, we need to find the existing request info in
				// our list, and then remove it.

				final List<String> idsToDelete = inc.getAsUplinkReqDeleted()
						.getREQUESTIDLIST().getREQUESTID();

				for (final String id : idsToDelete) {
					// This index variable will be set to the actual index of
					// the matching request in our list, if found.
					int ix = -1;

					for (final ICpdUplinkStatus cus : listToModify) {

						if (id.equals(cus.getId())) {
							ix = listToModify.indexOf(cus);

							// Found the matching request, so exit this search
							// loop.
							break;
						}

					}

					// Delete the matching item from our request list.
					if (ix >= 0) {
						logger.debug("UPLINK_REQ_DELETED: Deleted: " + id);
						statusChanges.add(CpdStatusChange.newRemovedStatus(listToModify.get(ix)));
						listToModify.remove(ix);
					} else {
						logger.warn("Received UPLINK_REQ_DELETED from CPD but no existing request info in provided list");
					}

				}

				break;

			default:
				throw new IllegalStateException("StatusType " + inc.getType()
						+ " is not handled!");

			}

		}

		return statusChanges;
	}

}