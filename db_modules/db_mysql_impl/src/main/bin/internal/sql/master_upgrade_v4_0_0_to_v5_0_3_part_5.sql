-- Master upgrade, part 5, ChannelData
-- The table creation has already been performed.

-- MPCS-10853 : Deprecate legacy channel value tables in database scripts
system echo 'Part 5' `date`


system echo 'Upgrading ChannelData table' `date`

CALL IssueByMission(
    'INSERT INTO ChannelData
    SELECT
    sessionId,
    hostId,
    sessionFragment,
    id,
    channelId,
    fromSse,
    type,
    channelIndex,
    module,
    name,
    dnFormat,
    euFormat
    FROM mission_ampcs_prev.ChannelData');

system echo 'End part 5' `date`
