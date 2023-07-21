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
package jpl.gds.context.impl;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.filtering.ScidFilter;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.filtering.IFilterableDataItem;
import jpl.gds.context.api.filtering.IScidFilterable;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.metadata.context.ContextKey;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.stax.StaxStreamWriterFactory;

/**
 * The SessionIdentification object incorporates the minimum set of session
 * configuration information to identify the session and locate it on the
 * network.  This object should be all that is required in messages, for example,
 * that do not include the entire set of session configuration.
 * 
 *
 */
public class ContextIdentification implements IContextIdentification {

    /**
     * Suffix for default context name. Prefix will be user name.
     */
	public static final String DEFAULT_CONTEXT_NAME_SUFFIX = "_context";
	
	/**
	 * Name of this context.
	 */
	protected String name;
	/**
	 * User-supplied type of this context.
	 */
	protected String type;
	/**
	 * User-supplied description of this context.
	 */
	protected String description;
	/**
	 * Name of user who instigated this context.
	 */
	protected String user;
	/**
	 * Starting timestamp for this context.
	 */
    protected IAccurateDateTime startTime;
	/**
	 * Ending timestamp for this context.
	 */
    protected IAccurateDateTime endTime;
	/**
	 * String representation of starting timestamp.
	 */
	protected String startTimeStr;
	/**
	 * String representation of ending timestamp.
	 */
	protected String endTimeStr;
	/**
	 * Key for this context.
	 */
	protected IContextKey key = new ContextKey();
	/**
	 * Metadata header for this context.
	 */
	protected ISerializableMetadata header;
	
	/** 
	 * Flag indicating data has changed since the last header fetched.
	 */
	protected boolean dirty = true;
	
	/**
	 * Spacecraft ID.
	 */
	protected Integer spacecraftId = Integer.valueOf(0);
	
	/**
	 * Filter object for checking SCID matches.
	 */
	protected ScidFilter scidFilter = new ScidFilter(null, true);

    private final boolean enableScidCheck;

	/**
     * Creates an empty ContextIdentification with its own context key. 
	 * @param mp the current mission properties object
	 * @param scid the spacecraft ID for this context
     */
    public ContextIdentification(final MissionProperties mp, final int scid) {
        setSpacecraftId(scid);
        this.enableScidCheck = mp.getScidChecksEnabled();
    	init();
    }
    
    /**
     * Creates an empty ContextIdentification with its own context key
     * and an undefined spacecraft ID. 
     * @param mp the current mission properties object
     */
    public ContextIdentification(final MissionProperties mp) {
        this.enableScidCheck = mp.getScidChecksEnabled();
        init();
    }
    

    /**
     * Creates an empty ContextIdentification with the supplied context key.
     * @param mp the current mission properties object 
     * @param inKey the input context key
     * @param scid the spacecraft ID for this context
     */
    public ContextIdentification(final MissionProperties mp, final IContextKey inKey, final int scid) {
        key = inKey;
        this.enableScidCheck = mp.getScidChecksEnabled();
        setSpacecraftId(scid);
        init();
    }

    @Override
	public String getContextId() {
    	return (this.key.getContextId());
    }
    
    private void init() {
    	key.clearFieldsForNewConfiguration();
        setUser(GdsSystemProperties.getSystemUserName());
        setName(null);
        setStartTime(new AccurateDateTime(System.currentTimeMillis()));
    }

    @Override
    public synchronized void setSpacecraftId(final Integer scid) {
        this.spacecraftId = scid;
        this.dirty = true;  
        if (scid != null && scid.intValue() != MissionProperties.UNKNOWN_ID) {
            final List<UnsignedInteger> scidList = new ArrayList<>();
            scidList.add(UnsignedInteger.valueOf(scid));
            scidFilter = new ScidFilter(scidList, true);
        } else if (scid == null) {
            scidFilter = new ScidFilter(null, true);
        }
    }

