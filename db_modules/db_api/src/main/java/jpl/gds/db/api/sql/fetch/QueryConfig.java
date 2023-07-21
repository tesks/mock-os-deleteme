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
package jpl.gds.db.api.sql.fetch;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.xml.parse.SAXParserPool;

/**
 * Class to look up query configurations from the XML files.
 *
 */
public class QueryConfig extends DefaultHandler {
    private static Tracer                    parseLogger                    = TraceManager
            .getTracer(Loggers.CONFIG);


    /** PARAM_SERIES_REPLACEMENT_TOKEN */
    public static final String                 PARAM_SERIES_REPLACEMENT_TOKEN = "${value-list}";

    private static final String                QUERY_CONFIG_FILE              = "query_config.xml";

    /** MPCS-8384 See below */
    private final Set<String>                  tableNames                     = new HashSet<>();

    private final Map<String, TableQuerySet>   queryTable                     = new HashMap<>();
    private StringBuilder                      buffer;
    private TableQuerySet                      currentQuerySet;

    private final Map<String, String>          tablePrefixes                  = new HashMap<>();

    private final Map<String, String>          bodyPrefixes                   = new HashMap<>();

    private final Map<String, String>          packetPrefixes                 = new HashMap<>();

    private final IMySqlAdaptationProperties dbProperties;

    private final SseContextFlag             sseFlag;

    /** MPCS-8384 Support for extended tables */
    private void init() {
        // @formatter:off
        final List<String> temp = Arrays.asList(IDbTableNames.DB_FRAME_DATA_TABLE_NAME,
                                                IDbTableNames.DB_PACKET_DATA_TABLE_NAME,
                                                IDbTableNames.DB_SSE_PACKET_DATA_TABLE_NAME,
                                                IDbTableNames.DB_EVR_DATA_TABLE_NAME,
                                                IDbTableNames.DB_SSE_EVR_DATA_TABLE_NAME,
                                                IDbTableNames.DB_EVR_METADATA_TABLE_NAME,
                                                IDbTableNames.DB_SSE_EVR_METADATA_TABLE_NAME,
                                                IDbTableNames.DB_LOG_MESSAGE_DATA_TABLE_NAME,
                                                IDbTableNames.DB_COMMAND_MESSAGE_DATA_TABLE_NAME,
                                                IDbTableNames.DB_PRODUCT_DATA_TABLE_NAME,
                                                IDbTableNames.DB_CHANNEL_VALUE_TABLE_NAME,
                                                IDbTableNames.DB_MONITOR_CHANNEL_VALUE_TABLE_NAME,
                                                IDbTableNames.DB_HEADER_CHANNEL_VALUE_TABLE_NAME,
                                                IDbTableNames.DB_SSE_CHANNEL_VALUE_DATA_TABLE_NAME,
                                                IDbTableNames.DB_CHANNEL_DATA_TABLE_NAME,
                                                IDbTableNames.DB_SESSION_DATA_TABLE_NAME,
                                                IDbTableNames.DB_END_SESSION_DATA_TABLE_NAME,
                                                IDbTableNames.DB_CONTEXT_CONFIG_TABLE_NAME,
                                                IDbTableNames.DB_CONTEXT_CONFIG_KEYVAL_TABLE_NAME,
                                                IDbTableNames.DB_CFDP_INDICATION_DATA_TABLE_NAME,
                                                IDbTableNames.DB_CFDP_FILE_GENERATION_DATA_TABLE_NAME,
                                                IDbTableNames.DB_CFDP_FILE_UPLINK_FINISHED_DATA_TABLE_NAME,
                                                IDbTableNames.DB_CFDP_REQUEST_RECEIVED_DATA_TABLE_NAME,
                                                IDbTableNames.DB_CFDP_REQUEST_RESULT_DATA_TABLE_NAME,
                                                IDbTableNames.DB_CFDP_PDU_RECEIVED_DATA_TABLE_NAME,
                                                IDbTableNames.DB_CFDP_PDU_SENT_DATA_TABLE_NAME,
                                                IDbTableNames.DB_CHANNEL_AGGREGATE_TABLE_NAME,
                                                IDbTableNames.DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME,
                                                IDbTableNames.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME,
                                                IDbTableNames.DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME);
        // @formatter:on

        /*
         * Code from R7.5
         */
        final String postfix = dbProperties.getExtendedPostfix();
        final Set<String> etables = dbProperties.getExtendedTables();
        final StringBuilder sb = new StringBuilder();

        for (final String s : temp) {
            tableNames.add(s);

            if (etables.contains(s)) {
                sb.setLength(0);

                sb.append(s).append(postfix);

                tableNames.add(sb.toString());
            }
        }
    }

    /**
     * @param dc
     *            the Adaptaiton's Archive Properties
     * @param sseFlag
     *            the SSE context flag
     */
    public QueryConfig(final IMySqlAdaptationProperties dc, final SseContextFlag sseFlag) {
        this.dbProperties = dc;
        this.sseFlag = sseFlag;
        init();
        loadNoThrow();
    }

    /**
     * Get query clause.
     *
     * @param type
     *            Query clause type
     * @param tableName
     *            Table name
     *
     * @return Query clause
     */
    public String getQueryClause(final QueryClauseType type, final String tableName) {
        final TableQuerySet set = queryTable.get(tableName);
        if (set == null) {
            return null;
        }

        return set.getQuery(type);
    }

