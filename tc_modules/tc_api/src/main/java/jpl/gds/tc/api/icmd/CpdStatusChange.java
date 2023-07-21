/*
 * Copyright 2006-2019. California Institute of Technology.
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

import jpl.gds.tc.api.ICpdUplinkStatus;
import org.eclipse.jface.viewers.TableViewer;

/**
 * Class to encapsulate a CPD Uplink Request's change in status. Supports ADDED, UPDATED, and REMOVED.
 *
 * This class is used to help the GUI reflect changes in CPD status through the CPD request and radiation tables.
 *
 */
public class CpdStatusChange {
    enum ChangeType {
        ADDED,
        UPDATED,
        REMOVED
    }

    private final ChangeType type;
    private ICpdUplinkStatus status;
    private ICpdUplinkStatus oldStatus;

    private CpdStatusChange(final ChangeType type) {
        this.type = type;
    }

    /**
     * Create a new ADDED status change. This will add the provided status to the GUI tables.
     *
     * @param status new status to add
     * @return CPD status change object
     */
    public static CpdStatusChange newAddedStatus(final ICpdUplinkStatus status) {
        final CpdStatusChange change = new CpdStatusChange(ChangeType.ADDED);
        change.status = status;
        return change;
    }

    /**
     * Create a new UPDATED status change. This will remove the old status from the GUI and replace it with the new status.
     *
     * @param newStatus new status to be shown in the GUI
     * @param oldStatus old status to remove from the GUI
     * @return CPD status change object
     */
    public static CpdStatusChange newUpdatedStatus(final ICpdUplinkStatus newStatus, final ICpdUplinkStatus oldStatus) {
        final CpdStatusChange change = new CpdStatusChange(ChangeType.UPDATED);
        change.status = newStatus;
        change.oldStatus = oldStatus;
        return change;
    }

    /**
     * Create a new REMOVED status change. This will remove a status from the GUI.
     *
     * @param removedStatus CPD uplink status to be removed from the GUI
     * @return CPD status change object
     */
    public static CpdStatusChange newRemovedStatus(final ICpdUplinkStatus removedStatus) {
        final CpdStatusChange change = new CpdStatusChange(ChangeType.REMOVED);
        change.status = removedStatus;
        return change;
    }

    /**
     * Perform the encapsulated GUI update on the provided table viewer.
     *
     * @param viewer table viewer
     */
    public void performChange(final TableViewer viewer) {
        switch (type) {
            case ADDED:
                viewer.add(status);
                break;
            case UPDATED:
                viewer.add(status);
                viewer.remove(oldStatus);
                break;
            case REMOVED:
                viewer.remove(status);
                break;
        }
    }
}
