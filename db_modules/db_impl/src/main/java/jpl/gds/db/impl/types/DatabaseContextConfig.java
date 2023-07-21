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

package jpl.gds.db.impl.types;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.db.api.types.IDbContextConfigUpdater;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;

/**
 * Holder object for database Context Config rows
 */
public class DatabaseContextConfig extends AbstractDatabaseItem implements IDbContextConfigUpdater {
    private static final String CSV_COL_HDR = DQ + "ContextConfig";
    private static final int BUILDER_CAPACITY = 1024;

    //field names in CSV
    private static final String FIELD_CONTEXT_ID = "CONTEXTID";
    private static final String FIELD_NAME = "NAME";
    private static final String FIELD_TYPE = "TYPE";
    private static final String FIELD_USER = "USER";
    private static final String FIELD_MPCS_VERSION = "MPCSVERSION";
    private static final String FIELD_SESSION_ID = "SESSIONID";
    private static final String FIELD_HOST = "CONTEXTHOST";

    private static final String META_OPEN = "[";
    private static final String META_CLOSE = "]";
    private static final String META_KEYS = "METADATAKEYWORDLIST";
    private static final String META_VALUES = "METADATAVALUESLIST";

    //key-value map; keep insertion order
    Map<String, String> map = new LinkedHashMap<>();

    //session ID, context ID, host name, host ID come from parent class

    private String user;
    private String type;
    private String name;
    private String mpcsVersion;
    private long parentId;

