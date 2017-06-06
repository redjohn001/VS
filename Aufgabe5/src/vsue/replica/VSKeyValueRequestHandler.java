package vsue.replica;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface VSKeyValueRequestHandler extends Remote {

	/**
	 * Delivers a request to the handler.
	 * 
	 * @param request  The request to deliver
	 * @throws RemoteException  if a remote-call error occurs
	 */
	public void handleRequest(VSKeyValueRequest request) throws RemoteException;
	
}
