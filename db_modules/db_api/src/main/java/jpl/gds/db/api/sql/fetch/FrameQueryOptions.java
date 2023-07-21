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

import java.util.HashSet;
import java.util.Set;


/**
 * Keeps track of query parameters. Just a simple holder for all types.
 * Null always means "do not query by that parameter."
 *
 * Some of these may be used by more than one DB table.
 *
 */
public class FrameQueryOptions extends Object implements IFrameQueryOptionsUpdater
{
    private String       _frameType = null;
    private Set<Integer> _vcid      = null;
    private Set<Integer> _dss       = null;
    private Long         _relayId   = null;
    private Long         _id        = null;
    private Boolean      _good      = null;
    private VcfcRanges   _vcfcs     = null;


    /**
     * Constructor FrameQueryOptions.
     */
    public FrameQueryOptions()
    {
        super();
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsUpdater#setFrameType(java.lang.String)
	 */
    @Override
	public void setFrameType(final String frameType)
    {
        _frameType = frameType;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsProvider#getFrameType()
	 */
    @Override
	public String getFrameType()
    {
        return _frameType;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsUpdater#setVcid(java.util.Set)
	 */
    @Override
	public void setVcid(final Set<Integer> vcid)
    {
        _vcid = ((vcid != null) && ! vcid.isEmpty()) ? vcid : null;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsUpdater#setVcid(int)
	 */
    @Override
	public void setVcid(final int vcid)
    {
        if (_vcid == null)
        {
            _vcid = new HashSet<Integer>(1);
        }
        else
        {
            _vcid.clear();
        }

        _vcid.add(vcid);
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsProvider#getVcid()
	 */
    @Override
	public Set<Integer> getVcid()
    {
        return _vcid;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsUpdater#setDss(java.util.Set)
	 */
    @Override
	public void setDss(final Set<Integer> dss)
    {
        _dss = ((dss != null) && ! dss.isEmpty()) ? dss : null;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsProvider#getDss()
	 */
    @Override
	public Set<Integer> getDss()
    {
        return _dss;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsUpdater#setRelayId(java.lang.Long)
	 */
    @Override
	public void setRelayId(final Long relayId)
    {
        _relayId = relayId;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsProvider#getRelayId()
	 */
    @Override
	public Long getRelayId()
    {
        return _relayId;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsUpdater#setFrameId(java.lang.Long)
	 */
    @Override
	public void setFrameId(final Long id)
    {
        _id = id;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsProvider#getFrameId()
	 */
    @Override
	public Long getFrameId()
    {
        return _id;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsUpdater#setGood(java.lang.Boolean)
	 */
    @Override
	public void setGood(final Boolean good)
    {
        _good = good;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsProvider#getGood()
	 */
    @Override
	public Boolean getGood()
    {
        return _good;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsUpdater#setVcfcs(jpl.gds.db.api.sql.fetch.VcfcRanges)
	 */
    @Override
	public void setVcfcs(final VcfcRanges vcfcs)
    {
        _vcfcs = vcfcs;
    }


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IFrameQueryOptionsProvider#getVcfcs()
	 */
    @Override
	public VcfcRanges getVcfcs()
    {
        return _vcfcs;
    }
}
