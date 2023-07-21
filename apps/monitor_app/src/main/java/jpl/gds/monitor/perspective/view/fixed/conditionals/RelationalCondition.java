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
package jpl.gds.monitor.perspective.view.fixed.conditionals;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.IImmutableLocalSolarTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;

/**
 * Evaluates relational conditions < > <= >= == !=
 */
public class RelationalCondition extends ConditionConfiguration {
    
    /**
     * Constructor: creates a new Condition Configuration object of type 
     * Relational Condition and sets the member variables
     * 
     * @param conditionId is the unique identifier for this condition object
     * @param channelId identifies the channel whose value is to be used for 
     *                  evaluation
     * @param source is the source field in the channel to examine
     * @param comparison is the comparison operator
     * @param value is the value to compare against (allowed values depend on 
     *              the ChannelType and source)
     */
    public RelationalCondition(final String conditionId, final String channelId, 
            final SourceField source, final Comparison comparison, final String value){
        super(conditionId, channelId, source, comparison, value);
    }
    
    /**
     * Default constructor
     */
    public RelationalCondition() {
        super();
    }


    @Override
    public boolean evaluate(final ApplicationContext appContext) {
        
        final IChannelDefinition def = appContext.getBean(IChannelDefinitionProvider.class).getDefinitionFromChannelId(channelId);
        if (def == null) {
            return false;
        }
        
        // get latest ChannelSample for channelId associated with this condition
    	/*
    	 * Realtime recorded filter in the perspective and
    	 * is now enum rather than boolean and DSS ID is required. 
    	 * However, there is currently no way to get the station ID in this object, so I have 
    	 * had to set it to 0 temporarily. Also, the method used to set the rt/rec filter type 
    	 * here will not work once fixed view preferences are made modifiable at runtime,
    	 * because it is set only upon parsing the perspective.
    	 * get current RT/Rec flag and station
    	 * filter from config
    	 */
    	final MonitorChannelSample data = appContext.getBean(MonitorChannelLad.class).getMostRecentValue(channelId, viewConfig.
    			getRealtimeRecordedFilterType(), viewConfig.getStationId());
        
        if(data == null) {
            return false;
        }
        
        // get the channel type stored in the Channel Definition
        final ChannelType type = def.getChannelType();
        
        /**********************************************************************
         * EU comparison (EUs are always doubles)
         *********************************************************************/
        //only perform comparison if channel has an EU, else return false
        if(source.equals(SourceField.EU)) {
            if(def.hasEu()) {
                final double givenValue = Double.valueOf(value);
                final double recentValue = Double.valueOf(data.getEuValue().getStringValue());
                return compareNumber(recentValue, givenValue);
            }
            else {
                return false;
            }
        }
        

        /**********************************************************************
         * TIME comparisons
         *********************************************************************/
        if(source.equals(SourceField.ERT)) {
            //convert value to IAccurateDateTime
            IAccurateDateTime givenValue = null;
            try {
                givenValue = new AccurateDateTime(value);
            } catch (final ParseException e) {
                TraceManager.getDefaultTracer().error

                ("ERT value caught unhandled and unexpected" +
                        " exception in RelationalCondition.java");
                e.printStackTrace();
            }
            
            final IAccurateDateTime recentValue = data.getErt();
            
            return compareTime(recentValue, givenValue);
        }
        else if(source.equals(SourceField.RCT)) {
            Date givenValue = null;

            final DateFormat f = TimeUtility.getFormatterFromPool();
            try {
                givenValue = f.parse(value);
            } catch (final ParseException e) {
                TraceManager.getDefaultTracer().error

                ("RCT value caught unhandled and unexpected" +
                        " exception in RelationalCondition.java");
                e.printStackTrace();
            } finally {
                TimeUtility.releaseFormatterToPool(f);
            }
            
            final Date recentValue = data.getRct();
            
            return compareTime(recentValue, givenValue);
        }
        else if(source.equals(SourceField.SCET)) {
            // convert value to IAccurateDateTime
            IAccurateDateTime givenValue = null;
            
            try {
                givenValue = new AccurateDateTime(value);
            } catch (final ParseException e) {
                TraceManager.getDefaultTracer().error

                ("SCET value caught unhandled and unexpected" +
                        " exception in RelationalCondition.java");
                e.printStackTrace();
            }
            final IAccurateDateTime recentValue = data.getScet();
            
            return compareTime(recentValue, givenValue);
        }
        else if(source.equals(SourceField.SCLK)) {
            final ISclk givenValue = TimeProperties.getInstance().getSclkFormatter().valueOf(value);
            
            final ISclk recentValue = data.getSclk();
            
            return compareSclk(recentValue, givenValue);
        }
        else if(source.equals(SourceField.LST)) {
            ILocalSolarTime givenValue = null;
            try {
                givenValue = LocalSolarTimeFactory.getNewLst(value, appContext.getBean(IContextIdentification.class).getSpacecraftId());
            } catch (final ParseException e) {
                TraceManager.getDefaultTracer()
                        .error("LST value caught unhandled and unexpected" + " exception in RelationalCondition.java");
                e.printStackTrace();
            }
            final ILocalSolarTime recentValue = data.getSol();
            return compareSol(recentValue, givenValue);
        }
        
        /**********************************************************************
         * STRING comparisons: 
         * There are 3 cases for string comparisons:
         *  1. channel type is ASCII
         *  2. source to be compared is STATUS
         *  3. source to be compared is VALUE and the channel type is STATUS 
         *     or BOOL
         *********************************************************************/
        else if(type.isStringType() || 
                source.equals(SourceField.STATUS) || 
                (source.equals(SourceField.VALUE) && 
                        (type.equals(ChannelType.STATUS) || 
                        type.equals(ChannelType.BOOLEAN)))) {
            String recentValue = null;
            
            //DN, Raw, Value, Status
            switch(source) {
            case DN:
            case RAW:
                recentValue = data.getDnValue().getStringValue();
                break;
            case STATUS:
                if (def.getChannelType().equals(ChannelType.STATUS) || 
                        def.getChannelType().equals(ChannelType.BOOLEAN)) {
                    recentValue = data.getEuValue().getStringValue();
                }
                break;
            case VALUE:
                recentValue = data.getEuValue().getStringValue();
                break;
            }
            
            return compareString(recentValue);
        }

        /**********************************************************************
         * NUMERIC comparisons
         * 
         * integer, float and double comparisons are done separately to avoid 
         * precision issues when comparing (i.e. comparing a float with a 
         * double will fail if the double is storing more precision)
         *********************************************************************/
        //INTEGER
        else if(type.isIntegralType() || type.equals(ChannelType.BOOLEAN)) {
            double recentValue;
            double givenValue;
            
            try {
                //if source is VALUE and the channel has an EU, the EU should be evaluated, not the DN
                try {
                    if(source.equals(SourceField.VALUE) && def.hasEu()) {
                        recentValue = Integer.valueOf(data.getEuValue().getStringValue());
                    }
                    else {
                        recentValue = Integer.valueOf(data.getDnValue().getStringValue());
                    }
                } catch (final NumberFormatException e) {
                    if(source.equals(SourceField.VALUE) && def.hasEu()) {
                        recentValue = Double.valueOf(data.getEuValue().getStringValue());
                    }
                    else {
                        recentValue = Double.valueOf(data.getDnValue().getStringValue());
                    }
                }
                //GDR parse_int accepts hex as well
                try {
                    givenValue = GDR.parse_int(value);
                }
                catch (final NumberFormatException e){
                    givenValue = Double.valueOf(value);
                }
                
                return compareNumber(recentValue, givenValue);
            }catch(final NumberFormatException e) {
                TraceManager.getDefaultTracer()
                        .error("RELATIONAL Condition has invalid value attribute. ConditionID "
                        + conditionId + " expected value to be a number");

            }
        }
        //FLOAT
        else if(type.equals(ChannelType.FLOAT)) {
            double recentValue;
            final double givenValue = Double.valueOf(value);
            
            try {
                //if source is VALUE and the channel has an EU, the EU should be evaluated, not the DN
                if(source.equals(SourceField.VALUE) && def.hasEu()) {
                    recentValue = Double.valueOf(data.getEuValue().getStringValue());
                }
                else {
                    recentValue = Double.valueOf(data.getDnValue().getStringValue());
                }
                
                return compareNumber(recentValue, givenValue);
            }catch(final NumberFormatException e) {
                TraceManager.getDefaultTracer()
                        .error("RELATIONAL Condition has invalid value attribute. ConditionID "
                        + conditionId + " expected value to be a number");

            }
        }
      
        
        return false;
    }
    
