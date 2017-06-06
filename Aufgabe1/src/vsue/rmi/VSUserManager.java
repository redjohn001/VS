package vsue.rmi;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VSUserManager {

	private final Map<String, VSAuctionEventHandler> userLookup;
	
	public VSUserManager() {
		userLookup = new ConcurrentHashMap<String, VSAuctionEventHandler>();
	}
	
	public void register(final String userName, final VSAuctionEventHandler handler){
		userLookup.put(userName, handler);
	}
	
	public void handleEvent(final String userName,final VSAuctionEventType event,final VSAuction auction){
		if (userName != null && userLookup.containsKey(userName)) {
			try {
				userLookup.get(userName).handleEvent(event, auction);
			} catch (RemoteException e) {
				userLookup.remove(userName);
			}
		}
	}
	
}
