/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.dictionary.mapper;

import com.google.common.hash.Hashing;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.mapper.IFswToDictMapping;
import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.xml.parse.SAXParserPool;
import org.apache.commons.io.IOUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * This class was copied and modified from the MSL/M20 PDPP implementation. Not all of the code here is necessary for
 * non-M20 adaptations, but we did not have time during the PDPP multimissionization effort to simplify it. For now,
 * all adaptations will rely on an implementation of IFswToDictionaryMapper and a mapper xml file.
 *
 * Comments were brought over intact for their historical value.
 */
public class ReferenceFswToDictionaryMapper extends DefaultHandler implements IFswToDictionaryMapper {

    /**
     * Trace logger for parser messages that should be logged into the AMPCS
     * database or published onto the AMPCS message service.
     */
    private final Tracer parserLog;

    /**
     * 02/07/2012  - MPCS-3166
     * <p>
     * If FSW Build ID maps to a different Dictionary Version than that being used by the current session, then use a highly
     * identifiable however nonsensical Dictionary Version when creating the .emd file.
     * <p>
     * After a discussion with Maher and his lead developer Alice, and a quick check-out with Dan Allard, we have agreed that the
     * FswDictionaryVersion value in the .emd file and the Product table of the database for products downlinked with an FSW Build
     * ID that maps to a different FSW Dictionary Version than that of the current downlink session shall be R0_0_0_00000000_00.
     */
    public static final String UNMATCHED_DICTIONARY_VERSION = "R0_0_0_00000000_00";

    // MPCS-8131 04/11/16 - moved FSW_TO_DICT_MAPPING filepath and file property names and default values to DictionaryConfiguration.

    // MPCS-8131  04/12/16 - Removed ECR & MPDU info

    private final Map<Long, SortedSet<ReferenceFswToDictMapping>> fswIdToDictMap;                                                                // one to many mapping - many due to multiple directories
    private final Map<String, SortedSet<ReferenceFswToDictMapping>> dictToMappingMap;                                                            // one to many mapping - due to ground
    private final Map<String, SortedSet<Long>> dictToFswIdMap;                                                                // one to many mapping

    /**
     * Parsing variables.
     */
    private String mappingVersion;
    private String schemaVersion;
    private String xmlPath;
    private StringBuilder text;
    private FswToDictMappingBuilder mappingBuilder;
    // MPCS-8568  - 12/07/16 - added
    private final ReferenceFswToDictMapping unmatchedDictionary;

    /**
     * MPCS-8773  - 1/26/2017 - Keeping track of the mapper file and a sha256 of the file so that
     * we can reload the mapper file when we detect there was a change.
     */
    private String mapperFilePath;
    private String sha256 = "";

    // MPCS-8131  05/02/16 removed getInstance - now handled by FswToDicionaryMapperFactory getStaticInstance

    // MPCS-8131  04/12/16 - removed ECR methods

    /*
     *  MPCS-8131  05/02/16 removed create and destroy - now handled by FswToDicionaryMapperFactory getStaticInstance
     *  		  and  resetStaticInstance
     */

    /**
     * Default constructor. Creates an empty mapper
     *
     * @throws DictionaryException if an error is encountered while creating the mapper
     */
    public ReferenceFswToDictionaryMapper() throws DictionaryException {
        this(null);
    }

    public ReferenceFswToDictionaryMapper(final String mapFile) throws DictionaryException {
        super();
        this.schemaVersion = null;
        this.fswIdToDictMap = new HashMap<>(10);
        this.dictToMappingMap = new HashMap<>(10);
        this.dictToFswIdMap = new HashMap<>(10);
        this.parserLog = TraceManager.getTracer(Loggers.DICTIONARY);

        if (mapFile != null) {
            mapperFilePath = mapFile;
            parse(mapFile);
        }

        unmatchedDictionary = new FswToDictMappingBuilder().build();
    }

