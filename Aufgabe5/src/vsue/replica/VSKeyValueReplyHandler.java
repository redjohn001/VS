package vsue.replica;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface VSKeyValueReplyHandler extends Remote {

	/**
	 * Delivers a reply to the handler.
	 * 
	 * @param reply  The reply to deliver
	 * @throws RemoteException  if a remote-call error occurs
	 */
	public void handleReply(VSKeyValueReply reply) throws RemoteException;
	
}
