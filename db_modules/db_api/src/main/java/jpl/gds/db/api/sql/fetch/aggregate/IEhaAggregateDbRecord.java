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

import java.util.Map;


public interface IEhaAggregateDbRecord extends IEhaDbRecord {
	
	public String getChannelIdsString();

	public void setChannelIdsString(String channelIdsString);

	public void setChannelType(String channelType);

	public void setContents(byte[] contents);

	public String getChannelType();

	public byte[] getContents();
	
	public String getPacketInfo();
	
	public void setPacketInfo(String packetInfo);
	
	public void setPacketInfoMap(Map<Long, PacketInfo> packetInfoMap);
	
	public Map<Long, PacketInfo> getPacketInfoMap();
	
	public void setHost(String host);
	
	public String getHost();

}
