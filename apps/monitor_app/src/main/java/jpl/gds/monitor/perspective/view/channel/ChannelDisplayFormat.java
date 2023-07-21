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

import jpl.gds.dictionary.api.channel.IChannelDefinition;


/**
 * The ChannelDisplayItem class holds the display configuration information for one
 * item in a channel list. List items may be channel IDs or separators.
 *
 */
public class ChannelDisplayFormat {

	private String chanId;
	private boolean isASeparator;
	private String separatorString;
	private String rawFormat;
	private String valueFormat;
	private boolean isALine;
	private IChannelDefinition def;

	/**
	 * Creates an instance of ChannelDisplayItem for a channel ID or separator string. Default display
	 * formats will be pulled from the channel dictionary table, if one exists.
	 * @param chanOrSep the channel Id or separator text.
	 * @param isSep true if this is a separator instance, false if a channel instance
	 */
	public ChannelDisplayFormat(IChannelDefinition cdef, final String chanOrSep, final boolean isSep) { 
		if (!isSep) {
			chanId = chanOrSep;
			isASeparator = false;
			this.def = cdef;
			if (def != null) {
				rawFormat = def.getDnFormat();
				valueFormat = def.getEuFormat();
			}
		} else {
			separatorString = chanOrSep;
			isASeparator = true;
			rawFormat = "%s";
			valueFormat = "%s";
		}
	}
	
	/**
	 * Basic constructor.
	 */
    public ChannelDisplayFormat() {}

	/**
	 * Creates an instance of ChannelDisplayItem for a channel definition. 
	 * @param def the Channel definition
	 */
	public ChannelDisplayFormat(final IChannelDefinition def) {
		this.def = def;
		chanId = def.getId();
		isASeparator = false;
		rawFormat = def.getDnFormat();
		valueFormat = def.getEuFormat();
	}

	/**
	 * Configures a separator display item to show a drawn line.
	 * @param enable true to turn on the line; false to turn off the line
	 */
	public void setLine(final boolean enable) {
		isALine = enable;
	}

	/**
	 * Indicates if a separator display item includes a drawn line.
	 * @return true if a line; false if not
	 */
	public boolean isLine() {
		return isALine;
	}

	/**
	 * Retrieves the channel ID for this display item.
	 * @return the channel ID
	 */
	public String getChanId() {
		return chanId;
	}

	/**
	 * Sets the channel ID for this display item.
	 * @param chanId the channel ID to set
	 */
	public void setChanId(final String chanId) {
		this.chanId = chanId;
	}

	/**
	 * Indicates whether this display item is a separator.
	 * @return true if a separator, false if a channel
	 */
	public boolean isSeparator() {
		return isASeparator;
	}

	/**
	 * Sets the flag indicating this display item is a separator.
	 * @param isSeparator true to set this item as a separator; false if not
	 */
	public void setSeparator(final boolean isSeparator) {
		this.isASeparator = isSeparator;
	}

	/**
	 * Retrieves the separator text.
	 * @return the text
	 */
	public String getSeparatorString() {
		return separatorString.replace("[COMMA]", ",").replace("[AMP]", "&");
	}

	/**
	 * Retrieves the separator text that can be written to the channel set file.
	 * @return the text
	 */
	public String getSaveableSeparatorString() {
		return separatorString.replace(",", "[COMMA]").replace("&", "[AMP]");
	}

	/**
	 * Sets the separator text.
	 * @param separatorString the text to set
	 */
	public void setSeparatorString(final String separatorString) {
		this.separatorString = separatorString;
	}

	/**
	 * Retrieves the raw display format specifier (C printf style).
	 * @return the format string
	 */
	public String getRawFormat() {
		return rawFormat;
	}

	/**
	 * Sets the raw display format specifier (C printf style)
	 * @param rawFormat the format string to set
	 */
	public void setRawFormat(final String rawFormat) {
		this.rawFormat = rawFormat;
	}

	/**
	 * Retrieves the value format specifier (C printf style)
	 * @return the format string
	 */
	public String getValueFormat() {
		return valueFormat;
	}

	/**
	 * Sets the value format specifier (C printf style)
	 * @param valueFormat the format string to set
	 */
	public void setValueFormat(final String valueFormat) {
		this.valueFormat = valueFormat;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o) {
		if (o == null) {
			return false;
		}
		if (!(o instanceof ChannelDisplayFormat)) {
			return false;
		}
		if (isASeparator) {
			return ((ChannelDisplayFormat)o).separatorString.equals(separatorString);
		} else {
			return ((ChannelDisplayFormat)o).chanId.equals(chanId);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		assert false : "hashCode not designed";
	return 42; // any arbitrary constant will do 
	}


	/**
	 * Retrieves the channel definition for this object.
	 * @return the ChannelDefinition, or null if this is s separator instance, or no definition is set
	 */
	public IChannelDefinition getChannelDef() {
		return def;
	}


	/**
	 * Returns a clone of this object.
	 * @return new ChannelDisplayItem
	 */
	public ChannelDisplayFormat copy() {
		final ChannelDisplayFormat result = new ChannelDisplayFormat();
		result.chanId = chanId;
		result.def = def;
		result.isALine = isALine;
		result.isASeparator = isASeparator;
		result.rawFormat = rawFormat;
		result.separatorString = separatorString;
		result.valueFormat = valueFormat;
		return result;
	}
	
	/**
	 * Sets the channel definition for this object.
	 * @param def the definition to set
	 */
	public void setChannelDef(final IChannelDefinition def) {
		this.def = def;
	}
}
