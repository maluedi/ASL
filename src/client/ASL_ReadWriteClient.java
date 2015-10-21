package client;

import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import util.ASL_Util;

public class ASL_ReadWriteClient implements Runnable {

	private ASL_Client client;
	private int msgSize;
	private long waitTime;
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

	public ASL_ReadWriteClient(String hostName, int portNumber, int msgSize,
			long waitTime, int id) throws ASL_Exception {
		this.client = new ASL_Client(hostName, portNumber);
		this.running = false;
		this.id = id;
		this.msgSize = msgSize;
		this.waitTime = waitTime;

		this.logger = LogManager.getLogger("ASL Client " + id);
		this.traceLogger = LogManager.getLogger("trace");

		this.pubId = client.register();
		this.pubQ = client.createQueue();
		
		this.counter = 0;
	}

	public void run() {
		try {
			Random r = new Random();
			int err;
			running = true;
			long effectiveWaitTime;

			char[] tmpm = new char[this.msgSize];
			int rcvr = 0;
			int[] qs = null;
			int nWaiting = 0;
			int nextComm = ASL_Util.PUSH;

			long t0, t1 = System.currentTimeMillis();
			while (running) {
				counter ++;
				if (nextComm == ASL_Util.PUSH) {
					for (int i = 0; i < this.msgSize; i++) {
						tmpm[i] = alphabet[r.nextInt(alphabet.length)];
					}
					rcvr = r.nextInt(sendId.length);
					//logger.info("push to " + rcvr);

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
						client.push(pubQ, sendId[rcvr], new String(tmpm));
						err = 0;
					} catch (ASL_Exception e) {
						logger.error(e.getLocalizedMessage());
						err = e.errCode;
					} 
					// logger.trace("end push");
					t1 = System.currentTimeMillis();
					traceLogger.trace(ASL_Util.PUSH + "," + err + "," + t0
							+ "," + t1);

					nextComm = ASL_Util.GET_QUEUES;

				} else if (nextComm == ASL_Util.POLL) {
					//logger.info("poll from " + qs[nWaiting - 1]);
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
						client.poll(qs[nWaiting - 1]);
						err = 0;
					} catch (ASL_Exception e) {
						logger.error(e.getLocalizedMessage());
						err = e.errCode;
					} 
					// logger.trace("end poll");
					t1 = System.currentTimeMillis();
					traceLogger.trace(ASL_Util.POLL + "," + err + "," + t0
							+ "," + t1);

					nWaiting--;
					if (nWaiting > 0) {
						nextComm = ASL_Util.POLL;
					} else {
						nextComm = ASL_Util.PUSH;
						// nextComm = ASL_Util.GET_QUEUES;
					}
				} else if (nextComm == ASL_Util.GET_QUEUES) {
					//logger.info("get queues");
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
						qs = client.getQueues();
						err = 0;
						nWaiting = qs.length;
					} catch (ASL_Exception e) {
						logger.error(e.getLocalizedMessage());
						err = e.errCode;
						nWaiting = 0;
					} 
					// logger.trace("end poll");
					t1 = System.currentTimeMillis();
					traceLogger.trace(ASL_Util.GET_QUEUES + "," + err + "," + t0
							+ "," + t1);

					if (nWaiting > 0) {
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
		logger.info(counter + " transactions");
	}

	public void stop() {
		running = false;
	}

	public static void main(String[] args) {
		if (args.length < 6) {
			System.err
					.println("Usage: ASL_Client <host> <port> <#clients> <msgSize[Bytes]> <waitTime[ms]> <runtime[s]>");
			System.exit(-1);
		}
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		int nClients = Integer.parseInt(args[2]);
		int msgSize = Integer.parseInt(args[3]);
		int waitTime = Integer.parseInt(args[4]);
		int runtime = Integer.parseInt(args[5]);
		ASL_ReadWriteClient[] clients = new ASL_ReadWriteClient[nClients];
		Thread[] clientThreads = new Thread[nClients];
		int[] ids = new int[nClients];
		for (int i = 0; i < nClients; i++) {
			try {
				clients[i] = new ASL_ReadWriteClient(host, port, msgSize,
						waitTime, i);
				ids[i] = clients[i].pubId;
			} catch (ASL_Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		for (int i = 0; i < nClients; i++) {
			clients[i].sendId = ids;
			clientThreads[i] = new Thread(clients[i], "ASL client " + i);
			clientThreads[i].start();
		}
		try {
			Thread.sleep(runtime * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < nClients; i++) {
			clients[i].stop();
		}
		for (int i = 0; i < nClients; i++) {
			try {
				clientThreads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}
