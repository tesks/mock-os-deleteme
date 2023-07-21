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
package jpl.gds.db.api.sql.fetch.aggregate;

import jpl.gds.shared.holders.ApidHolder;
import jpl.gds.shared.holders.HostNameHolder;
import jpl.gds.shared.holders.SessionFragmentHolder;

public class EhaDbRecord implements IEhaDbRecord {
	
	private long sessionId;
	private SessionFragmentHolder sessionFragmentHolder;
	private int hostId;
	private HostNameHolder hostNameHolder;
	private int spacecraftId;
	private Integer vcid;
	private Integer dssId;
	private ApidHolder apidHolder;
	private long rctCoarse;
	private int rctFine;
	
	public void setSessionId(long sessionId) {
		this.sessionId = sessionId;
	}

	public void setSessionFragmentHolder(SessionFragmentHolder sessionFragmentHolder) {
		this.sessionFragmentHolder = sessionFragmentHolder;	
	}

	public void setHostId(int hostId) {
		this.hostId = hostId;
	}

	public void setHostNameHolder(HostNameHolder hostNameHolder) {
		this.hostNameHolder = hostNameHolder;
	}

	public void setSpacecraftId(int spacecraftId) {
		this.spacecraftId = spacecraftId;
	}

	public void setVcid(Integer vcid) {
		this.vcid = vcid;	
	}

	public void setApid(ApidHolder apidHolder) {
		this.apidHolder = apidHolder;
	}

	public void setRctCoarse(long rctCoarse) {
		this.rctCoarse = rctCoarse;
	}

	public void setRctFine(int rctFine) {
		this.rctFine = rctFine;
	}

	public ApidHolder getApidHolder() {
		return apidHolder;
	}

	public void setApidHolder(ApidHolder apidHolder) {
		this.apidHolder = apidHolder;
	}

	public long getSessionId() {
		return sessionId;
	}

	public SessionFragmentHolder getSessionFragmentHolder() {
		return sessionFragmentHolder;
	}

	public int getHostId() {
		return hostId;
	}

	public HostNameHolder getHostNameHolder() {
		return hostNameHolder;
	}

	public int getSpacecraftId() {
		return spacecraftId;
	}

	public Integer getVcid() {
		return vcid;
	}

	public long getRctCoarse() {
		return rctCoarse;
	}

	public int getRctFine() {
		return rctFine;
	}

	@Override
	public void setDssId(Integer dssId) {
		this.dssId = dssId;
		
	}

	@Override
	public Integer getDssId() {
		return dssId;
	}
}

