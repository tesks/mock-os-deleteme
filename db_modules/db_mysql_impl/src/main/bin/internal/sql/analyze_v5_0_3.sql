-- ----------------------------------------------------------
-- Analyze tables following bulk delete
-- ----------------------------------------------------------

analyze table CfdpIndication;
analyze table CfdpFileGeneration;
analyze table CfdpFileUplinkFinished;
analyze table CfdpRequestReceived;
analyze table CfdpRequestResult;
analyze table CfdpPduReceived;
analyze table CfdpPduSent;
analyze table ChannelData;
analyze table CommandMessage;
analyze table CommandStatus;
analyze table EndSession;
analyze table Evr;
analyze table EvrMetadata;
analyze table Frame;
analyze table FrameBody;
analyze table Host;
analyze table LogMessage;
analyze table Packet;
analyze table PacketBody;
analyze table Product;
analyze table Session;
analyze table SseEvr;
analyze table SseEvrMetadata;
analyze table SsePacket;
analyze table SsePacketBody;
analyze table ContextConfig;
analyze table ContextConfigKeyValue;
analyze table Evr2;
analyze table Packet2;
analyze table Product2;
analyze table SseEvr2;
analyze table SsePacket2;
analyze table ChannelAggregate;
analyze table HeaderChannelAggregate;
analyze table SseChannelAggregate;
analyze table MonitorChannelAggregate;

-- flush tables;

show table status;