    /**
     * Helper function: compares two ILocalSolarTime objects based on the 
     * Comparison type
     * 
     * @param recentValue is the current value in the lad
     * @param givenValue is specified by the user
     * @return true if evaluation is true, false otherwise
     */
    private boolean compareSol(final ILocalSolarTime recentValue, final ILocalSolarTime givenValue) {
        if(recentValue == null) {
            return false;
        }
        
        switch(comparison) {
        case LT:
            return recentValue.compareTo((IImmutableLocalSolarTime)givenValue) < 0;
        case GT:
            return recentValue.compareTo((IImmutableLocalSolarTime)givenValue) > 0;
        case LE:
            return recentValue.compareTo((IImmutableLocalSolarTime)givenValue) < 0 || recentValue.equals(givenValue);
        case GE:
            return recentValue.compareTo((IImmutableLocalSolarTime)givenValue) > 0 || recentValue.equals(givenValue);
        case EQ:
            return recentValue.equals(givenValue);
        case NE:
            return !recentValue.equals(givenValue);  
        default:
            return false;
        }
    }

    /**
     * Helper function: compares two IAccurateDateTime objects based on the 
     * Comparison type
     * 
     * @param recentValue is the current value in the lad
     * @param givenValue is specified by the user
     * @return true if evaluation is true, false otherwise
     */
    private boolean compareTime(final IAccurateDateTime recentValue, final IAccurateDateTime givenValue) {
        switch(comparison) {
        case LT:
            return recentValue.compareTo(givenValue) < 0;
        case GT:
            return recentValue.compareTo(givenValue) > 0;
        case LE:
            return recentValue.compareTo(givenValue) < 0 || recentValue.equals(givenValue);
        case GE:
            return recentValue.compareTo(givenValue) > 0 || recentValue.equals(givenValue);
        case EQ:
            return recentValue.equals(givenValue);
        case NE:
            return !recentValue.equals(givenValue);
        default:
            return false;
        }
    }
    
