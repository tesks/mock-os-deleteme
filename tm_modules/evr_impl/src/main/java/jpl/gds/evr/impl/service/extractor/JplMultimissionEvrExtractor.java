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
package jpl.gds.evr.impl.service.extractor;

import jpl.gds.dictionary.api.evr.EvrArgumentType;
import jpl.gds.dictionary.api.evr.IEvrArgumentDefinition;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.evr.api.EvrMetadataKeywordEnum;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.service.extractor.EvrExtractorException;
import jpl.gds.evr.api.service.extractor.IRawEvrData;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.SprintfUtil;
import jpl.gds.shared.string.SprintfUtilException;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;
import org.springframework.context.ApplicationContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * JplMultimissionEvrExtractor is the multimission implementation of the adapter for EVR 
 * processing. Known missions this adapter supports are MSL and SMAP.
 * 
 */
public class JplMultimissionEvrExtractor extends AbstractEvrExtractor {
    /* Use FastTracer throughout for debug messages */
    private static Tracer debugTrace_;
    private static final String RT_RING = "RT";

    private Class<?>[] parameterTypes = null;
    private int[] parameterLengths = null;
    private int[] parameterOffsets = null;
    private List<IRawEvrData> rawParameters = null;
    private IEvr currentEvr = null;

    /**
     * Maximum number of EVR parameters.
     */
    public static final int MAX_PARAMETER_COUNT = 11;

