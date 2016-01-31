package db;
public class QueryFailedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public QueryFailedException() {
		super();
	}

	public QueryFailedException(String message) {
		super(message);
	}

	public QueryFailedException(Exception e) {
		super(e);
	}

}
