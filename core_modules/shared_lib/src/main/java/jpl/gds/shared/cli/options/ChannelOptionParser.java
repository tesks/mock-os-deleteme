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
package jpl.gds.shared.cli.options;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import jpl.gds.shared.channel.ChannelIdUtility;
import jpl.gds.shared.channel.ChannelListRange;
import jpl.gds.shared.channel.ChannelListRangeException;
import jpl.gds.shared.cli.cmdline.ICommandLine;

/**
 * CLI channel parser.  Accepts wild card and ranges.  Used most of the code from the fetch apps
 * to expand ranges.  Empty spaces in the csv (V1,,V2) will cause exceptions as well as values that do not match 
 * the established channel id format.
 * 
 */
public class ChannelOptionParser extends AbstractOptionParser<Collection<String>> {
    private final String wildCardSymbol;
    private final String replaceWildCardSymbol;
 
    /**
     * Parses the command line values 
     * 
     * @param wildCardSymbol - Wild card symbol to be substituted.
     * @param wildCardReplaceSymbol - Wild card substitution symbol.
     */
    public ChannelOptionParser(final String wildCardSymbol, final String wildCardReplaceSymbol) {
		super();
		this.wildCardSymbol = wildCardSymbol;
		this.replaceWildCardSymbol = wildCardReplaceSymbol;
	}

	/**
     * Scan for null and empty channel ids. Expand ranges. Return repaired
     * array.
     *
     * @param channelIds Array of channel ids
     *
     * @return Purified array
     *
     * @throws ChannelListRangeException Bad channel-list range
     */
    public final String[] purify(final String[] channelIds)
        throws ChannelListRangeException
    {
        final String[] rangedCids = clean(channelIds);

        if (rangedCids.length == 0)
        {
            return rangedCids;
        }

        final ChannelListRange clr          = new ChannelListRange();
        final String[]         unrangedCids =
                                   clr.genChannelListFromRange(rangedCids);

        return asArray(new TreeSet<String>(Arrays.asList(unrangedCids)));
    }
    
    /**
     * Scan for null and empty strings. Return repaired and ordered array.
     *
     * @param items Array of strings
     *
     * @return Purified array
     */
    private final String[] clean(final String[] items)
    {
        if (items == null)
        {
            return new String[0];
        }

        final Set<String> result = new TreeSet<String>();

        for (final String item : items)
        {
            if (item == null)
            {
                continue;
            }

            final String useItem = item.trim().toUpperCase();

            if (useItem.isEmpty())
            {
                continue;
            }

            if (wildCardSymbol == null || replaceWildCardSymbol == null) {
            		/**
            		 * No substitution. 
            		 */
            		result.add(useItem);
            } else {
	            // Replace command line wild card with the URL wild card.
	            result.add(StringUtils.replace(useItem, wildCardSymbol, replaceWildCardSymbol));
            }
        }

        return asArray(result);
    }
	
    /**
     * Convert a collection to an array
     *
     * @param target Collection to convert
     *
     * @return Array
     */
    private String[] asArray(final Collection<String> target)
    {
        return target.toArray(new String[target.size()]);
    }
 
	@Override
	public Collection<String> parse(final ICommandLine commandLine, final ICommandLineOption<Collection<String>> opt)
			throws ParseException {
         final String chanIdString = getValue(commandLine,opt);

        if (chanIdString == null) {
            return null;
        }
        
        String[] chanIdArr = null;

        chanIdArr = chanIdString.trim().split(",{1}");

        for (int i = 0; i < chanIdArr.length; i++) {
            chanIdArr[i] = chanIdArr[i].trim();

            if (!ChannelIdUtility.isChanIdString(chanIdArr[i])) {
                throw new ParseException("Option " + opt.getLongOrShort() + " input channel ID '"
                        + chanIdArr[i] + "' is not a valid channel ID."
                        + "  Channel IDs should follow "
                        + "the regular expression "
                        + ChannelIdUtility.CHANNEL_ID_REGEX);
            }
        }


        try {
            chanIdArr = purify(chanIdArr);
        } catch (final ChannelListRangeException clre) {
            throw new ParseException("Option " + opt.getLongOrShort() + " has invalid channel ranges for input string: " + chanIdString);
        }

        /**
         * Make a hash set to make sure there are no duplicates.
         */
        return new HashSet<>(Arrays.asList(chanIdArr));
	}
}
