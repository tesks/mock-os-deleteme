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
package jpl.gds.product.utilities.file;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import jpl.gds.product.api.file.IProductMetadata;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.ProductStatusType;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DataValidityTime;
import jpl.gds.shared.time.FastDateFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ICoarseFineTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.SclkFmt.DvtFormatter;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.stax.StaxSerializable;
import jpl.gds.shared.xml.stax.StaxStreamWriterFactory;

/**
 * This class is responsible for representing metadata which is relevant to data
 * products for all missions. It is extended by mission-specific metadata
 * classes. This class is used internally to represent the metadata for both 
 * entire data products and for individual data product parts, which generally require
 * a subset of the total product metadata.
 * <br>
 * For products, the SCLK, SCET, and LST will be the values for one of the
 * parts (generally the last received part). 
 * 
 *
 * TODO This class is also used for storing output records for database queries.
 * It should not be. A database object should be created for that and the members
 * that are included here only for that purpose should be removed.
 */
public abstract class AbstractProductMetadata implements IProductMetadata
{
    protected static final List<String> csvSkip =
        new ArrayList<String>(0);

    /** Double quote constant */
    protected static final String DQ          = "\"";
    /** comma constant */
    protected static final String COMMA       = ",";
    /** new line constant */
    protected static final String NL          = "\n";
    /** CSV column header */
    protected static final String CSV_COL_HDR = DQ + "Product";
    /** CSV Column separator */
    protected static final String CSV_COL_SEP = DQ + COMMA + DQ;
    /** CSV line terminator */
    protected static final String CSV_COL_TRL = DQ + NL;

    /** Display SOL times (such as LST) if this is true */
    protected final boolean useSolTime;
    
    /** SCLK formatter */
    protected final SclkFormatter sclkFmt;
    /** DVT SCLK formatter */
    protected final DvtFormatter dvtSclkFmt;
    
    /**
     * Trace logger instance shared with all instances.
     * 01/28/2015:: Created product specific tracers. This added an unexpected
     * debug message, so I restored the original default tracer for this case.
     * Made Tracer static for performance
     */
    protected static final Tracer                parseLogger           = TraceManager.getTracer(Loggers.TLM_PRODUCT);


    /**
     * Dictionary directory read from product EMD File.
     */
    protected String emdDictionaryDir;
    /**
     * Dictionary version read from product EMD File.
     */
    protected String emdDictionaryVersion;
 
    /**
     * Sub-directory in which products are stored.
     */
    protected String storageDirectory;

    /**
     * Version of the data product definition that goes with this data product.
     */
    protected int xmlVersion;

    /**
     * Time the record was created
     */
    private IAccurateDateTime rct = null;

    /**
     * Time the data product gets created on the ground.
     */
    protected IAccurateDateTime productCreationTime;

    /**
     * ID of the ground session that generated the data product.
     */
    protected long sessionId;

    /** Session fragment */
    private SessionFragmentHolder sessionFragment = SessionFragmentHolder.MINIMUM;

    /**
     * Host of the ground session that generated the data product.
     */
    protected String sessionHost;

    /**
     * Host ID
     */
    protected Integer sessionhostId = 0;

    /**
     * Application Process ID of the product or part.
     */
    protected int apid;
    /**
     * SCET of the product or part.
     */
    protected IAccurateDateTime scet;
    /**
     * LST of the product or part.
     */
    protected ILocalSolarTime sol = null;
    /** 
     * ERT of the product or part.
     */
    protected IAccurateDateTime ert;
    /**
     * SCLK of the product or part.
     */
    protected ICoarseFineTime sclk;
    /**
     * Flight command sequence ID associated with the data product.
     */
    protected int seqId;
    /**
     * Flight command sequence version associated with the data product.
     */
    protected int seqVersion;
    /**
     * Flight command number associated with the data product.
     */
    protected int commandNum;
    /**
     * Coarse (seconds) portion of the data validity time (DVT) of the data product.
     */
    protected long dvtCoarse;
    /**
     * Fine (sub-seconds) portion of the data validity time (DVT) of the data product.
     */
    protected long dvtFine;
    /**
     * Total number of data parts in the data product.
     */
    protected int totalParts;
    /**
     * Complete path to the data product data file.
     */
    protected String fullPath;
    /**
     * Flag indicating the product has partial receipt status.
     */
    protected boolean partial;

    /**
     * Virtual channel ID of the channel on which the product or part
     * was received.
     */
    protected Integer vcid;

