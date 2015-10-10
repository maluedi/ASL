package server;

import java.sql.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ASL_ConnectionPool {
	
	private BlockingQueue<Connection> pool;
	
	private int poolSize;
	
	private String dbUrl;
	private String dbUser;
	private String dbPassword;
	//private String driverClassName;
	
	public ASL_ConnectionPool(int poolSize, String url, String user, 
			String password, String driverClassName) throws ClassNotFoundException {
		this.poolSize = poolSize;
		
		this.dbUrl = url;
		this.dbUser = user;
		this.dbPassword = password;
		//this.driverClassName = driverClassName;
		
		this.pool = new ArrayBlockingQueue<Connection>(poolSize, true);
		Class.forName(driverClassName);
		
		//initPool(driverClassName);
	}
	
	public void initPool() throws SQLException{
		
		for(int i = 0; i < poolSize; i++){
			Connection conn = DriverManager.getConnection(dbUrl,dbUser,dbPassword);
			pool.offer(conn);
		}
	}
	
	public Connection borrowConnection() throws InterruptedException, SQLException{
		// wait until connection becomes available
		Connection conn = null;
		conn = pool.take();
		if(!conn.isValid(2)){
			conn.close();
			conn = DriverManager.getConnection(dbUrl,dbUser,dbPassword);
		}
		return conn;
	}
	
	public void returnConnection(Connection conn){
		if(conn != null){
			pool.offer(conn);
		}
	}
	
	public void closePool(){
		Connection conn = null;
		while((conn = pool.poll()) != null){
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
