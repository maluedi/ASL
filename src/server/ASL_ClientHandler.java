package server;

import java.io.*;
//import java.io.InputStreamReader;
//import java.io.BufferedReader;
//import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;



public class ASL_ClientHandler implements Runnable {
	private Socket socket = null;
	
	private ASL_ConnectionPool pool;
	
	public ASL_ClientHandler(Socket socket, ASL_ConnectionPool pool){
		this.socket = socket;
		this.pool = pool;
	}
	
	public void run() {
		try (
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		) {
			System.out.println("Received message: " + in.readUTF());
			
			String response = "";
			
			//--------sql---------
			
			Connection conn = null;
			Statement st = null;
			ResultSet rs = null;
			
			try{
				conn = pool.borrowConnection();
				st = conn.createStatement();
				rs = st.executeQuery("SELECT VERSION()");
				if(rs.next()){
					response = rs.getString(1);
				}
			} catch (SQLException e){
				System.err.println("Error in SQL connection");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					if( rs != null){
						rs.close();
					}
					if( st != null){
						st.close();
					}
					if( conn != null){
						pool.returnConnection(conn);
					}
				} catch (SQLException e) {
					
				}
			}
			
			//--------/sql--------
			System.out.println("Sending message: " + response);
			out.writeUTF(response); out.flush();
			socket.close();
		} catch (IOException ex){
			System.err.println(ex.getMessage());
		}
	}
}
