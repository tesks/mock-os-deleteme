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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.connection.ConnectionKey;
import jpl.gds.common.config.connection.IDatabaseConnectionSupport;
import jpl.gds.common.config.connection.IFileConnectionSupport;
import jpl.gds.common.config.connection.INetworkConnection;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.db.api.types.IDbSessionUpdater;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;


/**
 * Holder object for database Session rows.
 */
public class DatabaseSession extends AbstractDatabaseItem implements IDbSessionUpdater
{
    /** MPCS-6808 String constants pushed up */
    private static final String CSV_COL_HDR = DQ + "Session";

    private static final List<String> csvSkip =
        new ArrayList<String>(0);

    // Note that these are never primitives:

    private String name = null;
    private String type = null;
    private String description = null;
    private String fullName = null;
    private String user = null;
    private TelemetryConnectionType connectionType = null;
    private UplinkConnectionType uplinkConnectionType = null;
    private String outputDirectory = null;
    private String fswDictionaryDir = null;
    private String sseDictionaryDir = null;
    private String sseVersion = null;
    private String fswVersion = null;
    private VenueType venueType = null;
    private String testbedName = null;
    private TelemetryInputType rawInputType = null;
    private IAccurateDateTime startTime = null;
    private IAccurateDateTime endTime = null;
    private Integer spacecraftId = null;

    // MPCS-4819 null => NOT_APPLICABLE
    private DownlinkStreamType downlinkStreamId =
        DownlinkStreamType.NOT_APPLICABLE;

    private String mpcsVersion = null;
    private String fswDownlinkHost = null;
    private String fswUplinkHost = null;
    private Integer fswUplinkPort = null;
    private Integer fswDownlinkPort = null;
    private String sseHost = null;
    private Integer sseUplinkPort = null;
    private Integer sseDownlinkPort = null;
    private String inputFile = null;
    private String topic = null;

    // V4 additions

    private Boolean outputDirectoryOverride = null;
    private String  subtopic                = null;
    /* 
     * MPCS-6349 : DSS ID not set properly
     * Removed field station. Parent class has been updated with 
     * protected fields sessionDssId and recordDssId with get/set 
     * methods for both.
     */
    private Long    vcid                    = null;
    private Boolean fswDownlinkFlag         = null;
    private Boolean sseDownlinkFlag         = null;
    private Boolean uplinkFlag              = null;
    private Long    databaseSessionId       = null;
    private String  databaseHost            = null;


