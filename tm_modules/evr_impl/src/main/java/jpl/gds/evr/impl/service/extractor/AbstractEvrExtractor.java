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

import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDefinitionProvider;
import jpl.gds.dictionary.api.evr.IEvrDictionaryFactory;
import jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider;
import jpl.gds.evr.api.EvrMetadataKeywordEnum;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.IEvrFactory;
import jpl.gds.evr.api.config.EvrProperties;
import jpl.gds.evr.api.service.extractor.*;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.*;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.spring.bootstrap.SharedSpringBootstrap;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * AbstractEvrAdaptor is the super-class for all EVR Adaptations. The EVR mission
 * adaptation is responsible for extracting EVRs from packets and properly
 * interpreting them into Evr objects.
 * 
 */
public abstract class AbstractEvrExtractor implements IEvrExtractor {
    /** Shared logger instance */
    protected static Tracer log;

    /**
     * The name of the unknown evr level
     */
     public final static String UNKNOWN_KEYWORD = "UNKNOWN";

    /* add constants for valid EVR message character range. */
    private static final char MIN_CHAR         = StringUtil.SQL_MIN_CHAR;
    private static final char MAX_CHAR         = StringUtil.SQL_MAX_CHAR;

    /*  Default the FATAL level */
    private String fatalKeyword_ = "FATAL";


    private Boolean saveSourceFile_ = Boolean.FALSE;
    
    /** Flag indicating whether LST times should be generated. */
    protected boolean setSolTimes;

    protected ICommandDefinitionProvider commandDefProvider;
    /** Sequence dictionary */
    protected ISequenceDefinitionProvider seqDict;
    /** Map of EVR definitions */
    protected Map<Long, IEvrDefinition> definitionMap;
    /** Publication bus to use for message publication */
    protected IMessagePublicationBus bus;
    /** Print formatter object */
    protected SprintfFormat formatter;
    /** EVR extractor utility */
    protected IEvrExtractorUtility extractUtil;
    /** Raw EVR data factory */
    protected IRawEvrDataFactory           rawDataFactory;

    protected int currentOffset = 0;
       
    /*
     *  Removed unnecessary maps of EVR levels. EVR
     * levels have no associated numbers -- not in packets, and not in the
     * dictionary. And it really does not matter to any EVR adaptor what the
     * levels are, other than the FATAL one.
     */

    /**
     * The IPacketMessage currently being processed.
     */
    protected ITelemetryPacketMessage lastPm_;

    /**
     * Set containing allowed lengths in bytes for integer types
     */
    protected Set<Integer> allowedIntegerLengths_;
    /**
     * Set containing allowed lengths in bytes for floating point types
     */
    protected Set<Integer> allowedFloatingPointLengths_;
    /**
     * Set containing allowed format prefixes for integer types
     */
    protected Set<String> allowedIntegerFormatPrefixes_;

    /**
     * List of EVR metadata keys.
     * 
     */
    protected List<EvrMetadataKeywordEnum> metaKeys = null;

    /**
     * List of EVR metadata values.
     */
    protected List<String> metaData = null;
    
    /** Factory for creating EVR objects */
    protected IEvrFactory evrFactory;
    
    
    /** Added EVR table member. Cached EVR table instance. */
    protected EvrProperties evrProps;

    private final IStatusMessageFactory statusMessageFactory;

    /** Current dictionary configuration object */
    protected DictionaryProperties      dictConfig;

	protected IEvrDictionaryFactory evrDictFactory;

    protected final SseContextFlag         sseFlag;
    

    /**
     * Constructor.
     * 
     * @param context
     *            the current application context
     * @throws EvrExtractorException
     *             if there is a problem creating the extractor
     */
    public AbstractEvrExtractor(final ApplicationContext context) throws EvrExtractorException {

        // initialize the allowed lengths
        initializeAllowedLengths();
        initializeAllowedFormatPrefixes();
        
        this.evrFactory = context.getBean(IEvrFactory.class);
        this.evrDictFactory = context.getBean(IEvrDictionaryFactory.class);
        this.evrProps = context.getBean(EvrProperties.class);
        log = TraceManager.getTracer(context, Loggers.TLM_EVR);
        this.statusMessageFactory = context.getBean(IStatusMessageFactory.class);
        this.dictConfig = context.getBean(DictionaryProperties.class);
        
        final IEvrDefinitionProvider evrDict = context.getBean(IEvrDefinitionProvider.class);
   
        this.definitionMap = new HashMap<Long, IEvrDefinition>(evrDict.getEvrDefinitionMap());
        
        try {
            this.seqDict = context.getBean(ISequenceDefinitionProvider.class);
        } catch (final BeanCreationException e) {
            if (!e.contains(DictionaryException.class)) {
                throw new EvrExtractorException("Sequence dictionary could not be created from the service context", e);
            }
            
        }

        try {
            this.commandDefProvider = context.getBean(ICommandDefinitionProvider.class);
        
        } catch (final BeanCreationException e) {
            if (!e.contains(DictionaryException.class)) {
                throw new EvrExtractorException("Command dictionary could not be created from the service context", e);
            }
        }
      
        bus = context.getBean(SharedSpringBootstrap.PUBLICATION_BUS, IMessagePublicationBus.class);

        this.setSolTimes = context.getBean(EnableLstContextFlag.class).isLstEnabled();
        final int scid = context.getBean(IContextIdentification.class).getSpacecraftId();
        
        this.formatter = new SprintfFormat(scid);
        
        this.extractUtil = context.getBean(IEvrExtractorUtility.class);

        this.rawDataFactory = context.getBean(IRawEvrDataFactory.class);

        this.sseFlag = context.getBean(SseContextFlag.class);
    }

