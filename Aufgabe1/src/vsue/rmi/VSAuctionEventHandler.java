package vsue.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface VSAuctionEventHandler extends Remote {
	
	/**
	 * Notifies the event handler about an auction event.
	 * 
	 * @param event    The type of the event
	 * @param auction  The auction
	 */
	public void handleEvent(VSAuctionEventType event, VSAuction auction) throws RemoteException;
	
}
