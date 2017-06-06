package vsue.communication;

import java.net.Socket;

import vsue.faults.VSBuggyObjectConnection;
import vsue.rpc.VSRemoteException;
import vsue.rpc.VSRemoteReference;

public class VSConnectionFactory {

	
	
	public static VSObjectConnection newConnection(final Socket socket){
		return new VSObjectConnection(socket);
	}
	
	public static VSObjectConnection newClientConnection(final Socket socket){
		return new VSBuggyObjectConnection(socket);
	}
	

	public static VSObjectConnection newFromReference(final VSRemoteReference ref) throws VSRemoteException{
		try {
		Socket socket = new Socket(ref.getHost(), ref.getPort());
		return newConnection(socket);
		}catch (Exception e){
			throw new VSRemoteException(e.getMessage());
		}
	}

	public static VSObjectConnection newClientFromReference(final VSRemoteReference ref) throws VSRemoteException{
		try {
		Socket socket = new Socket(ref.getHost(), ref.getPort());
		return newClientConnection(socket);
		}catch (Exception e){
			throw new VSRemoteException(e.getMessage());
		}
	}
	
}
