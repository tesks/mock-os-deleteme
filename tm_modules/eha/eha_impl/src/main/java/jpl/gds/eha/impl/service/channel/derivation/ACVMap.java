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
package jpl.gds.eha.impl.service.channel.derivation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jpl.gds.eha.api.channel.IServiceChannelValue;


/**
 * A special map for IChannelValue. Since the equal for ACV tests only the
 * actual value, and not the channel id, this map is used to act like a set. The
 * key is also useful to look up the channel values.
 * <p>
 * NOTE: This class has been marked deprecated. The reason for this is that the
 * derivation interfaces have changed to use just a Map. (The more customer
 * classes we expose in our interfaces, the more we have to control and
 * maintain, and this class is used by our runtime logic. We want classes used
 * by our runtime logic to be those we can change freely. For instance, this
 * class is now in the wrong package, but I CANNOT move it, because the mission
 * derivations break.) We want folks writing derivations to use the new
 * interface, and not the old. To discourage further use of this class, we want
 * the customer building the derivations to KNOW it is deprecated for their use.
 * There is really no problem with us using it internally. At some point in
 * time, the new derivation interface will have been in place long enough that
 * we will be free to move or change this class at will. At that time, we can
 * remove the deprecated indicator.
 *
 *
 * @deprecated
 */
