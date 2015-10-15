package client;

import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import util.ASL_Util;

public class ASL_ReadWriteClient implements Runnable{
	
	private ASL_Client client;
	private int msgSize;
	public final int id;
	private final Logger logger;
	public static final char[] alphabet = ("abcdefghijklmnopqrstuvwxyz" + 
											"ABCDEFGHIJKLMNOPQRSTUVWXYZ" + 
											"1234567890" + 
											" !\"#$%&()*+,-./:;<=>?@[\\]^_{|}~").toCharArray();
	public boolean running;
	
	public ASL_ReadWriteClient(String hostName, int portNumber, int msgSize, int id){
		this.client = new ASL_Client(hostName,portNumber);
		this.running = false;
		this.id = id;
		
		this.logger = LogManager.getLogger("ASL Client " + id);
	}
	
	public void run(){
		try{
			Random r = new Random();
			long t0,t1;
			int err;
			client.register();
			int q = client.createQueue();
			running = true;
			while(running){
				char[] tmpm = new char[this.msgSize];
				for(int i=0; i < this.msgSize; i++){
					tmpm[i] = alphabet[r.nextInt(alphabet.length)];
				}
				//logger.trace("start push");
				t0 = System.currentTimeMillis();
				try{
					client.push(q, new String(tmpm));
					err = 0;
				} catch (ASL_Exception e){
					logger.error(e.getLocalizedMessage());
					err = e.errCode;
				}
//				logger.trace("end push");
				t1 = System.currentTimeMillis();
				logger.trace(ASL_Util.PUSH + "," + err + ",[" + t0 + ", " + t1 +"]");
				
				
//				logger.trace("start poll");
				t0 = System.currentTimeMillis();
				try{
					client.poll(q);
					err = 0;
				} catch(ASL_Exception e){
					logger.error(e.getLocalizedMessage());
					err = e.errCode;
				}
//				logger.trace("end poll");
				t1 = System.currentTimeMillis();
				logger.trace(ASL_Util.POLL + "," + err + ",[" + t0 + ", " + t1 +"]");
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
		if (args.length < 5) {
			System.err.println("Usage: ASL_Client <host> <port> <#clients> <msgSize> <runtime[s]>");
			System.exit(-1);
		}
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		int nClients = Integer.parseInt(args[2]);
		int msgSize = Integer.parseInt(args[3]);
		int runtime = Integer.parseInt(args[4]);
		ASL_ReadWriteClient[] clients = new ASL_ReadWriteClient[nClients];
		Thread[] clientThreads = new Thread[nClients];
		for(int i = 0; i < nClients; i++){
			clients[i] = new ASL_ReadWriteClient(host,port,msgSize,i);
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
