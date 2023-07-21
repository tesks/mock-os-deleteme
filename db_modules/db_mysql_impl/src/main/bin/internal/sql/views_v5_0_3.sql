
CREATE OR REPLACE ALGORITHM = MERGE VIEW PacketView(sessionId,
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
           EXACT_TIME_FROM_COARSE_FINE(p.scetCoarse, p.scetFine)) AS scet,
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
FROM Packet AS p STRAIGHT_JOIN Host AS h ON (p.hostId = h.hostId);


CREATE OR REPLACE ALGORITHM = MERGE VIEW PacketFrameView(sessionId,
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
           EXACT_TIME_FROM_COARSE_FINE(p.scetCoarse, p.scetFine)) AS scet,
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
FROM Packet AS p
STRAIGHT_JOIN Frame AS f ON ((p.hostId          = f.hostId)          AND
                             (p.sessionId       = f.sessionId)       AND
                             (p.sessionFragment = f.sessionFragment) AND
                             (p.frameId         = f.id))
STRAIGHT_JOIN Host AS h ON (p.hostId = h.hostId);


CREATE OR REPLACE ALGORITHM = MERGE VIEW SsePacketView(sessionId,
                                                       sessionHost,
                                                       hostId,
                                                       sesionFragment,
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
           EXACT_TIME_FROM_COARSE_FINE(p.scetCoarse, p.scetFine)) AS scet,
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
FROM SsePacket AS p STRAIGHT_JOIN Host AS h ON (p.hostId = h.hostId);



CREATE OR REPLACE ALGORITHM = MERGE VIEW FrameView(sessionId,
                                                   sessionHost,
                                                   hostId,
                                                   sessionFragment,
                                                   id,
                                                   type,
                                                   rct,
                                                   rctCoarse,
                                                   rctFine,
                                                   ert,
                                                   ertCoarse,
                                                   ertFine,
                                                   relaySpacecraftId,
                                                   dssId,
                                                   vcid,
                                                   vcfc,
                                                   bitRate,
                                                   badReason,
                                                   fillFrame,
                                                   bodyLength,
                                                   headerLength,
                                                   trailerLength) AS
SELECT f.sessionId,
       h.hostName AS sessionHost,
       f.hostId,
       f.sessionFragment,
       f.id,
       f.type,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(f.rctCoarse, f.rctFine)) AS rct,
       f.rctCoarse,
       f.rctFine,
       FROM_JAVATIME(
           ERT_EXACT_TIME_FROM_COARSE_FINE(f.ertCoarse, f.ertFine)) AS ert,
       f.ertCoarse,
       f.ertFine,
       f.relaySpacecraftId,
       f.dssId,
       f.vcid,
       f.vcfc,
       f.bitRate,
       f.badReason,
       f.fillFrame,
       f.bodyLength,
       f.headerLength,
       f.trailerLength
FROM Frame AS f STRAIGHT_JOIN Host AS h ON (f.hostId = h.hostId);

/* 
MPCS-10853 - Deprecate legacy channel value tables in database scripts
Removed the following Views:
    ChannelValueView
    ChannelValuePacketView
    SseChannelValueView
    SseChannelValueSsePacketView
    HeaderChannelValuePacketView
    HeaderChannelValueSsePacketView
    HeaderChannelValueView
    HeaderChannelValueFrameView
*/


CREATE OR REPLACE ALGORITHM = MERGE VIEW EvrView(sessionId,
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
           EXACT_TIME_FROM_COARSE_FINE(e.scetCoarse, e.scetFine)) AS scet,
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
FROM Evr AS e
STRAIGHT_JOIN Host AS h ON (e.hostId = h.hostId)
LEFT JOIN EvrMetadata AS em ON ((e.hostId          = em.hostId)          AND
                                (e.sessionId       = em.sessionId)       AND
                                (e.sessionFragment = em.sessionFragment) AND
                                (e.id              = em.id));




