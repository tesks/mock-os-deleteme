-- MPCS-9105 - Create extended SCET tables by default
-- this needs to run after the mission specific modifications (e.g more fields in Product)

-- MPCS-10853 -  : Deprecate legacy channel value tables in database scripts
system echo 'Creating Extended SCET tables' `date`
CREATE TABLE Evr2                LIKE Evr;
CREATE TABLE Packet2             LIKE Packet;
CREATE TABLE Product2            LIKE Product;
CREATE TABLE SseEvr2             LIKE SseEvr;
CREATE TABLE SsePacket2          LIKE SsePacket;
ALTER TABLE Evr2             MODIFY COLUMN scetFine    INT UNSIGNED NOT NULL;
ALTER TABLE Packet2          MODIFY COLUMN scetFine    INT UNSIGNED NOT NULL;
ALTER TABLE Product2         MODIFY COLUMN dvtScetFine INT UNSIGNED NOT NULL;
ALTER TABLE SseEvr2          MODIFY COLUMN scetFine    INT UNSIGNED NOT NULL;
ALTER TABLE SsePacket2       MODIFY COLUMN scetFine    INT UNSIGNED NOT NULL;

-- MPCS-10584 - Create Extended SCET Views

-- From coarse and fine to Java time, for extended SCET
CREATE FUNCTION SCET_EXACT_TIME_FROM_COARSE_FINE(C INT UNSIGNED,
                                                 F INT UNSIGNED)
       RETURNS BIGINT DETERMINISTIC
RETURN IF(C IS NULL,
          0,
          (C * 1000) + IF(F IS NULL, 0, F DIV 1000000));

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

/* 
MPCS-10853 - Deprecate legacy channel value tables in database scripts
Removed the following Views:
    ChannelValue2View
    ChannelValue2Packet2View
    SseChannelValue2View
    SseChannelValue2SsePacket2View
    HeaderChannelValue2Packet2View
    HeaderChannelValue2SsePacket2View
    HeaderChannelValue2View
    HeaderChannelValue2FrameView
*/
