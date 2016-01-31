package db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class SQLModule {

	protected Connection connection;
	public SQLModule(String driver, String url, String username, String password) throws FailedConnectionException
	{
		try {
			Class.forName(driver);

			connection = DriverManager.getConnection(url, username, password);

		} catch (Exception e) {
			throw new FailedConnectionException("Couldn't connect to database");
		}
	}

	public ResultSet executeQuery(String query) throws QueryFailedException
	{
		Statement statement;
		ResultSet set = null;
		try {
			statement = connection.createStatement();
			set = statement.executeQuery(query);
		} catch (SQLException se) {
			String message = "";
			message += "SQL Exception:\n";

			while( se != null )
			{
				message += "State  : " + se.getSQLState()+"\n";
				message += "Message: " + se.getMessage()+"\n";
				message += "Error  : " + se.getErrorCode()+"\n";

				se = se.getNextException() ;
			}
			throw new QueryFailedException(message);
			
		} catch (Exception e)
		{
			throw new QueryFailedException(e);
		}

		return set;

	}
}