CREATE OR REPLACE ALGORITHM = MERGE VIEW EvrPacketView(sessionId,
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
           EXACT_TIME_FROM_COARSE_FINE(e.scetCoarse, e.scetFine)) AS scet,
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
FROM Evr AS e
STRAIGHT_JOIN Packet AS p ON ((e.hostId          = p.hostId)          AND
                              (e.sessionId       = p.sessionId)       AND
                              (e.sessionFragment = p.sessionFragment) AND
                              (e.packetId        = p.id))
STRAIGHT_JOIN Host AS h ON (e.hostId = h.hostId)
LEFT JOIN EvrMetadata AS em ON ((e.hostId          = em.hostId)          AND
                                (e.sessionId       = em.sessionId)       AND
                                (e.sessionFragment = em.sessionFragment) AND
                                (e.id              = em.id));


CREATE OR REPLACE ALGORITHM = MERGE VIEW SseEvrView(sessionId,
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
           EXACT_TIME_FROM_COARSE_FINE(e.scetCoarse, e.scetFine)) AS scet,
       e.scetCoarse,
       e.scetFine,
       e.sclkCoarse,
       e.sclkFine,
       e.level,
       e.module,
       e.message,
       em.keyword,
       em.value
FROM SseEvr AS e
STRAIGHT_JOIN Host AS h ON (e.hostId = h.hostId)
LEFT JOIN SseEvrMetadata AS em ON ((e.hostId          = em.hostId)          AND
                                   (e.sessionId       = em.sessionId)       AND
                                   (e.sessionFragment = em.sessionFragment) AND
                                   (e.id              = em.id));


CREATE OR REPLACE ALGORITHM = MERGE VIEW SseEvrSsePacketView(sessionId,
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
           EXACT_TIME_FROM_COARSE_FINE(e.scetCoarse, e.scetFine)) AS scet,
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
FROM SseEvr AS e
STRAIGHT_JOIN SsePacket AS p ON ((e.hostId          = p.hostId)          AND
                                 (e.sessionId       = p.sessionId)       AND
                                 (e.sessionFragment = p.sessionFragment) AND
                                 (e.packetId        = p.id))
STRAIGHT_JOIN Host AS h ON (e.hostId = h.hostId)
LEFT JOIN SseEvrMetadata AS em ON ((e.hostId          = em.hostId)          AND
                                   (e.sessionId       = em.sessionId)       AND
                                   (e.sessionFragment = em.sessionFragment) AND
                                   (e.id              = em.id));


CREATE OR REPLACE ALGORITHM = MERGE VIEW LogMessageView(sessionId,
                                                        sessionHost,
                                                        hostId,
                                                        sessionFragment,
                                                        rct,
                                                        rctCoarse,
                                                        rctFine,
                                                        eventTime,
                                                        eventTimeCoarse,
                                                        eventTimeFine,
                                                        classification,
                                                        message,
                                                        type) AS
SELECT lm.sessionId,
       h.hostName AS sessionHost,
       lm.hostId,
       lm.sessionFragment,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(lm.rctCoarse, lm.rctFine)) AS rct,
       lm.rctCoarse,
       lm.rctFine,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(lm.eventTimeCoarse, lm.eventTimeFine))
           AS eventTime,
       lm.eventTimeCoarse,
       lm.eventTimeFine,
       lm.classification,
       lm.message,
       lm.type
FROM LogMessage AS lm STRAIGHT_JOIN Host AS h ON (lm.hostId = h.hostId);


CREATE OR REPLACE ALGORITHM = MERGE VIEW CommandMessageView(sessionId,
                                                            sessionHost,
                                                            hostId,
                                                            sessionFragment,
                                                            requestId,
                                                            message,
                                                            type,
                                                            originalFile,
                                                            scmfFile,
                                                            commandedSide,
                                                            finalized,
                                                            rct,
                                                            rctCoarse,
                                                            rctFine,
                                                            eventTime,
                                                            eventTimeCoarse,
                                                            eventTimeFine,
                                                            status,
                                                            failReason,
                                                            checksum,
                                                            totalCltus,
                                                            dssId,
                                                            bit1RadTime,
                                                            bit1RadTimeCoarse,
                                                            bit1RadTimeFine,
                                                            lastBitRadTime,
                                                            lastBitRadTimeCoarse,
                                                            lastBitRadTimeFine,
                                                            final) AS
