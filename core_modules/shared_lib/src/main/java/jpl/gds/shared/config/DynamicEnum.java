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

package jpl.gds.shared.config;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic Dynamic Enum - Acts as an Enum but allows updating values at runtime
 *
 * @since 8.3
 *
 * @param <U> the enum class
 */

public class DynamicEnum<U extends DynamicEnum<U>>{
    /** map to hold elements, per class type */
    protected static Map<Class<? extends DynamicEnum<?>>, Map<String, DynamicEnum<?>>> elements =
            new LinkedHashMap<>();

    private final String name;

    /**
     * Get name
     * @return Name
     */
    public final String name() {
        return name;
    }

    private final int ordinal;

    /**
     * Get ordinal
     * @return Ordinal
     */
    public final int ordinal() {
        return ordinal;
    }

    /**
     * Constructor
     * @param name Name
     * @param ordinal Order
     */
    public DynamicEnum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
        Map<String, DynamicEnum<?>> typeElements = elements.get(getClass());
        if (typeElements == null) {
            typeElements = new LinkedHashMap<>();
            elements.putIfAbsent(getDynamicEnumClass(), typeElements);
        }
        typeElements.putIfAbsent(name, this);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends DynamicEnum<?>> getDynamicEnumClass() {
        return (Class<? extends DynamicEnum<?>>)getClass();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public final boolean equals(Object other) {
        if(other == null){
            return false;
        }
        if (this.getClass() != other.getClass()){
            return false;
        }

        //comparison based on enum name
        return this.name.equals((((DynamicEnum) other).name()));
    }

    @Override
    public final int hashCode() {
        //comparison based on enum name
        return name.hashCode();
    }

    /**
     * Gets object based on name
     * @param enumType Enumeration class
     * @param name String to get value from
     * @param <U> Generic type
     * @return Object
     */
    @SuppressWarnings("unchecked")
    public static <U extends DynamicEnum<U>> U valueOf(Class<U> enumType, String name) {
        U val = (U)elements.get(enumType).get(name);
        if(val != null){
            return val;
        }

        throw new IllegalArgumentException(
                "No enum constant " + enumType.getCanonicalName() + "." + name);
    }

    /**
     * Get all values the enum has registered
     * @param enumType Enumeration class
     * @param <U> Generic type
     * @return Array of values
     */
    @SuppressWarnings("unchecked")
    public static <U> U[] values(Class<U> enumType) {
        Collection<DynamicEnum<?>> values =  elements.get(enumType).values();
        int n = values.size();
        U[] typedValues = (U[])Array.newInstance(enumType, n);
        int i = 0;
        for (DynamicEnum<?> value : values) {
            Array.set(typedValues, i, value);
            i++;
        }

        return typedValues;
    }

    /** Clear all values */
    public static void clear(){
        elements.clear();
    }
}

