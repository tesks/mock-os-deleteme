-- Copy data from regular to extended SCET tables
-- Extended SCET tables are created as part of the crete_extended_scet script

NOWARNING;

-- Populate new tables from old

system echo 'Upgrading Extended SCET tables' `date`

DROP PROCEDURE IF EXISTS PopulateExtended;

DELIMITER //

-- copy from regular tables, same DB version
CREATE PROCEDURE PopulateExtended()
BEGIN
    SELECT 'Extending SCET using data in current version ...' message;
    DELETE FROM Packet2;
    DELETE FROM Evr2;
    DELETE FROM Product2;
    DELETE FROM SseEvr2;
    DELETE FROM SsePacket2;
    INSERT INTO Evr2
    SELECT
        sessionId,
        hostId,
        sessionFragment,
        id,
        packetId,
        name,
        eventId,
        rctCoarse,
        rctFine,
        ertCoarse,
        ertFine,
        scetCoarse,
        scetFine * 1000000,
        sclkCoarse,
        sclkFine,
        dssId,
        vcid,
        level,
        module,
        message,
        isRealtime
        FROM Evr;

    INSERT INTO Packet2
    SELECT
        sessionId,
        hostId,
        sessionFragment,
        id,
        frameId,
        rctCoarse,
        rctFine,
        scetCoarse,
        scetFine * 1000000,
        ertCoarse,
        ertFine,
        sclkCoarse,
        sclkFine,
        dssId,
        vcid,
        sourceVcfc,
        apid,
        spsc,
        apidName,
        badReason,
        fillFlag,
        bodyLength,
        headerLength,
        trailerLength
    FROM Packet;

    -- Avoid having to deal with different versions by mission.
    -- There aren't so many Products to worry about.

    INSERT INTO Product2 SELECT * FROM Product;

    UPDATE Product2 SET dvtScetFine = dvtScetFine * 1000000;

    INSERT INTO SseEvr2
    SELECT
        sessionId,
        hostId,
        sessionFragment,
        id,
        packetId,
        name,
        eventId,
        rctCoarse,
        rctFine,
        ertCoarse,
        ertFine,
        scetCoarse,
        scetFine * 1000000,
        sclkCoarse,
        sclkFine,
        level,
        module,
        message
    FROM SseEvr;

    INSERT INTO SsePacket2
    SELECT
        sessionId,
        hostId,
        sessionFragment,
        id,
        rctCoarse,
        rctFine,
        scetCoarse,
        scetFine * 1000000,
        ertCoarse,
        ertFine,
        sclkCoarse,
        sclkFine,
        apid,
        spsc,
        apidName,
        badReason,
        bodyLength,
        headerLength,
        trailerLength
    FROM SsePacket;

END //

DELIMITER ;

DROP PROCEDURE IF EXISTS CopyExtended;

DELIMITER //

-- copy from extended tables, previous version
CREATE PROCEDURE CopyExtended(oldDb VARCHAR(255))
BEGIN

  SELECT "Copying data over from previous version ... " message;

  DELETE FROM Packet2;
  DELETE FROM Evr2;
  DELETE FROM Product2;
  DELETE FROM SseEvr2;
  DELETE FROM SsePacket2;

  CALL IssueDynamic(CONCAT('INSERT INTO Evr2 SELECT * FROM ', oldDb, '.Evr2'));

  CALL IssueDynamic(CONCAT('INSERT INTO Packet2 SELECT * FROM ', oldDb, '.Packet2'));

  CALL IssueDynamic(CONCAT('INSERT INTO Product2 SELECT * FROM ', oldDb, '.Product2'));

  CALL IssueDynamic(CONCAT('INSERT INTO SseEvr2 SELECT * FROM ', oldDb, '.SseEvr2'));

  CALL IssueDynamic(CONCAT('INSERT INTO SsePacket2 SELECT * FROM ', oldDb, '.SsePacket2'));

END //

DELIMITER ;


DROP FUNCTION IF EXISTS SCET_EXACT_TIME_FROM_COARSE_FINE;


-- From coarse and fine to Java time, for extended SCET
CREATE FUNCTION SCET_EXACT_TIME_FROM_COARSE_FINE(C INT UNSIGNED,
                                                 F INT UNSIGNED)
    RETURNS BIGINT DETERMINISTIC
    RETURN IF(C IS NULL,
              0,
              (C * 1000) + IF(F IS NULL, 0, F DIV 1000000));


-- Detect presence of extended tables

DROP FUNCTION IF EXISTS IS_EXTENDED;

DELIMITER //

CREATE FUNCTION IS_EXTENDED()
RETURNS TINYINT UNSIGNED
DETERMINISTIC READS SQL DATA
BEGIN

    DECLARE _exists TINYINT UNSIGNED DEFAULT 0;

    SELECT COUNT(*) INTO _exists
    FROM information_schema.TABLES
    WHERE ((TABLE_SCHEMA = DATABASE()) AND (TABLE_NAME = 'Evr2'));

    RETURN _exists;

END //

DELIMITER ;

-- Load index caches for 64-bit hosts with 32G or more
DROP PROCEDURE IF EXISTS LoadCache2;

DELIMITER //

CREATE PROCEDURE LoadCache2()
BEGIN

    SET GLOBAL EVR_CACHE.key_buffer_size=0;

    SET GLOBAL EVR_CACHE.key_buffer_size=300*1024*1024;

    SET GLOBAL EVR_CACHE.key_cache_division_limit=70;

    CACHE INDEX Evr2        in EVR_CACHE;
    CACHE INDEX EvrMetadata in EVR_CACHE;

    LOAD INDEX INTO CACHE Evr2        IGNORE LEAVES;
    LOAD INDEX INTO CACHE EvrMetadata IGNORE LEAVES;

    -- Sse

    CACHE INDEX SseEvr2        in EVR_CACHE;
    CACHE INDEX SseEvrMetadata in EVR_CACHE;

    LOAD INDEX INTO CACHE SseEvr2        IGNORE LEAVES;
    LOAD INDEX INTO CACHE SseEvrMetadata IGNORE LEAVES;

    SET GLOBAL FRAME_CACHE.key_buffer_size=0;

    SET GLOBAL FRAME_CACHE.key_buffer_size=2*1024*1024*1024;

    SET GLOBAL FRAME_CACHE.key_cache_division_limit=70;

    CACHE INDEX Frame in FRAME_CACHE;

    LOAD INDEX INTO CACHE Frame IGNORE LEAVES;

    SET GLOBAL PACKET_CACHE.key_buffer_size=0;

    SET GLOBAL PACKET_CACHE.key_buffer_size=1*1024*1024*1024;

    SET GLOBAL PACKET_CACHE.key_cache_division_limit=70;

    CACHE INDEX Packet2 in PACKET_CACHE;

    LOAD INDEX INTO CACHE Packet2 IGNORE LEAVES;

    -- Sse

    CACHE INDEX SsePacket2 in PACKET_CACHE;

    LOAD INDEX INTO CACHE SsePacket2 IGNORE LEAVES;

    SET GLOBAL BODY_CACHE.key_buffer_size=0;

    SET GLOBAL BODY_CACHE.key_buffer_size=64*1024*1024;

    SET GLOBAL BODY_CACHE.key_cache_division_limit=70;

    CACHE INDEX FrameBody     in BODY_CACHE;
    CACHE INDEX PacketBody    in BODY_CACHE;
    CACHE INDEX SsePacketBody in BODY_CACHE;

    LOAD INDEX INTO CACHE FrameBody     IGNORE LEAVES;
    LOAD INDEX INTO CACHE PacketBody    IGNORE LEAVES;
    LOAD INDEX INTO CACHE SsePacketBody IGNORE LEAVES;

    SET GLOBAL key_buffer_size=1*1024*1024*1024;

    SET GLOBAL key_cache_division_limit=70;

    LOAD INDEX INTO CACHE LogMessage IGNORE LEAVES;
    LOAD INDEX INTO CACHE Product2   IGNORE LEAVES;

