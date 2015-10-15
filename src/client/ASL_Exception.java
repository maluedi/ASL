package client;

import util.ASL_Util;

/**
 * @author Marcel Lüdi
 * 
 *         Exception for the different failure states of the message passing
 *         system 
 *
 */
public class ASL_Exception extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final int errCode;

	/**
	 * Creates a new Exception with the specified error code
	 * 
	 * @param errCode
	 * 			  the code of the exception type
	 */
	public ASL_Exception(int errCode) {
		this.errCode = errCode;
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#getLocalizedMessage()
	 */
	@Override
	public String getLocalizedMessage() {
		String msg = "";
		switch (errCode) {
		case ASL_Util.QUEUE_DOES_NOT_EXIST:
			msg = "Queue does not exist";
			break;
		case ASL_Util.SENDER_DOES_NOT_EXIST:
			msg = "Sender does not exist";
			break;
		case ASL_Util.RECEIVER_DOES_NOT_EXIST:
			msg = "Receiver does not exist";
			break;
		case ASL_Util.QUEUE_IS_EMPTY:
			msg = "Queue is empty";
			break;
		case ASL_Util.NO_MESSAGE_FROM_SENDER:
			msg = "No message from this sender";
			break;
		case ASL_Util.INTERNAL_ERROR:
			msg = "Internal Error";
			break;
		}
		return msg;
	}

}
