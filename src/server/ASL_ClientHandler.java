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
			int command = in.readByte();
			int queue,sender,receiver;
			String message = "";
			String sql = "";
			switch(command){
			case 1:	//push
				queue = in.readInt();
				sender = in.readInt();
				receiver = in.readInt();
				message = in.readUTF();
				if(receiver == 0){
					sql = ("select push_message("
							+ queue + ","
							+ sender + ","
							+ message + 
						");");				
				} else {
					sql = ("select push_message("
							+ queue + ","
							+ sender + ","
							+ sender + ","
							+ message + 
						");");
				}
				break;
			case 2: //poll
				queue = in.readInt();
				sender = in.readInt();
				receiver = in.readInt();
				if (sender == 0){
					sql = ("select * from poll_message("
							+ queue + ","
							+ receiver + 
						");");	
				} else {
					sql = ("select * from poll_message("
							+ queue + ","
							+ sender + ","
							+ receiver + 
						");");	
				}
				break;
			case 3: //peek
				queue = in.readInt();
				sender = in.readInt();
				receiver = in.readInt();
				if (sender == 0){
					sql = ("select * from peek_message("
							+ queue + ","
							+ receiver + 
						");");	
				} else {
					sql = ("select * from peek_message("
							+ queue + ","
							+ sender + ","
							+ receiver + 
						");");	
				}
				break;
			case 4: //create queue
				sql = "select * from create_queue()";
				break;
			case 5: //delete queue
				queue = in.readInt();
				sql = ("select delete_queue(" 
						+ queue +
					");");
				break;
			case 6: //get queues
				receiver = in.readInt();
				sql = ("select * from get_queues(" 
						+ receiver +
					");");
				break;
			case 7: //get message from sender
				sender = in.readInt();
				receiver = in.readInt();
				sql = ("select * from get_message("
						+ sender + ","
						+ receiver + 
					");");	
				break;
			case 8: //register user
				sql = "select * from register_user();";
				break;
			}
			
			//--------sql---------
			
			Connection conn = null;
			PreparedStatement pst = null;
			ResultSet rs = null;
			
			try{
				conn = pool.borrowConnection();
				st = conn.createStatement();
				rs = st.executeQuery(sql);
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
