package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
//import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ASL_Listener implements Runnable {
	
	private int portNumber;
	private BlockingQueue<Socket> queue;
	private ServerSocket serverSocket;
	
	public boolean listening;
	
	public ASL_Listener(int portNumber) throws IOException{
		this.portNumber = portNumber;
		queue = new LinkedBlockingQueue<Socket>();
		serverSocket = new ServerSocket(portNumber);
	}
	
	public int getPort(){
		return portNumber;
	}
	
	public BlockingQueue<Socket> getQueue(){
		return queue;
	}

	@Override
	public void run() {
		listening = true;
		try{
			while (listening) {
				Socket socket = serverSocket.accept();
				if(!queue.offer(socket)){
					socket.close();
				}
			}
		} catch (SocketException e) {
			// shutdown
		}catch (IOException ex) {
			System.err.println(ex.getLocalizedMessage());
		} 
	}
	
	public void stop() {
		listening = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
