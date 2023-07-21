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
package jpl.gds.shared.channel;


import java.util.regex.Pattern;

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.types.Pair;

/**
 * A channel ID is used to represent the identification of a single telemetry
 * channel. Channel IDs take the form of one letter, followed by zero or more
 * alphanumeric characters (this is the channel source), followed by a dash,
 * followed by 1 or more numbers (this is the channel number). In general,
 * ChannelIDs are now just represented as Strings in the remaining code. This
 * class exists for compatibility with old interfaces only, and to provide
 * several static methods that are useful for manipulating Channel ID strings.
 * 
 * Class is final so that copy constructor is sufficient to clone.
 * 
 */
public final class ChannelIdUtility
{
	
    /** Regular expression to match channel ids */
	public static final String CHANNEL_ID_REGEX = "[A-Za-z]{1}[A-Za-z0-9]{0,}-[0-9]{1,}";

	private static final String CHANNEL_ID_RANGE_REGEX = CHANNEL_ID_REGEX + "\\.\\." + CHANNEL_ID_REGEX;
	private static final String CHANNEL_ID_WILDCARD_REGEX =
		"[A-Za-z%_]{1}[A-Za-z0-9%_]{0,}[-]{0,1}[0-9%_]{1,}";
	private static final String PERCENT =
		"[A-Za-z]{0,}[A-Za-z0-9]{0,}[-]{0,1}[%]{1,}[-]{0,1}[0-9]{0,}";
	private static final String UNDERSCORE =
		"[_]{3,}";
	private static final String CHANNEL_SEP = "-";

	private static final int MAX_NUMBER  = 9999;

	private static boolean checkIntId(final int id) {
		if ((id < 'A') || (id > 'Z')) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the numeric portion of the given channel ID as an integer.
	 * 
	 * @param chanId legally formed channel ID string
	 * @return channel number
	 */
	public static int getChannelIdNumber(final String chanId) {
		if (chanId == null) {
			throw new IllegalArgumentException("chanId argument cannot be null");
		}
		if (!isChanIdString(chanId)) {
			throw new IllegalArgumentException("chanId " + chanId + " is not a valid channel ID");
		}
		final String[] pieces = chanId.split(CHANNEL_SEP);
		return Integer.parseInt(pieces[1]);		
	}

	/**
	 * Gets an integer representation of the channel source. This only takes into account
	 * the first letter of the source, not multi-letter sources.  This method is largely used 
	 * in legacy channel index generation.
	 * 
	 * @param chanId legally formed channel ID string
	 * @return source number
	 */
	public static int getStemAsInteger(final String chanId) {
		if (chanId == null) {
			return -1;
		}
		if (!isChanIdString(chanId)) {
			throw new IllegalArgumentException("chanId " + chanId + " is not a valid channel ID");
		}
		final char c = chanId.charAt(0);
		final int id = c - ('A') + 1;

		return id; 
	}

	/**
	 * Verifies that a channel ID string is legally formed.
	 * 
	 * @param test the channel ID string to test
	 * @return true if the channel ID string is valid; false if not
	 */
	public static boolean isChanIdString(final String test) {
		return (Pattern.matches(CHANNEL_ID_REGEX, test) || Pattern.matches(CHANNEL_ID_RANGE_REGEX, test) || 
				Pattern.matches(CHANNEL_ID_WILDCARD_REGEX,test) || Pattern.matches(PERCENT, test) || Pattern.matches(UNDERSCORE, test));
	}


	/**
	 * Verifies that a channel ID string is legally formed and not a wildcard
     * or a range.
	 * 
	 * @param test the channel ID string to test
     *
	 * @return true if the channel ID string is valid; false if not
	 */
	public static boolean isPureChannelIdString(final String test)
    {
		return Pattern.matches(CHANNEL_ID_REGEX, test);
	}

	/**
	 * Verifies that string is a legally formed channel ID range.
	 * 
	 * @param test the string to test
     *
	 * @return true if the string is a channel ID range, false otherwise
	 */
	public static boolean isChannelIdRangeString(final String test)
	{
		return Pattern.matches(CHANNEL_ID_RANGE_REGEX,test);
	}

	/**
	 * Constructs a valid channel ID string from an integer source specifier, and a channel number.
	 * This method is only used for channels that have a single letter source.
	 * 
	 * @param src integer value representing the source character
	 * @param num the channel number
	 * @return formatted channel ID string
	 */
	public static String constructChannelId(final int src, final int num) {
		if (checkIntId(src) == false) {
			throw new ChannelIdException("Bad Channel source: " + src);
		}
		final char idChar = (char)src;
		if (num > MAX_NUMBER) {
			throw new ChannelIdException("Bad Channel Number Format: " + num);
		}
		final StringBuilder ret = new StringBuilder();
		final String nbuff = String.valueOf(num);
		ret.append(idChar);
		ret.append(CHANNEL_SEP);
		ret.append(GDR.fillZero(nbuff, 4));
		return ret.toString();
	}

	/**
	 * Constructs a valid channel ID string from a String source specifier, and a channel number..
	 * 
	 * @param src the string representing the channel source
	 * @param num the channel number
	 * @return formatted channel ID string
	 */
	public static String constructChannelId(final String src, final int num) {

		if (num > MAX_NUMBER) {
			throw new ChannelIdException("Bad Channel Number Format: " + num);
		}
		final StringBuilder ret = new StringBuilder();
		final String nbuff = String.valueOf(num);
		ret.append(src);
		ret.append(CHANNEL_SEP);
		ret.append(GDR.fillZero(nbuff, 4));
		return ret.toString();
	}

    /**
     * Split apart a channel-id according to the rules. It returns an encoded stem letter and
     * a number. Only a single letter is allowed in the stem.
     *
     * @param cid Channel-id
     *
     * @return Pair of type and channel
     *
     * @throws ChannelIdException On failure to split
     */
    public static Pair<Integer, Integer> parseLegacyChannelId(final String cid)
        throws ChannelIdException
    {
        final String[] split = cid.split("-", -1);

        if ((split.length != 2)      ||
            (split[0].length() != 1) ||
            (split[1].length() < 1)  ||
            (split[1].length() > 4))
        {
            throw new ChannelIdException("Bad channel-id: '" + cid + "'");
        }

        final char ctype = split[0].toUpperCase().charAt(0);

        if ((ctype < 'A') || (ctype > 'Z'))
        {
            throw new ChannelIdException("Bad channel-id: '" + cid + "'");
        }

        final int type    = ctype - 'A' + 1;
        int       channel = 0;

        try
        {
            channel = Integer.parseInt(split[1]);
        }
        catch (final NumberFormatException nfe)
        {
            throw new ChannelIdException("Bad channel-id: '" + cid + "'");
        }

        if ((channel < 0) || (channel > 9999))
        {
            throw new ChannelIdException("Bad channel-id: '" + cid + "'");
        }

        return new Pair<Integer, Integer>(type, channel);
    }
    
 
}
