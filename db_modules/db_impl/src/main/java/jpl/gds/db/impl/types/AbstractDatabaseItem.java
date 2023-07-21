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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.db.api.types.IDbQueryable;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.TimeProperties;


/**
 * Top-level class for objects queried from the database. Holds basic session
 * information.
 *
 * NB: We need the record byte length even when we do not have a body. We do
 * not want to make a dummy body just to hold the length. So we split the two.
 * If there is a body, the length field always matches the length of the body
 * field. If there is no body, the body field is always zero-length.
 *
 */
public abstract class AbstractDatabaseItem extends Object implements IDbQueryable, IDbRecord
{
    /** MPCS-6808 Provide for subclasses */
    /** THIS LOGGER MUST BE STATIC FOR PERFORMANCE REASONS, DO NOT CHANGE IT! */
    protected static final Tracer      log               = TraceManager.getTracer(Loggers.DATABASE);


    /** MPCS-6808 Provide for subclasses */
    protected static final String DQ    = "\"";

    /** MPCS-6808 Provide for subclasses */
    protected static final String SQ    = "'";

    /** MPCS-6808 Provide for subclasses */
    protected static final String COMMA = ",";

    /** MPCS-6808 Provide for subclasses */
    protected static final String NL = "\n";

    /** MPCS-6808 Provide for subclasses */
    protected static final String CSV_COL_SEP = DQ + COMMA + DQ;

    /** MPCS-6808 Provide for subclasses */
    protected static final String CSV_COL_TRL = DQ + NL;

    private static final byte[] ZERO_BODY = new byte[0];

    /** Display SOL times (such as LST) if this is true */
    /** MPCS-10408: Made SOL flag static so we only lookup once for performance boost */
    protected static final boolean     useSolTime        = TimeProperties.getInstance().usesLst();

	/**
	 * The session ID associated with this value
	 */
	protected Long sessionId;

    /**
     * The context ID associated with this value
     * MPCS-10119 - Added field
     */
    protected Long contextId;

    /** Session fragment */
    private SessionFragmentHolder sessionFragment;

	/**
	 * The session host associated with this value.
	 */
	protected String sessionHost;
	
    /** Session host id */
	protected Integer sessionHostId;

    /**
     * The context host associated with this value.
     */
    protected String contextHost;

    /** Session context host id */
    protected Integer contextHostId;

    /** Record offset */
	protected Long recordOffset;
	
    /** Record bytes MPCS-5189 */
	private byte[] recordBytes = ZERO_BODY;

    /** Record bytes MPCS-5189 */
	private int recordBytesLength = 0;

    /** Header bytes */
    private HeaderHolder header = HeaderHolder.NULL_HOLDER;

    /** Trailer bytes */
    private TrailerHolder trailer = TrailerHolder.NULL_HOLDER;
    
    /** Session DSS ID - MPCS-6349 */
    protected int sessionDssId = 0;
    
    /** Record DSS ID - MPCS-6349 */
    protected int recordDssId = 0;

    private Integer sessionVcid = null;
    
    /**
     * Added Application Context for Spring integration
     */
    protected final ApplicationContext appContext;
    
    /**
     * The Mission Properties Object
     */
    protected final MissionProperties missionProperties;

    /**
     * Constructor.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public AbstractDatabaseItem(final ApplicationContext appContext) {
        this(appContext, null, SessionFragmentHolder.MINIMUM, null);
	}

    /**
     * Constructor.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param sessionId
     *            Session id
     * @param sessionHost
     *            Session host
     */
    public AbstractDatabaseItem(final ApplicationContext appContext, final Long sessionId, final String sessionHost) {
        this(appContext, sessionId, null, sessionHost);
    }
	

