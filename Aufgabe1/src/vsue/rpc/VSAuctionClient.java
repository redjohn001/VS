package vsue.rpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import vsue.faults.VSBuggyObjectConnection;
import vsue.rmi.VSAuction;
import vsue.rmi.VSAuctionEventHandler;
import vsue.rmi.VSAuctionEventType;
import vsue.rmi.VSAuctionException;
import vsue.rmi.VSAuctionService;

public class VSAuctionClient implements VSAuctionEventHandler {

	/* The user name provided via command line. */
	private final String userName;
	/* the service as remote stub */
	private VSAuctionService service;
	/* this object exported as callback stub */
	private final VSAuctionEventHandler clientHandler;

	public VSAuctionClient(String userName) throws VSRemoteException {
		this.userName = userName;
		try {
			clientHandler = (VSAuctionEventHandler) VSRemoteObjectManager.getInstance().exportObject(this);
		} catch (VSRemoteException ex) {
			System.err.println(ex);
			throw ex;
		}

	}

	// #############################
	// # INITIALIZATION & SHUTDOWN #
	// #############################

	public void init(String registryHost, int registryPort) {
		try {
			Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);
			service = (VSAuctionService) registry.lookup(VSAuctionServer.DEFAULT_SERVICE_NAME);
		} catch (RemoteException e) {
			System.err.println(
					"no registry found with the given hostname '" + registryHost + "' and port " + registryPort);
		} catch (NotBoundException ex) {
			System.err.println("the service is not started or known to the registry");
		}
	}

	public void shutdown() {
		try {
			VSRemoteObjectManager.getInstance().unexportObject(this);
		} catch (RuntimeException e) {
			System.err.println("failed to close client... close by force");
			System.exit(0);
		}
		VSRemoteObjectManager.getInstance().getConnections().shutdown();
	}

	// #################
	// # EVENT HANDLER #
	// #################

	@Override
	public void handleEvent(VSAuctionEventType event, VSAuction auction) {
		System.out.print("Message from auction '" + auction.getName() + "'-->");
		switch (event) {
		case AUCTION_END:
			System.out.println("'Auction ended'");
			break;
		case AUCTION_WON:
			System.out.println("'Auction won'");
			break;
		case HIGHER_BID:
			System.out.println("'Auction outbid'");
			break;
		default:
			System.out.println("unknown notification");
			break;
		}
	}

	// ##################
	// # CLIENT METHODS #
	// ##################

	public void register(String auctionName, int duration, int startingPrice) {
		if (service == null) {
			System.out.println("no service is running");
			return;
		}
		VSAuction auction = new VSAuction(auctionName, userName, startingPrice);
		try {
			service.registerAuction(auction, duration, clientHandler);
		} catch (RemoteException e) {
			System.err.println("no server found to register the auction");
		} catch (VSAuctionException e) {
			System.err.println(e.getMessage());
		}

	}

	public void list() {
		if (service == null) {
			System.out.println("no service is running");
			return;
		}
		try {
			final VSAuction auctions[] = service.getAuctions();
			if (auctions.length > 0) {
				System.out.println("auction\t|\tprice\t|\tcreator\t");
				for (VSAuction auc : auctions) {
					System.out.println(auc.getName() + "\t\t" + auc.getPrice() + "\t\t" + auc.getOwner());
				}
			} else {
				System.out.println("no auctions registered");
			}
		} catch (RemoteException e) {
			System.err.println("auctions could not be resolved");
		}
	}

	public void bid(String auctionName, int price) {
		if (service == null) {
			System.out.println("no service is running");
			return;
		}
		try {
			service.placeBid(userName, auctionName, price, clientHandler);
		} catch (RemoteException e) {
			System.err.println("connection to server lost");

		} catch (VSAuctionException e) {
			System.err.println(e.getMessage());
		}
	}

	// #########
	// # SHELL #
	// #########

	public void shell() {
		// Create input reader and process commands
		BufferedReader commandLine = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			// Print prompt
			System.out.print("> ");
			System.out.flush();

			// Read next line
			String command = null;
			try {
				command = commandLine.readLine();
			} catch (IOException ioe) {
				break;
			}
			if (command == null)
				break;
			if (command.isEmpty())
				continue;

			// Prepare command
			String[] args = command.split(" ");
			if (args.length == 0)
				continue;
			args[0] = args[0].toLowerCase();

			// Process command
			try {
				boolean loop = processCommand(args);
				if (!loop)
					break;
			} catch (IllegalArgumentException iae) {
				System.err.println(iae.getMessage());
			}
		}

		// Close input reader
		try {
			commandLine.close();
		} catch (IOException ioe) {
			// Ignore
		}
	}

	private boolean processCommand(String[] args) {
		switch (args[0]) {
		case "register":
		case "r":
			if (args.length < 3)
				throw new IllegalArgumentException("Usage: register <auction-name> <duration> [<starting-price>]");
			int duration = Integer.parseInt(args[2]);
			int startingPrice = (args.length > 3) ? Integer.parseInt(args[3]) : 0;
			register(args[1], duration, startingPrice);
			break;
		case "list":
		case "l":
			list();
			break;
		case "bid":
		case "b":
			if (args.length < 3)
				throw new IllegalArgumentException("Usage: bid <auction-name> <price>");
			int price = Integer.parseInt(args[2]);
			bid(args[1], price);
			break;
		case "exit":
		case "quit":
		case "x":
		case "q":
			return false;
		case VSBuggyObjectConnection.COMMANDSTRING:
			VSBuggyObjectConnection.commandLineProcessor(args);
			break;
		default:
			throw new IllegalArgumentException("Unknown command: " + args[0]);
		}
		return true;
	}

	// ########
	// # MAIN #
	// ########

	public static void main(String[] args) throws VSRemoteException {
		// Check arguments
		if (args.length < 3) {
			System.err.println("usage: java " + VSAuctionClient.class.getName()
					+ " <user-name> <registry_host> <registry_port>");
			System.exit(1);
		}

		// Create and execute client
		VSAuctionClient client = new VSAuctionClient(args[0]);
		client.init(args[1], Integer.parseInt(args[2]));
		client.shell();
		client.shutdown();
	}

}
