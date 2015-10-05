package client;

import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*;

import util.ASL_Exception;
import util.ASL_Message;
import util.ASL_Util;

public class ASL_Client {
	
	private String hostName;
	private int portNumber;
	private int id;
	private DateFormat df;
	
	public ASL_Client(String hostName, int portNumber){
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSXXX");
	}
	
	public void register() throws ASL_Exception{
		try(
			Socket socket = new Socket(hostName,portNumber);
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		) {
			out.writeByte(ASL_Util.RUSER);
			out.flush();
			int err = in.readByte();
			if(err == 0){
				id = in.readInt();
			} else {
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			System.err.println("Unknown host: " + hostName);
		} catch (IOException e1) {
			System.err.println("I/O exception on connection to " + hostName);
		} 
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
	
	public int createQueue() throws ASL_Exception{
		int result = 0;
		try(
			Socket socket = new Socket(hostName,portNumber);
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		) {
			out.writeByte(ASL_Util.CQUEUE);
			out.flush();
			int err = in.readByte();
			if(err == 0){
				result = in.readInt();
			} else {
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			System.err.println("Unknown host: " + hostName);
		} catch (IOException e1) {
			System.err.println("I/O exception on connection to " + hostName);
		} 
		
		return result;
	}
	
	public void deleteQueue(int queue) throws ASL_Exception {
		try(
			Socket socket = new Socket(hostName,portNumber);
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		) {
			out.writeByte(ASL_Util.DQUEUE);
			out.writeInt(queue);
			out.flush();
			int err = in.readByte();
			if(err != 0){
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			System.err.println("Unknown host: " + hostName);
		} catch (IOException e1) {
			System.err.println("I/O exception on connection to " + hostName);
		} 
	}
	
	public void push(int queue, int receiver, String message) throws ASL_Exception{
		try(
			Socket socket = new Socket(hostName,portNumber);
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		) {
			out.writeByte(ASL_Util.PUSH);
			out.writeInt(queue);
			out.writeInt(this.id);
			out.writeInt(receiver);
			out.writeUTF(message);
			out.flush();
			int err = in.readByte();
			if(err != 0){
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			System.err.println("Unknown host: " + hostName);
		} catch (IOException e1) {
			System.err.println("I/O exception on connection to " + hostName);
		} 
	}
	
	public void push(int queue, String message) throws ASL_Exception{
		push(queue, 0, message);
	}
	
	public ASL_Message poll(int queue, int sender) throws ASL_Exception{
		ASL_Message result = null;
		try(
			Socket socket = new Socket(hostName,portNumber);
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		) {
			out.writeByte(ASL_Util.POLL);
			out.flush();
			int err = in.readByte();
			if(err == 0){
				int q = in.readInt();
				int s = in.readInt();
				int r = in.readInt();
				String ts = in.readUTF();
				Date t = null;
				try {
					t = df.parse(ts);
				} catch (ParseException e) {
					System.err.println("Could not parse timestamp: " + ts);
				}
				String m = in.readUTF();
				result = new ASL_Message(q,s,r,t,m);
			} else {
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			System.err.println("Unknown host: " + hostName);
		} catch (IOException e1) {
			System.err.println("I/O exception on connection to " + hostName);
		} 
		
		return result;
	}
	
	public ASL_Message pop(int queue) throws ASL_Exception{
		return poll(queue,0);
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