    /**
     * Constructor.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param sessionId
     *            Session id
     * @param sessionHost
     *            Session host
     * @param fragment
     *            Session fragment
     */
	public AbstractDatabaseItem(final ApplicationContext appContext,
								final Long                  sessionId,
                                final SessionFragmentHolder fragment,
                                final String                sessionHost)
	{
        this.appContext = appContext;
        this.missionProperties = appContext.getBean(MissionProperties.class);
        this.sessionId = sessionId;
        this.sessionFragment = (fragment != null) ? fragment : SessionFragmentHolder.MINIMUM;
        this.sessionHost = sessionHost;

        this.recordOffset = 0L;

        this.recordBytes = ZERO_BODY;
        this.recordBytesLength = 0;

        log.setAppContext(appContext);
	}
	


	@Override
    public Long getSessionId() {
		return this.sessionId;
	}


    @Override
    public Long getContextId() {
        return contextId;
    }

	@Override
    public SessionFragmentHolder getSessionFragment() {
		return sessionFragment;
	}



	@Override
    public String getSessionHost() {
		return this.sessionHost;
	}

    @Override
    public String getContextHost() {
        return this.contextHost;
    }

	@Override
    public Long getRecordOffset() {
		return(this.recordOffset);
	}


	@Override
	@SuppressWarnings("EI_EXPOSE_REP")
    public byte[] getRecordBytes() {
		return(this.recordBytes);
	}


	/**
     * Set session id.
     *
     * @param sessionId Session id
     */
	@Override
    public void setSessionId(final Long sessionId) {
		this.sessionId = sessionId;
	}

    @Override
    public void setContextId(final Long contextId) {
        this.contextId = contextId;
    }

    /**
     * Set session fragment.
     *
     * @param fragment Session fragment
     */
	@Override
    public void setSessionFragment(final SessionFragmentHolder fragment) {
        this.sessionFragment = ((fragment != null) ? fragment : SessionFragmentHolder.MINIMUM);
	}


    /**
     * Set session host
     *
     * @param sessionHost Session host
     */
	@Override
    public void setSessionHost(final String sessionHost) {
		this.sessionHost = sessionHost;
	}

    /**
     * Set context host
     *
     * @param contextHost Context host
     */
    @Override
    public void setContextHost(final String contextHost) {
        this.contextHost = contextHost;
    }

    /**
     * Set context host id.
     *
     * @param contextHostId Context host id
     */
    @Override
    public void setContextHostId(Integer contextHostId) {
        this.contextHostId = contextHostId;
    }

    @Override
    public Integer getSessionHostId() {
	    return this.sessionHostId;
	}

    @Override
    public Integer getContextHostId() {
        return this.contextHostId;
    }

    /**
     * Set session host id
     *
     * @param sessionHostId Session host id
     */
    @Override
    public void setSessionHostId(final Integer sessionHostId) {
	    this.sessionHostId = sessionHostId;
	}


    @Override
    public Integer getSessionDssId() {
        return sessionDssId;
    }


    /**
     * Set DSS id from Session.
     *
     * @param dssId DSS id
     */
    @Override
    public void setSessionDssId(final int dssId) {
        sessionDssId = dssId;
    }

    /* 
     * BEGIN: MPCS-6349 : DSS ID not set properly
     * Removed field dssId from all subclasses. Updated this class with 
     * protected fields sessionDssId and recordDssId with get/set 
     * methods for both.
     */
    @Override
    public int getRecordDssId() {
        return recordDssId;
    }


    /**
     * Set DSS id from Record.
     *
     * @param dssId DSS id
     */
    @Override
    public void setRecordDssId(final int dssId) {
        recordDssId = dssId;
    }
    /*
     * END: MPCS-6349 : DSS ID not set properly
     */
    
    @Override
    public Integer getSessionVcid()
    {
        return sessionVcid;
    }


    /**
     * Set VCID from Session.
     *
     * @param vcid VCID from Session
     */
    public void setSessionVcid(final Integer vcid) {
        sessionVcid = vcid;
    }


    /**
     * Set record bytes
     *
     * @param bytes Record bytes
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setRecordBytes(final byte[] bytes) {

        if (bytes != null)
        {
            this.recordBytes       = bytes;
            this.recordBytesLength = bytes.length;
        }
        else
        {
            this.recordBytes       = ZERO_BODY;
            this.recordBytesLength = 0;
        }
	}


    /**
     * Set record bytes length. This is used to set the length when we do
     * not have a body. 
     *
     * @param length Record bytes length
     */
    @Override
    public void setRecordBytesLength(final int length) {

        recordBytes       = ZERO_BODY;
        recordBytesLength = length;
    }