    public DatabaseContextConfig(final ApplicationContext appContext) {
        super(appContext);

        this.user = null;
        this.type = null;
        this.mpcsVersion = null;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue(final String key) {
        return map.get(key);
    }

    @Override
    public String getMpcsVersion() {
        return mpcsVersion;
    }

    @Override
    public long getParentId() {
        return parentId;
    }

    @Override
    public Map<String, String> getFileData(final String noData) {
        return null;
    }

    @Override
    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public void setName(final String name){
        this.name = name;
    }

    @Override
    public void setUser(final String user) {
        this.user = user;
    }

    @Override
    public void setValue(final String key, final String val) {
        map.put(key, val);
    }

    @Override
    public void setMpcsVersion(final String mpcsVersion) {
        this.mpcsVersion = mpcsVersion;
    }

    @Override
    public void setParentId(final long parentId){
        this.parentId = parentId;
    }


    @Override
    public String toCsv(final List<String> csvColumns) {
        final StringBuilder csv = new StringBuilder(BUILDER_CAPACITY);
        final DateFormat df  = TimeUtility.getFormatterFromPool();

        csv.append(CSV_COL_HDR);

        for (final String cce : csvColumns) {
            final String upcce = cce.toUpperCase();

            csv.append(CSV_COL_SEP);

            switch (upcce) {
                case FIELD_CONTEXT_ID:
                    csv.append(contextId);
                    break;

                case FIELD_NAME:
                    if (name != null) {
                        csv.append(name);
                    }
                    break;

                case FIELD_TYPE:
                    if (type != null) {
                        csv.append(type);
                    }
                    break;

                case FIELD_USER:
                    if (user != null) {
                        csv.append(user);
                    }
                    break;

                case FIELD_HOST:
                    if (sessionHost != null)
                    {
                        csv.append(sessionHost);
                    }
                    break;

                case FIELD_MPCS_VERSION:
                    if (mpcsVersion != null) {
                        csv.append(mpcsVersion);
                    }
                    break;

                case FIELD_SESSION_ID:
                    if (sessionId != null) {
                        csv.append(sessionId);
                    }
                    break;
                case META_KEYS:
                    csv.append(META_OPEN + String.join(COMMA, map.keySet()) + META_CLOSE);
                    break;
                case META_VALUES:
                    csv.append(META_OPEN + String.join(COMMA, map.values()) + META_CLOSE);
                    break;
                default:
                    break;
            }
        }

        csv.append(CSV_COL_TRL);
        TimeUtility.releaseFormatterToPool(df);

        return csv.toString();
    }

    @Override
    public void parseCsv(final String csvStr, final List<String> csvColumns) {
        // The following removes the start/end quotes w/ the substring
        // and splits based on ",". It leaves the trailing empty string in the case that
        // csvStr ends with "". The empty strings serve as place holders.

        final String[] dataArray = csvStr.substring(1, csvStr.length() - 1).split(CSV_COL_SEP, -1);

        if ((csvColumns.size() + 1) != dataArray.length) {
            throw new IllegalArgumentException("CSV column length mismatch, received " + dataArray.length +
                                                       " but expected at least " + (csvColumns.size() + 1));
        }

        // Clear everything we might process, in case empty column or not in list

        contextId               = 0L;
        type                    = null;
        user                    = null;
        mpcsVersion             = null;

        int    next  = 1; // Skip recordType
        String token = null;

        for (final String cce : csvColumns) {
            token = dataArray[next].trim();

            ++next;

            if (token.isEmpty()) {
                continue;
            }

            final String upcce = cce.toUpperCase();

            try {
                switch (upcce) {
                    case FIELD_CONTEXT_ID:
                        contextId = Long.valueOf(token);
                        break;

                    case FIELD_NAME:
                        name = token;
                        break;

                    case FIELD_TYPE:
                        type = token;
                        break;

                    case FIELD_USER:
                        user = token;
                        break;

                    case FIELD_HOST:
                        sessionHost = token;
                        break;

                    case FIELD_MPCS_VERSION:
                        mpcsVersion = token;
                        break;

                    default:
                        break;
                }
            }
            catch (final RuntimeException re) {
                re.printStackTrace();
                throw re;
            }
            catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setIntoContextIdentification(final IContextIdentification si) {
        si.setNumber(getContextId());
        si.setType(getType());
        si.setUser(getUser());
        si.setName(getName());
        si.setHost(getSessionHost());
        si.setHostId(getSessionHostId());
        si.setSpacecraftId(Integer.parseInt(getValue(MetadataKey.SPACECRAFT_ID.name())));
        si.setStartTime(parseTime(getValue(MetadataKey.CREATE_TIME.name())));
        si.setEndTime(parseTime(getValue(MetadataKey.END_TIME.name())));
    }

    @Override
    public void setIntoContextConfiguration(final IContextConfiguration cc) {
        setIntoContextIdentification(cc.getContextId());
        cc.getGeneralInfo().setOutputDir(getValue(MetadataKey.APPLICATION_OUTPUT_DIRECTORY.name()));
        setIntoDictionaryConfiguration(cc.getDictionaryConfig());
        //TODO other fields
    }

    @Override
    public void setIntoDictionaryConfiguration(final DictionaryProperties dc) {
        dc.setFswVersion(getValue(MetadataKey.FSW_DICTIONARY_VERSION.name()));
        dc.setFswDictionaryDir(MetadataKey.FSW_DICTIONARY_DIR.name());
        dc.setSseVersion(MetadataKey.SSE_DICTIONARY_VERSION.name());
        dc.setSseDictionaryDir(MetadataKey.SSE_DICTIONARY_DIR.name());
    }

    @Override
    public void setTemplateContext(final Map<String, Object> templateMap) {
        super.setTemplateContext(templateMap);

        put(templateMap, "mpcsVersion",            mpcsVersion);
        put(templateMap, "contextName",            name);
        put(templateMap, "contextType",            type);
        put(templateMap, "user",                   user);
        //metadata
        putMap(templateMap, "metadata",            map);
    }

    private IAccurateDateTime parseTime(final String str){
        IAccurateDateTime time;
        try{
            time = new AccurateDateTime(str);
        }
        catch (final ParseException e){
            log.error("Error parsing time: ", str);
            return null;
        }
        return time;
    }

    private static void putMap(final Map<String, Object> map, final String key, final Map<String,String> value){
        if (value != null) {
            map.put(key, value);
        }
    }
}
