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
package jpl.gds.monitor.perspective.view.channel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.shared.log.TraceManager;


/**
 * This class encapsulates an ordered set of channels display items for display or query. 
 * Such sets need to be updated, replaced, accessed and also 
 * writable to and from a channel set file, which can consist
 * of multiple lines of comma-separated channel display information. In addition to
 * channel IDs, channel sets can include separator elements, for use in report or display output.
 * Each item in the set can have DN (raw) and EU (value) display formats.
 * <br>
 * Format of each line in a channel set file is one of the following:
 * <br>
 * For a channel: ID;RawFormat;ValueFormat
 * <br>
 * For a blank line separator: [SEP];%s;%s
 * <br>
 * For a drawn line separator: [SEP][LINE]:%s;%s
 * <br>
 * For a text separator: [SEP]text;%s;%s
 * <br>
 * For a text + drawn line separator: [SEP][LINE]text;%s;%s
 * <br>
 * Duplicates may or may not be allowed within the set.
 */
public class ChannelSet
{
	private final static String SEPARATOR_START_TOKEN = "[SEP]";
	private final static String SEPARATOR_LINE_TOKEN = "[LINE]";

	private final boolean allowDuplicates;
	private List<ChannelDisplayFormat> set;
	private final static String CHANNEL_SEPARATOR_CHARACTER = ",";
	private final static String FIELD_SEPARATOR_CHARACTER = ";";

	/**
	 * Constructs an empty channel set that allows duplicate entries.
	 * 
	 */
	public ChannelSet()
	{
		this(true);
	}

	/**
	 * Public constructor
	 * 
	 * @param allowDuplicates true to allow duplicate channel id entries.
	 */
	public ChannelSet(final boolean allowDuplicates)
	{
		this.allowDuplicates = allowDuplicates;
		set = new ArrayList<ChannelDisplayFormat>();
	}

	/**
	 * Indicates if this channel set is empty of all separators and channels.
	 * @return true if empty, false if not
	 */
	public boolean isEmpty() {
		return set.isEmpty();
	}

	/**
	 * Add channel id to the end of the set. If it is a duplicate and duplicates 
	 * are not allowed, silently do not add to set.
	 * 
	 * @param channel the ChannelId to add
	 */
	public void addChannel(final String channel)
	{
		if(allowDuplicates == false)
		{
			if (contains(channel)) {
				return;
			}
		}
		set.add(new ChannelDisplayFormat(null, channel, false));
	}

	/**
	 * Add channel definition to the end of the set and return its display format object. 
	 * If it is a duplicate and duplicates are not allowed, silently do not add the definition.
	 * 
	 * @param channel the ChannelId to add
	 * @return new ChanneldisplayFormat object, or null if the channel was not added
	 */
	public ChannelDisplayFormat addChannel(final IChannelDefinition channel)
	{
		if(allowDuplicates == false)
		{
			if (contains(channel.getId())) {
				return null;
			}
		}
		final ChannelDisplayFormat newDef = new ChannelDisplayFormat(channel);
		set.add(newDef);
		return newDef;
	}

	/**
	 * Add a text separator to the end of the set. 
	 * 
	 * @param sep the separator string to add
	 */
	public void addSeparator(final String sep)
	{
		set.add(new ChannelDisplayFormat(null, sep, true));
	}

	/**
	 * Add a line-style separator to the end of the set. 
	 */
	public void addLine()
	{
		final ChannelDisplayFormat cd = new ChannelDisplayFormat(null, "", true);
		cd.setLine(true);
		set.add(cd);
	}

	/**
	 * Add a text and line-style separator to the set. 
	 * @param sep the separator text
	 */
	public void addSeparatorWithLine(final String sep)
	{
		final ChannelDisplayFormat cd = new ChannelDisplayFormat(null, sep, true);
		cd.setLine(true);
		set.add(cd);
	}

	/**
	 * Creates a copy of this channel set.
	 * @return new ChannelSet
	 */
	public ChannelSet copy()
	{
		final ChannelSet result = new ChannelSet();
		final ChannelDisplayFormat[] ids = getDisplayCharacteristics();
		for(final ChannelDisplayFormat item: ids) {
			result.set.add(item.copy());
		}
		return result;
	}

