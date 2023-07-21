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
package jpl.gds.eha.api.channel;

import java.util.HashMap;
import java.util.Map;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.eha.api.config.EhaProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.types.Pair;

/**
 * ChannelChangeFilter is used to detect changes in channel values, within a
 * configured in tolerance, and keeps track of current, previous, and delta
 * values for channels. It is used in both database and message processing when
 * channel change detection is needed.
 * 
 *
 */
public class ChannelChangeFilter {
    
    private static final Tracer             logger     = TraceManager.getDefaultTracer();

    // Maps to latest type and value for each channel id
    private final Map<String, ChannelValue> _changeMap =
        new HashMap<String, ChannelValue>();
    private final double       _double_factor;
    private final Map<String, Object> _deltaMap = new HashMap<String, Object>();
    private final Map<String, Object> _lastMap =
        new HashMap<String, Object>();
    
    

    /**
     * Constructor.
     * 
     * @param props
     *            The Eha Properties object
     */
    public ChannelChangeFilter(final EhaProperties props) {

        // These are usually [0.0, 1.0] but negative can be used to turn off
        // equality checking completely.

        _double_factor = props.getFloatChangeFactor();
    }

    /**
     * Determines if the given DatabaseChannelSample represents a DN value change from
     * the previously recorded one.
     * 
     * @param ci
     *            the ID of the channel to check
     * @param ct
     *            the type of the Channel to check
     * @param value
     *            the value of the Channel to check
     * 
     * @return the same channel sample if the channel changed value, null if not
     * @throws IllegalStateException
     *             if the data type of the channel is not consistent
     *             with the previous value
     */
    public boolean getFilteredValue(final String ci, final ChannelType ct, final Object value) 
      throws IllegalStateException {

 //       final String ci = val.getChannelId();

        if (ci == null)
        {
            throw new IllegalArgumentException(
                          "Input channel value has a null channel id");
        }

//        final ChannelType ct = val.getChannelType();

        if (ct == null)
        {
            throw new IllegalArgumentException(
                          "Input channel value has a null channel type in " +
                          "its channel definition");
        }

 //       final Object value = val.getValue();

        if (value == null)
        {
            throw new IllegalArgumentException(
                          "Input channel value DN is null");
        }

        // Convert to our real enum

        // Check for a channel-value change

        final ChannelValue latest =   new ChannelValue(ct,
                value,
                _double_factor);

        final ChannelValue last   = _changeMap.get(ci);

        if (last != null)
        {
            if (latest.getType() != last.getType())
            {
                throw new IllegalStateException("Inconsistent type for channel id " +
                                       ci);
            }

            if (last.isEqual(latest))
            {
                // No change
                return false;
            }
        }

        // Remember this value and type

        if (last != null) {
            addDelta(ci, latest);
            _lastMap.put(ci, last.getValue());
        }

        _changeMap.put(ci, latest);

        // Return changed value
        return true;
    }

//    /**
//     * Determines if the given IInternalChannelValue represents a DN value change from
//     * the previously recorded one.
//     * 
//     * @param val the channel value to check
//     * @return the same channel value if the channel changed value, null if not 
//     * @throws IllegalStateException if the data type of the channel is not consistent
//     * with the previous value
//     */
//   public IInternalChannelValue getFilteredValue(final IInternalChannelValue val) 
//   throws IllegalStateException {
//
//        final String ci = val.getChanId();
//
//        if (ci == null)
//        {
//            throw new IllegalArgumentException(
//                          "Input channel value has a null channel id");
//        }
//
//        final ChannelType ct = val.getChannelType();
//
//        if (ct == null)
//        {
//            throw new IllegalArgumentException(
//                          "Input channel value has a null channel type in " +
//                          "its channel definition");
//        }
//
//        final Object value = val.getDn();
//
//        if (value == null)
//        {
//            throw new IllegalArgumentException(
//                          "Input channel value DN is null");
//        }
//
//  
//        // Check for a channel-value change
//
//        final ChannelValue latest = new ChannelValue(ct,
//                                                     value,
//                                                     _double_factor);
//
//        final ChannelValue last   = _changeMap.get(ci);
//
//        if (last != null)
//        {
//            if (latest.getType() != last.getType())
//            {
//                throw new IllegalStateException("Inconsistent type for channel id " +
//                                       ci);
//            }
//
//            if (last.isEqual(latest))
//            {
//                // No change
//                return null;
//            }
//        }
//
//        // Remember this value and type
//
//        if (last != null) {
//            addDelta(ci, latest);
//            _lastMap.put(ci, last.getValue());
//        }
//
//        _changeMap.put(ci, latest);
//
//        // Return changed value
//        return val;
//    }