    /**
     * Helper function: compares two Date objects based on the Comparison type
     * 
     * @param recentValue is the current value in the lad
     * @param givenValue is specified by the user
     * @return true if evaluation is true, false otherwise
     */
    private boolean compareTime(final Date recentValue, final Date givenValue) {
        if(recentValue == null) {
            return false;
        }
        switch(comparison) {
        case LT:
            return recentValue.before(givenValue);
        case GT:
            return recentValue.after(givenValue);
        case LE:
            return recentValue.before(givenValue) || recentValue.equals(givenValue);
        case GE:
            return recentValue.after(givenValue) || recentValue.equals(givenValue);
        case EQ:
            return recentValue.equals(givenValue);
        case NE:
            return !recentValue.equals(givenValue);
        default:
            return false;
        }
    }
    
    /**
     * Helper function: compares two ISclk objects based on the Comparison type
     * 
     * @param recentValue is the current value in the lad
     * @param givenValue is specified by the user
     * @return true if evaluation is true, false otherwise
     */
    private boolean compareSclk(final ISclk recentValue, final ISclk givenValue) {
        switch(comparison) {
        case LT:
            return recentValue.compareTo(givenValue) < 0;
        case GT:
            return recentValue.compareTo(givenValue) > 0;
        case LE:
            return recentValue.compareTo(givenValue) < 0 || recentValue.equals(givenValue);
        case GE:
            return recentValue.compareTo(givenValue) > 0 || recentValue.equals(givenValue);
        case EQ:
            return recentValue.equals(givenValue);
        case NE:
            return !recentValue.equals(givenValue);
        default:
            return false;
        }
    }
    
    /**
     * Helper function: compares two String objects based on the Comparison type
     * 
     * @param recentValue is the current value in the lad
     * @return true if evaluation is true, false otherwise
     */
    private boolean compareString(final String recentValue) {
        switch(comparison) {
        case LT:
            return (recentValue != null && recentValue.compareTo(value) < 0);
        
        case GT:
            return (recentValue != null && recentValue.compareTo(value) > 0);
        case LE:
            return (recentValue != null && 
                    (recentValue.compareTo(value) < 0 || 
                            recentValue.compareTo(value) == 0));
        case GE:
            return (recentValue != null && 
                    (recentValue.compareTo(value) > 0 || 
                            recentValue.compareTo(value) == 0));
        case EQ:
            return (recentValue != null && recentValue.compareTo(value) == 0);
        case NE:
            return (recentValue != null && recentValue.compareTo(value) != 0);
        default:
            return false;
        }
    }
    
    /**
     * Helper function: compares two numbers based on the Comparison type
     * 
     * @param recentValue is the current value in the lad
     * @param givenValue is specified by the user
     * @return true if evaluation is true, false otherwise
     */
    private boolean compareNumber(final double recentValue, final double givenValue) {
        switch(comparison) {
        case LT:
            return recentValue < givenValue;
        case GT:
            return recentValue > givenValue;
        case LE:
            return recentValue <= givenValue;
        case GE:
            return recentValue >= givenValue;
        case EQ:
            return recentValue == givenValue;
        case NE:
            return recentValue != givenValue;
        default:
            return false;
        }
    }

    @Override
    public boolean evaluate(final ApplicationContext appContext, final int stalenessInterval) {
        return false;
    }
}
