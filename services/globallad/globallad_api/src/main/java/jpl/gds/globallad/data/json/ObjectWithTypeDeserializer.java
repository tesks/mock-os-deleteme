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
package jpl.gds.globallad.data.json;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Since the map objectStrings in the global lad are objects and can be any class and the json serializtion
 * will make all objectStrings strings, we include the type of the objectString to be written to the json string.
 * 
 * Expecting the objectString to to be "{value as string}:{class}".
 * 
 * This requires that whatever class type is used as a objectString it must have a constructor with a single string
 * input parameter, which is used to create the objectString value//
 */
public class ObjectWithTypeDeserializer extends JsonDeserializer<Object> {
	private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<String, Class<?>>();

	public ObjectWithTypeDeserializer() {	}

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public Object deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		String objectString = jp.getText();
		String[] objectStringChunks = objectString.split(ObjectWithTypeSerializer.SEP);
		
		if (objectStringChunks.length == 2) {
			String valueString = objectStringChunks[0];
			String className = objectStringChunks[1];
			
			Class<?> clazz = classCache.get(className);
		
			if (clazz == null) {
				try {
					clazz = Class.forName(className);
					classCache.put(className, clazz);
				} catch (ClassNotFoundException e) {
					// Let if fail down the line.
					e.printStackTrace();
				}
			}
			
			/**
			 * Create an instance of the class using the constructor with a string constructor.
			 */
			Object convertedobjectString = null;
			
			try {
				Constructor<?> ctor = clazz.getConstructor(String.class);
				convertedobjectString = ctor.newInstance(valueString);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			
			return convertedobjectString;
		} else {
			return objectString;
		}
	}
}
