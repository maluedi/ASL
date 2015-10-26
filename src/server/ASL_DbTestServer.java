package server;

import java.sql.*;
import java.util.ArrayDeque;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import util.ASL_Util;

public class ASL_DbTestServer implements Runnable {

	private Connection conn;
	private int msgSize;
	public final int id;
	public final int pubId;
	public final int pubQ;

	public int[] sendId;

	public int counter;

	private final Logger logger;
	private final Logger traceLogger;
	public static final char[] alphabet = ("abcdefghijklmnopqrstuvwxyz"
			+ "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "1234567890"
			+ " !\"#$%&()*+,-./:;<=>?@[\\]^_{|}~").toCharArray();

	public boolean running;

	public ASL_DbTestServer(String url, String user, String password,
			String driverName, int msgSize, int id)
			throws ClassNotFoundException, SQLException {

		this.id = id;
		this.msgSize = msgSize;
		Class.forName(driverName);
		this.conn = DriverManager.getConnection(url, user, password);

		this.logger = LogManager.getLogger("Server " + id);
		this.traceLogger = LogManager.getLogger("trace");

		Statement st = conn.createStatement();
		st.execute("select * from register_user();");
		ResultSet rs = st.getResultSet();
		if (rs.next()) {
			this.pubId = rs.getInt(1);
		} else {
			this.pubId = 0;
		}
		rs.close();

		st.execute("select * from create_queue()");
		rs = st.getResultSet();
		if (rs.next()) {
			this.pubQ = rs.getInt(1);
		} else {
			this.pubQ = 0;
		}
		rs.close();
		st.close();
	}

	@Override
	public void run() {
		try {
			int waitTime = 10;
			Random r = new Random();
			int err;
			running = true;
			long effectiveWaitTime;

			char[] tmpm = new char[this.msgSize];
			int rcvr = 0;
			ArrayDeque<Integer> qs = new ArrayDeque<Integer>();
			int nextComm = ASL_Util.PUSH;

			long t0, t1 = System.currentTimeMillis();
			while (running) {
				counter++;
				if (nextComm == ASL_Util.PUSH) {
					for (int i = 0; i < this.msgSize; i++) {
						tmpm[i] = alphabet[r.nextInt(alphabet.length)];
					}
					rcvr = r.nextInt(sendId.length);
					// logger.info("push to " + rcvr);

					effectiveWaitTime = waitTime
							- (System.currentTimeMillis() - t1);
					if (effectiveWaitTime > 0) {
						Thread.sleep(effectiveWaitTime);
					} else {
						logger.info("wait time too long: "
								+ (waitTime - effectiveWaitTime));
					}

					// logger.info("push");
					t0 = System.currentTimeMillis();
					try {
						Statement st = conn.createStatement();
						st.execute("select push_message(" + pubQ + "," + pubId
								+ "," + sendId[rcvr] + ",'" + tmpm.toString()
								+ "');");
						st.close();
						err = 0;
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
						logger.error(e.getLocalizedMessage());
					}
					t1 = System.currentTimeMillis();
					traceLogger.trace(ASL_Util.PUSH + "," + err + "," + t0
							+ "," + t1);

					nextComm = ASL_Util.GET_QUEUES;

				} else if (nextComm == ASL_Util.POLL) {
					// logger.info("poll from " + qs[nWaiting - 1]);
					effectiveWaitTime = waitTime
							- (System.currentTimeMillis() - t1);
					if (effectiveWaitTime > 0) {
						Thread.sleep(effectiveWaitTime);
					} else {
						logger.info("wait time too long: "
								+ (waitTime - effectiveWaitTime));
					}
					// logger.info("poll");
					t0 = System.currentTimeMillis();
					try {

						Statement st = conn.createStatement();
						st.executeQuery("select * from poll_message("
								+ qs.poll() + "," + pubId + ");");
						st.close();
						err = 0;
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
						logger.error(e.getLocalizedMessage());
					}
					// logger.trace("end poll");
					t1 = System.currentTimeMillis();
					traceLogger.trace(ASL_Util.POLL + "," + err + "," + t0
							+ "," + t1);

					if (!qs.isEmpty()) {
						nextComm = ASL_Util.POLL;
					} else {
						nextComm = ASL_Util.PUSH;
						// nextComm = ASL_Util.GET_QUEUES;
					}
				} else if (nextComm == ASL_Util.GET_QUEUES) {
					// logger.info("get queues");
					effectiveWaitTime = waitTime
							- (System.currentTimeMillis() - t1);
					if (effectiveWaitTime > 0) {
						Thread.sleep(effectiveWaitTime);
					} else {
						logger.info("wait time too long: "
								+ (waitTime - effectiveWaitTime));
					}
					// logger.info("poll");
					t0 = System.currentTimeMillis();
					try {
						Statement st = conn.createStatement();
						st.execute("select * from get_queues(" + pubId + ");");
						ResultSet rs = st.getResultSet();
						while (rs.next()) {
							qs.add(rs.getInt(1));
						}
						rs.close();
						st.close();
						err = 0;
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
						logger.error(e.getLocalizedMessage());
					}
					// logger.trace("end poll");
					t1 = System.currentTimeMillis();
					traceLogger.trace(ASL_Util.GET_QUEUES + "," + err + ","
							+ t0 + "," + t1);
					if (!qs.isEmpty()) {
						nextComm = ASL_Util.POLL;
					} else {
						nextComm = ASL_Util.PUSH;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getLocalizedMessage());
		}
	}

	public void stop() {
		running = false;
	}

	public static void main(String[] args) {
		if (args.length < 6) {
			System.err
					.println("Usage: ASL_Server <db-ip:db-port> <db-user> <db-password> <# of db-connections> <msg size [B]> <runtime [s]");
			System.exit(-1);
		}
		String url = "jdbc:postgresql://" + args[0] + "/ASL";
		String user = args[1];// "postgres";
		String password = args[2];// "qwer1";
		String driverName = "org.postgresql.Driver";
		int nConns = Integer.parseInt(args[3]);
		int msgSize = Integer.parseInt(args[4]);
		long runtime = Integer.parseInt(args[5]) * 1000;

		ASL_DbTestServer[] servers = new ASL_DbTestServer[nConns];
		Thread[] serverThreads = new Thread[nConns];
		int[] ids = new int[nConns];

		try {
			for (int i = 0; i < nConns; i++) {
				servers[i] = new ASL_DbTestServer(url, user, password,
						driverName, msgSize, i);
				ids[i] = servers[i].pubId;
			}
			for (int i = 0; i < nConns; i++) {
				servers[i].sendId = ids;
				serverThreads[i] = new Thread(servers[i], "ASL server " + i);
				serverThreads[i].start();
			}
			try {
				Thread.sleep(runtime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for (int i = 0; i < nConns; i++) {
				servers[i].stop();
			}
			for (int i = 0; i < nConns; i++) {
				try {
					serverThreads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (ClassNotFoundException ex) {
			System.err.println("JDBC Driver not found");
			ex.printStackTrace();
			System.exit(-1);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
