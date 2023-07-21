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
package jpl.gds.dictionary.api.command;

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.util.BinOctHexUtility;

import java.util.*;

/**
 * CommandEnumerationDefinition is used to define a named lookup table that maps
 * keeps a map of bit (numeric) values, dictionary names, and FSW names for
 * command argument enumerations. This map is populated with ICommandEnumerationValue
 * objects.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * CommandEnumerationDefinition is used
 * in the command dictionary implementation where a map of bit value to FSW name
 * or bit value to dictionary name is required. For instance, enumerated command
 * arguments may be entered as numeric (bit) values, or as strings (dictionary
 * or FSW name) by the user. Each CommandEnumerationDefinition also has a name,
 * which should be unique within a dictionary. This allows the re-use of
 * enumerations within the dictionaries, so care should be taken to assign a
 * unique name to each separate enumeration constructed by a given dictionary
 * parser.
 * <p>
 * 
 *
 */
public class CommandEnumerationDefinition {

    /**
     * The name of the enumeration.
     */
    private String name; 

    /**
     * Description of the enumeration (optional)
     */
    private String description;

    /** The map of enum values for this enumeration */
    private final Map<String, ICommandEnumerationValue> enumValuesByDict  = new HashMap<>();
    private final Map<String, ICommandEnumerationValue> enumValuesByFsw   = new HashMap<>();
    private final Map<String, ICommandEnumerationValue> enumValuesByBit   = new HashMap<>();
    private final List<ICommandEnumerationValue>        enumerationValues = new ArrayList<>();

    /**
     * Creates an instance of CommandEnumerationDefinition with the given name.
     * 
     * @param enumName the name of the enumeration; should be non-null and unique within
     * a dictionary
     */
    public CommandEnumerationDefinition(final String enumName) {
        name = enumName;
    }

    /**
     * Adds an ICommandEnumerationValue to the enumeration.
     * 
     * @param value the ICommandEnumerationValue to add
     */
    public synchronized void addEnumerationValue(ICommandEnumerationValue value) {
        /*
         * Added null check to emulate previous
         * behavior for unit tests.
         */
        if (value == null) {
            throw new IllegalArgumentException("enum value cannot be null");
        }
        String dictValue = value.getDictionaryValue().toUpperCase();
        if (!this.enumValuesByDict.containsKey(dictValue)) {
            this.enumValuesByDict.put(dictValue, value);
        } else {
            // It's already in the groupings
            return;
        }

        if (value.getFswValue() != null) {
            this.enumValuesByFsw.put(value.getFswValue().toUpperCase(), value);
        }

        this.enumValuesByBit.put(value.getBitValue(), value);

        this.enumerationValues.add(value);

    }

    /**
     * Gets the name of this enumeration.
     * 
     * @return Returns the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this enumeration.
     *
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the optional description of this enumeration.
     * 
     * @return returns the description; may be null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the optional description of this enumeration.
     *
     * @param description the description to set
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Gets a non-modifiable list of the ICommandEnumerationValue objects
     * in this enumeration.
     * 
     * @return List of ICommandEnumerationValues
     */
    public synchronized List<ICommandEnumerationValue> getEnumerationValues() {
        return Collections.unmodifiableList(enumerationValues);
    }

    /**
     * Add a list of enumeration values to the existing list. Will not
     * add any object that is already on the list.
     * 
     * @param values the list of values to add
     */
    public synchronized void addEnumerationValues(List<ICommandEnumerationValue> values)
    {
        for (ICommandEnumerationValue value : values) {
            String dictValue = value.getDictionaryValue().toUpperCase();
            if (!this.enumValuesByDict.containsKey(dictValue)) {
                this.enumValuesByDict.put(dictValue, value);
            } else {
                // It's already in the groupins
                continue;
            }

            if (value.getFswValue() != null) {
                this.enumValuesByFsw.put(value.getFswValue().toUpperCase(), value);
            }

            this.enumValuesByBit.put(value.getBitValue(), value);

            this.enumerationValues.add(value);
        }
    }

