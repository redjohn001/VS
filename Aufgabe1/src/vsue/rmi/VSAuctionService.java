package vsue.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface VSAuctionService extends Remote {

	/**
	 * Registers a new auction.
	 *
	 * @param auction   The auction to register
	 *        duration  Time in seconds until the auction ends
	 *        handler   The handler to notify when the auction has ended
	 * @exception  VSAuctionException  if the duration is negative or an auction
	 *                                 with the same name already exists
	 */
	public void registerAuction(VSAuction auction, int duration, VSAuctionEventHandler handler) throws VSAuctionException, RemoteException;

	/**
	 * Retrieves all auctions currently in progress.
	 * 
	 * @return  The auctions currently in progress.
	 */
	public VSAuction[] getAuctions() throws RemoteException;

	/**
	 * Places a new bid.
	 * 
	 * @param userName     The name of the bidder
	 * @param auctionName  The name of the auction
	 * @param price        The bid
	 * @param handler      The handler to notify if another bid is placed
	 *                     that is higher than this bid
	 * @return <tt>true</tt> if the bid has been placed, otherwise <tt>false</tt>
	 * @throws VSAuctionException  if no auction with the specified name is
	 *                             currently in progress 
	 */
	public boolean placeBid(String userName, String auctionName, int price, VSAuctionEventHandler handler) throws VSAuctionException, RemoteException;

}
