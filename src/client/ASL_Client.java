package client;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.*;

public class ASL_Client {
	
	private String hostName;
	private int portNumber;
	private int id;
	
	public ASL_Client(String hostName, int portNumber){
		this.hostName = hostName;
		this.portNumber = portNumber;
	}
	
	public void register(){
		
	}
	
	public int getId(){
		return this.id;
	}
	
	public void send(String message){
		try(
			Socket socket = new Socket(hostName,portNumber);
//			PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
//			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		) {
			System.out.println("Sending message: " + message);
			out.writeUTF(message); out.flush();
			System.out.println("Received message: " + in.readUTF());
		} catch (UnknownHostException e) {
			System.err.println("Unknown host: " + hostName);
		} catch (IOException e1) {
			System.err.println("I/O exception on connection to " + hostName);
		} 
	}
	
	public int createQueue(){
		return -1;
	}
	
	public boolean deleteQueue(){
		return false;
	}
	
	public boolean push(int queue, String message, int receiver){
		return false;
	}
	
	public boolean push(int queue, String message){
		return push(queue, message, -1);
	}
	
	public String pop(int queue, int sender){
		return "";
	}
	
	public String pop(int queue){
		return pop(queue,-1);
	}
	
	public String peek(int queue, int sender){
		return "";
	}
	
	public String peek(int queue){
		return peek(queue, -1);
	}
	
	public int[] getQueues(){
		return null;
	}

	public static void main(String[] args) {
		if (args.length < 3){
			System.err.println("Usage: ASL_Client <host> <port> <message>");
			System.exit(-1);
		}
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		String message = args[2];
		
		ASL_Client client = new ASL_Client(host,port);
		client.send(message);
	}

}