    /**
     * Creates an instance of DatabaseSessionConfiguration.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public DatabaseSession(final ApplicationContext appContext)
    {
    	super(appContext);
    	
        this.name = null;
        this.type = null;
        this.description = null;
        this.fullName = null;
        this.user = null;
        this.connectionType = null;
        this.uplinkConnectionType = null;
        this.outputDirectory = null;
        this.fswDictionaryDir = null;
        this.sseDictionaryDir = null;
        this.sseVersion = null;
        this.fswVersion = null;
        this.venueType = null;
        this.testbedName = null;
        this.rawInputType = null;
        this.startTime = null;
        this.endTime = null;
        this.spacecraftId = null;

        // MPCS-4819 null => NOT_APPLICABLE; use function to set
        setDownlinkStreamId(DownlinkStreamType.NOT_APPLICABLE);

        this.mpcsVersion = null;
        this.fswDownlinkHost = null;
        this.fswUplinkHost = null;
        this.fswUplinkPort = null;
        this.fswDownlinkPort = null;
        this.sseHost = null;
        this.sseUplinkPort = null;
        this.sseDownlinkPort = null;
        this.inputFile = null;
        this.topic = null;

        this.outputDirectoryOverride = null;
        this.subtopic                = null;
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed field station. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */
        this.vcid                    = null;
        this.fswDownlinkFlag         = null;
        this.sseDownlinkFlag         = null;
        this.uplinkFlag              = null;
        this.databaseSessionId       = null;
        this.databaseHost            = null;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setName(java.lang.String)
     */
    @Override
    public void setName(final String name)
    {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setType(java.lang.String)
     */
    @Override
    public void setType(final String type)
    {
        this.type = type;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setDescription(java.lang.String)
     */
    @Override
    public void setDescription(final String description)
    {
        this.description = description;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setFullName(java.lang.String)
     */
    @Override
    public void setFullName(final String fullName)
    {
        this.fullName = fullName;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setUser(java.lang.String)
     */
    @Override
    public void setUser(final String user)
    {
        this.user = user;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setConnectionType(jpl.gds.common.config.types.TelemetryConnectionType)
     */
    @Override
    public void setConnectionType(final TelemetryConnectionType ct)
    {
        this.connectionType = ct;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setUplinkConnectionType(jpl.gds.session.config.UplinkConnectionType)
     */
    @Override
    public void setUplinkConnectionType(final UplinkConnectionType ct)
    {
        this.uplinkConnectionType = ct;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setOutputDirectory(java.lang.String)
     */
    @Override
    public void setOutputDirectory(final String outputDirectory)
    {
        this.outputDirectory = outputDirectory;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setFswDictionaryDir(java.lang.String)
     */
    @Override
    public void setFswDictionaryDir(final String fswDictionaryDir)
    {
        this.fswDictionaryDir = fswDictionaryDir;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setSseDictionaryDir(java.lang.String)
     */
    @Override
    public void setSseDictionaryDir(final String sseDictionaryDir)
    {
        this.sseDictionaryDir = sseDictionaryDir;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setSseVersion(java.lang.String)
     */
    @Override
    public void setSseVersion(final String sseVersion)
    {
        this.sseVersion = sseVersion;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setFswVersion(java.lang.String)
     */
    @Override
    public void setFswVersion(final String fswVersion)
    {
        this.fswVersion = fswVersion;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setVenueType(jpl.gds.common.config.types.VenueType)
     */
    @Override
    public void setVenueType(final VenueType venueType)
    {
        this.venueType = venueType;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setTestbedName(java.lang.String)
     */
    @Override
    public void setTestbedName(final String testbedName)
    {
        this.testbedName = testbedName;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setRawInputType(jpl.gds.common.config.types.TelemetryInputType)
     */
    @Override
    public void setRawInputType(final TelemetryInputType rawInputType)
    {
        this.rawInputType = rawInputType;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setStartTime(java.util.Date)
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setStartTime(final IAccurateDateTime startTime)
    {
        this.startTime = startTime;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setEndTime(java.util.Date)
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP2")
    public void setEndTime(final IAccurateDateTime endTime)
    {
        this.endTime = endTime;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setSpacecraftId(java.lang.Integer)
     */
    @Override
    public void setSpacecraftId(final Integer spacecraftId)
    {
        this.spacecraftId = spacecraftId;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setDownlinkStreamId(jpl.gds.common.config.types.DownlinkStreamType)
     */
    @Override
    public void setDownlinkStreamId(
                    final DownlinkStreamType downlinkStreamId)
    {
        // MPCS-4819  Check for null

        this.downlinkStreamId =
            ((downlinkStreamId != null)
                 ? downlinkStreamId
                 : DownlinkStreamType.NOT_APPLICABLE);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setMpcsVersion(java.lang.String)
     */
    @Override
    public void setMpcsVersion(final String mpcsVersion)
    {
        this.mpcsVersion = mpcsVersion;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setFswDownlinkHost(java.lang.String)
     */
    @Override
    public void setFswDownlinkHost(final String fswDownlinkHost)
    {
        this.fswDownlinkHost = fswDownlinkHost;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setFswUplinkHost(java.lang.String)
     */
    @Override
    public void setFswUplinkHost(final String fswUplinkHost)
    {
        this.fswUplinkHost = fswUplinkHost;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setFswUplinkPort(java.lang.Integer)
     */
    @Override
    public void setFswUplinkPort(final Integer fswUplinkPort)
    {
        this.fswUplinkPort = fswUplinkPort;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setFswDownlinkPort(java.lang.Integer)
     */
    @Override
    public void setFswDownlinkPort(final Integer fswDownlinkPort)
    {
        this.fswDownlinkPort = fswDownlinkPort;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setSseHost(java.lang.String)
     */
    @Override
    public void setSseHost(final String sseHost)
    {
        this.sseHost = sseHost;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setSseUplinkPort(java.lang.Integer)
     */
    @Override
    public void setSseUplinkPort(final Integer sseUplinkPort)
    {
        this.sseUplinkPort = sseUplinkPort;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setSseDownlinkPort(java.lang.Integer)
     */
    @Override
    public void setSseDownlinkPort(final Integer sseDownlinkPort)
    {
        this.sseDownlinkPort = sseDownlinkPort;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getName()
     */
    @Override
    public String getName()
    {
        return (this.name);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getType()
     */
    @Override
    public String getType()
    {
        return (this.type);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getDescription()
     */
    @Override
    public String getDescription()
    {
        return (this.description);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getFullName()
     */
    @Override
    public String getFullName()
    {
        return (this.fullName);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getUser()
     */
    @Override
    public String getUser()
    {
        return (this.user);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getConnectionType()
     */
    @Override
    public TelemetryConnectionType getConnectionType()
    {
        return this.connectionType;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getUplinkConnectionType()
     */
    @Override
    public UplinkConnectionType getUplinkConnectionType()
    {
        return this.uplinkConnectionType;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getOutputDirectory()
     */
    @Override
    public String getOutputDirectory()
    {
        return (this.outputDirectory);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getFswDictionaryDir()
     */
    @Override
    public String getFswDictionaryDir()
    {
        return (this.fswDictionaryDir);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getSseDictionaryDir()
     */
    @Override
    public String getSseDictionaryDir()
    {
        return (this.sseDictionaryDir);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getSseVersion()
     */
    @Override
    public String getSseVersion()
    {
        return (this.sseVersion);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getFswVersion()
     */
    @Override
    public String getFswVersion()
    {
        return (this.fswVersion);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getVenueType()
     */
    @Override
    public VenueType getVenueType()
    {
        return (this.venueType);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getTestbedName()
     */
    @Override
    public String getTestbedName()
    {
        return (this.testbedName);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getRawInputType()
     */
    @Override
    public TelemetryInputType getRawInputType()
    {
        return (this.rawInputType);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getStartTime()
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getStartTime()
    {
        return (this.startTime);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getEndTime()
     */
    @Override
    @SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getEndTime()
    {
        return (this.endTime);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getSpacecraftId()
     */
    @Override
    public Integer getSpacecraftId()
    {
        return (this.spacecraftId);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getDownlinkStreamId()
     */
    @Override
    public DownlinkStreamType getDownlinkStreamId()
    {
        return this.downlinkStreamId;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getMpcsVersion()
     */
    @Override
    public String getMpcsVersion()
    {
        return (this.mpcsVersion);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getFswDownlinkHost()
     */
    @Override
    public String getFswDownlinkHost()
    {
        return (this.fswDownlinkHost);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getFswUplinkHost()
     */
    @Override
    public String getFswUplinkHost()
    {
        return (this.fswUplinkHost);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getFswUplinkPort()
     */
    @Override
    public Integer getFswUplinkPort()
    {
        return (this.fswUplinkPort);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getFswDownlinkPort()
     */
    @Override
    public Integer getFswDownlinkPort()
    {
        return (this.fswDownlinkPort);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getSseHost()
     */
    @Override
    public String getSseHost()
    {
        return (this.sseHost);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getSseUplinkPort()
     */
    @Override
    public Integer getSseUplinkPort()
    {
        return (this.sseUplinkPort);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getSseDownlinkPort()
     */
    @Override
    public Integer getSseDownlinkPort()
    {
        return (this.sseDownlinkPort);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getInputFile()
     */
	@Override
    public String getInputFile() {
		return inputFile;
	}


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setInputFile(java.lang.String)
     */
	@Override
    public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getTopic()
     */
	@Override
    public String getTopic()
    {
		return topic;
	}


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setTopic(java.lang.String)
     */
	@Override
    public void setTopic(final String topic)
    {
        this.topic = StringUtil.emptyAsNull(topic);
	}


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getOutputDirectoryOverride()
     */
    @Override
    public Boolean getOutputDirectoryOverride()
    {
        return outputDirectoryOverride;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setOutputDirectoryOverride(java.lang.Boolean)
     */
    @Override
    public void setOutputDirectoryOverride(final Boolean state)
    {
        outputDirectoryOverride = state;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getSubtopic()
     */
    @Override
    public String getSubtopic()
    {
        return subtopic;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setSubtopic(java.lang.String)
     */
    @Override
    public void setSubtopic(final String state)
    {
        subtopic = state;
    }

    
    /*
     * MPCS-6034 : MTAK get_eha after downlink summary
     * MPCS-6035 : MTAK doesn't seem to get the last value of the MON channel
     * 
     * Parent class AbstractDatabaseItem contains getSessionDssId and 
     * setSessionDssId. Removed the following duplicate methods to prevent
     * the same issue from occurring again and replaced all occurrences of 
     * getStation, setStation with getSessionDssId and setSessionDssId 
     * respectively.
     * - public Integer getStation()
     * - public void setStation(final Integer state)
     */


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getVcid()
     */
    @Override
    public Long getVcid()
    {
        return vcid;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setVcid(java.lang.Long)
     */
    @Override
    public void setVcid(final Long state)
    {
        vcid = state;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getFswDownlinkFlag()
     */
    @Override
    public Boolean getFswDownlinkFlag()
    {
        return fswDownlinkFlag;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setFswDownlinkFlag(java.lang.Boolean)
     */
    @Override
    public void setFswDownlinkFlag(final Boolean state)
    {
        fswDownlinkFlag = state;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getSseDownlinkFlag()
     */
    @Override
    public Boolean getSseDownlinkFlag()
    {
        return sseDownlinkFlag;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setSseDownlinkFlag(java.lang.Boolean)
     */
    @Override
    public void setSseDownlinkFlag(final Boolean state)
    {
        sseDownlinkFlag = state;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getUplinkFlag()
     */
    @Override
    public Boolean getUplinkFlag()
    {
        return uplinkFlag;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setUplinkFlag(java.lang.Boolean)
     */
    @Override
    public void setUplinkFlag(final Boolean state)
    {
        uplinkFlag = state;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getDatabaseSessionId()
     */
    @Override
    public Long getDatabaseSessionId()
    {
        return databaseSessionId;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setDatabaseSessionId(java.lang.Long)
     */
    @Override
    public void setDatabaseSessionId(final Long state)
    {
        databaseSessionId = state;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getDatabaseHost()
     */
    @Override
    public String getDatabaseHost()
    {
        return databaseHost;
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setDatabaseHost(java.lang.String)
     */
    @Override
    public void setDatabaseHost(final String state)
    {
        databaseHost = state;
    }
	

    /** MPCS-6806 Remove private append methods */


    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#toCsv(java.util.List)
     */
    @Override
	public String toCsv(final List<String> csvColumns)
	{
    	/* 
    	 * MPCS-6349 : DSS ID not set properly
    	 * Removed field station. Parent class has been updated with 
    	 * protected fields sessionDssId and recordDssId with get/set 
    	 * methods for both.
    	 */

		final StringBuilder csv = new StringBuilder(1024);
		final DateFormat    df  = TimeUtility.getFormatterFromPool();

		csv.append(CSV_COL_HDR);

        for (final String cce : csvColumns)
        {
            final String upcce = cce.toUpperCase();

            csv.append(CSV_COL_SEP);

            switch (upcce)
            {
                case "SESSIONID":
                    if (sessionId != null)
                    {
                        csv.append(sessionId);
                    }
                    break;

                case "NAME":
                    if (name != null)
                    {
                        csv.append(name);
                    }
                    break;

                case "TYPE":
                    if (type != null)
                    {
                        csv.append(type);
                    }
                    break;

                case "DESCRIPTION":
                    if (description != null)
                    {
                        csv.append(description);
                    }
                    break;

                case "USER":
                    if (user != null)
                    {
                        csv.append(user);
                    }
                    break;

                case "SESSIONHOST":
                    if (sessionHost != null)
                    {
                        csv.append(sessionHost);
                    }
                    break;

                case "CONNECTIONTYPE":
                    if (connectionType != null)
                    {
                        csv.append(connectionType);
                    }
                    break;

                case "UPLINKCONNECTIONTYPE":
                    if (uplinkConnectionType != null)
                    {
                        csv.append(uplinkConnectionType);
                    }
                    break;

                case "STARTTIME":
                    if (startTime != null)
                    {
                        csv.append(df.format(startTime));
                    }
                    break;

                case "ENDTIME":
                    if (endTime != null)
                    {
                        csv.append(df.format(endTime));
                    }
                    break;

                case "OUTPUTDIRECTORY":
                    if (outputDirectory != null)
                    {
                        csv.append(outputDirectory);
                    }
                    break;

                case "OUTPUTDIRECTORYOVERRIDE":
                    if (outputDirectoryOverride != null)
                    {
                        csv.append(outputDirectoryOverride);
                    }
                    break;

                case "FSWVERSION":
                    if (fswVersion != null)
                    {
                        csv.append(fswVersion);
                    }
                    break;

                case "SSEVERSION":
                    if (sseVersion != null)
                    {
                        csv.append(sseVersion);
                    }
                    break;

                case "FSWDICTIONARYDIR":
                    if (fswDictionaryDir != null)
                    {
                        csv.append(fswDictionaryDir);
                    }
                    break;

                case "SSEDICTIONARYDIR":
                    if (sseDictionaryDir != null)
                    {
                        csv.append(sseDictionaryDir);
                    }
                    break;

                case "VENUE":
                    if (venueType != null)
                    {
                        csv.append(venueType);
                    }
                    break;

                case "RAWINPUTTYPE":
                    if (rawInputType != null)
                    {
                        csv.append(rawInputType);
                    }
                    break;

                case "TESTBEDNAME":
                    if (testbedName != null)
                    {
                        csv.append(testbedName);
                    }
                    break;

                case "DOWNLINKSTREAMID":
                    // MPCS-4819 Use function to get

                    final DownlinkStreamType sdsie = getDownlinkStreamId();

                    if (sdsie != null)
                    {
                        csv.append(DownlinkStreamType.convert(sdsie));
                    }
                    break;

                case "SPACECRAFTID":
                    if (spacecraftId != null)
                    {
                        csv.append(spacecraftId);
                    }
                    break;

                case "MPCSVERSION":
                    if (mpcsVersion != null)
                    {
                        csv.append(mpcsVersion);
                    }
                    break;

                case "FULLNAME":
                    if (fullName != null)
                    {
                        csv.append(fullName);
                    }
                    break;

                case "FSWDOWNLINKHOST":
                    if (fswDownlinkHost != null)
                    {
                        csv.append(fswDownlinkHost);
                    }
                    break;

                case "FSWUPLINKHOST":
                    if (fswUplinkHost != null)
                    {
                        csv.append(fswUplinkHost);
                    }
                    break;

                case "FSWDOWNLINKPORT":
                    if (fswDownlinkPort != null)
                    {
                        csv.append(fswDownlinkPort);
                    }
                    break;

                case "FSWUPLINKPORT":
                    if (fswUplinkPort != null)
                    {
                        csv.append(fswUplinkPort);
                    }
                    break;

                case "SSEHOST":
                    if (sseHost != null)
                    {
                        csv.append(sseHost);
                    }
                    break;

                case "SSEDOWNLINKPORT":
                    if (sseDownlinkPort != null)
                    {
                        csv.append(sseDownlinkPort);
                    }
                    break;

                case "SSEUPLINKPORT":
                    if (sseUplinkPort != null)
                    {
                        csv.append(sseUplinkPort);
                    }
                    break;

                case "INPUTFILE":
                    if (inputFile != null)
                    {
                        csv.append(inputFile);
                    }
                    break;

                case "TOPIC":
                    if (topic != null)
                    {
                        csv.append(topic);
                    }
                    break;

                case "SUBTOPIC":
                    if (subtopic != null)
                    {
                        csv.append(subtopic);
                    }
                    break;

                case "SESSIONDSSID":
                    csv.append(sessionDssId); // int
                    break;

                case "VCID":
                    if (vcid != null)
                    {
                        csv.append(vcid);
                    }
                    break;

                case "FSWDOWNLINKFLAG":
                    if (fswDownlinkFlag != null)
                    {
                        csv.append(fswDownlinkFlag);
                    }
                    break;

                case "SSEDOWNLINKFLAG":
                    if (sseDownlinkFlag != null)
                    {
                        csv.append(sseDownlinkFlag);
                    }
                    break;

                case "UPLINKFLAG":
                    if (uplinkFlag != null)
                    {
                        csv.append(uplinkFlag);
                    }
                    break;

                case "DATABASESESSIONID":
                    if (databaseSessionId != null)
                    {
                        csv.append(databaseSessionId);
                    }
                    break;

                case "DATABASESESSIONHOST":
                    if (databaseHost != null)
                    {
                        csv.append(databaseHost);
                    }
                    break;
                    
                //MPCS-8021 Add named VCID column to csv.
                case "VCIDNAME":
                	// MPCS-8021 - updated for better parsing
                	if(missionProperties.shouldMapQueryOutputVcid() && vcid != null){
                		csv.append(missionProperties.mapDownlinkVcidToName(this.vcid.intValue()));
                	} else {
                		csv.append("");
                	}

                	break;

                case "SESSIONFRAGMENT":
                    csv.append(getSessionFragment());
                    break;

                default:

                	//MPCS-8021  Add named VCID column to csv.
                	//Put here due to the configurable nature of the column name
                	if(missionProperties.getVcidColumnName().toUpperCase().equals(upcce))
                	{
                		if(missionProperties.shouldMapQueryOutputVcid() && vcid != null){
                			csv.append(missionProperties.mapDownlinkVcidToName(this.vcid.intValue()));
                		} else {
                			csv.append("");
                		}
                	}
                	else if (! csvSkip.contains(upcce))
                	{
                		log.warn("Column " + 
                				cce       +
                				" is not supported, skipped");

                		csvSkip.add(upcce);
                	}

                	break;
            }
        }

		csv.append(CSV_COL_TRL);

		TimeUtility.releaseFormatterToPool(df);

		return csv.toString();
	}


	/**
     *
     * MPCS-6808 Massive rewrite
	 */
    @Override
	public void parseCsv(final String              csvStr,
                         final List<String> csvColumns)
    {		
		// The following removes the start/end quotes w/ the substring
		// and splits based on ",". It leaves the trailing empty string in the case that 
		// csvStr ends with "". The empty strings serve as place holders.

        final String[] dataArray = csvStr.substring(1, csvStr.length() - 1).split(CSV_COL_SEP, -1);

        if ((csvColumns.size() + 1) != dataArray.length)
        {
            throw new IllegalArgumentException("CSV column length mismatch, received " +
                                               dataArray.length                        +
                                               " but expected "                        +
                                               (csvColumns.size() + 1));
        }

        // Clear everything we might process, in case empty column or not in list

        sessionId               = null;
        sessionHost             = null;
        name                    = null;
        type                    = null;
        description             = null;
        user                    = null;
        connectionType          = null;
        uplinkConnectionType    = null;
        startTime               = null;
        endTime                 = null;
        outputDirectory         = null;
        outputDirectoryOverride = null;
        fswVersion              = null;
        sseVersion              = null;
        fswDictionaryDir        = null;
        sseDictionaryDir        = null;
        venueType               = null;
        rawInputType            = null;
        testbedName             = null;
        spacecraftId            = null;
        mpcsVersion             = null;
        fullName                = null;
        fswDownlinkHost         = null;
        fswUplinkHost           = null;
        fswDownlinkPort         = null;
        fswUplinkPort           = null;
        sseHost                 = null;
        sseDownlinkPort         = null;
        sseUplinkPort           = null;
        inputFile               = null;
        topic                   = null;
        subtopic                = null;
        sessionDssId            = StationIdHolder.UNSPECIFIED_VALUE;
        vcid                    = null;
        fswDownlinkFlag         = null;
        sseDownlinkFlag         = null;
        uplinkFlag              = null;
        databaseSessionId       = null;
        databaseHost            = null;

        setDownlinkStreamId(DownlinkStreamType.NOT_APPLICABLE);
 
        int    next  = 1; // Skip recordType
        String token = null;

        for (final String cce : csvColumns)
        {
            token = dataArray[next].trim();

            ++next;

            if (token.isEmpty())
            {
                continue;
            }

            final String upcce = cce.toUpperCase();

            try
            {
                switch (upcce)
                {
                    case "SESSIONID":
                        sessionId = Long.valueOf(token);
                        break;

                    case "SESSIONHOST":
                        sessionHost = token;
                        break; 

                    case "NAME":
                        name = token;
                        break; 

                    case "TYPE":
                        type = token;
                        break; 

                    case "DESCRIPTION":
                        description = token;
                        break; 

                    case "USER":
                        user = token;
                        break; 

                    case "CONNECTIONTYPE":
                        connectionType = TelemetryConnectionType.valueOf(token);
                        break; 

                    case "UPLINKCONNECTIONTYPE":
                        uplinkConnectionType = UplinkConnectionType.valueOf(token);
                        break; 

                    case "STARTTIME":
                        startTime = new AccurateDateTime(token);
                        break; 

                    case "ENDTIME":
                        endTime = new AccurateDateTime(token);
                        break; 

                    case "OUTPUTDIRECTORY":
                        outputDirectory = token;
                        break; 

                    case "OUTPUTDIRECTORYOVERRIDE":
                        outputDirectoryOverride = Boolean.valueOf(token);
                        break; 

                    case "FSWVERSION":
                        fswVersion = token;
                        break; 

                    case "SSEVERSION":
                        sseVersion = token;
                        break; 

                    case "FSWDICTIONARYDIR":
                        fswDictionaryDir = token;
                        break; 

                    case "SSEDICTIONARYDIR":
                        sseDictionaryDir = token;
                        break; 

                    case "VENUE":
                        venueType = VenueType.valueOf(token);
                        break; 

                    case "RAWINPUTTYPE":
                        rawInputType = TelemetryInputType.valueOf(token);
                        break; 

                    case "TESTBEDNAME":
                        testbedName = token;
                        break; 

                    case "DOWNLINKSTREAMID":
                        // MPCS-4819 Use function to set
                        // assignStreamId may return null but setter will use
                        // not applicable

                        setDownlinkStreamId(DownlinkStreamType.convert(token));
                        break; 

                    case "SPACECRAFTID":
                        spacecraftId = Integer.valueOf(token);
                        break; 

                    case "MPCSVERSION":
                        mpcsVersion = token;
                        break; 

                    case "FULLNAME":
                        fullName = token;
                        break; 

                    case "FSWDOWNLINKHOST":
                        fswDownlinkHost = token;
                        break; 

                    case "FSWUPLINKHOST":
                        fswUplinkHost = token;
                        break; 

                    case "FSWDOWNLINKPORT":
                        fswDownlinkPort = Integer.valueOf(token);
                        break; 

                    case "FSWUPLINKPORT":
                        fswUplinkPort = Integer.valueOf(token);
                        break;

                    case "SSEHOST":
                        sseHost = token;
                        break;

                    case "SSEDOWNLINKPORT":
                        sseDownlinkPort = Integer.valueOf(token);
                        break; 

                    case "SSEUPLINKPORT":
                        sseUplinkPort = Integer.valueOf(token);
                        break;

                    case "INPUTFILE":
                        inputFile = token;
                        break;

                    case "TOPIC":
                        topic = token;
                        break;

                    case "SUBTOPIC":
                        subtopic = token;
                        break;

                    case "SESSIONDSSID":
                        /* 
                         * MPCS-6349 : DSS ID not set properly
                         * Removed dssId. Parent class has been updated with 
                         * protected fields sessionDssId and recordDssId with get/set 
                         * methods for both.
                         */

                        sessionDssId = Integer.valueOf(token);
                        break;

                    case "VCID":
                        vcid = Long.valueOf(token);
                        break;

                    case "FSWDOWNLINKFLAG":
                        fswDownlinkFlag = Boolean.valueOf(token);
                        break;

                    case "SSEDOWNLINKFLAG":
                        sseDownlinkFlag = Boolean.valueOf(token);
                        break;

                    case "UPLINKFLAG":
                        uplinkFlag = Boolean.valueOf(token);
                        break;

                    case "DATABASESESSIONID":
                        databaseSessionId = Long.valueOf(token);
                        break;

                    case "DATABASESESSIONHOST":
                        databaseHost = token;
                        break;
                        
                    //MPCS-8021 Named VCID column added.
                    case "VCIDNAME":
                    	//vcid name is mapped, not stored. do nothing
                    	break;

                    default:
                    	//MPCS-8021 Named VCID column added. Added here as well due to configurable nature of the column name
                    	if(missionProperties.getVcidColumnName().toUpperCase().equals(upcce))
                    	{
                    		//vcid name is mapped, not stored. do nothing
                    	}
                    	else if (! csvSkip.contains(upcce))
                        {
                            log.warn("Column " + 
                                     cce       +
                                     " is not supported, skipped");

                            csvSkip.add(upcce);
                        }

                        break;
                }
             }
             catch (final RuntimeException re)
             {
                 re.printStackTrace();

                 throw re;
		     }
             catch (final Exception e)
             {
                 e.printStackTrace();
             }
        }
	}

    /**
     * Put non-empty value to map.
     *
     * @param map   Map
     * @param key   Key
     * @param value Value
     * @param df    Date format
     */
    private static void put(final Map<String, Object> map,
                            final String              key,
                            final IAccurateDateTime   value,
                            final DateFormat          df)
    {
        if (value != null)
        {
            map.put(key, df.format(value));
        }
    }


    /**
     * Put non-empty value to map.
     *
     * @param map   Map
     * @param key   Key
     * @param value Value
     */
    private static void put(final Map<String, Object>         map,
                            final String                      key,
                            final DownlinkStreamType value)
    {
        if (value != null)
        {
            map.put(key, DownlinkStreamType.convert(value));
        }
    }


    /**
     * {@inheritDoc}
     */
	@Override
	public void setTemplateContext(final Map<String, Object> map)
	{
		super.setTemplateContext(map);

        final DateFormat df = TimeUtility.getFormatterFromPool();

        if (missionProperties.missionHasSse()) {
        	put(map, "hasSse",                 "true");
        }
        put(map, "mpcsVersion",            mpcsVersion);
        put(map, "testName",               name); //Deprecated for R8
        put(map, "sessionName",            name);
        put(map, "testType",               type); // Deprecated for R8
        put(map, "sessionType",            type);
        put(map, "testDescription",        description); // Deprecated for R8
        put(map, "sessionDescription",     description); // Deprecated for R8
        put(map, "user",                   user);
        put(map, "downlinkConnectionType", connectionType);
        put(map, "uplinkConnectionType",   uplinkConnectionType);
        put(map, "startTime",              startTime, df);
        put(map, "endTime",                endTime,   df);
        put(map, "fswVersion",             fswVersion);
        put(map, "sseVersion",             sseVersion);
        put(map, "fswDictionaryDir",       fswDictionaryDir);
        put(map, "sseDictionaryDir",       sseDictionaryDir);
        put(map, "outputDir",              outputDirectory);
        put(map, "outputDirOverride",      outputDirectoryOverride);

        // MPCS-4819 Use function to get
        put(map, "downlinkStreamId",       getDownlinkStreamId());

        put(map, "testbedName",            testbedName);
        put(map, "spacecraftId",           spacecraftId);
        put(map, "venueType",              venueType);
        put(map, "inputFormat",            rawInputType);
        put(map, "fswDownlinkHost",        fswDownlinkHost);
        put(map, "fswUplinkHost",          fswUplinkHost);
        put(map, "fswDownlinkPort",        fswDownlinkPort);
        put(map, "fswUplinkPort",          fswUplinkPort);
        put(map, "sseHost",                sseHost);
        put(map, "sseUplinkPort",          sseUplinkPort);
        put(map, "sseDownlinkPort",        sseDownlinkPort);
        put(map, "inputFile",              inputFile);
        put(map, "topic",                  topic);
        put(map, "fullName",               fullName);
        put(map, "subtopic",               subtopic);
        
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */
        put(map, "dssId",                  sessionDssId);
        put(map, "vcid",                   vcid);
        put(map, "fswDownlinkFlag",        fswDownlinkFlag);
        put(map, "sseDownlinkFlag",        sseDownlinkFlag);
        put(map, "uplinkFlag",             uplinkFlag);
        put(map, "databaseSessionId",      databaseSessionId);
        put(map, "databaseHost",           databaseHost);
        
        //MPCS-8021 add mapping of VCID name
        // MPCS-8021 updated for efficiency
        if (missionProperties.shouldMapQueryOutputVcid() && this.vcid != null)
        {
        	map.put(missionProperties.getVcidColumnName(),
        			missionProperties.mapDownlinkVcidToName(this.vcid.intValue()));
        }

        TimeUtility.releaseFormatterToPool(df);
    }


	/* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionProvider#getFileData(java.lang.String)
     */
	@Override
    public Map<String,String> getFileData(final String NO_DATA)
    {
		return null;
	}

//	 /* (non-Javadoc)
//     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setIntoHostConfiguration(jpl.gds.session.config.HostConfiguration)
//     */
//    @Override
//    public void setIntoHostConfiguration(ConnectionConfiguration hc) {
//        final String tFswDownlinkHost = getFswDownlinkHost();
//        if (tFswDownlinkHost != null) {
//            hc.setFswDownlinkHost(tFswDownlinkHost);
//        }
//
//        final String tFswUplinkHost = getFswUplinkHost();
//        if (tFswUplinkHost != null) {
//            hc.setFswUplinkHost(tFswUplinkHost);
//        }
//
//        final Integer tFswUplinkPort = getFswUplinkPort();
//        if (tFswUplinkPort != null) {
//            hc.setFswUplinkPort(tFswUplinkPort.intValue());
//        }
//
//        final Integer tFswDownlinkPort = getFswDownlinkPort();
//        if (tFswDownlinkPort != null) {
//            hc.setFswDownlinkPort(tFswDownlinkPort.intValue());
//        }
//
//        final String tSseHost = getSseHost();
//        if (tSseHost != null) {
//            hc.setSseHost(tSseHost);
//        }
//
//        final Integer tSseUplinkPort = getSseUplinkPort();
//        if (tSseUplinkPort != null) {
//            hc.setSseUplinkPort(tSseUplinkPort.intValue());
//        }
//
//        final Integer tSseDownlinkPort = getSseDownlinkPort();
//        if (tSseDownlinkPort != null) {
//            hc.setSseDownlinkPort(tSseDownlinkPort.intValue());
//        }
//    }
    
    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setIntoSessionIdentification(jpl.gds.session.config.SessionIdentification)
     */
    @Override
    public void setIntoContextIdentification(final IContextIdentification si) {
        
        si.setNumber(getSessionId());
        si.setFragment(getSessionFragment().getValue());
        si.setName(getName());
        si.setType(getType());
        si.setDescription(getDescription());
        si.setHost(getSessionHost());
        si.setHostId(getSessionHostId());
        si.setUser(getUser());
        si.setStartTime(getStartTime());
        si.setEndTime(getEndTime());
        si.setSpacecraftId(getSpacecraftId());
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setIntoSessionConfiguration(jpl.gds.session.config.SessionConfiguration)
     */
    @Override
    public void setIntoContextConfiguration(final IContextConfiguration sc) {
        setIntoContextIdentification(sc.getContextId());
        sc.getVenueConfiguration().setVenueType(getVenueType());
        sc.getGeneralInfo().setOutputDir(getOutputDirectory());
        setIntoDictionaryConfiguration(sc.getDictionaryConfig());
        sc.getVenueConfiguration().setDownlinkStreamId(getDownlinkStreamId());
        sc.getVenueConfiguration().setTestbedName(getTestbedName());
        sc.setConfigFile(null);
        sc.getGeneralInfo().setRootPublicationTopic(getTopic());
        sc.getFilterInformation().setDssId(getSessionDssId() == 0 ? null : getSessionDssId());
        sc.getGeneralInfo().setSubtopic(getSubtopic());

        // MPCS-8997: Note that session VCID used to be use here, but for a session object,
        // sessionVcid IS vcid. The weird thing is that the SessionConfiguration vcid is an int, but the vcid here is a long.
        // VCID is nowhere near 32 bits in CCSDS frames, so using the intValue here is ok.
        sc.getFilterInformation().setVcid(getVcid() == null ? null : getVcid().intValue());
        
        if (getConnectionType() != TelemetryConnectionType.UNKNOWN) {
            sc.getConnectionConfiguration().createFswDownlinkConnection(getConnectionType());
            sc.getConnectionConfiguration().getFswDownlinkConnection().setInputType(getRawInputType());
            
            if (sc.getConnectionConfiguration().getFswDownlinkConnection() instanceof IFileConnectionSupport) {
                ((IFileConnectionSupport)sc.getConnectionConfiguration().getFswDownlinkConnection()).setFile(getInputFile());
            }
            
            if (sc.getConnectionConfiguration().getFswDownlinkConnection() instanceof IDatabaseConnectionSupport) {
                ((IDatabaseConnectionSupport)sc.getConnectionConfiguration().getFswDownlinkConnection()).
                getDatabaseConnectionKey().addHostPattern(getDatabaseHost());
                ((IDatabaseConnectionSupport)sc.getConnectionConfiguration().getFswDownlinkConnection()).
                getDatabaseConnectionKey().addSessionKey(getDatabaseSessionId());
            }
            
            if (sc.getConnectionConfiguration().getFswDownlinkConnection() instanceof INetworkConnection) {
                if (getFswDownlinkPort() != null) {
                    ((INetworkConnection)sc.getConnectionConfiguration().getFswDownlinkConnection()).setPort(getFswDownlinkPort());
                }
                if (getFswDownlinkHost() != null) {
                    ((INetworkConnection)sc.getConnectionConfiguration().getFswDownlinkConnection()).setHost(getFswDownlinkHost());
                }
            }
        } else {
            sc.getConnectionConfiguration().remove(ConnectionKey.FSW_DOWNLINK);
        }
        
        if (getUplinkConnectionType() != UplinkConnectionType.UNKNOWN) {
            sc.getConnectionConfiguration().createFswUplinkConnection(getUplinkConnectionType());
            if (getFswUplinkPort() != null) {
                ((INetworkConnection)sc.getConnectionConfiguration().getFswUplinkConnection()).setPort(getFswUplinkPort());
            }
            
            if (getFswUplinkHost() != null) {
                ((INetworkConnection)sc.getConnectionConfiguration().getFswUplinkConnection()).setHost(getFswUplinkHost());
            }
        } else {
            sc.getConnectionConfiguration().remove(ConnectionKey.FSW_UPLINK);
        }
        
        if (sc.getConnectionConfiguration().getSseDownlinkConnection() instanceof INetworkConnection) {
        	if (getSseDownlinkPort() != null) {
        		((INetworkConnection)sc.getConnectionConfiguration().getSseDownlinkConnection()).setPort(getSseDownlinkPort());
        	}
        	if (getSseHost() != null) {
        		((INetworkConnection)sc.getConnectionConfiguration().getSseDownlinkConnection()).setHost(getSseHost());
        	}
        }
        
        
        if (sc.getConnectionConfiguration().getSseUplinkConnection() != null) {
        	if (getSseUplinkPort() != null) {

        		((INetworkConnection)sc.getConnectionConfiguration().getSseUplinkConnection()).setPort(getSseUplinkPort());
        	}
        	if(getSseHost() != null) {

        		((INetworkConnection)sc.getConnectionConfiguration().getSseUplinkConnection()).setHost(getSseHost());
        	}
        }
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.db.api.types.IDatabaseSessionUpdater#setIntoDictionaryConfiguration(jpl.gds.dictionary.impl.impl.api.config.DictionaryConfiguration)
     */
    @Override
    public void setIntoDictionaryConfiguration(final DictionaryProperties dc) {
        dc.setFswVersion(getFswVersion());
        dc.setFswDictionaryDir(getFswDictionaryDir());
        dc.setSseVersion(getSseVersion());
        dc.setSseDictionaryDir(getSseDictionaryDir());
    }   

}