    private void addDelta(final String id, final ChannelValue val) {
        final ChannelValue last   = _changeMap.get(id);
        Object delta = null;
        
        Number value = null;
        Number lastValue = null;
        switch(val.getType()) {
            case ASCII:
            case DIGITAL:
            case STATUS:
            case BOOLEAN:
            case UNKNOWN:
                // no delta
                break;
            case UNSIGNED_INT:
            case SIGNED_INT:
            case TIME:
            	//in theory we could just use the float/double code for these, but this makes sure
            	//we get nice round values (brn)
            	value = (Number)val.getValue();
            	lastValue = (Number)last.getValue();
            	delta = Long.valueOf(value.longValue() - lastValue.longValue());
                break;
           case FLOAT:
        	   	value = (Number)val.getValue();
           		lastValue = (Number)last.getValue();
           		delta = Double.valueOf(value.doubleValue() - lastValue.doubleValue());
                break;
           default:
               // no delta:
               break;
        }
        if (delta != null) {
            _deltaMap.put(id, delta);
        }
    }

    /**
     * Gets the delta value for the last change to a specific channel.
     * 
     * @param id the channel ID 
     * @return Object representing the channel delta value (type dependent upon
     * channel data type) or null if no delta recorded
     */
    public Object getDelta(final String id) {
        return _deltaMap.get(id);
    }


    /**
     * Gets the last DN value for to a specific channel.
     * 
     * @param id the channel ID 
     * @return Object representing the channel DN value (type dependent upon
     * channel data type) or null if no delta recorded
     */
    public Object getPreviousValue(final String id) {
        return _lastMap.get(id);
    }
    
    /**
     * Holds the value for a channel value as a type and an object. Object will
     * be Long, Double, or String. The value can be null.
     *
     * There is no special code necessary for unsigned, those values will just be
     * non-negative.
     *
     * For numeric types, him is equal to me when:
     *     Abs(me - him) <= Abs(me * factor) with factor in [0.0, 1.0]
     *
     * For any type, if the factor is negative, equality is forced false.
     *
     */
    public static class ChannelValue extends Pair<ChannelType, Object>
    {
        private static final long serialVersionUID = 1L;
        
        // Values in [0.0, 1.0] (or negative) that tell us when a change is "enough"
        private final double _double_factor;


        /**
         * Constructor.
         *
         * @param cte           Channel type
         * @param value         Value as Object
         * @param double_factor When a change is "enough" for floats
         */
        public ChannelValue(final ChannelType cte,
                            final Object      value,
                            final double      double_factor)
        {
            super(cte, value);

            if ((double_factor > 1.0) || Double.isNaN(double_factor))
            {
                throw new IllegalArgumentException(
                              "Double factor must be in [0.0,1.0] or negative");
            }

            _double_factor = double_factor;
        }


        /**
         * Get type.
         *
         * @return ChannelTypeEnum
         */
        public ChannelType getType()
        {
            return getOne();
        }


        /**
         * Get value.
         *
         * @return Object
         */
        public Object getValue()
        {
            return getTwo();
        }


        /**
         * Compare against another channel value.
         *
         * @param cv Channel value to compare against
         *
         * @return boolean
         */
        public boolean isEqual(final ChannelValue cv)
        {
            if (getType() != cv.getType())
            {
                logger.error("Inconsistent channel value types in ChannelChangeFilter");

                return false;
            }

            final Object left  = getValue();
            final Object right = cv.getValue();

            if (left == null)
            {
                return (right == null);
            }

            if (right == null)
            {
                return false;
            }

            if (left.getClass() != right.getClass())
            {
                logger.error(
                    "Inconsistent channel value object types in ChannelChangeFilter");

                return false;
            }

            if (left instanceof Double)
            {
                if (_double_factor < 0.0)
                {
                    return false;
                }

                final double l = ((Double) left).doubleValue();
                final double r = ((Double) right).doubleValue();

                // Two NaNs are considered equal, otherwise a single NaN
                // is not equal to anything.

                if (Double.isNaN(l))
                {
                    return Double.isNaN(r);
                }
                else if (Double.isNaN(r))
                {
                    return false;
                }

                // Two infinities are considered equal if they are the same sign,
                // otherwise a single infinity is not equal to anything.

                if (Double.isInfinite(l))
                {
                    if (! Double.isInfinite(r))
                    {
                        return false;
                    }

                    // Both infinite

                    if (l > 0.0D)
                    {
                        return (r > 0.0D);
                    }

                    return (r < 0.0D);
                }
                else if (Double.isInfinite(r))
                {
                    return false;
                }

                return (Math.abs(l - r) <= _double_factor);
            }

            // Not a type we handle specially, regular equals must handle

            return left.equals(right);
        }


        /**
         * Defined to explicitly show that we are not incorporating the new
         * attributes into the logical value.
         *
         * @param other Object to compare against
         *
         * @return boolean
         */
        @Override
        public boolean equals(final Object other)
        {
            return super.equals(other);
        }


        /**
         * Defined to explicitly show that we are not incorporating the new
         * attributes into the logical value.
         *
         * @return int
         */
        @Override
        public int hashCode()
        {
            return super.hashCode();
        }


        /**
         * Get string value. For boolean or status, may be set in addition to
         * other value.
         *
         * @param cte Type of channel
         * @param val Channel value
         *
         * @return String as value or status or null
         */
        public static String computeStringValue(final ChannelType   cte,
                                                final IServiceChannelValue val)
        {
            if (cte.equals(ChannelType.ASCII))
            {
                return val.stringValue();
            }

            if (cte.hasEnumeration())
            {
                return val.getStatus();
            }

            return null;
        }
    }

}
