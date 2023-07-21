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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code ICommandTranslationTableMetadataProvider} defines the API for querying a CTS command translation table's
 * metadata.
 *
 * This is an AMPCS-facing interface.
 *
 * @since 8.2.0
 */
public interface ICommandTranslationTableMetadataProvider {

    /**
     * Get the FSW version string from the CTS translation table. E.g. FSW_version_id="127774343" in MSL dictionary.
     *
     * @return a FSW version string or null if undefined
     */
    String getFswVersion();

    /**
     * Get the dictionary version string from the CTS translation table. E.g.
     * dictionary_version_id="R9_3_8_20120118_01" in MSL dictionary.
     *
     * @return a dictionary version string or null if undefined
     */
    String getDictionaryVersion();

    /**
     * Get the map of File Load types from the CTS translation table. Entry's key is the type's name and value is the
     * number for the type. E.g.
     *
     * "generic" : 0
     * "sequence" : 1
     * "load_and_go" : 2
     *
     * in SMAP dictionary.
     *
     * Warning: Do not modify the map.
     *
     * @return map of File Load type names mapped to their numeric values, empty if undefined
     */
    Map<String, Integer> getFileLoadTypeMap();

    /**
     * Get the list of spacecraft IDs from the CTS translation table. E.g. 76, 158 in MSL dictionary.
     *
     * Warning: Do not modify the list.
     *
     * @return list of spacecraft IDs, empty if undefined
     */
    List<Integer> getSpacecraftIds();

    /**
     * Get the set of command stem names from the CTS translation table. E.g. "ACM_CHANGE_CONDITION",
     * "ACM_CLEAR_ERROR" in MSL command dictionary.
     *
     * @return set of command stem names, empty if undefined
     */
    Set<String> getStemNames();

    /**
     * Get the command stem's title from the CTS translation table. E.g. "forcibly changes the argument specified
     * condition to the argument specified boolean state of true or false" for MSL dictionary's
     * ACM_CHANGE_CONDITION stem. Stem name is required for lookup.
     *
     * @param stemName command stem's name
     * @return command stem's title, null if undefined
     */
    String getStemTitle(String stemName);

    /**
     * Get the command stem's opcode from the CTS translation table. E.g. 30309 for MSL dictionary's
     * ACM_CHANGE_CONDITION stem. Stem name is required for lookup.
     *
     * @param stemName command stem's name
     * @return command stem's opcode or -1 if undefined
     */
    int getStemOpCode(String stemName);

    /**
     * Get the command stem's module name from the CTS translation table. E.g. "ACM" for MSL dictionary's
     * ACM_CHANGE_CONDITION stem. Stem name is required for lookup.
     *
     * @param stemName command stem's name
     * @return command stem's module name or null if undefined
     */
    String getStemModuleName(String stemName);

    /**
     * Get the command stem's category number(s) if any from the CTS translation table. E.g. 16 for MSL dictionary's
     * ACM_CHANGE_CONDITION stem. Stem name is required for lookup.
     *
     * Note: CTS library keeps category number(s) as a list.
     *
     * Warning: Do not modify the list.
     *
     * @param stemName command stem's name
     * @return command stem's category number(s) or empty if undefined
     */
    List<Integer> getStemCategoryNumbers(String stemName);

    /**
     * Get the command stem's class number(s) if any from the CTS translation table. E.g. 1 for MSL dictionary's
     * ACM_CHANGE_CONDITION stem. Stem name is required for lookup.
     *
     * Note: CTS library keeps class number(s) as a list.
     *
     * Warning: Do not modify the list.
     *
     * @param stemName command stem's name
     * @return command stem's class number(s) or empty if undefined
     */
    List<Integer> getStemClassNumbers(String stemName);

    /**
     * Get the number of arguments in the command stem from the CTS translation table. Stem name is required for lookup.
     *
     * @param stemName command stem's name
     * @return number of arguments in the command stem
     */
    int getStemArgumentsCount(String stemName);

    /**
     * Get the name of the command stem argument from the CTS translation table. E.g. "CONDITION" for MSL dictionary's
     * ACM_CHANGE_CONDITION stem, first argument. Stem name and argument index (from 0) are required.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return name of the argument, null if undefined
     */
    String getStemArgumentName(String stemName, int argumentIndex);

    /**
     * Get the title of the command stem argument from the CTS translation table. E.g. "CONDITION" for MSL dictionary's
     * ACM_CHANGE_CONDITION stem, first argument. Stem name and argument index (from 0) are required.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return title of the argument, null if undefined
     */
    String getStemArgumentTitle(String stemName, int argumentIndex);

    /**
     * Get the command stem argument's type number from the CTS translation table. This number is specific to CTS's
     * implementation. Use {@code jpl.gds.tc.mps.impl.ctt.ICommandTranslationTableMetadataProvider#getArgumentTypeMap
     * ()} to look up what the corresponding type is. Stem name and argument index (from 0) are required.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return command stem argument's type number
     */
    int getStemArgumentTypeNumber(String stemName, int argumentIndex);

    /**
     * Get the command stem argument's type from the CTS translation table. This name is specific to CTS's
     * implementation.
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return command stem argument's name
     */
    String getStemArgumentType(String stemName, int argumentIndex);