END //

DELIMITER ;


-- Load index caches for 64-bit hosts with 144G
DROP PROCEDURE IF EXISTS LoadBigCache2;

DELIMITER //

CREATE PROCEDURE LoadBigCache2()
BEGIN

    SET GLOBAL EVR_CACHE.key_buffer_size=0;

    SET GLOBAL EVR_CACHE.key_buffer_size=2*1024*1024*1024;

    SET GLOBAL EVR_CACHE.key_cache_division_limit=70;

    CACHE INDEX Evr2        in EVR_CACHE;
    CACHE INDEX EvrMetadata in EVR_CACHE;

    LOAD INDEX INTO CACHE Evr2        IGNORE LEAVES;
    LOAD INDEX INTO CACHE EvrMetadata IGNORE LEAVES;

-- Sse

    CACHE INDEX SseEvr2        in EVR_CACHE;
    CACHE INDEX SseEvrMetadata in EVR_CACHE;

    LOAD INDEX INTO CACHE SseEvr2        IGNORE LEAVES;
    LOAD INDEX INTO CACHE SseEvrMetadata IGNORE LEAVES;

    SET GLOBAL FRAME_CACHE.key_buffer_size=0;

    SET GLOBAL FRAME_CACHE.key_buffer_size=8*1024*1024*1024;

    SET GLOBAL FRAME_CACHE.key_cache_division_limit=70;

    CACHE INDEX Frame in FRAME_CACHE;

    LOAD INDEX INTO CACHE Frame IGNORE LEAVES;

    SET GLOBAL PACKET_CACHE.key_buffer_size=0;

    SET GLOBAL PACKET_CACHE.key_buffer_size=4*1024*1024*1024;

    SET GLOBAL PACKET_CACHE.key_cache_division_limit=70;

    CACHE INDEX Packet2 in PACKET_CACHE;

    LOAD INDEX INTO CACHE Packet2 IGNORE LEAVES;

-- Sse

    CACHE INDEX SsePacket2 in PACKET_CACHE;

    LOAD INDEX INTO CACHE SsePacket2 IGNORE LEAVES;

    SET GLOBAL BODY_CACHE.key_buffer_size=0;

    SET GLOBAL BODY_CACHE.key_buffer_size=64*1024*1024;

    SET GLOBAL BODY_CACHE.key_cache_division_limit=70;

    CACHE INDEX FrameBody     in BODY_CACHE;
    CACHE INDEX PacketBody    in BODY_CACHE;
    CACHE INDEX SsePacketBody in BODY_CACHE;

    LOAD INDEX INTO CACHE FrameBody     IGNORE LEAVES;
    LOAD INDEX INTO CACHE PacketBody    IGNORE LEAVES;
    LOAD INDEX INTO CACHE SsePacketBody IGNORE LEAVES;

    SET GLOBAL key_buffer_size=4*1024*1024*1024;

    SET GLOBAL key_cache_division_limit=70;

    LOAD INDEX INTO CACHE LogMessage IGNORE LEAVES;
    LOAD INDEX INTO CACHE Product2   IGNORE LEAVES;

END //

DELIMITER ;


-- Load index caches for 64-bit hosts with 24G
DROP PROCEDURE IF EXISTS Load24Cache2;

DELIMITER //

CREATE PROCEDURE Load24Cache2()
BEGIN

    SET GLOBAL EVR_CACHE.key_buffer_size=0;

    SET GLOBAL EVR_CACHE.key_buffer_size=225*1024*1024;

    SET GLOBAL EVR_CACHE.key_cache_division_limit=70;

    CACHE INDEX Evr2        in EVR_CACHE;
    CACHE INDEX EvrMetadata in EVR_CACHE;

    LOAD INDEX INTO CACHE Evr2        IGNORE LEAVES;
    LOAD INDEX INTO CACHE EvrMetadata IGNORE LEAVES;

    -- Sse

    CACHE INDEX SseEvr2        in EVR_CACHE;
    CACHE INDEX SseEvrMetadata in EVR_CACHE;

    LOAD INDEX INTO CACHE SseEvr2        IGNORE LEAVES;
    LOAD INDEX INTO CACHE SseEvrMetadata IGNORE LEAVES;

    SET GLOBAL FRAME_CACHE.key_buffer_size=0;

    SET GLOBAL FRAME_CACHE.key_buffer_size=1536*1024*1024;

    SET GLOBAL FRAME_CACHE.key_cache_division_limit=70;

    CACHE INDEX Frame in FRAME_CACHE;

    LOAD INDEX INTO CACHE Frame IGNORE LEAVES;

    SET GLOBAL PACKET_CACHE.key_buffer_size=0;

    SET GLOBAL PACKET_CACHE.key_buffer_size=768*1024*1024;

    SET GLOBAL PACKET_CACHE.key_cache_division_limit=70;

    CACHE INDEX Packet2 in PACKET_CACHE;

    LOAD INDEX INTO CACHE Packet2 IGNORE LEAVES;

    -- Sse

    CACHE INDEX SsePacket2 in PACKET_CACHE;

    LOAD INDEX INTO CACHE SsePacket2 IGNORE LEAVES;

    SET GLOBAL BODY_CACHE.key_buffer_size=0;

    SET GLOBAL BODY_CACHE.key_buffer_size=49152*1024;

    SET GLOBAL BODY_CACHE.key_cache_division_limit=70;

    CACHE INDEX FrameBody     in BODY_CACHE;
    CACHE INDEX PacketBody    in BODY_CACHE;
    CACHE INDEX SsePacketBody in BODY_CACHE;

    LOAD INDEX INTO CACHE FrameBody     IGNORE LEAVES;
    LOAD INDEX INTO CACHE PacketBody    IGNORE LEAVES;
    LOAD INDEX INTO CACHE SsePacketBody IGNORE LEAVES;

    SET GLOBAL key_buffer_size=768*1024*1024;

    SET GLOBAL key_cache_division_limit=70;

    LOAD INDEX INTO CACHE LogMessage IGNORE LEAVES;
    LOAD INDEX INTO CACHE Product2   IGNORE LEAVES;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexPacket2;

DELIMITER //

CREATE PROCEDURE IndexPacket2()
BEGIN

    IF @extra != 0
    THEN
        ALTER TABLE Packet2
            ADD PRIMARY KEY(hostId, sessionId, sessionFragment, id),
            ADD INDEX ertIndex(ertCoarse, ertFine),
            ADD INDEX scetIndex(scetCoarse, scetFine),
            ADD INDEX sclkIndex(sclkCoarse, sclkFine);
    ELSE
        ALTER TABLE Packet2
            ADD PRIMARY KEY(hostId, sessionId, sessionFragment, id);
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexSsePacket2;

DELIMITER //

