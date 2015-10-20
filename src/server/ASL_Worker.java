package server;

import java.io.*;
//import java.io.InputStreamReader;
//import java.io.BufferedReader;
//import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import util.ASL_Util;

/**
 * @author Marcel Lüdi
 * 
 *         Processes the requests from the clients 
 *
 */
public class ASL_Worker implements Runnable {

	private BlockingQueue<ASL_Tuple> socketQueue;
	private int command;
	private int error;

	private ASL_ConnectionPool pool;
	
	private boolean working;
	private Thread workerThread;
	
	public final int id;
	
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private final Logger traceLogger;
	private final Logger logger;
	private long[] t = new long[6];

	/**
	 * Creates a new ASL_Worker which takes its requests from the socketQueue
	 * and data base connections from the pool
	 * 
	 * @param socketQueue
	 *            queue where sockets to clients get put after accepting
	 * @param pool
	 *            connection pool to the database
	 */
	public ASL_Worker(BlockingQueue<ASL_Tuple> socketQueue, ASL_ConnectionPool pool) {
		this.socketQueue = socketQueue;
		this.pool = pool;
		this.logger = LogManager.getLogger("ASL worker");
		this.traceLogger = LogManager.getLogger("trace");
		this.id = 0;
	}

	/**
	 * Creates a new ASL_Worker which takes its requests from the socketQueue
	 * and data base connections from the pool
	 * 
	 * @param socketQueue
	 *            queue where sockets to clients get put after accepting
	 * @param pool
	 *            connection pool to the database
	 * @param id
	 * 			  the id of the worker
	 */
	public ASL_Worker(BlockingQueue<ASL_Tuple> socketQueue, ASL_ConnectionPool pool, int id) {
		this.socketQueue = socketQueue;
		this.pool = pool;
		this.id = id;
		this.logger = LogManager.getLogger("ASL worker " + id);
		this.traceLogger = LogManager.getLogger("trace");
	}

	/**
	 * reads the request from the client and produces an appropriate 
	 * sql query
	 * 
	 * @param 
	 * 			  in the {@link DataInputStream} to the client
	 * @return 
	 * 			  the sql query
	 * @throws IOException 
	 * 			  if there is a problem while reading the request
	 */
	private String getSql(DataInputStream in) throws IOException {

		command = in.readByte();
		int queue, sender, receiver;
		String message = "";
		String sql = "";
		switch (command) {
		case ASL_Util.PUSH:
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
		case ASL_Util.POLL: 
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
		case ASL_Util.PEEK: 
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
		case ASL_Util.CREATE_QUEUE:
			sql = "select * from create_queue()";
			break;
		case ASL_Util.DELETE_QUEUE: 
			queue = in.readInt();
			sql = ("select delete_queue(" + queue + ");");
			break;
		case ASL_Util.GET_QUEUES:
			receiver = in.readInt();
			sql = ("select * from get_queues(" + receiver + ");");
			break;
		case ASL_Util.GET_MESSAGE: 
			sender = in.readInt();
			receiver = in.readInt();
			sql = ("select * from get_message(" + sender + "," + receiver + ");");
			break;
		case ASL_Util.REGISTER_USER:
			sql = "select * from register_user();";
			break;
		}
		return sql;
	}

