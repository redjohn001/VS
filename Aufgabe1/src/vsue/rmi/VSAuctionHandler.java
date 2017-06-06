package vsue.rmi;

import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class VSAuctionHandler {

	private final Map<String, VSHandledAuction> auctions;
	private final WeakReference<VSAuctionServiceEventHandler> handler;
	private final Lock lock = new ReentrantLock();
	private final Condition auctionSleep = lock.newCondition();
	private boolean running = true;
	private final Runnable timeHandler = new Runnable() {

		@Override
		public void run() {
			while (running) {
				VSAuctionServiceEventHandler resolvedReference = handler.get();
				if (resolvedReference == null) {
					shutdown();
					break;
				}
				lock.lock();
				try {
					long minTime = Long.MAX_VALUE;
					for (final VSHandledAuction auction : auctions.values()) {
						final long time = auction.getRemainingTime();
						if (time <= 0) {
							resolvedReference.fireEvent(auction.getHighestBidder(), VSAuctionEventType.AUCTION_WON,
									auction.getHandledAuction());
							resolvedReference.fireEvent(auction.getOwner(), VSAuctionEventType.AUCTION_END,
									auction.getHandledAuction());
							auctions.remove(auction.getName());
						} else {
							System.out.println("auction '" + auction.getName() + "' remaining time "
									+ TimeUnit.NANOSECONDS.toSeconds(time));
							minTime = Math.min(minTime, time);
						}
					}
					minTime = minTime / 2;
					if (minTime > TimeUnit.MICROSECONDS.toNanos(1)) {
						try {
							auctionSleep.awaitNanos(minTime);
						} catch (InterruptedException e) {
						}
					}
				} finally {
					lock.unlock();
				}
			}
		}
	};

	private final Thread timeWorker = new Thread(timeHandler);

	public VSAuctionHandler(final VSAuctionServiceEventHandler eventHandler) {
		handler = new WeakReference<VSAuctionServiceEventHandler>(eventHandler);
		auctions = new ConcurrentHashMap<String, VSHandledAuction>();
		timeWorker.start();
	}

	public void shutdown() {
		running = false;
		lock.lock();
		try{
			auctionSleep.signalAll();
		}finally{
			lock.unlock();
		}
		
	}

	public void register(final VSAuction auction, final long duration) throws VSAuctionException {
		lock.lock();
		try {
			String auctionName = auction.getName();
			if (auctions.containsKey(auctionName)) {
				throw new VSAuctionException("Auction with name " + auctionName + " exists. Please use another name.");
			}
			auctionSleep.signal();
			auctions.put(auctionName, new VSHandledAuction(auction, duration));
		} finally {
			lock.unlock();
		}
	}

	public VSHandledAuction getAuction(final String auctionName) throws VSAuctionException {
		if (auctions.containsKey(auctionName)) {
			return auctions.get(auctionName);
		}
		throw new VSAuctionException("The auction " + auctionName + " does not exist.");
	}

	public VSAuction[] getAuctions() throws RemoteException {
		List<VSHandledAuction> handledAuctions = new ArrayList<VSHandledAuction>(auctions.values());
		VSAuction values[] = new VSAuction[handledAuctions.size()];
		for (int i = 0; i < values.length; ++i) {
			values[i] = handledAuctions.get(i).getHandledAuction();
		}
		return values;
	}

}
