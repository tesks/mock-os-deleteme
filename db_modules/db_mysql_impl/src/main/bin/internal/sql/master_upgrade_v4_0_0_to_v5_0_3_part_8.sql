-- Master upgrade, part 8, Final
-- The table creation has already been performed.
-- MPCS-10853 : Deprecate legacy channel value tables in database scripts
system echo 'Part 8' `date`


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



system echo 'Putting back LogMessage indexes' `date`

system echo 'Analyze all tables' `date`

CALL AnalyzeTables();


system echo 'End part 8' `date`