    /**
     * MPCS-7331  - Updated method to use
     * GdsSystemProperties.getFullConfigPathList instead of the internal and
     * fixed list
     */
      private List<String> getQueryConfigFiles() {
        final List<String> dirs = GdsSystemProperties.getFullConfigPathList(sseFlag.isApplicationSse());
        final ArrayList<String> result = new ArrayList<>();
        
        String filename;
        
        for(final String dir : dirs){
            filename = dir + File.separator + QUERY_CONFIG_FILE;
            if (new File(filename).exists()) {
                result.add(filename);
            }
        }
        
        return result;
    }

    /**
     * Load query-config files.
     *
     * @throws DictionaryException
     *             Dictionary exception
     */
    public void load() throws DictionaryException {
        final List<String> filesToLoad = getQueryConfigFiles();

        this.queryTable.clear();

        for (final String file : filesToLoad) {
            parse(file);
        }
    }

    /**
     * Load query-config files without throwing
     * 
     * A failure to load will generate a stackTrace, and return an uninitialized
     * QueryConfig object
     * 
     * @return the QueryConfig object.
     */
    public QueryConfig loadNoThrow() {
        try {
            load();
        }
        catch (final DictionaryException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Get table prefix.
     *
     * @param tableName
     *            Table name
     *
     * @return Table prefix for table
     */
    public String getTablePrefix(final String tableName) {
        return this.tablePrefixes.get(tableName);
    }

    /**
     * Get table body prefix.
     *
     * @param tableName
     *            Table name
     *
     * @return Table prefix for table
     */
    public String getBodyPrefix(final String tableName) {
        return this.bodyPrefixes.get(tableName);
    }

    /**
     * Get packet prefix for a table.
     *
     * @param tableName
     *            Table name
     *
     * @return Packet prefix for table
     */
    public String getPacketPrefix(final String tableName) {
        return packetPrefixes.get(tableName);
    }

    /**
     * Parses the given XML File to populate the query definitions.
     *
     * @param filename
     *            the path to the XML file
     *
     * @throws DictionaryException
     *             Dictionary exception
     */
    public void parse(final String filename) throws DictionaryException {
        if (filename == null) {
            if (!sseFlag.isApplicationSse()) {
                parseLogger.error("Query configuration file path is undefined.");
            }
            throw new DictionaryException("Query configuration file path is undefined.");
        }
        final File path = new File(filename);
        parseLogger.debug("Parsing query definitions from ", FileUtility.createFilePathLogMessage(path));

        SAXParser sp = null;
        try {
            TraceManager.getDefaultTracer().debug("Parsing " + path.getCanonicalPath());

            sp = SAXParserPool.getInstance().getNonPooledParser();
            sp.parse(path, this);
        }
        catch (final SAXException e) {
            parseLogger.error(e.getMessage());
            throw new DictionaryException(e.getMessage(), e);
        }
        catch (final ParserConfigurationException e) {
            parseLogger.error("Unable to configure sax parser to read APID definition file");
            throw new DictionaryException("Unable to configure sax parser to read query definition file", e);
        }
        catch (final Exception e) {
            parseLogger.error("Unexpected error parsing or reading query definition file");
            throw new DictionaryException("Unexpected error parsing or reading query definition file", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName, final String qname, final Attributes attr)
            throws SAXException {

        newBuffer();

        for (final String name : tableNames) {
            if (qname.equalsIgnoreCase(name)) {
                this.currentQuerySet = this.queryTable.get(name);
                if (this.currentQuerySet == null) {
                    this.currentQuerySet = new TableQuerySet(name);
                    this.queryTable.put(name, this.currentQuerySet);
                }

                String prefix = attr.getValue("prefix");

                if (prefix != null) {
                    this.tablePrefixes.put(name, prefix);
                }

                prefix = attr.getValue("packetPrefix");

                if (prefix != null) {
                    this.packetPrefixes.put(name, prefix);
                }

                prefix = attr.getValue("bodyPrefix");

                if (prefix != null) {
                    this.bodyPrefixes.put(name, prefix);
                }

                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(final String uri, final String localName, final String qname) throws SAXException {
        final QueryClauseType[] types = QueryClauseType.values();
        for (final QueryClauseType type : types) {
            final String tag = type.getXmlTag();
            if (qname.equalsIgnoreCase(tag)) {
                this.currentQuerySet.addQuery(type, getBufferText());
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
    public void characters(final char[] chars, final int start, final int length) throws SAXException {

        if (this.buffer == null) {
            newBuffer();
        }
        final String newText = new String(chars, start, length);
        if (!newText.equals("\n")) {
            this.buffer.append(newText);
        }
    }

    /**
     * Returns the current content on the text buffer as a String.
     * 
     * @return the text String
     */
    protected String getBufferText() {
        if (this.buffer == null) {
            return null;
        }
        return this.buffer.toString().trim();
    }

    /**
     * Starts a new text buffer, which captures text parsed from XML element
     * values.
     */
    protected void newBuffer() {
        this.buffer = new StringBuilder();
    }

    /**
     * Class to hold queries read from a single query-config file.
     */
    private static class TableQuerySet {
        private final Map<QueryClauseType, String> queries;

        public TableQuerySet(final String tableName) {
            queries = new HashMap<QueryClauseType, String>();
        }

        public void addQuery(final QueryClauseType type, final String query) {
            this.queries.put(type, query);
        }

        public String getQuery(final QueryClauseType type) {
            return this.queries.get(type);
        }
    }
}
