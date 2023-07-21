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
package jpl.gds.db.impl.types;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.types.IDbChannelMetaDataUpdater;


/**
 * Holder for channel summary data fetched from database.
 */
public class DatabaseChannelMetaData extends AbstractDatabaseItem implements IDbChannelMetaDataUpdater {
	private String channelStem;
	private String channelId;
	private String channelName;
	private final int count;


	/**
     * Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     * @param channelStem
     *            the channel's stem
     * @param channelId
     *            the channel's ID
     * @param channelName
     *            the channel's name
     */
    public DatabaseChannelMetaData(final ApplicationContext appContext, final String channelStem,
            final String channelId, final String channelName) {
		super(appContext);
		this.channelStem = channelStem;
		this.channelId = channelId;
		this.channelName = channelName;
		this.count = -1;
	}

	/**
     * Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     * @param channelStem
     *            the channel's stem
     * @param channelId
     *            the channel's ID
     * @param channelName
     *            the channel's name
     * @param count
     *            the number of channel values with this metadata
     */
	public DatabaseChannelMetaData(final ApplicationContext appContext, final String channelStem, final String channelId,
			final String channelName, final int count) {
		super(appContext);
		this.channelStem = channelStem;
		this.channelId = channelId;
		this.channelName = channelName;
		this.count = count;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getChannelStem() {
		return channelStem;
	}


	/**
     * {@inheritDoc}
     */
	@Override
    public int getCount()
    {

		return count;
	}


	/**
     * {@inheritDoc}
     */
	@Override
    public void setChannelStem(final String channelStem) {
		this.channelStem = channelStem;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getChannelId() {
		return channelId;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setChannelId(final String channelId) {
		this.channelId = channelId;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getChannelName() {
		return channelName;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void setChannelName(final String channelName) {
		this.channelName = channelName;
	}


	/*
	 * {@inheritDoc}
     *
     * The if could be simplified but then it becomes harder to be
     * sure it is OK.
     *
     * The stem and count are populated only if the corresponding option
     * is set.
     *
     * Ignores csvColumns.
     *
	 */
    @Override
	public String getCsvHeader(final List<String> csvColumns)
    {
        final StringBuilder csv       = new StringBuilder();
        final boolean       haveStem  = (channelStem != null);
        final boolean       haveCount = (count >= 0);

        if (! haveStem)
        {
            if (! haveCount)
            {
                // Channel id and name

                csv.append("channelId");

                csv.append(COMMA);

                csv.append("channelName");
            }
            else
            {
                // Channel id and name and count

                csv.append("channelId");

                csv.append(COMMA);

                csv.append("channelName");

                csv.append(COMMA);

                csv.append("count");
            }
        }
        else
        {
            // We do have stem

            if (! haveCount)
            {
                // Channel stem

                csv.append("channelStem");
            }
            else
            {
                // Channel stem and count

                csv.append("channelStem");

                csv.append(COMMA);

                csv.append("count");
            }
        }

        csv.append(NL);

		return csv.toString();
	}


	/*
	 * {@inheritDoc}
	 * 
	 * @see
	 * jpl.gds.db.api.types.IDbQueryable#getFileData(java.lang.String)
	 */
    @Override
	public Map<String, String> getFileData(final String NO_DATA) {
		// not implemented
		return null;
	}


	/*
	 * {@inheritDoc}
	 */
    @Override
	public void parseCsv(final String       csvStr,
                         final List<String> csvColumns)
    {
		// not implemented
	}


	/*
	 * {@inheritDoc}
     *
     * The if could be simplified but then it becomes harder to be
     * sure it is OK.
     *
     * The stem and count are populated only if the corresponding option
     * is set. Channel id and channel name are checked only to make sure
     * that we do not put out "null". They are probably always populated.
     *
	 */
    @Override
	public String toCsv(final List<String> csvColumns)
    {
        final StringBuilder csv       = new StringBuilder();
        final boolean       haveStem  = (channelStem != null);
        final boolean       haveCount = (count >= 0);
        final boolean       haveCid   = (channelId != null);
        final boolean       haveName  = (channelName != null);

        if (! haveStem)
        {
            if (! haveCount)
            {
                // Channel id and name

                if (haveCid)
                {
                    csv.append(channelId);
                }

                csv.append(COMMA);

                if (haveName)
                {
                    csv.append(channelName);
                }
            }
            else
            {
                // Channel id and name and count

                if (haveCid)
                {
                    csv.append(channelId);
                }

                csv.append(COMMA);

                if (haveName)
                {
                    csv.append(channelName);
                }

                csv.append(COMMA);

                csv.append(count);
            }
        }
        else
        {
            // We do have stem

            if (! haveCount)
            {
                // Channel stem

                csv.append(channelStem);
            }
            else
            {
                // Channel stem and count

                csv.append(channelStem);

                csv.append(COMMA);

                csv.append(count);
            }
        }

        csv.append(NL);

		return csv.toString();
	}


	/*
	 * {@inheritDoc}
	 * 
	 * @see
	 * jpl.gds.db.api.types.AbstractDatabaseItem#setTemplateContext(java
	 * .util.HashMap)
	 */
	@Override
	public void setTemplateContext(final Map<String,Object> map) {
		super.setTemplateContext(map);
		
		if(channelStem != null) {
			map.put("channelStem", channelStem);
		}
		
		if(channelId != null) {
			map.put("channelId", channelId);
		}
		
		if(channelName != null) {
			map.put("channelName", channelName);
		}
		
		map.put("count", count);
	}
}
