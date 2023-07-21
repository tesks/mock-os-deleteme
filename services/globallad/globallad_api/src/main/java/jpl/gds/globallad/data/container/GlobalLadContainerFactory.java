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
/**
 * 
 */
package jpl.gds.globallad.data.container;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.GlobalLadContainerException;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.json.MapKeyDeserializer;
import jpl.gds.globallad.data.utilities.JsonUtilities;

@SuppressWarnings("unchecked")
public class GlobalLadContainerFactory {
	/**
	 * Used for serialization of the global lad containers to be used to build the container hierarchy of the 
	 * data buffer.
	 */
	public static final String CONTAINER_PATH = "path";
	
	/**
	 * Used for serialization of the global lad containers as the key to note that the object 
	 * should be deserialized as a ring buffer.
	 */
	public static final String DATA_BUFFER = "dataBuffer";

	
	
	// The top level container type.
	public static final String MASTER_CONTAINER_TYPE_AND_IDENTIFIER = "master";
	
	// Data level container type
	public static final String RING_BUFFER_CONTAINER_TYPE_AND_IDENTIFIER = "identifier";
	
	private static final Object[] nullObjectArray = (Object[]) null;
	private static final Class<?>[] nullClassArray = (Class<?>[]) null;
	
	private static final MapKeyDeserializer keyDeserializer = new MapKeyDeserializer();

	private static final Map<String, String> childContainerMap;
	private static final Map<String, String> childGetMap;
	private static Constructor<? extends IGlobalLadContainer> ringBufferConstructor;

