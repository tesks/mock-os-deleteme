-- Master upgrade, part 3, Packet, PacketBody, SsePacket, SsePacketBody.
-- The table creation has already been performed.

system echo 'Part 3' `date`

system echo 'Upgrading Packet table' `date`

CALL IssueByMission(
    'INSERT INTO Packet SELECT
    sessionId,
    hostId,
    sessionFragment,
    id,
    frameId,
    rctCoarse,
    rctFine,
    scetCoarse,
    scetFine,
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
    FROM mission_ampcs_prev.Packet');


system echo 'Upgrading PacketBody table' `date`

CALL IssueByMission(
    'INSERT INTO PacketBody SELECT
    sessionId,
    hostId,
    sessionFragment,
    id,
    body,
    header,
    trailer
    FROM mission_ampcs_prev.PacketBody');


system echo 'Upgrading SsePacket table' `date`

CALL IssueByMission(
    'INSERT INTO SsePacket SELECT
    sessionId,
    hostId,
    sessionFragment,
    id,
    rctCoarse,
    rctFine,
    scetCoarse,
    scetFine,
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
    FROM mission_ampcs_prev.SsePacket');


system echo 'Upgrading SsePacketBody table' `date`

CALL IssueByMission(
    'INSERT INTO SsePacketBody SELECT
    sessionId,
    hostId,
    sessionFragment,
    id,
    body,
    header,
    trailer
    FROM mission_ampcs_prev.SsePacketBody');


system echo 'End part 3' `date`
