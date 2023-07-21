-- Master upgrade, part 8, Cfdp tables.
-- MPCS-10869 : created

system echo 'Part 8' `date`


system echo 'Upgrading CfdpFileGeneration table' `date`

CALL IssueByMission(
    'INSERT INTO CfdpFileGeneration SELECT * FROM mission_ampcs_prev.CfdpFileGeneration');


system echo 'Upgrading CfdpFileUplinkFinished table' `date`

CALL IssueByMission(
    'INSERT INTO CfdpFileUplinkFinished SELECT * FROM mission_ampcs_prev.CfdpFileUplinkFinished');


system echo 'Upgrading CfdpIndication table' `date`

CALL IssueByMission(
    'INSERT INTO CfdpIndication SELECT * FROM mission_ampcs_prev.CfdpIndication');


system echo 'Upgrading CfdpPduReceived table' `date`

CALL IssueByMission(
    'INSERT INTO CfdpPduReceived SELECT * FROM mission_ampcs_prev.CfdpPduReceived');


system echo 'Upgrading CfdpPduSent table' `date`

CALL IssueByMission(
    'INSERT INTO CfdpPduSent SELECT * FROM mission_ampcs_prev.CfdpPduSent');


system echo 'Upgrading CfdpRequestReceived table' `date`

CALL IssueByMission(
    'INSERT INTO CfdpRequestReceived SELECT * FROM mission_ampcs_prev.CfdpRequestReceived');


system echo 'Upgrading CfdpRequestResult table' `date`

CALL IssueByMission(
    'INSERT INTO CfdpRequestResult SELECT * FROM mission_ampcs_prev.CfdpRequestResult');

system echo 'End part 8' `date`
