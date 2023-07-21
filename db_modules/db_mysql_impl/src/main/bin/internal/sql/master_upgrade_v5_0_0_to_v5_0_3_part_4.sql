-- Master upgrade, part 4, Evr, EvrMetadata, SseEvr, SseEvrMetadata.
-- The table creation has already been performed.
-- MPCS-10869 : Add Evr2 & SseEvr2

system echo 'Part 4' `date`

system echo 'Upgrading Evr table' `date`

CALL IssueByMission(
    'INSERT INTO Evr SELECT * FROM mission_ampcs_prev.Evr');


system echo 'Upgrading EvrMetadata table' `date`

CALL IssueByMission(
    'INSERT INTO EvrMetadata SELECT * FROM mission_ampcs_prev.EvrMetadata');


system echo 'Upgrading SseEvr table' `date`

CALL IssueByMission(
    'INSERT INTO SseEvr SELECT * FROM mission_ampcs_prev.SseEvr');


system echo 'Upgrading SseEvrMetadata table' `date`

CALL IssueByMission(
    'INSERT INTO SseEvrMetadata SELECT * FROM mission_ampcs_prev.SseEvrMetadata');


system echo 'End part 4' `date`
