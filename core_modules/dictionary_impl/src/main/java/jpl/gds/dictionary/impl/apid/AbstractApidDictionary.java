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
package jpl.gds.dictionary.impl.apid;

import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.IApidDictionary;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.impl.AbstractBaseDictionary;

import java.util.*;

/**
 * AbstractApidDictionary is the base class for APID dictionary parsers.
 *
 * Initialize version to "unknown" rather
 * that null so that the absence of an XML attribute with the expected
 *  name does not cause a Null Pointer Exception.
 * 
 *
 */
public abstract class AbstractApidDictionary extends AbstractBaseDictionary implements IApidDictionary {

	/**
	 * Stores the sorted set of EVR APIDs.
	 */
	private Set<Integer> evrApids;
	/**
	 * Stores the sorted set of product APIDs.
	 */
	private Set<Integer> productApids;
	/**
	 * Stores the sorted set of channel APIDs.
	 */
	private Set<Integer> ehaApids;
	/**
	 * Stores the sorted set of Decom APIDs.
	 */
	private Set<Integer> decomApids;
    /**
     * Stores the sorted set of CFDP APIDs.
     */
    private Set<Integer> cfdpApids;
	/**
	 * The map of APID number to APID definition object.
	 * 
	 * Make this map sorted for predictable results with Java 8.
	 */
    protected final Map<Integer, IApidDefinition> apids     = new TreeMap<>();
	
	/**
	 * Maximum APID number;
	 */
	private int finalApid = -1;
	
	/**
	 * Package protected constructor.
	 * @param maxSchemaVersion the currently implemented max schema version
	 * 
	 */
	AbstractApidDictionary(final String maxSchemaVersion) {
	    super(DictionaryType.APID, maxSchemaVersion);
	}

	@Override
	public synchronized void clear() {
		apids.clear();
		evrApids = null;
		productApids = null;
		ehaApids = null;
		decomApids = null;
        cfdpApids = null;
		finalApid = -1;
	    super.clear();	
	}

	@Override
	public int getMaxApid() {
		if (finalApid == -1) {
			final Collection<Integer> keys = apids.keySet();
			for (final Integer apid : keys) {
				if (apid > finalApid) {
					finalApid = apid;
				}
			}
		}
		return finalApid;
	}

	@Override
	public List<IApidDefinition> getApidDefinitions() {
        return new LinkedList<>(this.apids.values());
	}

	@Override
	public IApidDefinition getApidDefinition(final int apid) {
		return apids.get(apid);
	}

	@Override
	public IApidDefinition getApidDefinition(final String apidName) {
	    for (final IApidDefinition apid : apids.values()) {
	        if (apid.getName().equalsIgnoreCase(apidName)) {
	            return apid;
	        }
	    }
	    return null;
	}
	

	@Override
	public String getApidName(final int apid) {
		final IApidDefinition def = getApidDefinition(apid);
		if (def == null) {
			return null;
		}
		return def.getName();
	}

	@Override
	public boolean isDefinedApid(final int apid) {
		return apids.get(apid) != null;
	}

	@Override
	public synchronized SortedSet<Integer> getEvrApids() {
		if (evrApids == null) {
			extractApids();
		}
		if (evrApids == null) {
			return null;
		}
		return Collections.unmodifiableSortedSet((SortedSet<Integer>)evrApids);
	}

	@Override
	public synchronized SortedSet<Integer> getProductApids() {
		if (productApids == null) {
			extractApids();
		}
		if (productApids == null) {
			return null;
		}
		return Collections.unmodifiableSortedSet((SortedSet<Integer>)productApids);
	}

	@Override
	public synchronized SortedSet<Integer> getChannelApids() {
		if (ehaApids == null) {
			extractApids();
		}
		if (ehaApids == null) {
			return null;
		}
		return Collections.unmodifiableSortedSet((SortedSet<Integer>)ehaApids);
	}

	@Override
	public synchronized SortedSet<Integer> getDecomApids() {
		if (decomApids == null) {
			extractApids();
		}
		if (decomApids == null) {
			return null;
		}
		return Collections.unmodifiableSortedSet((SortedSet<Integer>)decomApids);
	}

    @Override
    public synchronized SortedSet<Integer> getCfdpApids() {
        if (cfdpApids == null) {
            extractApids();
        }
        if (cfdpApids == null) {
            return null;
        }
        return Collections.unmodifiableSortedSet((SortedSet<Integer>) cfdpApids);
    }


	/**
	 * Populates the APID sets for EVRs, products, channels, and decom APIDs.
	 * The corresponding Sets in this class should be set to null if the
	 * ApidDictionary implementation does not support the APID type. Otherwise,
	 * this method must populate all the APID sets.
	 */
	private synchronized void extractApids() {
		if (apids == null) {
			return;
		}
		final Set<Integer> set = apids.keySet();
		final Iterator<Integer> it = set.iterator();
		while (it.hasNext()) {
			final Integer key = it.next();
			final IApidDefinition a = apids.get(key);
			if (a.getContentType() == null) {
				continue;
			}

			switch (a.getContentType()) {
			case EVR:
				if (evrApids == null) {
					evrApids = new TreeSet<>();
				}
				evrApids.add(key);
				break;
			case DATA_PRODUCT:
				if (productApids == null) {
					productApids = new TreeSet<>();
				}
				productApids.add(key);
				break;
			case PRE_CHANNELIZED:
				if (ehaApids == null) {
					ehaApids = new TreeSet<>();
				}
				ehaApids.add(key);
				break;
			case DECOM_FROM_MAP:
				if (decomApids == null) {
					decomApids = new TreeSet<>();
				}
				decomApids.add(key);
				break;
			case CFDP_DATA:
			case CFDP_PROTOCOL:
			    if (cfdpApids == null) { 
			        cfdpApids = new TreeSet<>();
			    }
			    cfdpApids.add(key);
			    break;
			default:
				break;
			}
		}
	}
}
