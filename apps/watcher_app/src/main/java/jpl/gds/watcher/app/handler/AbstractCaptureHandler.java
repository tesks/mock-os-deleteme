package jpl.gds.watcher.app.handler;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.ArrayUtils;
import org.apache.velocity.Template;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.CsvQueryProperties;
import jpl.gds.common.config.CsvQueryPropertiesException;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.types.Pair;

/**
 * This class consolidates functionality duplicated in capture handlers.
 * Received messages are verified to be of the appropriate message type before
 * being parsed for storage and/or transmission
 * @param <T>
 *            The interface class for the messages to be processed by the handler
 */
public abstract class AbstractCaptureHandler <T extends IMessage> implements ICaptureHandler {

    /** the handler's tracer */
    protected final Tracer                log;
    /** The handler's application context */
    protected final ApplicationContext    appContext;
    
    /** Number of bytes written to the data stream */
    protected long                        dataBytesWritten = 0;
    /** The variable names for metadata columns to be skipped */
    protected final List<String>          metaSkip       = new ArrayList<>(0);
    
    private BufferedOutputStream          dataOutputStream;
    private BufferedWriter                metaOutputWriter;
    private File                          dataOutput;
    private File                          metadataOutput;
    private Socket                        fwdSocket;
    private DataOutputStream              fwdStream;
    private boolean                       shutdown       = false;
    
    private final IExternalMessageUtility externalMessageUtil;
    private final Object                  processingLock = new Object();

    private final List<String>            metaHeaders = new ArrayList<>(0);
    private final List<String>            metaColumns = new ArrayList<>(0);
    private Template                      styleTemplate;
    private String                        defStyle       = "csv";
    
    private boolean                       headersPrinted = false;
    private byte[]                        syncMarker     = new byte[0];

    private FileOutputStream              dataFileStream;
    private FileWriter                    metaFileWriter;
    private long                          receiveCount   = 0;
    private List<IMessageType>            allowedMessageTypes;
    private boolean                       captureMessages;
    private final SseContextFlag          sseFlag;

    /** A comma for using in metadata */
    public static final String            COMMA          = ",";
    /** A newline character for use in metadata */
    public static final String            NEWLINE        = "\n";
    /** A quotation mark for use in metadata */
    public static final String            QUOTE          = "\"";
    
