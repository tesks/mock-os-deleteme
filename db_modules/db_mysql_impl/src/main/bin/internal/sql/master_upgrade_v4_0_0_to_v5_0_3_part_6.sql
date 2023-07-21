-- Master upgrade, part 6, LogMessage.
-- The table creation has already been performed.

system echo 'Part 6' `date`


system echo 'Upgrading LogMessage table' `date`

CALL IssueByMission(
    'INSERT INTO LogMessage
    SELECT
    sessionId,
    hostId,
    sessionFragment,
    rctCoarse,
    rctFine,
    eventTimeCoarse,
    eventTimeFine,
    classification,
    message,
    type,
    NULL,
    NULL
    FROM mission_ampcs_prev.LogMessage');

system echo 'End part 6' `date`
