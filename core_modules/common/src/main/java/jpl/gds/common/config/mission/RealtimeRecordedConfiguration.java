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
package jpl.gds.common.config.mission;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import jpl.gds.common.types.EhaBool;
import jpl.gds.common.types.RecordedBool;
import jpl.gds.dictionary.api.apid.ApidContentType;
import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.shared.annotation.Singleton;
import jpl.gds.shared.holders.ApidHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;


/**
 * RealtimeRecordedConfiguration accesses configuration values used by the
 * downlink applications to properly mark EHA/EVR as realtime or recorded.
 *
 * NB: If an APID or VCID is not defined, it uses the fallback value. That is
 * not necessarily the same as not being configured as recorded. That means
 * that there is always a check for validity first.
 *
 * Validity for APID means being in the dictionary; for VCID it means being in
 * the list of configured VCIDs.
 *
 * NB: This class is a singleton, except sometimes when running under JUnit.
 * To that end, the resetConfiguration method is made private and called by
 * reflection.
 *
 */
@Singleton
public final class RealtimeRecordedConfiguration extends Object
{
    /** Strategy choices */
    public static enum StrategyEnum
    {
        /** Determine recorded by APID */
        BY_APID,

        /** Determine recorded by VCID */
        BY_VCID,

        /** Recorded state is set unconditionally */
        UNCONDITIONAL;
    }

    private final Tracer              log;

    private static final RecordedBool SSE_BOOL         = RecordedBool.REALTIME;

    private final IApidDefinitionProvider _defs;

    /**
     * The configuration items themselves. We always set these, so they are
     *  never null, even when not really needed.
     *
     * NB: _knownVcids is the set of all VCIDs configured. It is a superset of
     * _vcids, which is a set of the recorded VCIDs.
     */


    private final StrategyEnum _strategy;
    private final RecordedBool _fallback;
    private final RecordedBool _unconditional;
    private final Set<Long>    _knownVcids;
    private final Set<Long>    _vcids;
    private final Set<Long>    _ehaApids;
    private final Set<Long>    _evrApids;
    private final Set<Long>    _cfdpApids;
    
    private final MissionProperties missionProps;

    /**
     * test constructor.
     * 
     * @param apidDefs
     *            IApidDictionary instance to use for determining realtime/
     *            recorded APIDs
     * @param mp
     *            the current MissionProperties configuration object
     *
     */
    public RealtimeRecordedConfiguration(final IApidDefinitionProvider apidDefs, final MissionProperties mp) {
        this(apidDefs, mp, TraceManager.getTracer(Loggers.CONFIG));
    }

    /**
     * Private constructor.
     * 
     * @param apidDefs
     *            IApidDictionary instance to use for determining realtime/
     *            recorded APIDs
     * @param mp
     *            the current MissionProperties configuration object
     * @param t
     *            Tracer to log with
     *
     */
    public RealtimeRecordedConfiguration(final IApidDefinitionProvider apidDefs, final MissionProperties mp,
            final Tracer t) {
        super();
        log = t;
        log.setPrefix("RtRecConfig:");
        missionProps = mp;



        StrategyEnum strategy = missionProps.getTelemetryMarkingStrategy();

        RecordedBool unconditional = missionProps.getUnconditionalTelemetryMarking();

        final RecordedBool fallback = mp.getTelemetryMarkingFallback();

        // Get all known (configured) VCIDs

        _knownVcids = new TreeSet<Long>();
        for (final Integer vcid: missionProps.getAllDownlinkVcids()) {
            _knownVcids.add(Long.valueOf(vcid.intValue()));
        }
      
        final Set<Long> vcids = missionProps.getRecordedVcids();

        if ((vcids == null || vcids.isEmpty()) && (strategy == StrategyEnum.BY_VCID))
        {
            log.error("No recorded VCIDs were found, " ,
                         "resetting strategy to " ,
                         StrategyEnum.UNCONDITIONAL ,
                         " and unconditional state to fallback of " ,
                         fallback);

            strategy      = StrategyEnum.UNCONDITIONAL;
            unconditional = fallback;
        }

       
        final Set<Long> ehaApids = getEhaApids(apidDefs);
        final Set<Long> evrApids = getEvrApids(apidDefs);

        final Set<Long> cfdpApids = getCfdpApids(apidDefs);

        if ((ehaApids == null) &&
            (evrApids == null) &&
            (cfdpApids == null) &&
            (strategy == StrategyEnum.BY_APID))
        {
            log.warn("No recorded APIDs were found, " ,
                           "resetting strategy to " ,
                           StrategyEnum.UNCONDITIONAL ,
                           " and unconditional state to fallback of " ,
                           fallback);

            strategy      = StrategyEnum.UNCONDITIONAL;
            unconditional = fallback;
        }

        _defs          = apidDefs;
        _strategy      = strategy;
        _fallback      = fallback;
        _unconditional = unconditional;
        _vcids         = ((vcids != null)
                              ? Collections.unmodifiableSet(vcids)
                              : Collections.<Long>emptySet());
        _ehaApids      = ((ehaApids != null)
                              ? Collections.unmodifiableSet(ehaApids)
                              : Collections.<Long>emptySet());
        _evrApids      = ((evrApids != null)
                              ? Collections.unmodifiableSet(evrApids)
                              : Collections.<Long>emptySet());

        _cfdpApids     = ((cfdpApids != null)
                              ? Collections.unmodifiableSet(cfdpApids)
                              : Collections.<Long>emptySet());

        final StringBuilder sb = new StringBuilder();

        sb.append("Strategy=").append(_strategy);
        sb.append(",Fallback=").append(_fallback);
        sb.append(",Unconditional=").append(_unconditional);
        sb.append(",VCIDs=").append(_knownVcids);
        sb.append(",recorded VCIDs=").append(_vcids);
        sb.append(",recorded EhaAPIDs=").append(_ehaApids);
        sb.append(",recorded EvrAPIDs=").append(_evrApids);
        sb.append(",recorded CfdpAPIDs=").append(_cfdpApids);

        log.debug(sb);
    }