    /**
     * initializeAllowedLengths initializes the sets containing the allowed
     * lengths for integer and floating point numbers
     */

    private void initializeAllowedLengths() {

        allowedIntegerLengths_ = new TreeSet<Integer>();
        allowedFloatingPointLengths_ = new TreeSet<Integer>();

        allowedIntegerLengths_.add(1);
        allowedIntegerLengths_.add(2);
        allowedIntegerLengths_.add(4);
        allowedIntegerLengths_.add(8);

        allowedFloatingPointLengths_.add(4);
        allowedFloatingPointLengths_.add(8);
    }

    /**
     * initializeAllowedFormatPrefixes initializes the set of allowed prefixes
     * for the various types
     */

    private void initializeAllowedFormatPrefixes() {

        allowedIntegerFormatPrefixes_ = new TreeSet<String>();

        allowedIntegerFormatPrefixes_.add("h");
        allowedIntegerFormatPrefixes_.add("l");
        allowedIntegerFormatPrefixes_.add("L");
    }

    /**
     * Retrieves the fatal evr level name from the current
     * configuration and saves the level name that represents a fatal EVR.
     * 
     * Removed throws clause. Having no FATAL
     *          EVR level is acceptable. It will default.
     */
    protected void saveFatalKeyword() {

        fatalKeyword_ = evrProps.getFatalEvrLevel(sseFlag.isApplicationSse());
    }
    

    /**
     * Retrieves the flag that dictates if the source file of
     * the EVR is to be saved in the EVR metadata
     * @throws EvrExtractorException if no save source file flag defined in the configuration
     */
    protected void saveSourceFileFlag() throws EvrExtractorException {

        saveSourceFile_ = evrProps.getSaveSources();
       
    }


    /**
     * Helper method to add metadata to an Evr object.
     * 
     * @param evr
     *            the Evr to add metadata to
     * @param metaKeys
     *            a list of metadata keys (Strings)
     * @param metaData
     *            a list of metadata values (Strings)
     */
    protected void addMetadata(final IEvr evr,
            final List<EvrMetadataKeywordEnum> metaKeys,
            final List<String> metaData) {
        evr.setMetadataKeyValuesFromStrings(metaKeys, metaData);
    }


    /**
     * Log an EVR message at specified severity.
     * 
     * @param apid  APID of the packet that caused the problem
     * @param evr   Evr object constructed so far
     * @param msg   Message describing the failure
     * @param level Message severity level
     */
    private void logEvrMessage(final int           apid,
            final IEvr          evr,
            final String        msg,
            final TraceSeverity level)
    {
        final StringBuilder buf = new StringBuilder(1024);

        buf.append(msg).append(" [apid=").append(apid);

        if (evr != null)
        {
            buf.append(", event_id=").append(evr.getEventId());

            buf.append(", sclk=").append(evr.getSclk());

            final IAccurateDateTime ert = evr.getErt();

            buf.append(", ert=");

            if (ert != null)
            {
                buf.append(TimeUtility.format(ert));
            }
            else
            {
                buf.append("unknown");
            }
        }

        buf.append(']');
        final IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(level, buf.toString(),
                LogMessageType.GENERAL);
        bus.publish(logm);
        log.log(logm);
    }


    /**
     * Log an error to the log file.
     * 
     * @param apid
     *            the apid of the packet that caused the problem
     * @param evr
     *            the Evr object constructed so far
     * @param msg
     *            the error message describing the failure
     */
    protected void logError(final int apid, final IEvr evr, final String msg)
    {
        logEvrMessage(apid, evr, msg, TraceSeverity.ERROR);
    }