    /**
     * Product type text/APID name for the product or part.
     */
    protected String productType = UNKNOWN_PRODUCT_TYPE;
    /**
     * Ground generation status of the data product.
     */
    protected ProductStatusType groundStatus = ProductStatusType.UNKNOWN;
    /**
     * Ground version identifier for the data product.
     */
    protected String productVersion;

    /**
     * Flight version identifier for the software onboard.
     */
    protected Long fswVersion = 0L;
    /**
     * Name for the dictionary being used.
     */
    protected String fswDictionary;
    /**
     * Flight version identifier for the dictionary being used.
     */
    protected String fswDictionaryVersion;

    /**
     * List of product parts. Note that this is rarely populated and is not used by the 
     * product builder. It is only used during product decommutation.
     */
    protected final List<IProductPartProvider> parts = new ArrayList<IProductPartProvider>();

    private String sequenceCategory = null;
    private Long   sequenceNumber   = null;
    private Integer sessionDssId = null;
    private Integer sessionVcid = null;
    private int scid;
    private long actualFileSize;
    private long actualChecksum;
    
    /** Expected product checksum */
	protected long checksum;
	/** Expected product size */
	protected long fileSize;
    
    /**
     * Flag indicating that sequence dictionary error has been reported.
     */
    protected static boolean seqDictionaryReported = false;
    

    /**
     * Shared instance of the empty string for faster serialization.
     */
    protected static final String emptyStr = "";
    
    /**
     * Current application context.
     */
    protected final ApplicationContext appContext;
    /** 
     * Mission configuration properties 
     */
    protected final MissionProperties missionProperties;
     /** High volume product builder object factory */
    protected final IProductBuilderObjectFactory partFactory;
    /** SSE context flag */
    protected final SseContextFlag               sseFlag;

    protected boolean isCompressed;

