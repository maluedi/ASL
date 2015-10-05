package util;

public class ASL_Exception extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int errCode;

	public ASL_Exception(int errCode) {
		this.errCode = errCode;
	}
	
	@Override
	public String getLocalizedMessage(){
		String msg = "";
		switch(errCode){
		case 0:
			msg = "Internal Error"; break;
		case 1:
			msg = "Queue does not exist"; break;
		case 2:
			msg = "Sender does not exist"; break;
		case 3:
			msg = "Receiver does not exist"; break;
		case 4:
			msg = "Queue is empty"; break;
		}
		return msg;
	}

}
