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
package jpl.gds.monitor.guiapp.gui.views.nattable;

import java.util.UUID;

import jpl.gds.dictionary.api.ICategorySupport;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.evr.api.EvrMetadataKeywordEnum;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.message.api.MessageUtility;
import jpl.gds.monitor.config.GlobalPerspectiveParameter;
import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.config.MonitorConfigValues.SclkFormat;
import jpl.gds.monitor.guiapp.common.gui.DisplayConstants;
import jpl.gds.monitor.guiapp.common.gui.INatListItem;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.time.TimeUtility;

/**
 * A NAT List item wrapper for EVR messages. The data in this object reflects
 * one row in an EVR view. <p><br>
 * IMPORTANT: The names of methods in this class are not arbitrary. Methods that
 * return data to be displayed in a NAT table MUST be named by taking the
 * official table column name in the perspective configuration, replacing spaces
 * by underscores, and converting all to upper case. This gives the column
 * property name. The accessor method for that column must then be named getXXX
 * where XXX is the property name. In addition, none of the accessor methods may
 * return null. If there is no value, and empty string should be returned for
 * string properties, and a 0 value for numeric properties. Finally, accessor
 * methods may only return String, Long, and Double values.
 * methods may only return String, Long, and Double values.
 *
 */
public class EvrNatListItem implements INatListItem {
    
    private final MonitorConfigValues monitorConfig;
    
    private final IEvrMessage msg;
    private final IEvr evr;
    private final IEvrDefinition evrDef;
    private boolean mark;
    private final long receiptTime;   
    private final UUID uuid;

	private MonitorConfigValues configVals;
          
