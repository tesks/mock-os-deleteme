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
package jpl.gds.eha.impl.channel;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.ICategorySupport;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.IAlarmValueSet;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.channel.serialization.Proto3ChanDefType;
import jpl.gds.eha.api.channel.serialization.Proto3ChannelValue;
import jpl.gds.eha.api.channel.serialization.Proto3ChannelValue.*;
import jpl.gds.eha.api.channel.serialization.Proto3Dn;
import jpl.gds.eha.api.channel.serialization.Proto3Dn.DnCase;
import jpl.gds.eha.api.channel.serialization.Proto3DnType;
import jpl.gds.eha.impl.alarm.AlarmValueSet;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.time.*;

import java.text.DateFormat;
import java.util.List;
import java.util.Map;

/**
 * Client (read-only) representation of a channel sample.
 * 
 * @since R8
 */
public class ClientChannelValue implements IClientChannelValue {
    
    private static final String INVALID_DN = "Cannot create DN from bytes for unknown channel type:";
    private static final String ILLEGAL_VALUE = "Illegal value encountered for \"";
    private static final String ILLEGAL_VALUE_SEP = "\": (";
    
    /**
     * A channel Data Number cannot require more than MAX_DN_LEN bytes to represent.
     */
    public static final int MAX_DN_LEN = 512;
    
    /**
     * channel ID
     */
    protected String chanId;
    /**
     * spacecraft clock
     */
    protected ISclk sclk;
    /**
     * data number
     */
    protected Object dn;
    /**
     * spacecraft event time
     */
    protected IAccurateDateTime scet;
    /**
     * earth received time
     */
    protected IAccurateDateTime ert;
    /**
     * local solar time
     */
    protected ILocalSolarTime lst;
    /**
     * channel title
     */
    protected String title;
    /**
     * categories associated with this channel sample
     */
    protected Categories categories = null;
    /**
     * status value, for status/enum and boolean channels
     */
    protected String status;
    /**
     * realtime flag
     */
    protected boolean realtime = true;
    /**
     * engineering unit value
     */
    protected double eu = 0.0;
    /**
     * virtual channel ID
     */
    // triviski - not sure why vcid is allowed to be null..?
    // It seems that the publisher utility requires it to be null sometimes.  This seems like a bad design.
    protected Integer vcid;
    /**
     * station ID
     */
    protected int dssId = StationIdHolder.UNSPECIFIED_VALUE;
    /**
     * record creation time
     */
    protected IAccurateDateTime rct;
    /**
     * channel data type
     */
    protected ChannelType dataType = ChannelType.UNKNOWN;
    /**
     * channel definition type
     */
    protected ChannelDefinitionType definitionType = ChannelDefinitionType.FSW;
    /**
     * Flag indicating there is an engineering unit value
     */
    protected Boolean               hasEu;
    
    /** Alarm set for this channel value */
	protected IAlarmValueSet alarms;
	
	/** DN unit specifier */
	protected String dnUnits;
	/** EU unit specifier */
	protected String euUnits;
	
    /** The Channel definition */
	protected IChannelDefinition chanDef;

    /**
     * Constructor.
     * 
     * @param def
     *            the channel's dictionary definition
     */
    public ClientChannelValue(final IChannelDefinition def) {
        this(def, null);
    }
    
    /**
     * Constructor.
     * 
     * @param def
     *            the channel's dictionary definition
     * @param o
     *            data number value
     */
    public ClientChannelValue(final IChannelDefinition def, final Object o) {
        this(def.getId(), def.getChannelType());
        setChannelDefintion(def);
        dn = o;
    }
    
    
    /**
     * Constructor.
     */
    public ClientChannelValue() {

        ert = null;
        sclk = null;

        dn = null;
        eu = 0.0;
    }
    
    /**
     * Constructor.
     * 
     * @param channelId
     *            the channel identifier
     * @param type
     *            the channel data type
     */
    public ClientChannelValue(final String channelId, final ChannelType type) {
        this();
        chanId = channelId;
        dataType = type;
    }
    
    /**
     * Constructor.
     * 
     * @param channelId
     *            the channel identifier
     * @param type
     *            the channel data type
     * @param o
     *            data number value
     */
    public ClientChannelValue(final String channelId, final ChannelType type, final Object o) {
        this();
        chanId = channelId;
        dataType = type;
        dn = o;
    }
    
    /**
     * Constructor
     * @param msg a protobuf message containing values for a ClientChannelValue
     */
    public ClientChannelValue(final Proto3ChannelValue msg) {
        this();
    	load(msg);
    }

    @Override
    public IAccurateDateTime getScet() {
        return scet;
    }

