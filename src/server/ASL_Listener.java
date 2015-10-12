package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Marcel Lüdi
 * 
 * Listens on a port for incoming client requests and 
 * puts them on a queue for further processing 
 *
 */
public class ASL_Listener implements Runnable {
	
	private int portNumber;
	private BlockingQueue<ASL_Tuple> queue;
	private ServerSocket serverSocket;
	
	private long serial;
	
	public boolean listening;
	
	/**
	 * Creates a new ASL_Listener bound to the specified port number
	 * 
	 * @param portNumber
	 * 			  the port the listener will listen on
	 * @throws IOException
	 * 			  if the port is occupied
	 */
	public ASL_Listener(int portNumber) throws IOException{
		this.portNumber = portNumber;
		queue = new LinkedBlockingQueue<ASL_Tuple>();
		serverSocket = new ServerSocket(portNumber);
		serial = 0;
	}
	
	/**
	 * Returns the port this listener is listening on
	 * 
	 * @return
	 * 			  the port this listener is bound to
	 */
	public int getPort(){
		return portNumber;
	}
	
	/**
	 * Returns the queue where this listener deposits its accepted sockets
	 * 
	 * @return
	 * 			  the queue with the accepted sockets
	 */
	public BlockingQueue<ASL_Tuple> getQueue(){
		return queue;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		listening = true;
		try{
			while (listening) {
				Socket socket = serverSocket.accept();
				if(!queue.offer(new ASL_Tuple(socket,++serial))){
					socket.close();
				}
				System.out.println("accepted request " + serial);
			}
		} catch (SocketException e) {
			// shutdown
		}catch (IOException e) {
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		} 
	}
	
	/**
	 * Stop the listener from listening and terminate the thread
	 */
	public void stop() {
		listening = false;
		try {
			serverSocket.close(); //this gives a SocketException in the run() method if the thread is waiting for a connection
		} catch (IOException e) {
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

}
