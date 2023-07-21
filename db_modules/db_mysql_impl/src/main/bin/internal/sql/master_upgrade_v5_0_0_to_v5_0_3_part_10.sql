-- Master upgrade, part 10, Final
-- The table creation has already been performed.
-- MPCS-10853 -  Deprecate legacy channel value tables in database scripts
-- MPCS-10869 - moved to part 10.

system echo 'Part 10' `date`


system echo 'Putting back Frame indexes' `date`

CALL IndexFrame();



system echo 'Putting back Packet indexes' `date`

CALL IndexPacket();



system echo 'Putting back SsePacket indexes' `date`

CALL IndexSsePacket();



system echo 'Putting back Evr indexes' `date`

CALL IndexEvr();



system echo 'Putting back SseEvr indexes' `date`

CALL IndexSseEvr();


system echo 'Analyze all tables' `date`

CALL AnalyzeTables();


system echo 'End part 10' `date`
