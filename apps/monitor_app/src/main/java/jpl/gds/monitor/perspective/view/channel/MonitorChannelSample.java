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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.DerivationType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.channel.IChannelDerivation;
import jpl.gds.dictionary.api.client.alarm.IAlarmDictionaryManager;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.DictionaryTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.TimeUtility;

/**
 * ChannelSample is used to encapsulate a single channel value in chill_monitor.
 * This object is created from incoming channel values, either from the database
 * or the message service. From that point on, it is this object that represents
 * a channel sample, or data value, in the monitor.
 */
public class MonitorChannelSample
{
    private static DictionaryTemplateManager templateMgr;
    
	// Values for these fields come from the incoming database or message channel value.
	private ChannelDisplayItem dnValue;
	private ChannelDisplayItem euValue;
	private IAccurateDateTime ert;
	private ISclk sclk;
	private IAccurateDateTime scet;
	private ILocalSolarTime sol;
	private Date rct;
	private AlarmLevel dnAlarmLevel = null;
	private String dnAlarmState = null;
	private AlarmLevel euAlarmLevel = null;
	private String euAlarmState = null;
	private boolean isSampleRealtime = true;
	private Integer vcid = null;
	private int dssId = -1;

	/**
	 *  The channel definition for the channel associated with the data sample
	 */
	private IChannelDefinition chanDef = null;

	/**
	 * Indicates whether this value is from the global LAD.
	 */
	private boolean fromLad = false;

	/**
	 * This is used to track the creation time of this sample in the monitor.
	 */
	private long timestamp;

	/**
	 * Constructor. Object timestamp is set to the current system time.
	 */
	public MonitorChannelSample() {
		timestamp = System.currentTimeMillis();
	}

	/**
	 * Gets the timestamp of this object, indicating when it was created in the monitor.
	 * This is not any of the times associated with the data value itself.
	 * 
	 * @return the timestamp value, milliseconds
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Retrieves the flag indicating whether this channel sample is realtime
	 * or recorded.
	 * 
	 * @return true if the sample is realtime, false if not
	 */
	public boolean isRealtime() {
		return isSampleRealtime;
	}

	/**
	 * Retrieves the flag indicating whether this channel sample is realtime
	 * or recorded.
	 * 
	 * @param isRealtime true if the sample is realtime, false if not
	 */
	public void setRealtime(final boolean isRealtime) {
		this.isSampleRealtime = isRealtime;
	}

	/**
	 * Retrieves the flag indicating whether this sample came from the global LAD.
	 * 
	 * @return true if the sample came from the LAD, false if not
	 */
	public boolean isFromLad() {
		return fromLad;
	}

	/**
	 * Sets the flag indicating whether this sample came from the global LAD.
	 * 
	 * @param fromLad true if the sample came from the LAD, false if not
	 */
	public void setFromLad(final boolean fromLad) {
		this.fromLad = fromLad;
	}

	/**
	 * Gets the channel definition object associated with this channel sample.
	 * 
	 * @return ChannelDefinition object, or null if none set (shouldn't happen) 
	 */
	public IChannelDefinition getChanDef() {
		return chanDef;
	}

	/**
	 * Sets the channel definition object associated with this channel sample.
	 *
	 * @param channelDef The channel definition to set.
	 */
	public void setChanDef(final IChannelDefinition channelDef) {
		chanDef = channelDef;
	}

	/**
	 * Gets the alarm level associated with the DN/RAW portion of this channel sample.
	 * 
	 * @return the DN alarm level.
	 */
	public AlarmLevel getDnAlarmLevel()
	{
		return dnAlarmLevel;
	}
	/**
	 * Sets the alarm level associated with the DN/RAW portion of this channel sample.
	 * 
	 * @param alarmLevel The alarm level to set.
	 */
	public void setDnAlarmLevel(final AlarmLevel alarmLevel)
	{
		dnAlarmLevel = alarmLevel;
	}

	/**
	 * Gets the alarm level associated with the EU/VALUE portion of this channel sample.
	 * 
	 * @return the EU alarm level.
	 */
	public AlarmLevel getEuAlarmLevel()
	{
		return euAlarmLevel;
	}

	/**
	 * Sets the alarm level associated with the EU/VALUE portion of this channel sample.
	 * 
	 * @param alarmLevel The alarm level to set.
	 */
	public void setEuAlarmLevel(final AlarmLevel alarmLevel)
	{
		euAlarmLevel = alarmLevel;
	}

	/**
	 * Gets the channel ID associated with this sample.
	 * 
	 * @return the channel ID string
	 */
	public String getChanId()
	{
		if (chanDef == null) {
			return "";
		}
		return chanDef.getId();
	}

