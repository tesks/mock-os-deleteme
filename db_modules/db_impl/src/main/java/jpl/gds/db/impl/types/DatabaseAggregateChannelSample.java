package jpl.gds.db.impl.types;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.CsvQueryProperties;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.time.FastDateFormat;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.TimeUtility;

public class DatabaseAggregateChannelSample extends DatabaseChannelSample {
	
    public DatabaseAggregateChannelSample(final ApplicationContext appContext, final Long sessionId,
            final Boolean fromSse,
			final Boolean isRealtime, final ISclk sclk, final IAccurateDateTime ert, final IAccurateDateTime scet, final ILocalSolarTime sol,
			final Object v, final ChannelType ct, final String cid, final Long cindex, final String module, final String sessionHost, final Double eu,
			final String dnAlarm, final String euAlarm, final String status, final Integer scid, final String name, final Integer dssId, final Integer vcid,
			final IAccurateDateTime rct, final PacketIdHolder packetId, final Long frameId) {
		
        super(appContext, sessionId, fromSse, isRealtime, sclk, ert, scet, sol, v, ct, cid, cindex, module, sessionHost,
              eu, dnAlarm, euAlarm, status, scid, name, dssId, vcid, rct, packetId, frameId);
	}

	/**
     * {@inheritDoc}
     * 
     * NOTE: Hayk This is a copy of toCsv but made thread safe for aggregates,
     * need to merge this with toCsv at some point and get rid of the duplication
     *
     * @version MPCS-7587 Add named VCID column.
     * 
     */
    @Override
    public String toCsv(final List<String> csvColumns)
    {
        final StringBuilder csv  = new StringBuilder(1024);
        final StringBuilder csv2 = new StringBuilder(1024);

    	csv.setLength(0);

		csv.append(CSV_COL_HDR);

        for (final String upcce : csvColumns)
        {
            csv.append(CSV_COL_SEP);

            switch (upcce)
            {
                case "SESSIONID":
                    if (sessionId != null)
                    {
                        csv.append(sessionId);
                    }
                    break;

                case "SESSIONHOST":
                    if (sessionHost != null)
                    {
                        csv.append(sessionHost);
                    }
                    break;

                case "CHANNELID":
                   if (channelId != null)
                   {
                       csv.append(channelId);
                   }
                   break;

                case "DSSID":
                    /* 
                     * MPCS-6349 : DSS ID not set properly
                     * Removed dssId. Parent class has been updated with 
                     * protected fields sessionDssId and recordDssId with get/set 
                     * methods for both.
                     */
                    csv.append(recordDssId);
                    break;

                case "VCID":
                    if (vcid != null)
                    {
                        csv.append(vcid);
                    }
                    break;

                case "NAME":
                    if (name != null)
                    {
                        csv.append(name);
                    }
                    break;

                case "MODULE":
                    if (module != null)
                    {
                        csv.append(module);
                    }
                    break;

                case "ERT":
                    if (ert != null)
                    {
                        csv.append(ert.getFormattedErt(true));
                    }
                    break;

                case "SCET":
                    if (scet != null)
                    {
                        csv.append(scet.getFormattedScet(true));
                    }
                    break;

                case "LST":
                    if (useSolTime && (lst != null))
                    {
                        csv.append(lst.getFormattedSolFast(true));
                    }
                    break;

                case "SCLK":
                    if (sclk != null)
                    {
                        csv.append(sclk);
                    }
                    break;

                case CsvQueryProperties.DN:
                    if (value != null)
                    {
                        if (useFormatters && (dnFormat != null))
                        {
                            csv.append(formatUtil.anCsprintf(dnFormat, value).trim());
                        }
                        else
                        {
                            csv.append(value);
                        }
                    }
                    break;

                case "EU":
                    if (eu != null)
                    {
                        if (useFormatters && (euFormat != null))
                        {
                            csv.append(formatUtil.anCsprintf(euFormat, eu).trim());
                        }
                        else
                        {
                            csv.append(eu);
                        }
                    }
                    break;

                case "STATUS":
                    if ((channelType != null)        &&
                        channelType.hasEnumeration() &&
                        (status != null))
                    {
                        if (useFormatters && (euFormat != null))
                        {
                            csv.append(formatUtil.anCsprintf(euFormat, status).trim());
                        }
                        else
                        {
                            csv.append(status);
                        }
                    }
                    break;

                case "DNALARMSTATE":
                    if (dnAlarmState != null)
                    {
                        csv.append(dnAlarmState);
                    }
                    break;

                case "EUALARMSTATE":
                    if (euAlarmState != null)
                    {
                        csv.append(euAlarmState);
                    }
                    break;

                case "REALTIME":
                    if (isRealtime != null)
                    {
                        csv.append(isRealtime);
                    }
                    break;

                case CsvQueryProperties.TYPE:
                    if (channelType != null)
                    {
                        csv.append(channelType);
                    }
                    break;

                case "APID":
                    if (hasPacket && (apid != null))
                    {
                        csv.append(apid);
                    }
                    break;

                case "APIDNAME":
                    if (hasPacket          &&
                        (apidName != null) &&
                        ! apidName.isUnsupported())
                    {
                        csv.append(apidName);
                    }
                    break;

                case "SPSC":
                    if (hasPacket && (spsc != null))
                    {
                        csv.append(spsc);
                    }
                    break;

                case "PACKETRCT":
                    if (hasPacket && (packetRct != null))
                    {
                        csv.append(TimeUtility.format(packetRct));
                    }
                    break;

                case "SOURCEVCFC":
                    if (hasPacket      &&
                        (vcfc != null) &&
                        ! vcfc.isUnsupported())
                    {
                        csv.append(vcfc);
                    }
                    break;

                case "RCT":
                   if (rct != null)
                   {
                       csv.append(FastDateFormat.format(rct, calendar, csv2));
                   }
                   break;

                //MPCS-7587 - Add named VCID column to csv.
                case "VCIDNAME":
                	// MPCS-8021  - updated for better parsing
                	if(missionProperties.shouldMapQueryOutputVcid() && vcid != null){
                		csv.append(missionProperties.mapDownlinkVcidToName(this.vcid));
                	} else {
                		csv.append("");
                	}

                	break;

                default:

                	// MPCS-7587 - Add named VCID column to csv.
                	// MPCS-8021  - updated for better parsing
                	// Put here due to the configurable nature of the column name
                	if(missionProperties.getVcidColumnName().toUpperCase().equals(upcce))
                	{
                		if(missionProperties.shouldMapQueryOutputVcid() && vcid != null){
                			csv.append(missionProperties.mapDownlinkVcidToName(this.vcid));
                		} else {
                			csv.append("");
                		}
                	}
                	else if (! csvSkip.contains(upcce))
                	{
                		log.warn("Column " + 
                				upcce       +
                				" is not supported, skipped");

                		csvSkip.add(upcce);
                	}

                    break;
            }
        }

		csv.append(CSV_COL_TRL);

		return csv.toString();
    }
    
