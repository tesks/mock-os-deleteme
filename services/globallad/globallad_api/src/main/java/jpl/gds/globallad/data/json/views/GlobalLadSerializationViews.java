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
package jpl.gds.globallad.data.json.views;

/**
 * 
 * Views class used to define views to be used when serializing / deserializing global lad data objects
 * using Jersey / Jackson.
 * <br>
 * The global lad data words are annotated to have multiple views, which means that the output JSON will have different values.
 * <p>
 * <b>GlobalView</b> - All output formats have these values.
 * <br>
 * <b>RestRequestView</b> - Only used for returning query results.  The values here are mostly converted on the fly from the stored data 
 * within the data objects.
 * <br>
 * <b>SerializationView</b> - Used to serialize the objects to JSON.  This will be a strict set of data that can be used to recreate the object at a later time.
 * <br>
 * <b>NewView</b> - Used by Jersey to serialize objects that implement IGlobalLadSerializable.  This interface has been deprecated and should be removed in 
 * future releases.
 * <br>
 */
public class GlobalLadSerializationViews {
	/**
	 * Global view.  If this view is set on a field it will be included in both the RestRequestView and SerializationView.
	 */
	public static class GlobalView {}
	
	/**
	 * Only used for returning query results.  The values here are mostly converted on the fly from the stored data 
	 * within the data objects.	 
	 */
	public static class RestRequestView extends GlobalView {}
	
	/**
	 * Used to serialize the objects to JSON.  This will be a strict set of data that can be used to recreate the object at a later time.
 	 */
	public static class SerializationView extends GlobalView {}
	
	/**
	 * Used by Jersey to serialize objects that implement IGlobalLadSerializable.  This interface has been deprecated and should be removed in 
	 * future releases.	 
	 */
	public static class NewView {}
}
