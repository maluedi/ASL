package server;

import java.sql.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author Marcel Lüdi
 *
 * Manages the connections to the database 
 */
public class ASL_ConnectionPool {
	
	private BlockingQueue<Connection> pool;
	
	private int poolSize;
	
	private String dbUrl;
	private String dbUser;
	private String dbPassword;
	
	/**
	 * Creates a new Connection pool with the specified capacity to the 
	 * specified data base using the specified database driver. The 
	 * Connections are not opened after this constructor is finished. Use
	 * initPool() to fill the pool with connections.
	 * 
	 * @param poolSize 
	 * 			  the number of available connections
	 * @param url
	 * 			  the url of the database
	 * @param user
	 * 			  the user account of the database
	 * @param password
	 * 			  the password to the user account
	 * @param driverClassName
	 * 			  the name of the JDBC driver
	 * @throws ClassNotFoundException
	 * 			  if the driver is not found
	 */
	public ASL_ConnectionPool(int poolSize, String url, String user, 
			String password, String driverClassName) throws ClassNotFoundException {
		this.poolSize = poolSize;
		
		this.dbUrl = url;
		this.dbUser = user;
		this.dbPassword = password;
		
		this.pool = new ArrayBlockingQueue<Connection>(poolSize, true);
		Class.forName(driverClassName);
	}
	
	/**
	 * Fills the connection pool with connections to the database
	 * 
	 * @throws SQLException
	 * 			  if there is a problem connecting to the database
	 */
	public void initPool() throws SQLException{
		
		for(int i = 0; i < poolSize; i++){
			Connection conn = DriverManager.getConnection(dbUrl,dbUser,dbPassword);
			pool.offer(conn);
		}
	}
	
	/**
	 * Returns a connection to the database
	 * @return
	 * 			  the connection to the database
	 * @throws InterruptedException
	 * 			  if the thread is interrupted while waiting for a connection
	 * @throws SQLException
	 * 			  if there is a problem with the database connection
	 */
	public Connection borrowConnection() throws InterruptedException, SQLException{
		// wait until connection becomes available
		Connection conn = null;
		conn = pool.take();
		
		// check if the connection is valid and replace it if necessary
		if(conn == null || !conn.isValid(2)){
			if(conn != null){
				try {
					conn.close();
				} catch (SQLException e) {
					System.err.println(e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
			conn = DriverManager.getConnection(dbUrl,dbUser,dbPassword);
		}
		return conn;
	}
	
	/**
	 * Returns the connection to the connection pool
	 * 
	 * @param conn
	 * 			  the returned connection
	 */
	public void returnConnection(Connection conn){
		pool.offer(conn);
	}
	
	/**
	 * Closes all connections to the database, emptying the pool
	 */
	public void closePool(){
		Connection conn = null;
		while((conn = pool.poll()) != null){
			try {
				conn.close();
			} catch (SQLException e) {
				System.err.println(e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}
}