    /**
     * Get the recorded APIDs.
     *
     * @param defs  APID dictionary
     * @param apids Set of APIDs to be examined
     *
     * @return Set of recorded APIDs
     */
    private Set<Long> getApids(final IApidDefinitionProvider defs,
                                      final Set<Long>       apids)
    {
        final Set<Long> set = new TreeSet<Long>();

        for (final Long apid : apids)
        {
            if ((apid == null)                ||
                (apid < ApidHolder.MIN_VALUE) ||
                (apid > ApidHolder.MAX_VALUE))
            {
                log.error("APID dictionary " ,
                             "returned out-of-range APID " ,
                             apid ,
                             "; skipped");
            }
            else if (defs.getApidDefinition(asInteger(apid)).isRecorded())
            {
                set.add(apid);
            }
        }

        return (! set.isEmpty() ? set : null);
    }


    /**
     * Get the recorded APIDs for EHA.
     *
     * @param defs APID dictionary
     *
     * @return Set of recorded APIDs
     */
    private Set<Long> getEhaApids(final IApidDefinitionProvider defs)
    {
        final Set<Long> apids = new TreeSet<Long>();
        Set<Integer>    temp  = defs.getChannelApids();

        if (temp != null)
        {
            for (final Integer i : temp)
            {
                apids.add(asLong(i));
            }
        }

        temp = defs.getDecomApids();

        if (temp != null)
        {
            for (final Integer i : temp)
            {
                apids.add(asLong(i));
            }
        }

        return getApids(defs, apids);
    }


    /**
     * Get the recorded APIDs for EVR.
     *
     * @param defs APID dictionary
     *
     * @return Set of recorded APIDs
     */
    private Set<Long> getEvrApids(final IApidDefinitionProvider defs)
    {
        final Set<Long>    apids = new TreeSet<Long>();
        final Set<Integer> temp  = defs.getEvrApids();

        if (temp != null)
        {
            for (final Integer i : temp)
            {
                apids.add(asLong(i));
            }
        }

        return getApids(defs, apids);
    }


    /**
     * Get the recorded APIDs for CFDP.
     *
     * @param defs APID dictionary
     *             
     *
     * @return Set of recorded APIDs
     */
    private Set<Long> getCfdpApids(final IApidDefinitionProvider defs)
    {
        final Set<Long>    apids = new TreeSet<Long>();
        final Set<Integer> temp  = defs.getCfdpApids();

        if (temp != null)
        {
            for (final Integer i : temp)
            {
                apids.add(asLong(i));
            }
        }

        return getApids(defs, apids);
    }


    /**
     * Get the strategy.
     *
     * @return Strategy choice
     */
    public StrategyEnum getTelemetryMarkingStrategy()
    {
        return _strategy;
    }


    /**
     * Get the unconditional marking value.
     *
     * @return Marking choice
     */
    public RecordedBool getTelemetryUnconditionalMarking()
    {
        return _unconditional;
    }


    /**
     * Get the fallback marking value.
     *
     * @return Marking choice
     */
    public RecordedBool getTelemetryFallbackMarking()
    {
        return _fallback;
    }


    /**
     * Get the recorded VCIDs. May be empty, but never null.
     *
     * @return Set of recorded VCIDs
     */
    public Set<Long> getTelemetryRecordedVcids()
    {
        return _vcids;
    }


