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
package jpl.gds.db.app.ctab;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpl.gds.db.api.sql.order.IChannelValueOrderByType;
import jpl.gds.db.api.types.IDbChannelSampleProvider;
import jpl.gds.db.impl.types.DatabaseChannelSample;
import jpl.gds.db.mysql.impl.sql.order.ChannelValueOrderByType;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.time.ILocalSolarTime;

/**
 * CTAB is a way of printing channel values. The user specifies a template like:
 *
 * SCET C1(DN) SCLK C2(EU)
 *
 * and channel values queried from the database are formatted that way.
 *
 * This class holds the sequence elements and applies them to the data.
 *
 * A missing template is the same as:
 *
 * ERT C1(DN) C2(DN) ... for all specified channels.
 *
 * Not synchronized in any way.
 *
 * The sort ordering is determined from the order in which the time elements
 * appear in the template. There must be at least one time element. The sort
 * ordering will be used to query from the database.
 *
 * The ordering used to combine lines need not involve all time types.
 *
 * The constructor must pass a list of channel names, used both to construct the
 * header line and to specify the number of channels.
 *
 * The basic idea is that a CtabSequence will be created from a list of channels
 * and a template (or default.) The database will be queried using the sort
 * order, and a list of channels presented successively to the CtabSequence to
 * turn them into a line for displaying.
 *
 * At least one of the channels presented must be non-null.
 *
 * Elements are combined when they have exactly the same time values as
 * specified in the template, and represent different channels not already
 * present. Thus, C1, C2, and C3 may be combined if the times match, but
 * C1, C2, and C2 will combine only the first two if the times match.
 *
 * Note that the sequence elements are given a reference to the list of
 * channels, but do not dereference it until necessary.
 *
 */
public class CtabSequence extends Object
{
    private static final String ME = "CtabSequence";

    private final List<IDbChannelSampleProvider> _channels;

    private final List<IDbChannelSampleProvider> _empty;

    private final List<AbstractCtabElement>     _elements   =
        new ArrayList<AbstractCtabElement>();

    private final List<String>                  _names;

    private final List<ChannelValueOrderByType> _orders     =
        new ArrayList<ChannelValueOrderByType>(4);

    private final List<ChannelValueOrderByType> _sortOrders =
        new ArrayList<ChannelValueOrderByType>(4);

    private final StringBuilder                 _sb         =
        new StringBuilder();

    private final int                           _channelCount;
    private final PrintWriter                   _printWriter;

	private final boolean _formatChannelVals;

    private final SprintfFormat _formatUtil;


    private static final String  NCGO    = "(?:";
    private static final String  NCGC    = ")";
    private static final String  PO      = "\\(";
    private static final String  PC      = "\\)";
    private static final String  TIMES   = "(ERT|SCET|SCLK|LST)"; // Captured
    private static final String  INDEX   = "([1-9][0-9]*)";       // Ditto
    private static final String  TYPE    = "(DN|EU)";             // Ditto
    private static final String  CID     = NCGO                             +
                                               "C" + INDEX + PO + TYPE + PC +
                                           NCGC;
    private static final String  ELEMENT = NCGO + TIMES + "|" + CID + NCGC;
    private static final String  WHITE   = "\\s*";
    private static final String  UNIT    = WHITE + ELEMENT + WHITE;
    private static final Pattern PATTERN = Pattern.compile(UNIT);


    /**
     * Build CTAB sequence from template.
     *
     * @param template
     *            Template describing CTAB fields
     * @param names
     *            Channel names to be inserted
     * @param writer
     *            PrintWriter to use for printing
     * @param formatChanVals
     *            true if chanvals should be formatted
     * @param formatter
     *            the formatter with which to format chanvals
     *
     * @throws CtabException
     *             Ctab exception
     */
    public CtabSequence(final String       template,
                        final List<String> names,
                        final PrintWriter  writer,
                        final boolean formatChanVals,
                        final SprintfFormat formatter) throws CtabException
    {
        super();
        
        this._formatChannelVals = formatChanVals;
        this._formatUtil = formatter;

        _printWriter = (writer != null) ? writer : new PrintWriter(System.out);

        if (names == null)
        {
            throw new CtabException(ME + " Null names");
        }

        if (names.isEmpty())
        {
            throw new CtabException(ME + " No channel names provided");
        }

        _channelCount = names.size();

        final List<String> tempNames = new ArrayList<String>(_channelCount);

        tempNames.addAll(names);

        _names = Collections.unmodifiableList(tempNames);

        // Construct empty list for use in presetting _channels

        final List<DatabaseChannelSample> temp =
            new ArrayList<DatabaseChannelSample>(_channelCount);

        for (int i = 0; i < _channelCount; ++i)
        {
            temp.add(null);
        }

        _empty = Collections.unmodifiableList(temp);

        _channels = new ArrayList<IDbChannelSampleProvider>(_channelCount);

        resetChannels();

        // Take care of templates

        if ((template != null) && (template.length() > 0))
        {
            parseTemplate(template);
        }
        else
        {
            createDefaultTemplate();
        }

        _sortOrders.addAll(_orders);
    }


