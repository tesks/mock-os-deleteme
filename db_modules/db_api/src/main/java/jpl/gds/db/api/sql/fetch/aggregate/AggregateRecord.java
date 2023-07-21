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


public class AggregateRecord extends EhaDbRecord implements IEhaAggregateDbRecord {

	private String channelType;
	private byte[] contents;
	private String channelIdsString;
	private String packetInfo;
	private Map<Long, PacketInfo> packetInfoMap;
	private String host; 

	public String getPacketInfo() {
		return packetInfo;
	}

	public void setPacketInfo(String packetInfo) {
		this.packetInfo = packetInfo;
	}

	public String getChannelIdsString() {
		return channelIdsString;
	}

	public void setChannelIdsString(String channelIdsString) {
		this.channelIdsString = channelIdsString;
	}

	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	public void setContents(byte[] contents) {
		this.contents = contents;
	}

	public String getChannelType() {
		return channelType;
	}

	public byte[] getContents() {
		return contents;
	}

	public void setPacketInfoMap(Map<Long, PacketInfo> packetInfoMap) {
		this.packetInfoMap = packetInfoMap;
	}
	
	public Map<Long, PacketInfo> getPacketInfoMap() {
		return packetInfoMap;
	}

	public void setHost(String host) {
		this.host = host;
	}
	
	public String getHost() {
		return host;
	}
}