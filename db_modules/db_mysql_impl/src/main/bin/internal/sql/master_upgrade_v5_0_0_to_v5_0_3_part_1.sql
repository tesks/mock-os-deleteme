-- Master upgrade part 1, Initial plus Host, Session, EndSession
-- The table creation has already been performed.

-- MPCS-10853 : Deprecate legacy channel value tables in database scripts
system echo 'Part 1' `date`

system echo 'Cleaning up' `date`

WARNINGS;

CALL SetUpgradeParameters();

DELETE FROM Host;
DELETE FROM Session;
DELETE FROM EndSession;
DELETE FROM Frame;
DELETE FROM FrameBody;
DELETE FROM Packet;
DELETE FROM PacketBody;
DELETE FROM SsePacket;
DELETE FROM SsePacketBody;
DELETE FROM Evr;
DELETE FROM EvrMetadata;
DELETE FROM SseEvr;
DELETE FROM SseEvrMetadata;
DELETE FROM LogMessage;
DELETE FROM CommandMessage;
DELETE FROM CommandStatus;
DELETE FROM Product;
DELETE FROM ChannelData;

CALL DropTimeIndexes('Frame');
CALL DropTimeIndexes('Packet');
CALL DropTimeIndexes('SsePacket');
CALL DropTimeIndexes('Evr');
CALL DropTimeIndexes('SseEvr');
CALL DropTimeIndexes('LogMessage');
CALL DropTimeIndexes('Product');

DROP INDEX `PRIMARY`  ON Frame;
DROP INDEX `PRIMARY`  ON Packet;
DROP INDEX `PRIMARY`  ON SsePacket;
DROP INDEX comboIndex ON Evr;
DROP INDEX comboIndex ON SseEvr;
DROP INDEX comboIndex ON LogMessage;
DROP INDEX comboIndex ON Product;

system echo 'Upgrading Host table' `date`

CALL IssueByMission('INSERT INTO Host SELECT * FROM mission_ampcs_prev.Host');

-- MPCS-5073
CALL ValidateHostTable();


system echo 'Upgrading Session table' `date`

-- MPCS-5013  Get rid of NEN_SN

CALL IssueByMission(
    'INSERT INTO Session SELECT * FROM mission_ampcs_prev.Session');


system echo 'Upgrading EndSession table' `date`

CALL IssueByMission(
    'INSERT INTO EndSession SELECT * FROM mission_ampcs_prev.EndSession');


system echo 'Upgrading ContextConfig table' `date`

CALL IssueByMission(
    'INSERT INTO ContextConfig SELECT * FROM mission_ampcs_prev.ContextConfig');


system echo 'Upgrading ContextConfigKeyValue table' `date`

CALL IssueByMission(
    'INSERT INTO ContextConfigKeyValue SELECT * FROM mission_ampcs_prev.ContextConfigKeyValue');


system echo 'End part 1' `date`
