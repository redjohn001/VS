package vsue.rmi;

import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

public class VSAuctionServiceImpl implements VSAuctionService {

	
	private final VSUserManager userManager = new VSUserManager();
	private final VSAuctionServiceEventHandler eventHandler = new VSAuctionServiceEventHandler(userManager);
	private final VSAuctionHandler auctionHandler = new VSAuctionHandler(eventHandler);
	

	public void shutdown () {
		auctionHandler.shutdown();
		eventHandler.shutdown();
	}
	

	@Override
	public void registerAuction(final VSAuction auction, final int duration, final VSAuctionEventHandler handler)
			throws VSAuctionException, RemoteException {
		// auction empty
		if (auction == null) {
			throw new VSAuctionException("Invalid auction");
		}
		auctionHandler.register(auction, TimeUnit.SECONDS.toNanos(duration));
		userManager.register(auction.getOwner(), handler);
	}

	@Override
	public VSAuction[] getAuctions() throws RemoteException {
		return auctionHandler.getAuctions();
	}

	@Override
	public boolean placeBid(final String userName, final String auctionName, final int price,
			final VSAuctionEventHandler handler) throws VSAuctionException, RemoteException {

		// get auction
		final VSHandledAuction auctionHandle = auctionHandler.getAuction(auctionName);
		final VSAuction auction = auctionHandle.getHandledAuction();
		if (auction.getOwner().equals(userName)){
			throw new VSAuctionException("can not place a bid for own auction");
		}
		if (auction.getPrice() < price) {
			// notify old highest bidder that there is a new highest bidder
			final String previousHighest = auctionHandle.getHighestBidder();
			eventHandler.fireEvent(previousHighest, VSAuctionEventType.HIGHER_BID, auction);
			// set new highest bidder
			auction.setPrice(price);
			auctionHandle.setHighestBidder(userName);
			userManager.register(userName, handler);
			return true;
		} else {
			return false;
		}

	}
}
