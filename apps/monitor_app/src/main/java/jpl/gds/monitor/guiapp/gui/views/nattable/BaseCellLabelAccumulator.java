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
package jpl.gds.monitor.guiapp.gui.views.nattable;

import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.IConfigLabelAccumulator;

import ca.odell.glazedlists.SortedList;
import jpl.gds.monitor.guiapp.common.gui.INatListItem;

/**
 * Basic cell label accumulator for the AbstractNatTableViewCompoosite. This
 * class dynamically applies configuration labels to table rows based upon the
 * data in the row. This base implementation adds the configuration label for
 * marked rows. It should be extended by subclasses to add labels unique to
 * their specific views. The labels, in turn, control how the cells in the row
 * are rendered.
 *
 * @param <T> type that extends INatListItem
 *
 */
public class BaseCellLabelAccumulator<T extends INatListItem> implements IConfigLabelAccumulator {
    
    /** Label attached to "marked" columns. */
    public static final String MARK_LABEL = "MARKED";

    /** layer this accumulator applies to */
    protected ILayer layer;
    /** rows to be sorted */
    protected SortedList<T> sortedList;

    /**
     * Constructor.
     * 
     * @param bodyLayer
     *            the body layer to be used to get row indices
     * @param sortedEventList
     *            the sorted list containing rows to be rendered.
     */
    public BaseCellLabelAccumulator(final ILayer bodyLayer,
            final SortedList<T> sortedEventList) {
        this.layer = bodyLayer;
        sortedList = sortedEventList;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.nebula.widgets.nattable.layer.cell.IConfigLabelAccumulator#accumulateConfigLabels(org.eclipse.nebula.widgets.nattable.layer.LabelStack,
     *      int, int)
     */
    @Override
    public void accumulateConfigLabels(LabelStack configLabels,
            int columnPosition, int rowPosition) {
        
        /* Add the config label for marked records. */
        int rowIndex = layer.getRowIndexByPosition(rowPosition);
        INatListItem item = sortedList.get(rowIndex);

        if (item.isMarked()) {
            configLabels.addLabel(MARK_LABEL);
        } else {
            configLabels.removeLabel(MARK_LABEL);
        }
        
    }

}