    /**
     * Return the sort ordering as specified by the template.
     *
     * @return List<ChannelValueOrderByType>
     */
    public List<ChannelValueOrderByType> getSortOrdering()
    {
        return new ArrayList<ChannelValueOrderByType>(_sortOrders);
    }


    /**
     * Return print writer.
     *
     * @return PrintWriter
     */
    public PrintWriter getPrintWriter()
    {
        return _printWriter;
    }


    /**
     * Accept next channel value and deal with it.
     *
     * @param channel The next channel value
     *
     * @throws CtabException Ctab exception
     */
    public void provideChannel(final IDbChannelSampleProvider channel)
        throws CtabException
    {
        final int index = getIndex(channel); // Checks for null, etc.

        if ((_channels.get(index) != null) || ! timesMatch(channel))
        {
            // This index is populated already or times don't match,
            // terminate sequence

            processChannels();
        }

        // He either goes in the current list or a new reset list

        _channels.set(index, channel);
    }


    /**
     * No more channels, drive out partial list
     *
     * @throws CtabException Ctab exception
     */
    public void purgeChannel() throws CtabException
    {
        if (! _channels.equals(_empty))
        {
            processChannels();
        }
    }


    /**
     * See if this channel matches the times of the current channels. It must
     * match all times selected in the template. If the channel list is empty,
     * that counts as a match.
     *
     * We have to match only one channel.
     *
     * @param channel
     *
     * @return True if matches
     *
     * @throws CtabException Ctab exception
     */
    private boolean timesMatch(final IDbChannelSampleProvider channel)
        throws CtabException
    {
        if (channel == null)
        {
            throw new CtabException(ME + ".timesMatch Null channel");
        }

        for (final IDbChannelSampleProvider item : _channels)
        {
            if (item == null)
            {
                continue;
            }

            for (final ChannelValueOrderByType order : _orders)
            {
                switch (order.getValueAsInt())
                {
                    case IChannelValueOrderByType.ERT_TYPE:

                        if (! channel.getErt().equals(item.getErt()))
                        {
                            return false;
                        }

                        break;

                    case IChannelValueOrderByType.SCET_TYPE:

                        if (! channel.getScet().equals(item.getScet()))
                        {
                            return false;
                        }

                        break;

                    case IChannelValueOrderByType.SCLK_TYPE:

                        if (! channel.getSclk().equals(item.getSclk()))
                        {
                            return false;
                        }

                        break;

                    // Beware that LST may be null

                    case IChannelValueOrderByType.LST_TYPE:

                        final ILocalSolarTime lst1  = channel.getLst();
                        final ILocalSolarTime lst2  = item.getLst();
                        final boolean        null1 = (lst1 == null);
                        final boolean        null2 = (lst2 == null);

                        if (null1 != null2)
                        {
                            return false;
                        }

                        // Both null or neither null

                        if (! null1 && ! lst1.equals(lst2))
                        {
                            return false;
                        }

                        break;

                    default:

                        throw new CtabException(ME                         +
                                                ".timesMatch Unsupported " +
                                                "order-by: "               +
                                                order);
                }
            }

            // We matched one guy, the others must be the same

            return true;
        }

        // There were no non-null guys

        return true;
    }


    /**
     * Reset _channels to initial state
     */
    private void resetChannels()
    {
        _channels.clear();

        _channels.addAll(_empty);
    }


    /**
     * Look up index of channel name.
     *
     * @param channel
     *
     * @throws CtabException Ctab exception
     */
    private int getIndex(final IDbChannelSampleProvider channel)
        throws CtabException
    {
        if (channel == null)
        {
            throw new CtabException(ME + ".getIndex Null channel");
        }

        final String name = channel.getChannelId().toUpperCase();
        final int    index = _names.indexOf(name);

        if (index < 0)
        {
            throw new CtabException(ME                            +
                                    ".getIndex Unknown channel: " +
                                    name);
        }

        return index;
    }


    /**
     * Apply channels to sequence and print.
     *
     * @throws CtabException Ctab exception
     */
    private void processChannels() throws CtabException
    {
        _sb.setLength(0);

        try
        {
            for (final AbstractCtabElement element : _elements)
            {
                element.appendAsString(_sb);
            }

            _printWriter.println(_sb);
        }
        finally
        {
            resetChannels();

            _sb.setLength(0);
        }
    }


    /**
     * Get header string.
     *
     * @return String
     *
     * @throws CtabException Ctab exception
     */
    public String getHeader() throws CtabException
    {
        _sb.setLength(0);

        for (final AbstractCtabElement element : _elements)
        {
            element.appendAsHeader(_sb);
        }

        final String result = _sb.toString();

        _sb.setLength(0);

        return result;
    }


    /**
     * Print header string.
     *
     * @throws CtabException Ctab exception
     */
    public void printHeader() throws CtabException
    {
        _printWriter.println(getHeader());
    }