    /**
     * Default constructor
     * 
     * @param context
     *            the current application context
     * 
     * @throws EvrExtractorException
     *             thrown if encountered
     */
    public JplMultimissionEvrExtractor(final ApplicationContext context) throws EvrExtractorException {

        super(context);
        debugTrace_ = TraceManager.getTracer(context, Loggers.TLM_EVR);
        saveFatalKeyword();
        saveSourceFileFlag();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.evr.impl.service.extractor.AbstractEvrExtractor#extractEvr(jpl.gds.tm.service.api.packet.ITelemetryPacketMessage)
     */
    @Override
    public synchronized IEvr extractEvr(final ITelemetryPacketMessage pm) throws EvrExtractorException {
        final ITelemetryPacketInfo pktInfo = pm.getPacketInfo();
        final int apid = pktInfo.getApid();
        final Integer vcid = pktInfo.getVcid();

        final int dssId = pktInfo.getDssId();

        final int seqCount = pktInfo.getSeqCount();

        try {
            final IEvr tempEvr = evrFactory.createEvr();

            final byte[] buff = pm.getPacket();
            lastPm_ = pm;
            tempEvr.setScet(pm.getPacketInfo().getScet());
            tempEvr.setSclk(pm.getPacketInfo().getSclk());
            tempEvr.setSol(pm.getPacketInfo().getLst());
            tempEvr.setErt(pm.getPacketInfo().getErt());
            tempEvr.setRct(pm.getRct());
            tempEvr.setDssId(dssId);
            tempEvr.setVcid(vcid);

            tempEvr.setPacketId(pm.getPacketId());

            debugTrace_.trace("EVR Buffer length ", buff.length);

            // start reading the packet data
            currentOffset = 0;
            
            final int packetHeaderLength = pktInfo.getPrimaryHeaderLength()
            		+ pktInfo.getSecondaryHeaderLength(); // skip packet header and spacecraft clock;
            
            if (buff.length < packetHeaderLength) {
                fail(apid, vcid, dssId, seqCount, currentEvr,
                        "Ran out of bytes in the EVR packet header");
            }
            /** No longer skip headers based on hardcoded value */
            currentOffset += packetHeaderLength;

            extractEvr(buff, currentOffset, buff.length - currentOffset - 1,
                    apid, vcid, dssId, seqCount, tempEvr);

            return currentEvr;

        } catch (final EvrExtractorException e) {
            throw e;

        } catch (final Exception e) {

            e.printStackTrace();
            fail(apid, vcid, dssId, seqCount, currentEvr, "Unexpected error extracting EVR: " + e.toString());
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.evr.impl.service.extractor.AbstractEvrExtractor#extractEvr(jpl.gds.tm.service.api.packet.ITelemetryPacketMessage)
     */
    @Override
    public synchronized IEvr extractEvr(final byte[] buff, final int startOffset,
            final int length, final int apid, final Integer vcid, final int dssId,
            final int seqCount, final IEvr evr) throws EvrExtractorException {
        currentEvr = evr;
        metaKeys = new ArrayList<EvrMetadataKeywordEnum>();
        metaData = new ArrayList<String>();

        currentOffset = startOffset;

        currentEvr.setVcid(vcid);

        try {

            // 6 bytes: task name (or "_INTR_" for interrupt context)
            if ((currentOffset + 6) > buff.length) {
                fail(apid, vcid, dssId, seqCount, currentEvr,
                        "Ran out of bytes getting EVR task name");
            }
            final String taskName = GDR.get_printable_string(buff, currentOffset, 6);
            metaKeys.add(EvrMetadataKeywordEnum.TASKNAME);
            metaData.add(taskName);
            currentOffset += 6;
            debugTrace_.trace("EVR TaskName ", taskName);
            // 4 bytes: event ID
            if ((currentOffset + 4) > buff.length) {
                fail(apid, vcid, dssId, seqCount, currentEvr,
                        "Ran out of bytes getting EVR event ID from EVR packet");
            }
            final long eventId = GDR.get_u32(buff, currentOffset);
            currentOffset += 4;
            debugTrace_.trace("EVR event id ", eventId);

            // Get the dictionary entry for this event ID
       
            final IEvrDefinition currentEvrDefinition = this.definitionMap.get(eventId);

            /*  Used cached EVR table instance. */
            if ( currentEvrDefinition == null) {
                final IEvrDefinition dummyEvrDef = evrDictFactory.getMultimissionEvrDefinition();
                dummyEvrDef.setId(eventId);
                dummyEvrDef.setLevel(UNKNOWN_KEYWORD);
                currentEvr.setEvrDefinition(dummyEvrDef);
                throw new EvrExtractorException("Could not find Evr event id "
                        + currentEvr.getEventId() + " in Evr dictionary");
            }

            debugTrace_.trace("found EVR ID in dictionary");
            currentEvr.setEvrDefinition(currentEvrDefinition);
            
            // 4 bytes: overall sequence number
            if ((currentOffset + 4) > buff.length) {
                makeBadEvr(apid, currentEvr,
                        "Ran out of bytes getting EVR overall sequence number");
                return currentEvr;
            }
            final int sequenceId = GDR.get_i32(buff, currentOffset);

            int finalSequenceId = sequenceId;
            if (sequenceId < 0) {
                finalSequenceId = sequenceId & 0x7fffffff;
            }

            metaKeys.add(EvrMetadataKeywordEnum.SEQUENCEID);
            // reverse decode logic
            if (sequenceId < 0) { // MSB is on - it is from Real time buffer
                debugTrace_.trace("EVR sequenceId ", finalSequenceId);
                metaData.add( RT_RING + ":" + finalSequenceId);


            } else {
                debugTrace_.trace("EVR sequenceId ", sequenceId);
                metaData.add( Integer.toString(sequenceId) );


            }

            currentOffset += 4;

            // 4 bytes: category sequence number
            if ((currentOffset + 4) > buff.length) {
                makeBadEvr(apid, currentEvr,
                        "Ran out of bytes getting EVR category sequence");
                return currentEvr;
            }
            final long categorySequenceId = GDR.get_u32(buff, currentOffset);
            debugTrace_.trace("EVR categorySequenceId ", categorySequenceId);
            metaKeys.add(EvrMetadataKeywordEnum.CATEGORYSEQUENCEID);
            metaData.add(String.valueOf(categorySequenceId));
            currentOffset += 4;

            // 1 byte: number of parameters
            if ((currentOffset + 1) > buff.length) {
                makeBadEvr(apid, currentEvr,
                        "Ran out of bytes getting number of EVR parameters "
                                + "(off=" + currentOffset + " buff.length="
                                + buff.length + ")");
                return currentEvr;
            }
            int parameterCount = GDR.get_u8(buff, currentOffset);
            debugTrace_.trace("EVR parameterCount ", parameterCount);

            currentOffset += 1;

            // Get EVR category, using dictionary entry
            debugTrace_.trace("EVR level ", currentEvrDefinition.getLevel());

            /*
             * A fatal EVR will include stack traces that always start at byte
             * 19 as the first parameter in the message. Stack traces can
             * contain between 1 and 6 entries, and there are 4 bytes per entry,
             * so the resulting length is between 4 and 24 bytes. Non-fatal EVRs
             * do not include stack traces.
             */

            // Special case for fatal - get stack dump. Note that the offset
            // check is to confirm that
            // a stack dump is really there. It's supposed to be according to
            // the MSAP specification,
            // but MSL is currently leaving the stack dump out.
            if (evrIsFatal(currentEvrDefinition.getLevel())
                    && !((currentOffset + 1) > buff.length)) {

                // initialize a data formatter

                debugTrace_.trace("EVR FATAL");
                // 1 byte: stack dump length
                if ((currentOffset + 1) > buff.length) {
                    logError(apid, currentEvr,
                            "Ran out of bytes getting EVR stack dump length");
                    addMetadata(currentEvr, metaKeys, metaData);
                    return currentEvr;
                }
                final int stackDumpLength = GDR.get_u8(buff, currentOffset);
                currentOffset += 1;
                if (stackDumpLength % 4 != 0) {
                    logError(apid, currentEvr,
                            "Fatal EVR had bad stack dump length "
                                    + stackDumpLength);
                    addMetadata(currentEvr, metaKeys, metaData);
                    return currentEvr;
                }
                final int addressCount = stackDumpLength / 4;
                final List<Long> addressList = new ArrayList<Long>(addressCount);
                final StringBuilder fmt = new StringBuilder();
                for (int i = 0; i < addressCount; ++i) {
                    // 4 bytes: stack address
                    if ((currentOffset + 4) > buff.length) {
                        logError(apid, currentEvr,
                                "Ran out of bytes getting EVR stack address #"
                                        + (i + 1));
                        addMetadata(currentEvr, metaKeys, metaData);
                        return currentEvr;
                    }
                    final Long address = Long.valueOf(GDR.get_u32(buff,
                            currentOffset));
                    currentOffset += 4;
                    addressList.add(address);
                    if (fmt.length() > 0) {
                        fmt.append(',');
                    }
                    fmt.append("0x%08x");
                }
                metaKeys.add(EvrMetadataKeywordEnum.ADDRESSSTACK);
                metaData.add(formatter.sprintf(fmt.toString(),
                        addressList.toArray()));
                --parameterCount;
            }

            if (parameterCount != currentEvrDefinition.getNargs()) {
                makeBadEvr(
                        apid,
                        currentEvr,
                        "Number of parameters specified in EVR ("
                                + parameterCount
                                + ") does not match nargs in dictionary entry ("
                                + currentEvrDefinition.getNargs() + ")");

                return currentEvr;
            }

            // Get parameters

            /*
             * The length of a string parameter is between 0 and 80 characters
             * and the length of other parameters can be 1, 2, 4 or 8 bytes. The
             * type of a parameter is not known to the EVR module, but is known
             * by the dictionary and will be used when decoding the event
             * message on the ground. [That's what it says in section 3.1.4 of
             * the 7/19/05 version of Muh-Wang Yang's EVR module of the Flight
             * Software Development Document. However, the information isn't
             * currently provided in the EVR dictionary.]
             */

            if ((parameterCount < 0) || (parameterCount > MAX_PARAMETER_COUNT)) {
                makeBadEvr(apid, currentEvr, "Invalid EVR parameter count "
                        + parameterCount);
                return currentEvr;
            }

            String originalFormat = currentEvrDefinition.getFormatString();

            /*
             * Remove extra quotes around message text if they are present
             */
            if (originalFormat.startsWith("\"")
                    && originalFormat.endsWith("\"")) {
                originalFormat = originalFormat.substring(1,
                        originalFormat.length() - 1);
            }
            debugTrace_.trace("EVR format string ", originalFormat);

            try {
                formatParameters(parameterCount, apid, vcid, dssId, seqCount,
                        currentEvrDefinition, originalFormat, buff);

            } catch (final EvrExtractorException extractExcept) {
                makeBadEvr(apid, currentEvr, extractExcept.getMessage());
                return currentEvr;
            }

            debugTrace_.trace("EVR message: ", currentEvr.getMessage());

            addMetadata(currentEvr, metaKeys, metaData);

        } catch (final EvrExtractorException e) {
            makeBadEvr(apid, currentEvr, e.getMessage());

        } catch (final Exception e) {
            e.printStackTrace();
            makeBadEvr(apid, currentEvr, "Unexpected error extracting EVR: "
                    + e.toString());
        }
        return currentEvr;
    }

    /**
     * . getRawParameters extracts Evr parameters from a packet buffer and
     * places them on a list for later use.string formatter.
     * 
     * @param parameterCount
     *            the number of parameters to extract
     * @param buff
     *            the data buffer
     */
    private void getRawParameters(final int parameterCount, final byte[] buff)
            throws EvrExtractorException {

        rawParameters = new ArrayList<IRawEvrData>();
        for (int i = 0; i < parameterCount; i++) {

            if (parameterTypes[i] == String.class) {

                if (parameterLengths[i] > 0) {

                    validateString(buff, parameterLengths[i],
                            parameterOffsets[i]);
                    final IRawEvrData stringData = rawDataFactory.create(
                            buff, parameterLengths[i], parameterOffsets[i],
                            parameterTypes[i]);

                    rawParameters.add(stringData);
                } else {

                    final IRawEvrData emptyStringData = this.rawDataFactory.create();
                    emptyStringData.setDataToEmptyString();

                    rawParameters.add(emptyStringData);
                }

            } else if (typeIsInteger(parameterTypes[i])) {

                if (allowedIntegerLengths_.contains(parameterLengths[i])) {

                    final RawEvrData longData = new RawEvrData(
                            buff, parameterLengths[i], parameterOffsets[i],
                            parameterTypes[i]);

                    rawParameters.add(longData);
                } else {

                    debugTrace_.trace("Found integer EVR data type of length ", parameterLengths[i]);
                    debugTrace_.trace("Allowed lengths for integers are ");
                    final Iterator<Integer> allowedIntIterator = allowedIntegerLengths_
                            .iterator();
                    while (allowedIntIterator.hasNext()) {

                        debugTrace_.trace(allowedIntIterator.next());
                    }

                    throw new EvrExtractorException(
                            "Invalid integer data type length: "
                                    + parameterLengths[i]);
                }

            } else if (typeIsFloat(parameterTypes[i])) {

                if (allowedFloatingPointLengths_.contains(parameterLengths[i])) {

                    final RawEvrData floatData = new RawEvrData(
                            buff, parameterLengths[i], parameterOffsets[i],
                            parameterTypes[i]);

                    rawParameters.add(floatData);
                } else {

                    debugTrace_.trace("Found floating point EVR data type of length ",
                            parameterLengths[i]);
                    debugTrace_.trace("Allowed lengths for floating point numbers are ");
                    final Iterator<Integer> allowedFpIterator = allowedFloatingPointLengths_
                            .iterator();
                    while (allowedFpIterator.hasNext()) {

                        debugTrace_.trace(allowedFpIterator.next());
                    }

                    throw new EvrExtractorException(
                            "Invalid floating point data type length: "
                                    + parameterLengths[i]);
                }
            }
        }
    }

    /**
     * getParameterInfo retrieves information about the individual evr
     * parameters: their lengths, the offsets into the evr data buffer, and the
     * corresponding Java classes to which the parameters are to be mapped
     * according to the definitions specified in the evr dictionary
     * 
     * @param parameterCount
     *            -- the number of parameters for this evr
     * @param evrDataBuffer
     *            -- the evr data buffer containing the parameters
     * @param apid
     *            -- the apid of the evr being processed
     * @param vcid
     *            -- the vcid of the source frame
     * @param seqCount
     *            -- the sequence count of the packet
     * @param currentEvrDefinition
     *            -- the evr being processed
     * @param originalFormat
     *            -- the original evr message format as specified in the EVR
     *            dictionary
     * @throws EvrExtractorException
     */
    private void getParameterInfo(final int parameterCount,
            final byte[] evrDataBuffer, final int apid, final Integer vcid, final int dssId, 
            final int seqCount, final String originalFormat,
            final IEvrDefinition currentEvrDefinition)
                    throws EvrExtractorException {

        if (currentEvrDefinition == null) {
            throw new EvrExtractorException(
                    "cannot process parameter info retrieval because EVR definition is null"
                            + "; Message format: " + originalFormat);
        }

        parameterLengths = new int[parameterCount];
        parameterOffsets = new int[parameterCount];

        for (int i = 0; i < parameterCount; ++i) {

            // grab arg entry in EVR definition
            final IEvrArgumentDefinition arg = (currentEvrDefinition.getArgs()).get(i);

            // 1 byte: parameter length

            debugTrace_.trace("parameter count i = ", i);
            debugTrace_.trace("offset ", currentOffset);
            if ((currentOffset + 1) > evrDataBuffer.length) {
                fail(apid, vcid, dssId, seqCount, currentEvr,
                        "Ran out of bytes getting EVR parameter #" + i);
            }
            parameterLengths[i] = GDR.get_u8(evrDataBuffer, currentOffset);
            debugTrace_.trace("parameter length ", parameterLengths[i]);

            final boolean is64Int = arg.getType() == EvrArgumentType.I64 || arg.getType() == EvrArgumentType.U64;
            final boolean is32Int = arg.getType() == EvrArgumentType.I32 || arg.getType() == EvrArgumentType.U32;

            // warn if parameter length is invalid for the type
            if ((is64Int
                    && parameterLengths[i] != 1
                    && parameterLengths[i] != 2
                    && parameterLengths[i] != 4 && parameterLengths[i] != 8)
                    || (is32Int
                            && parameterLengths[i] != 1
                            && parameterLengths[i] != 2 && parameterLengths[i] != 4)
                            || ((arg.getType() == EvrArgumentType.F64)
                                    && parameterLengths[i] != 4 && parameterLengths[i] != 8)) {
                logWarn(apid, currentEvr,
                        "Parameter length in EVR (" + parameterLengths[i]
                                + " bytes) is invalid for "
                                + "type in dictionary entry ("
                                + (currentEvrDefinition.getArgs()).get(i).getType()
                                + ")");
            }

            if ((parameterLengths[i] < 0) || (parameterLengths[i] > 80)) {
                fail(apid, vcid, dssId, seqCount, currentEvr,
                        "Invalid parameter length " + parameterLengths[i]);
            }

            currentOffset += 1;

            // 1, 2, 4, 8, 0..80 bytes: parameter value
            parameterOffsets[i] = currentOffset;
            debugTrace_.trace("parameter offset ", currentOffset);
            if ((currentOffset + parameterLengths[i]) > evrDataBuffer.length) {
                fail(apid, vcid, dssId, seqCount, currentEvr,
                        "Ran out of bytes getting EVR parameter value #"
                                + (i + 1));
            }
            currentOffset += parameterLengths[i];
        }

        // get the parameter types

        /* 
         * Use local call to get types rather than method in the
         * EVR definition.
         */
        parameterTypes = getExpectedTypes();

        if (parameterCount != parameterTypes.length) {

            final String dumpMessage = dumpEvrParameters(parameterCount,
                    evrDataBuffer);
            throw new EvrExtractorException("number of parameter types "
                    + parameterTypes.length + ", derived from "
                    + "EVR message format "
                    + "is not equal to the parameter count " + parameterCount
                    + " specified in the EVR--EVR DUMP:: " + dumpMessage
                    + "; Message format: " + originalFormat);
        }
    }

    /**
     * formatParameters builds a formatted evr message string according to the
     * specifications of the format in the EVR dictionary. formatParameters is
     * called only if the number of parameters to be formatted is positive
     * 
     * @param parameterCount
     *            -- the expected number of parameters in the EVR
     * @param apid
     *            -- the apid for the evr being processed
     * @param vcid
     *            -- the vcid of the source frame
     * @param dssId
     * @param seqCount
     *            -- the sequence count of the packet
     * @param currentEvrDefinition
     *            -- the current evr definition as obtained from the EVR
     *            dictionary
     * @param originalMessage
     *            -- the original format of the EVR message as retrieved from
     *            the evr definition
     * @param evrDataBuffer
     *            -- the buffer containing the evr parameter data
     * @throws EvrExtractorException
     */

    private void formatParameters(final int parameterCount, final int apid,
            final Integer vcid, final int dssId, final int seqCount,
            final IEvrDefinition currentEvrDefinition,
            final String originalMessage, final byte[] evrDataBuffer)
                    throws EvrExtractorException {

        // get parameter formats

        List<String> parameterFormats = null;
        try {
            parameterFormats = SprintfUtil.getFormatLetters( originalMessage );
        } catch (final SprintfUtilException e) {			  
            throw new EvrExtractorException( "Could not format EVR message using format string ( " + originalMessage + "): " + e.getMessage());
        }

        // If we could not find any format statements in the message,
        // finalize the evr message and return

        if (parameterFormats.isEmpty()) {

            setEvrMessage(currentEvr, originalMessage);
            return;
        }

        // check the parameter count--if it's 0, at this point the
        // parameterFormat list contains a format statement, so throw
        // an exception
        if (parameterCount == 0) {
            throw new EvrExtractorException(
                    "Evr parameter count was 0, but message \""
                            + originalMessage + "\" contains format statements");
        }

        // the parameter count is positive and we have formats in the EVR
        // message

        getParameterInfo(parameterCount, evrDataBuffer, apid, vcid, dssId, seqCount,
                originalMessage, currentEvrDefinition);

        // Build a list of printable objects

        try {
            getRawParameters(parameterCount, evrDataBuffer);
        } catch (final EvrExtractorException e) {
            fail(apid, vcid, dssId, seqCount, currentEvr, e.getMessage());
        }

        // Replace parameter values (table lookup, opcode replacement)

        String messageWithReplacements = originalMessage;
        if (currentEvrDefinition != null) {


            messageWithReplacements = extractUtil.replaceParameters(
                    rawParameters, originalMessage, currentEvrDefinition);
        }

        // get the revised parameter format list
        if (!(messageWithReplacements.equals(originalMessage))) {

            try {
                parameterFormats = SprintfUtil.getFormatLetters( messageWithReplacements );
            } catch (final SprintfUtilException e) {			  
                throw new EvrExtractorException( "Could not format EVR message using format string ( " + messageWithReplacements+ "): " + e.getMessage());
            }
        }

        // check that the number of parameters is equal to the number of format
        // statements in the message

        if (parameterFormats.size() != rawParameters.size()) {
            throw new EvrExtractorException("EVR Extraction failed for event ID "
                    + currentEvr.getEventId() + "--"
                    + "parameter list length " + rawParameters.size()
                    + " is not equal to the " + "number of parameters "
                    + parameterFormats.size() + " to be formatted.");
        }

        List<Object> formattedParameters = null;
        try {
            formattedParameters = getFormattedParameters(rawParameters,
                    parameterFormats);
        } catch (final EvrExtractorException e) {
            fail(apid, vcid, dssId, seqCount, currentEvr, e.getMessage());
        }

        // Format the string, and extract the source code file reference
//        SprintfFormat dataFormatter = SessionUtility.getSprintfFormatter();
        String finalMessage = "";

        try {

            if (formattedParameters.size() == 1) {
                finalMessage = formatter.anCsprintf(
                        messageWithReplacements, formattedParameters.get(0));
            } else {
                finalMessage = formatter.sprintf(messageWithReplacements,
                        formattedParameters.toArray());
            }

        } catch (final ClassCastException cce) {
            throw new EvrExtractorException("EVR extraction failed for event ID "
                    + currentEvr.getEventId() + "--"
                    + "mismatch between format statement and argument type",
                    cce);
        }

        setEvrMessage(currentEvr, finalMessage);
    }
   
  

    /**
     * dumpEvrParameters is a routine designed to assist in debugging problems
     * with EVR processing. It generates a string representation of the EVR's
     * parameters, along with information to identify the EVR.
     * 
     * @param parameterCount
     *            -- the number of parameters as specified in the original evr.
     * @param parameterData
     *            -- the evr parameter data as a byte array
     * @return a string containing a dump of the EVR event id, the level, the
     *         sclk, the scet, the ert, and hex dumps of each of the parameters
     */
    private String dumpEvrParameters(final int parameterCount,
            final byte[] parameterData) {
        final DateFormat format = TimeUtility.getFormatterFromPool();
        final StringBuffer parameterBuffer = new StringBuffer(1024);
        parameterBuffer.append("Event ID: " + currentEvr.getEventId() + "; " +
                "Level: " + currentEvr.getLevel() + "; ");

        final ISclk sclk = currentEvr.getSclk();

        parameterBuffer.append("SCLK: "
                + ((sclk != null) ? sclk.toString() : "null") + "; ");

        final IAccurateDateTime currentScet = currentEvr.getScet();
        final String scetString = (currentScet != null) ? currentScet
                .getFormattedScet(true) : "null";
                final IAccurateDateTime currentErt = currentEvr.getErt();
                final String ertString = (currentErt != null) ? currentErt
                        .getFormattedErt(true) : "null";
                        final ILocalSolarTime currentSol = currentEvr.getSol();
                        final String solString = (currentSol != null) ? currentSol
                                .getFormattedSol(true) : "null";

                                parameterBuffer.append("SCET: " + scetString + "; " +
                                        "ERT: " + ertString + ";" +
                                        "LST: " + solString + ";" +
                                        "Parameter Dump: ");
                                if (parameterCount == 0) {
                                    parameterBuffer.append("  No parameters to dump\n");
                                } else {

                                    for (int parmCounter = 0; parmCounter < parameterLengths.length; parmCounter++) {
                                        parameterBuffer.append("Parameter Number " + (parmCounter + 1)
                                                + "; " 
                                                + "Bytes: "
                                                + parameterLengths[parmCounter] + "; ");
                                        final StringWriter strOut = new StringWriter();
                                        final PrintWriter out = new PrintWriter(strOut);
                                        out.printf("Hex Value: ");
                                        for (int icount = 0; icount < parameterLengths[parmCounter]; ++icount) {
                                            out.printf(
                                                    "%02X ",
                                                    Byte.valueOf(parameterData[parameterOffsets[parmCounter]
                                                            + icount]));
                                        }
                                        out.close();
                                        parameterBuffer.append(strOut.toString() + ";");
                                    }
                                }
                                TimeUtility.releaseFormatterToPool(format);
                                return parameterBuffer.toString();
    }


    /**
     * Checks that a string contains printable ASCII characters. If a newline
     * character is found, it is replaced with a space.
     * 
     * @param evrData
     *            -- evr data buffer
     * @param offset
     *            -- offset into the buffer at which the string is found
     * @param length
     *            -- length of the string
     * @throws EvrExtractorException
     *             thrown if encountered
     */
    protected void validateString(final byte[] evrData, final int length,
            final int offset) throws EvrExtractorException {

        boolean stringIsValid = true;

        for (int byteCount = 0; byteCount < length; ++byteCount) {
            final int next = evrData[byteCount + offset] & 0xFF;

            // Note: 127 is DEL and is not printable

            if ((next < 32) || (next >= 127)) {

                if (next == 10) {
                    evrData[byteCount + offset] = 32;
                } else {
                    stringIsValid = false;
                }

            }

        }

        if (!stringIsValid) {

            final StringWriter strOut = new StringWriter();
            final PrintWriter out = new PrintWriter(strOut);
            out.printf("Invalid string found in EVR");


            out.close();
            throw (new EvrExtractorException(strOut.toString()));
        }

    }

    /**
     * Return an array of Class which contain Class objects corresponding to the
     * type of data that the parameter should be contained in (or formatted as)
     * 
     * @return Class list (classes can be Byte, Short, Integer, Long, or String)
     */
    public synchronized Class<?>[] getExpectedTypes() {
        final List< Class<?> > types = new LinkedList< Class<?>>();

        final List<IEvrArgumentDefinition> args = currentEvr.getEvrDefinition().getArgs();
        
        for (int i = 0; i < args.size(); i++ ) {
            final EvrArgumentType type = args.get(i).getType();

            /*
             *  For some reason the floating point
             * types below were returning Long and Integer classes instead of
             * Float and Double classes.
             */
            if (type == EvrArgumentType.F32) { 
                types.add(Float.class);       
            } else if (type == EvrArgumentType.F64) {
                types.add(Double.class);      
            } else if (type == EvrArgumentType.U64 ||
                    type == EvrArgumentType.I64) {
                types.add( Long.class );
            } else if ( type == EvrArgumentType.U32 ||
                    type == EvrArgumentType.I32 ||
                    type == EvrArgumentType.F32) {
                types.add( Integer.class );
            } else if ( type == EvrArgumentType.U16 ||
                    type == EvrArgumentType.I16) {
                types.add( Short.class );
            } else if (type == EvrArgumentType.OPCODE ||
                    type == EvrArgumentType.SEQID) {
                if (args.get(i).getLength() == 2) {
                    types.add( Short.class );
                } else {
                    types.add( Integer.class );
                }
            } else if ( type == EvrArgumentType.U8 ||
                    type == EvrArgumentType.I8 ||
                    type == EvrArgumentType.BOOL ) {
                types.add( Byte.class );
            } else if ( type == EvrArgumentType.FIX_STRING ||
                    type == EvrArgumentType.VAR_STRING ) {
                types.add( String.class );
            } else if ( type == EvrArgumentType.ENUM ) {
                types.add(Integer.class); 
            } else {
                types.add( String.class );
            }

        }

        Class<?>[] classArray = null;

        if( !types.isEmpty() ) {
            classArray = new Class<?>[ types.size() ];
            final Iterator< Class<?> > typeIterator = types.iterator();
            int tempCount = 0;
            while( typeIterator.hasNext() ) {
                classArray[tempCount++] = typeIterator.next();
            }
        } else {
            classArray = new Class<?>[0];
        }

        return classArray;
    }
}