    @Override
    public Integer getSpacecraftId() {
        return this.spacecraftId;
    }
	
	@Override
	public synchronized void clearFieldsForNewConfiguration() {
        setStartTime(new AccurateDateTime());
        setEndTime(null);
        key.clearFieldsForNewConfiguration();
        this.dirty = true;
	}
	
	@Override
	public synchronized void copyValuesFrom(final IContextIdentification toCopy) {
		
		if (!(toCopy instanceof ContextIdentification)) {
			throw new IllegalArgumentException("Argument must be a ContextIdentification");
		}
		
		final IContextIdentification tc = toCopy;
		
		// make sure you use the setter methods like setStartTime(...)
		// instead of doing something like this.startTime = ...
		// because some of the setter methods like setStartTime(...) actually
		// set the value of more than one member variable
		
		getContextKey().copyValuesFrom(tc.getContextKey());
		
		if (tc.getStartTime() != null) {
            setStartTime(new AccurateDateTime(tc.getStartTime().getTime()));
		}
		if (tc.getEndTime() != null) {
            setEndTime(new AccurateDateTime(tc.getEndTime().getTime()));
		}
		setSpacecraftId(tc.getSpacecraftId());
		setDescription(tc.getDescription());
		setName(tc.getName());
		setType(tc.getType());
		setUser(tc.getUser());
		this.dirty = true;

	}
	
	@Override
	public synchronized void setFragment(final Integer fragment)
    {
        this.key.setFragment(((fragment != null)
                               ? fragment
                               : Integer.valueOf(1)));
        this.dirty = true;
	}
	
	@Override
	public Integer getFragment()
    {
		return key.getFragment();
	}

	@Override
	public Long getNumber() {
		return key.getNumber();
	}

	@Override
	public synchronized void setNumber(final Long sessionKey) {
	    key.setNumber(sessionKey);
	    this.dirty = true;
	}

	@Override
	public String getHost() {
		return key.getHost();
	}

	@Override
	public synchronized void setHost(final String host) {
		this.key.setHost(host);
		this.dirty = true;
	}

	@Override
	public Integer getHostId() {
		return key.getHostId();
	}

	@Override
	public synchronized void setHostId(final Integer hostId) {
		this.key.setHostId(hostId);
		this.dirty = true;
	}

	@Override
	public String getUser() {
		return user;
	}

	@Override
	public synchronized void setUser(final String user) {
		this.user = user;
		this.dirty = true;
	}

	@Override
    public IAccurateDateTime getEndTime() {
        return (endTime != null) ? new AccurateDateTime(endTime.getTime()) : null;
	}

	@Override
	public synchronized String getEndTimeStr() {
		final DateFormat format = TimeUtility.getFormatterFromPool();
		
		try {
			if (endTimeStr == null && endTime != null) {
				endTimeStr = format.format(endTime);
			}
		} finally {
			TimeUtility.releaseFormatterToPool(format);
		}
		return endTimeStr;
	}

	@Override
    public synchronized void setEndTime(final IAccurateDateTime inEndTime) {
		
		// tolerate null input values
		if (inEndTime == null) {
			this.endTime = null;
			this.endTimeStr = null;
			return;
		}
		final DateFormat format = TimeUtility.getFormatterFromPool();
        this.endTime = new AccurateDateTime(inEndTime.getTime());
		try {
			if (endTimeStr == null) {
				this.endTimeStr = format.format(this.endTime);
			}
		} finally {
			TimeUtility.releaseFormatterToPool(format);
		}
		this.dirty = true;
	}

	@Override
    public IAccurateDateTime getStartTime() {
        return (startTime != null) ? new AccurateDateTime(startTime.getTime()) : null;
	}

	@Override
	public String getStartTimeStr() {
		return startTimeStr;
	}