CREATE PROCEDURE IndexSsePacket2()
BEGIN

    IF @extra != 0
    THEN
        ALTER TABLE SsePacket2
            ADD PRIMARY KEY(hostId, sessionId, sessionFragment, id),
            ADD INDEX ertIndex(ertCoarse, ertFine),
            ADD INDEX scetIndex(scetCoarse, scetFine),
            ADD INDEX sclkIndex(sclkCoarse, sclkFine);
    ELSE
        ALTER TABLE SsePacket2
            ADD PRIMARY KEY(hostId, sessionId, sessionFragment, id);
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexEvr2;

DELIMITER //

CREATE PROCEDURE IndexEvr2()
BEGIN

    IF @extra != 0
    THEN
        ALTER TABLE Evr2
            ADD INDEX comboIndex(hostId, sessionId),
            ADD INDEX ertIndex(ertCoarse, ertFine),
            ADD INDEX scetIndex(scetCoarse, scetFine),
            ADD INDEX sclkIndex(sclkCoarse, sclkFine);
    ELSE
        ALTER TABLE Evr2
            ADD INDEX comboIndex(hostId, sessionId);
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexSseEvr2;

DELIMITER //

CREATE PROCEDURE IndexSseEvr2()
BEGIN

    IF @extra != 0
    THEN
        ALTER TABLE SseEvr2
            ADD INDEX comboIndex(hostId, sessionId),
            ADD INDEX ertIndex(ertCoarse, ertFine),
            ADD INDEX scetIndex(scetCoarse, scetFine),
            ADD INDEX sclkIndex(sclkCoarse, sclkFine);
    ELSE
        ALTER TABLE SseEvr2
            ADD INDEX comboIndex(hostId, sessionId);
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS IndexProduct2;

DELIMITER //

CREATE PROCEDURE IndexProduct2()
BEGIN

    IF @extra != 0
    THEN
        ALTER TABLE Product2
            ADD INDEX comboIndex(hostId, sessionId),
            ADD INDEX creationTimeIndex(creationTimeCoarse, creationTimeFine),
            ADD INDEX ertIndex(ertCoarse, ertFine),
            ADD INDEX scetIndex(dvtScetCoarse, dvtScetFine),
            ADD INDEX sclkIndex(dvtSclkCoarse, dvtSclkFine);
    ELSE
        ALTER TABLE Product2
            ADD INDEX comboIndex(hostId, sessionId);
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS OptimizeTables2;

DELIMITER //

CREATE PROCEDURE OptimizeTables2()
BEGIN

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
    OPTIMIZE TABLE Evr2;
    OPTIMIZE TABLE EvrMetadata;
    OPTIMIZE TABLE Frame;
    OPTIMIZE TABLE FrameBody;
    OPTIMIZE TABLE Host;
    OPTIMIZE TABLE LogMessage;
    OPTIMIZE TABLE Packet2;
    OPTIMIZE TABLE PacketBody;
    OPTIMIZE TABLE Product2;
    OPTIMIZE TABLE Session;
    OPTIMIZE TABLE SseEvr2;
    OPTIMIZE TABLE SseEvrMetadata;
    OPTIMIZE TABLE SsePacket2;
    OPTIMIZE TABLE SsePacketBody;

    OPTIMIZE TABLE ChannelAggregate;
    OPTIMIZE TABLE HeaderChannelAggregate;
    OPTIMIZE TABLE MonitorChannelAggregate;
    OPTIMIZE TABLE SseChannelAggregate;


END //

DELIMITER ;


DROP PROCEDURE IF EXISTS AnalyzeTables2;

DELIMITER //

CREATE PROCEDURE AnalyzeTables2()
BEGIN

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
    ANALYZE TABLE Evr2;
    ANALYZE TABLE EvrMetadata;
    ANALYZE TABLE Frame;
    ANALYZE TABLE FrameBody;
    ANALYZE TABLE Host;
    ANALYZE TABLE LogMessage;
    ANALYZE TABLE Packet2;
    ANALYZE TABLE PacketBody;
    ANALYZE TABLE Product2;
    ANALYZE TABLE Session;
    ANALYZE TABLE SseEvr2;
    ANALYZE TABLE SseEvrMetadata;
    ANALYZE TABLE SsePacket2;
    ANALYZE TABLE SsePacketBody;

    ANALYZE TABLE ChannelAggregate;
    ANALYZE TABLE HeaderChannelAggregate;
    ANALYZE TABLE MonitorChannelAggregate;
    ANALYZE TABLE SseChannelAggregate;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CopyDatabase2;

DELIMITER //

CREATE PROCEDURE CopyDatabase2(oldDb VARCHAR(256))
BEGIN

    CALL CopyTable(oldDb, 'CfdpIndication');
    CALL CopyTable(oldDb, 'CfdpFileGeneration');
    CALL CopyTable(oldDb, 'CfdpFileUplinkFinished');
    CALL CopyTable(oldDb, 'CfdpRequestReceived');
    CALL CopyTable(oldDb, 'CfdpRequestResult');
    CALL CopyTable(oldDb, 'CfdpPduReceived');
    CALL CopyTable(oldDb, 'CfdpPduSent');
    CALL CopyTable(oldDb, 'ChannelData');
    CALL CopyTable(oldDb, 'CommandMessage');
    CALL CopyTable(oldDb, 'CommandStatus');
    CALL CopyTable(oldDb, 'EndSession');
    CALL CopyTable(oldDb, 'Evr2');
    CALL CopyTable(oldDb, 'EvrMetadata');
    CALL CopyTable(oldDb, 'Frame');
    CALL CopyTable(oldDb, 'FrameBody');
    CALL CopyTable(oldDb, 'Host');
    CALL CopyTable(oldDb, 'LogMessage');
    CALL CopyTable(oldDb, 'Packet2');
    CALL CopyTable(oldDb, 'PacketBody');
    CALL CopyTable(oldDb, 'Product2');
    CALL CopyTable(oldDb, 'Session');
    CALL CopyTable(oldDb, 'SseEvr2');
    CALL CopyTable(oldDb, 'SseEvrMetadata');
    CALL CopyTable(oldDb, 'SsePacket2');
    CALL CopyTable(oldDb, 'SsePacketBody');

    CALL CopyTable(oldDb, 'ChannelAggregate');
    CALL CopyTable(oldDb, 'HeaderChannelAggregate');
    CALL CopyTable(oldDb, 'MonitorChannelAggregate');
    CALL CopyTable(oldDb, 'SseChannelAggregate');

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CheckDatabase2;

DELIMITER //

