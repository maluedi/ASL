package util;

//import java.util.Date;

public class ASL_Message {

	public int queue, sender, receiver;
	public String message;
	public String timestamp;

	public ASL_Message(int queue, int sender, int receiver, String timestamp,
			String message) {
		this.queue = queue;
		this.sender = sender;
		this.receiver = receiver;
		this.timestamp = timestamp;
		this.message = message;
	}

	public String toString() {
		return timestamp + ": \n" + 
				"\tsender:   " + sender + "\n" + 
				"\treceiver: " + receiver + "\n" + 
				"\tqueue:    " + queue + "\n" + 
				"\tmessage:  " + message;
	}

}