SELECT cm.sessionId,
       h.hostName AS sessionHost,
       cm.hostId,
       cm.sessionFragment,
       cm.requestId,
       cm.message,
       cm.type,
       cm.originalFile,
       cm.scmfFile,
       cm.commandedSide,
       cm.finalized,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(cs.rctCoarse, cs.rctFine)) AS rct,
       cs.rctCoarse,
       cs.rctFine,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(cs.eventTimeCoarse, cs.eventTimeFine))
           AS eventTime,
       cs.eventTimeCoarse,
       cs.eventTimeFine,
       cs.status,
       cs.failReason,
       cm.checksum,
       cm.totalCltus,
       cs.dssId,
       FROM_JAVATIME(EXACT_TIME_FROM_COARSE_FINE(cs.bit1RadTimeCoarse,
                                                 cs.bit1RadTimeFine))
           AS bit1RadTime,
       cs.bit1RadTimeCoarse,
       cs.bit1RadTimeFine,
       FROM_JAVATIME(EXACT_TIME_FROM_COARSE_FINE(cs.lastBitRadTimeCoarse,
                                                 cs.lastBitRadTimeFine))
           AS lastBitRadTime,
       cs.lastBitRadTimeCoarse,
       cs.lastBitRadTimeFine,
       cs.final
FROM CommandMessage AS cm
STRAIGHT_JOIN CommandStatus AS cs ON ((cm.hostId          = cs.hostId)          AND
                                      (cm.sessionId       = cs.sessionId)       AND
                                      (cm.sessionFragment = cs.sessionFragment) AND
                                      (cm.requestId       = cs.requestId))
STRAIGHT_JOIN Host AS h ON (cm.hostId = h.hostId);


CREATE OR REPLACE ALGORITHM = MERGE VIEW SessionView(sessionHost,
                                                     hostId,
                                                     sessionId,
                                                     sessionFragment,
                                                     name,
                                                     type,
                                                     description,
                                                     fullName,
                                                     user,
                                                     host,
                                                     outputDirectory,
                                                     outputDirectoryOverride,
                                                     fswDictionaryDir,
                                                     sseDictionaryDir,
                                                     sseVersion,
                                                     fswVersion,
                                                     venueType,
                                                     testbedName,
                                                     rawInputType,
                                                     startTime,
                                                     startTimeDerived,
                                                     startTimeCoarse,
                                                     startTimeFine,
                                                     endTime,
                                                     endTimeDerived,
                                                     endTimeCoarse,
                                                     endTimeFine,
                                                     spacecraftId,
                                                     downlinkStreamId,
                                                     mpcsVersion,
                                                     fswDownlinkHost,
                                                     fswUplinkHost,
                                                     fswUplinkPort,
                                                     fswDownlinkPort,
                                                     sseHost,
                                                     sseUplinkPort,
                                                     sseDownlinkPort,
                                                     inputFile,
                                                     downlinkConnectionType,
                                                     uplinkConnectionType,
                                                     topic,
                                                     subtopic,
                                                     dssId,
                                                     vcid,
                                                     fswDownlinkFlag,
                                                     sseDownlinkFlag,
                                                     uplinkFlag,
                                                     databaseSessionId,
                                                     databaseHost) AS
