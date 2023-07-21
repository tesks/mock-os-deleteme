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
package jpl.gds.dictionary.impl.client.cache;

import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;

/**
 * The dictionary id object to for a dictionary. Note, this does not care if
 * this is an FSW or SSE dictionary.
 * 
 *
 */
public class DictionaryId {
    private final String         version;
    private final String         dictDir;
    private final DictionaryType type;
    private final boolean        isSse;
    private final String         filePath;

    /**
     * @param filePath
     * @param version
     * @param dictDir
     * @param type
     * @param isSse
     */
    public DictionaryId(String filePath, String version, String dictDir, DictionaryType type, boolean isSse) {
        this.version = version;
        this.dictDir = dictDir;
        this.type = type;
        this.isSse = isSse;
        this.filePath = filePath;
    }

    /**
     * Gets the dictionary version and directory from dictConfig using isSse to
     * determine which version and directory to use, sse or fsw.
     * 
     * @param filePath
     * @param dictConfig
     * @param type
     * @param isSse
     */
    public DictionaryId(String filePath, DictionaryProperties dictConfig, DictionaryType type, boolean isSse) {
        this(filePath, isSse ? dictConfig.getSseVersion() : dictConfig.getFswVersion(),
                isSse ? dictConfig.getSseDictionaryDir() : dictConfig.getFswDictionaryDir(), type, isSse);
    }

    /**
     * @return the filePath
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return the dictDir
     */
    public String getDictDir() {
        return dictDir;
    }

    /**
     * @return the type
     */
    public DictionaryType getType() {
        return type;
    }

    /**
     * @return the isSse
     */
    public boolean isSse() {
        return isSse;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dictDir == null) ? 0 : dictDir.hashCode());
        result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
        result = prime * result + (isSse ? 1231 : 1237);
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DictionaryId other = (DictionaryId) obj;
        if (dictDir == null) {
            if (other.dictDir != null)
                return false;
        } else if (!dictDir.equals(other.dictDir))
            return false;
        if (filePath == null) {
            if (other.filePath != null)
                return false;
        } else if (!filePath.equals(other.filePath))
            return false;
        if (isSse != other.isSse)
            return false;
        if (type != other.type)
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }
}
