package util;

public class ASL_Util {
	
	// commands
	public static final int PUSH = 1;
	public static final int POLL = 2;
	public static final int PEEK = 3;
	public static final int CQUEUE = 4;
	public static final int DQUEUE = 5;
	public static final int GQUEUE = 6;
	public static final int GMSG = 7;
	public static final int RUSER = 8;
	
	//error codes
	public static final int INTERNAL_ERROR = 0;
	public static final int QUEUE_DOES_NOT_EXIST = 1;
	public static final int SENDER_DOES_NOT_EXIST = 2;
	public static final int RECEIVER_DOES_NOT_EXIST = 3;
	public static final int QUEUE_IS_EMPTY = 4;
	public static final int NO_MESSAGE_FROM_SENDER = 5;
}
