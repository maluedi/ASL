package util;

/**
 * @author Marcel Lüdi
 * 
 * Container for messages retrieved from the message passing system 
 *
 */
public class ASL_Message {

	public int queue, sender, receiver;
	public String message;
	public String timestamp;

	/**
	 * Creates a new ASL_Message object with the specified properties
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
	public ASL_Message(int queue, int sender, int receiver, String timestamp,
			String message) {
		this.queue = queue;
		this.sender = sender;
		this.receiver = receiver;
		this.timestamp = timestamp;
		this.message = message;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return timestamp + ": \n" + 
				"\tsender:   " + sender + "\n" + 
				"\treceiver: " + receiver + "\n" + 
				"\tqueue:    " + queue + "\n" + 
				"\tmessage:  " + message;
	}

}
