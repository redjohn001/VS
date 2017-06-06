package vsue.rmi;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VSAuctionServiceEventHandler {
	private final ExecutorService worker = Executors.newCachedThreadPool();
	private final WeakReference<VSUserManager> manager; //not the owner of this reference
	
	public VSAuctionServiceEventHandler(final VSUserManager manager) {
		this.manager = new WeakReference<VSUserManager>(manager);
	}
	
	public void fireEvent(final String handlerId, final VSAuctionEventType type,final VSAuction auction){
		System.out.println("handle event for "+handlerId + " of type "+type+ " for auction: "+ auction.getName());
		Runnable task = new Runnable() {
			@Override
			public void run() {
				final VSUserManager resolvedManager = manager.get();
				if(resolvedManager != null){
					resolvedManager.handleEvent(handlerId, type, auction);
				}
			}
		};
		worker.submit(task);
	}
	
	public void shutdown ( ){
		worker.shutdown();
	}
}
