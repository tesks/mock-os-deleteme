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
package jpl.gds.dictionary.impl.decom;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.decom.IChannelDecomDictionary;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapId;
import jpl.gds.dictionary.impl.AbstractBaseDictionary;
import jpl.gds.shared.log.Tracer;

/**
 * Multimission implementation of {@link IChannelDecomDictionary} as of AMPCS R7.4.  This implementation
 * is compatible with the pre-7.4 generic packet decom dictionary, but works differently in several fundamental ways.
 * It acts as a "virtual" dictionary; there is currently no actual file backing this dictionary.  Instead, it consists
 * of packet decom maps and shared generic decom maps that it loads lazily.
 * 
 * It does not truly need a channel map to be initialized; channel definitions are to be looked up lazily by channel
 * processors operating on generic decom maps.  In fact, the only reason this class implements IChannelDecomDictionary
 * and not IDecomDictionary directly is the need for backwards compatibility with the legacy packet-only generic decom maps.
 *
 */
public class MultimissionGenericDecomDictionary extends AbstractBaseDictionary implements IChannelDecomDictionary {

    private static final String MM_SCHEMA_VERSION = 
            DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.DECOM);
	
	private final Map<IDecomMapId, IDecomMapDefinition> decomMapsById = new HashMap<>();
	private final Map<Integer, IDecomMapDefinition> decomMapsByApid = new HashMap<>();
	@SuppressWarnings("unused")
	private Map<String, IChannelDefinition> channelMap;
	
	private IDecomMapDefinition generalMap = null;

	private String baseDir;
	private final static String PACKET_SUBDIR = "packet";
	private final String PACKET_MAP_PATH_TEMPLATE = PACKET_SUBDIR + File.separator + "apid_%d_packet_map.xml";

	/**
	 * Constructor.
	 */
	protected MultimissionGenericDecomDictionary() {
		super(DictionaryType.DECOM, MM_SCHEMA_VERSION);
	}
	
    @Override
    public void parse(final String dirName, final DictionaryProperties config)
            throws DictionaryException {
        parse(dirName, config, tracer);
    }

    @Override
    public void parse(final String dirName, final DictionaryProperties config, final Tracer log)
            throws DictionaryException {
        // No parsing needs to be done, but save the file argument, which should
        // be a directory
        if (dirName == null) {
            final String message = "Base directory for generic decom maps not defined.";
            tracer.error(message);
            throw new DictionaryException(message);
        }
        
        if (config == null) {
            throw new IllegalArgumentException("Dictionary configuration may not be null");
        }
        
        super.setDictionaryConfiguration(config);
        if (!new File(dirName).isDirectory()) {
            throw new DictionaryException(dirName + " is not a directory. Cannot load generic decom maps.");
        }
        baseDir = dirName;
    }

	@Override
	public void clear() {
		decomMapsById.clear();

	}

	@Override
	public IDecomMapDefinition addDecomMapFromFile(final String filename)
			throws DictionaryException {
		if (filename == null) {
			throw new DictionaryException("Decom Map file cannot be null");
		}
		final MultimissionDecomTelemetryMapParser parser = new MultimissionDecomTelemetryMapParser();
		return parser.parseFile(filename);
	}
	
	@Override
	public IDecomMapDefinition addDecomPacketMap(final int apid) throws DictionaryException {
		if (apid < 0) {
			throw new IllegalArgumentException("Illegal APID: " + apid
					+ " (cannot be negative)");
		}
		
		if (this.decomMapsByApid.containsKey(apid)) {
			tracer.debug("Replacing decom map for APID " + apid);
			this.decomMapsByApid.remove(apid);
		}
		
		final String filename = getFullFilePath(getFileNameForApid(apid));	
		final MultimissionDecomTelemetryMapParser parser = new MultimissionDecomTelemetryMapParser();
		final IDecomMapDefinition def = parser.parseFile(filename);
		this.decomMapsByApid.put(apid, def);
		return def;
	}

	private String getFileNameForApid(final int apid) {
		return String.format(PACKET_MAP_PATH_TEMPLATE, apid);
	}

	private String getFullFilePath(final String filename) {
		return baseDir + File.separator + filename;
	}

	@Override
	public IDecomMapDefinition getDecomMapByApid(final int apid) {
		final IDecomMapDefinition def = decomMapsByApid.get(apid);
		if (def == null) {
			try {
				this.addDecomPacketMap(apid);
			} catch (final DictionaryException e) {
				// R8 Refactor - do nothing; fall through and let the next statement handle
			}
		}
		return decomMapsByApid.getOrDefault(apid, getGeneralDecomMap());
	}

	@Override
	public IDecomMapDefinition getDecomMapById(final IDecomMapId id) {
		final IDecomMapDefinition def = decomMapsById.get(id);
		if (def == null) {
			final MultimissionMultiDecomMapParser parser = new MultimissionMultiDecomMapParser(id.getNamespace());
			try {
				final String filename = String.join(File.separator, id.getNamespaceElements()) + ".xml";
				parser.parseFile(getFullFilePath(filename));
			} catch (final DictionaryException e) {
				return null;
			}
			decomMapsById.putAll(parser.collectMaps());
		}
		return decomMapsById.get(id);
	}


	@Override
	public IDecomMapDefinition getGeneralDecomMap() {
		return generalMap;
	}


	@Override
	public Map<Integer, IDecomMapDefinition> getAllDecomMaps() {
		return decomMapsByApid;
	}


	@Override
	public void setChannelMap(final Map<String, IChannelDefinition> chanMap) {
		channelMap = chanMap;
	}


	@Override
	public void setGeneralMap(final IDecomMapDefinition map) {
		generalMap = map;
	}


	@Override
	public void addMap(final int apid, final IDecomMapDefinition map) {
		decomMapsByApid.put(apid, map);
	}



}
