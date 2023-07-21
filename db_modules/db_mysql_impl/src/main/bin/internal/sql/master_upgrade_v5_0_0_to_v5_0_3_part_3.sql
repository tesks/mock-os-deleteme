-- Master upgrade, part 3, Packet, PacketBody, SsePacket, SsePacketBody.
-- The table creation has already been performed.

system echo 'Part 3' `date`

system echo 'Upgrading Packet table' `date`

CALL IssueByMission(
    'INSERT INTO Packet SELECT * FROM mission_ampcs_prev.Packet');


system echo 'Upgrading PacketBody table' `date`

CALL IssueByMission(
    'INSERT INTO PacketBody SELECT * FROM mission_ampcs_prev.PacketBody');


system echo 'Upgrading SsePacket table' `date`

CALL IssueByMission(
    'INSERT INTO SsePacket SELECT * FROM mission_ampcs_prev.SsePacket');


system echo 'Upgrading SsePacketBody table' `date`

CALL IssueByMission(
    'INSERT INTO SsePacketBody SELECT * FROM mission_ampcs_prev.SsePacketBody');


system echo 'End part 3' `date`
