package server;

import java.io.IOException;
import java.net.Socket;

public class ASL_Tuple {
	
	public final Socket s;
	public final long rcvTime;
	
	public ASL_Tuple(Socket s, long rcvTime){
		this.s = s;
		this.rcvTime = rcvTime;
	}
	
	public void finish() throws IOException{
		this.s.close();
	}

}