	/**
	 * Removes all entries (channels and separators) from the set.
	 */
	public void clearChannels()
	{
		set.clear();
	}

	/**
	 * Retrieves an ordered list of Channel IDs in the set.
	 * 
	 * @return array of channel IDs
	 */
	public String[] getIds()
	{
		final ArrayList<String> result = new ArrayList<String>();

		for(int index = 0; index < set.size(); index++)
		{
			if (!set.get(index).isSeparator()) {
				result.add(set.get(index).getChanId());
			}
		}
		String[] ids = new String[result.size()];
		ids = result.toArray(ids);

		return(ids);
	}

	/**
	 * Retrieves an ordered list of channel display items (both channels and
	 * separators) from the set.
	 * @return array of channel display items
	 */
	public ChannelDisplayFormat[] getDisplayCharacteristics() {
		ChannelDisplayFormat[] result = new ChannelDisplayFormat[set.size()];
		result = set.toArray(result);
		return result;
	}

	/**
	 * Retrieves a channel display items given a channel ID.
	 * @param id channel ID to look for
	 * @return channel display format for the channel, or null if not found
	 */
	public ChannelDisplayFormat getDisplayCharacteristics(final String id) {

		ChannelDisplayFormat[] result = new ChannelDisplayFormat[set.size()];
		result = set.toArray(result);
		for (int i = 0; i < result.length; i++) {
			if (result[i].isSeparator()) {
				continue;
			}
			if (result[i].getChanId().equals(id)) {
				return result[i];
			}
		}
		return null;
	}

	/**
	 * Returns the number of channels display items (both channels and separators) in the set.
	 * @return the number of display items in the set
	 */
	public int size()
	{
		return set.size();
	}

	/**
	 * Return true if the set contains the input Channel Id 
	 * (checked using toString()).
	 * 
	 * @param id the Channel Id string to look for
	 * @return true if the id is found in the set; false if not
	 */
	public boolean contains(final String id)
	{
		boolean contains = false;
		for(int index = 0; index < set.size(); index++)
		{
			final ChannelDisplayFormat inId = set.get(index);
			if (inId.isSeparator()) {
				continue;
			}
			if(id.toString().equals(inId.getChanId()))
			{
				contains = true;
				break;
			}
		}
		return contains;
	}

