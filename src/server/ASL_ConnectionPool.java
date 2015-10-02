package server;

import java.sql.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ASL_ConnectionPool {
	
	private BlockingQueue<Connection> pool;
	
	private int maxPoolSize;
	private int initialPoolSize;
	private int currentPoolSize;
	
	private String dbUrl;
	private String dbUser;
	private String dbPassword;
	
	public ASL_ConnectionPool(int maxPoolSize, int initialPoolSize, String url, String user, 
			String password, String driverClassName) throws ClassNotFoundException, SQLException{
		this.maxPoolSize = maxPoolSize;
		this.initialPoolSize = initialPoolSize;
		
		this.dbUrl = url;
		this.dbUser = user;
		this.dbPassword = password;
		
		this.pool = new ArrayBlockingQueue<Connection>(maxPoolSize, true);
		
		initPool(driverClassName);
	}
	
	private void initPool(String driverClassName) throws ClassNotFoundException, SQLException{
		Class.forName(driverClassName);
		
		for(int i = 0; i < initialPoolSize; i++){
			openConnection();
		}
	}
	
	private synchronized void openConnection() throws SQLException{
		if(currentPoolSize >= maxPoolSize){ 
			return; 
		}
		
		Connection conn = DriverManager.getConnection(dbUrl,dbUser,dbPassword);
		if(pool.offer(conn)){
			currentPoolSize++;
		}
	}
	
	public Connection borrowConnection() throws InterruptedException, SQLException{
		if(pool.isEmpty() && currentPoolSize < maxPoolSize){
			openConnection();
		}
		
		// wait until connection becomes available
		Connection c = pool.take();
		return c;
	}
	
	public void returnConnection(Connection conn){
		if(conn != null){
			pool.offer(conn);
		}
	}
}