    /**
     * Constructor.
     * @param configVals the current monitor configuration values object
     * 
     * @param msg2 the EVR message containing the EVR this list item will wrap
     */
    public EvrNatListItem(final MonitorConfigValues configVals, final IEvrMessage msg2) {
    	monitorConfig = configVals;
        this.receiptTime = System.currentTimeMillis();
        uuid = new UUID(this.receiptTime, this.hashCode());
        this.msg = msg2;
        this.evr = msg2.getEvr();
        this.evrDef = evr.getEvrDefinition();       
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.gui.INatListItem#getUUID()
     */
    @Override
    public UUID getUUID() {
        return uuid;
    }
    
    /**
     * Gets the EVR object associated with this data item.
     * 
     * @return IEvr object
     */
    public IEvr getEvr() {
        return this.evr;
    }
    
    /**
     * Gets the EVR definition associated with this data item.
     * 
     * @return IEvrDefinition
     */
    public IEvrDefinition getDefinition() {
        return this.evrDef;
    }
    
    /**
     * Gets the recorded state (true or false) associated with this data item as
     * a boolean. This method is not used for display of data in the NAT Table, but
     * rather for filtering.
     * 
     * @return true or false string
     */
    public boolean getRecordedAsFlag() {
        return !this.evr.isRealtime();
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.gui.INatListItem#setMark(boolean)
     */
    @Override
    public void setMark(final boolean enable) {
        this.mark = enable;    
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.gui.INatListItem#isMarked()
     */
    @Override
    public boolean isMarked() {
        return this.mark;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.gui.INatListItem#getReceiptTime()
     */
    @Override
    public long getReceiptTime() {
        return this.receiptTime;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.gui.INatListItem#getRecordIdString()
     */
    @Override
    public String getRecordIdString() {
        return String.valueOf(getID());
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.gui.INatListItem#getRecordDetailString()
     */
    @Override
    public String getRecordDetailString() {
        try {
            return MessageUtility.getMessageText(this.msg, "xml");
        } catch (final TemplateException e) {
            e.printStackTrace();
        }
    
        return "Unable to format EVR content";
    }

    /* 
     * START property accessors. Do not rename any method below this
     * point unless you know what you are doing. No method below this
     * point may return null, either.
     */
    
    /**
     * Gets the session ID associated with this data item
     * @return session ID
     */
    public Long getSESSION_ID() {
        return this.msg.getContextKey().getNumber() == null ? 0L : this.msg.getContextKey().getNumber();
    }

    /**
     * Gets the session host associated with this data item.
     * @return session host name
     */
    public String getSESSION_HOST() {
        final String host =  this.msg.getContextKey().getHost();
        if (host == null) {
            return "";
        }
        return host;
    }

    /**
     * Gets the EVR event ID.
     * 
     * @return event ID
     */
    public long getID() {
        return this.evr.getEvrDefinition().getId();
    }
    
    /**
     * Gets the EVR message string associated with this data item. 
     * 
     * @return message string
     */
    public String getMESSAGE() {
        if (this.evr.getMessage() == null) {
            return "";
        }
        return this.evr.getMessage();
    }
    
    /**
     * Gets the formatted SCLK associated with this data item.
     * 
     * @return formatted SCLK string
     */
    public String getSCLK() {
        if (this.evr.getSclk() == null) {
            return "";
        }

        if (monitorConfig.getValue(GlobalPerspectiveParameter.SCLK_FORMAT).equals(SclkFormat.DECIMAL)) {
            return this.evr.getSclk().toDecimalString();
        } else {
            return this.evr.getSclk().toTicksString(); 
        }
            
    }
    
    /**
     * Gets the formatted record creation time (RCT) associated with this EVR item.
     * 
     * @return formatted RCT time string
     */
    public String getRCT() {
        if (this.evr.getRct() == null) {
            return "";
        }
        return TimeUtility.format(this.evr.getRct());
    }
    
    /**
     * Gets the formatted SCET associated with this data item.
     * 
     * @return formatted SCET string
     */
    public String getSCET() {
        
        if (this.evr.getScet() == null) {
            return "";
        }
        
        return this.evr.getScet().getFormattedScetFast(true);
    }
    
    
    /**
     * Gets the formatted ERT associated with this data item.
     * 
     * @return formatted ERT string
     */
    public String getERT() {
        
        if (this.evr.getErt() == null) {
            return "";
        }
        
        return this.evr.getErt().getFormattedErtFast(true);
    }
    
    /**
     * Gets the formatted LST associated with this data item.
     * 
     * @return formatted LST string
     */
    public String getLST() {
        if (this.evr.getSol() == null) {
            return "";
        }
        return this.evr.getSol().getFormattedSolFast(true);
    }
    
    /**
     * Gets the EVR module associated with this data item.
     * 
     * @return module string
     */
    public String getMODULE() {
        if (this.evrDef.getCategory(ICategorySupport.MODULE) == null) {
            return "";
        }
        
        return this.evrDef.getCategory(ICategorySupport.MODULE);
    }
    
    /**
     * Gets the EVR Ops Category associated with this data item.
     * 
     * @return module string
     */
    public String getOPS_CAT() {
        if (this.evrDef.getCategory(ICategorySupport.OPS_CAT) == null) {
            return "";
        }
        
        return this.evrDef.getCategory(ICategorySupport.OPS_CAT);
    }
    
    /**
     * Gets the virtual channel ID associated with this data item.
     * @return VCID
     */
    public Long getVCID() {
        if (this.evr.getVcid() == null) {
            return 0L;
        } 
        return Long.valueOf(this.evr.getVcid().intValue());
    }
    
    /**
     * Gets the station ID associated with this data item.
     * @return DSSID
     */
    public Long getDSS_ID() {
        return Long.valueOf(Math.max(this.evr.getDssId(), 0));
        
    }
    
    /**
     * Gets the EVR level associated with this data item.
     * @return level string
     */
    public String getLEVEL() {
        if (this.evrDef.getLevel() == null) {
            return DisplayConstants.UNKNOWN_EVR_LEVEL;
        }
        return this.evrDef.getLevel();
    }
    
    /**
     * Gets the EVR name associated with this data item.
     * @return name string
     */
    public String getNAME() {
        if (this.evrDef.getName() == null) {
            return "";
        }
        return this.evrDef.getName();
    }
    
    /**
     * Gets the source (FSW or SSE) associated with this data item.
     * @return source string
     */
    public String getSOURCE() {
        if (this.msg.isFromSse()) {
            return "SSE";
            
        } else {
            return "FSW";
        }
    }
    
    /**
     * Gets the recorded state (true or false) associated with this data item as
     * a string.
     * 
     * @return true or false string
     */
    public String getRECORDED() {
        return String.valueOf(!evr.isRealtime());
    }
    
    /**
     * Gets the EVR task name associated with this data item.
     * @return task name string
     */
    public String getTASK_NAME() {
        final String val = this.evr.getMetadata().getMetadataValue(EvrMetadataKeywordEnum.TASKNAME);
        return val == null ? "" : val;
    }
    
    /**
     * Gets the EVR overall sequence ID associated with this data item.
     * @return sequence ID
     */
    public String getSEQ_ID() {
        final String val = this.evr.getMetadata().getMetadataValue(EvrMetadataKeywordEnum.SEQUENCEID);
        return val == null ? "" : val;
    }
    
    /**
     * Gets the EVR category sequence ID associated with this data item.
     * @return sequence ID
     */
    public Long getCATEGORY_SEQ_ID() {
        final String val = this.evr.getMetadata().getMetadataValue(EvrMetadataKeywordEnum.CATEGORYSEQUENCEID);
        return val == null ? 0 : Long.valueOf(val);
    }
}