	@Override
    public synchronized void setStartTime(final IAccurateDateTime inStartTime) {
		
		// tolerate null input values
		if (inStartTime == null) {
			this.startTime = null;
			this.startTimeStr = null;
			return;
		}
		final DateFormat format = TimeUtility.getFormatterFromPool();
        this.startTime = new AccurateDateTime(inStartTime.getTime());
		try {
			startTimeStr = format.format(this.startTime);
		} finally {
			TimeUtility.releaseFormatterToPool(format);
		}
		this.dirty = true;
	}
	
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public synchronized void setDescription(final String sessionDesc)
    {
        if (sessionDesc == null)
        {
            description = null;
        } else {
        	description = replaceInvalidCharsWithSpaces(sessionDesc);
        }
    	this.dirty = true;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public synchronized void setName(final String name) {
		this.name = name;

		if (this.name == null) {
			this.name = this.user + DEFAULT_CONTEXT_NAME_SUFFIX;
		}
		this.dirty = true;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public synchronized void setType(final String type)
    {
        if (type == null)
        {
            this.type = null;
        } else {
		    this.type = replaceInvalidCharsWithSpaces(type);
        }
    	this.dirty = true;
	}
	
	private String replaceInvalidCharsWithSpaces(final String origString) {
		final StringBuilder buffer = new StringBuilder(origString);
		for (int i = 0; i < buffer.length(); i++) {
			final char c = buffer.charAt(i);
			if (c == '&' || c == '>' || c == '<' || c == '%') {
				buffer.setCharAt(i, ' ');
			}
		}
		return buffer.toString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#toXml()
	 */
	@Override
	public synchronized String toXml() {
		String output = "";
		try {
			output = StaxStreamWriterFactory.toXml(this);
		} catch (final XMLStreamException e) {
            TraceManager.getDefaultTracer().error("Could not transform ContextIdentification object to XML: "
					+ e.getMessage(), e);
		}

		return (output);
	}

    /**
     * Produces an XML version of this session configuration that is
     * pretty-printed.
     * @return XML string
     */
	public synchronized String toPrettyXml() {
		String output = "";
		try {
			output = StaxStreamWriterFactory.toPrettyXml(this);
		} catch (final XMLStreamException e) {
            TraceManager.getDefaultTracer()
                    .error("Could not transform COntextIdentification object to XML: "
					+ e.getMessage(), e);
		}

		return (output);
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public synchronized void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException {

		writer.writeStartElement("ContextId"); 

		generateInternalStaxElements("BasicContextInfo", writer);

		writer.writeEndElement(); // </ContextId>

	}
	
	/**
	 * Generates the XML elements for this context identification.
	 * 
	 * @param basicInfoElementName name to use for the enclosing XML element
	 * @param writer the XMLStreamWriter for output
	 * @throws XMLStreamException if there is an issue generating the XML output
	 */
	protected void generateInternalStaxElements(final String basicInfoElementName, final XMLStreamWriter writer) throws XMLStreamException {
		final DateFormat df = TimeUtility.getFormatterFromPool();

		try {
			writer.writeStartElement(basicInfoElementName); 
			
			XmlUtility.writeSimpleElement(writer, "Number", key.getNumber() != null ? key.getNumber() : "0");
			
		    XmlUtility.writeSimpleElement(writer, "SpacecraftId", spacecraftId);
			
			XmlUtility.writeSimpleElement(writer, "Name", name);

			/*
			 * Write these elements as CDATAs.
			 */
			XmlUtility.writeSimpleCDataElement(writer, "Type", type);
			XmlUtility.writeSimpleCDataElement(writer, "Description", description);

			writer.writeEndElement(); // <end basic info element>

			writer.writeStartElement("Venue"); // <Venue>

			XmlUtility.writeSimpleElement(writer, "User", user);
			XmlUtility.writeSimpleElement(writer, "Host", key.getHost());
			XmlUtility.writeSimpleElement(writer, "HostId", key.getHostId() != null ? key.getHostId() : "-1");

			writer.writeEndElement(); // </Venue>

			// Start time was not being emitted if null. The schema
			// does not allow that. Set it to date "0" instead.
			XmlUtility.writeSimpleElement(writer, "StartTime", startTime != null ? getStartTimeStr() : df.format(new AccurateDateTime(0)));
			XmlUtility.writeSimpleElement(writer, "EndTime", endTime != null ? getEndTimeStr() : null);

		} finally {
			TimeUtility.releaseFormatterToPool(df);
		}
	}

	@Override
	public synchronized String getFullName()
    {
        final StringBuilder temp = new StringBuilder();

		temp.append((getName() == null) ? "No_Name" : getName());

        temp.append('/');
        
        /*
         * Add PID to full name in order to ensure uniqueness.
         */
        final int pid = GdsSystemProperties.getJavaVmPid();

        temp.append(pid);
        
        temp.append('/');
        
		try
        {
			temp.append(key.getHost() + "." + user);
		}
        catch (final IllegalStateException e)
        {
			temp.append("NO_VENUE");
		}

        temp.append('/');

		temp.append(getStartTimeStr() == null ? "No_Time" : getStartTimeStr());
		
		temp.append('/');

		temp.append(getFragment());


		return temp.toString();
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ContextIdentification)) {
			return false;
		}

		final IContextIdentification config = (IContextIdentification) o;

		return config.getFullName().equals(getFullName());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getFullName().hashCode();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getFullName();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.template.Templatable#setTemplateContext(java.util.Map)
	 */
	@Override
	public synchronized void setTemplateContext(final Map<String, Object> map) {
	    
	    map.put("fullName", getFullName());
	    
	    if (getSpacecraftId() != null) {
            map.put("spacecraftId", getSpacecraftId());
        }
	    
		if (key.getNumber() != null) {
			map.put("contextNumber", key.getNumber());
		}
		
		map.put("contextFragment", key.getFragment());
	
		if (name != null) {
			map.put("contextName", name);
		} else {
			map.put("contextName", "");
		}

		if (type != null) {
			map.put("contextType", type);
		} else {
			map.put("contextType", "");
		}

		if (description != null) {
			map.put("contextDescription", description);
		} else {
			map.put("contextDescription", "");
		}

		if (key.getHost() != null) {
			map.put("host", key.getHost());
		} else {
			map.put("host", "");
		}

		if (key.getHostId() != null) {
			map.put("hostId", key.getHostId());
		}

		if (user != null) {
			map.put("user", user);
		} else {
			map.put("user", "");
		}

		if (startTime != null) {
			map.put("startTime", getStartTimeStr());
		} else {
			map.put("startTime", "");
		}

		if (getEndTimeStr() != null) {
			map.put("endTime", getEndTimeStr());
		}
		// No else. Do not include endTime if it has no value

	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.metadata.context.IContextKey#isDirty()
	 */
	@Override
	public boolean isDirty() {
		return this.dirty;
	}

    @Override
    public synchronized ISerializableMetadata getMetadataHeader() {

    	if (this.dirty || this.header == null) {
    		header = new MetadataMap(getContextKey());
    		header.setValue(MetadataKey.CONTEXT_USER, user == null ? "unknown" : user);
    	    header.setValue(MetadataKey.SPACECRAFT_ID, spacecraftId == null ? 0 : getSpacecraftId());
    		this.dirty = false;
    	}
       
        return header;
    }
    
	@Override
	public IContextKey getContextKey() {
		return this.key;
	}

	@Override
	public boolean accept(final IFilterableDataItem data) {
	    if (data instanceof IScidFilterable && this.enableScidCheck) {
	        final Integer sc = ((IScidFilterable)data).getScid();
	        if (!scidFilter.accept(sc == null ? 
	                null : UnsignedInteger.valueOf(sc.intValue()))) {
	            return false;
	        }
	    }
	    return true;
	}

}