    /**
     * {@inheritDoc}
     * 
     * @version MPCS-7587 Add named VCID column.
     */
    @Override
    public void setTemplateContext(final Map<String, Object> map)
    {
        super.setTemplateContextCommon(map);

        if(fromSse != null)
        {
            map.put("fromSse",fromSse);
        }

        if(isRealtime != null)
        {
            map.put("isRealTime",isRealtime);
        }


        if (rct != null)
        {
            // rct should never be null from the database

            map.put("rct", FastDateFormat.format(rct, null, null));
        }

        if (ert != null)
        {
            map.put("ert", ert.getFormattedErt(true));
            map.put("ertExact", ert.getTime());
            map.put("ertExactFine", ert.getNanoseconds());
        }

        if (scet != null)
        {
            map.put("scet", scet.getFormattedScet(true));
            map.put("scetExact", scet.getTime());
            map.put("scetExactFine", scet.getNanoseconds());
        }


        if (useSolTime && (lst != null))
        {
            map.put("lst", lst.getFormattedSol(true));
            map.put("lstExact", lst.getTime());
            map.put("lstExactFine", lst.getSolNumber());
        }

        if (sclk != null)
        {
            map.put("sclk", sclk);
            map.put("sclkCoarse",sclk.getCoarse());
            map.put("sclkFine",sclk.getFine());
        }
        if (module != null) {
            map.put("module", module);
        }

        if (value != null)
        {
            /*
             * MPCS-5526. Formatting data number in templates causes
             * template errors. Changed this to add two variables to the template for
             * data number: one formatted and the other unformatted, so templates can
             * choose which one to use. 
             */
            if (useFormatters && dnFormat != null) {
                map.put("formattedDataNumber", formatUtil.anCsprintf(dnFormat, value));
            } else {
                map.put("formattedDataNumber", value); 
            }
            map.put("dataNumber", value);
            map.put("channelId", (channelId != null) ? channelId : "");
            map.put("channelIndex", channelIndex);
            map.put("name", name);
            map.put("channelType",getChannelType().getBriefChannelType());
        }

        if (eu != null) {
            /*
             * MPCS-5526. Formatting EU in templates causes
             * template errors. Changed this to add two variables to the template for
             * EU: one formatted and the other unformatted, so templates can
             * choose which one to use. 
             */
            if (useFormatters && euFormat != null) {
                map.put("formattedEu", formatUtil.anCsprintf(euFormat, eu));
            } else {
                map.put("formattedEu", eu);
            }
            map.put("eu", eu);
        }

        if (dnAlarmState != null)
        {
            map.put("dnAlarmState", dnAlarmState);


            if (dnAlarmState.equalsIgnoreCase("RED"))
            {
                map.put("redDnType",  "EXCLUSIVE");
                map.put("redDnAlarm", "HIGH");
            }
            else if (dnAlarmState.equalsIgnoreCase("YELLOW"))
            {
                map.put("yellowDnType",  "EXCLUSIVE");
                map.put("yellowDnAlarm", "HIGH");
            }
        }

        if (euAlarmState != null) {
            map.put("euAlarmState", euAlarmState);
        }

        if (deltaValue != null) {
            map.put("delta", deltaValue);
        }
        if (previousValue != null) {
            map.put("previous", previousValue);
        }

        if (channelType != null)
        {
            map.put("channelType",      channelType);
            map.put("channelShortType", channelType.getBriefChannelType());
        }

        /*
         * MPCS-5526. Formatting status in templates causes
         * template errors. Changed this to add two variables to the template for
         * status: one formatted and the other unformatted, so templates can
         * choose which one to use. 
         */
        if (status != null) {
            if (useFormatters && euFormat != null) {
                map.put("formattedStatus", formatUtil.anCsprintf(euFormat, status));
            } else {
                map.put("formattedStatus", status);
            }
            map.put("status", status);
        }
        
        /* 
         * MPCS-6349 : DSS ID not set properly
         * Removed dssId. Parent class has been updated with 
         * protected fields sessionDssId and recordDssId with get/set 
         * methods for both.
         */
        map.put("dssId", recordDssId);
        
        if (vcid != null)
        {
            map.put("vcid", vcid);
        }
        
        //MPCS-7587 - add mapping of VCID name
        // MPCS-8021 - updated for efficiency
        if (missionProperties.shouldMapQueryOutputVcid() && this.vcid != null)
        {
        	map.put(missionProperties.getVcidColumnName(),
        			missionProperties.mapDownlinkVcidToName(this.vcid));
        }

        map.put("spacecraftID",spacecraftId);
        map.put("spacecraftName", missionProperties.mapScidToName(spacecraftId));

        if (hasPacket)
        {
            map.put("hasPacket", true);

            if (apid != null)
            {
                map.put("apid", apid);
            }

            if ((apidName != null) && ! apidName.isUnsupported())
            {
                map.put("apidName", apidName);
            }

            if (spsc != null)
            {
                map.put("spsc", spsc);
            }

            if (packetRct != null)
            {
                map.put("packetRct", TimeUtility.format(packetRct));
            }

            if ((vcfc != null) && ! vcfc.isUnsupported())
            {
                map.put("vcfc", vcfc);
            }
        }
    }
	
}
