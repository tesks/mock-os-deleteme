-- ----------------------------------------------------------
-- Delete all rows from all tables except Host.
-- Truncate is faster than Delete.
-- ----------------------------------------------------------

-- MPCS-10853 - : Deprecate legacy channel value tables in database scripts
TRUNCATE TABLE CfdpIndication;
TRUNCATE TABLE CfdpFileGeneration;
TRUNCATE TABLE CfdpFileUplinkFinished;
TRUNCATE TABLE CfdpRequestReceived;
TRUNCATE TABLE CfdpRequestResult;
TRUNCATE TABLE CfdpPduReceived;
TRUNCATE TABLE CfdpPduSent;
TRUNCATE TABLE ChannelData;
TRUNCATE TABLE CommandMessage;
TRUNCATE TABLE CommandStatus;
TRUNCATE TABLE EndSession;
TRUNCATE TABLE Evr;
TRUNCATE TABLE EvrMetadata;
TRUNCATE TABLE Frame;
TRUNCATE TABLE FrameBody;
TRUNCATE TABLE LogMessage;
TRUNCATE TABLE Packet;
TRUNCATE TABLE PacketBody;
TRUNCATE TABLE Product;
TRUNCATE TABLE Session;
TRUNCATE TABLE SseEvr;
TRUNCATE TABLE SseEvrMetadata;
TRUNCATE TABLE SsePacket;
TRUNCATE TABLE SsePacketBody;
TRUNCATE table ContextConfig;
TRUNCATE table ContextConfigKeyValue;

TRUNCATE TABLE Evr2;
TRUNCATE TABLE Packet2;
TRUNCATE TABLE Product2;
TRUNCATE TABLE SseEvr2;
TRUNCATE TABLE SsePacket2;

TRUNCATE TABLE ChannelAggregate;
TRUNCATE TABLE HeaderChannelAggregate;
TRUNCATE TABLE SseChannelAggregate;
TRUNCATE TABLE MonitorChannelAggregate;
