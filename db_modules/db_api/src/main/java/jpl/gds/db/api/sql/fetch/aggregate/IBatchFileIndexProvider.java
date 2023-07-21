package jpl.gds.db.api.sql.fetch.aggregate;

import java.io.Closeable;

public interface IBatchFileIndexProvider extends Iterable<ComparableIndexItem<String>>, Closeable {

}