	static {
		GlobalLadProperties gladConfig = GlobalLadProperties.getGlobalInstance();
		
		try {
			Class<? extends IGlobalLadContainer> ringBufferClass = 
					(Class<? extends IGlobalLadContainer>) Class.forName(gladConfig.getRingBufferClassName());
			ringBufferConstructor = ringBufferClass.getConstructor(String.class, Object.class, byte.class);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		childContainerMap = gladConfig.getChildMapping();
		childGetMap = new HashMap<String, String>();

		for (Entry<String, String> entry : childContainerMap.entrySet()) {
			childGetMap.put(entry.getKey(),
			String.format("get%s", WordUtils.capitalize(entry.getValue())));
		}
	}
	
	/**
	 * Checks if the container type matches the master identifier.
	 * @param container
	 * @return
	 */
	public static boolean isMasterContainer(IGlobalLadContainer container) {
		return MASTER_CONTAINER_TYPE_AND_IDENTIFIER.equals(container.getContainerType());
	}
	
	/**
	 * @return child container map.
	 */
	public static Map<String, String> getChildContainerMap() {
		return childContainerMap;
	}
	
	/**
	 * @param parentContainerType
	 * @param data
	 * @return
	 * @throws GlobalLadContainerException 
	 */
	public static IGlobalLadContainer createChildContainer(String parentContainerType, IGlobalLADData data) throws GlobalLadContainerException {
		String childType = childContainerMap.get(parentContainerType);
		String getMethod = childGetMap.get(parentContainerType);
		
		if (childType == null) {
			throw new GlobalLadContainerException("No child container mapping was found for parent container type " + parentContainerType);
		} 

		try {
			if (childType.equals(RING_BUFFER_CONTAINER_TYPE_AND_IDENTIFIER)) {
				return createRingBuffer("identifier", data.getIdentifier(), data.getUserDataType());
			} else {
				Method m = data.getClass().getMethod(getMethod, nullClassArray);
				Object identifier = m.invoke(data, nullObjectArray);
				
				return createChildContainer(childType, identifier);
			}
		} catch (Exception e) {
			System.err.println(ExceptionUtils.getFullStackTrace(e));
			throw new GlobalLadContainerException(String.format("Could not create container for parent container type %s, child type %s and data %s: %s", 
					parentContainerType, childType, data, e.getMessage()));
		}
	}

	/**
	 * A master container is the top level global lad container.  Basically this is the root 
	 * node of the global lad data tree.
	 * 
	 * @return
	 */
	public static IGlobalLadContainer createMasterContainer() {
		return createChildContainer(MASTER_CONTAINER_TYPE_AND_IDENTIFIER, MASTER_CONTAINER_TYPE_AND_IDENTIFIER);
	}
	
	/**
	 * Creates a new instance of the object identified in the config as the ring buffer class.
	 * 
	 * @param containerType
	 * @param identifier
	 * @param userDataType
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static IGlobalLadContainer createRingBuffer(String containerType, Object identifier, byte userDataType) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return ringBufferConstructor.newInstance(containerType, identifier, userDataType);
	}
	
	/**
	 * Creates a container with the the given type and identifier.
	 * 
	 * @param containerType - Used to identify what type of data this container is holding.  Child containers
	 * are created with reflection based on the mapping in the global lad properties file.
	 * @param identifier - Object identifier for this container.  Used when inserting or querying global lad data.
	 */
	public static IGlobalLadContainer createChildContainer(String containerType, Object identifier) {
		return new BasicGlobalLadContainer(containerType, identifier);
	}
	
	/**
	 * Serialization methods below for rehydrating containers.  This should be used to deserialize a full global 
	 * lad container map.  If the data is as simple as a list of global lad objects the object mapper should be used.
	 */
	
	/**
	 * 
	 * @param dumpFile
	 * 
	 * @return 
	 * 
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws GlobalLadContainerException
	 */
	public static IGlobalLadContainer rehydrate(File dumpFile) throws JsonParseException, IOException, GlobalLadContainerException {
		JsonParser parser = JsonUtilities.getSerializationMapper()
				.getFactory()
				.createParser(dumpFile);
		
		return rehydrate(parser);
	}

	/**
	 * @param raw
	 * @return
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws GlobalLadContainerException
	 */
	public static IGlobalLadContainer rehydrate(byte[] raw) throws JsonParseException, IOException, GlobalLadContainerException {
		
		JsonParser parser = JsonUtilities.getSerializationMapper()
				.getFactory()
				.createParser(raw);
		
		return rehydrate(parser);
	}

	
	/**
	 * Recreates a global lad container from the input stream.  
	 *  
	 * @throws IOException 
	 * @throws JsonParseException 
	 * @throws GlobalLadContainerException 
	 */
	public static IGlobalLadContainer rehydrate(InputStream stream) throws JsonParseException, IOException, GlobalLadContainerException {
		
		JsonParser parser = JsonUtilities.getSerializationMapper()
				.getFactory()
				.createParser(stream);
		
		return rehydrate(parser);
	}
	
	/**
	 * 
	 * @param parser
	 * @return
	 * @throws JsonParseException
	 * @throws IOException
	 * @throws GlobalLadContainerException
	 */
	public static IGlobalLadContainer rehydrate(JsonParser parser) throws JsonParseException, IOException, GlobalLadContainerException {

		IGlobalLadSerializable master = (IGlobalLadSerializable) GlobalLadContainerFactory.createMasterContainer();
		
		List<String> ctypes = new ArrayList<String>();
		List<Object> cids = new ArrayList<Object>();
		
		/**
		 * The containers are expected to be serialized as a list of objects with the path and data buffer keyed objects.
		 * The path is used to create the container path to the data buffer.  
		 */
		while (parser.nextToken() != null) {
			String fieldName = parser.getCurrentName();
			if (CONTAINER_PATH.equals(fieldName)) {
				parser.nextToken(); // Current token is a [
				
				while (parser.nextToken() != JsonToken.END_ARRAY) {
					String p = parser.getText();
					String[] chunks = StringUtils.split(p, ",");
					
					Object id = keyDeserializer.deserializeKey(chunks[0], null);
					String type = chunks[1];
					
					if (!GlobalLadContainerFactory.MASTER_CONTAINER_TYPE_AND_IDENTIFIER.equals(type)) {
						ctypes.add(type);
						cids.add(id);
					}
				}
			} else if (DATA_BUFFER.equals(fieldName)) {
				parser.nextToken();
				IGlobalLadContainer container = parser.readValueAs(IGlobalLadContainer.class);
				master.rehydrate(container, ctypes, cids);
				ctypes.clear();
				cids.clear();
			}
		}
		
		parser.close();
		return (IGlobalLadContainer) master;
	}
}