SELECT
       h.hostName AS sessionHost,
       s.hostId,
       s.sessionId,
       s.sessionFragment,
       s.name,
       s.type,
       s.description,
       s.fullName,
       s.user,
       s.host,
       s.outputDirectory,
       s.outputDirectoryOverride,
       s.fswDictionaryDir,
       s.sseDictionaryDir,
       s.sseVersion,
       s.fswVersion,
       s.venueType,
       s.testbedName,
       s.rawInputType,
       s.startTime,
       FROM_JAVATIME(EXACT_TIME_FROM_COARSE_FINE(s.startTimeCoarse,
                                                 s.startTimeFine)),
       s.startTimeCoarse,
       s.startTimeFine,
       es.endTime,
       FROM_JAVATIME(EXACT_TIME_FROM_COARSE_FINE(es.endTimeCoarse,
                                                 es.endTimeFine)),
       es.endTimeCoarse,
       es.endTimeFine,
       s.spacecraftId,
       s.downlinkStreamId,
       s.mpcsVersion,
       s.fswDownlinkHost,
       s.fswUplinkHost,
       s.fswUplinkPort,
       s.fswDownlinkPort,
       s.sseHost,
       s.sseUplinkPort,
       s.sseDownlinkPort,
       s.inputFile,
       s.downlinkConnectionType,
       s.uplinkConnectionType,
       s.topic,
       s.subtopic,
       s.dssId,
       s.vcid,
       s.fswDownlinkFlag,
       s.sseDownlinkFlag,
       s.uplinkFlag,
       s.databaseSessionId,
       s.databaseHost
FROM Session AS s
STRAIGHT_JOIN Host AS h ON (s.hostId = h.hostId)
LEFT JOIN EndSession AS es ON ((s.hostId          = es.hostId)    AND
                               (s.sessionId       = es.sessionId) AND
                               (s.sessionFragment = es.sessionFragment));


CREATE OR REPLACE ALGORITHM = MERGE VIEW ProductView(sessionId,
                                                     sessionHost,
                                                     hostId,
                                                     sessionFragment,
                                                     rct,
                                                     rctCoarse,
                                                     rctFine,
                                                     creationTime,
                                                     creationTimeCoarse,
                                                     creationTimeFine,
                                                     dvtScet,
                                                     dvtScetCoarse,
                                                     dvtScetFine,
                                                     vcid,
                                                     isPartial,
                                                     apid,
                                                     apidName,
                                                     sequenceId,
                                                     sequenceVersion,
                                                     commandNumber,
                                                     xmlVersion,
                                                     totalParts,
                                                     dvtSclkCoarse,
                                                     dvtSclkFine,
                                                     fullPath,
                                                     fileName,
                                                     ert,
                                                     ertCoarse,
                                                     ertFine,
                                                     groundStatus,
                                                     sequenceCategory,
                                                     sequenceNumber,
                                                     version,
                                                     checksum,
                                                     cfdpTransactionId,
                                                     fileSize) AS
SELECT p.sessionId,
       h.hostName AS sessionHost,
       p.hostId,
       p.sessionFragment,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(p.rctCoarse, p.rctFine)) AS rct,
       p.rctCoarse,
       p.rctFine,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(p.creationTimeCoarse, p.creationTimeFine))
           AS creationTime,
       p.creationTimeCoarse,
       p.creationTimeFine,
       FROM_JAVATIME(
           EXACT_TIME_FROM_COARSE_FINE(p.dvtScetCoarse, p.dvtScetFine)) AS dvtScet,
       p.dvtScetCoarse,
       p.dvtScetFine,
       p.vcid,
       p.isPartial,
       p.apid,
       p.apidName,
       p.sequenceId,
       p.sequenceVersion,
       p.commandNumber,
       p.xmlVersion,
       p.totalParts,
       p.dvtSclkCoarse,
       p.dvtSclkFine,
       p.fullPath,
       p.fileName,
       FROM_JAVATIME(
           ERT_EXACT_TIME_FROM_COARSE_FINE(p.ertCoarse, p.ertFine)) AS ert,
       p.ertCoarse,
       p.ertFine,
       p.groundStatus,
       p.sequenceCategory,
       p.sequenceNumber,
       p.version,
       p.checksum,
       p.cfdpTransactionId,
       p.fileSize
FROM Product AS p STRAIGHT_JOIN Host AS h ON (p.hostId = h.hostId);