	/**
	 * executes the sql query on the database and produces a result set
	 * 
	 * @param sql 
	 * 			  the query for the given command
	 * @return 
	 * 			  the list of objects to be sent to the client
	 */
	private List<Object> getResult(String sql) {
		List<Object> result = new ArrayList<Object>();

		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;

		error = 0;

		try {
			conn = pool.borrowConnection();
			t[3] = System.currentTimeMillis();
			st = conn.createStatement();
			boolean res = st.execute(sql);
			switch (command) {
			case ASL_Util.POLL:
			case ASL_Util.PEEK:
				if (res && (rs = st.getResultSet()).next()) {
					result.add(rs.getInt("queue"));
					result.add(rs.getInt("sender"));
					result.add(rs.getInt("receiver"));
					try {
						result.add(df.parse(rs.getString("entrytime")).getTime());
					} catch (ParseException e) {
						//e.printStackTrace();
						logger.error(e.getLocalizedMessage());
						result.add((long) 0);
					}
					result.add(rs.getString("message"));
				} else {
					error = ASL_Util.INTERNAL_ERROR;
				}
				break;
			case ASL_Util.CREATE_QUEUE:
				if (res && (rs = st.getResultSet()).next()) {
					result.add(rs.getInt(1));
				} else {
					error = ASL_Util.INTERNAL_ERROR;
				}
				break;
			case ASL_Util.GET_QUEUES:
				if (res && (rs = st.getResultSet()).next()) {
					do {
						result.add(rs.getInt(1));
					} while (rs.next());
				}
				break;
			case ASL_Util.GET_MESSAGE:
				if (res) {
					if ((rs = st.getResultSet()).next()) {
						result.add(rs.getInt("queue"));
						result.add(rs.getInt("sender"));
						result.add(rs.getInt("receiver"));
						try {
							result.add(df.parse(rs.getString("entrytime")).getTime());
						} catch (ParseException e) {
							e.printStackTrace();
							result.add(0);
						}
						result.add(rs.getString("message"));
					} else {
						error = ASL_Util.NO_MESSAGE_FROM_SENDER;
					}
				} else {
					error = ASL_Util.INTERNAL_ERROR;
				}
				break;
			case ASL_Util.REGISTER_USER:
				if (res && (rs = st.getResultSet()).next()) {
					result.add(rs.getInt(1));
				} else {
					error = ASL_Util.INTERNAL_ERROR;
				}
				break;
			}

		} catch (SQLException e) {
			switch (Integer.parseInt(e.getSQLState())) {
			case 23101:
				error = ASL_Util.QUEUE_DOES_NOT_EXIST;
				break;
			case 23102:
				error = ASL_Util.SENDER_DOES_NOT_EXIST;
				break;
			case 23103:
				error = ASL_Util.RECEIVER_DOES_NOT_EXIST;
				break;
			case 23104:
				error = ASL_Util.QUEUE_IS_EMPTY;
				break;
			default:
				error = ASL_Util.INTERNAL_ERROR;
				System.err.println(e.getLocalizedMessage());
				break;
			}
		} catch (InterruptedException e) {
			error = ASL_Util.INTERNAL_ERROR;
			//System.err.println(e.getLocalizedMessage());
			logger.error(e.getLocalizedMessage());
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				//System.err.println(e.getLocalizedMessage());
				logger.error(e.getLocalizedMessage());
			}
			pool.returnConnection(conn);
		}
		//result.add(err);
		return result;
	}

	/**
	 * sends the results to the client over the output stream
	 * 
	 * @param result
	 * 			  the set of objects to send to the client
	 * @param out
	 * 			  the output stream to the client
	 * @throws IOException
	 * 			  if there is a problem in the output stream
	 */
	private void sendResult(List<Object> result, DataOutputStream out)
			throws IOException {

		//int err = (int) result.get(result.size() - 1);
		out.writeByte(error);								//error code (0 = OK)
		if (error == 0) {
			switch (command) {
			case ASL_Util.POLL:
			case ASL_Util.PEEK:
			case ASL_Util.GET_MESSAGE:
				out.writeInt((int) result.get(0)); 		//queue
				out.writeInt((int) result.get(1));		//sender
				out.writeInt((int) result.get(2));		//receiver
				out.writeLong((long) result.get(3));	//time stamp
				out.writeUTF((String) result.get(4));	//message
			case ASL_Util.CREATE_QUEUE:
				out.writeInt((int) result.get(0));		//queue id
				break;
			case ASL_Util.GET_QUEUES:
				out.writeInt(result.size() - 1);		//number of queues
				for (int i = 0; i < result.size() - 1; i++) {
					out.writeInt((int) result.get(i));	//queue id
				}
			case ASL_Util.REGISTER_USER:
				out.writeInt((int) result.get(0));		//user id
				break;
			}
		}
		out.flush();
	}

	/**
	 * processes the request from the client owning the specified socket
	 * 
	 * @param socket
	 * 			  the socket from the client
	 */
	private void process(Socket socket) {
		//Socket socket = request.s;
		//long reqID = request.rcvTime;
		try (
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		) {
			
			// get request from client
			//logger.trace("reading input: " + reqID);
			t[1] = System.currentTimeMillis();
			String sql = getSql(in);
			
			// get result from database
			//logger.trace("starting db interaction: " + reqID);
			t[2] = System.currentTimeMillis();
			List<Object> result = getResult(sql);
			
			// send result to client
			//logger.trace("sending result: " + reqID);
			t[4] = System.currentTimeMillis();
			sendResult(result, out);
		} catch (IOException e) {
			//System.err.println(e.getLocalizedMessage());
			logger.error(e.getLocalizedMessage());
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		working = true;
		workerThread = Thread.currentThread();
		while (working) {
			try {
				//take socket
				ASL_Tuple req = socketQueue.take();
				t[0] = req.rcvTime;
				
				//process the request
				//logger.trace("start processing: " + req.id);
				//t[1] = System.currentTimeMillis();
				process(req.s);
				//close socket
				req.finish();
				//logger.trace("finished processing: " + req.id);
				t[5] = System.currentTimeMillis();
				
				traceLogger.trace(command + "," + error + "," + t[0] + "," + t[1] + "," + t[2] + "," + t[3] + "," + t[4] + "," + t[5]);
				
			} catch (InterruptedException e) {
				//server shutdown
			} catch (IOException e) {
				//System.err.println(e.getLocalizedMessage()); // error in the client connection
				logger.error(e.getLocalizedMessage());
			} catch (Exception e){
				//e.printStackTrace();
				logger.error(e.getLocalizedMessage());
			}
		}
		logger.info("shutting down");
	}
	
	/**
	 * Stops the worker thread and terminates it
	 */
	public void stop() {
		working = false;
		workerThread.interrupt();
	}

}
