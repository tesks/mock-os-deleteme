/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.tc.mps.impl.ctt;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code InternalMpsCommandArgument} is a package-private data structure that represents information about a
 * command stem's argument according to CTS.
 *
 * @since 8.2.0
 */
class InternalMpsCommandArgumentMetadata {

    /*
    Note: Because this is a pure get/set POJO, no unit test needed.
     */

    private int typeNum;
    private int entryLength;
    private int resultLenth;
    private String name;
    private String title;
    private String format;
    private boolean defaultValueExists;
    private String defaultValue;
    private String range;
    private String conversionRoutineName;
    private int minRepeat;
    private int maxRepeat;
    private List<String> enumKeys = new ArrayList<>();
    private String defaultEnumKey;

    int getTypeNum() {
        return this.typeNum;
    }

    InternalMpsCommandArgumentMetadata setTypeNum(final int typeNum) {
        this.typeNum = typeNum;
        return this;
    }

    int getEntryLength() {
        return this.entryLength;
    }

    InternalMpsCommandArgumentMetadata setEntryLength(final int entryLength) {
        this.entryLength = entryLength;
        return this;
    }

    int getResultLenth() {
        return this.resultLenth;
    }

    InternalMpsCommandArgumentMetadata setResultLenth(final int resultLenth) {
        this.resultLenth = resultLenth;
        return this;
    }

    String getName() {
        return this.name;
    }

    InternalMpsCommandArgumentMetadata setName(final String name) {
        this.name = name;
        return this;
    }

    String getTitle() {
        return this.title;
    }

    InternalMpsCommandArgumentMetadata setTitle(final String title) {
        this.title = title;
        return this;
    }

    String getFormat() {
        return this.format;
    }

    InternalMpsCommandArgumentMetadata setFormat(final String format) {
        this.format = format;
        return this;
    }

    boolean hasDefaultValue() {
        return this.defaultValueExists;
    }

    InternalMpsCommandArgumentMetadata setDefaultValueExistsFlag(final boolean flag) {
        this.defaultValueExists = flag;
        return this;
    }

    String getDefaultValue() {
        return this.defaultValue;
    }

    InternalMpsCommandArgumentMetadata setDefaultValue(final String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    String getRange() {
        return this.range;
    }

    InternalMpsCommandArgumentMetadata setRange(final String range) {
        this.range = range;
        return this;
    }

    String getConversionRoutineName() {
        return this.conversionRoutineName;
    }

    InternalMpsCommandArgumentMetadata setConversionRoutineName(final String conversionRoutineName) {
        this.conversionRoutineName = conversionRoutineName;
        return this;
    }

    int getMinRepeat() {
        return this.minRepeat;
    }

    InternalMpsCommandArgumentMetadata setMinRepeat(final int minRepeat) {
        this.minRepeat = minRepeat;
        return this;
    }

    int getMaxRepeat() {
        return this.maxRepeat;
    }

    InternalMpsCommandArgumentMetadata setMaxRepeat(final int maxRepeat) {
        this.maxRepeat = maxRepeat;
        return this;
    }

    List<String> getEnumKeys() {
        return this.enumKeys;
    }

    InternalMpsCommandArgumentMetadata addEnumKey(final String enumKey) {
        this.enumKeys.add(enumKey);
        return this;
    }

    String getDefaultEnumKey() {
        return this.defaultEnumKey;
    }

    InternalMpsCommandArgumentMetadata setDefaultEnumKey(final String defaultEnumKey) {
        this.defaultEnumKey = defaultEnumKey;
        return this;
    }

}
