package vsue.rpc;

import java.rmi.RemoteException;

public class VSRemoteException extends RemoteException {

	private static final long serialVersionUID = 1L;

	public VSRemoteException(String message) {
		super(message);
	}
	
}
