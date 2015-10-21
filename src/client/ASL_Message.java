package client;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Marcel Lüdi
 * 
 * Container for messages retrieved from the message passing system.
 *
 */
public class ASL_Message {

	public int queue, sender, receiver;
	public String message;
	public Date timestamp;

	/**
	 * Creates a new ASL_Message object with the specified properties.
	 * 
	 * @param queue
	 * 			  the queue the message was stored in
	 * @param sender
	 * 			  the id of the sender
	 * @param receiver
	 * 			  the id of the receiver
	 * @param timestamp
	 * 			  the time the message entered the database in milliseconds from January 1, 1970 00:00:00 GMT
	 * @param message
	 * 			  the content of the message
	 */
	public ASL_Message(int queue, int sender, int receiver, long timestamp,
			String message) {
		this.queue = queue;
		this.sender = sender;
		this.receiver = receiver;
		this.timestamp = new Date(timestamp);
		this.message = message;
	}
	
	/**
	 * Creates a new ASL_Message object with the specified properties.
	 * 
	 * @param queue
	 * 			  the queue the message was stored in
	 * @param sender
	 * 			  the id of the sender
	 * @param receiver
	 * 			  the id of the receiver
	 * @param timestamp
	 * 			  the time the message entered the database
	 * @param message
	 * 			  the content of the message
	 */
	public ASL_Message(int queue, int sender, int receiver, Date timestamp,
			String message) {
		this.queue = queue;
		this.sender = sender;
		this.receiver = receiver;
		this.timestamp = timestamp;
		this.message = message;
	}

	/**
	 * Creates a new ASL_Message object with default properties.
	 * Queue, Sender and Receiver are set to 0 and the time stamp is set
	 * to January 1, 1970 00:00:00 GMT. The message is an empty String.
	 */
	public ASL_Message() {
		this(0,0,0,0,"");
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if(receiver != 0){
			return new SimpleDateFormat("dd.MM.yy HH:mm:ss").format(timestamp) + ": \n" + 
					"\tsender:   " + sender + "\n" + 
					"\treceiver: " + receiver + "\n" + 
					"\tqueue:    " + queue + "\n" + 
					"\tmessage:  " + message;
		} else {
			return new SimpleDateFormat("dd.MM.yy HH:mm:ss").format(timestamp) + ": \n" + 
					"\tsender:   " + sender + "\n" + 
					"\tqueue:    " + queue + "\n" + 
					"\tmessage:  " + message;
		}
	}

}
