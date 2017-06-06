package vsue.rmi;

public class VSHandledAuction {

	
	private final VSAuction handledAuction;
	private transient final long timestamp;
	private transient final long duration;
	private transient String highestBidder;
	
	public VSHandledAuction(final VSAuction auction, final long duration) {
		handledAuction = auction;
		this.duration = duration;
		timestamp = System.nanoTime();
		highestBidder = null;
	}

	public String getHighestBidder() {
		return highestBidder;
	}

	public void setHighestBidder(String highestBidder) {
		this.highestBidder = highestBidder;
	}

	public VSAuction getHandledAuction() {
		return handledAuction;
	}

	public String getOwner() {
		return handledAuction.getOwner();
	}
	
	public String getName() {
		return handledAuction.getName();
	}
	
	
	public long getRemainingTime(){
		return duration - (System.nanoTime() - timestamp);
	}
}
