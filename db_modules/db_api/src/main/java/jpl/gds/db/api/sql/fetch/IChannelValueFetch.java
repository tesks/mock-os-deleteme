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
package jpl.gds.db.api.sql.fetch;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.shared.channel.ChannelListRange;
import jpl.gds.shared.channel.ChannelListRangeException;
import jpl.gds.shared.time.DatabaseTimeRange;

public interface IChannelValueFetch extends IDbSqlFetch  {
    /**
     * {@inheritDoc}
     */
    @Override
    List<? extends IDbRecord> get(IDbContextInfoProvider tsi, DatabaseTimeRange range, int batchSize, Object... params)
            throws DatabaseException;

    /**
     * {@inheritDoc}
     */
    @Override
    List<? extends IDbRecord> getNextResultBatch() throws DatabaseException;

    /**
     * Scan for null and empty channel ids. Expand ranges. Return repaired
     * array.
     *
     * @param channelIds
     *            Array of channel ids
     *
     * @return Purified array
     *
     * @throws ChannelListRangeException
     *             Bad channel-list range
     */
    public static String[] purify(final String[] channelIds) throws ChannelListRangeException {
        final String[] rangedCids = clean(channelIds);

        if (rangedCids.length == 0) {
            return rangedCids;
        }

        final ChannelListRange clr = new ChannelListRange();
        final String[] unrangedCids = clr.genChannelListFromRange(rangedCids);

        return asArray(new TreeSet<String>(Arrays.asList(unrangedCids)));
    }

    /**
     * Scan for null and empty strings. Return repaired and ordered array.
     *
     * @param items
     *            Array of strings
     *
     * @return Purified array
     */
    public static String[] clean(final String[] items) {
        if (items == null) {
            return new String[0];
        }

        final Set<String> result = new TreeSet<String>();

        for (final String item : items) {
            if (item == null) {
                continue;
            }

            final String useItem = item.trim().toUpperCase();

            if (useItem.isEmpty()) {
                continue;
            }

            result.add(useItem);
        }

        return asArray(result);
    }

    /**
     * Convert a collection to an array
     *
     * @param target
     *            Collection to convert
     *
     * @return Array
     */
    public static String[] asArray(final Collection<String> target) {
        return ((target != null) ? target.toArray(new String[target.size()]) : null);
    }
}