    /**
     * Create default template.
     *
     * @throws CtabException Ctab exception
     */
    private void createDefaultTemplate() throws CtabException
    {
        _elements.add(new ErtCtabElement(_channels));

        for (int i = 0; i < _channelCount; ++i)
        {
            _elements.add(new DnCtabElement(_channels, i, _names.get(i), _formatChannelVals, _formatUtil));
        }

        _orders.add(ChannelValueOrderByType.ERT);
    }


    /**
     * Build CTAB sequence from template.
     *
     * @param template
     *
     * @throws CtabException Ctab exception
     */
    private void parseTemplate(final String template) throws CtabException
    {
        if (template == null)
        {
            throw new CtabException(ME + ".parseTemplate Null template");
        }

        final BitSet dneus = new BitSet(_channelCount);

        _sb.setLength(0);

        _sb.append(template.toUpperCase());

        while (_sb.length() > 0)
        {
            final Matcher m = PATTERN.matcher(_sb);

            if (! m.lookingAt() || (m.groupCount() != 3))
            {
                if (_sb.length() > 20)
                {
                    // Guard against a ridiculously long message
                    _sb.setLength(20);
                }

                throw new CtabException(
                              ME                                             +
                              ".parseTemplate Could not parse template at '" +
                              _sb                                            +
                              "'");
            }

            final String time = m.group(1);

            if (time != null)
            {
                handleTime(time);
            }
            else
            {
                handleValue(m.group(2), m.group(3), dneus);
            }

            // Delete everything we matched

            _sb.delete(0, m.group(0).length());
        }

        // Check that out-of-range indices do not appear

        if (dneus.length() > _channelCount)
        {
            throw new CtabException(ME +
                                    ".parseTemplate Template has channel " +
                                    "index that exceeds channel count: "   +
                                    _channelCount);
        }

        // Check that each channel index appears

        if (dneus.cardinality() < _channelCount)
        {
            throw new CtabException(ME +
                                    ".parseTemplate Template does not " +
                                    "specify all channel indices: "     +
                                    _channelCount);
        }
    }


    /**
     * Add time elements to list.
     *
     * @param time
     *
     * @throws CtabException Ctab exception
     */
    private void handleTime(final String time) throws CtabException
    {
        if ("ERT".equals(time))
        {
            _elements.add(new ErtCtabElement(_channels));

            if (! _orders.contains(ChannelValueOrderByType.ERT))
            {
                _orders.add(ChannelValueOrderByType.ERT);
            }

            return;
        }

        if ("SCET".equals(time))
        {
            _elements.add(new ScetCtabElement(_channels));

            if (! _orders.contains(ChannelValueOrderByType.SCET))
            {
                _orders.add(ChannelValueOrderByType.SCET);
            }

            return;
        }

        if ("SCLK".equals(time))
        {
            _elements.add(new SclkCtabElement(_channels));

            if (! _orders.contains(ChannelValueOrderByType.SCLK))
            {
                _orders.add(ChannelValueOrderByType.SCLK);
            }

            return;
        }

        
        if ("LST".equals(time))
        {
            _elements.add(new LstCtabElement(_channels));

            if (! _orders.contains(ChannelValueOrderByType.LST))
            {
                _orders.add(ChannelValueOrderByType.LST);
            }

            return;
        }

        throw new CtabException(ME                                     +
                                ".handleTime Unrecognized time type '" +
                                time                                   +
                                "'");
    }


    /**
     * Add value elements to list. We keep track of the indices we have seen.
     *
     * @param indexString
     * @param type
     * @param dneus
     *
     * @throws CtabException Ctab exception
     */
    private void handleValue(final String indexString,
                             final String type,
                             final BitSet dneus) throws CtabException
    {
        int index = 0;

        try
        {
            index = Integer.parseInt(indexString);
        }
        catch (final NumberFormatException nfe)
        {
            throw new CtabException(ME                             +
                                        ".handleValue Bad index '" +
                                        indexString                +
                                        "'",
                                    nfe);
        }

        if (index < 1)
        {
            throw new CtabException(ME                    +
                                    ".handleValue Index " +
                                    index                 +
                                    " is less than 1");
        }

        if (index > _channelCount)
        {
            throw new CtabException(ME                    +
                                    ".handleValue Index " +
                                    index                 +
                                    " exceeds number of channels " +
                                    _channelCount);
        }

        final int indexm1 = index - 1;

        if ("DN".equals(type))
        {
            _elements.add(
                new DnCtabElement(_channels, indexm1, _names.get(indexm1), _formatChannelVals, _formatUtil));

            // Remember we have seen this index
            dneus.set(indexm1);

            return;
        }

        if ("EU".equals(type))
        {
            _elements.add(
                new EuCtabElement(_channels, indexm1, _names.get(indexm1), _formatChannelVals, _formatUtil));

            // Remember we have seen this index
            dneus.set(indexm1);

            return;
        }

        throw new CtabException(ME                                      +
                                ".handleValue Unrecognized data type '" +
                                type                                    +
                                "'");
    }
}
