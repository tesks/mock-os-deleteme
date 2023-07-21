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
package jpl.gds.globallad.data.container;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ThreadFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;

public final class GlobalLadUtilities {
	/**
	 * Static property maps used to create ObjectMappers.
	 */
	private static final Map<String, Object> propertiesPretty; 
	private static final Map<String, Object> propertiesNotPretty;
	
	static {
		propertiesPretty = new HashMap<String, Object>(1);
		propertiesPretty.put(JsonGenerator.PRETTY_PRINTING, true);
		propertiesNotPretty = Collections.<String, Object>emptyMap();
	}
	
	/**
	 * Merges the map and uses an ert comparator for sorting.
	 * 
	 * @param source
	 * @param dest
	 * @param numRecords
	 * @return
	 */
	public static final Map<Object, Collection<IGlobalLADData>> mergeMapErt(
			final Map<Object, Collection<IGlobalLADData>> source, 
			final Map<Object, Collection<IGlobalLADData>> dest, 
			final int numRecords) {
		return mergeMap(IGlobalLadDataFactory.ertComparator, source, dest, numRecords);
	}

	/**
	 * Merges the map and uses an scet comparator for sorting.
	 * 
	 * @param source
	 * @param dest
	 * @param numRecords
	 * @return
	 */
	public static final Map<Object, Collection<IGlobalLADData>> mergeMapScet(
			final Map<Object, Collection<IGlobalLADData>> source, 
			final Map<Object, Collection<IGlobalLADData>> dest, 
			final int numRecords) {
		return mergeMap(IGlobalLadDataFactory.scetComparator, source, dest, numRecords);
	}
	
	/**
	 * Merges the map and uses an event comparator for sorting.
	 * 
	 * @param source
	 * @param dest
	 * @param numRecords
	 * @return
	 */
	public static final Map<Object, Collection<IGlobalLADData>> mergeMapEvent(
			final Map<Object, Collection<IGlobalLADData>> source, 
			final Map<Object, Collection<IGlobalLADData>> dest, 
			final int numRecords) {
		return mergeMap(IGlobalLadDataFactory.eventComparator, source, dest, numRecords);
	}
	
	/**
	 * Merges the maps based on the given time type.
	 * 
	 * @param timeType
	 * @param source
	 * @param dest
	 * @param numRecords
	 * @return
	 */
	public static final Map<Object, Collection<IGlobalLADData>> mergeMap(
			GlobalLadPrimaryTime timeType,
			final Map<Object, Collection<IGlobalLADData>> source, 
			final Map<Object, Collection<IGlobalLADData>> dest, 
			final int numRecords) {
		
		switch(timeType) {
		case ERT:
			return mergeMapErt(source, dest, numRecords);
		case EVENT:
			return mergeMapEvent(source, dest, numRecords);
		case SCET:
		case SCLK:
		case LST:
		default:
			return mergeMapScet(source, dest, numRecords);
		}
	}

	/**
	 * This takes the place of using Map.addAll(map).  This will merge all data by checking if keys exist
	 * and merging the data together.  If multiple data types are queried that have the same data it will
	 * merge those two collections.  An example would be if fsw EHA PWR-1234 were queried for both recorded and 
	 * real time.  If both had values this would merge the data together.
	 * 
	 * @param comparator - used for the sorted set used to merge colliding results.
	 * @param source - The source of the merge.  All data from source will be merged into dest.
	 * @param dest - The map to be changed in place.  
	 * @param numRecords - If this value is less than or equal to 0 no trimming will be done.  Otherwise all merged collections will 
	 * be trimmed to numRecords length.
	 * @return - dest after the merge is complete.  This will change the map in place.
	 */
	public static final Map<Object, Collection<IGlobalLADData>> mergeMap(final Comparator<IGlobalLADData> comparator, 
			final Map<Object, Collection<IGlobalLADData>> source, 
			final Map<Object, Collection<IGlobalLADData>> dest, 
			final int numRecords) {
		
		for (final Object sourceKey : source.keySet()) {
			if (dest.containsKey(sourceKey)) {
				/**
				 * Dest has a key, we need to do a merge.
				 */
				boolean changed = dest.get(sourceKey).addAll(source.get(sourceKey));
				
				if (changed && numRecords > 0 && dest.get(sourceKey).size() > numRecords) {
					/**
					 * Some trimming is required.  The easiest thing to do is create a new 
					 * tree and add the correct number of items to it.
					 */
					SortedSet<IGlobalLADData> newSet = new TreeSet<IGlobalLADData>(comparator);
					Iterator<IGlobalLADData> iterator = dest.get(sourceKey).iterator();
					
					while (iterator.hasNext() && newSet.size() < numRecords) {
						newSet.add(iterator.next());
					}
					
					/**
					 * Replace the old set with the new one.
					 */
					dest.put(sourceKey, newSet);
				}
			} else {
				/**
				 * The key was not found, just add the entry.
				 */
				dest.put(sourceKey, source.get(sourceKey));
			}
		}
		
		return dest;
	}
	