    @Override
    public IAccurateDateTime getRct() {
        if (rct == null) {
            return null;
        }
        return new AccurateDateTime(rct.getTime());
    }

    @Override
    public int getDssId() {
        return dssId;
    }


    @Override
    public Integer getVcid() {
        return vcid;
    }

    @Override
    public ILocalSolarTime getLst() {
        return lst;
    }

    @Override
    public double getEu() {
        return eu;
    }

    @Override
    public boolean hasEu() {
        if (hasEu != null) {
            return hasEu;
        }
        else if (chanDef != null) {
            return chanDef.hasEu();
        }
        else {
            return false;
        }
    }

    @Override
    public ISclk getSclk() {
        return sclk;
    }

    @Override
    public IAccurateDateTime getErt() {
        return ert;
    }

    @Override
    public String getChanId() {
        return chanDef != null ? chanDef.getId() : chanId;
    }

    @Override
    public byte byteValue() {
        if (!dataType.isNumberType()) {
            return 0;
        }
        if (dn == null) {
            return 0;
        } else {
            return ((Number )dn).byteValue();
        }
    }

    @Override
    public short shortValue() {
        if (!dataType.isNumberType()) {
            return 0;
        }
        if (dn == null) {
            return 0;
        } else {
            return ((Number)dn).shortValue();
        }
    }

    @Override
    public int intValue() {
        if (!dataType.isNumberType()) {
            return 0;
        }
        if (dn == null) {
            return 0;
        } else {
            return ((Number)dn).intValue();
        }
    }

    @Override
    public boolean booleanValue() {
        if (!dataType.isNumberType()) {
            return false;
        }
        if (dn == null) {
            return false;
        } else {
            return longValue() != 0;
        }
    }

    @Override
    public long longValue() {
        if (!dataType.isNumberType()) {
            return 0;
        }
        if (dn == null) {
            return 0;
        } else {
            return ((Number)dn).longValue();
        }
    }

    @Override
    public float floatValue() {
        if (!dataType.isNumberType()) {
            return 0;
        }
        if (dn == null) {
            return 0;
        } else {
            return ((Number)dn).floatValue();
        }
    }


    @Override
    public double doubleValue() {
        if (!dataType.isNumberType()) {
            return 0;
        }
        if (dn == null) {
            return 0;
        } else {
            return ((Number)dn).doubleValue();
        }
    }

    @Override
    public String stringValue() {
        if (dn == null) {
            return "";
        }
        return dn.toString();
    }


    @Override
    public Object getDn() {
        return dn;
    }

