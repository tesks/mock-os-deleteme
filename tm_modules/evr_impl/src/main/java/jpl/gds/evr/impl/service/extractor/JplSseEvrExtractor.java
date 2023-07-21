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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.evr.api.EvrMetadataKeywordEnum;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.service.extractor.EvrExtractorException;
import jpl.gds.evr.api.service.extractor.IEvrExtractor;
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

/**
 * <code>JplSseEvrExtractor</code> is a multi-mission adapter for EVR processing,
 * which is currently used for the processing of JPL SSE EVRs in conjunction
 * with the SseEvrDefinition class.
 *
 *
 * @see IEvrExtractor
 * @see IEvr
 * 
 */
public class JplSseEvrExtractor extends AbstractEvrExtractor {

    /* Use FastTracer for debug messages throughout */
    private static  Tracer debugTrace;
    private static final String RT_RING = "RT";
    private static final String NULL = "null";

    private Class<?> [] parameterTypes = null;
    private int[] parameterLengths = null;
    private int[] parameterOffsets = null;
    private List< IRawEvrData > rawParameters = null;
    private IEvr currentEvr =  null;


    /**
     * Maximum number of Evr parameters.
     */
    public static final int MAX_PARAMETER_COUNT = 11;

    /**
     * Default constructor.
     * 
     * @param context
     *            the current application context
     * 
     * @throws EvrExtractorException
     *             thrown when problem occurs while creating and setting up this
     *             EVR adapter
     */
    public JplSseEvrExtractor(final ApplicationContext context) throws EvrExtractorException {

        super(context);
        debugTrace = TraceManager.getTracer(context, Loggers.TLM_EVR);
        saveFatalKeyword();
        saveSourceFileFlag();
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.evr.impl.service.extractor.AbstractEvrExtractor#extractEvr(byte[], int, int, int, java.lang.Integer, int, int, jpl.gds.evr.api.IEvr)
     */
    @Override
    protected IEvr extractEvr(final byte[] buff, final int startOffset, final int length, final int apid, final Integer vcid, final int dssId, final int seqCount, final IEvr evr) 
            throws EvrExtractorException {

        try {
            currentEvr = evr;

            currentEvr.setVcid(vcid);

            metaKeys = new ArrayList<EvrMetadataKeywordEnum>();
            metaData = new ArrayList<String>();

            currentOffset = startOffset;

            // 6 bytes: task name (or "_INTR_" for interrupt context)
            if ( (currentOffset + 6) > buff.length) {
                fail(apid, vcid, dssId, seqCount, currentEvr, "Ran out of bytes getting EVR task name");
            }
            final String taskName = GDR.get_printable_string(buff, currentOffset, 6);
            metaKeys.add(EvrMetadataKeywordEnum.TASKNAME);
            metaData.add(taskName);
            currentOffset += 6;
            debugTrace.trace("EVR TaskName " + taskName);
            // 4 bytes: event ID
            if ((currentOffset + 4) > buff.length) {
                fail(apid, vcid, dssId, seqCount, currentEvr, "Ran out of bytes getting EVR event ID from EVR packet");
            }
            final long eventId = GDR.get_u32(buff, currentOffset);
            currentOffset += 4;
            debugTrace.debug("EVR event id " + eventId);

            // Get the dictionary entry for this event ID
            /* Get EVR definition from SSE-specific table */
            final IEvrDefinition currentEvrDefinition = this.definitionMap.get(eventId);

            if( currentEvrDefinition == null) {
                final IEvrDefinition dummyEvrDef = evrDictFactory.getJplSseEvrDefinition();
                dummyEvrDef.setId(eventId);
                /* Set level to UNKNOWN */
                dummyEvrDef.setLevel(UNKNOWN_KEYWORD);
                currentEvr.setEvrDefinition(dummyEvrDef);
                throw new EvrExtractorException( "Could not find Evr event id " +
                        eventId + " in Evr dictionary" );
            }

            debugTrace.trace("found EVR ID in dictionary");
            currentEvr.setEvrDefinition(currentEvrDefinition);

            // 4 bytes: overall sequence number
            if ((currentOffset + 4) > buff.length) {
                makeBadEvr(apid, currentEvr, "Ran out of bytes getting overall EVR sequence number");
                return currentEvr;
            }
            final int sequenceId = GDR.get_i32(buff, currentOffset);

            int finalSequenceId = sequenceId;
            if( sequenceId < 0 ) {

                finalSequenceId = sequenceId & 0x7fffffff;
            }

            metaKeys.add(EvrMetadataKeywordEnum.SEQUENCEID);
            if (sequenceId < 0) { // MSB is on - it is from Real time buffer
                debugTrace.trace("EVR sequenceId " +finalSequenceId);
                metaData.add( RT_RING + ":" + finalSequenceId);


            } else {
                debugTrace.trace("EVR sequenceId " + sequenceId);
                metaData.add( Integer.toString(sequenceId) );


            }
            currentOffset += 4;

            // 4 bytes: category sequence number
            if ((currentOffset + 4) > buff.length) {
                makeBadEvr(apid, currentEvr, "Ran out of bytes getting EVR category sequence");
                return currentEvr;
            }
            final long categorySequenceId = GDR.get_u32(buff, currentOffset);
            debugTrace.trace("EVR categorySequenceId " + categorySequenceId);
            metaKeys.add(EvrMetadataKeywordEnum.CATEGORYSEQUENCEID);
            metaData.add(String.valueOf(categorySequenceId));
            currentOffset += 4;

            // 1 byte: number of parameters
            if ((currentOffset + 1) > buff.length) {
                makeBadEvr(apid, currentEvr, "Ran out of bytes getting number of EVR parameters " +
                        "(off=" + currentOffset + " buff.length=" + buff.length + ")");
                return currentEvr;
            }
            int parameterCount = GDR.get_u8(buff, currentOffset );
            debugTrace.trace("EVR parameterCount " + parameterCount);

            currentOffset += 1;

       
            debugTrace.trace("EVR level " + currentEvrDefinition.getLevel());

            /*
             * A fatal EVR will include stack traces that always start at byte 19 as
             * the first parameter in the message. Stack traces can contain between
             * 1 and 6 entries, and there are 4 bytes per entry, so the resulting
             * length is between 4 and 24 bytes. Non-fatal EVRs do not include stack
             * traces.
             */

            // Special case for fatal - get stack dump. Note that the offset check is to confirm that
            // a stack dump is really there.  
            if ( evrIsFatal( currentEvrDefinition.getLevel() ) && !((currentOffset + 1) > buff.length )) {

//                // initialize a data formatter
//                SprintfFormat dataFormatter = SessionUtility.getSprintfFormatter();

                debugTrace.trace("EVR FATAL");
                // 1 byte: stack dump length
                if ((currentOffset + 1) > buff.length) {
                    logError(apid, currentEvr, "Ran out of bytes getting EVR stack dump length");
                    addMetadata(currentEvr, metaKeys, metaData);
                    return currentEvr;
                }
                final int stackDumpLength = GDR.get_u8(buff, currentOffset );
                currentOffset += 1;
                if (stackDumpLength % 4 != 0) {
                    logError(apid, currentEvr, "Fatal EVR had bad stack dump length " + stackDumpLength);
                    addMetadata(currentEvr, metaKeys, metaData);
                    return currentEvr;
                }
                final int addressCount = stackDumpLength / 4;
                final List<Long> addressList = new ArrayList<Long>(addressCount);
                final StringBuffer fmt = new StringBuffer();
                for (int i = 0; i < addressCount; ++i) {
                    // 4 bytes: stack address
                    if ((currentOffset + 4) > buff.length) {
                        logError(apid, currentEvr,
                                "Ran out of bytes getting EVR stack address #" + (i + 1));
                        addMetadata(currentEvr, metaKeys, metaData);
                        return currentEvr;
                    }
                    final Long address = Long.valueOf(GDR.get_u32(buff, currentOffset));
                    currentOffset += 4;
                    addressList.add(address);
                    if (fmt.length() > 0) {
                        fmt.append(',');
                    }
                    fmt.append("0x%08x");
                }
                metaKeys.add(EvrMetadataKeywordEnum.ADDRESSSTACK);
                metaData.add(formatter.sprintf(fmt.toString(), addressList.toArray()));
                --parameterCount;
            }

            // Get parameters

            /*
             * The length of a string parameter is between 0 and 80 characters
             * and the length of other parameters can be 1, 2, 4 or 8 bytes.
             * The type of a parameter is not known to the EVR module, but is
             * known by the dictionary and will be used when decoding the event
             * message on the ground.
             * [That's what it says in section 3.1.4 of the 7/19/05 version
             * of Muh-Wang Yang's EVR module of the Flight Software Development
             * Document. However, the information isn't currently provided
             * in the EVR dictionary.]
             */

            if ((parameterCount < 0) || (parameterCount > MAX_PARAMETER_COUNT)) {
                makeBadEvr(apid, currentEvr, "Invalid EVR parameter count " + parameterCount);
                return currentEvr;
            }

            String originalFormat = currentEvrDefinition.getFormatString();
            /*
             * Remove extra quotes around message text if they are present
             */
            if (originalFormat.startsWith("\"") && originalFormat.endsWith("\"")) {
                originalFormat = originalFormat.substring(1, originalFormat.length() - 1);
            }
            debugTrace.trace("EVR format string " + originalFormat);

            try {
                formatParameters( parameterCount,
                        apid,
                        vcid,
                        dssId,
                        seqCount,
                        currentEvrDefinition,
                        originalFormat,
                        buff );

            }
            catch( final EvrExtractorException extractExcept ) {
                makeBadEvr(apid, currentEvr, extractExcept.getMessage());
                return currentEvr;
            }

            debugTrace.debug("EVR message: " + currentEvr.getMessage());

            addMetadata(currentEvr, metaKeys, metaData );

        } catch( final EvrExtractorException extractExcept ) {
            makeBadEvr(apid, currentEvr, extractExcept.getMessage());
        } catch (final Exception e) {
            e.printStackTrace();
            makeBadEvr(apid, currentEvr, "Unexpected error extracting EVR: " + e.toString());
        }
        return currentEvr;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.evr.impl.service.extractor.AbstractEvrExtractor#extractEvr(jpl.gds.tm.service.api.packet.ITelemetryPacketMessage)
     */
    @Override
    @SuppressWarnings("PMD.AvoidRethrowingException")
    public IEvr extractEvr(final ITelemetryPacketMessage pm) throws EvrExtractorException {
        final ITelemetryPacketInfo pktInfo = pm.getPacketInfo();
        final int apid = pktInfo.getApid();
        final Integer vcid = pktInfo.getVcid();

        final int dssId = pktInfo.getDssId();

        final int seqCount = pktInfo.getSeqCount();

        try {
            final IEvr tempEvr = evrFactory.createEvr();

            final byte[] buff = pm.getPacket();
            lastPm_ = pm;
            tempEvr.setScet( pm.getPacketInfo().getScet() );
            tempEvr.setSclk( pm.getPacketInfo().getSclk() );
            tempEvr.setSol( pm.getPacketInfo().getLst() );
            tempEvr.setErt( pm.getPacketInfo().getErt() );
            tempEvr.setRct( pm.getRct() );
            tempEvr.setDssId(dssId);
            tempEvr.setVcid(vcid);

            tempEvr.setPacketId(pm.getPacketId());

            debugTrace.trace("EVR Buffer length " + buff.length);

            // start reading the packet data
            currentOffset = 0;
            
            final int packetHeaderLength = pktInfo.getPrimaryHeaderLength()
            		+ pktInfo.getSecondaryHeaderLength(); // skip packet header and spacecraft clock; // skip packet header and spacecraft clock
            if (buff.length < packetHeaderLength) {
                fail(apid, vcid, dssId, seqCount, currentEvr, "Ran out of bytes in the EVR packet header");
            }
            /** No longer skip headers based on hardcoded value */
            currentOffset += packetHeaderLength;

            extractEvr(buff, currentOffset, buff.length - currentOffset - 1, apid, vcid, dssId, seqCount, tempEvr);

            return currentEvr;

        } catch (final EvrExtractorException e) {
            throw e;

        } catch (final Exception e) {

            e.printStackTrace();
            fail(apid, vcid, dssId, seqCount, currentEvr, "Unexpected error extracting EVR: " + e.toString());
            return null;
        }
    }

    /**.
     * 
     * @param parameterCount the number of parameters to extract
     * @param buffer the data buffer
     * @param offset array the list of parameter offsets within the data buffer
     * @param length array the list of parameter lengths within the data buffer
     * @param type array of parameter Classes
     * @param the list of raw parameters
     */
    /**
     * <code>getRawParameters</code> extracts EVR parameters from a packet
     * buffer and places them on a list for later use by the string formatter.
     * 
     * @param parameterCount
     *            number of parameters in the EVR
     * @param buff
     *            packet buffer
     * @throws EvrExtractorException
     *             thrown if problem is encountered extracting parameters
     */
    private void getRawParameters( final int parameterCount,
            final byte[] buff ) throws EvrExtractorException {

        rawParameters = new ArrayList<IRawEvrData >();
        for (int i = 0; i < parameterCount; i++) {

            if ( parameterTypes[i] == String.class) {

                if( parameterLengths[i] > 0 ) {
                    validateString( buff,
                            parameterLengths[i],
                            parameterOffsets[i] );
                    final IRawEvrData stringData = rawDataFactory.create(buff,
                                    parameterLengths[i],
                                    parameterOffsets[i],
                                    parameterTypes[i] );

                    rawParameters.add(stringData);
                }else{
                    final IRawEvrData emptyStringData = rawDataFactory.create();
                    emptyStringData.setDataToEmptyString();

                    rawParameters.add(emptyStringData);
                }

            } else if ( typeIsInteger( parameterTypes[i] ) ) {

                if( allowedIntegerLengths_.contains( parameterLengths[i] ) ) {
                    final IRawEvrData longData = rawDataFactory.create(buff,
                                    parameterLengths[i],
                                    parameterOffsets[i],
                                    parameterTypes[i] );

                    rawParameters.add(longData);
                }else{

                    debugTrace.debug( "Found integer EVR data type of length " +
                            parameterLengths[i] );
                    debugTrace.debug( "Allowed lengths for integers are " );
                    final Iterator< Integer > allowedIntIterator = allowedIntegerLengths_.iterator();
                    while( allowedIntIterator.hasNext() ) {

                        debugTrace.debug( allowedIntIterator.next() );
                    }

                    throw new EvrExtractorException("Invalid integer data type length: " +
                            parameterLengths[i]);
                }

            } else if ( typeIsFloat( parameterTypes[i] ) ) {

                if( allowedFloatingPointLengths_.contains( parameterLengths[i] ) ) {
                    final RawEvrData floatData =
                            new RawEvrData( buff,
                                    parameterLengths[i],
                                    parameterOffsets[i],
                                    parameterTypes[i] );

                    rawParameters.add(floatData);
                }else{

                    debugTrace.debug( "Found floating point EVR data type of length " +
                            parameterLengths[i] );
                    debugTrace.debug( "Allowed lengths for floating point numbers are " );
                    final Iterator< Integer > allowedFpIterator = allowedFloatingPointLengths_.iterator();
                    while( allowedFpIterator.hasNext() ) {

                        debugTrace.debug( allowedFpIterator.next() );
                    }

                    throw new EvrExtractorException("Invalid floating point data type length: " + parameterLengths[i]);
                }
            }
        }
    }

    /**
     * <code>getParameterInfo</code> retrieves information about the individual
     * EVR parameters: their lengths, their offsets into the EVR data buffer,
     * and the corresponding Java classes to which the parameters are to be
     * mapped to according to the definitions specified in the EVR dictionary.
     * 
     * @param parameterCount
     *            number of parameters for this EVR
     * @param evrDataBuffer
     *            data buffer that contains the parameters
     * @param apid
     *            APID number of the EVR being processed
     * @param vcid
     *            VCID number of the source frame
     * @param seqCount
     *            sequence count of the packet
     * @param originalFormat
     *            the original EVR message format as specified in the EVR
     *            definition
     * @param currentEvrDefinition
     *            the EVR definition being used to interpret the parameters       
     * @throws EvrExtractorException
     *             thrown if a problem is encountered processing the parameters
     */
    private void getParameterInfo( final int parameterCount,
            final byte[] evrDataBuffer,
            final int apid,
            final Integer vcid,
            final int dssId,
            final int seqCount,
            final String originalFormat,
            final IEvrDefinition currentEvrDefinition) throws EvrExtractorException {

        parameterLengths = new int[parameterCount];
        parameterOffsets = new int[parameterCount];

        for ( int i = 0; i < parameterCount; ++i ) {

            // 1 byte: parameter length

            debugTrace.trace("parameter count i = " + i);
            debugTrace.trace("offset " + currentOffset );
            if (( currentOffset + 1) > evrDataBuffer.length) {
                fail(apid, vcid, dssId, seqCount, currentEvr, "Ran out of bytes getting EVR parameter #" + i);
            }
            parameterLengths[i] = GDR.get_u8( evrDataBuffer, currentOffset );
            debugTrace.trace("parameter length " + parameterLengths[i]);
            if ((parameterLengths[i] < 0) || (parameterLengths[i] > 80)) {
                fail(apid, vcid, dssId, seqCount, currentEvr, "Invalid parameter length " + parameterLengths[i]);
            }

            currentOffset += 1;

            // 1, 2, 4, 8, 0..80 bytes: parameter value
            parameterOffsets[i] = currentOffset;
            debugTrace.trace("parameter offset " + currentOffset);
            if (( currentOffset + parameterLengths[i]) > evrDataBuffer.length) {
                fail(apid, vcid, dssId, seqCount, currentEvr,
                        "Ran out of bytes getting EVR parameter value #" + (i + 1));
            }
            currentOffset += parameterLengths[i];
        }

        // get the parameter types

        if ( currentEvrDefinition == null ) {
            throw new EvrExtractorException( "cannot get expected types because EVR definition is null" +
                    "; Message format: " + originalFormat );
        }

        /* 
         *  Use local call to get expected types instead of
         * method in the EVR definition.
         */
        parameterTypes = getExpectedTypes( parameterLengths, originalFormat);

        if( parameterCount != parameterTypes.length ) {

            final String dumpMessage = dumpEvrParameters( parameterCount, evrDataBuffer );
            throw new EvrExtractorException( "number of parameter types " +
                    parameterTypes.length +
                    ", derived from " +
                    "EVR message format " +
                    "is not equal to the parameter count " +
                    parameterCount +
                    " specified in the EVR--EVR DUMP:: " +
                    dumpMessage +
                    "; Message format: " + originalFormat );
        }
    }

    /**
     * <code>formatParameters</code> builds a formatted EVR message string
     * according to the specifications of the format in the EVR definition.
     * <code>formatParameters</code> is called only if the number of parameters
     * to be formatted is positive.
     * 
     * @param parameterCount
     *            the expected number of parameters in the EVR
     * @param apid
     *            APID number of the EVR being processed
     * @param vcid
     *            virtual channel ID number of the source frame
     * @param seqCount
     *            sequence count of the packet
     * @param currentEvrDefinition
     *            EVR definition to be used to format the EVR
     * @param originalMessage
     *            original format of the EVR message as retrieved from the EVR
     *            definition
     * @param evrDataBuffer
     *            buffer containing the EVR parameter data
     * @throws EvrExtractorException
     *             thrown if a problem is encountered formatting the EVR
     */
    private void formatParameters( final int parameterCount,
            final int apid,
            final Integer vcid,
            final int dssId,
            final int seqCount,
            final IEvrDefinition currentEvrDefinition,
            final String originalMessage,
            final byte[] evrDataBuffer ) throws EvrExtractorException {


        // get parameter formats
        /*
         *  Removed use of getParameterFormats() method in
         * favor of method in SprintfUtil for locating formatters.
         */
        List< String > parameterFormats = null;
        try {
            parameterFormats = SprintfUtil.getFormatLetters( originalMessage );
        } catch (final SprintfUtilException e) {			  
            throw new EvrExtractorException( "Could not format EVR message using format string ( " + originalMessage + "): " + e.getMessage());
        }

        // If we could not find any format statements in the message,
        // finalize the evr message and return

        if( parameterFormats.isEmpty() ) {

            setEvrMessage(currentEvr,originalMessage );
            return;
        }

        // check the parameter count--if it's 0, at this point the
        // parameterFormat list contains a format statement, so issue a
        // warning, set the Evr message and return

        if( parameterCount == 0 ) {
            throw new EvrExtractorException( "Evr parameter count was 0, but message \""+
                    originalMessage +
                    "\" contains format statements" );
        }

        // the parameter count is positive and we have formats in the EVR
        // message

        getParameterInfo( parameterCount,
                evrDataBuffer,
                apid,
                vcid,
                dssId,
                seqCount,
                originalMessage,
                currentEvrDefinition );

        // Build a list of printable objects

        try {
            getRawParameters( parameterCount,
                    evrDataBuffer );
        }
        catch (final EvrExtractorException e) {
            fail(apid, vcid, dssId, seqCount, currentEvr, e.getMessage());
        }

        final String messageWithReplacements = originalMessage;

        // check that the number of parameters is equal to the number of format
        // statements in the message

        if( parameterFormats.size() != rawParameters.size() ) {
            throw new EvrExtractorException( "EVR Extraction failed for event ID " +
                    currentEvr.getEventId() + "--" +
                    "parameter list length " +
                    rawParameters.size() +
                    " is not equal to the " +
                    "number of parameters " +
                    parameterFormats.size() +
                    " to be formatted." );
        }

        List< Object > formattedParameters = null;
        try {
            formattedParameters = getFormattedParameters( rawParameters,
                    parameterFormats );
        }
        catch (final EvrExtractorException e) {
            fail(apid, vcid, dssId, seqCount, currentEvr, e.getMessage());
        }

        // Format the string, and extract the source code file reference
        String finalMessage = "";

        try{

            if( formattedParameters.size() == 1 ) {
                finalMessage = formatter.anCsprintf( messageWithReplacements,
                        formattedParameters.get( 0 ));
            }else{
                finalMessage = formatter.sprintf( messageWithReplacements,
                        formattedParameters.toArray());
            }

        } catch (final ClassCastException cce) {
            throw new EvrExtractorException("EVR extraction failed for event ID " +
                    currentEvr.getEventId() + "--" +
                    "mismatch between format statement and argument type", cce);
        }

        setEvrMessage(currentEvr,finalMessage );
    }

    /**
     * <code>dumpEvrParameters</code> is a routine designed to assist in
     * debugging problems with EVR processing. It generates a string
     * representation of the EVR's parameters, along with information to
     * identify the EVR.
     * 
     * @param parameterCount
     *            number of parameters as specified in the original EVR
     * @param parameterData
     *            EVR parameter data as a byte array
     * @return a <code>String</code> object containing a dump of the EVR's event
     *         ID, its level, its SCLK value, its SCET value, its ERT, and the
     *         hex dumps of each of the parameters
     */
    private String dumpEvrParameters( final int parameterCount, final byte[] parameterData )
    {
        final DateFormat format = TimeUtility.getFormatterFromPool();
        final StringBuffer parameterBuffer = new StringBuffer(80);
        parameterBuffer.append( "Event ID: " + currentEvr.getEventId() + "; " +
                "Level: " + currentEvr.getLevel() + "; " );

        final ISclk sclk = currentEvr.getSclk();
        final IAccurateDateTime currentScet = currentEvr.getScet();
        final String scetString = (currentScet != null) ? currentScet.getFormattedScet(true) : NULL;

        parameterBuffer.append( "SCLK: " + ((sclk != null) ? sclk.toString() : NULL) + "; " 
                + "SCET: " + scetString + "; " );

        if (this.setSolTimes) {
            final ILocalSolarTime currentSol = currentEvr.getSol();
            final String solString = (currentSol != null) ? currentSol.getFormattedSol(true) : NULL;
            parameterBuffer.append( "LST: " + solString + "; " );
        }

        final IAccurateDateTime currentErt = currentEvr.getErt();
        final String ertString = (currentErt != null) ? currentErt.getFormattedErt(true) : NULL;
        parameterBuffer.append( "ERT: " + ertString + ";" +
                "Parameter Dump: " );

        if( parameterCount == 0 ) {
            parameterBuffer.append( "  No parameters to dump\n" );
        }else{

            for( int parmCounter = 0; parmCounter < parameterLengths.length; parmCounter++ ) {
                parameterBuffer.append( "Parameter Number " + ( parmCounter + 1 ) + "; " +
                        "Bytes: " + parameterLengths[ parmCounter ] + "; " );
                final StringWriter strOut = new StringWriter();
                final PrintWriter out = new PrintWriter( strOut );
                out.printf( "Hex Value: ");
                for( int icount = 0; icount < parameterLengths[parmCounter]; ++icount ) {
                    out.printf( "%02X ",
                            Byte.valueOf( parameterData[ parameterOffsets[parmCounter] + icount ] ) );
                }
                out.close();
                parameterBuffer.append(  strOut.toString() + ";");
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
     *            EVR data buffer
     * @param length
     *            length of the string to validate
     * @param offset
     *            starting address of the string to validate
     * @throws EvrExtractorException
     *             thrown if a problem is encountered while processing and
     *             validating the string
     */
    protected void validateString( final byte[] evrData,
            final int length,
            final int offset ) throws EvrExtractorException{

        boolean stringIsValid = true;

        for( int byteCount = 0; byteCount < length; ++byteCount ) {
            final int ix = byteCount + offset;

            if( ( evrData[ix] < 32 ) ||
                    ( (0xff & (evrData[ix])) > 127 ) ) {

                if (evrData[ix] == 10) {
                    evrData[ix] = 32;
                }
                else {
                    stringIsValid = false;
                }

            }
        }

        if( !stringIsValid ) {

            final StringWriter strOut = new StringWriter();
            final PrintWriter out = new PrintWriter( strOut );
            out.printf( "Invalid string found in EVR" );

            out.close();
            throw ( new EvrExtractorException( strOut.toString() ) );
        }

    }
    
    /**
     * Based upon the given EVR format (message) string and a list of parameter lengths, return 
     * a list of Class objects indicating the actual expected type of each parameter.
     * 
     * @param parameterLengths array of lengths of individual EVR parameters
     * @param format the EVR format (message) string
     * @return array of Class objects representing parameter classes
     */
    @SuppressWarnings("PMD.SwitchDensity")
    public Class<?>[] getExpectedTypes(final int[] parameterLengths, final String format) {

        if( parameterLengths.length <= 0 || format == null)
        {
            return null;
        }
        final List< Class<?> > types = new LinkedList< Class<?>>();
        List<String> formatChars = null;


        try {
            formatChars = SprintfUtil.getFormatLetters(format);  
        } catch (final SprintfUtilException e) {
            // If there are actually illegal formatters, it will have been caught by
            // a previous call in the EVR processing and processing of the EVR will
            // be aborted. Technically, we should never get here, which is why I just return null.
            e.printStackTrace();
            debugTrace.warn("EVR format string contains illegal format specifier: " + e.getMessage());
            return null;
        }
        int typeIndex = 0;
        for(final String formatter: formatChars) {

            // If there are are fewer parameters in the EVR packet than formatters, just stop early.
            if( typeIndex >= parameterLengths.length )
            {
                break;
            }

            final char c = formatter.charAt(0);
            switch (c) {
            case 'c':
            case 'd':
            case 'i':
            case 'o':
            case 'u':
            case 'x':
            case 'X':

                if (parameterLengths[typeIndex] == 1) {
                    types.add(Byte.class);
                } else if (parameterLengths[typeIndex] == 2) {
                    types.add(Short.class);
                } else if (parameterLengths[typeIndex] <= 4) {
                    types.add(Integer.class);
                } else {
                    types.add(Long.class);
                }
                break;
            case 'e':
            case 'E':
            case 'f':
            case 'g':
            case 'G':
                if( parameterLengths[typeIndex] == 4 ) {
                    types.add( Float.class );
                }else{
                    types.add( Double.class );
                }
                break;
            case 's':
                types.add( String.class );
                break;
            }
            typeIndex++;
        }

        final Class<?>[] classArray = new Class<?>[types.size()];
        return types.toArray(classArray);
    }


}
