package server;

import java.io.IOException;
import java.net.ServerSocket;
//import java.sql.Connection;
//import java.sql.DriverManager;
import java.sql.SQLException;

public class ASL_Server {
	
	private int portNumber;
//	private String dbUrl;
//	private String dbUser;
//	private String dbPassword;
	
	private ASL_ConnectionPool pool;
	
	public boolean listening;
	
	public ASL_Server(int portNumber, String url, String user, String password, String driverClassName) throws ClassNotFoundException, SQLException{
		super();
		this.portNumber = portNumber;
//		this.dbUrl = url;
//		this.dbUser = user;
//		this.dbPassword = password;
		
		this.pool = new ASL_ConnectionPool(30,5,url,user,password,driverClassName);
	}

	
	public void run(){
		listening = true;
		try(
			ServerSocket serverSocket = new ServerSocket(portNumber);
		){
			while(listening){
				new ASL_ServerThread(serverSocket.accept(),pool).start();
			}
		} catch(IOException ex){
			System.err.println("Could not listen on port " + portNumber);
		}
	}

	public static void main(String[] args) {
		if(args.length < 4){
			System.err.println("Usage: ASL_Server <port> <db-url> <db-user> <db-password>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		String url = args[1];
		String user = args[2];
		String password = args[3];
		String driverName = "org.postgresql.Driver";
		
//		try {
//			Class.forName("org.postgresql.Driver");
//		} catch (ClassNotFoundException ex){
//			System.err.println("JDBC Driver not found");
//			System.exit(-1);
//		}
		
		try{
			ASL_Server server = new ASL_Server(port,url,user,password,driverName);
			server.run();
		} catch (ClassNotFoundException ex){
			System.err.println("JDBC Driver not found");
			System.exit(-1);
		} catch (SQLException ex1){
			System.err.println("SQL exception: " + ex1.getMessage());
			System.exit(-1);
		}
	}

}
