package client;

import java.net.Socket;
import java.net.UnknownHostException;
//import java.text.DateFormat;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
import java.io.*;

import util.ASL_Util;

/**
 * @author Marcel L�di
 * 
 *         Generates requests for the middle ware
 *
 */
public class ASL_Client {

	private String hostName;
	private int portNumber;
	private int id;

	/**
	 * Creates a new ASL_Client connected to the host at the specified host name
	 * and port
	 * 
	 * @param hostName
	 *            the ip of the host
	 * @param portNumber
	 *            the port of the host
	 */
	public ASL_Client(String hostName, int portNumber) {
		this.hostName = hostName;
		this.portNumber = portNumber;
	}

	/**
	 * Register a new user in the message passing system
	 * 
	 * @return the newly generated id
	 * @throws ASL_Exception
	 *             if there is an error while generating the id
	 */
	public int register() throws ASL_Exception {
		try (
			Socket socket = new Socket(hostName, portNumber);
			DataInputStream in = new DataInputStream(
					new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(socket.getOutputStream()));
		) { // request
			out.writeByte(ASL_Util.REGISTER_USER); // register new user
			out.flush();
			int err = in.readByte();
			if (err == 0) { // response
				id = in.readInt(); // user id
			} else {
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			//System.err.println("Unknown host: " + hostName);
		} catch (IOException e) {
			e.printStackTrace();
			//System.err.println("I/O exception on connection to " + hostName);
		}
		return id;
	}

	/**
	 * @return the id used by the client (generated with register())
	 */
	public int getId() {
		return this.id;
	}
	
	/**
	 * Use on own risk! does not check if the user actually exists in the system.
	 * @param id
	 * 			  the new id of this client
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Creates a new queue in the message passing system
	 * 
	 * @return the id of the new queue
	 * @throws ASL_Exception
	 *             if the queue could not be created
	 */
	public int createQueue() throws ASL_Exception {
		int result = 0;
		try (
			Socket socket = new Socket(hostName, portNumber);
			DataInputStream in = new DataInputStream(
					new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(socket.getOutputStream()));
		) { // request:
			out.writeByte(ASL_Util.CREATE_QUEUE); // create queue
			out.flush();
			int err = in.readByte();
			if (err == 0) { // response
				result = in.readInt(); // queue id
			} else {
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			//System.err.println("Unknown host: " + hostName);
		} catch (IOException e) {
			e.printStackTrace();
			//System.err.println("I/O exception on connection to " + hostName);
		}

		return result;
	}

	/**
	 * Delete a queue from the message passing system
	 * 
	 * @param queue
	 *            the id of the queue to be deleted
	 * @throws ASL_Exception
	 *             if the queue does not exist or could not be deleted
	 */
	public void deleteQueue(int queue) throws ASL_Exception {
		try (
			Socket socket = new Socket(hostName, portNumber);
			DataInputStream in = new DataInputStream(
					new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(socket.getOutputStream()));
		) { // request:
			out.writeByte(ASL_Util.DELETE_QUEUE); // delete queue
			out.writeInt(queue); // queue
			out.flush();
			int err = in.readByte();
			if (err != 0) { // no response
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			//System.err.println("Unknown host: " + hostName);
		} catch (IOException e) {
			e.printStackTrace();
			//System.err.println("I/O exception on connection to " + hostName);
		}
	}

	/**
	 * Push a message on a queue to a particular receiver
	 * 
	 * @param queue
	 *            the id of the receiving queue
	 * @param receiver
	 *            the user id of the receiver
	 * @param message
	 *            the content of the message
	 * @throws ASL_Exception
	 *             if the queue or the receiver does not exist or the message
	 *             could not be pushed for other reasons
	 */
	public void push(int queue, int receiver, String message)
			throws ASL_Exception {
		try (
			Socket socket = new Socket(hostName, portNumber);
			DataInputStream in = new DataInputStream(
					new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(socket.getOutputStream()));
		) { // request:
			out.writeByte(ASL_Util.PUSH); // push
			out.writeInt(queue); // queue
			out.writeInt(this.id); // sender
			out.writeInt(receiver); // receiver
			out.writeUTF(message); // message
			out.flush();
			int err = in.readByte();
			if (err != 0) { // no response
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			//System.err.println("Unknown host: " + hostName);
		} catch (IOException e) {
			e.printStackTrace();
			//System.err.println("I/O exception on connection to " + hostName);
		}
	}

	/**
	 * Push a message on a queue without specifying a receiver
	 * 
	 * @param queue
	 *            the id of the receiving queue
	 * @param message
	 *            the content of the message
	 * @throws ASL_Exception
	 *             if the queue or the receiver does not exist or the message
	 *             could not be pushed for other reasons
	 */
	public void push(int queue, String message) throws ASL_Exception {
		push(queue, 0, message);
	}

	/**
	 * request a message from a queue sent by the specified user and delete it
	 * from the queue
	 * 
	 * @param queue
	 *            the id of the queue
	 * @param sender
	 *            the user id of the sender
	 * @return the message returned from the queue
	 * @throws ASL_Exception
	 *             if the queue or the sender does not exist, or if there is no
	 *             message from this sender
	 */
	public ASL_Message poll(int queue, int sender) throws ASL_Exception {
		ASL_Message result = null;
		try (
			Socket socket = new Socket(hostName, portNumber);
			DataInputStream in = new DataInputStream(
					new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(socket.getOutputStream()));
		) { // request:
			out.writeByte(ASL_Util.POLL); // poll
			out.writeInt(queue); // queue
			out.writeInt(sender); // sender
			out.writeInt(this.id); // receiver
			out.flush();
			int err = in.readByte();
			if (err == 0) { // response:
				int q = in.readInt(); // queue
				int s = in.readInt(); // sender
				int r = in.readInt(); // receiver
				long t = in.readLong(); // time stamp
				String m = in.readUTF(); // message
				result = new ASL_Message(q, s, r, t, m);
			} else {
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			//System.err.println("Unknown host: " + hostName);
		} catch (IOException e) {
			e.printStackTrace();
			//System.err.println("I/O exception on connection to " + hostName);
		}

		return result;
	}

	/**
	 * request a message from a queue and delete it from the queue
	 * 
	 * @param queue
	 *            the id of the queue
	 * @return the message returned from the queue
	 * @throws ASL_Exception
	 *             if the queue does not exist or the queue is empty
	 */
	public ASL_Message poll(int queue) throws ASL_Exception {
		return poll(queue, 0);
	}

	/**
	 * request a message from a queue sent by a particular user without deleting
	 * it from the queue
	 * 
	 * @param queue
	 *            the id of the queue
	 * @param sender
	 *            the user id of the sender
	 * @return the message returned from the queue
	 * @throws ASL_Exception
	 *             if the queue or the sender does not exist, or if there is no
	 *             message from this sender
	 */
	public ASL_Message peek(int queue, int sender) throws ASL_Exception {
		ASL_Message result = null;
		try (
			Socket socket = new Socket(hostName, portNumber);
			DataInputStream in = new DataInputStream(
					new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(socket.getOutputStream()));
		) { // request:
			out.writeByte(ASL_Util.PEEK); // peek
			out.writeInt(queue); // queue
			out.writeInt(sender); // sender
			out.writeInt(this.id); // receiver
			out.flush();
			int err = in.readByte();
			if (err == 0) { // response:
				int q = in.readInt(); // queue
				int s = in.readInt(); // sender
				int r = in.readInt(); // receiver
				long t = in.readLong(); // time stamp
				String m = in.readUTF(); // message
				result = new ASL_Message(q, s, r, t, m);
			} else {
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			//System.err.println("Unknown host: " + hostName);
		} catch (IOException e) {
			e.printStackTrace();
			//System.err.println("I/O exception on connection to " + hostName);
		}

		return result;
	}

	/**
	 * request a message from a queue without deleting it from the queue
	 * 
	 * @param queue
	 *            the id of the queue
	 * @return the message returned from the queue
	 * @throws ASL_Exception
	 *             if the queue does not exist or the queue is empty
	 */
	public ASL_Message peek(int queue) throws ASL_Exception {
		return peek(queue, 0);
	}

	/**
	 * Query for queues where messages for this client are waiting
	 * 
	 * @return the id's of the queues with messages for this client
	 * @throws ASL_Exception
	 *             if there is a problem retrieving the queues
	 */
	public int[] getQueues() throws ASL_Exception {
		int[] result = null;
		try (
			Socket socket = new Socket(hostName, portNumber);
			DataInputStream in = new DataInputStream(
					new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(socket.getOutputStream()));
		) { // request:
			out.writeByte(ASL_Util.GET_QUEUES); // get queues with messages for
												// receiver
			out.writeInt(this.id); // receiver
			out.flush();
			int err = in.readByte();
			if (err == 0) { // response:
				int n = in.readInt(); // number of queues
				result = new int[n];
				for (int i = 0; i < n; i++) {
					result[i] = in.readInt(); // queue id
				}
			} else {
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
//			System.err.println("Unknown host: " + hostName);
		} catch (IOException e) {
			e.printStackTrace();
//			System.err.println("I/O exception on connection to " + hostName);
		}

		return result;
	}

	/**
	 * Query for a message from a particular sender
	 * 
	 * @param sender
	 *            the user id of the sender
	 * @return the message from the sender
	 * @throws ASL_Exception
	 *             if there is no message from this sender of there is is a
	 *             problem retrieving the message
	 */
	public ASL_Message getMessage(int sender) throws ASL_Exception {
		ASL_Message result = null;
		try (
			Socket socket = new Socket(hostName, portNumber);
			DataInputStream in = new DataInputStream(
					new BufferedInputStream(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(socket.getOutputStream()));
		) { // request:
			out.writeByte(ASL_Util.GET_MESSAGE); // get message from sender for
													// receiver
			out.writeInt(sender); // sender
			out.writeInt(this.id); // receiver
			out.flush();
			int err = in.readByte();
			if (err == 0) { // response:
				int q = in.readInt(); // queue
				int s = in.readInt(); // sender (=sender from before) //
				int r = in.readInt(); // receiver (=receiver from before)
										// //these two could be omitted
				long t = in.readLong(); // time stamp
				String m = in.readUTF(); // message
				result = new ASL_Message(q, s, r, t, m);
			} else {
				throw new ASL_Exception(err);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			//System.err.println("Unknown host: " + hostName);
		} catch (IOException e) {
			e.printStackTrace();
			//System.err.println("I/O exception on connection to " + hostName);
		}

		return result;
	}

}