    /**
     * Get the command stem argument type map from the CTS translation table. Entry's key is CTS-defined index of the
     * argument type, and the value is the actual argument type name.
     *
     * Warning: Do not modify the map.
     *
     * @return map of CTS-defined argument type numbers and corresponding type names
     */
    Map<Integer, String> getArgumentTypeMap();

    /**
     * Get the command stem argument's entry length, in units of 16-bit words, from the CTS translation table. Stem
     * name and argument index (from 0) are required. This is a piece of translation table metadata that may be
     * useful in debugging and not needed by most users.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return command stem argument's entry length in units of 16-bit words
     */
    int getStemArgumentEntryLength(String stemName, int argumentIndex);

    /**
     * Get the command stem argument's result length, in units of bits, from the CTS translation table. Stem name and
     * argument index (from 0) are required. This is a piece of translation table metadata that may be useful in
     * debugging and not needed by most users.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return command stem argument's result length in units of bits
     */
    int getStemArgumentResultLenth(String stemName, int argumentIndex);

    /**
     * Get the command stem argument's format name from the CTS translation table. E.g. "ENUM" for MSL dictionary's
     * ACM_CHANGE_CONDITION stem, first argument. Stem name and argument index (from 0) are required.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return name of the argument format
     */
    String getStemArgumentFormat(String stemName, int argumentIndex);

    /**
     * Check if the command stem argument has a default value from the CTS translation table. Stem name and argument
     * index (from 0) are required.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return true if the argument has a default value defined, false otherwise
     */
    boolean stemArgumentHasDefaultValue(String stemName, int argumentIndex);

    /**
     * Get the command stem argument's default value from the CTS translation table. Stem name and argument index
     * (from 0) are required.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return default value for the command stem argument, null if undefined
     */
    String getStemArgumentDefaultValue(String stemName, int argumentIndex);

    /**
     * Get the command stem argument's range from the CTS translation table. Format is "minvalue...minvalue", e.g.
     * "0X00...0X64" and "0...86400". Stem name and argument index (from 0) are required.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return range string of the command stem argument, null if undefined
     */
    String getStemArgumentRange(String stemName, int argumentIndex);

    /**
     * Get the name of the external conversion (translation) routine (known as a "user hook") for the command stem
     * argument, from the CTS translation table. E.g. "IEEE64FLOAT". Stem name and argument index (from 0) are required.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return name of the external conversion routine for the command stem argument, null if undefined
     */
    String getStemArgumentConversionRoutineName(String stemName, int argumentIndex);

    /**
     * Get the minimum number of repetitions needed for the command stem argument repeat group, from the CTS
     * translation table. Stem name and argument index (from 0) are required.
     *
     * Following is taken from the CTS Javadoc:
     *
     * ... returns the minimum number of repetitions for the set of sub-arguments to follow this repeat start marker
     * argument (this set of sub-arguments is known as a repeat group); -1 is returned if this argument is not a
     * repeat group start marker (CTT_AT_REPEAT) argument. The set of sub-arguments to follow may include nested
     * repeat argument groups. This is allowed, but has never been used.
     *
     * Example: consider a repeat group consisting of a quoted string CTT_AT_QUOTED_STRING) sub-argument, followed by
     * a decimal (CTT_AT_DECIMAL) sub-argument, followed by a unsigned number (CTT_AT_UNSIGNED_NUMBER) sub-argument.
     * Let us say that this group has a minimum repetition of 1 and a maximum repetition of 3. On the mnemonic
     * command line, the following would then be allowed:
     *
     * ,("Quoted String 1",32,0xff,"Quoted String 2",14,0xef,"Quoted String 3",26,0x4e),
     * ,("Another String 1",66,0xea33,"Another String 2",54,0xffd),
     * ,("Yet Another String",145,0x03),
     *
     * In the above three cases, everything between the parenthesis is a single repeat argument. Case 1 has three
     * repetitions, case 2 has two and case 3 has one. Zero is possible if the argument is so defined: this would
     * appear as ",()," on the command line.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return minimum number of repetitions for the command stem argument, -1 if argument is not a repeat group
     * start marker
     */
    int getStemArgumentMinRepeat(String stemName, int argumentIndex);

    /**
     * Get the maximum number of repetitions allowed for the command stem argument repeat group, from the CTS
     * translation table. Stem name and argument index (from 0) are required.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return maximum number of repetitions for the command stem argument, -1 if argument is not a repeat group
     * start marker
     */
    int getStemArgumentMaxRepeat(String stemName, int argumentIndex);

    /**
     * Get the command stem argument's enumeration keys as a list from CTS translation table. E.g. "ON", "OFF". Stem
     * name and argument index (from 0) are required.
     *
     * Warning: Do not modify the list.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return list of enumeration keys for the command stem argument, empty list if undefined
     */
    List<String> getStemArgumentEnumKeys(String stemName, int argumentIndex);

    /**
     * Get the default enumeration key for command stem argument from the CTS translation table. Only valid if the
     * argument type is CTT_AT_ENUM and {@code jpl.gds.tc.mps.impl.ctt.ICommandTranslationTableMetadataProvider
     * #stemArgumentHasDefaultValue(java.lang.String, int)} is {@code true}. Stem name and argument index (from 0) are
     * required.
     *
     * @param stemName command stem's name
     * @param argumentIndex argument index (starting at 0)
     * @return default enumeration key for the command stem argument, null if undefined
     */
    String getStemArgumentDefaultEnumKey(String stemName, int argumentIndex);

}
