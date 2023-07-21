-- Master upgrade, part 7, CommandMessage, CommandStatus.
-- The table creation has already been performed.

system echo 'Part 7' `date`


system echo 'Upgrading CommandMessage table' `date`

CALL IssueByMission(
    'INSERT INTO CommandMessage SELECT * FROM mission_ampcs_prev.CommandMessage');


system echo 'Upgrading CommandStatus table' `date`

CALL IssueByMission(
    'INSERT INTO CommandStatus SELECT * FROM mission_ampcs_prev.CommandStatus');

system echo 'End part 7' `date`