    /**
     * Log a warning to the log file.
     * 
     * @param apid
     *            the apid of the packet that caused the problem
     * @param evr
     *            the Evr object constructed so far
     * @param msg
     *            the error message describing the warning
     */
    protected void logWarn(final int apid, final IEvr evr, final String msg)
    {
        logEvrMessage(apid, evr, msg, TraceSeverity.WARNING);
    }


    /**
     * Fail in processing an Evr by throwing an exception.
     * 
     * @param apid
     *            the apid of the packet causing the problem
     * @param vcid
     *            the vcid of the source frame
     * @param dssId 
     *            the station ID of the source frame           
     * @param seqCount
     *            the sequence count of the packet
     * @param evr
     *            the Evr object constructed so far
     * @param msg
     *            an error message
     * @throws EvrExtractorException
     *             every time
     */
    protected void fail(final int apid, final Integer vcid, final int dssId,
            final int seqCount, final IEvr evr, final String msg)
                    throws EvrExtractorException {
        final StringBuilder buf = new StringBuilder(1024);
        buf.append(msg);
        if (evr != null) {
            buf.append(" [apid=" + apid);
            buf.append(", vcid=" + vcid);
            buf.append(", dss_id=" + dssId);
            buf.append(", spsc=" + seqCount);
            if (evr != null) {
                buf.append(", sclk=" + evr.getSclk());
                buf.append(", event_id=" + evr.getEventId());
            }
            buf.append("]");
        }
        // Hexdump below disabled
        // buf.append(jpl.gds.util.Dumper.dumpstr(this.lastPm_.getPkt(), 0,
        // this.lastPm_.getNumBytes()));
        throw new EvrExtractorException(buf.toString());
    }

    /**
     * Fail in processing an Evr by throwing an exception.
     * 
     * @param apid
     *            the apid of the packet causing the problem
     * @param evr
     *            the Evr object constructed so far
     * @param msg
     *            an error message
     * @throws EvrExtractorException
     *             every time
     */
    protected void makeBadEvr(final int apid, final IEvr evr, final String msg)
            throws EvrExtractorException {
    	/*
    	 * Set the badEvr flag to true
    	 */
    	evr.setBadEvr(true);
        setEvrMessage(evr, "EVR processing error: " + msg);
        logWarn(apid, evr, msg);
    }


    /**
     * Reads the raw evr data parameters and the list of
     * requested formats and converts the evr data parameters to a type suitable
     * for printing.
     * 
     * @param rawParameters
     *            - the list of raw data parameters
     * @param requestedFormats
     *            - the list of formats as specified in the evr definition file
     * @return a list of objects suitable for printing
     * @throws EvrExtractorException if there is any problem extracting parameter
     */
    protected List<Object> getFormattedParameters(
            final List<IRawEvrData> rawParameters,
            final List<String> requestedFormats) throws EvrExtractorException {

        final List<Object> formattedParameters = new ArrayList<Object>(
                rawParameters.size());

        final Iterator<IRawEvrData> parameterIterator = rawParameters.iterator();
        final Iterator<String> formatIterator = requestedFormats.iterator();

        while (parameterIterator.hasNext()) {

            final String currentFormat = formatIterator.next();
            final IRawEvrData currentData = parameterIterator.next();

            if (!formattedParameters.add(currentData.formatData(currentFormat))) {

                throw new EvrExtractorException(
                        "Could not process formatted EVR data element: "
                                + currentData.getDumpDataInformation());
            }

        }
        return formattedParameters;
    }


    /**
     * Checks that the level for the input level is the fatal level
     * as defined by the mission
     * 
     * @param inputLevel
     *            string representation of the level to check
     * @return true if the input level is the fatal level; and false otherwise
     */
    public boolean evrIsFatal(final String inputLevel) {

        /*  Ok to have to FATAL level. */
        if (fatalKeyword_ == null) {
            return false;
        }
        return inputLevel.equalsIgnoreCase(fatalKeyword_);
    }
    

    /**
     * typeIsInteger checks that the class type of inputClass is either Byte,
     * Short, Integer, or Long. This is used to convert byte arrays to the
     * appropriate data types for use in EVR messages.
     * 
     * @param inputClass
     *             the class to check
     * @return true if inputClass is one of Byte, Short, Integer, or Long; false
     *         otherwise
     */
    protected boolean typeIsInteger(final Class<?> inputClass) {

        return (inputClass == Integer.class || 
                inputClass == Long.class ||
                inputClass == Short.class ||
                inputClass == Byte.class);
    }

    /**
     * typeIsFloat checks that the class type of inputClass is either Float or
     * Double. This is used to convert byte arrays to the appropriate floating
     * point data types for use in EVR messages.
     * 
     * @param inputClass
     *            the class to check
     * @return true if inputClass is one of Float or Double; false otherwise
     */
    protected boolean typeIsFloat(final Class<?> inputClass) {

        return (inputClass == Float.class ||
                inputClass == Double.class);
    }