	/**
	 * Gets the DN/Raw value for this channel sample.
	 * 
	 * @return the DN/Raw value as a LadChannelDisplayItem
	 */
	public ChannelDisplayItem getDnValue()
	{
		return dnValue;
	}

	/**
	 * Sets the DN/Raw value for this channel sample.
	 * 
	 * @param dnValue the DN/Raw value as a LadChannelDisplayItem
	 */
	public void setDnValue(final ChannelDisplayItem dnValue)
	{
		if (this.dnValue != null) {
			ChannelDisplayItemPool.getInstance().releaseToPool(this.dnValue);
		}
		this.dnValue = dnValue;
	}

	/**
	 * Gets the EU/Value value for this channel sample.
	 * 
	 * @return the EU/Value value as a LadChannelDisplayItem
	 */
	public ChannelDisplayItem getEuValue()
	{
		return euValue;
	}

	/**
	 * Sets the EU/Value value for this channel sample.
	 * 
	 * @param euValue the EU/Value value as a LadChannelDisplayItem
	 */
	public void setEuValue(final ChannelDisplayItem euValue)
	{
		if (this.euValue != null) {
			ChannelDisplayItemPool.getInstance().releaseToPool(this.euValue);
		}
		this.euValue = euValue;
	}

	/**
	 * Gets the ERT time of this channel sample.
	 * 
	 * @return Returns the ERT time.
	 */
	public IAccurateDateTime getErt()
	{
		return ert;
	}

	/**
	 * Sets the ERT time of this channel sample.
	 * 
	 * @param time The ERT time to set.
	 */
	public void setErt(final IAccurateDateTime time)
	{
		ert = time;
	}

	/**
	 * Gets the RCT time for this channel sample.
	 * 
	 * @return Returns the RCT time.
	 */
	public Date getRct() {
		if (rct == null) {
			return null;
		}
		return new Date(rct.getTime());
	}

	/**
	 * Sets the RCT time for this channel sample.
	 * 
	 * @param time The RCT time to set.
	 */
    public void setRct(final IAccurateDateTime time) {
		if (time == null) {
			rct = null;
		} else {
			rct = new Date(time.getTime());
		}
	}

	/**
	 * Gets the SCLK time for this channel sample.
	 * 
	 * @return the SCLK object
	 */
    public ISclk getSclk()
	{
		return sclk;
	}
	/**
	 * Sets the SCLK time for this channel sample.
	 * 
	 * @param time The SCLK to set.
	 */
	public void setSclk(final ISclk time)
	{
		sclk = time;
	}

	/**
	 * Sets the SCET time for this channel sample.
	 * 
	 * @param time The scet to set.
	 */
	public void setScet(final IAccurateDateTime time)
	{
		scet = time;
	}

	/**
	 * Gets the SCET time for this channel sample.
	 * 
	 * @return the SCET as an IAccurateDateTime object
	 */
	public IAccurateDateTime getScet()
	{
		return scet;
	}

	/**
	 * Sets the local solar time for this channel sample.
	 * 
	 * @param time The ILocalSolarTime to set.
	 */
	public void setSol(final ILocalSolarTime time)
	{
		sol = time;
	}

	/**
	 * Gets the local solar time for this channel sample.
	 * 
	 * @return LocalSolarTiem object
	 */
	public ILocalSolarTime getSol()
	{
		return sol;
	}

	/**
	 * Sets the DN/Raw alarm state string for this channel sample.
	 * 
	 * @param state state string to set
	 */
	public void setDnAlarmState(final String state) {
		dnAlarmState = state;
	}

	/**
	 * Gets the DN/Raw alarm state string for this channel sample.
	 * 
	 * @return state string, or null if value is not in alarm
	 */
	public String getDnAlarmState()
	{
		return dnAlarmState;
	}


	/**
	 * Sets the EU/Value alarm state string for this channel sample.
	 * 
	 * @param state state string to set
	 */
	public void setEuAlarmState(final String state) {
		euAlarmState = state;
	}

	/**
	 * Gets the DN/Raw alarm state string for this channel sample.
	 * 
	 * @return state string, or null if value is not in alarm
	 */
	public String getEuAlarmState()
	{
		return euAlarmState;
	}

	public void setVcid(final Integer vcid) {
		this.vcid = vcid;
	}

	public Integer getVcid() {
		return vcid;
	}

	public void setDssId(final Integer dssId) {
		this.dssId = dssId;
	}

	public int getDssId() {
		return dssId;
	}

