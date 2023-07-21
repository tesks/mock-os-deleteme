/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.globallad.io;

import jpl.gds.eha.api.channel.IAlarmValueSet;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.channel.alarm.IAlarmValueSetFactory;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.evr.api.EvrMetadata;
import jpl.gds.evr.api.EvrMetadataKeywordEnum;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.globallad.data.EhaGlobalLadData;
import jpl.gds.globallad.data.EvrGlobalLadData;
import jpl.gds.globallad.data.GlobalLadDataException;
import jpl.gds.globallad.data.GlobalLadUserDatatypeConverter;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.service.GlobalLadConversionException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;

/**
 * Copied from globallad_connector module. There's a subtle incompatibility, and it was not desirable to make changes to
 * that component.
 */
public class MessageToGladDataConverter {

    private static final Tracer                log = TraceManager.getTracer(Loggers.GLAD);
    private final        IAlarmValueSetFactory alarmSetFactory;

    /**
     * Constructor
     *
     * @param alarmValueSetFactory
     */
    public MessageToGladDataConverter(final IAlarmValueSetFactory alarmValueSetFactory) {
        this.alarmSetFactory = alarmValueSetFactory;
    }

    /**
     * Convert message to GLAD data
     *
     * @param message message
     * @param scid    scid
     * @param venue   venue
     * @return glad data
     * @throws GlobalLadConversionException
     */
    public IGlobalLADData convert(final IMessage message, final int scid, final String venue) throws GlobalLadConversionException {
        try {
            if (message.isType(EvrMessageType.Evr)) {
                return createEvr(message, scid, venue);
            } else if (message.isType(EhaMessageType.AlarmedEhaChannel)) {
                return createEha(message, scid, venue);
            }
        } catch (final GlobalLadConversionException e) {
            throw e;
        } catch (final Exception e) {
            throw new GlobalLadConversionException(e);
        }

        // If we get here the message type is not supported.
        throw new GlobalLadConversionException(
                "Unsupported message type for global lad data conversion: " + message.getType());
    }

    /**
     * Create EVR GLAD data from message
     *
     * @param message evr message
     * @param scid    scid
     * @param venue   venue
     * @return glad data
     * @throws GlobalLadConversionException
     */
    private IGlobalLADData createEvr(final IMessage message, final int scid, final String venue) throws
                                                                                     GlobalLadConversionException {
        final IEvrMessage       m    = (IEvrMessage) message;
        final IEvr              evr  = m.getEvr();
        final ISclk             sclk = evr.getSclk();
        final IAccurateDateTime ert  = evr.getErt();
        final IAccurateDateTime scet = evr.getScet();

        /*
         * MPCS-8041 Mar 21 2016 Lavin Zhang
         * Added conditional to stop processing of bad EVRs
         */
        if (evr.isBadEvr()) {
            log.debug(String.format("Skipping bad EVR ID:%s", evr.getEventId()));
            return null;
        }

        /**
         * MPCS-7888 - 1/21/2016 - triviski - Including evr metadata.
         */
        final EvrMetadata md = evr.getMetadata();

        /**
         * MPCS-7900 - triviski 1/25/2016 - Checking the definition to make sure it is valid.
         */
        if (evr.getName() == null) {
            /**
             * This means the def is bad and this will cause a NPE in the create.  Throw here with
             * a valid error message.
             */
            throw new GlobalLadConversionException("EVR definition is not properly set: " + m.getOneLineSummary());
        }
        final long sid = m.getContextKey().getNumber() == null ? -1L : m.getContextKey().getNumber();

        final IGlobalLADData data = new EvrGlobalLadData(
                evr.getEventId(), // evr id
                evr.getLevel().intern(), // evrLevel,
                evr.getName(), // evrName,
                evr.isRealtime(), // isRealTime,
                !evr.isFromSse(), // isFsw
                evr.getMessage(), // evr message
                md.getMetadataValue(EvrMetadataKeywordEnum.TASKNAME),
                md.getMetadataValue(EvrMetadataKeywordEnum.SEQUENCEID),
                md.getMetadataValue(EvrMetadataKeywordEnum.CATEGORYSEQUENCEID),
                md.getMetadataValue(EvrMetadataKeywordEnum.ADDRESSSTACK),
                md.getMetadataValue(EvrMetadataKeywordEnum.SOURCE),
                md.getMetadataValue(EvrMetadataKeywordEnum.TASKID),
                md.getMetadataValue(EvrMetadataKeywordEnum.ERRNO),
                sclk.getCoarse(), // sclkCoarse,
                sclk.getFine(), // sclkFine,
                ert.getTime(), // ertMilliseconds,
                ert.getNanoseconds(),
                scet.getTime(),
                scet.getNanoseconds(),
                sid,
                scid,
                venue,
                (byte) evr.getDssId(),
                evr.getVcid().byteValue(), //vcid,
                m.getContextKey().getHost().intern()
        );

        GlobalLadUserDatatypeConverter.setUserDataTypeFromData(data);

        return data;
    }