    @Override
    public int hashCode() {
        return chanDef != null ? chanDef.getId().hashCode() : chanId.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder ret = new StringBuilder("Channel Value: " + getChanId());
        ret.append(" (" + getChannelType() + ")");
        if (dn != null) {
            final String str = dn.toString();
            ret.append(" DN: " + str);
        } else {
            ret.append("null");
        }
        /* Use hasEu() on channel value rather than on definition. */
        if (hasEu()) {
            ret.append(" EU: " + eu);
        }
        return new String(ret);
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public byte[] getDnAsBytes() {
        byte[] stuff = null;
        switch (dataType) {
        case FLOAT:
            if (dn instanceof Float) {
                stuff = new byte[4];
                GDR.set_float(stuff, 0, (Float)dn);
            } else {
                stuff = new byte[8];
                GDR.set_double(stuff, 0, (Double)dn);
            }
            break;
        case BOOLEAN:
            stuff = new byte[1];
            final boolean val = booleanValue();
            GDR.set_u8(stuff, 0, val ? 1 : 0);
            break;
        case SIGNED_INT:
        case STATUS:
            if (dn instanceof Byte) {
                stuff = new byte[1];
                GDR.set_i8(stuff, 0, (Byte)dn);
            } else if (dn instanceof Short) {
                stuff = new byte[2];
                GDR.set_i16(stuff, 0, (Short)dn);
            } else if (dn instanceof Integer) {
                stuff = new byte[4];
                GDR.set_i32(stuff, 0, (Integer)dn);
            } else {
                stuff = new byte[8];
                GDR.set_i64(stuff, 0, (Long)dn);
            }
            break;
        case UNSIGNED_INT:
        case DIGITAL:
        case TIME:
            if (dn instanceof Byte) {
                stuff = new byte[1];
                GDR.set_u8(stuff, 0, (Byte)dn);
            } else if (dn instanceof Short) {
                stuff = new byte[2];
                GDR.set_u16(stuff, 0, (Short)dn);
            } else if (dn instanceof Integer) {
                stuff = new byte[4];
                GDR.set_u32(stuff, 0, (Integer)dn);
            } else {
                stuff = new byte[8];
                GDR.set_u64(stuff, 0, (Long)dn);
            }
            break;
        case ASCII:
            stuff = new byte[((String)dn).length()];
            GDR.set_string_no_pad(stuff, 0, (String)dn);
            break;
            default:
                throw new IllegalStateException(INVALID_DN + dataType);

        }
        return stuff;
    }
    

    @Override
    public boolean equals(final Object that) {
        if (that == null || !(that instanceof ClientChannelValue)) {
            return false;
        }
        final ClientChannelValue val = (ClientChannelValue)that;
        return dn.equals(val.dn);
    }
    
    /**
     * 11/20/2017 - I need to be able to serialize these channel values with the alarms, so I am
     * moving the alarm value set to the base implementation.  This is better anyway because at some point the clients 
     * will be able to get the entire alarm history.
     */
    
	/**
	 * @param isOnEu
	 * @return
	 */
	private AlarmLevel computeAlarmLevel(final boolean isOnEu) {
	    AlarmLevel result = AlarmLevel.NONE;
        if (alarms == null) {
            return result;
        }
        final IAlarmValueSet yellowSet = alarms.getAlarmSet(AlarmLevel.YELLOW, isOnEu);
        final IAlarmValueSet redSet = alarms.getAlarmSet(AlarmLevel.RED, isOnEu);
        if (redSet.inAlarm()) {
            result = AlarmLevel.RED;
        } else if (yellowSet.inAlarm()) {
            result = AlarmLevel.YELLOW;
        } 

        return result;
	}
	
	@Override
	public IAlarmValueSet getAlarms() {
		return alarms;
	}


	@Override
	public AlarmLevel getDnAlarmLevel() {
		return computeAlarmLevel(false);
	}

	@Override
	public AlarmLevel getEuAlarmLevel() {
		return computeAlarmLevel(true);
	}


	@Override
	public String getDnAlarmState() {
	    return computeAlarmState(false);
	}

	@Override
	public String getEuAlarmState() {
	    return computeAlarmState(true);
	}

	private String computeAlarmState(final boolean isOnEu) {
	    if (alarms == null) {
            return null;
        }
        String result = null;
        final IAlarmValueSet yellowSet = alarms.getAlarmSet(AlarmLevel.YELLOW, isOnEu);
        final IAlarmValueSet redSet = alarms.getAlarmSet(AlarmLevel.RED, isOnEu);
        if (redSet.inAlarm()) {
            result = getAlarmStateString(redSet.getAlarmValueList());
        } else if (yellowSet.inAlarm()) {
            result = getAlarmStateString(yellowSet.getAlarmValueList());
        } 
        return result;
	}
	
	private String getAlarmStateString(final List<IAlarmValue> alarmSet) {
		final StringBuilder buf = new StringBuilder();
		for (int index = 0; index < alarmSet.size(); index++) {
			buf.append(alarmSet.get(index).getState());
			if (index != alarmSet.size() - 1) {
				buf.append(":");
			}
		}
		return buf.toString();
	}


    @Override
    public boolean isRealtime() {
        return realtime;
    }

    @Override
    public ChannelDefinitionType getDefinitionType() {
        return chanDef != null ? chanDef.getDefinitionType() : definitionType;
    }
    
    /**
     * Sets the data number form an object.
     * 
     * @param o
     *            DN object
     */
    protected void setDn(final Object o) {
        dn = o;
    }
    
    /**
     * Sets the data number from a byte array.
     * 
     * @param stuff
     *            byte array containing the DN
     * @param len
     *            byte length of the DN
     */
    protected void setDnFromBytes(final byte[] stuff, final int len) {
        switch (dataType) {
        case FLOAT:
            if (len == 4) {
                setDn(GDR.get_float(stuff, 0));
            } else {
                setDn(GDR.get_double(stuff, 0));
            }
            break;
        case BOOLEAN:          
            final int val = GDR.get_u8(stuff, 0);
            setDn(val);
            break;
        case SIGNED_INT:
        case STATUS:
            if (len == 1) {
                setDn(GDR.get_i8(stuff, 0));
            } else if (len == 2) {
                setDn(GDR.get_i16(stuff, 0)); 
            } else if (len == 4) {
                setDn(GDR.get_i32(stuff, 0));
            } else {
                setDn(GDR.get_i64(stuff, 0));
            }
            break;
        case UNSIGNED_INT:
        case DIGITAL:
        case TIME:
            if (len == 1) {
                setDn(GDR.get_u8(stuff, 0));
            } else if (len == 2) {
                setDn(GDR.get_u16(stuff, 0)); 
            } else if (len == 4) {
                setDn(GDR.get_u32(stuff, 0));
            } else {
                setDn(GDR.get_u64(stuff, 0));
            }
            break;
        case ASCII:
            if (stuff.length == 0) {
                setDn("");
            } else {
                setDn(GDR.get_string(stuff, 0, stuff.length));
            }
            break;
            default:
                throw new IllegalStateException(INVALID_DN + dataType);
        }
    }
    
    @Override
    public String getCategory(final String categoryName) {
        if (chanDef == null && categories == null) {
             return null;
        }
        return chanDef != null ? chanDef.getCategory(categoryName) : categories.getCategory(categoryName);
    }

    void setCategory(final String categoryName, final String categoryValue) {
        if(categories == null){
            categories = new Categories();
        }
        categories.setCategory(categoryName, categoryValue);
    }
    
    @Override
    public String getEscapedCsv() {
        final StringBuilder builder = new StringBuilder(128);

        if (sclk != null) {
            builder.append(sclk.toString()); 
            builder.append(CSV_SEPARATOR);
            builder.append(sclk.getCoarse());
            builder.append(CSV_SEPARATOR);
            builder.append(sclk.getFine());
            builder.append(CSV_SEPARATOR);
            builder.append(sclk.getBinaryGdrLong());
        } else {
            builder.append(CSV_SEPARATOR);
            builder.append("0");
            builder.append(CSV_SEPARATOR);
            builder.append("0");
            builder.append(CSV_SEPARATOR);
            builder.append("0");
        }
        builder.append(CSV_SEPARATOR);

        if (ert != null) {
            builder.append(ert.getFormattedErt(true));
            builder.append(CSV_SEPARATOR);
            builder.append(ert.getTime());
            builder.append(CSV_SEPARATOR);
            builder.append(ert.getNanoseconds());
        } else {
            builder.append(CSV_SEPARATOR);
            builder.append("0");
            builder.append(CSV_SEPARATOR);
            builder.append("0");
        }
        builder.append(CSV_SEPARATOR);

        if (scet != null) {
            final DateFormat df = TimeUtility.getFormatterFromPool();
            builder.append(df.format(scet));
            builder.append(CSV_SEPARATOR);
            TimeUtility.releaseFormatterToPool(df);
            builder.append(scet.getTime());

            builder.append(CSV_SEPARATOR);
            builder.append(scet.getNanoseconds());
        } else {
            builder.append(CSV_SEPARATOR);
            builder.append("0");
            // need extra separator, to keep everything in place
            builder.append(CSV_SEPARATOR);
        }
        builder.append(CSV_SEPARATOR);

        if (lst != null) {
            builder.append(lst.getFormattedSol(true));
            builder.append(CSV_SEPARATOR);
            builder.append(lst.getSolExact());

        } else {
            builder.append(CSV_SEPARATOR);
            builder.append("0");
        }
        builder.append(CSV_SEPARATOR);

        builder.append(realtime);
        builder.append(CSV_SEPARATOR);


            builder.append(getChanId());
            builder.append(CSV_SEPARATOR);

            builder.append(dataType.toString());
            builder.append(CSV_SEPARATOR);

            if (title != null) {
                builder.append(getTitle());
            }
            builder.append(CSV_SEPARATOR);
            
            if (getCategory(ICategorySupport.MODULE) != null) {
                builder.append(getCategory(ICategorySupport.MODULE));
            }
            builder.append(CSV_SEPARATOR);

            if (getDnUnits() != null) {
                builder.append(getDnUnits());
            }
            builder.append(CSV_SEPARATOR);

            if (getEuUnits() != null) {
                builder.append(getEuUnits());
            }
            builder.append(CSV_SEPARATOR);
        

        if (dn != null) {

            if (status != null) {
                builder.append(getStatus());
            }
            builder.append(CSV_SEPARATOR);

            if (dataType.equals(ChannelType.ASCII) &&
                    (((String)dn).length() > MAX_DN_LEN))
            {
                builder.append(((String)dn).substring(0, MAX_DN_LEN));
            }
            else
            {
                builder.append(dn.toString());
            }
            builder.append(CSV_SEPARATOR);

            /* Use hasEu() on channel value rather than on definition. */
            if (hasEu()) {
                builder.append(getEu());
            }

            /* 
             *  MTAK monitor channel with
             * chill_down session specific DSSID doesn't have the correct 
             * get_eha dssId field populated
             */
             builder.append(CSV_SEPARATOR);

        } else {
            builder.append(CSV_SEPARATOR);
        }

        /* 
         * MTAK monitor channel with
         * chill_down session specific DSSID doesn't have the correct 
         * get_eha dssId field populated
         */
        if (getDssId() >= 0) {
            builder.append(getDssId());
        }

        builder.append(CSV_SEPARATOR);
        if (getVcid() != null) {
            builder.append(getVcid());
        }

        
        /* R8 Refactor TODO - I changed this such that the simpler
         * alarm state information is used, and there is only one entry
         * for the DN or EU rather than the whole alarm set. How does this
         * affect the MTAK interface?
         */
        final AlarmLevel dnAlarmLevel = computeAlarmLevel(false);
        if (dnAlarmLevel != AlarmLevel.NONE) {
            builder.append(CSV_SEPARATOR);
            builder.append(computeAlarmState(false));
            builder.append(CSV_SEPARATOR);
            builder.append(dnAlarmLevel.toString());
        }
        
        final AlarmLevel euAlarmLevel = computeAlarmLevel(true);
        if (euAlarmLevel != AlarmLevel.NONE) {
            builder.append(CSV_SEPARATOR);
            builder.append(computeAlarmState(true));
            builder.append(CSV_SEPARATOR);
            builder.append(euAlarmLevel);
        }

        return builder.toString();  
    }


    @Override
    public void setTemplateContext(final Map<String,Object> map) {

        map.put("channelId", getChanId());
        
        //  Changed to put the enums in the map rather than
        // strings. Some templates making calls in these objects.
        map.put("valType", dataType);
        map.put("channelType", dataType);

        map.put("opsCategory",getCategory(IChannelDefinition.OPS_CAT) != null ? getCategory(IChannelDefinition.OPS_CAT) : "");
        map.put("subsystem", getCategory(IChannelDefinition.SUBSYSTEM) != null ? getCategory(IChannelDefinition.SUBSYSTEM) : "");
        map.put("module", getCategory(IChannelDefinition.MODULE) != null ? getCategory(IChannelDefinition.MODULE) : "");

        if (dn != null) {
            map.put("preFormatted", false);
            /* Use hasEu() on channel value rather than on definition. */
            if (hasEu()) {
                map.put("hasEu", true);
                map.put("eu", getEu());
            } else {
                map.put("hasEu", false);
            }

            map.put("dataNumber", dn);

            if (dataType.equals(ChannelType.ASCII)  && ((String)dn).length() > MAX_DN_LEN) {
                map.put("dataNumber", ((String)dn).substring(0, MAX_DN_LEN));
            }

            map.put("status", getStatus());
        } else {
            map.put("dataNumber", "");
        }

        // Add $fswName variable for old templates
        final String temp = getTitle();
        if (temp != null) {
            map.put("title", temp);
            map.put("name", temp);
            map.put("fswName", temp);
        } else {
            map.put("title", "");
            map.put("name", "");
            map.put("fswName", "");
        }
        if (ert != null) {
            map.put("ert", ert.getFormattedErt(true));
            map.put("ertExact", ert.getTime());
            map.put("ertExactFine", ert.getNanoseconds());
        } else {
            map.put("ert", "");
            map.put("ertExact",0);
            map.put("ertExactFine",0);
        }
        if (sclk != null) {
            map.put("sclk", sclk);
            map.put("sclkCoarse",sclk.getCoarse());
            map.put("sclkFine",sclk.getFine());
            map.put("sclkExact",sclk.getBinaryGdrLong());
        } else {
            map.put("sclk","");
            map.put("sclkCoarse",0);
            map.put("sclkFine",0);
            map.put("sclkExact",0);
        }
        if (scet != null) {
            map.put("scet", scet.getFormattedScet(true));
            map.put("scetExact",scet.getTime());
            map.put("scetExactFine",scet.getNanoseconds());
        } else {
            map.put("scet", "");
            map.put("scetExact",0);
            map.put("scetExactFine", 0);
        }
        if (lst != null) {
            map.put("lst", lst.getFormattedSol(true));
            map.put("lstExact",lst.getSolExact());
            map.put("lstExactFine",lst.getSolNumber());
        } else {
            map.put("lst","");
            map.put("lstExact",0);
            map.put("lstExactFine", 0);
        }
        if (rct != null) {
            map.put("rct", rct.getFormattedErt(true));
            map.put("rctExact", rct.getTime());
        } else {
            // Correct variable names for empty RCT
            map.put("rct", "");
            map.put("rctExact",0);
            map.put("rctExactFine",0);
        }

        //null value is checked in velocity template
        map.put("vcid", vcid);
        map.put("dssId", dssId);

        map.put("inAlarm", false);
        
        // Added alarms in template context.
        final AlarmValueSet templateAlarms = new AlarmValueSet();
        final AlarmLevel dnAlarmLevel = computeAlarmLevel(false);
        if (dnAlarmLevel != AlarmLevel.NONE) {
            map.put("inAlarm", true);
            map.put("dnAlarmLevel", dnAlarmLevel.toString());
            map.put("dnAlarmState", computeAlarmState(false));
            templateAlarms.addAlarmSet(getAlarms().getAlarmSet(false));
        }

        final AlarmLevel euAlarmLevel = computeAlarmLevel(true);
        if (euAlarmLevel != AlarmLevel.NONE) {
            map.put("inAlarm", true);
            map.put("euAlarmLevel", euAlarmLevel.toString());
            map.put("euAlarmState", computeAlarmState(true));
            templateAlarms.addAlarmSet(getAlarms().getAlarmSet(true));
        } 
        
        if(!templateAlarms.getAlarmValueList().isEmpty()) {
        	map.put("alarms", templateAlarms.getAlarmValueList());
        }

        map.put("realTime", realtime);
        
        if (getDnUnits() != null) {
            map.put("dnUnits", getDnUnits());
        } else {
            map.put("dnUnits", "");
        }
             
        if (getEuUnits() != null) {
            map.put("euUnits", getEuUnits());
        } else {
            map.put("euUnits", "");
        }
    }
    
    @Override
    public String getTitle() {
        return this.chanDef != null? chanDef.getTitle() : this.title;
    }

    @Override
    public AlarmLevel getWorstAlarmLevel() {
 
        if (getDnAlarmLevel().compareTo(getEuAlarmLevel()) > 0) {
            return getDnAlarmLevel();
        } else {
            return getEuAlarmLevel();
        }
    }
    
    /**
     * Copies members from this object to another client channel value.
     * 
     * @param ecdr
     *            object to copy to
     */
    protected void copyMembersTo(final ClientChannelValue ecdr) {
        ecdr.chanDef = chanDef;
        ecdr.ert = ert;
        ecdr.sclk = sclk;
        ecdr.scet = scet;
        ecdr.lst = lst;
        ecdr.realtime = realtime;
        ecdr.rct = rct;
        ecdr.dssId = dssId;
        ecdr.vcid = vcid;
        ecdr.dn = dn;
        ecdr.eu = eu;
        ecdr.alarms = alarms;
        ecdr.status = status;
        ecdr.title = getTitle();
        ecdr.chanId = getChanId();
        ecdr.dataType = getChannelType();
        ecdr.categories = ecdr.chanDef == null ? categories : getChannelDefinition().getCategories();
        ecdr.definitionType = getDefinitionType();
        ecdr.hasEu = hasEu();
        //  Added copy final of units
        ecdr.dnUnits = getDnUnits();
        ecdr.euUnits = getEuUnits();

    }
    
    /**
     * Gets the Channel Definition
     * 
     * @return <IChannelDefinition>
     */
    protected IChannelDefinition getChannelDefinition() {
        return this.chanDef;
    }
    
    /**
     * Sets the <IChannelDefinition> for this channel value 
     * @param inDef The <IChannelDefinition> to set
     */
    protected void setChannelDefintion(final IChannelDefinition inDef) {
        this.chanDef = inDef;
        this.chanId = chanDef.getId();
        this.dataType = chanDef.getChannelType();
        this.title = chanDef.getTitle();
        this.categories = chanDef.getCategories();
        this.definitionType = chanDef.getDefinitionType();
        this.hasEu = chanDef.hasEu();
        //  Added copy of units
        this.dnUnits = chanDef.getDnUnits();
        this.euUnits = chanDef.getEuUnits();
    }

    @Override
    public ChannelType getChannelType() {
        return chanDef != null ? chanDef.getChannelType() : dataType;
    }
    
    @Override
    public Proto3ChannelValue build() {
		/*
         * Set non calculated values in message
         */
        final Proto3ChannelValue.Builder retVal = Proto3ChannelValue.newBuilder();
        retVal.setTitle(getTitle())
			.setChannelId(getChanId())
			.setChanDefType(Proto3ChanDefType.valueOf("CHAN_DEF_TYPE_" + getDefinitionType().name()))
			.setDn(getDnAsProto())
		    .setDssId(getDssId())
            .setIsRealtime(isRealtime());
        
        if (getErt() != null) {
		    retVal.setErt(getErt().buildAccurateDateTime());
        }

        if (getSclk() != null) {
		    retVal.setSclk(getSclk().buildSclk());
        }
        
        if (alarms != null) {
        		retVal.setAlarms(alarms.getProto());
        }
        
        if(hasEu()){
        	retVal.setEu(getEu());
        }
        
        if(getVcid() != null) {
        		retVal.setVcid(getVcid());
        }
        
        if(getRct() != null){
		    retVal.setRct(getRct().buildAccurateDateTime());
        }
        
        /**
         * triviski 7/12/2017 - Not all channels have LST.  Check if it is null first.
         */
        if (null != getLst()) {
        	retVal.setLst(getLst().buildLocalSolarTime());
        }
        
        if(getScet() != null){
        	retVal.setScet(getScet().buildAccurateDateTime());
        }
        else {
        	retVal.setScet(new AccurateDateTime().buildAccurateDateTime());
        }
        
        if (null != getStatus()) {
        	retVal.setStatus(getStatus());
        }
        
        // Add units fields to the proto object
        if (getDnUnits() != null) {
            retVal.setDnUnits(getDnUnits());
        }
        
        if (getEuUnits() != null) {
            retVal.setEuUnits(getEuUnits());
        }

        // set categories to message
        if(getCategory(ICategorySupport.SUBSYSTEM) != null) {
            retVal.setSubsystem(getCategory(ICategorySupport.SUBSYSTEM));
        }
        if(getCategory(ICategorySupport.OPS_CAT) != null) {
            retVal.setOpsCat(getCategory(ICategorySupport.OPS_CAT));
        }
        if(getCategory(ICategorySupport.MODULE) != null) {
            retVal.setModule(getCategory(ICategorySupport.MODULE));
        }

        return retVal.build();
	}
    
    /**
     * Populate the channel value from a protobuf message, which should have been created using
     * build().
     * 
     * @param msg the protobuf message containing the channel value
     */
    public void load(final Proto3ChannelValue msg){
        this.title = msg.getTitle();
        this.dataType = ChannelType.valueOf(msg.getDn().getType().name().substring("DN_TYPE_".length()));
        this.dn = getDnFromProto(msg.getDn());
        this.chanId = msg.getChannelId();

		if(msg.getHasEuCase().equals(HasEuCase.EU)){
			this.eu = msg.getEu();
            this.hasEu = true;
		}
		this.dssId = msg.getDssId();
		if(msg.getHasVcidCase().equals(HasVcidCase.VCID)){
			this.vcid = msg.getVcid();
		}
		this.realtime = msg.getIsRealtime();
        if (msg.getHasErtCase().equals(HasErtCase.ERT)) {
            this.ert = new AccurateDateTime(msg.getErt());
        }
		if(msg.getHasRctCase().equals(HasRctCase.RCT)){
			this.rct = new AccurateDateTime(msg.getRct().getMilliseconds());
		}
		
		// check for null before populating LST
		if(msg.getHasLstCase().equals(HasLstCase.LST)) {
   		    this.lst = LocalSolarTimeFactory.getNewLstNoScid(msg.getLst().getMilliseconds(), msg.getLst().getSol());
		}
        if (msg.getHasScetCase().equals(HasScetCase.SCET)) {
            this.scet = new AccurateDateTime(msg.getScet());
        }
        if (msg.getHasSclkCase().equals(HasSclkCase.SCLK)) {
            this.sclk = new Sclk(msg.getSclk());
        }
		
		/**
		 * Only set the alarms if they are set
		 */
		switch(msg.getHasAlarmsCase()) {
		case ALARMS:
			this.alarms = new AlarmValueSet(msg.getAlarms());
			break;
		case HASALARMS_NOT_SET:
			this.alarms = null;
			break;			
		}
		
		// set units fields from message
        this.dnUnits = msg.getDnUnits();
        this.euUnits = msg.getEuUnits();

        // Set categories and status from message
        setCategory(ICategorySupport.SUBSYSTEM, msg.getSubsystem());
        setCategory(ICategorySupport.OPS_CAT, msg.getOpsCat());
        
        // Don't set module for header or monitor channel
        if (!(msg.getChanDefType().equals(Proto3ChanDefType.CHAN_DEF_TYPE_H)
                || msg.getChanDefType().equals(Proto3ChanDefType.CHAN_DEF_TYPE_M))) {
            setCategory(ICategorySupport.MODULE, msg.getModule());
        }
        this.status = msg.getStatus();
        switch (msg.getChanDefType()) {
            case CHAN_DEF_TYPE_H:
                this.definitionType = ChannelDefinitionType.H;
                break;
            case CHAN_DEF_TYPE_M:
                this.definitionType = ChannelDefinitionType.M;
                break;
            case CHAN_DEF_TYPE_SSE:
                this.definitionType = ChannelDefinitionType.SSE;
                break;
            case UNRECOGNIZED:
            case CHAN_DEF_TYPE_UNKNOWN:
            case CHAN_DEF_TYPE_FSW:
                this.definitionType = ChannelDefinitionType.FSW;
                break;
        }
    }
    
    /**
     * Get the DN value in a protocol buffer message
     * @return a Proto3Dn format protocol buffer message containing the DN value
     */
    protected Proto3Dn getDnAsProto(){
        /*
         * Set appropriate DN Value based upon DN Type
         */
		final Proto3Dn.Builder prot3Dn = Proto3Dn.newBuilder();
		prot3Dn.setType(Proto3DnType.valueOf("DN_TYPE_" + dataType.name()));
        switch (dataType) {
            case ASCII:
            	prot3Dn.setString(dn.toString());
            	break;
            case BOOLEAN:
                if (dn instanceof Byte) {
                    prot3Dn.setBool(((Byte)dn) != 0);
                }
                else if (dn instanceof Integer) {
                    prot3Dn.setBool(((Integer)dn) != 0);
                }
                else if (dn instanceof Long) {
                    prot3Dn.setBool(((Long)dn) != 0);
                }
            	else if (dn instanceof String) {
            		prot3Dn.setBool(Boolean.valueOf((String)dn));
            	}
                else if (null == dn) {
            		prot3Dn.setBool(false);
            	}
                else {
                	throw new RuntimeException(ILLEGAL_VALUE + dataType + ILLEGAL_VALUE_SEP + dn.getClass() + ")" + dn);
                }
                break;
            case SIGNED_INT:
                if (dn instanceof Byte) {
                    prot3Dn.setInt((Byte) dn);
                }
                else if (dn instanceof Short) {
                    prot3Dn.setInt((Short) dn);
                }
                else if (dn instanceof Integer) {
                    prot3Dn.setInt((Integer) dn);
                }
                else if (dn instanceof Long) {
                    prot3Dn.setLong((Long) dn);
                }
                else {
                	throw new RuntimeException(ILLEGAL_VALUE + dataType + ILLEGAL_VALUE_SEP + dn.getClass() + ")" + dn);
                }
                break;
            case STATUS:
            case UNSIGNED_INT:
            case DIGITAL:
            case TIME:
                if (dn instanceof Byte) {
                    prot3Dn.setUint((Byte) dn);
                }
                else if (dn instanceof Short) {
                    prot3Dn.setUint((Short) dn);
                }
                else if (dn instanceof Integer) {
                    prot3Dn.setUint((Integer) dn);
                }
                else if (dn instanceof Long) {
                    prot3Dn.setUlong((Long) dn);
                }
                else {
                	throw new RuntimeException(ILLEGAL_VALUE + dataType + ILLEGAL_VALUE_SEP + dn.getClass() + ")" + dn);
                }
                break;
            case FLOAT:
                if (dn instanceof Float) {
                    prot3Dn.setFloat((Float) dn);
                }
                else if (dn instanceof Double) {
                    prot3Dn.setDouble((Double) dn);
                }
                else {
                	throw new RuntimeException(ILLEGAL_VALUE + dataType + ILLEGAL_VALUE_SEP + dn.getClass() + ")" + dn);
                }
                break;
            case UNKNOWN:
            default:
                break;
            
        }
        
        return prot3Dn.build();
	}
    
    /**
     * Get a DN object from a protocol buffer message
     * @param prot3Dn the protobuf message containing a DN value
     * @return a DN object
     */
    protected Object getDnFromProto(final Proto3Dn prot3Dn){
    	
    	Object tempDn;
    	
    	final ChannelType inType = ChannelType.valueOf(prot3Dn.getType().name().substring("DN_TYPE_".length()));
    	if(!dataType.equals(inType)){
    		throw new IllegalStateException("Cannot create DN from bytes for channel type " + dataType + " with a supplied value type of " + inType);
    	}
    	
        switch (dataType) {
            case ASCII:
            	tempDn = prot3Dn.getString();
            	break;
            case BOOLEAN:
                tempDn = prot3Dn.getBool() ? 1 : 0;
                break;
            case SIGNED_INT:
                if (prot3Dn.getDnCase().equals(DnCase._INT)) {
                    tempDn = prot3Dn.getInt();
                }
                else {
                    tempDn = prot3Dn.getLong();
                }
                break;
            case STATUS:
            case UNSIGNED_INT:
            case DIGITAL:
            case TIME:
                if (prot3Dn.getDnCase().equals(DnCase._UINT)) {
                    tempDn = prot3Dn.getUint();
                }
                else {
                    tempDn = prot3Dn.getUlong();
                }
                break;
            case FLOAT:
                if (prot3Dn.getDnCase().equals(DnCase._FLOAT)) {
                    tempDn = prot3Dn.getFloat();
                }
                else {
                    tempDn = prot3Dn.getDouble();
                }
                break;
            case UNKNOWN:
            default:
            	throw new IllegalStateException(INVALID_DN + dataType);
        }
        
        return tempDn;
	}

    @Override
    public String getDnUnits() {
        return this.chanDef != null ? this.chanDef.getDnUnits() : this.dnUnits;
    }

    @Override
    public String getEuUnits() {
        return this.chanDef != null ? this.chanDef.getEuUnits() : this.euUnits;
    }

}
