-- Master upgrade, part 5, ChannelData
-- The table creation has already been performed.
-- MPCS-10853  : Deprecate legacy channel value tables in database scripts
-- MPCS-10869 : Add Aggregates

system echo 'Part 5' `date`


system echo 'Upgrading ChannelData table' `date`

CALL IssueByMission(
    'INSERT INTO ChannelData SELECT * FROM mission_ampcs_prev.ChannelData');

system echo 'Upgrading ChannelAggregate table' `date`

CALL IssueByMission(
    'INSERT INTO ChannelAggregate SELECT * FROM mission_ampcs_prev.ChannelAggregate');

system echo 'Upgrading HeaderChannelAggregate table' `date`

CALL IssueByMission(
    'INSERT INTO HeaderChannelAggregate SELECT * FROM mission_ampcs_prev.HeaderChannelAggregate');

system echo 'Upgrading MonitorChannelAggregate table' `date`

CALL IssueByMission(
    'INSERT INTO MonitorChannelAggregate SELECT * FROM mission_ampcs_prev.MonitorChannelAggregate');

system echo 'Upgrading SseChannelAggregate table' `date`

CALL IssueByMission(
    'INSERT INTO SseChannelAggregate SELECT * FROM mission_ampcs_prev.SseChannelAggregate');


system echo 'End part 5' `date`
