-- MPCS-10853: Deprecate legacy channel value tables in database scripts
-- R8.1 does not support ECDR with EHA Aggregates

/*
ALTER TABLE ChannelValue2
   ADD INDEX ecdrScetIndex(sessionId, scetCoarse, scetFine);

ALTER TABLE SseChannelValue2
   ADD INDEX ecdrScetIndex(sessionId, scetCoarse, scetFine);

ALTER TABLE HeaderChannelValue2
   ADD INDEX ecdrErtIndex(sessionId, ertCoarse, ertFine);

ALTER TABLE MonitorChannelValue
   ADD INDEX ecdrErtIndex(sessionId, mstCoarse, mstFine);
*/