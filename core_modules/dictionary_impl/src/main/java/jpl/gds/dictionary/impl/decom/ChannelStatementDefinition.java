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
/**
 * 
 */
package jpl.gds.dictionary.impl.decom;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.decom.IChannelStatementDefinition;

/**
 * This class represents a channel statement in a generic decom map used for
 * packet decommutation.
 * 
 */
public class ChannelStatementDefinition extends Statement implements IChannelStatementDefinition {

	private String channelId;
	private IChannelDefinition cachedChannelDefinition;
	private ChannelType cachedChannelType;
	private int width;
	private int offset;

	/**
	 * Package-protected constructor. 
	 * 
	 * @param chanDef the IChannelDefinition object for the channel
	 *        this statement references; may not be null
	 * @param w
	 *            the bit width of the channel sample in the decom map
	 * @param o
	 *            the bit offset of the channel sample in the decom map
	 * 
	 */
	/* package */ ChannelStatementDefinition(final IChannelDefinition chanDef,
			final int w, final int o) {

		if (chanDef == null) {
			throw new IllegalArgumentException("Null channel definition encountered");
		}

		if (w == 0) {
			throw new IllegalArgumentException(
					"Channel statement cannot have 0 width");
		}

	    cachedChannelDefinition = chanDef;
	
		channelId = chanDef.getId();
		width = w;
		offset = o;
		cachedChannelType = cachedChannelDefinition.getChannelType();

		if (cachedChannelType == null) {
			throw new IllegalArgumentException("Channel " + channelId
					+ " definition does not specify type");
		}
	}

	@Override
    public boolean widthSpecified() {

		if (width > 0) {
			return true;
		}

		return false;
	}


	@Override
    public boolean offsetSpecified() {

		if (offset < 0) {
			return false;
		}

		return true;
	}


	@Override
    public String getChannelId() {
		return channelId;
	}
	

	@Override
    public int getWidth() {
		return width;
	}
	

    @Override
    public void setWidth(final int w) {
        width = w;
    }


	@Override
    public int getOffset() {
		return offset;
	}
	

    @Override
    public void setOffset(final int o) {
        offset = o;
    }


	@Override
    public IChannelDefinition getChannelDefinition() {
		return cachedChannelDefinition;
	}
	

    @Override
    public void setChannelDefinition(final IChannelDefinition cd) {
        if (cd == null) {
            throw new IllegalArgumentException("Null channel definition encountered");
        }
        cachedChannelDefinition = cd;
        
        /*  Reset fields taken from the definition. */
        channelId = cd.getId();
        cachedChannelType = cd.getChannelType();
    }


	@Override
    public ChannelType getChannelType() {
		return cachedChannelType;
	}

	@Override
	public String getFormat() {
		return cachedChannelDefinition.getDnFormat();
	}

	@Override
	public String getName() {
		return cachedChannelDefinition.getName();
	}

	@Override
	public int getBitOffset() {
		return offset;
	}

	@Override
	public String getDescription() {
		return cachedChannelDefinition.getDescription();
	}
	
}