	/**
	 * Loads the channel set with channel IDs from a file.
	 * @param filename the file to load
	 * @throws IOException if there is an error reading the file
	 */
	public void loadFromFile(IChannelDefinitionProvider chanDefs, final String filename) throws IOException
	{
		final BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));

		String readLine = reader.readLine();

		while(readLine != null)
		{
			final String chanSetLine = readLine.trim();

			// skip lines that are commented
			if(chanSetLine.startsWith("//") == false)
			{
				parseChanSetLine(chanDefs, chanSetLine);
			}

			readLine = reader.readLine();
		}

		reader.close();
	}

	/**
	 * Loads the set of channel display information from a string, which may consist
	 * of multiple channel display items separated by commas. Newlines are tolerated.
	 * @param content the String to parse for channel IDs.
	 */
	public void loadFromString(IChannelDefinitionProvider chanDefs, final String content)
	{
		final StringTokenizer tokens = new StringTokenizer(content, "\n");

		while(tokens.hasMoreTokens())
		{
			final String chanSetLine = tokens.nextToken().trim();
			if (chanSetLine.equals("")) {
				continue;
			}
			parseChanSetLine(chanDefs, chanSetLine);
		}
	}

	private void parseChanSetLine(IChannelDefinitionProvider chanDefs, final String line)
	{
		// Parse comma-separated channel ids
		final String[] vals = line.split(CHANNEL_SEPARATOR_CHARACTER);

		for(int index = 0; index < vals.length; index++)
		{
			ChannelDisplayFormat newEntry = null;
			final String val = vals[index].trim();
			final String[] pieces = val.split(FIELD_SEPARATOR_CHARACTER);
			if (pieces[0].startsWith(SEPARATOR_START_TOKEN)) {
				String sep = pieces[0].substring(SEPARATOR_START_TOKEN.length());
				if (sep.startsWith(SEPARATOR_LINE_TOKEN)) {
					sep = sep.substring(SEPARATOR_LINE_TOKEN.length());
					newEntry = new ChannelDisplayFormat(null, sep, true);
					newEntry.setLine(true);
				} else {
					newEntry = new ChannelDisplayFormat(null, sep, true);
				}
			} else {
				try {
					final String id = pieces[0];
					newEntry = new ChannelDisplayFormat(chanDefs.getDefinitionFromChannelId(id), id, false);
				} catch (final Exception e) {
					e.printStackTrace();
					TraceManager.getDefaultTracer().error("Bad channel ID or separator text is likely cause: " + pieces[0]);

					continue;
				}
			}
			if (pieces.length > 1 && !pieces[1].equals("null")) {
				newEntry.setRawFormat(pieces[1]);
			}
			if (pieces.length > 2 && !pieces[2].equals("null")) {
				newEntry.setValueFormat(pieces[2]);
			}
			set.add(newEntry);
		}
	}

	/**
	 * Saves the set of channel display items (IDs and separators) to a file.
	 * @param filename the file to write to
	 * @throws IOException if there is an error writing the file
	 */
	public void saveToFile(final String filename) throws IOException
	{
		final BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
		writer.write(this.toString());
		writer.flush();
		writer.close();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		int written = 0;
		final ChannelDisplayFormat[] ids = getDisplayCharacteristics();
		final StringBuffer chanBuf = new StringBuffer();
		for(int index = 0; index < ids.length; index++)
		{
			if (ids[index].isSeparator()) {
				if (ids[index].isLine()) {
					chanBuf.append(SEPARATOR_START_TOKEN + SEPARATOR_LINE_TOKEN + ids[index].getSaveableSeparatorString());
				} else {
					chanBuf.append(SEPARATOR_START_TOKEN+ids[index].getSaveableSeparatorString());
				}
			} else {
				chanBuf.append(ids[index].getChanId());
			}
			chanBuf.append(FIELD_SEPARATOR_CHARACTER);
			chanBuf.append(ids[index].getRawFormat());
			chanBuf.append(FIELD_SEPARATOR_CHARACTER);
			chanBuf.append(ids[index].getValueFormat());

			if(index < (ids.length - 1))
			{
				chanBuf.append(",");
			}
			if(written == 9)
			{
				chanBuf.append("\n");
				written = 0;
			}
			written++;
		}
		return chanBuf.toString();
	}

	/**
	 * Sorts the channel list in either ascending or descending order.
	 * @param ascending true of sort should be ascending; false for descending
	 */
	public void sort(final boolean ascending) {
		final ChannelDisplayFormat[] items = getDisplayCharacteristics();
		final Collator collator = Collator.getInstance(Locale.getDefault());
		boolean changed = true;
		while (changed) {
			changed = false;    
			for (int i = 0; i < items.length - 1; i++) {
				String value1;
				if (items[i].isSeparator()) {
					value1 = items[i].getSeparatorString();
				} else {
					value1 = items[i].getChanId().toString();
				}
				String value2;
				if (items[i+1].isSeparator()) {
					value2 = items[i+1].getSeparatorString();
				} else {
					value2 = items[i+1].getChanId();
				}
				boolean condition;
				if (ascending) {
					condition = collator.compare(value1, value2) > 0;
				} else {
					condition = collator.compare(value1, value2) < 0;
				}
				if (condition) {
					changed = true;
					final ChannelDisplayFormat temp = items[i];
					items[i] = items[i+1];
					items[i+1] = temp;
				}
			}
		}

		set = new ArrayList<ChannelDisplayFormat>(items.length);
		for (int i = 0; i < items.length; i++) {
			set.add(items[i]);
		}
	}

	/**
	 * Removed the channel with the given ID from the set.
	 * @param id channel ID to remove
	 */
	public void removeChannel(final String id) {
		int removeIndex = -1;
		for(int index = 0; index < set.size(); index++)
		{
			if (!set.get(index).isSeparator()) {
				if (set.get(index).getChanId().equals(id)) {
				    removeIndex = index;	
				    break;
				}
			}
		}
		if (removeIndex != -1) {
			set.remove(removeIndex);		
		}
	}
}