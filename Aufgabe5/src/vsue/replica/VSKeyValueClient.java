package vsue.replica;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import vsue.replica.VSKeyValueReplica.VSOperation;

public class VSKeyValueClient implements VSKeyValueReplyHandler {

	/* The addresses of all potential replicas. */
	private final InetSocketAddress[] replicaAddresses;
	private final VSKeyValueRequestHandler replica;
	private final AtomicInteger messageId = new AtomicInteger();
	private final Map<Integer, VSKeyValueReply> reply = new ConcurrentHashMap<Integer, VSKeyValueReply>();

	public VSKeyValueClient(InetSocketAddress[] replicaAddresses)
			throws RemoteException, NotBoundException {
		this.replicaAddresses = replicaAddresses;

		// TODO Aufgabe 5.2 replace with final int replicaNumber = selectRandomReplica();
		final int replicaNumber = 0;//selectRandomReplica();
		replica = (VSKeyValueRequestHandler) LocateRegistry.getRegistry(
				this.replicaAddresses[replicaNumber].getHostName(),
				this.replicaAddresses[replicaNumber].getPort()).lookup(
				VSKeyValueReplica.SERVICE_NAME);
	}

	private int selectRandomReplica() {
		return (int) (System.currentTimeMillis() % (long) this.replicaAddresses.length);
	}

	// #############################
	// # INITIALIZATION & SHUTDOWN #
	// #############################

	public void init() {
		// Export client
		try {
			UnicastRemoteObject.exportObject(this, 0);
		} catch (RemoteException re) {
			System.err.println("Unable to export client: " + re);
			System.err
					.println("The client will not be able to receive replies");
			return;
		}
	}

	public void shutdown() {
		// Unexport client
		try {
			UnicastRemoteObject.unexportObject(this, true);
		} catch (NoSuchObjectException nsoe) {
			// Ignore
		}
	}

	// #################
	// # REPLY HANDLER #
	// #################

	@Override
	public void handleReply(VSKeyValueReply reply) {
		/*
		 * TODO: Handle incoming replies sent by replicas
		 */
		// accept the first reply only
		if (!this.reply.containsKey(reply.getId())) {
			this.reply.put(reply.getId(), reply);
		} else {
			System.out.println("debug: ignored dubplicate reply for id " + reply.getId());
		}
	}

	// ###################
	// # KEY-VALUE STORE #
	// ###################

	public void put(String key, String value) throws RemoteException {
		/*
		 * TODO: Invoke PUT operation
		 */
		final int id = messageId.getAndIncrement();
		replica.handleRequest(new VSKeyValueRequest(id, this, key, value,
				VSOperation.PUT));

	}

	public String get(String key) throws VSKeyValueException, RemoteException {
		/*
		 * TODO: Invoke GET operation
		 */
		final int id = messageId.getAndIncrement();
		replica.handleRequest(new VSKeyValueRequest(id, this, key, null,
				VSOperation.GET));
		while (!reply.containsKey(id)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// nothing
			}
		}
		try {
			return (String) reply.get(id).getResult();
		} catch (VSKeyValueException vke) {
			throw vke;
		} catch (Throwable ex) {
			throw new RemoteException(ex.getMessage());
		}
	}

	public void delete(String key) throws RemoteException {
		/*
		 * TODO: Invoke DELETE operation
		 */
		final int id = messageId.getAndIncrement();
		replica.handleRequest(new VSKeyValueRequest(id, this, key, null, VSOperation.DELETE));
	}

	public long exists(String key) throws RemoteException {
		/*
		 * TODO: Invoke EXISTS operation
		 */
		final int id = messageId.getAndIncrement();
		replica.handleRequest(new VSKeyValueRequest(id, this, key, null, VSOperation.EXISTS));

		while (!reply.containsKey(id)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// nothing
			}
		}
		try {
			return (long) reply.get(id).getResult();
		} catch (Throwable ex) {
			throw new RemoteException(ex.getMessage());
		}
	}

	// #########
	// # SHELL #
	// #########

	public void shell() {
		// Create input reader and process commands
		BufferedReader commandLine = new BufferedReader(new InputStreamReader(
				System.in));
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
			} catch (Exception e) {
				System.err.println("Error: " + e.getMessage());
			}
		}

		// Close input reader
		try {
			commandLine.close();
		} catch (IOException ioe) {
			// Ignore
		}
	}

	private boolean processCommand(String[] args) throws VSKeyValueException,
			RemoteException {
		switch (args[0]) {
		case "put":
		case "p":
			if (args.length < 3)
				throw new IllegalArgumentException("Usage: put <key> <value>");
			put(args[1], args[2]);
			break;
		case "get":
		case "g":
			if (args.length < 2)
				throw new IllegalArgumentException("Usage: get <key>");
			String value = get(args[1]);
			System.out.println(value);
			break;
		case "delete":
		case "del":
		case "d":
			if (args.length < 2)
				throw new IllegalArgumentException("Usage: delete <key>");
			delete(args[1]);
			break;
		case "exists":
		case "e":
			if (args.length < 2)
				throw new IllegalArgumentException("Usage: exists <key>");
			long timestamp = exists(args[1]);
			System.out.println((timestamp < 0) ? "Key not found"
					: "Last modified: " + new Date(timestamp));
			break;
		case "exit":
		case "quit":
		case "x":
		case "q":
			return false;
		default:
			throw new IllegalArgumentException("Unknown command: " + args[0]);
		}
		return true;
	}

	// ########
	// # MAIN #
	// ########

	public static void main(String[] args) throws IOException,
			NotBoundException {
		// Check arguments
		if (args.length < 1) {
			System.err.println("usage: java "
					+ VSKeyValueClient.class.getName()
					+ " <path-to-replica-addresses-file>");
			System.exit(1);
		}

		// Load replica addresses
		LinkedHashSet<InetSocketAddress> addresses = new LinkedHashSet<InetSocketAddress>();
		List<String> lines = Files.readAllLines(Paths.get(args[0]),
				Charset.defaultCharset());
		for (String line : lines) {
			// Skip empty lines and comments
			if (line.isEmpty())
				continue;
			if (line.startsWith("#"))
				continue;

			// Parse line
			try {
				String[] parts = line.split(":");
				InetSocketAddress address = new InetSocketAddress(parts[0],
						Integer.parseInt(parts[1]));
				addresses.add(address);
			} catch (Exception e) {
				System.err.println("Ignore line \"" + line + "\" (" + e + ")");
			}
		}
		InetSocketAddress[] replicaAddresses = addresses
				.toArray(new InetSocketAddress[addresses.size()]);

		// Create and execute client
		VSKeyValueClient client = new VSKeyValueClient(replicaAddresses);
		client.init();
		client.shell();
		client.shutdown();
	}

}
