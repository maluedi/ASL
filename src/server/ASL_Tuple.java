package server;

import java.net.Socket;

public class ASL_Tuple {
	
	public final Socket s;
	public final long id;
	
	public ASL_Tuple(Socket s, long id){
		this.s = s;
		this.id = id;
	}

}