	/**
	 * Takes all the data objects within source and adds them to a new tree set ordered based
	 * on the time type. Note that the comparators used are in descending order, which means the first 
	 * elements in the list will be the greatest.  Use reversed=true to get an ascending set. 
	 * 
	 * Added method to flatten a result map into a sorted tree set of results.
	 * 
	 * Updating to use the flatten comparators for all the time types.
	 *
	 * @param timeType compare order for the resulting tree set
	 * @param source results map to be flattened.
	 * @param reversed If true the resulting set will be reversed. 
	 * @return A new tree set sorted based on timeType.
	 */
	public static final Collection<IGlobalLADData> flattenMap(GlobalLadPrimaryTime timeType, 
			final Map<Object, Collection<IGlobalLADData>> source, 
			boolean reversed) {
		Comparator<IGlobalLADData> comparator;

		switch(timeType) {
		case ERT:
			comparator = IGlobalLadDataFactory.flattenErtComparator;
			break;
		case EVENT:
			// Event and all are the same, starting at the event time.
		case ALL:
			/**
			 * When flattening the map we need to use the special flattenAllComparator.
			 */
			comparator = IGlobalLadDataFactory.flattenAllComparator;
			break;
		case SCET:
		case SCLK:
		case LST:
		default:
			comparator = IGlobalLadDataFactory.flattenScetComparator;
			break;
		}
		
		TreeSet<IGlobalLADData> results = new TreeSet<IGlobalLADData>(comparator);
		
		for (Collection<IGlobalLADData> datas : source.values()) {
			datas.stream().forEach(results::add);
		}

		return reversed ? results.descendingSet() : results;
	}
	
	/**
	 * @param jsonFile
	 * @param json
	 * @param prettyPrint
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static final void writeJsonToFile(final File jsonFile, final JsonObject json, final boolean prettyPrint) throws FileNotFoundException, IOException {
		try (final FileOutputStream stream = new FileOutputStream(jsonFile)) {
			writeJsonToStream(json, stream, true, prettyPrint);
		}	
	}

	/**
	 * @param json
	 * @param stream
	 * @param closeStream
	 * @param prettyPrint
	 * @throws IOException
	 */
	public static final void writeJsonToStream(final JsonObject json, final OutputStream stream, final boolean closeStream, final boolean prettyPrint) throws IOException {
        try {
    		JsonWriter jsonWriter = Json
    				.createWriterFactory(prettyPrint ? propertiesPretty : propertiesNotPretty)
    				.createWriter(stream);
    		jsonWriter.writeObject(json);
        } finally {
    			if (closeStream) {
    				stream.close();
    			}
        }
	}
	
	/**
	 * Conv method to create a thread pool that will set the name of the workers based on the format.  An example 
	 * is "glad-data-creation-thread-%d".
	 * 
	 * @param baseWorkerNameFormat
	 * @return
	 */
	public static final ThreadFactory createThreadFactory(String baseWorkerNameFormat) {
		return new ThreadFactoryBuilder().setNameFormat(baseWorkerNameFormat).build();
	}
}
