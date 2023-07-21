-- Master upgrade part 2, Frame and FrameBody.
-- The table creation has already been performed.

system echo 'Part 2' `date`

system echo 'Upgrading Frame table' `date`

CALL IssueByMission(
    'INSERT INTO Frame SELECT
    sessionId,
    hostId,
    sessionFragment,
    id,
    type,
    rctCoarse,
    rctFine,
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
    trailerLength
    FROM mission_ampcs_prev.Frame');


system echo 'Upgrading FrameBody table' `date`

CALL IssueByMission(
    'INSERT INTO FrameBody SELECT
    sessionId,
    hostId,
    sessionFragment,
    id,
    body,
    header,
    trailer
    FROM mission_ampcs_prev.FrameBody');


system echo 'End part 2' `date`
