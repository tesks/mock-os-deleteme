-- ----------------------------------------------------------
-- Delete all rows from all tables.
-- Truncate is faster than Delete.
-- ----------------------------------------------------------

-- MPCS-8773 - Removed stats table.
TRUNCATE TABLE action;
TRUNCATE TABLE classmaps;
TRUNCATE TABLE logs;
TRUNCATE TABLE process;
TRUNCATE TABLE products;
TRUNCATE TABLE status;


-- Insert to add Generic processor classes
INSERT INTO classmaps (mnemonic, className, enabled) VALUES ('logger', 'jpl.gds.product.processors.ConsoleLoggerProcessor', 1);