    /**
     * Create EHA GLAD data from message
     *
     * @param message message
     * @param scid    scid
     * @param venue   venue
     * @return glad data
     * @throws GlobalLadDataException
     * @throws GlobalLadConversionException
     */
    private IGlobalLADData createEha(final IMessage message, final int scid, final String venue) throws
                                                                                     GlobalLadDataException,
                                                                                     GlobalLadConversionException {
        final IAlarmedChannelValueMessage m = (IAlarmedChannelValueMessage) message;

        final IClientChannelValue cv = m.getChannelValue();

        /**
         * MPCS-7900 - triviski 1/25/2016 - Checking the definition to make sure it is valid.
         */
        if (cv.getChanId() == null) {
            /**
             * This means the def is bad and this will cause a NPE in the create.  Throw here with
             * a valid error message.
             */
            throw new GlobalLadConversionException("Channel definition has not bee properly set: " + message);
        }


        final ISclk             sclk = cv.getSclk();
        final IAccurateDateTime ert  = cv.getErt();
        final IAccurateDateTime scet = cv.getScet();

        /**
         * In the case that we are running in no database mode the session number will be null.  It needs to be
         * checked so it can be added properly.
         *
         * TODO:  If no database is set should it go to the lad?
         */
        final long sid = m.getContextKey().getNumber() == null ? -1L : m.getContextKey().getNumber();

        boolean isHeader  = false;
        boolean isFsw     = false;
        boolean isMonitor = false;
        boolean isSse     = false;

        /**
         * Only fsw can be recorded.
         */
        boolean isRealtime = true;

        /**
         * Must use the definition type and not the enum that is part of the channel.  That is for the
         * database and is not set properly at runtime.
         */
        switch (cv.getDefinitionType()) {
            case FSW:
                isFsw = true;
                isRealtime = cv.isRealtime();
                break;
            case H:
                isHeader = true;
                break;
            case M:
                isMonitor = true;
                break;
            case SSE:
                isSse = true;
                break;
            default:
                break;
        }

        /**
         * MPCS-7623 - triviski  8/2015 - Add alarm information.
         *
         * MPCS-7888 - triviski 2/1/2016 - VCID is null for header channels.
         */
        final byte vcid = (byte) (cv.getVcid() == null ? -1 : cv.getVcid());

        // If there are no alarms, we want to create a new empty set.
        final IAlarmValueSet alarms = cv.getAlarms() == null ? alarmSetFactory.create() : cv.getAlarms();

        final IGlobalLADData data = new EhaGlobalLadData(
                cv.getChannelType(), // chanType,
                cv.getChanId().intern(), // channelId,
                cv.getDn(), // dn,
                cv.getEu(), // eu,
                alarms, // alarm set
                isRealtime, // isRealTime,
                isHeader,
                isMonitor,
                isSse,
                isFsw,
                sclk.getCoarse(),
                sclk.getFine(),
                ert.getTime(),
                ert.getNanoseconds(),
                scet.getTime(),
                scet.getNanoseconds(),
                sid,
                scid,
                venue,
                (byte) cv.getDssId(),
                vcid,
                m.getContextKey().getHost().intern(),
                cv.getStatus().intern() // status
        );

        GlobalLadUserDatatypeConverter.setUserDataTypeFromData(data);

        return data;
    }
}