	/**
	 * Resets all fields including the timestamp. Returns LadChannelDisplayItems to the pool.
	 */
	protected void reset() {

		final ChannelDisplayItemPool pool = ChannelDisplayItemPool.getInstance();

		chanDef = null;
		dnAlarmLevel = null;
		dnAlarmState = null;

		if (dnValue != null) {
			pool.releaseToPool(dnValue);
			dnValue = null;
		}
		ert = null;
		euAlarmLevel = null;
		euAlarmState = null;

		if (euValue != null) {
			pool.releaseToPool(euValue);
			euValue = null;
		}

		fromLad = false;		 
		isSampleRealtime = false;
		rct = null;
		scet = null;
		sclk = null;
		sol = null;			 

		timestamp = System.currentTimeMillis();

		vcid= null;
		dssId = -1;
	}

	/**
	 * Creates and populates a ChannelSample object from an IChannelValue object.
	 * 
	 * @param val the IChannelValue to get data from
	 * @return the new ChannelSample object
	 */
	public static MonitorChannelSample create(final IChannelDefinitionProvider chanDict, final IClientChannelValue val)
	{
		final String id = val.getChanId();
		final IChannelDefinition def = chanDict.getDefinitionFromChannelId(id);
		final ChannelDisplayItemPool ladItemPool = ChannelDisplayItemPool.getInstance();

		if (def == null) {
			return null;
		}

		final MonitorChannelSample data = new MonitorChannelSample();



		data.setChanDef(def);
		data.setRealtime(val.isRealtime());

		final ChannelDisplayItem dnItem = ladItemPool.getFromPool();
		final Object dnVal = val.getDn();
		dnItem.setFormat(def.getDnFormat());
		dnItem.setValue(dnVal);

		data.setDnValue(dnItem);

		final ChannelDisplayItem euItem = ladItemPool.getFromPool();

		if (def.getLookupTable() != null && (def.getChannelType().equals(ChannelType.STATUS) || 
				def.getChannelType().equals(ChannelType.BOOLEAN))) {

			euItem.setFormat("%s");
			try {
                euItem.setValue(val.getStatus());

			} catch (final Exception e) {
				e.printStackTrace();
			}
			data.setEuValue(euItem);
			/**
			 * Since the definition is getting looked up
			 * there is no reason to use the channel def that is in the channel.  This 
			 * will make the new Global LAD integration work easier.
			 */
		} else if (def.hasEu()) {
			final Object euVal = val.getEu();
			euItem.setFormat(def.getEuFormat());
			euItem.setValue(euVal);
			data.setEuValue(euItem);
		} else {
			euItem.setFormat(def.getDnFormat());
			euItem.setValue(dnVal);
			data.setEuValue(euItem);
		}

		data.setErt(val.getErt());
		data.setSclk(val.getSclk());
		data.setScet(val.getScet());
		data.setSol(val.getLst());
		data.setRct(val.getRct());

		data.setDnAlarmLevel(val.getDnAlarmLevel());
		data.setEuAlarmLevel(val.getEuAlarmLevel());
		data.setDnAlarmState(val.getDnAlarmState());
		data.setEuAlarmState(val.getEuAlarmState());

		data.setVcid(val.getVcid());
		data.setDssId(val.getDssId());

		return data;
	}
	
