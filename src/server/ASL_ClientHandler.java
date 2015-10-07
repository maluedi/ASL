package server;

import java.io.*;
//import java.io.InputStreamReader;
//import java.io.BufferedReader;
//import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import util.ASL_Util;

public class ASL_ClientHandler implements Runnable {

	private Socket socket = null;
	private int command;

	private ASL_ConnectionPool pool;

	public ASL_ClientHandler(Socket socket, ASL_ConnectionPool pool) {
		this.socket = socket;
		this.pool = pool;
	}

	private String getSql(DataInputStream in) throws IOException {

		command = in.readByte();
		int queue, sender, receiver;
		String message = "";
		String sql = "";
		switch (command) {
		case 1: // push
			queue = in.readInt();
			sender = in.readInt();
			receiver = in.readInt();
			message = in.readUTF();
			if (receiver == 0) {
				sql = ("select push_message(" + queue + "," + sender + ",'"
						+ message + "');");
			} else {
				sql = ("select push_message(" + queue + "," + sender + ","
						+ receiver + ",'" + message + "');");
			}
			break;
		case 2: // poll
			queue = in.readInt();
			sender = in.readInt();
			receiver = in.readInt();
			if (sender == 0) {
				sql = ("select * from poll_message(" + queue + "," + receiver + ");");
			} else {
				sql = ("select * from poll_message(" + queue + "," + sender
						+ "," + receiver + ");");
			}
			break;
		case 3: // peek
			queue = in.readInt();
			sender = in.readInt();
			receiver = in.readInt();
			if (sender == 0) {
				sql = ("select * from peek_message(" + queue + "," + receiver + ");");
			} else {
				sql = ("select * from peek_message(" + queue + "," + sender
						+ "," + receiver + ");");
			}
			break;
		case 4: // create queue
			sql = "select * from create_queue()";
			break;
		case 5: // delete queue
			queue = in.readInt();
			sql = ("select delete_queue(" + queue + ");");
			break;
		case 6: // get queues
			receiver = in.readInt();
			sql = ("select * from get_queues(" + receiver + ");");
			break;
		case 7: // get message from sender
			sender = in.readInt();
			receiver = in.readInt();
			sql = ("select * from get_message(" + sender + "," + receiver + ");");
			break;
		case 8: // register user
			sql = "select * from register_user();";
			break;
		}
		System.out.println(sql);
		return sql;
	}

	private List<Object> getResult(String sql) {
		List<Object> result = new ArrayList<Object>();

		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;

		int err = 0;

		try {
			conn = pool.borrowConnection();
			st = conn.createStatement();
			boolean res = st.execute(sql);
			switch (command) {
			case 1:
				break;
			 case 2:
			 case 3:
				 if(res && (rs = st.getResultSet()).next()){
					 result.add(rs.getInt("queue"));
					 result.add(rs.getInt("sender"));
					 result.add(rs.getInt("receiver"));
					 result.add(rs.getString("entrytime"));
					 result.add(rs.getString("message"));
				 } else {
					err = ASL_Util.INTERNAL_ERROR;
				 }
				 break;
			case 4:
				if (res && (rs = st.getResultSet()).next()) {
					result.add(rs.getInt(1));
				} else {
					err = ASL_Util.INTERNAL_ERROR;
				}
				break;
			case 5:
				break;
			case 6:
				if (res && (rs = st.getResultSet()).next()) {
					do{
						result.add(rs.getInt(1));
					}while(rs.next());
				}
				break;
			case 7:
				 if(res){
					 if((rs = st.getResultSet()).next()){
						 result.add(rs.getInt("queue"));
						 result.add(rs.getInt("sender"));
						 result.add(rs.getInt("receiver"));
						 result.add(rs.getString("entrytime"));
						 result.add(rs.getString("message"));
					 } else {
						 err = ASL_Util.NO_MESSAGE_FROM_SENDER;
					 }
				 } else {
					err = ASL_Util.INTERNAL_ERROR;
				 }
				 break;
			case 8:
				if (res && (rs = st.getResultSet()).next()) {
					result.add(rs.getInt(1));
				} else {
					err = ASL_Util.INTERNAL_ERROR;
				}
				break;
			}

		} catch (SQLException e) {
			switch (Integer.parseInt(e.getSQLState())) {
			case 23101:
				err = ASL_Util.QUEUE_DOES_NOT_EXIST;
				break;
			case 23102:
				err = ASL_Util.SENDER_DOES_NOT_EXIST;
				break;
			case 23103:
				err = ASL_Util.RECEIVER_DOES_NOT_EXIST;
				break;
			case 23104:
				err = ASL_Util.QUEUE_IS_EMPTY;
				break;
			default:
				err = ASL_Util.INTERNAL_ERROR;
				break;
			}
			System.out.println("[Notice] Error in SQL connection");
			System.out.println("[Notice] " + e.getLocalizedMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
			err = ASL_Util.INTERNAL_ERROR;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
				if (conn != null) {
					pool.returnConnection(conn);
				}
			} catch (SQLException e) {
				System.err.println(e.getLocalizedMessage());
			}
		}
		result.add(err);
		return result;
	}

	private void sendResult(List<Object> result, DataOutputStream out)
			throws IOException {

		int err = (int) result.get(result.size()-1);
		out.writeByte(err);
		if (err == 0) {
			switch (command) {
			case 1:
				break;
			case 2:
			case 3:
			case 7:
				out.writeInt((int) result.get(0));
				out.writeInt((int) result.get(1));
				out.writeInt((int) result.get(2));
				out.writeUTF((String) result.get(3));
				out.writeUTF((String) result.get(4));
			case 4:
				out.writeInt((int) result.get(0));
				break;
			case 5:
				break;
			case 6:
				out.writeInt(result.size()-1);
				for(int i = 0; i < result.size()-1; i++){
					out.writeInt((int) result.get(i));
				}
			case 8:
				out.writeInt((int) result.get(0));
				break;
			}
		} 
		out.flush();
	}

	public void run() {
		try (
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		) {
			String sql = getSql(in);

			// --------sql---------

			List<Object> result = getResult(sql);

			// --------/sql--------

			sendResult(result, out);
		} catch (IOException ex) {
			System.err.println(ex.getLocalizedMessage());
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				System.err.println(e.getLocalizedMessage());
			}
		}
	}
}