    /**
     * Creates an instance of AbstractProductMetadata.
     * @param appContext the current application context
     */
    public AbstractProductMetadata(final ApplicationContext appContext) {
    	this.appContext = appContext;
    	final TimeProperties tc = TimeProperties.getInstance();
    	useSolTime = tc.usesLst();
    	sclkFmt = tc.getSclkFormatter();
    	dvtSclkFmt = tc.getDvtFormatter();
    	this.storageDirectory = appContext
    			.getBean(IProductPropertiesProvider.class)
    			.getStorageSubdir();
    	this.missionProperties = appContext.getBean(MissionProperties.class);
    	this.partFactory = appContext.getBean(IProductBuilderObjectFactory.class);
        parseLogger.setAppContext(appContext);
        sseFlag = appContext.getBean(SseContextFlag.class);
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#toString()
     */
    @Override
    public String toString() {
        return "Product path=" + getFullPath() + " created=" + this.getProductCreationTimeStr();
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getVcid()
     */
    @Override
    public Integer getVcid() {
        return this.vcid;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setVcid(java.lang.Integer)
     */
    @Override
    public void setVcid(final Integer vcid) {
        this.vcid = vcid;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setScid(int)
     */
    @Override
    public void setScid(final int scid) {
        this.scid = scid;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getScid()
     */
    @Override
    public int getScid() {
        return this.scid;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSessionId()
     */
    @Override
	public Long getSessionId() {
        return this.sessionId;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setSessionId(java.lang.Long)
     */
    @Override
	public void setSessionId(final Long testIdKey) {
    	if (testIdKey == null) {
    		this.sessionId = 0;
    	} else {
            this.sessionId = testIdKey;
    	}
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSessionFragment()
     */
     @Override
     public SessionFragmentHolder getSessionFragment()
     {
         return sessionFragment;
     }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setSessionFragment(jpl.gds.shared.holders.SessionFragmentHolder)
     */
     @Override
     public void setSessionFragment(final SessionFragmentHolder fragment)
     {
         sessionFragment = ((fragment != null) ? fragment
                                               : SessionFragmentHolder.MINIMUM);
     }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSessionDssId()
     */
    @Override
    public Integer getSessionDssId()
    {
        return sessionDssId;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setSessionDssId(java.lang.Integer)
     */
    @Override
    public void setSessionDssId(final Integer dssId)
    {
        sessionDssId = dssId;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSessionVcid()
     */
    @Override
    public Integer getSessionVcid()
    {
        return sessionVcid;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setSessionVcid(java.lang.Integer)
     */
    @Override
    public void setSessionVcid(final Integer vcid)
    {
        sessionVcid = vcid;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setApid(int)
     */
    @Override
    public void setApid(final int apid) {
        this.apid = apid;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getApid()
     */
    @Override
    public int getApid() {
        return this.apid;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getScet()
     */
    @Override
    public IAccurateDateTime getScet() {
        return this.scet;
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSol()
     */
    @Override
    public ILocalSolarTime getSol() {
    	return this.sol;
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getScetExact()
     */
    @Override
    public long getScetExact()
    {
    	return(this.scet != null ? this.scet.getTime() : 0);
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSolExact()
     */
    @Override
    public long getSolExact()
    {
        return(this.sol != null ? this.sol.getSolExact() : 0);
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setScet(jpl.gds.shared.time.IAccurateDateTime)
     */
    @Override
    public void setScet(final IAccurateDateTime scet) {
        this.scet = scet;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setScet(java.lang.String)
     */
    @Override
    public void setScet(final String scetStr) throws SAXException {
        try {
            this.scet = new AccurateDateTime(scetStr);
        } catch (final java.text.ParseException pex) {
        	// TODO(MBI): Why must this throw a SAXException?!
            throw new SAXException("Bad SCET Format on time " + scetStr);
        } 
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setSol(jpl.gds.shared.time.ILocalSolarTime)
     */
    @Override
    public void setSol(final ILocalSolarTime sol)
    {
        if (useSolTime)
        {
            this.sol = sol;
        }
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getScetStr()
     */
    @Override
    public String getScetStr() {
        if (this.scet != null) {
            final String result = this.scet.getFormattedScet(true);
            return result;
        }
        return emptyStr;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getRctStr()
     */
    @Override
    public String getRctStr()
    {
        if (rct != null)
        {
            return FastDateFormat.format(rct, null, null);
        }

        return emptyStr;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSolStr()
     */
    @Override
    public String getSolStr() {
        if (this.sol != null) {
            final String result = this.sol.getFormattedSol(true);
            return result;
        }
        return emptyStr;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getProductCreationTimeStr()
     */
    @Override
    public String getProductCreationTimeStr() {
        if (this.productCreationTime != null) {
            final DateFormat df = TimeUtility.getFormatterFromPool();
            final String result = df.format(this.productCreationTime);
            TimeUtility.releaseFormatterToPool(df);
            return result;
        }
        return emptyStr;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getErtStr()
     */
    @Override
    public String getErtStr() {
        if (this.ert != null) {
            final String result = this.ert.getFormattedErt(true);
            return result;
        }
        return emptyStr;
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getErtExact()
     */
    @Override
    public long getErtExact()
    {
    	return(this.ert != null ? this.ert.getTime() : 0);
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getErtExactFine()
     */
    @Override
    public long getErtExactFine()
    {
    	return(this.ert != null ? this.ert.getNanoseconds() : 0);
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSclk()
     */
    @Override
    public ICoarseFineTime getSclk() {
        return this.sclk;
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSclkCoarse()
     */
    @Override
    public long getSclkCoarse()
    {
    	return(this.sclk != null ? this.sclk.getCoarse() : 0);
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSclkFine()
     */
    @Override
    public long getSclkFine()
    {
    	return(this.sclk != null ? this.sclk.getFine() : 0);
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSclkExact()
     */
    @Override
    public long getSclkExact()
    {
        return (this.sclk != null ? this.sclk.getExact() : 0);
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSclkStr()
     */
    @Override
    public String getSclkStr() {
        if (this.sclk != null) {
            return this.sclk.toString();
        }
        return emptyStr;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setSclk(jpl.gds.shared.time.AbstractSpacecraftClock)
     */
    @Override
    public void setSclk(final ICoarseFineTime sclk) {
        this.sclk = sclk;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setStorageDirectory(java.lang.String)
     */
    @Override
    public void setStorageDirectory(final String storageDirectory) {
        this.storageDirectory = storageDirectory;
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getStorageDirectory()
     */
    @Override
    public String getStorageDirectory() {
    	return this.storageDirectory;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setFullPath(java.lang.String)
     */
    @Override
    public void setFullPath(final String filenameNoSuffix) {
        this.fullPath = filenameNoSuffix;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getFullPath()
     */
    @Override
    public String getFullPath() {
        if (this.fullPath != null) {
            return this.fullPath;
        }

        if (this.storageDirectory == null) {
            return getFilenameWithPrefix();
        }
        return this.storageDirectory + File.separator + getFilenameWithPrefix();
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setTemplateContext(java.util.Map)
     */
    @Override
	public void setTemplateContext(final Map<String,Object> map)
    {
        map.put("productMetadata", this);
        map.put("testSessionId", this.sessionId); //Deprecated for R8
        map.put("testNumber", this.sessionId);  //Deprecated for R8
        map.put("sessionId", this.sessionId);
        map.put("sessionHost", this.sessionHost);
        map.put("sessionFragment", sessionFragment.getValue());
        map.put("apid", this.apid);
        map.put("vcid", this.vcid);
        map.put("scid", this.scid);
        if (this.productType != null) {
        	map.put("productType", this.productType);
        }
        map.put("scet", getScetStr());

        if (useSolTime && (this.sol != null))
        {
        	map.put("lst", getSolStr());
        }

        map.put("full_path", getFullPath()); // Deprecated for R8
        map.put("fullPath", getFullPath()); 
        map.put("totalParts", this.getTotalParts()); //  Need to use getter to get corrected value
        map.put("absoluteDataFile",getAbsoluteDataFile()); // - Deprecated for R8. Same as fullPath.
        map.put("partial", this.partial);
        map.put("dvtCoarse", this.dvtCoarse);
        map.put("dvtFine", dvtFine);
        map.put("commandNumber", this.commandNum);
        map.put("seqId", this.seqId);
        map.put("seqVersion", this.seqVersion);
        map.put("productDefVersion", this.xmlVersion);
        
        if (this.productCreationTime != null) {
            map.put("creationTime", getProductCreationTimeStr());
        }
        
        if (this.ert != null) {
            map.put("ert", getErtStr());
        }
        
        if (this.sclk != null) {
            map.put("sclk", getSclkStr());
        }
        
        map.put("fileSize", fileSize);
        map.put("checksum", checksum);
        map.put("groundStatus", groundStatus);
        if (productVersion != null) {
            map.put("productVersion", productVersion);
        } 
              
        //used to pass mission-specific values to the templates
        //(override these in the subclasses if you need them...
        //see MslProductMetadata.setTemplateContext for an example)
        final Map<String,String> properties = getMissionProperties();
        final List<String> names = new ArrayList<String>();
        final List<String> values = new ArrayList<String>();
        for(final Entry<String,String> name : properties.entrySet())
        {
        	names.add(name.getKey());
        	values.add(name.getValue());
        }
        map.put("missionPropertyNames",names);
		map.put("missionPropertyValues",values);

        if (sessionDssId != null)
        {
            map.put("sessionDssId", sessionDssId);
        }

        final String tvcid = getTransformedVcid();

        if (! tvcid.isEmpty())
        {
            map.put(missionProperties.getVcidColumnName(), tvcid);
        }

        if (this.rct != null)
        {
            map.put("rct", FastDateFormat.format(this.rct, null, null));
            map.put("rctExact", this.rct.getTime());
        }
    }

    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getMissionProperties()
     */
    @Override
    public Map<String,String> getMissionProperties()
    {
    	return(new HashMap<String,String>(0));
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getAbsoluteDataFile()
     */
    @Override
    public String getAbsoluteDataFile()
    {

        return getFullPath();
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#parseFromElement(java.lang.String, java.lang.String)
     */
    @Override
    public boolean parseFromElement(final String elemName, final String text) {
    	final String checkElemName = elemName.toLowerCase();

        if (checkElemName.equals("testsessionid")) {
            setSessionId(XmlUtility.getLongFromText(text));
        } else if (checkElemName.equals("testsessionhost")) {
            setSessionHost(text);
        } else if (checkElemName.equals("scid")) {
            this.scid = XmlUtility.getIntFromText(text);
        } else if (checkElemName.equals("apid")) {
            this.apid = XmlUtility.getIntFromText(text);
        } else if (checkElemName.equals("productname")) {
            this.productType = text;
        } else if (checkElemName.equals("vcid")) {
            this.vcid = XmlUtility.getIntFromText(text);
        } else if (checkElemName.equals("scet")) {
            try {
                this.scet = new AccurateDateTime(text);
            } catch (final ParseException e) {
                parseLogger
                        .error("Error parsing SCET from product EMD file or product message for product "
                                + this.getFilenameWithPrefix());
            }
        } else if (useSolTime && checkElemName.equals("lst")) {
            try {
                this.sol = LocalSolarTimeFactory.getNewLst(text, this.scid);
            } catch (final ParseException e) {
                parseLogger
                        .error("Error parsing LST from product EMD file or product message for product "
                                + this.getFilenameWithPrefix());
            }
        } else if (checkElemName.equals("sclk")) {
            try {
                this.sclk = dvtSclkFmt.valueOf(text);
            } catch (final NumberFormatException nex) {
                parseLogger
                        .error("Error parsing SCLK from product EMD file or product message for product "
                                + this.getFilenameWithPrefix());
            }
        } else if (checkElemName.equals("dvtfine")) {
            this.dvtFine = XmlUtility.getIntFromText(text);
        } else if (checkElemName.equals("dvtcoarse")) {
            this.dvtCoarse = XmlUtility.getLongFromText(text);
        } else if (checkElemName.endsWith("ert")) {
        	try {
        		this.ert = new AccurateDateTime(text);
        	} catch (final ParseException e) {
                parseLogger
        		.error("Error parsing ERT from product EMD file or product message for product "
        				+ this.getFilenameWithPrefix());
        	}
        } else if (checkElemName.equals("datafilename")) {
            this.fullPath = text;
        } else if (checkElemName.equals("commandnumber")) {
            this.commandNum = XmlUtility.getIntFromText(text);
        } else if (checkElemName.equals("sequenceid")) {
            this.seqId = XmlUtility.getIntFromText(text);
        } else if (checkElemName.equals("sequenceversion")) {
            this.seqVersion = XmlUtility.getIntFromText(text);
        } else if (checkElemName.equals("totalparts")) {
            this.totalParts = XmlUtility.getIntFromText(text);
        } else if (checkElemName.equals("xmlversion")) {
            this.xmlVersion = XmlUtility.getIntFromText(text);
        } else if (checkElemName.equals("groundcreationtime")) {
			try {
				this.productCreationTime = getDateFromText(text,
						TimeUtility.isConfiguredToUseDoyFormat());
			} catch (final ParseException e) {
                parseLogger
                        .error("Error parsing creation time from product EMD file or product message for product "
                                + this.getFilenameWithPrefix());
            }
        } else if (checkElemName.equals("groundstatus")) {
            this.groundStatus = Enum.valueOf(ProductStatusType.class, text);
            if (this.groundStatus == ProductStatusType.UNKNOWN
                    || this.groundStatus == ProductStatusType.PARTIAL) {
                this.partial = true;
            } else {
                this.partial = false;
            }
        } else if (checkElemName.equals("fswdictionarydir")) {
            this.emdDictionaryDir = text;
        } else if (checkElemName.equals("fswdictionaryversion")) {
            this.emdDictionaryVersion = text;
        } else {
        	return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setSequenceId(int)
     */
    @Override
    public void setSequenceId(final int seq) { 
        this.seqId = seq;
        /* 
         * Do not try again to load the sequence
         * dictionary if we tried before.
         */
        if (seqDictionaryReported) {
            return;
        }
        try {
            /*  3/14/18 - Must now check to see if the sequence dictionary is loaded first. */
            if (appContext.getBean(FlightDictionaryLoadingStrategy.class).isSequenceEnabled()) {
                final ISequenceDefinitionProvider seqDict = appContext.getBean(ISequenceDefinitionProvider.class);
                if (seqDict != null) {
                    /*  7/30/15. Previous code was trying to map a whole
                     * sequence ID to a category, which will not work. We want only the
                     * category.
                     */
                    final int catId = seqDict.getCategoryIdFromSeqId(seqId);
                    setSequenceCategory(seqDict.getCategoryNameByCategoryId(catId));
                } else {
                    parseLogger.debug("Sequence dictionary is null in AbstractProductMetadata");
                }

            } else {
                parseLogger.debug("Sequence dictionary not enabled in AbstractProductMetadata");
            }
        } catch (final Exception e) {
            if (!seqDictionaryReported) {
                parseLogger.debug("Unable to create seqid dictionary - " + e.getMessage());
                seqDictionaryReported = true;
            } 
        }
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSequenceId()
     */
    @Override
    public int getSequenceId() {
        return this.seqId;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setSequenceVersion(int)
     */
    @Override
    public void setSequenceVersion(final int seqVersion) {
        this.seqVersion = seqVersion;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSequenceVersion()
     */
    @Override
    public int getSequenceVersion() {
        return this.seqVersion;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setCommandNumber(int)
     */
    @Override
    public void setCommandNumber(final int commandNum) {
        this.commandNum = commandNum;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getCommandNumber()
     */
    @Override
    public int getCommandNumber() {
        return this.commandNum;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setXmlVersion(int)
     */
    @Override
    public void setXmlVersion(final int xmlVersion) {
        this.xmlVersion = xmlVersion;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getXmlVersion()
     */
    @Override
    public int getXmlVersion() {
        return this.xmlVersion;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setDvtCoarse(long)
     */
    @Override
    public void setDvtCoarse(final long dvtCoarse) {
        this.dvtCoarse = dvtCoarse;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getDvtCoarse()
     */
    @Override
    public long getDvtCoarse() {
        return this.dvtCoarse;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setDvtFine(long)
     */
    @Override
    public void setDvtFine(final long dvtFine) {
        this.dvtFine = dvtFine;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getDvtFine()
     */
    @Override
    public long getDvtFine() {
        return this.dvtFine;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getDvtString()
     */
    @Override
	public String getDvtString() {
    	ICoarseFineTime scClock;
    	
    	scClock = new DataValidityTime(this.dvtCoarse, this.dvtFine);
    	
        return scClock.toString();
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setTotalParts(int)
     */
    @Override
    public void setTotalParts(final int totalParts) {
        this.totalParts = totalParts;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getTotalParts()
     */
    @Override
    public int getTotalParts() {
        return this.totalParts;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getLastPart()
     */
    @Override
    public IProductPartProvider getLastPart() {
        if ((this.parts == null) || (this.parts.size() == 0)) {
            return null;
        }
        return (this.parts.get(this.parts.size() - 1));
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#partIterator()
     */
    @Override
    public Iterator<IProductPartProvider> partIterator() {
        return this.parts.iterator();
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getPartList()
     */
    @Override
    public List<IProductPartProvider> getPartList() {
        return this.parts;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getProductCreationTime()
     */
    @Override
    public IAccurateDateTime getProductCreationTime() {
        return this.productCreationTime;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setProductCreationTime(java.util.Date)
     */
    @Override
    public void setProductCreationTime(final IAccurateDateTime productCreationTime) {
        this.productCreationTime = productCreationTime;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getRct()
     */
    @Override
    public IAccurateDateTime getRct()
    {
        return rct;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setRct(java.util.Date)
     */
    @Override
    public void setRct(final IAccurateDateTime rct)
    {
        this.rct = rct;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#isPartial()
     */
    @Override
    public boolean isPartial() {
        return this.partial;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setPartial(boolean)
     */
    @Override
    public void setPartial(final boolean partial) {
        this.partial = partial;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSessionHost()
     */
    @Override
	public String getSessionHost() {
        return this.sessionHost;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setSessionHost(java.lang.String)
     */
    @Override
	public void setSessionHost(final String testHost) {
        this.sessionHost = testHost;
    }

    @Override
    public Integer getSessionHostId() {
        return this.sessionhostId;
    }

    @Override
    public void setSessionHostId(final Integer hostId) {
        this.sessionhostId = hostId;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getFilenameWithPrefix()
     */
    @Override
    public abstract String getFilenameWithPrefix();

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getFilename()
     */
    @Override
    public abstract String getFilename();

    /**
     * Tool allows user to get just the file name, no extension or version
     * For example, if the file is "test-1.emd" then "test" will be returned.
     */
    public String getFilenameNoVersionOrExtension() {
        String filenameWithExt = getFilename();
        return filenameWithExt.substring(0, filenameWithExt.lastIndexOf('-'));
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getDirectoryName()
     */
    @Override
    public abstract String getDirectoryName();

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getCommandId()
     */
    @Override
    public abstract long getCommandId();

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#addPart(jpl.gds.product.AbstractProductPart)
     */
    @Override
    public void addPart(final IProductPartProvider part) {
        this.parts.add(part);
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getErt()
     */
    @Override
    public IAccurateDateTime getErt() {
        return this.ert;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setErt(jpl.gds.shared.time.IAccurateDateTime)
     */
    @Override
    public void setErt(final IAccurateDateTime ert) {
        this.ert = ert;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getGroundStatus()
     */
    @Override
    public ProductStatusType getGroundStatus() {
        return this.groundStatus;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setGroundStatus(jpl.gds.product.ProductStatusType)
     */
    @Override
    public void setGroundStatus(final ProductStatusType groundStatus) {
        this.groundStatus = groundStatus;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSequenceCategory()
     */
    @Override
    public String getSequenceCategory()
    {
        return sequenceCategory;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setSequenceCategory(java.lang.String)
     */
    @Override
    public void setSequenceCategory(final String sc)
    {
        sequenceCategory = sc;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getSequenceNumber()
     */
    @Override
    public Long getSequenceNumber()
    {
        return sequenceNumber;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setSequenceNumber(java.lang.Long)
     */
    @Override
    public void setSequenceNumber(final Long sn)
    {
        sequenceNumber = sn;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setSclk(java.lang.String)
     */
    @Override
    public void setSclk(final String sclkStr) throws SAXException {
        try {
            sclk = sclkFmt.valueOf(sclkStr);
        } catch (final NumberFormatException nex) {
            throw new SAXException(nex.getMessage() + "Bad SCLK Format");
        }
    }

    /**
     * Creates a 4 digit character string representing the given number, left
     * padded with 0s.
     * 
     * @param n the number to represent
     * @return the padded string representation of the number
     */
    protected String zeroPad(final int n) {
        if (n > 999) {
            return Integer.toString(n);
        }
        if (n > 99) {
            return "0" + n;
        }
        if (n > 9) {
            return "00" + n;
        }
        return "000" + n;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getProductType()
     */
    @Override
    public String getProductType() {
        return this.productType;
    }

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setProductType(java.lang.String)
     */
    @Override
    public void setProductType(final String productType) {
        this.productType = productType;
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#productTypeIsValid()
     */
    @Override
    public boolean productTypeIsValid() {
    	return !( this.productType.equalsIgnoreCase( UNKNOWN_PRODUCT_TYPE ) );
    }
   
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getEmdDictionaryDir()
     */
    @Override
    public String getEmdDictionaryDir() {
        return this.emdDictionaryDir;
    }
   
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setEmdDictionaryDir(java.lang.String)
     */
    @Override
    public void setEmdDictionaryDir(final String emdDictionaryDir) {
        this.emdDictionaryDir = emdDictionaryDir;
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getEmdDictionaryVersion()
     */
    @Override
    public String getEmdDictionaryVersion() {
        return this.emdDictionaryVersion;
    }
   
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setEmdDictionaryVersion(java.lang.String)
     */
    @Override
    public void setEmdDictionaryVersion(final String emdDictionaryVersion) {
        this.emdDictionaryVersion = emdDictionaryVersion;
    }
	
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#toXml()
     */
    @Override
	public String toXml()
	{
		String output = "";
		try
		{
			output = StaxStreamWriterFactory.toXml(this);
		}
		catch(final XMLStreamException e)
		{
			e.printStackTrace();
            parseLogger.error("Could not transform ProductMetadata object to XML: " + e.getMessage());
		}
		
		return(output);
	}
	
	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getAdditionalMissionData()
     */
	@Override
    public Map<String,Object> getAdditionalMissionData() {
		final Map<String, Object> dataMap = new HashMap<String, Object>();

		dataMap.put("file_size", fileSize);
		dataMap.put("ground_status", groundStatus);

		return dataMap;
	}

	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getProductVersion()
     */
	@Override
    public String getProductVersion() {
		return productVersion;
	}

	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setProductVersion(java.lang.String)
     */
	@Override
    public void setProductVersion(final String productVersion) {
		this.productVersion = productVersion;
	}

    /* (non-Javadoc)
     * @see jpl.gds.product.api.IProductMetadataUpdater#setFswVersion(long)
     * @param fswVersion
     */
    @Override
    public void setFswVersion(long fswVersion) {
        this.fswVersion = fswVersion;
    }

    /* (non-Javadoc)
     * @see IProductMetadataProvider#getFswVersion()
     * @return
     */
    @Override
    public long getFswVersion() { return this.fswVersion;}

    /* (non-Javadoc)
     * @see IProductMetadataUpdater#setFswDictionaryDir(String)
     * @param fswDictionaryDir
     */
    @Override
    public void setFswDictionaryDir(String fswDictionaryDir) {
        this.fswDictionary = fswDictionaryDir;
    }

    /* (non-Javadoc)
     * @see IProductMetadataProvider#getFswDictionaryDir()
     * @return
     */
    @Override
    public String getFswDictionaryDir() { return this.fswDictionary; }

    /* (non-Javadoc)
     * @see IProductMetadataUpdater#setFswDictionaryVersion(String)
     * @return
     */
    @Override
    public void setFswDictionaryVersion(String fswDictionaryVersion) {
        this.fswDictionaryVersion = fswDictionaryVersion;
    }

    /* (non-Javadoc)
     * @see IProductMetadataProvider#getFswDictionaryVersion()
     * @return
     */
    @Override
    public String getFswDictionaryVersion() { return this.fswDictionaryVersion; }

    /* (non-Javadoc)
     * @see IProductMetadata#getIsCompressed()
     * @return
     */
    @Override
    public boolean getIsCompressed() {
        return isCompressed;
    }

    /* (non-Javadoc)
     * @see IProductMetadata#setIsCompressed()
     * @return
     */
    @Override
    public void setIsCompressed(boolean compressed) {
        isCompressed = compressed;
    }


    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getTransformedVcid()
     */
    @Override
    public String getTransformedVcid()
    {
        if (sessionVcid == null)
        {
            return "";
        }

        if (missionProperties.shouldMapQueryOutputVcid())
        {
            return StringUtil.safeTrim(missionProperties.mapDownlinkVcidToName(sessionVcid));
        }

        return sessionVcid.toString();
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getTransformedStringId(java.lang.String)
     */
    @Override
    public int getTransformedStringId(final String str)
    {
        return (missionProperties.mapNameToDownlinkVcid(str));
    }

	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getChecksum()
     */
	@Override
    public long getChecksum() {
	    return this.checksum;
	}

	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setChecksum(long)
     */
	@Override
    public void setChecksum(final long checksum) {
	    this.checksum = checksum;
	}

	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getFileSize()
     */
	@Override
    public long getFileSize() {
	    return this.fileSize;
	}

	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setFileSize(long)
     */
	@Override
    public void setFileSize(final long fileSize) {
	    this.fileSize = fileSize;
	}
	
	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getActualChecksum()
     */
	@Override
    public long getActualChecksum() {
	    return this.actualChecksum;
	}

	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setActualChecksum(long)
     */
	@Override
    public void setActualChecksum(final long checksum) {
	    this.actualChecksum = checksum;
	}

	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getActualFileSize()
     */
	@Override
    public long getActualFileSize() {
	    return this.actualFileSize;
	}

	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataUpdater#setActualFileSize(long)
     */
	@Override
    public void setActualFileSize(final long fileSize) {
	    this.actualFileSize = fileSize;
	}

	/* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getFileData(java.lang.String)
     */
	@Override
	public Map<String, String> getFileData(final String NO_DATA) {
		final DateFormat df = TimeUtility.getFormatterFromPool();
		final Map<String, String> map = new HashMap<String, String>();

		map.put("ert", ert != null ? df.format(ert) : NO_DATA);
		map.put("scet", scet != null ? df.format(scet) : NO_DATA);

        if (useSolTime)
        {
            map.put("sol", sol != null ? df.format(sol) : NO_DATA);
        }

		map.put("sclk", sclk != null ? sclk.toString() : NO_DATA);
		map.put("file_size", Long.toString(fileSize));
		final String groundSt = groundStatus != null ? groundStatus.toString()
				: NO_DATA;
		map.put("ground_status", groundSt);
		map.put("type", "DP " + groundSt);
		map.put("source", "FSW"); // For session report csv format
		map.put("id", Long.toString(getCommandId()));

		final String prodType = productType != null ? productType : NO_DATA;
		map.put("product_type", prodType);
		map.put("apid", Integer.toString(apid));
		final String dvt = (new DataValidityTime(dvtCoarse, dvtFine)).toString();
		map.put("dvt", dvt);
        // 01/28/2015: Need to use getter to get corrected value
		map.put("total_parts", Integer.toString(getTotalParts()));
        // For session report csv format
		map.put("csv_id", prodType + " (" + apid + ")");

		TimeUtility.releaseFormatterToPool(df);

		return map;
	}


    /**
     * Avoid appending "null" for null objects.
     *
     * @param sb String builder
     * @param o  Object to append to string builder
     *
     */
    protected static void append(final StringBuilder sb,
                                 final Object        o)
    {
        if (o != null)
        {
            sb.append(o);
        }
    }
	

    /* (non-Javadoc)
     * @see jpl.gds.product.IProductMetadataProvider#getCsvHeader(java.util.List)
     */
    @Override
	public String getCsvHeader(final List<String> csvColumns)
    {
        final StringBuilder sb = new StringBuilder();

        sb.append("recordType");

        for (final String cce : csvColumns)
        {
            sb.append(',').append(cce);
        }

        sb.append('\n');

        return sb.toString();
    }
    
    /**
     * Converts text into a Date object.
     * 
     * @param text
     *            the text to convert (must be an ISO formatted date/time)
     * @param useDoyFormat
     *            flag indicating whether or not DOY date format is expected in
     *            text
     * @return the Date object, or null if the input text is null
     * @throws ParseException
     *             if the string cannot be parsed to a date
     */
    protected IAccurateDateTime getDateFromText(final String text, final boolean useDoyFormat)
            throws ParseException {
        IAccurateDateTime d = null;

        if (text != null) {
            DateFormat format = null;

            if (useDoyFormat) {
                format = TimeUtility.getDoyFormatterFromPool();
            } else {
                format = TimeUtility.getISOFormatterFromPool();
            }

            d = new AccurateDateTime(format.parse(text));
            return d;
        }

        return null;
    }
}