	/**
     * Gets a formatted channel definition (including latest value of the channel) for display.
     * @param appContext the current application context
     * @param id the Channel ID of the channel to get the definition for
     * @param data the latest sample (current value) for the channel, which may be null.
     * @return formatted text for display
     */
    public synchronized static String getChanDefText(final ApplicationContext appContext, final String id, final MonitorChannelSample data)
    {      
        final StringBuilder chanDefText = new StringBuilder();
        
        final IChannelUtilityDictionaryManager chanTable = appContext.getBean(IChannelUtilityDictionaryManager.class);
        final IAlarmDictionaryManager alarmTable = appContext.getBean(IAlarmDictionaryManager.class);
        final IChannelDefinition def = chanTable.getDefinitionFromChannelId(id);
        final SprintfFormat formatUtil = appContext.getBean(SprintfFormat.class);      
        final DateFormat defaultTimeFormat = TimeUtility.getIsoFormatter(); 
        
        if (def == null) {
            return "There is no definition for channel " + id;

        }
        
        if (data != null) {
            //append raw, value, ert, sclk, scet, sol, alarm
            chanDefText.append("------------------------------\n");
            chanDefText.append("Channel Value: " + data.getChanId() + "\n");
            chanDefText.append("------------------------------\n");
            chanDefText.append("DN/Raw: " + (data.getDnValue() == null || data.getDnValue().getFormattedValue(formatUtil).equals("N / A") ? "Not Available" : data.getDnValue().getStringValue()) + "\n");
            chanDefText.append("DN Units: " +   (def.getDnUnits() == null ? "" : " " + def.getDnUnits()) + "\n");
            if (def != null && def.hasEu()) {
                chanDefText.append("EU: " + (data.getEuValue() == null ? "Not Available" : data.getEuValue().getFormattedValue(formatUtil) + "\n"));
                chanDefText.append("EU Units: " +   (def.getEuUnits() == null ? "" : " " + def.getEuUnits()) + "\n");
            } 
            if (def != null && def.getChannelType().equals(ChannelType.BOOLEAN) || def.getChannelType().equals(ChannelType.STATUS)) {
                chanDefText.append("Status: " + (data.getEuValue() == null ? "Not Available" : data.getEuValue().getFormattedValue(formatUtil) + "\n"));
            } 

            final ChannelDefinitionType type = def.getDefinitionType();

            if (type == null || type.equals(ChannelDefinitionType.FSW) || type.equals(ChannelDefinitionType.SSE)) {
                chanDefText.append("ERT: " + (data.getErt() == null ? "Not Available" : data.getErt().getFormattedErt(true)) + "\n");
                chanDefText.append("SCLK: " + (data.getSclk() == null ? "Not Available" : data.getSclk().toString()) + "\n");
                chanDefText.append("SCET: " + (data.getScet() == null ? "Not Available" : data.getScet().getFormattedScet(true)) + "\n");
                chanDefText.append("LST: " + (data.getSol() == null ? "Not Available" : data.getSol().getFormattedSol(true)) + "\n");
                chanDefText.append("RCT: " + (data.getRct() == null ? "Not Available" : defaultTimeFormat.format(data.getRct())) + "\n");
            } else if (type.equals(ChannelDefinitionType.M)) {
                chanDefText.append("MST: " + (data.getErt() == null ? "Not Available" : data.getErt().getFormattedErt(true)) + "\n");
                chanDefText.append("RCT: " + (data.getRct() == null ? "Not Available" : defaultTimeFormat.format(data.getRct())) + "\n");
            }

            //only show alarm status for dynamic fields for now, since we don't store recent data for static channel fields
            //if a user requests it in the future, it can be added.
            if (data.getDnAlarmLevel() != AlarmLevel.NONE || data.getEuAlarmLevel() != AlarmLevel.NONE) {
                chanDefText.append("In Alarm: " + data.getAlarmState() + "\n");
            }
        }
        
        // store the channel definition, use velocity template for formatting
        if (def != null) {
            try {
                if (templateMgr == null) {
                    try {
                        templateMgr = MissionConfiguredTemplateManagerFactory.getNewDictionaryTemplateManager(appContext.getBean(SseContextFlag.class));
                    } catch (final TemplateException e) {
                        e.printStackTrace();
                        return chanDefText.toString();
                    }
                }

                final Template t = templateMgr.getTemplateForStyle("Channel", "keyvalue");
                if (t == null) {
                    return chanDefText.toString();
                }
                final Map<String,Object> variables = new HashMap<String, Object>();
                variables.put("channelDef", def);
                if (def.isDerived() && chanTable != null) {
                    final IChannelDerivation dev = chanTable.getDerivationForChannelId(def.getId());
                    if (def.getDerivationType() == DerivationType.BIT_UNPACK) {
                        variables.put("bitUnpack", dev);
                    } else if (def.getDerivationType() == DerivationType.ALGORITHMIC) {
                        variables.put("algoDerivation", dev);
                    }
                }
                
                final List<IAlarmDefinition> alarmDefs = alarmTable.getSingleChannelAlarmDefinitions(id);
                if (alarmDefs != null) {
                    variables.put("alarms", alarmDefs);
                }
                final String temp = TemplateManager.createText(t, variables);
                chanDefText.append("\n" + temp);

            } catch (final Exception ex) {
                ex.printStackTrace();
                TraceManager.getDefaultTracer()
                        .error("Error in show definition menu item handling " + ex.toString());
            }
        }

        return chanDefText.toString();
    }

    private String getAlarmState() {

        final AlarmLevel dnLevel = getDnAlarmLevel();
        final AlarmLevel euLevel = getEuAlarmLevel();
        String state = null;
        if (dnLevel != AlarmLevel.NONE) {
            state = "DN-" + getDnAlarmState();
        }
        if (euLevel != AlarmLevel.NONE) {
            if (state == null) {
                state = "EU-" + getEuAlarmState();
            } else {
                state = state + "," + "EU-" + getEuAlarmState();
            }
        }
        if (state == null) {
            state = "";
        }
        return state;
    }



	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		try {
			reset();
		} finally {
			super.finalize();
		}
	}
}

