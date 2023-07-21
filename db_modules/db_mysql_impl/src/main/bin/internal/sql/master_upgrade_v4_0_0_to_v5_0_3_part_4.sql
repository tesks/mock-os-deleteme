-- Master upgrade, part 4, Evr, EvrMetadata, SseEvr, SseEvrMetadata.
-- The table creation has already been performed.

system echo 'Part 4' `date`

system echo 'Upgrading Evr table' `date`

CALL IssueByMission(
    'INSERT INTO Evr SELECT
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
     scetFine,
     sclkCoarse,
     sclkFine,
     dssId,
     vcid,
     level,
     module,
     message,
     isRealtime
     FROM mission_ampcs_prev.Evr');


system echo 'Upgrading EvrMetadata table' `date`

CALL IssueByMission(
    'INSERT INTO EvrMetadata SELECT
    sessionId,
    hostId,
    sessionFragment,
    id,
    keyword,
    value
    FROM mission_ampcs_prev.EvrMetadata');


system echo 'Upgrading SseEvr table' `date`

CALL IssueByMission(
    'INSERT INTO SseEvr SELECT
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
     scetFine,
     sclkCoarse,
     sclkFine,
     level,
     module,
     message
     FROM mission_ampcs_prev.SseEvr');


system echo 'Upgrading SseEvrMetadata table' `date`

CALL IssueByMission(
    'INSERT INTO SseEvrMetadata SELECT
    sessionId,
    hostId,
    sessionFragment,
    id,
    keyword,
    value
    FROM mission_ampcs_prev.SseEvrMetadata');


system echo 'End part 4' `date`
