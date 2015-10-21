package client;

import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import util.ASL_Util;

public class ASL_ReadWriteClient implements Runnable{
	
	private ASL_Client client;
	private int msgSize;
	private long waitTime;
	public final int id;
	
	public final int pubId;
	public final int pubQ;
	
	public int sendId;
	public int sendQ;
	
	private final Logger logger;
	private final Logger traceLogger;
	public static final char[] alphabet = ("abcdefghijklmnopqrstuvwxyz" + 
											"ABCDEFGHIJKLMNOPQRSTUVWXYZ" + 
											"1234567890" + 
											" !\"#$%&()*+,-./:;<=>?@[\\]^_{|}~").toCharArray();
	public boolean running;
	
	public ASL_ReadWriteClient(String hostName, int portNumber, int msgSize, long waitTime, int id) throws ASL_Exception{
		this.client = new ASL_Client(hostName,portNumber);
		this.running = false;
		this.id = id;
		this.msgSize = msgSize;
		this.waitTime = waitTime;
		
		this.logger = LogManager.getLogger("ASL Client " + id);
		this.traceLogger = LogManager.getLogger("trace");
		
		this.pubId = client.register();
		this.pubQ = client.createQueue();
		
		this.sendId = this.pubId;
		this.sendQ = this.pubQ;
	}
	
	public void run(){
		try{
			Random r = new Random();
			int err;
			running = true;
			long t0, t1 = System.currentTimeMillis();
			long effectiveWaitTime;
			char[] tmpm = new char[this.msgSize];
			while(running){
				for(int i=0; i < this.msgSize; i++){
					tmpm[i] = alphabet[r.nextInt(alphabet.length)];
				}
				
				effectiveWaitTime = waitTime - (System.currentTimeMillis()-t1);
				if(effectiveWaitTime > 0){
					Thread.sleep(effectiveWaitTime);
				} else {
					logger.info("wait time too long: " + (waitTime-effectiveWaitTime));
				}
				
				//logger.info("push");
				t0 = System.currentTimeMillis();
				try{
					client.push(sendQ, new String(tmpm));
					err = 0;
				} catch (ASL_Exception e){
					logger.error(e.getLocalizedMessage());
					err = e.errCode;
				}
//				logger.trace("end push");
				t1 = System.currentTimeMillis();
				traceLogger.trace(ASL_Util.PUSH + "," + err + "," + t0 + "," + t1);
				
				effectiveWaitTime = waitTime - (System.currentTimeMillis()-t1);
				if(effectiveWaitTime > 0){
					Thread.sleep(effectiveWaitTime);
				} else {
					logger.info("wait time too long: " + (waitTime-effectiveWaitTime));
				}
				//logger.info("poll");
				t0 = System.currentTimeMillis();
				try{
					client.poll(pubQ);
					err = 0;
				} catch(ASL_Exception e){
					logger.error(e.getLocalizedMessage());
					err = e.errCode;
				}
//				logger.trace("end poll");
				t1 = System.currentTimeMillis();
				traceLogger.trace(ASL_Util.POLL + "," + err + "," + t0 + "," + t1);
			}
		} catch(Exception e){
			//e.printStackTrace();
			logger.error(e.getLocalizedMessage());
		}
	}
	
	public void stop(){
		running = false;
	}
	
	public static void main(String[] args){
		if (args.length < 6) {
			System.err.println("Usage: ASL_Client <host> <port> <#clients> <msgSize[Bytes]> <waitTime[ms]> <runtime[s]>");
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
		for(int i = 0; i < nClients; i++){
			try {
				clients[i] = new ASL_ReadWriteClient(host,port,msgSize,waitTime,i);
			} catch (ASL_Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		for(int i = 0; i < nClients; i++){
			clients[i].sendId = clients[(i+1)%nClients].pubId;
			clients[i].sendQ = clients[(i+1)%nClients].pubQ;
			clientThreads[i] = new Thread(clients[i],"ASL client "+i);
			clientThreads[i].start();
		}
		try {
			Thread.sleep(runtime*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for(int i = 0; i < nClients; i++){
			clients[i].stop();
		}
		for(int i = 0; i < nClients; i++){
			try {
				clientThreads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
}
