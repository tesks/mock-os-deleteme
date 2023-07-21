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
package jpl.gds.cfdp.common.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.types.UnsignedInteger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CFDP Common properties object
 *
 * @since R8.1
 * 
 * @version MPCS-10532 - 04/18/19 - Updated to add service classes property, changed getMnemonics to List
 */
public class CfdpCommonProperties extends GdsHierarchicalProperties {
    /**
     * CFDP Common properties file
     */
    protected static final String PROPERTY_FILE = "cfdp_common.properties";

    private static final String PROPERTY_PREFIX = "cfdpCommon.";
    private static final String MNEMONIC_BLOCK = PROPERTY_PREFIX + "mnemonic.";

    private static final String ENTITY_ID_SUFFIX = ".entity.id";
    
    private static final String SERVICE_CLASSES = PROPERTY_PREFIX + "serviceClasses";
    
    private final Map<String, Long> mnemonicToEntityIdMap;
    
    private final List<UnsignedInteger> serviceClasses;

    /**
     * CFDP Common properties constructor
     */
    public CfdpCommonProperties() {
        super(PROPERTY_FILE, true);

        final Pattern pattern = Pattern.compile(MNEMONIC_BLOCK.replaceAll("[.]", "[.]") + "([^.]+)" + ENTITY_ID_SUFFIX.replaceAll("[.]", "[.]"));

        mnemonicToEntityIdMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (final String propertyKey : properties.stringPropertyNames()) {
            final Matcher matcher = pattern.matcher(propertyKey);

            if (matcher.matches()) {
                final String mnemonic = matcher.group(1);
                final String entityIdStr = properties.getProperty(MNEMONIC_BLOCK + mnemonic + ENTITY_ID_SUFFIX);
                mnemonicToEntityIdMap.put(mnemonic, Long.parseUnsignedLong(entityIdStr));
            }

        }
        
        serviceClasses = new ArrayList<>();
        final List<Integer> scTmp = this.getIntListProperty(SERVICE_CLASSES, null, ",");
        
        if(scTmp != null && !scTmp.isEmpty()) {
        	for(final Integer sc : scTmp) {
        		serviceClasses.add(UnsignedInteger.valueOfIntegerAsUnsigned(sc));
        	}
        }

    }

    /**
     * Get entity ID mapped to mnemonic.
     *
     * @param mnemonic The CFDP entity ID's mnemonic string
     * @return null if mnemonic is not mapped to any entity ID, the entity ID value if mapping exists
     */
    public Long getEntityId(final String mnemonic) {
        return mnemonicToEntityIdMap.get(mnemonic);
    }
    
    /**
     * Translates the String supplied to the corresponding entity ID.<br>
     * If the supplied string does correspond to a mnemonic, then the corresponding numeric Entity ID is returned<br>
     * If the supplied string is not a valid mnemonic, but is a numeric value, then the numeric value is returned<br>
     * If the string is not a valid mnemonic and is not a numeric value, an exception will be thrown<br>
     * @param mnemonicOrEntityId the String name of a mnemonic or a numeric value in String format
     * @return the Long Entity ID of the supplied value
     */
    public Long translatePossibleMnemonic(final String mnemonicOrEntityId) {
        long entityId = -1;
        final Long mappedId = getEntityId(mnemonicOrEntityId);

        if (mappedId == null) {
            // No mapping found
        	entityId = Long.parseUnsignedLong(mnemonicOrEntityId);

        } else {
            entityId = mappedId.longValue();
        }

        return entityId;
    }
    
    /**
     * Translates the String supplied to the corresponding mnemonic name.<br>
     * If the supplied string is a numeric value of a mnemonic, then the mnemonic is returned<br>
     * If the supplied string is a numeric value that does not correspond to a mnemonic or is not a numeric value, then the corresponding numeric Entity ID is returned<br>
     * @param mnemonicOrEntityId the String a numeric value in String format or a String mnemonic
     * @return the String mnemonic name of the supplied value
     */
    public String translateIdToMnemonic(final String mnemonicOrEntityId) {
    	long entityId = -1;
    	try {
    		final Long tmp = Long.parseUnsignedLong(mnemonicOrEntityId);
    		entityId = tmp;
    	} catch (final NumberFormatException e) {
    		return mnemonicOrEntityId;
    	}
    	
    	for(final Entry<String,Long> val : mnemonicToEntityIdMap.entrySet()) {
    		if(val.getValue() == entityId) {
    			return val.getKey();
    		}
    	}
    	
    	return mnemonicOrEntityId;
    	
    }

    /**
     * Get the complete mnemonics mapped.
     *
     * @return list of mnemonics defined
     */
    public List<String> getMnemonics() {
        return Lists.newArrayList(mnemonicToEntityIdMap.keySet());
    }


    /**
     * Get a copy of the mnemonic map
     *
     * @return map of mnemonics to entity ID
     */
    public Map<String, Long> getMnemonicMap() {
        return Maps.newHashMap(mnemonicToEntityIdMap);
    }


    /**
     * Get the list of supported CFDP service classes
     * 
     * @return the list of CFDP service classes supported
     */
    public List<UnsignedInteger> getServiceClasses() {
    	return serviceClasses;
    }

    /**
     * Gets the unique prefix for properties loaded by this properties object. This method should be overridden
     * by subclasses so that the different objects can all be placed into a hash table or
     * set. The hashCode is computed from the lookup key.
     *
     * @return lookup key string
     * 8/10/16 - Added during R8 refactor
     */
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

}