    @Override
    public HeaderHolder getRawHeader() {
        return header;
    }


    /**
     * Setter for header holder.
     *
     * @param hdr Header holder
     */
    public void setRawHeader(final HeaderHolder hdr) {
        header = HeaderHolder.getSafeHolder(hdr);
    }


    @Override
    public TrailerHolder getRawTrailer() {
        return trailer;
    }


    /**
     * Setter for trailer holder.
     *
     * @param hdr Trailer holder
     */
    public void setRawTrailer(final TrailerHolder hdr) {
        trailer = TrailerHolder.getSafeHolder(hdr);
    }

	@Override
    public int getRecordLength() {
		return this.recordBytesLength;
	}


    /**
     * Set record offset.
     *
     * @param offset Record offset
     */
	@Override
    public void setRecordOffset(final Long offset) {
		this.recordOffset = offset;
	}


    /**
     * Add Session fields to map for access by templates.
     *
     * @param map Map to update
     */
	@Override
    public void setTemplateContext(final Map<String, Object> map) {
        final Long zero = 0L;
        final Long tsi  = (this.sessionId != null) ? this.sessionId : zero;

        map.put("testSessionId", tsi); // deprecated for R8
		map.put("sessionId",     tsi);
		map.put("testNumber",    tsi); // deprecated for R8
        map.put("testKey",       tsi); // deprecated for R8
        map.put("sessionNumber", tsi); // deprecated for R8
        map.put("sessionKey",    tsi); // deprecated for R8

        map.put("contextId", this.contextId != null ? this.contextId : zero);

        map.put("sessionFragment", sessionFragment.getValue());
        
		map.put("testSessionHost", this.sessionHost); // deprecated for R8
		map.put("sessionHost",     this.sessionHost);
        map.put("contextHost",     this.sessionHost);
		map.put("host",            this.sessionHost); // deprecated for R8
        map.put("testHost",        this.sessionHost); // deprecated for R8
        
        map.put("fileByteOffset",
                (this.recordOffset != null) ? this.recordOffset : zero);

        map.put("length", getRecordLength());

        map.put("rawHeaderLength", header.getLength());

        map.put("rawTrailerLength", trailer.getLength());
        
        // MPCS-6349 : DSS ID not set properly
        map.put("sessionDssId", sessionDssId);
        
        final String tvcid = getTransformedVcid();

        if (! tvcid.isEmpty())
        {
            map.put(appContext.getBean(MissionProperties.class).getVcidColumnName(), tvcid);
        }
	}



	@Override
    public String getPartialSessionCsvHeader() {
		return "";
    }


    @Override
    public String getTransformedVcid() {
        if (sessionVcid == null)
        {
            return "";
        }

        if (appContext.getBean(MissionProperties.class).shouldMapQueryOutputVcid())
        {
            return StringUtil.safeTrim(
            		appContext.getBean(MissionProperties.class).mapDownlinkVcidToName(sessionVcid));
        }

        return sessionVcid.toString();
    }


    @Override
    public int getTransformedStringId(final String str) {
        return (appContext.getBean(MissionProperties.class).mapNameToDownlinkVcid(str));
    }
	


    @Override
    public String getCsvHeader(final List<String> csvColumns) {
        final StringBuilder sb = new StringBuilder();

        sb.append("recordType");

        for (final String cce : csvColumns)
        {
            sb.append(COMMA).append(cce);
        }

        sb.append(NL);

        return sb.toString();
    }

    /**
     * Put non-empty value to map.
     *
     * @param map   Map
     * @param key   Key
     * @param value Value
     */
    protected static void put(final Map<String, Object> map, final String key, final Object value) {
        if (value != null) {
            final String s = value.toString().trim();

            if (! s.isEmpty()) {
                map.put(key, s);
            }
        }
    }
}
