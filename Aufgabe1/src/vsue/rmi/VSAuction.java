package vsue.rmi;

import java.io.Serializable;

public class VSAuction implements Serializable{

	private static final long serialVersionUID = 1L;

	/* The auction name. */
	private final String name;

	/* User the owner of the auction. */
	private final String owner;
	
	/* The currently highest bid for this auction. */
	private int price;
	
	public VSAuction(String name, String owner, int startingPrice) {
		this.name = name;
		this.price = startingPrice;
		this.owner = owner;
	}
	
	
	public String getName() {
		return name;
	}

	public String getOwner() {
		return owner;
	}

	public int getPrice() {
		return price;
	}
	
	public void setPrice(int value) {
		price = value;
	}

	@Override
	public String toString() {
		return name + " | " + owner + " | " + price;
	}
}