    /**
     * Checks that the source file flag saveSourceFile_ has been
     * set.
     * 
     * @return true if saveSourceFile_ is Boolean( true ); false otherwise.
     */
    protected boolean saveSourceFiles() {
        return saveSourceFile_.booleanValue();
    }

    /**
     * <code>saveSourceString</code> extracts the source file string from the
     * EVR message and returns an EVR message without the source file value. The
     * source file value is saved in the metadata information for the EVR.
     * 
     * @param inputMessage
     *            EVR message with the source file value at the beginning
     * @return modified EVR message without the source file value
     */
    protected String saveSourceString( final String inputMessage )  {

        String strippedMessage = inputMessage;
        final int endp = inputMessage.indexOf(')');
        if (endp != -1) {
            final String source = inputMessage.substring(1, endp);
            strippedMessage = inputMessage.substring(endp + 2);
            metaKeys.add(EvrMetadataKeywordEnum.SOURCE);
            metaData.add(source);
        }
        return strippedMessage;
    }

    /**
     * Sets the final message string into the EVR, stripping leading and
     * training white space.
     *     
     * @param evr the EVR to set the message into
     * @param inputMessage the EVR message string with arguments replaced     
     */
    protected void setEvrMessage(final IEvr evr, final String inputMessage) {

        String finalMessage = inputMessage;
        if (saveSourceFiles()) {
            finalMessage = saveSourceString(inputMessage);
        }
        /* Clean the EVR message more thoroughly. */
        evr.setMessage(this.cleanEvrMessage(finalMessage));
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.evr.api.service.extractor.IEvrExtractor#extractEvr(jpl.gds.tm.service.api.packet.ITelemetryPacketMessage)
     */
    @Override
    public abstract IEvr extractEvr(ITelemetryPacketMessage pm)
            throws EvrExtractorException;

    /**
     * {@inheritDoc}
     * @see jpl.gds.evr.api.service.extractor.IEvrExtractor#extractEvr(byte[], int, int, int, java.lang.Integer, int, int)
     */
    @Override
    public IEvr extractEvr(final byte[] buff, final int startOffset,
            final int length, final int apid, final Integer vcid,
            final int dssId, final int seqCount) throws EvrExtractorException {
        return extractEvr(buff, startOffset, length, apid, vcid, dssId,
                seqCount, evrFactory.createEvr());
    }

    /**
     * Create an EVR object from the bytes in a CCSDS packet. Return
     * <code>null</code> if the packet does not hold an EVR. Throw an exception
     * if there is some corruption, such as when the parameter count or lengths
     * are invalid. In the case of a fatal EVR with some corruption, attempt to
     * return the best possible EVR without throwing an exception.
     * 
     * @param buff
     *            byte array buffer of a packet that may contain an EVR
     * @param startOffset
     *            starting address of buffer memory to process
     * @param length
     *            length of the buffer memory to process, starting from
     *            <code>startOffset</code>
     * @param apid
     *            APID number of the packet, to which <code>buff</code> data
     *            belongs to
     * @param vcid
     *            virtual channel ID of the source frame
     * @param dssId
     *            station ID of the source frame           
     * @param seqCount
     *            sequence count of the packet
     * @param evr
     *            an <code>IEvr</code> object that should carry the EVR data
     *            upon return
     * @return <code>IEvr</code> object that was passed to the method as
     *         <code>evr</code>, but having its contents populated with the
     *         proper EVR data that's processed from <code>buff</code>
     * @throws EvrExtractorException
     *             thrown if problem is encountered while processing the EVR
     */
    protected abstract IEvr extractEvr(final byte[] buff,
            final int startOffset, final int length, final int apid,
            final Integer vcid, final int dssId, final int seqCount,
            final IEvr evr) throws EvrExtractorException;

    /**
     * Cleans an EVR message string of non-printables and control
     * characters by replacing them with spaces. 
     * @param message message string to clean
     * @return cleaned string
     * 
     */
    protected String cleanEvrMessage(final String message) {
        if (message == null) {
            return null;
        }

        final StringBuilder finalMessage = new StringBuilder(message.trim());

        final int len  = finalMessage.length();

        for (int i = 0; i < len; i++)
        {
            final char c = finalMessage.charAt(i);

            if ((c < MIN_CHAR) && (c > MAX_CHAR))
            {
                finalMessage.setCharAt(i, ' ');
            }
        }

        return finalMessage.toString();
    }


    /**
     * {@inheritDoc}
     */
    public int getCurrentOffset() {
        return currentOffset;
    }
}
