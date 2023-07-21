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
package jpl.gds.db.api.sql.fetch;

import java.util.Set;

import jpl.gds.db.api.IDbInteractor;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.IAccurateDateTime;


/**
 * Keeps track of query parameters in order to build where clauses for fetch
 * for Frame.
 *
 */
public class FrameWhereControl extends WhereControl
{
    private static final String SESSIONID_COLUMN    =
                                    IDbInteractor.SESSION_ID;
    private static final String SESSIONHOST_COLUMN  =
                                    IDbInteractor.HOST_ID;
    private static final String TYPE_COLUMN         = "type";
    private static final String ERTEXACT_COLUMN     = "ertCoarse";
    private static final String ERTEXACTFINE_COLUMN = "ertFine";

    /** MPCS-6808 Add RCT */
    private static final String RCTEXACT_COLUMN     = "rctCoarse";
    private static final String RCTEXACTFINE_COLUMN = "rctFine";

    private static final String VCID_COLUMN         = "vcid";
    private static final String DSS_COLUMN          = "dssId";
    private static final String RELAYID_COLUMN      = "relaySpacecraftId";
    private static final String ID_COLUMN           = "id";
    private static final String BADREASON_COLUMN    = "badReason";

    private static final String JOIN_ID_COLUMN      = "id";
    private static final String JOIN_HOST_COLUMN    =
                                    IDbInteractor.HOST_ID;


    /**
     * Constructor FrameWhereControl.
     *
     * @param canned Constant string to incorporate
     */
    public FrameWhereControl(final String canned)
    {
        super(canned);
    }


    /**
     * Add a query for sessionId column.
     *
     * @throws WhereControlException WhereControl exception
     */
    public void addQueryForSessionId() throws WhereControlException
    {
        addJoinQuery(SESSIONID_COLUMN, JOIN_ID_COLUMN);
    }


    /**
     * Add a query for sessionHost column.
     *
     * @throws WhereControlException WhereControl exception
     */
    public void addQueryForSessionHost() throws WhereControlException
    {
        addJoinQuery(SESSIONHOST_COLUMN, JOIN_HOST_COLUMN);
    }


    /**
     * Add query or queries for ertCoarse/Fine column.
     *
     * @param range Time-range
     *
     * @throws WhereControlException WhereControl exception
     */
    public void addQueryForErtCoarseFine(final DatabaseTimeRange range)
        throws WhereControlException
    {
        if (range == null)
        {
            return;
        }

        if (! range.getTimeType().equals(DatabaseTimeType.ERT))
        {
            throw new WhereControlException("Database time type must be ERT at this point");
        }

        IAccurateDateTime temp = range.getStartTime();

        if (temp != null)
        {
        	// MPCS-8104 - Added getNanoseconds due to updated DatabaseTimeRange
            addErtQuery(ERTEXACT_COLUMN,
                        ERTEXACTFINE_COLUMN,
                        temp.getTime(),
                        temp.getNanoseconds(),
                        false);
        }

        temp = range.getStopTime();

        if (temp != null)
        {
        	// MPCS-8104 - Added getNanoseconds due to updated DatabaseTimeRange
            addErtQuery(ERTEXACT_COLUMN,
                        ERTEXACTFINE_COLUMN,
                        temp.getTime(),
                        temp.getNanoseconds(),
                        true);
        }
    }


    /**
     * Add query or queries for rctCoarse/Fine column.
     *
     * @param range Time-range
     *
     * @throws WhereControlException WhereControl exception
     *
     * @version MPCS-6808  Add RCT
     */
    public void addQueryForRctCoarseFine(final DatabaseTimeRange range)
        throws WhereControlException
    {
        if (range == null)
        {
            return;
        }

        if (! range.getTimeType().equals(DatabaseTimeType.RCT))
        {
            throw new WhereControlException("Database time type must be RCT at this point");
        }

        IAccurateDateTime temp = range.getStartTime();

        if (temp != null)
        {
            addRctQuery(RCTEXACT_COLUMN,
                        RCTEXACTFINE_COLUMN,
                        temp.getTime(),
                        false);
        }

        temp = range.getStopTime();

        if (temp != null)
        {
            addRctQuery(RCTEXACT_COLUMN,
                        RCTEXACTFINE_COLUMN,
                        temp.getTime(),
                        true);
        }
    }


    /**
     * Add a query for type column.
     *
     * @param type Type
     *
     * @throws WhereControlException WhereControl exception
     */
    public void addQueryForType(final String type) throws WhereControlException
    {
        if (type != null)
        {
            addSimpleQuery(TYPE_COLUMN, type, true, "=");
        }
    }


    /**
     * Add a query for vcid column.
     *
     * @param vcid VCID
     *
     * @throws WhereControlException WhereControl exception
     */
    public void addQueryForVcid(final Set<Integer> vcid)
        throws WhereControlException
    {
        if ((vcid != null) && ! vcid.isEmpty())
        {
            addVcidQuery(VCID_COLUMN, vcid);
        }
    }


    /**
     * Add a query for dss column.
     *
     * @param dss DSS id
     *
     * @throws WhereControlException WhereControl exception
     */
    public void addQueryForDss(final Set<Integer> dss)
        throws WhereControlException
    {
        if ((dss != null) && ! dss.isEmpty())
        {
            addDssQuery(DSS_COLUMN, dss);
        }
    }


    /**
     * Add a query for relayId column.
     *
     * @param relayId Relay id
     *
     * @throws WhereControlException WhereControl exception
     */
    public void addQueryForRelayId(final Long relayId)
        throws WhereControlException
    {
        if (relayId != null)
        {
            addSimpleQuery(RELAYID_COLUMN, relayId, false, "=");
        }
    }


    /**
     * Add a query for id column.
     *
     * @param id Id
     *
     * @throws WhereControlException WhereControl exception
     */
    public void addQueryForId(final Long id)
        throws WhereControlException
    {
        if (id != null)
        {
            addSimpleQuery(ID_COLUMN, id, false, ">=");
        }
    }


    /**
     * Add a query for good/bad which is actually on the badReason column.
     *
     * @param isGood is-good status
     *
     * @throws WhereControlException WhereControl exception
     */
    public void addQueryForBadReason(final Boolean isGood)
        throws WhereControlException
    {
        if (isGood != null)
        {
            addTrueIsNullQuery(BADREASON_COLUMN, isGood);
        }
    }


    /**
     * Add a query for vcfc column.
     *
     * @param vcfcs VCFCS
     *
     * @throws WhereControlException WhereControl exception
     */
    public void addQueryForVcfc(final VcfcRanges vcfcs)
        throws WhereControlException
    {
        if ((vcfcs != null) && ! vcfcs.isEmpty())
        {
            addVcfcRangesQuery(vcfcs);
        }
    }
}