    /**
     * Calculates the sha 256 of the mapper file.  If the mapper file is not set or doesn't exists returns the empty string.
     *
     * @return hash string
     * @throws FileNotFoundException
     * @throws IOException
     */
    private String getMapperSHA256(final String mapFilePath) throws FileNotFoundException, IOException {
        if (mapFilePath == null) {
            return "";
        } else {
            final File mapFile = new File(mapFilePath);

            if (mapFile.exists()) {
                try (FileInputStream fos = new FileInputStream(mapFile)) {
                    return Hashing.sha256().hashBytes(IOUtils.toByteArray(fos)).toString();
                }
            } else {
                return "";
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper#reload()
     */
    @Override
    public boolean reload() throws DictionaryException {
        try {
            final String sha = getMapperSHA256(mapperFilePath);

            if (!sha.equals(sha256)) {
                // The files are different, so we need to load it again.
                parse(mapperFilePath);
                return true;
            } else {
                return false;
            }
        } catch (final IOException e) {
            throw new DictionaryException("Failed to calculate hash on mapper file: " + e.getMessage());
        }
    }

    /**
     * @return map of FswBuildIds to GDS_VERSION_ID Strings.
     */
    @Override
    public Map<Long, String> getFswIdToDictVersionMap() {
        final Map<Long, String> map = new HashMap<Long, String>();
        for (final Map.Entry<Long, SortedSet<ReferenceFswToDictMapping>> entryKeyValue : fswIdToDictMap.entrySet()) {


            final SortedSet<ReferenceFswToDictMapping> entries = entryKeyValue.getValue();

            for (final ReferenceFswToDictMapping entry : entries) {
                map.put(entryKeyValue.getKey(), entry.getDictionaryVersion());
            }
        }
        return map;
    }

    /**
     * Returns the correct full Dictionary Version for the provided FSW ID.
     *
     * @param fswId
     * @return the correct full Dictionary Version for the provided FSW ID.
     */
    @Override
    public IFswToDictMapping getDictionary(final long fswId) {
        try {
            return fswIdToDictMap.get(fswId).last();
        } catch (final Exception e) {
            return unmatchedDictionary;
        }
    }

    //MPCS-8411  09/07/16 - removed getDictionary(final SortedSet<Long> fswIds)

    /**
     * Returns the correct Flight Release Dictionary Version for the provided
     * FSW ID. This value is truncated and does not contain the Ground Revision.
     *
     * @param fswId FSW ID for which a Flight Release Dictionary is being requested
     * @return the correct Flight Release Dictionary Version for the provided
     * FSW ID.
     * MPCS-8131 - 05/20/16 - added method
     */
    @Override
    public String getReleaseVersionId(final long fswId) {
        try {
            return fswIdToDictMap.get(fswId).last().getFswReleaseVersion();
        } catch (final Exception e) {
            return UNMATCHED_DICTIONARY_VERSION;
        }
    }

    /**
     * Returns the correct Flight Released Dictionary Version for the provided
     * FSW IDs This value is truncated and does not contain the Ground Revision.
     * If the provided set of FSW ID values do not have the same dictionary, or
     * there is no dictionary for all of the values, then
     * UNMATCHED_DICTIONARY_VERSION is returned.
     *
     * @param fswIds FSW IDs for which a flight release dictionary is being requested
     * @return the correct
     */
    public String getReleaseVersionId(final SortedSet<Long> fswIds) {
        String release = null;
        try {
            for (final Long l : fswIds) {
                final String tmpRelease = fswIdToDictMap.get(l).last().getFswReleaseVersion();

                if (release == null) {
                    release = tmpRelease;
                } else if (!release.equals(tmpRelease)) {
                    release = UNMATCHED_DICTIONARY_VERSION;
                    break;
                }
            }
        } catch (final Exception e) {
            //do nothing
        }
        return (release == null) ? UNMATCHED_DICTIONARY_VERSION : release;
    }

    /**
     * Returns the FSW Build IDs for the given Dictionary Version. More than one
     * FSW Build ID can utilize a single dictionary.
     *
     * @param dict dict the name (only, not path) of the dictionary file to find
     * @return the sorted set of FSW Build IDs mapped to the given Dictionary
     * Version.
     */
    @Override
    public SortedSet<Long> getBuildVersionIds(final String dict) {
        final SortedSet<Long> ids = new TreeSet<Long>();
        final SortedSet<ReferenceFswToDictMapping> mappings = dictToMappingMap.get(dict);
        if (null != mappings) {
            for (final ReferenceFswToDictMapping mapping : mappings) {
                ids.add(mapping.getFswBuildVersion());
            }
        }
        return ids;
    }


    /**
     * MPCS-8131 05/16/16 - Moved both isExists methods from here to Factory
     */


    /**
     * Determine if the given Dictionary Version has a map entry in the table
     *
     * @param dict the name (only, not path) of the dictionary file being checked
     * @return true if this mapping table contains a mapping for the specified
     * dictionary, false if not.
     */
    @Override
    public boolean isMapped(final String dict) {
        return dictToMappingMap.containsKey(dict);
    }

    /**
     * Determine if the given FSW Build Id has a map entry in the table
     *
     * @param fswId FSW ID being checked
     * @return true if this mapping table contains a mapping for the specified
     * FSW Build Id, false if not.
     */
    @Override
    public boolean isMapped(final long fswId) {
        return fswIdToDictMap.containsKey(fswId);
    }

    /**
     * @param fswIds1
     * @param fswIds2
     * @return true if provided FSW Build Ids represent the same Flight
     * Dictionary Release Version (ignoring ground revisions), and false
     * if not.
     * @Override Determine if two FSW Build Ids represent the same Flight Dictionary
     * Release.
     * 05/20/16  - MPCS-8131 - Now actually gets FSW release version for each set and
     * compares those. Previous behavior could report false if fswIds1 and fswIds2
     * were mutually exclusive sets, but could map to the same release version.
     */
    @Override
    public boolean isSameFlightDictionary(final Set<Long> fswIds1, final Set<Long> fswIds2) {
        try {
            final SortedSet<Long> sorted1 = new TreeSet<Long>(fswIds1);
            final SortedSet<Long> sorted2 = new TreeSet<Long>(fswIds2);

            final String dict1 = getReleaseVersionId(sorted1);
            final String dict2 = getReleaseVersionId(sorted2);

            return !(UNMATCHED_DICTIONARY_VERSION.equals(dict1) || UNMATCHED_DICTIONARY_VERSION.equals(dict2)) && dict1.equals(dict2);

        } catch (final Exception e) {
            // do nothing
        }
        return false;
    }

    /**
     * Determine if two FSW Build Ids represent the same Flight Dictionary
     * Release.
     *
     * @param fswId1
     * @param fswId2
     * @return true if provided FSW Build Ids represent the same Flight
     * Dictionary Release Version (ignoring ground revisions), and false
     * if not.
     */
    @Override
    public boolean isSameFlightDictionary(final long fswId1, final long fswId2) {
        final String dict1 = getReleaseVersionId(fswId1);
        final String dict2 = getReleaseVersionId(fswId2);
        try {
            return !(UNMATCHED_DICTIONARY_VERSION.equals(dict1) || UNMATCHED_DICTIONARY_VERSION.equals(dict2)) && dict1.equals(dict2);
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * Determines whether the provided Flight Software Build ID and the provided
     * Dictionary Version have the same Flight Software version.
     *
     * @param fswId the Flight Software Build ID
     * @param dict  the Dictionary Version
     * @return true if the dictionary is from the same FSW Build, false if not.
     */
    @Override
    public boolean isSameFlightDictionary(final long fswId, final String dict) {
        return getBuildVersionIds(dict).contains(fswId);
    }

    // MPCS-8131  04/12/16 - removed containsECR110724ProductMetadata and getMpduLengthForFswBuildId methods

    /**
     * Returns the correct Flight Release Dictionary Version ID for the provided
     * Full Dictionary version. This value is truncated and does not contain the
     * Ground Revision.
     *
     * @param dict the Dictionary Version being checked
     * @return the correct full Dictionary Version ID for the provided Flight
     * Released Dictionary version.
     */
    @Override
    public String getReleaseVersionId(final String dict) {
        try {
            return dictToMappingMap.get(dict).last().getFswReleaseVersion();
        } catch (final Exception e) {
            return UNMATCHED_DICTIONARY_VERSION;
        }
    }

    /**
     * Returns the complete Set of all supported FSW Build Ids.
     *
     * @return a Set of supported Software Versions.
     */
    @Override
    public Set<Long> getSupportedBuildVersionIds() {
        return fswIdToDictMap.keySet();
    }

    @Override
    public int getMpduSize(final long fswId) {
        if (fswIdToDictMap.containsKey(fswId)) {
            return fswIdToDictMap.get(fswId).last().getMpduSize();
        } else {
            return -1;
        }
    }

    /**
     * Parses the given XML File to populate the FSW Version Mappings to
     * Dictionary ID definitions. Upon creation of this FSW/Dictionary mapper
     * object, the mappings file specified in the DictionaryConfiguration is
     * utilized.
     *
     * @param filename the path to the XML FSW Version Mappings to Dictionary ID
     *                 definitions file.
     * @throws DictionaryException
     * MPCS-8131 - 05/03/16 - changed to private, is only used
     * during construction
     */
    private void parse(final String filename) throws DictionaryException {
        if (filename == null) {
            throw new DictionaryException("FSW Version Mappings to Dictionary ID path is undefined.");
        }
        this.xmlPath = filename;
        SAXParser sp = null;
        try {
            final File file = new File(this.xmlPath);
            fswIdToDictMap.clear();
            dictToFswIdMap.clear();
            dictToMappingMap.clear();
            sp = SAXParserPool.getInstance().getNonPooledParser();
            sp.parse(file, this);

            // Once we parse, set the sha.
            sha256 = getMapperSHA256(filename);
        } catch (final SAXException e) {
            parserLog.error(e.getMessage());
            throw new DictionaryException(e.getMessage(), e);
        } catch (final ParserConfigurationException e) {
            parserLog.error("Unable to configure sax parser to read FSW Version Mappings to Dictionary IDs file.");
            throw new DictionaryException("Unable to configure sax parser to read FSW Version Mappings to Dictionary IDs.", e);
        } catch (final Exception e) {
            parserLog.error("Unexpected error parsing or reading FSW Version Mappings to Dictionary IDs.");
            throw new DictionaryException("Unexpected error parsing or reading FSW Version Mappings to Dictionary IDs.", e);
        }
    }

    /**
     * Returns the String representing the full dictionary version used when
     * there is not an appropriate dictionary release version
     */
    @Override
    public String getUnmatchedDictionaryVersion() {
        return UNMATCHED_DICTIONARY_VERSION;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
    public void characters(final char[] chars, final int start, final int length) throws SAXException {
        final String newText = new String(chars, start, length);
        if (!newText.equals("\n")) {
            this.text.append(newText);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error(final SAXParseException e) throws SAXException {
        throw new SAXException("Parse error in FSW Version Mappings to Dictionary IDs, line " + e.getLineNumber() + " col " + e.getColumnNumber() + ": " + e.getMessage());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(final SAXParseException e) throws SAXException {
        throw new SAXException("Fatal parse error in FSW Version Mappings to Dictionary IDs, line " + e.getLineNumber() + " col " + e.getColumnNumber() + ": " + e.getMessage());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(final SAXParseException e) {
        parserLog.warn("Parse warning in FSW Version Mappings to Dictionary IDs, line " + e.getLineNumber() + " col " + e.getColumnNumber()
                + ": " + e.getMessage());
    }

    /**
     * @param uri
     * @param localName
     * MPCS-8568  - 12/07/16 - Removed try/catches that weren't used. Change to use mappingBuilder
     */
    @Override
    public void startElement(final String uri, final String localName, final String qname, final Attributes attr) throws SAXException {
        this.text = new StringBuilder(1024);

        if (qname.equalsIgnoreCase("FswToDictionaryMappings")) {
            return;
        }

        if (qname.equalsIgnoreCase("FswToDictionaryMapping")) {
            mappingBuilder = new FswToDictMappingBuilder();
            try {
                this.mappingBuilder.setFswBuildVersion(GDR.parse_long(attr.getValue("FSW_version_id")));
            } catch (final NumberFormatException e) {
                throw new SAXException("FSW Build ID must be a number, not " + attr.getValue("FSW_version_id"));
            }

            this.mappingBuilder.setFswReleaseVersion(attr.getValue("FSW_release_version_id"));

            this.mappingBuilder.setDictionaryVersion(attr.getValue("dictionary_version_id"));

            // MPCS-8568 -  - 12/12/16 - parse dicitionary directory
            this.mappingBuilder.setFswDirectory(attr.getValue("fsw_directory"));

            try {
                this.mappingBuilder.setGroundRevision(GDR.parse_int(attr.getValue("ground_revision")));
            } catch (final NumberFormatException e) {
                throw new SAXException("Dictionary Version ID must be a number, not " + attr.getValue("version"));
            }

            /**
             * MPCS-8276  6/13/2016 - Parsing and setting the MPDU size. This is optional, so if it is
             * not set or is incorrect, just don't do anything.
             */
            final String mpduSizeStr = attr.getValue("mpdu_size");

            if (mpduSizeStr != null && GDR.isIntString(mpduSizeStr)) {
                this.mappingBuilder.setMpduSize(GDR.parse_int(mpduSizeStr));
            }

            this.mappingBuilder.setCustomer(attr.getValue("customer"));
            this.mappingBuilder.setTimeStamp(attr.getValue("timestamp"));
        }
    }

    /**
     * @param uri
     * @param localName
     */
    @Override
    public void endElement(final String uri, final String localName, final String qname) throws SAXException {
        if (qname.equalsIgnoreCase("FswToDictionaryMappingVersion")) {
            this.mappingVersion = this.text.toString().trim();
        } else if (qname.equalsIgnoreCase("schema_version")) {
            this.schemaVersion = this.text.toString().trim();
            final float version = Float.parseFloat(this.schemaVersion);

            if (version < 1.0) {
                throw new SAXException("Unable to read FSW Version Mappings to Dictionary IDs file: schema version mismatch (configured for 2.x, found " + this.schemaVersion + ").");
            }
        }
        //now with the added dimension, keep the dictionary with the highest ground revision for each directory path
        else if (qname.equalsIgnoreCase("FswToDictionaryMapping")) {
            // MPCS-8568  - 12/07/16 - get the mapping. Moved adding map entries to addToMaps function.
            final ReferenceFswToDictMapping currMapping = mappingBuilder.build();
            addToMaps(currMapping);
        } else if (qname.equalsIgnoreCase("FswToDictionaryMappings")) {
            // Do Nothing.
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": Mapping Version: " + mappingVersion + ", Schema Version: " + schemaVersion + ", Source: " + FileUtility.createFilePathLogMessage(this.xmlPath);
    }

    /**
     * Adds an entry to the dictionary mapping tables in the same manner an entry line in the dictionary mapping file is added.
     *
     * @param fswBuildVersion
     * @param fswReleaseVersion
     * @param dictionaryVersion
     * @param groundRevision
     * @param fswDirectory
     * @param customer
     * @param timeStamp
     * @param mpduSize
     */
    @Override
    public void addFswToDictMapping(final Long fswBuildVersion, final String fswReleaseVersion, final String dictionaryVersion, final Integer groundRevision, final String fswDirectory, final String customer, final String timeStamp, final Integer mpduSize) {

        final FswToDictMappingBuilder builder = new FswToDictMappingBuilder();
        if (fswBuildVersion != null) {
            builder.setFswBuildVersion(fswBuildVersion);
        }
        if (fswReleaseVersion != null) {
            builder.setFswReleaseVersion(fswReleaseVersion);
        }
        if (dictionaryVersion != null) {
            builder.setDictionaryVersion(dictionaryVersion);
        }
        if (groundRevision != null) {
            builder.setGroundRevision(groundRevision);
        }
        if (fswDirectory != null) {
            builder.setFswDirectory(fswDirectory);
        }
        if (customer != null) {
            builder.setCustomer(customer);
        }
        if (timeStamp != null) {
            builder.setTimeStamp(timeStamp);
        }
        if (mpduSize != null) {
            builder.setMpduSize(mpduSize);
        }

        addToMaps(builder.build());
    }

    /**
     * Check the maps and add the supplied entry to the mappings as necessary
     *
     * @param newMap
     * MPCS-8568  12/07/16 -  Added. Functionality in endElement was moved to this class and updated.
     */
    private void addToMaps(final ReferenceFswToDictMapping newMap) {
        SortedSet<ReferenceFswToDictMapping> previousMappings = fswIdToDictMap.get(newMap.getFswBuildVersion());
        if (previousMappings == null) {
            previousMappings = new TreeSet<ReferenceFswToDictMapping>();
            previousMappings.add(newMap);
        } else {

            boolean newEntry = true;

            for (final ReferenceFswToDictMapping previousMapping : previousMappings) {
                if (!newEntry) {
                    break;
                }
                if (newMap.getFswDirectory().equals(previousMapping.getFswDirectory())) {
                    if (newMap.getGroundRevision() > previousMapping.getGroundRevision()) {
                        previousMappings.remove(previousMapping);
                        previousMappings.add(newMap);
                    }
                    newEntry = false;
                }
            }
            if (newEntry) {
                previousMappings.add(newMap);
            }
        }
        fswIdToDictMap.put(newMap.getFswBuildVersion(), previousMappings);

        SortedSet<Long> fswIdMappings;
        fswIdMappings = this.dictToFswIdMap.get(newMap.getDictionaryVersion());
        if (null == fswIdMappings) {
            fswIdMappings = new TreeSet<Long>();
            this.dictToFswIdMap.put(newMap.getDictionaryVersion(), fswIdMappings);
        }
        fswIdMappings.add(newMap.getFswBuildVersion());

        fswIdMappings = this.dictToFswIdMap.get(newMap.getFswReleaseVersion());
        if (null == fswIdMappings) {
            fswIdMappings = new TreeSet<Long>();
            this.dictToFswIdMap.put(newMap.getFswReleaseVersion(), fswIdMappings);
        }
        fswIdMappings.add(newMap.getFswBuildVersion());

        SortedSet<ReferenceFswToDictMapping> fswMappings;
        fswMappings = this.dictToMappingMap.get(newMap.getDictionaryVersion());
        if (null == fswMappings) {
            fswMappings = new TreeSet<ReferenceFswToDictMapping>();
            this.dictToMappingMap.put(newMap.getDictionaryVersion(), fswMappings);
        }
        fswMappings.add(newMap);

        fswMappings = this.dictToMappingMap.get(newMap.getFswReleaseVersion());
        if (null == fswMappings) {
            fswMappings = new TreeSet<ReferenceFswToDictMapping>();
            this.dictToMappingMap.put(newMap.getFswReleaseVersion(), fswMappings);
        }
        fswMappings.add(newMap);
    }


    /**
     * Builder for ReferenceFswToDictMapping objects. Holds the mutable values for one.
     * Returns an immutable ReferenceFswToDictMapping object when the build method is
     * called.
     *
     */
    public static class FswToDictMappingBuilder {
        long fswBuildVersion = -1L;
        String fswReleaseVersion = "<UNKNOWN>";
        String dictionaryVersion = UNMATCHED_DICTIONARY_VERSION;
        int groundRevision = 1;
        String fswDirectory = "<UNKNOWN>";
        String customer = "<UNKNOWN>";
        String timeStamp = "0000000T00:00:00";
        int mpduSize = -1;

        /**
         * @param fswBuildVersion
         * @return this FswToDictMappingBuilder
         */
        public FswToDictMappingBuilder setFswBuildVersion(final long fswBuildVersion) {
            this.fswBuildVersion = fswBuildVersion;

            return this;
        }

        /**
         * @param fswReleaseVersion
         * @return this FswToDictMappingBuilder
         */
        public FswToDictMappingBuilder setFswReleaseVersion(final String fswReleaseVersion) {
            this.fswReleaseVersion = fswReleaseVersion;

            return this;
        }

        /**
         * @param dictionaryVersion
         * @return this FswToDictMappingBuilder
         */
        public FswToDictMappingBuilder setDictionaryVersion(final String dictionaryVersion) {
            this.dictionaryVersion = dictionaryVersion;

            return this;
        }

        /**
         * @param groundRevision
         * @return this FswToDictMappingBuilder
         */
        public FswToDictMappingBuilder setGroundRevision(final int groundRevision) {
            this.groundRevision = groundRevision;

            return this;
        }

        /**
         * @param fswDirectory
         * @return this FswToDictMappingBuilder
         */
        public FswToDictMappingBuilder setFswDirectory(final String fswDirectory) {
            this.fswDirectory = fswDirectory;

            return this;
        }

        /**
         * @param customer
         * @return this FswToDictMappingBuilder
         */
        public FswToDictMappingBuilder setCustomer(final String customer) {
            this.customer = customer;

            return this;
        }

        /**
         * @param timeStamp
         * @return this FswToDictMappingBuilder
         */
        public FswToDictMappingBuilder setTimeStamp(final String timeStamp) {
            this.timeStamp = timeStamp;

            return this;
        }

        /**
         * @param mpduSize
         * @return this FswToDictMappingBuilder
         */
        public FswToDictMappingBuilder setMpduSize(final int mpduSize) {
            this.mpduSize = mpduSize;

            return this;
        }

        /**
         * @return this FswToDictMappingBuilder
         */
        public ReferenceFswToDictMapping build() {
            return new ReferenceFswToDictMapping(fswBuildVersion, fswReleaseVersion, dictionaryVersion, groundRevision, fswDirectory, customer, timeStamp, mpduSize);
        }

    }
}