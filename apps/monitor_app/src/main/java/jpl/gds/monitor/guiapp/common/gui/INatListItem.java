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
package jpl.gds.monitor.guiapp.common.gui;

import java.util.UUID;

/**
 * This interface is to be implemented by classes that will be stored in an
 * event list used as a data source in the AbstractNatTableViewComposite.
 *
 */
public interface INatListItem {
    /**
     * Unique ID for this object instance. Required by NAT tables to preserve 
     * table selection and cell attributes when the table records are sorted.
     * 
     * @return a UUID object
     */
    public UUID getUUID();

    /**
     * Gets the receipt/creation time (at the monitor) of this object instance.
     * A wall clock time.
     * 
     * @return time, milliseconds since 1/1/1970.
     */
    public long getReceiptTime();

    /**
     * Marks or unmarks this objects. Marked records can be highlighted in NAT
     * table views. This method affects the underlying object, and therefore the
     * mark is seen by all users of the event list that carries it.
     * 
     * @param enable
     *            true to mark this object, false to unmark it
     */
    public void setMark(boolean enable);

    /**
     * Indicates whether this object is currently marked.
     * 
     * @return true if marked, false if not
     */
    public boolean isMarked();

    /**
     * Gets a unique display name for this object; used for dialog titles.
     * 
     * @return display name
     */
    public String getRecordIdString();

    /**
     * Provides text about this object instance to be displayed in the
     * "View Details..." window of the NAT table view.
     * 
     * @return description text; may be empty, but not null
     */
    public String getRecordDetailString();
}