public class ACVMap extends Object
implements Map<String, IServiceChannelValue>
{
    /**
     * Map of channel ID to channel value.
     */
	private final Map<String, IServiceChannelValue> _map;


	/**
	 * Construct an ACVMap using the given Map. Note that the input Map is 
	 * not copied.
	 * 
	 * @param map a Map of Channel ID to channel value
	 */
	public ACVMap(final Map<String, IServiceChannelValue> map)
	{
		super();

		if (map == null)
		{
			throw new IllegalArgumentException();
		}

		_map = map;
	}


	/**
	 * Construct an empty ACVMap.
	 */
	public ACVMap()
	{
		this(new HashMap<String, IServiceChannelValue>());
	}


	/**
	 * Construct an ACVMap with an initial capacity.
	 *
	 * @param capacity the number of expected entries in the map
	 */
	public ACVMap(final int capacity)
	{
		this(new HashMap<String, IServiceChannelValue>(capacity));
	}


	/**
	 * Construct an ACVMap from another ACVMap. A new Map will be created
	 * with the same elements as the input Map.
	 *
	 * @param other the other ACVMap
	 */
	public ACVMap(final ACVMap other)
	{
		this(new HashMap<String, IServiceChannelValue>(other));
	}


    /**
     * Construct an ACVMap from a list of channel values. Take the elements in
     * order, so we keep only the latest of each channel.
     * 
     * @param other a list of channel values to create the map from
     */
	public ACVMap(final List<? extends IServiceChannelValue> other)
	{
		this(new HashMap<String, IServiceChannelValue>(other.size()));

		for (final IServiceChannelValue acv : other)
		{
			if (acv != null)
			{
				add(acv);
			}
		}
	}

	/**
	 * Return an immutable empty ACVMap. This cannot be a static final attribute
	 * because of initialization ordering.
	 *
	 * @return Immutable empty map
	 */
	public static ACVMap getEmptyMap()
	{
		return SafeACVMap.EMPTY;
	}


	/**
	 * Checks that the key matches the value's channel id.
	 *
	 * @param key   The channel id
	 * @param value IInternalChannelValue to add to add
	 *
	 * @return the old value, or null
	 */
	@Override
    public IServiceChannelValue put(final String key,
			final IServiceChannelValue value)
	{
		if (key == null)
		{
			throw new IllegalArgumentException("ACVMap doesn't accept null key");
		}

		if (value == null)
		{
			throw new IllegalArgumentException("ACVMap doesn't accept null value");
		}

		final String vkey = value.getChanId();

		if (! key.equals(vkey))
		{
			throw new IllegalArgumentException("ACV mismatch: " +
					key              +
					" versus "       +
					vkey);
		}

		return _map.put(vkey, value);
	}


	/**
	 * Incorporates the values in another map into the existing map, checking as we go for 
	 * null values. Null values will not be incorporated.
	 *
	 * @param other the Map of channel IDs and channel value to add
	 */
	@Override
    public void putAll(
			final Map<? extends String, ? extends IServiceChannelValue> other)
	{
		if (other == null)
		{
			return;
		}

		for (final IServiceChannelValue acv : other.values())
		{
			if (acv != null)
			{
				add(acv);
			}
		}
	}

	/**
	 * Convenience method to add to map.
	 *
	 * @param acv IInternalChannelValue to add to map
	 */
	public void add(final IServiceChannelValue acv)
	{
		if (acv == null)
		{
			throw new IllegalArgumentException("ACVMap doesn't accept null value");
		}

		final String ci = acv.getChanId();

		if (ci == null)
		{
			throw new IllegalArgumentException("ACVMap doesn't accept null key");
		}

		_map.put(ci, acv);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Map#get(java.lang.Object)
	 */
	@Override
    public IServiceChannelValue get(final Object key)
	{
		return _map.get(key);
	}

	/**
	 * Convenience method to get from map
	 *
	 * @param key Name of channel id
	 *
	 * @return IInternalChannelValue
	 */
	public IServiceChannelValue get(final String key)
	{
		return _map.get(key);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	@Override
    public boolean containsKey(final Object key)
	{
		return _map.containsKey(key);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	@Override
    public boolean containsValue(final Object value)
	{
		return _map.containsValue(value);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Map#size()
	 */
	@Override
    public int size()
	{
		return _map.size();
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Map#isEmpty()
	 */
	@Override
    public boolean isEmpty()
	{
		return _map.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Map#clear()
	 */
	@Override
    public void clear()
	{
		_map.clear();
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Map#entrySet()
	 */
	@Override
    public Set<Map.Entry<String, IServiceChannelValue>> entrySet()
	{
		return _map.entrySet();
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Map#keySet()
	 */
	@Override
    public Set<String> keySet()
	{
		return _map.keySet();
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Map#values()
	 */
	@Override
    public Collection<IServiceChannelValue> values()
	{
		return _map.values();
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	@Override
    public IServiceChannelValue remove(final Object o)
	{
		return _map.remove(o);
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object other)
	{
		if (other == this)
		{
			return true;
		}

		if (! (other instanceof ACVMap))
		{
			return false;
		}

		final ACVMap o = (ACVMap) other;

		return _map.equals(o._map);
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return _map.hashCode();
	}

    /**
     * Creates a non-modifiable ACVMap
     *
     */
	public static class SafeACVMap extends ACVMap
	{
	    /**
	     * An EMPTY map.
	     */
		public static final SafeACVMap EMPTY = new SafeACVMap();


		/**
		 * Construct an empty SafeACVMap.
		 *
		 * @param capacity
		 */
		 private SafeACVMap()
		{
			super(Collections.unmodifiableMap(
					new HashMap<String, IServiceChannelValue>(0)));
		}


		/**
		 * Construct a SafeACVMap from another one. Note the map is deep-copied.
		 *
		 * @param other  the map to copy
		 */
		 public SafeACVMap(final ACVMap other)
		 {
			 super(Collections.unmodifiableMap(deepCopy(other._map)));
		 }


		 /**
		  * Deep-copy the underlying map so things are totally safe.
		  *
		  * @param other The map to deep copy.
		  *
		  * @return the deep-copied result map
		  */
		 private static Map<String, IServiceChannelValue> deepCopy(
				 final Map<String, IServiceChannelValue> other)
				 {
			 final Map<String, IServiceChannelValue> map =
				 new HashMap<String, IServiceChannelValue>(other);
			 // R8 Refactor TODO - Trying this without a copy.
//			 for (final Map.Entry<String, IServiceChannelValue> e :
//				 other.entrySet())
//			 {
//			     // R8 Refactor TODO - Trying this without a copy. Why is a deep copy of the entire channel value
//			     // necessary?  This seems like it could affect performance. I see no reason 
//				 map.put(e.getKey(),e.getValue().copy());
//			 }

			 return map;
	     }
	}
}