    /**
     * Constructor
     * @param appContext the current application context
     * @param csvQueryList the csv query list properties to be retrieved
     */
    protected AbstractCaptureHandler(final ApplicationContext appContext, final String csvQueryList) {
        this.appContext = appContext;
        log = TraceManager.getTracer(appContext, Loggers.WATCHER);
        externalMessageUtil = this.appContext.getBean(IExternalMessageUtility.class);
        sseFlag = appContext.getBean(SseContextFlag.class);

        try {
            final Pair<List<String>, List<String>> metaDisplayOrder = CsvQueryProperties.instance()
                    .getCsvLists(csvQueryList);
            metaColumns.addAll(metaDisplayOrder.getOne());
            metaHeaders.addAll(metaDisplayOrder.getTwo());
        } catch (final CsvQueryPropertiesException e) {
            log.error(e.getMessage());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(final IExternalMessage m) {
        try {

            // Do nothing is capture handler has been shut down
            if (shutdown) {
                return;
            }

            // Increment the total message count
            receiveCount++;

            // Get the message type and process the message if it is a type we
            // are interested in
            final IMessageType type = externalMessageUtil.getInternalType(m);
            if (!isSelectedType(type)) {
                return;
            }

            /**
             * Synchronize here rather than on the whole method for performance
             * reasons. The vast majority of the time we do not even make it to
             * this point.
             */
            synchronized (processingLock) {

                final IMessage[] internalMessages = externalMessageUtil.instantiateMessages(m);

                for (final IMessage internalMsg : internalMessages) {

                    final T castMsg = checkedCastMessage(internalMsg);
                    
                    if (castMsg == null || !messageFilterCheck(castMsg)) {
                        continue;
                    }
                    // Write the metadata to the capture file if so configured
                    if (metadataOutput != null) {
                        metaOutputWriter.write(constructMetadataEntry(castMsg));
                        metaOutputWriter.flush();
                    } else {
                        if (!this.headersPrinted) {
                            writeHeader();
                        }
                        log.info(constructMetadataEntry(castMsg));
                    }

                    // Write the data to the capture file if so configured
                    if (dataOutput != null) {
                        final byte[] dataBytes = prepDataBytes(castMsg);
                        

                        dataOutputStream.write(dataBytes);
                        dataOutputStream.flush();

                        dataBytesWritten += dataBytes.length;
                    }

                    if (fwdStream != null) {
                        final byte[] msg = castMsg.toBinary();

                        final byte[] data = ArrayUtils.addAll(syncMarker, msg);

                        try {
                            fwdStream.write(data);
                            fwdStream.flush();
                        } catch (final IOException e) {
                            log.error("Error while writing message to forwarding socket: " + e.getMessage());
                        }
                    }

                }
            }

        }
        catch (final MessageServiceException e) {
            log.error("Error receiving message: " + e.getMessage());
        } catch (final IOException e) {
            log.error("Error writing message to file: " + e.getMessage());
        }
    }

    @Override
    public boolean setDataOutputFile(final String fileName) {
        final File output = new File(fileName);

        if (output.exists() && !output.isFile()) {
            return false;
        }
        try {
            setDataOutput(output);
        } catch (final FileNotFoundException e) {
            log.warn(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean setMetadataOutputFile(final String fileName) {
        final File output = new File(fileName);

        if (output.exists() && !output.isFile()) {
            return false;
        }
        try {
            setMetadataOutput(output);
        } catch (final IOException e) {
            log.warn(e.getMessage());
            return false;
        }

        writeHeader();

        return true;
    }
    
    @Override
    public void setForwardingStream(final String host, final int port) {
        try {

            this.fwdSocket = new Socket(host, port);
            this.fwdStream = new DataOutputStream(this.fwdSocket.getOutputStream());

            

        } catch (final UnknownHostException e) {
            log.error("Unknown host: " + host);
            log.error("Data will not be forwarded");
            if (this.fwdSocket != null) {
                try {
                    this.fwdSocket.close();
                } catch (final IOException e1) {
                    // whatevs
                }
            }
            this.fwdStream = null;
        } catch (final IOException e) {
            log.error("Error establishing connection to " + host + ":" + port + " - " + e.getMessage());
            log.error("Data will not be forwarded");
            if (this.fwdSocket != null) {
                try {
                    this.fwdSocket.close();
                } catch (final IOException e1) {
                    // whatevs
                }
            }
            this.fwdSocket = null;

            if (this.fwdStream != null) {
                try {
                    this.fwdStream.close();
                } catch (final IOException e1) {
                    // whatevs
                }
            }
            this.fwdStream = null;
        }
    }

    @Override
    public void setSyncMarker(final byte[] syncMarker) {
        if (syncMarker == null) {
            this.syncMarker = new byte[0];
        } else {
            this.syncMarker = syncMarker;
        }
    }
    
    @Override
    public void setSyncMarker(final String syncMarker) {
        final byte[] marker;
        if(syncMarker == null) {
            marker = new byte[0];
        } else {
            marker = DatatypeConverter.parseHexBinary(syncMarker);
        }
        
        this.setSyncMarker(marker);
    }
    
    @Override
    public synchronized void setCaptureMessageFilter(final Collection<IMessageType> types) {
        if (types != null) {
            allowedMessageTypes = new ArrayList<>();
           for(final IMessageType type : types) {
                final String temp = type.getSubscriptionTag().trim();
                final IMessageConfiguration config = MessageRegistry.getMessageConfig(temp);
                if (config != null) {
                    allowedMessageTypes.add(config.getMessageType());
                } else {
                    log.warn("Unrecognized message type requested for capture: " + temp + ". Ignored.");
                }
            }
        } else {
            allowedMessageTypes = null;
        }
    }

    @Override
    public void setCaptureMessageStyle(final String dbTableName, final String defStyle) {
        final String style = defStyle != null ? defStyle : "csv";
        this.defStyle = style.toLowerCase();
        if (!this.defStyle.equalsIgnoreCase("csv")) {
            try {
                this.styleTemplate = MissionConfiguredTemplateManagerFactory.getNewDatabaseTemplateManager(sseFlag)
                        .getTemplateForStyle(dbTableName, this.defStyle);
            } catch (final TemplateException e) {
                log.warn("Unable to retrieve specified style, using csv");
                this.defStyle = "csv";
            }
        }
    }
    
    /**
     * Returns the variable names, in order, for the values to be displayed in
     * the CSV format
     * 
     * @return the names of variables to be displayed in CSV format
     */
    public List<String> getCsvColumns(){
        return Collections.unmodifiableList(this.metaColumns);
    }
    
    /**
     * The names of variables that are in the getCsvColumns set that are not
     * supported by the handler.
     * 
     * @return the unsupported CSV variables
     */
    public Collection<String> getMetaSkip() {
        return metaSkip;
    }

    @Override
    public void setCaptureMessages(final boolean captureMessages) {
        this.captureMessages = captureMessages;
    }

    @Override
    public boolean isCaptureMessages() {
        return this.captureMessages;
    }

    /**
     * Sets the value of the data output file member. The File object may
     * represent either a directory or a file.
     * 
     * @param dataOutput
     *            the dataOutput to set, as a File.
     * @throws FileNotFoundException
     *             if the file exists but is a directory rather than a regular file, does not exist but cannot be
     *             created, or cannot be opened for any other reason
     */
    public void setDataOutput(final File dataOutput) throws FileNotFoundException {
        if (null != this.dataOutputStream) {
            if ((null != dataOutput) && !dataOutput.equals(this.dataOutput)) {
                this.dataOutput = dataOutput;
                this.dataOutputStream = null;
            }
        } else {
            this.dataOutput = dataOutput;
        }
        if (dataFileStream == null) {
            dataFileStream = new FileOutputStream(this.dataOutput);
        }
        if (dataOutputStream == null) {
            dataOutputStream = new BufferedOutputStream(dataFileStream);
        }
    }
    
    /**
     * Report if the data from received messages will be written to a file
     * 
     * @return TRUE if data is being written to a local file, FALSE otherwise
     */
    public boolean isDataOutput(){
        return this.dataOutput != null;
    }

    /**
     * Sets the value of the metadata output file member. The File object
     * may represent either a directory or a file.
     * 
     * @param metadataOutput
     *            the metadataOutput to set, as a File.
     * @throws IOException
     *             if the file exists but is a directory rather than a regular file, does not exist but cannot be
     *             created, or cannot be opened for any other reason
     */
    public void setMetadataOutput(final File metadataOutput) throws IOException {
        if (null != this.metaOutputWriter) {
            if ((null != metadataOutput) && !metadataOutput.equals(this.metadataOutput)) {
                this.metadataOutput = metadataOutput;
                this.metaOutputWriter = null;
            }
        } else {
            this.metadataOutput = metadataOutput;
        }
        if (metaFileWriter == null) {
            metaFileWriter = new FileWriter(this.metadataOutput);
        }
        if (metaOutputWriter == null) {
            metaOutputWriter = new BufferedWriter(metaFileWriter);
        }
    }
    
    /**
     * Report if the metadata from received messages will be written to a file
     * 
     * @return TRUE if metadata is being written to a local file, FALSE
     *         otherwise
     */
    public boolean isMetadtaOutput(){
        return this.metadataOutput != null;
    }
    
    /**
     * Gets the total count of data objects received by this object.
     * 
     * @return the receive count
     */
    public long getReceiveCount() {
        return receiveCount;
    }

    /**
     * Indicates if the given message type is one that should be handled.
     * 
     * @param type
     *            the internal message type
     * @return true if the message type is selected for monitoring
     */
    public boolean isSelectedType(final IMessageType type) {
        if (allowedMessageTypes == null || allowedMessageTypes.isEmpty()) {
            return true;
        }
        return allowedMessageTypes.contains(type);
    }
    
    /**
     * Uses the currently selected template to write a header to the metadata
     * output
     */
    protected void writeHeader() {

        String header;

        if (this.defStyle.equalsIgnoreCase("xml")) {
            header = "<?xml version=\"1.0\"?>\n";
        } else if (this.defStyle.equalsIgnoreCase("csv")) {
            header = getCsvHeader();
        } else if (this.defStyle.toUpperCase().contains("CSV")) {
            final HashMap<String, Object> context = new HashMap<>();
            context.put("header", true);
            context.put("vcidColumn", appContext.getBean(MissionProperties.class).getVcidColumnName());
            header = TemplateManager.createText(styleTemplate, context);
        } else {
            return;
        }

        if (this.metadataOutput != null) {
            try {
                metaOutputWriter.write(header);
                metaOutputWriter.flush();
            } catch (final IOException e) {
                log.error("Error writing message to file: " + e.getMessage());
            }
        } else {
            log.info(header);
        }
        this.headersPrinted = true;
    }

    /**
     * Get the CSV style metadata column headers as a single comma delimited String
     * 
     * @return the header column names as a single comma delimited String
     */
    protected String getCsvHeader() {
        final StringBuilder sb = new StringBuilder("recordType");

        for (final String val : this.metaHeaders) {
            sb.append(COMMA).append(val);
        }

        sb.append(NEWLINE);

        return sb.toString();
    }
    
    @Override
    public void shutdown() {
        shutdown = true;
        if (dataFileStream != null) {
            try {
                dataFileStream.close();
            } catch (final IOException e) {
                // do nothing, this is being shut down anyhow
            }
            dataFileStream = null;
        }
        if (dataOutputStream != null) {
            try {
                dataOutputStream.close();
            } catch (final IOException e) {
                // do nothing
            } finally {
                dataOutputStream = null;
            }
        }

        if (metaFileWriter != null) {
            try {
                metaFileWriter.close();
            } catch (final IOException e) {
                // do nothing
            }
            metaFileWriter = null;
        }
        if (metaOutputWriter != null) {
            try {
                metaOutputWriter.close();
            } catch (final IOException e) {
                // do nothing
            } finally {
                metaOutputWriter = null;
            }
        }
        
        if (fwdStream != null) {
            try {
                fwdStream.close();
            } catch (final IOException e) {
                // do nothing
            } finally {
                fwdStream = null;
            }
        }
        
        if (fwdSocket != null) {
            try {
                fwdSocket.close();
            } catch (final IOException e) {
                //do nothing
            } finally {
                fwdSocket = null;
            }
        }
    }
    
    /**
     * casts the IMessage object to the appropriate message type. If the message encounters an error in casting an
     * exception is thrown
     * 
     * @param msg
     *            The message to be cast to its appropriate class
     * @return the casted message
     */
    @SuppressWarnings("unchecked")
    public T castMessage(final IMessage msg){
        return (T) msg;
    }
    
    /**
     * Safely casts the IMessage object to the appropriate message type. If the message encounters an error in casting,
     * the user is warned and a null reference is returned.
     * 
     * @param msg
     *            The message to be cast to its appropriate class
     * @return The message casted to the appropriate message interface, or null if the message is not of the appropriate
     *         type
     */
    public T checkedCastMessage(final IMessage msg) {
        T retMsg = null;
        try {
            retMsg = castMessage(msg);
        } catch (final ClassCastException e){
            log.warn("Received an incompatible message. Expected: " + getMessageClassName()
                    + " Received: " + msg.getClass().getName());
        }
        return retMsg;
    }
    
    /**
     * Check an individual message to verify it passes the filtering criteria for the given handler
     *      For filtering of messages that can only be done after parsing them.
     * @param msg
     *            the message to be validated
     * @return TRUE if the message passes the check against the message filter, FALSE otherwise
     */
    public boolean messageFilterCheck(final T msg) {
        return msg != null;
    }

    /**
     * Returns the metadata for the supplied message in a string format as
     * specified by the currently selected template
     * 
     * @param msg
     *            the message to be transformed
     * @return the string representation of the metadata entry
     */
    protected String constructMetadataEntry(final T msg) {
        if (defStyle.equalsIgnoreCase("csv")) {
            return constructCsvMetadataEntry(msg);
        } else {
            final HashMap<String, Object> context = new HashMap<>();
            context.put("body", true);
            if (this.metadataOutput != null) {
                context.put("formatter", new SprintfFormat());
            }
            context.putAll(messageToMap(msg));
            return TemplateManager.createText(styleTemplate, context);
        }
    }
    
    /*
     * Transfers all of the necessary IMessage elements to a
     * HashMap for parsing by a template
     */
    /**
     * @param msg
     *            the message to be converted to a map
     * @return All values from the message as a hashmap
     */
    protected HashMap<String, Object> messageToMap(final T msg) {
        final HashMap<String, Object> retMap = new HashMap<>();

        msg.setTemplateContext(retMap);

        retMap.put("host", msg.getContextKey().getHost()); //  deprecated for R8 db templates
        retMap.put("testHost", msg.getContextKey().getHost()); // deprecated for R8 db templates

        retMap.put("testKey", msg.getContextKey().getNumber()); // deprecated for R8 db templates

        retMap.put("fileByteOffset", this.dataBytesWritten);

        return retMap;
    }
    
    /**
     * Returns the metadata for the supplied message in a string format for the
     * standard/default CSV format.
     * @param msg the message to be parsed as a CSV entry
     * @return the singular metadata entry as CSV format in a string
     */
    protected abstract String constructCsvMetadataEntry(final T msg);
    
    
    /**
     * Get the data bytes from a message that will be written to the data file
     * 
     * @param msg
     *            the message to be written to file
     * @return the data bytes to be written to file
     */
    protected abstract byte[] prepDataBytes(final T msg);
    
    /**
     * 
     * @return String name of the interface class used for messages in this handler
     */
    protected String getMessageClassName(){
        return "T";
    }
    

}
