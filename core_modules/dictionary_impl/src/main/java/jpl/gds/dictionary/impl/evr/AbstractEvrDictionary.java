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
package jpl.gds.dictionary.impl.evr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.xml.sax.SAXException;

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDictionary;
import jpl.gds.dictionary.impl.AbstractBaseDictionary;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;


/**
 * AbstractEvrDictionary is the base class all EVR Dictionary parsers should
 * extend. It provides a hash map for storage of parsed Evrs and general access
 * methods to the EVR List.
 *
 * Initialize version to "unknown" rather that null so that the absence of an XML attribute
 * with the expected name does not cause a Null Pointer Exception.
 * 

 */
public abstract class AbstractEvrDictionary extends AbstractBaseDictionary implements IEvrDictionary {

	/** Specific EVR debugging tracer */
    protected final static Tracer                    debugTracer   = TraceManager
            .getTracer(Loggers.TLM_EVR);


	/*
	 * evrList must be synchronized so that we can make an atomic snapshot for other threads
	 */
	/** List of EVR definitions that have been parsed. */
	private final List<IEvrDefinition> evrList = Collections.synchronizedList(new ArrayList<IEvrDefinition>());

	private final Map<Long, IEvrDefinition> evrMapById = Collections.synchronizedMap(new HashMap<Long, IEvrDefinition>());
	
	/*  Make this map sorted for predictable results with Java 8.*/
	/** Map of EVR enumeration argument typedefs that have been parsed. */
	private final Map<String, EnumerationDefinition> typedefs = new TreeMap<String, EnumerationDefinition>();

	/** True if we are parsing the EVR format string */
	private boolean parsingFormat = false;

	/** EVR format string, so far. Used only when parsingFormat is true */
	private final StringBuilder formatSoFar = new StringBuilder();

	/** The EVR definition currently under construction by the parser */
	protected IEvrDefinition currentEvrDefinition;


	/**
	 * Constructor.
	 * 
	 * @param maxSchemaVersion the maximum supported schema version
	 * 
	 */

	AbstractEvrDictionary(final String maxSchemaVersion) {
	    super (DictionaryType.EVR, maxSchemaVersion);
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.IBaseDictionary#clear()
	 */
	@Override
	public void clear() {
		this.evrList.clear();
		this.evrMapById.clear();
		this.parsingFormat = false;
		this.formatSoFar.setLength(0);
		this.currentEvrDefinition = null;
		super.clear();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrDictionary#getEvrDefinitions()
	 */
	@Override
	public List<IEvrDefinition> getEvrDefinitions() {
		/*
		 * Now copies the contents of evrList to a local list
		 * in order to allow iteration through it without the danger of a ConcurrentModificationException.
		 */
		synchronized(this.evrList) {
			return Collections.unmodifiableList(new ArrayList<IEvrDefinition>(this.evrList));
		}
	}
	
	@Override
	public Map<Long, IEvrDefinition> getEvrDefinitionMap() {
	    return Collections.unmodifiableMap(this.evrMapById);
	}

	@Override
	public IEvrDefinition getDefinitionFromEvrId(final Long id) {
	    return this.evrMapById.get(id);
	}
	  
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrDictionary#getEnumDefinitions()
	 */
	@Override
	public Map<String,EnumerationDefinition> getEnumDefinitions() {
		return Collections.unmodifiableMap(this.typedefs);
	}

	/**
	 * Appends a newly parsed EVR Definition to the running EVRs list.
	 * 
	 * @param evr	<code>IEvrDefinition</code> object to add to the parsed EVRs
	 * 				list
	 */
	protected void addEvr(final IEvrDefinition evr) {
		this.evrList.add(evr);
		this.evrMapById.put(evr.getId(), evr);
	}

	/**
	 * Adds a new enumeration definition (enum typedef) to the map
	 * of enumeration definitions.
	 * 
	 * @param d EnumerationDefinition to add
	 */
	protected void addEnumTypedef(final EnumerationDefinition d) {
		this.typedefs.put(d.getName(), d);
	}

	/**
	 * Gets an enumeration definition (enum typedef) from the map of enumeration
	 * definitions.
	 * 
	 * @param name
	 *            name of the enumeration definition to get
	 * @return the EnumerationDefinition object associated with the input name,
	 *         or null if no match found
	 */
	protected EnumerationDefinition getEnumTypedef(final String name) {
		return this.typedefs.get(name);
	}


	/**
	 * Begin accumulating characters for EVR format string.
	 */
	protected void startFormatString()
	{
		if (this.parsingFormat)
		{
			return;
		}

		this.parsingFormat = true;

		this.formatSoFar.setLength(0);

		final String soFar = this.currentEvrDefinition.getFormatString();

		if (soFar != null)
		{
			this.formatSoFar.append(soFar);
		}
	}


	/**
	 * Accumulate more characters for EVR format string.
	 *
	 * @param str    Characters to add
	 * @param offset Offset within str to start
	 * @param length Length within str to append
	 */
	protected void accumulateFormatString(final char[] str,
			final int    offset,
			final int    length)
	{
		if (this.parsingFormat)
		{
			this.formatSoFar.append(str, offset, length);
		}
	}


	/**
	 * Stop accumulating characters for EVR format string and save result.
	 */
	protected void endFormatString()
	{
		if (! this.parsingFormat)
		{
			return;
		}

		if (this.formatSoFar.length() > 0)
		{
			this.currentEvrDefinition.setFormatString(this.formatSoFar.toString());
		}
		else
		{
			this.currentEvrDefinition.setFormatString(null);
		}

		this.parsingFormat = false;

		this.formatSoFar.setLength(0);
	}
	
	/**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.impl.impl.impl.evr.AbstractEvrDictionary#characters(char[],
     *      int, int)
     */
    @Override
    public void characters(final char[] ch, final int start, final int end)
            throws SAXException {

        text.append(ch, start, end);

        // Also add for format string if needed
        accumulateFormatString(ch, start, end);
    }
}