    /**
     * Get the recorded EHA APIDs. May be empty, but never null.
     *
     * @return Set of recorded EHA APIDs
     */
    public Set<Long> getTelemetryRecordedEhaApids()
    {
        return _ehaApids;
    }


    /**
     * Get the recorded CFDP APIDs. May be empty, but never null.
     *
     * @return Set of recorded CFDP APIDs
     */
    public Set<Long> getTelemetryRecordedCfdpApids() {
        return _cfdpApids;
    }


    /**
     * Get the recorded EVR APIDs. May be empty, but never null.
     *
     * @return Set of recorded EVR APIDs
     */
    public Set<Long> getTelemetryRecordedEvrApids()
    {
        return _evrApids;
    }


    /**
     * Is this VCID to be R/T or recorded?
     *
     * @param vcid VCID
     *
     * @return Recorded bool state
     */
    private RecordedBool getByVcid(final Long vcid)
    {
        if ((vcid == null) || ! _knownVcids.contains(vcid))
        {
            return _fallback;
        }

        return RecordedBool.valueOf(_vcids.contains(vcid));
    }


    /**
     * Is this APID to be R/T or recorded?
     *
     * @param eha  EHA or EVR
     * @param apid APID
     *
     * @return Recorded bool state
     */
    private RecordedBool getByApid(final EhaBool eha,
                                   final Long    apid)
    {
        if ((apid == null) || ! checkApid(eha, apid))
        {
            return _fallback;
        }

        return RecordedBool.valueOf(
                   (eha.get() ? _ehaApids : _evrApids).contains(apid));
    }


    /**
     * Is this APID/VCID to be R/T or recorded?
     *
     * @param eha EHA or EVR
     * @param apid Packet APID
     * @param vcid data virtual channel ID
     * @param isSse true if this check if for SSE, false for flight
     *
     * @return Recorded bool state
     */
    public RecordedBool getState(final EhaBool eha,
                                 final Integer apid, 
                                 final Integer vcid, 
                                 final boolean isSse)
    {
        return getState(eha,
                        asLong(apid),
                        asLong(vcid),
                        isSse);
    }


    /**
     * Is this APID/VCID to be R/T or recorded?
     *
     * @param eha  EHA or EVR
     * @param apid APID
     * @param vcid VCID
     * @param sse  True if SSE
     *
     * @return Recorded bool state
     */
    public RecordedBool getState(final EhaBool eha,
                                 final Long    apid,
                                 final Long    vcid,
                                 final boolean sse)
    {
        if (sse)
        {
            return SSE_BOOL;
        }

        switch (_strategy)
        {
            case BY_APID:
                return getByApid(eha, apid);

            case BY_VCID:
                return getByVcid(vcid);

            case UNCONDITIONAL:
            default:
                break;
        }

        return _unconditional;
    }


    /**
     * Is this VCID to be R/T or recorded? No APID available.
     *
     * @param vcid VCID
     * @param sse  True if SSE
     *
     * @return Recorded bool state
     */
    public RecordedBool getState(final Long    vcid,
                                 final boolean sse)
    {
        if (sse)
        {
            return SSE_BOOL;
        }

        switch (_strategy)
        {
            case BY_APID:
                return _fallback;

            case BY_VCID:
                return getByVcid(vcid);

            case UNCONDITIONAL:
            default:
                break;
        }

        return _unconditional;
    }


    /**
     * Check APID to see if it exists.
     *
     * @param eha  EHA or EVR
     * @param apid APID to check
     *
     * @return True if an APID of proper type
     */
    private boolean checkApid(final EhaBool eha,
                              final Long    apid)
    {
        if (apid == null)
        {
            return false;
        }

        final IApidDefinition def = _defs.getApidDefinition(asInteger(apid));

        if (def == null)
        {
            // No such APID

            return false;
        }

        final ApidContentType type = def.getContentType();

        if (eha.get())
        {

            return ((type == ApidContentType.PRE_CHANNELIZED) ||
                    (type == ApidContentType.DECOM_FROM_MAP));
        }

        return (type == ApidContentType.EVR);
    }


    /**
     * Convert integer object to long object.
     *
     * @param i Integer object
     *
     * @return Long object
     */
    private static Long asLong(final Integer i)
    {
        return ((i != null) ? Long.valueOf(i.longValue()) : null);
    }


    /**
     * Convert long object to integer object.
     *
     * @param i Long object
     *
     * @return Integer object
     */
    private static Integer asInteger(final Long i)
    {
        return ((i != null) ? Integer.valueOf(i.intValue()) : null);
    }


}