CREATE PROCEDURE CheckDatabase2()
BEGIN

    DECLARE l_total int unsigned DEFAULT 0;
    DECLARE l_body  int unsigned DEFAULT 0;
    DECLARE l_join  int unsigned DEFAULT 0;

    -- Verify that there is exactly one distinct hostOffset

    SELECT COUNT(DISTINCT hostOffset) FROM Host INTO l_total;

    IF l_total != 1
    THEN
        SELECT CONCAT('Host offset not 1: ', l_total) AS error;
    END IF;

    -- Session versus Host

    SELECT COUNT(*) FROM Session INTO l_total;

    SELECT COUNT(*) FROM Session AS s STRAIGHT_JOIN Host AS h
        ON ((s.hostId=h.hostId) AND (s.host=h.hostName)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Session/Host disconnect: ', l_total - l_join) AS error;
    END IF;

    -- Session versus EndSession

    SELECT COUNT(*) FROM Session AS s STRAIGHT_JOIN EndSession AS e
        ON ((s.sessionId=e.sessionId) AND
            (s.hostId=e.hostId)       AND
            (s.sessionFragment=e.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Session/EndSession disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- ChannelAggregate versus Session

    SELECT SUM(count) FROM ChannelAggregate INTO l_total;

    SELECT SUM(count) FROM ChannelAggregate AS ca STRAIGHT_JOIN Session AS s
        ON ((ca.sessionId=s.sessionId) AND
            (ca.hostId=s.hostId)       AND
            (ca.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('ChannelAggregate/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;


    -- SseChannelAggregate versus Session

    SELECT SUM(count) FROM SseChannelAggregate INTO l_total;

    SELECT SUM(count) FROM SseChannelAggregate AS sca STRAIGHT_JOIN Session AS s
        ON ((sca.sessionId=s.sessionId) AND
            (sca.hostId=s.hostId)       AND
            (sca.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('SseChannelAggregate/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;


    -- MonitorChannelAggregate versus Session

    SELECT SUM(count) FROM MonitorChannelAggregate INTO l_total;

    SELECT SUM(count) FROM MonitorChannelAggregate AS ma STRAIGHT_JOIN Session AS s
        ON ((ma.sessionId=s.sessionId) AND
            (ma.hostId=s.hostId)       AND
            (ma.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('MonitorChannelAggregate/Session disconnect: ',
                      l_total - l_join) AS error;
    END IF;

    -- HeaderChannelAggregate versus Session

    SELECT SUM(count) FROM HeaderChannelAggregate INTO l_total;

    SELECT SUM(count) FROM HeaderChannelAggregate AS ha STRAIGHT_JOIN Session AS s
        ON ((ha.sessionId=s.sessionId) AND
            (ha.hostId=s.hostId)       AND
            (ha.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('HeaderChannelAggregate/Session disconnect: ',
                      l_total - l_join) AS error;
    END IF;

    -- Frame versus Session

    SELECT COUNT(*) FROM Frame     INTO l_total;
    SELECT COUNT(*) FROM FrameBody INTO l_body;

    SELECT COUNT(*) FROM Frame AS f STRAIGHT_JOIN Session AS s
        ON ((f.sessionId=s.sessionId) AND
            (f.hostId=s.hostId)       AND
            (f.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Frame/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- Frame versus FrameBody

    SELECT COUNT(*) FROM Frame AS f STRAIGHT_JOIN FrameBody AS fb
        ON ((f.sessionId       = fb.sessionId)       AND
            (f.hostId          = fb.hostId)          AND
            (f.sessionFragment = fb.sessionFragment) AND
            (f.id              = fb.id)) INTO l_join;

    IF (l_total != l_join)
    THEN
        SELECT CONCAT('Frame disconnect: ', l_total - l_join)
            AS error;
    END IF;

    IF (l_body != l_join)
    THEN
        SELECT CONCAT('FrameBody disconnect: ', l_body - l_join)
            AS error;
    END IF;

    -- Packet2 versus Session

    SELECT COUNT(*) FROM Packet2    INTO l_total;
    SELECT COUNT(*) FROM PacketBody INTO l_body;

    SELECT COUNT(*) FROM Packet2 AS p STRAIGHT_JOIN Session AS s
        ON ((p.sessionId=s.sessionId) AND
            (p.hostId=s.hostId)       AND
            (p.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Packet2/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- Packet2 versus PacketBody

    SELECT COUNT(*) FROM Packet2 AS p STRAIGHT_JOIN PacketBody AS pb
        ON ((p.sessionId       = pb.sessionId)       AND
            (p.hostId          = pb.hostId)          AND
            (p.sessionFragment = pb.sessionFragment) AND
            (p.id              = pb.id)) INTO l_join;

    IF (l_total != l_join)
    THEN
        SELECT CONCAT('Packet2 disconnect: ', l_total - l_join)
            AS error;
    END IF;

    IF (l_body != l_join)
    THEN
        SELECT CONCAT('PacketBody disconnect: ', l_body - l_join)
            AS error;
    END IF;

    -- SsePacket2 versus Session

    SELECT COUNT(*) FROM SsePacket2    INTO l_total;
    SELECT COUNT(*) FROM SsePacketBody INTO l_body;

    SELECT COUNT(*) FROM SsePacket2 AS p STRAIGHT_JOIN Session AS s
        ON ((p.sessionId=s.sessionId) AND
            (p.hostId=s.hostId)       AND
            (p.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('SsePacket2/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- SsePacket2 versus SsePacketBody

    SELECT COUNT(*) FROM SsePacket2 AS p STRAIGHT_JOIN SsePacketBody AS pb
        ON ((p.sessionId       = pb.sessionId)       AND
            (p.hostId          = pb.hostId)          AND
            (p.sessionFragment = pb.sessionFragment) AND
            (p.id              = pb.id)) INTO l_join;

    IF (l_total != l_join)
    THEN
        SELECT CONCAT('SsePacket2 disconnect: ', l_total - l_join)
            AS error;
    END IF;

    IF (l_body != l_join)
    THEN
        SELECT CONCAT('SsePacketBody disconnect: ', l_body - l_join)
            AS error;
    END IF;

    -- Evr2 versus Session

    SELECT COUNT(*) FROM Evr2 INTO l_total;

    SELECT COUNT(*) FROM Evr2 AS e STRAIGHT_JOIN Session AS s
        ON ((e.sessionId=s.sessionId) AND
            (e.hostId=s.hostId)       AND
            (e.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Evr2/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- EvrMetadata versus Evr2

    SELECT COUNT(*) FROM EvrMetadata INTO l_total;

    SELECT COUNT(*) FROM Evr2 AS e STRAIGHT_JOIN EvrMetadata AS em
        ON ((e.sessionId       = em.sessionId)       AND
            (e.hostId          = em.hostId)          AND
            (e.sessionFragment = em.sessionFragment) AND
            (e.id              = em.id)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Evr2/EvrMetadata disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- SseEvr2 versus Session

    SELECT COUNT(*) FROM SseEvr2 INTO l_total;

    SELECT COUNT(*) FROM SseEvr2 AS e STRAIGHT_JOIN Session AS s
        ON ((e.sessionId=s.sessionId) AND
            (e.hostId=s.hostId)       AND
            (e.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('SseEvr2/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- SseEvrMetadata versus SseEvr2

    SELECT COUNT(*) FROM SseEvrMetadata INTO l_total;

    SELECT COUNT(*) FROM SseEvr2 AS e STRAIGHT_JOIN SseEvrMetadata AS em
        ON ((e.sessionId       = em.sessionId)       AND
            (e.hostId          = em.hostId)          AND
            (e.sessionFragment = em.sessionFragment) AND
            (e.id              = em.id)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('SseEvr2/SseEvrMetadata disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- LogMessage versus Session

    SELECT COUNT(*) FROM LogMessage INTO l_total;

    SELECT COUNT(*) FROM LogMessage AS lm STRAIGHT_JOIN Session AS s
        ON ((lm.sessionId=s.sessionId) AND
            (lm.hostId=s.hostId)       AND
            (lm.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('LogMessage/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- CommandMessage versus Session

    SELECT COUNT(*) FROM CommandMessage INTO l_total;

    SELECT COUNT(*) FROM CommandMessage AS cm STRAIGHT_JOIN Session AS s
        ON ((cm.sessionId=s.sessionId) AND
            (cm.hostId=s.hostId)       AND
            (cm.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('CommandMessage/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;

    -- CommandStatus versus CommandMessage

    SELECT COUNT(*) FROM CommandStatus INTO l_total;

    SELECT COUNT(*) FROM CommandMessage AS cm STRAIGHT_JOIN CommandStatus AS cs
        ON ((cm.sessionId       = cs.sessionId)       AND
            (cm.hostId          = cs.hostId)          AND
            (cm.sessionFragment = cs.sessionFragment) AND
            (cm.requestId       = cs.requestId)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('CommandMessage/CommandStatus disconnect: ',
                      l_total - l_join)
            AS error;
    END IF;

    -- Product2 versus Session

    SELECT COUNT(*) FROM Product2 INTO l_total;

    SELECT COUNT(*) FROM Product2 AS p STRAIGHT_JOIN Session AS s
        ON ((p.sessionId=s.sessionId) AND
            (p.hostId=s.hostId)       AND
            (p.sessionFragment=s.sessionFragment)) INTO l_join;

    IF l_total != l_join
    THEN
        SELECT CONCAT('Product2/Session disconnect: ', l_total - l_join)
            AS error;
    END IF;
END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CheckTableCounts2;

DELIMITER //

CREATE PROCEDURE CheckTableCounts2(oldDb VARCHAR(256))
BEGIN

    CALL CheckTableCount('CfdpIndication',      oldDb);
    CALL CheckTableCount('CfdpFileGeneration',  oldDb);
    CALL CheckTableCount('CfdpFileUplinkFinished', oldDb);
    CALL CheckTableCount('CfdpRequestReceived', oldDb);
    CALL CheckTableCount('CfdpRequestResult',   oldDb);
    CALL CheckTableCount('CfdpPduReceived',     oldDb);
    CALL CheckTableCount('CfdpPduSent',         oldDb);
    CALL CheckTableCount('ChannelData',         oldDb);
    CALL CheckTableCount('CommandMessage',      oldDb);
    CALL CheckTableCount('CommandStatus',       oldDb);
    CALL CheckTableCount('EndSession',          oldDb);
    CALL CheckTableCount('Evr2',                oldDb);
    CALL CheckTableCount('EvrMetadata',         oldDb);
    CALL CheckTableCount('Frame',               oldDb);
    CALL CheckTableCount('FrameBody',           oldDb);
    CALL CheckTableCount('Host',                oldDb);
    CALL CheckTableCount('LogMessage',          oldDb);
    CALL CheckTableCount('Packet2',             oldDb);
    CALL CheckTableCount('PacketBody',          oldDb);
    CALL CheckTableCount('Product2',            oldDb);
    CALL CheckTableCount('Session',             oldDb);
    CALL CheckTableCount('SseEvr2',             oldDb);
    CALL CheckTableCount('SseEvrMetadata',      oldDb);
    CALL CheckTableCount('SsePacket2',          oldDb);
    CALL CheckTableCount('SsePacketBody',       oldDb);

    CALL CheckAggregateTableCount('ChannelAggregate',       oldDb);
    CALL CheckAggregateTableCount('HeaderChannelAggregate', oldDb);
    CALL CheckAggregateTableCount('MonitorChannelAggregate',oldDb);
    CALL CheckAggregateTableCount('SseChannelAggregate',    oldDb);

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS AddTimeIndexes2;

DELIMITER //

CREATE PROCEDURE AddTimeIndexes2()
BEGIN

    ALTER TABLE ChannelAggregate
        ADD INDEX beginErtIndex(beginErtCoarse, beginErtFine),
        ADD INDEX endErtIndex(endErtCoarse, endErtFine),
        ADD INDEX beginScetIndex(beginScetCoarse),
        ADD INDEX endScetIndex(endScetCoarse),
        ADD INDEX beginSclkIndex(beginSclkCoarse),
        ADD INDEX endSclkIndex(endSclkCoarse);

    SHOW WARNINGS;

    ALTER TABLE Evr2
        ADD INDEX ertIndex(ertCoarse, ertFine),
        ADD INDEX scetIndex(scetCoarse, scetFine),
        ADD INDEX sclkIndex(sclkCoarse, sclkFine);

    SHOW WARNINGS;

    ALTER TABLE Frame
        ADD INDEX ertIndex(ertCoarse, ertFine);

    SHOW WARNINGS;

    ALTER TABLE HeaderChannelAggregate
        ADD INDEX beginErtIndex(beginErtCoarse, beginErtFine),
        ADD INDEX endErtIndex(endErtCoarse, endErtFine);

    SHOW WARNINGS;

    ALTER TABLE LogMessage
        ADD INDEX eventTimeIndex(eventTimeCoarse, eventTimeFine);

    SHOW WARNINGS;

    ALTER TABLE MonitorChannelAggregate
        ADD INDEX beginMstIndex(beginMstCoarse, beginMstFine),
        ADD INDEX endMstIndex(endMstCoarse, endMstFine);

    SHOW WARNINGS;

    ALTER TABLE Packet2
        ADD INDEX ertIndex(ertCoarse, ertFine),
        ADD INDEX scetIndex(scetCoarse, scetFine),
        ADD INDEX sclkIndex(sclkCoarse, sclkFine);

    SHOW WARNINGS;

    ALTER TABLE Product2
        ADD INDEX creationTimeIndex(creationTimeCoarse, creationTimeFine),
        ADD INDEX ertIndex(ertCoarse, ertFine),
        ADD INDEX scetIndex(dvtScetCoarse, dvtScetFine),
        ADD INDEX sclkIndex(dvtSclkCoarse, dvtSclkFine);

    SHOW WARNINGS;

    ALTER TABLE SseChannelAggregate
        ADD INDEX beginErtIndex(beginErtCoarse, beginErtFine),
        ADD INDEX endErtIndex(endErtCoarse, endErtFine),
        ADD INDEX beginScetIndex(beginScetCoarse),
        ADD INDEX endScetIndex(endScetCoarse),
        ADD INDEX beginSclkIndex(beginSclkCoarse),
        ADD INDEX endSclkIndex(endSclkCoarse);

    SHOW WARNINGS;

    ALTER TABLE SseEvr2
        ADD INDEX ertIndex(ertCoarse, ertFine),
        ADD INDEX scetIndex(scetCoarse, scetFine),
        ADD INDEX sclkIndex(sclkCoarse, sclkFine);

    SHOW WARNINGS;

    ALTER TABLE SsePacket2
        ADD INDEX ertIndex(ertCoarse, ertFine),
        ADD INDEX scetIndex(scetCoarse, scetFine),
        ADD INDEX sclkIndex(sclkCoarse, sclkFine);

    SHOW WARNINGS;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS RemoveTimeIndexes2;

DELIMITER //

CREATE PROCEDURE RemoveTimeIndexes2()
BEGIN
    ALTER TABLE ChannelAggregate
        DROP INDEX beginErtIndex,
        DROP INDEX endErtIndex,
        DROP INDEX beginScetIndex,
        DROP INDEX endScetIndex,
        DROP INDEX beginSclkIndex,
        DROP INDEX endSclkIndex;

    SHOW WARNINGS;

    ALTER TABLE Evr2
        DROP INDEX ertIndex,
        DROP INDEX scetIndex,
        DROP INDEX sclkIndex;

    SHOW WARNINGS;

    ALTER TABLE Frame
        DROP INDEX ertIndex;

    SHOW WARNINGS;

    ALTER TABLE HeaderChannelAggregate
        DROP INDEX beginErtIndex,
        DROP INDEX endErtIndex;

    SHOW WARNINGS;

    ALTER TABLE LogMessage
        DROP INDEX eventTimeIndex;

    SHOW WARNINGS;

    ALTER TABLE MonitorChannelAggregate
        DROP INDEX beginMstIndex,
        DROP INDEX endMstIndex;

    SHOW WARNINGS;

    ALTER TABLE Packet2
        DROP INDEX ertIndex,
        DROP INDEX scetIndex,
        DROP INDEX sclkIndex;

    SHOW WARNINGS;

    ALTER TABLE Product2
        DROP INDEX creationTimeIndex,
        DROP INDEX ertIndex,
        DROP INDEX scetIndex,
        DROP INDEX sclkIndex;

    SHOW WARNINGS;

    ALTER TABLE SseChannelAggregate
        DROP INDEX beginErtIndex,
        DROP INDEX endErtIndex,
        DROP INDEX beginScetIndex,
        DROP INDEX endScetIndex,
        DROP INDEX beginSclkIndex,
        DROP INDEX endSclkIndex;

    SHOW WARNINGS;

    ALTER TABLE SseEvr2
        DROP INDEX ertIndex,
        DROP INDEX scetIndex,
        DROP INDEX sclkIndex;

    SHOW WARNINGS;

    ALTER TABLE SsePacket2
        DROP INDEX ertIndex,
        DROP INDEX scetIndex,
        DROP INDEX sclkIndex;

    SHOW WARNINGS;

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS RemoveSession2;

DELIMITER //

CREATE PROCEDURE RemoveSession2(session   MEDIUMINT UNSIGNED,
                                hostIdent MEDIUMINT UNSIGNED)
BEGIN

    DELETE FROM Session    WHERE (sessionId=session) AND (hostId=hostIdent);
    DELETE FROM EndSession WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM ChannelData WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM CommandMessage WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM CommandStatus WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM Evr2 WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM EvrMetadata WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM Frame WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM FrameBody WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM LogMessage WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM Packet2 WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM PacketBody WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM Product2 WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM SseEvr2 WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM SseEvrMetadata WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM SsePacket2 WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM SsePacketBody WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM ChannelAggregate WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM HeaderChannelAggregate 
        WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM MonitorChannelAggregate 
        WHERE (sessionId=session) AND (hostId=hostIdent);

    DELETE FROM SseChannelAggregate WHERE (sessionId=session) AND (hostId=hostIdent);

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS RemoveSubsession2;

DELIMITER //

CREATE PROCEDURE RemoveSubsession2(session    MEDIUMINT UNSIGNED,
                                   subsession TINYINT   UNSIGNED,
                                   hostIdent  MEDIUMINT UNSIGNED)
BEGIN

    DELETE FROM Session    WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                 (sessionFragment=subsession);
    DELETE FROM EndSession WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                 (sessionFragment=subsession);

    DELETE FROM ChannelData WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                  (sessionFragment=subsession);

    DELETE FROM CommandMessage WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                     (sessionFragment=subsession);

    DELETE FROM CommandStatus WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                    (sessionFragment=subsession);

    DELETE FROM Evr2 WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM EvrMetadata WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                  (sessionFragment=subsession);

    DELETE FROM Frame WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM FrameBody WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM LogMessage WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                 (sessionFragment=subsession);

    DELETE FROM Packet2 WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM PacketBody WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                 (sessionFragment=subsession);

    DELETE FROM Product2 WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM SseEvr2 WHERE (sessionId=session) AND (hostId=hostIdent) AND
                              (sessionFragment=subsession);

    DELETE FROM SseEvrMetadata WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                     (sessionFragment=subsession);

    DELETE FROM SsePacket2 WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM SsePacketBody WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                    (sessionFragment=subsession);

    DELETE FROM ChannelAggregate WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                    (sessionFragment=subsession);
    
    DELETE FROM HeaderChannelAggregate 
        WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM MonitorChannelAggregate 
        WHERE (sessionId=session) AND (hostId=hostIdent) AND (sessionFragment=subsession);

    DELETE FROM SseChannelAggregate WHERE (sessionId=session) AND (hostId=hostIdent) AND
                                       (sessionFragment=subsession);

END //

DELIMITER ;


DROP PROCEDURE IF EXISTS CheckBodyLengths2;

DELIMITER //

CREATE PROCEDURE CheckBodyLengths2()
BEGIN

    SELECT p.sessionId, p.hostId, p.id, p.bodyLength,
           UNCOMPRESSED_LENGTH(pb.body) AS ulength
    FROM Packet2 AS p STRAIGHT_JOIN PacketBody AS pb
    ON
    (
        (p.hostId          = pb.hostId)          AND
        (p.sessionId       = pb.sessionId)       AND
        (p.sessionFragment = pb.sessionFragment) AND
        (p.id              = pb.id)
    )
    WHERE (p.bodyLength != UNCOMPRESSED_LENGTH(pb.body));

    SELECT sp.sessionId, sp.hostId, sp.id, sp.bodyLength,
           UNCOMPRESSED_LENGTH(spb.body) AS ulength
    FROM SsePacket2 AS sp STRAIGHT_JOIN SsePacketBody AS spb
    ON
    (
        (sp.hostId          = spb.hostId)          AND
        (sp.sessionId       = spb.sessionId)       AND
        (sp.sessionFragment = spb.sessionFragment) AND
        (sp.id              = spb.id)
    )
    WHERE (sp.bodyLength != UNCOMPRESSED_LENGTH(spb.body));

    SELECT f.sessionId, f.hostId, f.id, f.bodyLength,
           UNCOMPRESSED_LENGTH(fb.body) AS ulength
    FROM Frame AS f STRAIGHT_JOIN FrameBody AS fb
    ON
    (
        (f.hostId          = fb.hostId)          AND
        (f.sessionId       = fb.sessionId)       AND
        (f.sessionFragment = fb.sessionFragment) AND
        (f.id              = fb.id)
    )
    WHERE (f.bodyLength != UNCOMPRESSED_LENGTH(fb.body));

END //

DELIMITER ;


-- Remove dead links, generally caused by not storing *Packets or Frames.
-- MPCS-7681

DROP PROCEDURE IF EXISTS RemoveDeadLinks2;

DELIMITER //

CREATE PROCEDURE RemoveDeadLinks2()
BEGIN

    UPDATE Packet2 AS p
           LEFT JOIN Frame AS f
           ON
           (
            (f.hostId          = p.hostId)          AND
            (f.sessionId       = p.sessionId)       AND
            (f.sessionFragment = p.sessionFragment) AND
            (f.id              = p.frameId)
           )
    SET p.frameId = NULL
    WHERE ((p.frameId IS NOT NULL) AND (f.id IS NULL));

    UPDATE Evr2 AS e
           LEFT JOIN Packet2 AS p
           ON
           (
            (p.hostId          = e.hostId)          AND
            (p.sessionId       = e.sessionId)       AND
            (p.sessionFragment = e.sessionFragment) AND
            (p.id              = e.packetId)
           )
    SET e.packetId = NULL
    WHERE ((e.packetId IS NOT NULL) AND (p.id IS NULL));

    UPDATE SseEvr2 AS e
           LEFT JOIN SsePacket2 AS p
           ON
           (
            (p.hostId          = e.hostId)          AND
            (p.sessionId       = e.sessionId)       AND
            (p.sessionFragment = e.sessionFragment) AND
            (p.id              = e.packetId)
           )
    SET e.packetId = NULL
    WHERE ((e.packetId IS NOT NULL) AND (p.id IS NULL));

END //

DELIMITER ;

system echo 'Creating Extended SCET Views' `date`

CREATE OR REPLACE ALGORITHM = MERGE VIEW Packet2View(sessionId,
                                                     sessionHost,
                                                     hostId,
                                                     sessionFragment,
                                                     id,
                                                     frameId,
                                                     rct,
                                                     rctCoarse,
                                                     rctFine,
                                                     scet,
                                                     scetCoarse,
                                                     scetFine,
                                                     ert,
                                                     ertCoarse,
                                                     ertFine,
                                                     sclkCoarse,
                                                     sclkFine,
                                                     dssId,
                                                     vcid,
                                                     sourceVcfc,
                                                     apid,
                                                     spsc,
                                                     apidName,
                                                     badReason,
                                                     fillFlag,
                                                     bodyLength,
                                                     headerLength,
                                                     trailerLength) AS
SELECT p.sessionId,
       h.hostName AS sessionHost,
       p.hostId,
       p.sessionFragment,
       p.id,
       p.frameId,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(p.rctCoarse, p.rctFine)) AS rct,
       p.rctCoarse,
       p.rctFine,
       FROM_JAVATIME(
           SCET_EXACT_TIME_FROM_COARSE_FINE(p.scetCoarse, p.scetFine)) AS scet,
       p.scetCoarse,
       p.scetFine,
       FROM_JAVATIME(
           ERT_EXACT_TIME_FROM_COARSE_FINE(p.ertCoarse, p.ertFine)) AS ert,
       p.ertCoarse,
       p.ertFine,
       p.sclkCoarse,
       p.sclkFine,
       p.dssId,
       p.vcid,
       p.sourceVcfc,
       p.apid,
       p.spsc,
       p.apidName,
       p.badReason,
       p.fillFlag,
       p.bodyLength,
       p.headerLength,
       p.trailerLength
FROM Packet2 AS p STRAIGHT_JOIN Host AS h ON (p.hostId = h.hostId);


CREATE OR REPLACE ALGORITHM = MERGE VIEW Packet2FrameView(sessionId,
                                                          sessionHost,
                                                          hostId,
                                                          sessionFragment,
                                                          id,
                                                          frameId,
                                                          rct,
                                                          rctCoarse,
                                                          rctFine,
                                                          scet,
                                                          scetCoarse,
                                                          scetFine,
                                                          ert,
                                                          ertCoarse,
                                                          ertFine,
                                                          sclkCoarse,
                                                          sclkFine,
                                                          dssId,
                                                          vcid,
                                                          sourceVcfc,
                                                          apid,
                                                          spsc,
                                                          apidName,
                                                          badReason,
                                                          fillFlag,
                                                          bodyLength,
                                                          headerLength,
                                                          trailerLength,
                                                          type,
                                                          relaySpacecraftId,
                                                          bitRate,
                                                          frameBadReason,
                                                          fillFrame) AS
SELECT p.sessionId,
       h.hostName AS sessionHost,
       p.hostId,
       p.sessionFragment,
       p.id,
       p.frameId,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(p.rctCoarse, p.rctFine)) AS rct,
       p.rctCoarse,
       p.rctFine,
       FROM_JAVATIME(
           SCET_EXACT_TIME_FROM_COARSE_FINE(p.scetCoarse, p.scetFine)) AS scet,
       p.scetCoarse,
       p.scetFine,
       FROM_JAVATIME(
           ERT_EXACT_TIME_FROM_COARSE_FINE(p.ertCoarse, p.ertFine)) AS ert,
       p.ertCoarse,
       p.ertFine,
       p.sclkCoarse,
       p.sclkFine,
       p.dssId,
       p.vcid,
       p.sourceVcfc,
       p.apid,
       p.spsc,
       p.apidName,
       p.badReason,
       p.fillFlag,
       p.bodyLength,
       p.headerLength,
       p.trailerLength,
       f.type,
       f.relaySpacecraftId,
       f.bitRate,
       f.badReason,
       f.fillFrame
FROM Packet2 AS p
STRAIGHT_JOIN Frame AS f ON ((p.hostId          = f.hostId)          AND
                             (p.sessionId       = f.sessionId)       AND
                             (p.sessionFragment = f.sessionFragment) AND
                             (p.frameId         = f.id))
STRAIGHT_JOIN Host AS h ON (p.hostId = h.hostId);


CREATE OR REPLACE ALGORITHM = MERGE VIEW SsePacket2View(sessionId,
                                                        sessionHost,
                                                        hostId,
                                                        sessionFragment,
                                                        id,
                                                        rct,
                                                        rctCoarse,
                                                        rctFine,
                                                        scet,
                                                        scetCoarse,
                                                        scetFine,
                                                        ert,
                                                        ertCoarse,
                                                        ertFine,
                                                        sclkCoarse,
                                                        sclkFine,
                                                        apid,
                                                        spsc,
                                                        apidName,
                                                        badReason,
                                                        bodyLength,
                                                        headerLength,
                                                        trailerLength) AS
SELECT p.sessionId,
       h.hostName AS sessionHost,
       p.hostId,
       p.sessionFragment,
       p.id,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(p.rctCoarse, p.rctFine)) AS rct,
       p.rctCoarse,
       p.rctFine,
       FROM_JAVATIME(
           SCET_EXACT_TIME_FROM_COARSE_FINE(p.scetCoarse, p.scetFine)) AS scet,
       p.scetCoarse,
       p.scetFine,
       FROM_JAVATIME(
           ERT_EXACT_TIME_FROM_COARSE_FINE(p.ertCoarse, p.ertFine)) AS ert,
       p.ertCoarse,
       p.ertFine,
       p.sclkCoarse,
       p.sclkFine,
       p.apid,
       p.spsc,
       p.apidName,
       p.badReason,
       p.bodyLength,
       p.headerLength,
       p.trailerLength
FROM SsePacket2 AS p STRAIGHT_JOIN Host AS h ON (p.hostId = h.hostId);



CREATE OR REPLACE ALGORITHM = MERGE VIEW Evr2View(sessionId,
                                                  sessionHost,
                                                  hostId,
                                                  sessionFragment,
                                                  id,
                                                  packetId,
                                                  name,
                                                  eventId,
                                                  rct,
                                                  rctCoarse,
                                                  rctFine,
                                                  ert,
                                                  ertCoarse,
                                                  ertFine,
                                                  scet,
                                                  scetCoarse,
                                                  scetFine,
                                                  sclkCoarse,
                                                  sclkFine,
                                                  dssId,
                                                  vcid,
                                                  level,
                                                  module,
                                                  message,
                                                  isRealtime,
                                                  keyword,
                                                  value) AS
SELECT e.sessionId,
       h.hostName AS sessionHost,
       e.hostId,
       e.sessionFragment,
       e.id,
       e.packetId,
       e.name,
       e.eventId,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(e.rctCoarse, e.rctFine)) AS rct,
       e.rctCoarse,
       e.rctFine,
       FROM_JAVATIME(
           ERT_EXACT_TIME_FROM_COARSE_FINE(e.ertCoarse, e.ertFine)) AS ert,
       e.ertCoarse,
       e.ertFine,
       FROM_JAVATIME(
           SCET_EXACT_TIME_FROM_COARSE_FINE(e.scetCoarse, e.scetFine)) AS scet,
       e.scetCoarse,
       e.scetFine,
       e.sclkCoarse,
       e.sclkFine,
       e.dssId,
       e.vcid,
       e.level,
       e.module,
       e.message,
       e.isRealtime,
       em.keyword,
       em.value
FROM Evr2 AS e
STRAIGHT_JOIN Host AS h ON (e.hostId = h.hostId)
LEFT JOIN EvrMetadata AS em ON ((e.hostId          = em.hostId)          AND
                                (e.sessionId       = em.sessionId)       AND
                                (e.sessionFragment = em.sessionFragment) AND
                                (e.id              = em.id));




CREATE OR REPLACE ALGORITHM = MERGE VIEW Evr2Packet2View(sessionId,
                                                         sessionHost,
                                                         hostId,
                                                         sessionFragment,
                                                         id,
                                                         packetId,
                                                         name,
                                                         eventId,
                                                         rct,
                                                         rctCoarse,
                                                         rctFine,
                                                         ert,
                                                         ertCoarse,
                                                         ertFine,
                                                         scet,
                                                         scetCoarse,
                                                         scetFine,
                                                         sclkCoarse,
                                                         sclkFine,
                                                         dssId,
                                                         vcid,
                                                         level,
                                                         module,
                                                         message,
                                                         isRealtime,
                                                         keyword,
                                                         value,
                                                         frameId,
                                                         apid,
                                                         apidName,
                                                         spsc,
                                                         badReason,
                                                         fillFlag,
                                                         sourceVcfc) AS
SELECT e.sessionId,
       h.hostName AS sessionHost,
       e.hostId,
       e.sessionFragment,
       e.id,
       e.packetId,
       e.name,
       e.eventId,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(e.rctCoarse, e.rctFine)) AS rct,
       e.rctCoarse,
       e.rctFine,
       FROM_JAVATIME(
           ERT_EXACT_TIME_FROM_COARSE_FINE(e.ertCoarse, e.ertFine)) AS ert,
       e.ertCoarse,
       e.ertFine,
       FROM_JAVATIME(
           SCET_EXACT_TIME_FROM_COARSE_FINE(e.scetCoarse, e.scetFine)) AS scet,
       e.scetCoarse,
       e.scetFine,
       e.sclkCoarse,
       e.sclkFine,
       e.dssId,
       e.vcid,
       e.level,
       e.module,
       e.message,
       e.isRealtime,
       em.keyword,
       em.value,
       p.frameId,
       p.apid,
       p.apidName,
       p.spsc,
       p.badReason,
       p.fillFlag,
       p.sourceVcfc
FROM Evr2 AS e
STRAIGHT_JOIN Packet2 AS p ON ((e.hostId          = p.hostId)          AND
                               (e.sessionId       = p.sessionId)       AND
                               (e.sessionFragment = p.sessionFragment) AND
                               (e.packetId        = p.id))
STRAIGHT_JOIN Host AS h ON (e.hostId = h.hostId)
LEFT JOIN EvrMetadata AS em ON ((e.hostId          = em.hostId)          AND
                                (e.sessionId       = em.sessionId)       AND
                                (e.sessionFragment = em.sessionFragment) AND
                                (e.id              = em.id));


CREATE OR REPLACE ALGORITHM = MERGE VIEW SseEvr2View(sessionId,
                                                     sessionHost,
                                                     hostId,
                                                     sessionFragment,
                                                     id,
                                                     packetId,
                                                     name,
                                                     eventId,
                                                     rct,
                                                     rctCoarse,
                                                     rctFine,
                                                     ert,
                                                     ertCoarse,
                                                     ertFine,
                                                     scet,
                                                     scetCoarse,
                                                     scetFine,
                                                     sclkCoarse,
                                                     sclkFine,
                                                     level,
                                                     module,
                                                     message,
                                                     keyword,
                                                     value) AS
SELECT e.sessionId,
       h.hostName AS sessionHost,
       e.hostId,
       e.sessionFragment,
       e.id,
       e.packetId,
       e.name,
       e.eventId,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(e.rctCoarse, e.rctFine)) AS rct,
       e.rctCoarse,
       e.rctFine,
       FROM_JAVATIME(
           ERT_EXACT_TIME_FROM_COARSE_FINE(e.ertCoarse, e.ertFine)) AS ert,
       e.ertCoarse,
       e.ertFine,
       FROM_JAVATIME(
           SCET_EXACT_TIME_FROM_COARSE_FINE(e.scetCoarse, e.scetFine)) AS scet,
       e.scetCoarse,
       e.scetFine,
       e.sclkCoarse,
       e.sclkFine,
       e.level,
       e.module,
       e.message,
       em.keyword,
       em.value
FROM SseEvr2 AS e
STRAIGHT_JOIN Host AS h ON (e.hostId = h.hostId)
LEFT JOIN SseEvrMetadata AS em ON ((e.hostId          = em.hostId)          AND
                                   (e.sessionId       = em.sessionId)       AND
                                   (e.sessionFragment = em.sessionFragment) AND
                                   (e.id              = em.id));


CREATE OR REPLACE ALGORITHM = MERGE VIEW SseEvr2SsePacket2View(sessionId,
                                                               sessionHost,
                                                               hostId,
                                                               sessionFragment,
                                                               id,
                                                               packetId,
                                                               name,
                                                               eventId,
                                                               rct,
                                                               rctCoarse,
                                                               rctFine,
                                                               ert,
                                                               ertCoarse,
                                                               ertFine,
                                                               scet,
                                                               scetCoarse,
                                                               scetFine,
                                                               sclkCoarse,
                                                               sclkFine,
                                                               level,
                                                               module,
                                                               message,
                                                               keyword,
                                                               value,
                                                               apid,
                                                               apidName,
                                                               spsc,
                                                               badReason) AS
SELECT e.sessionId,
       h.hostName AS sessionHost,
       e.hostId,
       e.sessionFragment,
       e.id,
       e.packetId,
       e.name,
       e.eventId,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(e.rctCoarse, e.rctFine)) AS rct,
       e.rctCoarse,
       e.rctFine,
       FROM_JAVATIME(
           ERT_EXACT_TIME_FROM_COARSE_FINE(e.ertCoarse, e.ertFine)) AS ert,
       e.ertCoarse,
       e.ertFine,
       FROM_JAVATIME(
           SCET_EXACT_TIME_FROM_COARSE_FINE(e.scetCoarse, e.scetFine)) AS scet,
       e.scetCoarse,
       e.scetFine,
       e.sclkCoarse,
       e.sclkFine,
       e.level,
       e.module,
       e.message,
       em.keyword,
       em.value,
       p.apid,
       p.apidName,
       p.spsc,
       p.badReason
FROM SseEvr2 AS e
STRAIGHT_JOIN SsePacket2 AS p ON ((e.hostId          = p.hostId)          AND
                                  (e.sessionId       = p.sessionId)       AND
                                  (e.sessionFragment = p.sessionFragment) AND
                                  (e.packetId        = p.id))
STRAIGHT_JOIN Host AS h ON (e.hostId = h.hostId)
LEFT JOIN SseEvrMetadata AS em ON ((e.hostId          = em.hostId)          AND
                                   (e.sessionId       = em.sessionId)       AND
                                   (e.sessionFragment = em.sessionFragment) AND
                                   (e.id              = em.id));

WARNINGS;
