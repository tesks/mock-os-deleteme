-- Master upgrade, part 7, CommandMessage, CommandStatus.
-- The table creation has already been performed.

system echo 'Part 7' `date`


system echo 'Upgrading CommandMessage table' `date`

CALL IssueByMission(
    'INSERT INTO CommandMessage
     SELECT
     sessionId,
     hostId,
     sessionFragment,
     requestId,
     message,
     type,
     originalFile,
     scmfFile,
     commandedSide,
     finalized,
     checksum,
     totalCltus
     FROM mission_ampcs_prev.CommandMessage');


CALL IssueByMission(
    'INSERT INTO CommandStatus
    SELECT
    sessionId,
    hostId,
    sessionFragment,
    requestId,
    rctCoarse,
    rctFine,
    eventTimeCoarse,
    eventTimeFine,
    status,
    failReason,
    dssId,
    bit1RadTimeCoarse,
    bit1RadTimeFine,
    lastBitRadTimeCoarse,
    lastBitRadTimeFine,
    final
    FROM mission_ampcs_prev.CommandStatus');

system echo 'End part 7' `date`