    /**
     * Clears all enumeration values.
     */
    public synchronized void clearAllValues() {
        this.enumValuesByDict.clear();
        this.enumValuesByFsw.clear();
        this.enumValuesByBit.clear();
        this.enumerationValues.clear();
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public synchronized String toString() {
        StringBuilder text = new StringBuilder();
        for (ICommandEnumerationValue val: this.enumerationValues) {
            String aLine = val.getBitValue() + "\t" + val.getFswValue() + "\t" + val.getDictionaryValue();
            text.append(aLine + "\n");
        }
        return text.toString();

    }

    /**
     * Given a dictionary value, get the corresponding enumeration value object.
     * 
     * @param value
     *            The dictionary value of the enum value to get
     * 
     * @return The enumeration value object corresponding to the input value or
     *         null if it can't be found
     */
    public synchronized ICommandEnumerationValue lookupByDictionaryValue(final String value) {
        return this.enumValuesByDict.get(value.toUpperCase());
    }

    /**
     * Given an FSW value, get the corresponding enumeration value object.
     * 
     * @param value
     *            The FSW value of the enum value to get
     * 
     * @return The enumeration value object corresponding to the input value or
     *         null if it can't be found
     */
    public synchronized ICommandEnumerationValue lookupByFswValue(final String value) {
        return value == null ? null : this.enumValuesByFsw.get(value.toUpperCase());
    }

    /**
     * Given a bit value, get the corresponding enumeration value object.
     * The bit value must be parsable as a long integer or this method will
     * return null.
     * 
     * @param value
     *            The bit value of the enum value to get
     * 
     * @return The enumeration value object corresponding to the input bit value
     *         or null if it can't be found
     */
    public synchronized ICommandEnumerationValue lookupByBitValue(final String value)
    {
            return this.enumValuesByBit.get(value);
    }

    /**
     * Gets the enumeration value object with the minimum bit value.
     * 
     * @return ICommandEnumerationValue object with the lowest bit value
     *         (numerically) or null if no enumeration values have been added
     */
    public synchronized ICommandEnumerationValue getMinimumValue()
    {
        if(this.enumValuesByBit.isEmpty()) {
            return null;
        }

        List<String> strKeys = new ArrayList<>(this.enumValuesByBit.keySet());
        Map<Long, String> translatedKeys = bitToLongKeys(strKeys);

        List<Long> keys = new ArrayList<>(translatedKeys.keySet());
        keys.sort(null);

        String minKey = translatedKeys.get(keys.get(0));

        return this.enumValuesByBit.get(minKey);
    }

    /**
     * Gets the enumeration value object with the maximum bit value.
     * 
     * @return ICommandEnumerationValue object with the highest bit value
     *         (numerically) or null if no enumeration values have been added
     */
    public synchronized ICommandEnumerationValue getMaximumValue()
    {
        if (this.enumValuesByBit.isEmpty()) {
            return null;
        }

        List<String> strKeys = new ArrayList<>(this.enumValuesByBit.keySet());
        Map<Long, String> translatedKeys = bitToLongKeys(strKeys);

        List<Long> keys = new ArrayList<>(translatedKeys.keySet());
        keys.sort(null);
        Collections.reverse(keys);

        String maxKey = translatedKeys.get(keys.get(0));

        return this.enumValuesByBit.get(maxKey);
    }

    private Map<Long, String> bitToLongKeys(List<String> keys) {
        Map<Long, String> retMap = new TreeMap<>();

        String tmpKey;
        for(String key : keys) {
            tmpKey = key;
            if(tmpKey.matches("[-]?[0-9]+")) {
                retMap.put(GDR.parse_long(tmpKey), key);
            } else if(BinOctHexUtility.isValidHex(tmpKey)) {
                if(!BinOctHexUtility.hasHexPrefix(key)) {
                    tmpKey = BinOctHexUtility.HEX_STRING_PREFIX1 + tmpKey;
                }
                retMap.put(GDR.parse_long(tmpKey), key);
            } else {
                if(!BinOctHexUtility.hasBinaryPrefix(key)) {
                    tmpKey = BinOctHexUtility.BINARY_STRING_PREFIX1 + tmpKey;
                }
                retMap.put(GDR.parse_long(tmpKey), key);
            }
        }

        return retMap;
    }
}
