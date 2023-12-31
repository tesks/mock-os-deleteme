-- ----------------------------------------------------------
-- Compact tables
-- Same as optimize.sql, will omit that file for v5_0_0
-- ----------------------------------------------------------

OPTIMIZE TABLE CfdpIndication;
OPTIMIZE TABLE CfdpFileGeneration;
OPTIMIZE TABLE CfdpFileUplinkFinished;
OPTIMIZE TABLE CfdpRequestReceived;
OPTIMIZE TABLE CfdpRequestResult;
OPTIMIZE TABLE CfdpPduReceived;
OPTIMIZE TABLE CfdpPduSent;
OPTIMIZE TABLE ChannelData;
OPTIMIZE TABLE CommandMessage;
OPTIMIZE TABLE CommandStatus;
OPTIMIZE TABLE EndSession;
OPTIMIZE TABLE Evr;
OPTIMIZE TABLE EvrMetadata;
OPTIMIZE TABLE Frame;
OPTIMIZE TABLE FrameBody;
OPTIMIZE TABLE Host;
OPTIMIZE TABLE LogMessage;
OPTIMIZE TABLE Packet;
OPTIMIZE TABLE PacketBody;
OPTIMIZE TABLE Product;
OPTIMIZE TABLE Session;
OPTIMIZE TABLE SseEvr;
OPTIMIZE TABLE SseEvrMetadata;
OPTIMIZE TABLE SsePacket;
OPTIMIZE TABLE SsePacketBody;
OPTIMIZE table ContextConfig;
OPTIMIZE table ContextConfigKeyValue;
OPTIMIZE TABLE Evr2;
OPTIMIZE TABLE Packet2;
OPTIMIZE TABLE Product2;
OPTIMIZE TABLE SseEvr2;
OPTIMIZE TABLE SsePacket2;
OPTIMIZE TABLE ChannelAggregate;
OPTIMIZE TABLE HeaderChannelAggregate;
OPTIMIZE TABLE SseChannelAggregate;
OPTIMIZE TABLE MonitorChannelAggregate;

ANALYZE TABLE CfdpIndication;
ANALYZE TABLE CfdpFileGeneration;
ANALYZE TABLE CfdpFileUplinkFinished;
ANALYZE TABLE CfdpRequestReceived;
ANALYZE TABLE CfdpRequestResult;
ANALYZE TABLE CfdpPduReceived;
ANALYZE TABLE CfdpPduSent;
ANALYZE TABLE ChannelData;
ANALYZE TABLE CommandMessage;
ANALYZE TABLE CommandStatus;
ANALYZE TABLE EndSession;
ANALYZE TABLE Evr;
ANALYZE TABLE EvrMetadata;
ANALYZE TABLE Frame;
ANALYZE TABLE FrameBody;
ANALYZE TABLE Host;
ANALYZE TABLE LogMessage;
ANALYZE TABLE Packet;
ANALYZE TABLE PacketBody;
ANALYZE TABLE Product;
ANALYZE TABLE Session;
ANALYZE TABLE SseEvr;
ANALYZE TABLE SseEvrMetadata;
ANALYZE TABLE SsePacket;
ANALYZE TABLE SsePacketBody;
ANALYZE table ContextConfig;
ANALYZE table ContextConfigKeyValue;
ANALYZE TABLE Evr2;
ANALYZE TABLE Packet2;
ANALYZE TABLE Product2;
ANALYZE TABLE SseEvr2;
ANALYZE TABLE SsePacket2;
ANALYZE TABLE ChannelAggregate;
ANALYZE TABLE HeaderChannelAggregate;
ANALYZE TABLE SseChannelAggregate;
ANALYZE TABLE MonitorChannelAggregate;