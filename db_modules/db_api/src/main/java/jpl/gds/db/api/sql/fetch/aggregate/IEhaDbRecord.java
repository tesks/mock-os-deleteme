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

public interface IEhaDbRecord {
	
	public void setSessionId(long sessionId);

	public void setSessionFragmentHolder(SessionFragmentHolder sessionFragmentHolder);

	public void setHostId(int hostId);

	public void setHostNameHolder(HostNameHolder hostNameHolder);

	public void setSpacecraftId(int spacecraftId);

	public void setVcid(Integer vcid);
	
	public void setDssId(Integer dssId);

	public void setApid(ApidHolder apidHolder);

	public void setRctCoarse(long rctCoarse);

	public void setRctFine(int rctFine);

	public ApidHolder getApidHolder();

	public void setApidHolder(ApidHolder apidHolder);

	public long getSessionId();

	public SessionFragmentHolder getSessionFragmentHolder();

	public int getHostId();

	public HostNameHolder getHostNameHolder();

	public int getSpacecraftId();

	public Integer getVcid();
	
	public Integer getDssId();

	public long getRctCoarse();

	public int getRctFine();
}

