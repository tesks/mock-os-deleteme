package jpl.gds.db.api.sql.fetch.aggregate;

public class AggregateFetchException extends Exception {
    private static final long serialVersionUID = 8810184671091847097L;
    
    public AggregateFetchException() {
        super();
    }
    
    public AggregateFetchException(final String message) {
        super(message);
    }
    
    public AggregateFetchException(final Throwable cause) {
        super(cause.getLocalizedMessage(), cause);
    }
    
    public AggregateFetchException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
