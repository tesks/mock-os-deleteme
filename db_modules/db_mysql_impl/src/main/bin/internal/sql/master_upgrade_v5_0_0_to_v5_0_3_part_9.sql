-- Master upgrade, part 8, Product table.
-- MPCS-10869 : created - because there are no product DB changes, let's just port them over straight

system echo 'Part 9' `date`


system echo 'Upgrading product table' `date`

CALL IssueByMission(
    'INSERT INTO Product SELECT * FROM mission_ampcs_prev.Product');


system echo 'Putting back Product indexes' `date`

CALL IndexProduct();


system echo 'End part 9' `date`