-- GENERIC addendum upgrade. Does Product.
-- The table creation has already been performed.

system echo 'GENERIC addendum upgrade' `date`

system echo 'Cleaning up' `date`

WARNINGS;

DELETE FROM Product;

system echo 'Upgrading Product table' `date`

CALL IssueByMission(
    'INSERT INTO Product
    SELECT
    sessionId,
    hostId,
    sessionFragment,
    rctCoarse,
    rctFine,
    creationTimeCoarse,
    creationTimeFine,
    dvtScetCoarse,
    dvtScetFine,
    vcid,
    isPartial,
    apid,
    apidName,
    sequenceId,
    sequenceVersion,
    commandNumber,
    xmlVersion,
    totalParts,
    dvtSclkCoarse,
    dvtSclkFine,
    fullPath,
    fileName,
    ertCoarse,
    ertFine,
    groundStatus,
    sequenceCategory,
    sequenceNumber,
    version,
    checksum,
    cfdpTransactionId,
    fileSize
    FROM mission_ampcs_prev.Product');

system echo 'Putting back Product indexes' `date`

CALL IndexProduct();

system echo 'GENERIC addendum upgrade finished' `date`
