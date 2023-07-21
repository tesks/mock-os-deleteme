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
package jpl.gds.db.impl.aggregate.batch.process;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.sql.fetch.aggregate.IChannelStreamFilter;
import jpl.gds.db.api.types.IDbChannelSampleUpdater;
import jpl.gds.db.impl.types.DatabaseChannelSample;
import jpl.gds.eha.api.channel.ChannelChangeFilter;

/**
 * This class is used to filter for changes only in the channel sample query 
 * output stream. Expects the sample stream to be in the final sorted order
 * for the change detection to work properly.
 *
 */
public class ChangesOnlyFilter implements IChannelStreamFilter<String> {

    private final ApplicationContext appContext;
    private final List<String> csvColumns;
    private final ChannelChangeFilter changeFilter;
    private final boolean includesHeaderRow;
    private boolean foundHeaderRow;

    public ChangesOnlyFilter(final ApplicationContext appContext,
            final ChannelChangeFilter changeFilter,
            final List<String> csvColumns,
            final boolean includesHeaderRow) {
        
        this.appContext = appContext;
        this.changeFilter = changeFilter;
        this.csvColumns = csvColumns;
        this.includesHeaderRow = includesHeaderRow;
    }

    @Override
    public List<String> filterRecordList(final List<String> recordList) {
        final List<String> changesOnlyRecordList = new ArrayList<>();
        final IDbChannelSampleUpdater chanSample = new DatabaseChannelSample(appContext);
        for (final String record : recordList) {

            // MPCS-11035 - chill_get_chanvals --changesOnly option throws
            // IllegalArgumentException Error
            // Issue caused by the combination of --changesOnly with -m, --showColumns
            // Handle the header row correctly when -m, --showColumns flag is specified
            // The column headers is the only row that does not start with the " character
            if (includesHeaderRow && !foundHeaderRow && !record.startsWith("\"")) {
                changesOnlyRecordList.add(record);
                foundHeaderRow = true;
                continue;
            }

            // Re-construct the Channel Sample Object from the CSV string form 
            chanSample.parseCsv(StringUtils.chomp(record), csvColumns);

            // MPCS-10915 - chill_get_chanvals using --changesOnly option
            // reports IllegalArgumentException: Input channel value DN is null
            // When DatabaseChannelSample reconstructs the object from the CSV string
            // empty DN values get set to null.
            //
            // If the value is null, set it to an empty String
            if (chanSample.getValue() == null) {
                chanSample.setValue("");
            }

            // Keep record if the value has changed
            if (valueHasChanged(chanSample)) {
                changesOnlyRecordList.add(record);
            }
        }
        
        return changesOnlyRecordList;
    }
    
    private boolean valueHasChanged(final IDbChannelSampleUpdater ref) {
        return changeFilter.getFilteredValue(ref.getChannelId(),
                ref.getChannelType(), ref.getValue());
    }

}
