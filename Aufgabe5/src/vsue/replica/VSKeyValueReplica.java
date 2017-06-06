package vsue.replica;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.protocols.SEQUENCER;
import org.jgroups.stack.ProtocolStack;

public class VSKeyValueReplica extends ReceiverAdapter implements
		VSKeyValueRequestHandler {

	public static final String GROUP_CHANEL_ID = "gruppe1";
	public static final String SERVICE_NAME = "store";

	private final JChannel channel;
	private final Registry rmiRegi;
	private long lastTimestamp = 0;
	private final Map<String, String> kvStore = new ConcurrentHashMap<String, String>();
	private final Map<String, Long> kvTimeStamp = new ConcurrentHashMap<String, Long>();

	public static enum VSOperation {
		PUT, GET, DELETE, EXISTS
	}


	public VSKeyValueReplica(InetSocketAddress address) throws Exception {
		System.out.println(address);
		rmiRegi = LocateRegistry.createRegistry(address.getPort());
		rmiRegi.bind(SERVICE_NAME,
				(VSKeyValueRequestHandler) UnicastRemoteObject.exportObject(
						this, address.getPort()));
		channel = new JChannel();
		ProtocolStack protocolStack = channel.getProtocolStack();
		protocolStack.addProtocol(new SEQUENCER());
		channel.setReceiver(this);
		channel.connect(GROUP_CHANEL_ID);
	}

	public void shutdown() {
		try {
			UnicastRemoteObject.unexportObject(this, true);
		} catch (NoSuchObjectException nsoe) {
			// Ignore
		}
		channel.disconnect();
		channel.close();

	}

	@Override
	public void receive(Message msg) { // <- MessageListener
		final Object msgObj = msg.getObject();
		if (msgObj instanceof VSKeyValueRequest){
			lastTimestamp = 1 + Math.max(lastTimestamp, ((VSKeyValueRequest) msgObj).getTimestamp());
			onNewRequest((VSKeyValueRequest) msgObj);
		}
		else {
			System.err
					.println("could not process message from group " + msgObj);
		}
	}

	public void viewAccepted(View newView) { // <- MembershipListener
		System.out.println("received view " + newView);
	}

	// triggered from jgroups or leader
	public void onNewRequest(final VSKeyValueRequest request) {
		try {
			final String k = request.getKey();
			final VSKeyValueReplyHandler stub = request.getClientStub();
			final int id = request.getId();
			switch (request.getOperation()) {
			case DELETE:
				kvStore.remove(k);
				kvTimeStamp.remove(k);
				break;
			case EXISTS:
				if (kvStore.containsKey(k)) {
					stub.handleReply(new VSKeyValueReply(id, kvTimeStamp.get(k)));
				} else {
					stub.handleReply(new VSKeyValueReply(id, -1L));
				}
				break;
			case GET:
				Object result = new VSKeyValueException(
						"no such key in storage :" + k);
				if (kvStore.containsKey(k)) {
					result = kvStore.get(k);
				}
				stub.handleReply(new VSKeyValueReply(id, result));
				break;
			case PUT:
				kvStore.put(k, request.getValue());
				kvTimeStamp.put(k, lastTimestamp);
				break;
			}
		} catch (RemoteException re) {

		} catch (NullPointerException npe) {

		} catch (Exception e) {

		}

	}

	@Override
	public void handleRequest(VSKeyValueRequest request) throws RemoteException {
		// add to group and wait for leader to decide which is the next request.
		try {
			request.setTimestamp(System.currentTimeMillis());
			channel.send(null, request);
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}

	public void printStore() {
		
		System.out.println("storage values");
		for (Entry<String, String> e : kvStore.entrySet()) {
			System.out.println(e.getKey() + " " + e.getValue());
		}
		
		System.out.println("timestamps");
		for (Entry<String, Long> e : kvTimeStamp.entrySet()) {
			final String ts = new Date(e.getValue()).toString();
			System.out.println(e.getKey() + " " + ts );
		}
		
		

	}

	private static void processCommand(String[] args) {
		switch (args[0]) {
		default:
			System.out.println("unknown command");
		}
	}

	public static void main(String[] args) throws Exception {
		assert args.length == 2 : "usage: vskvr <idx> <host-file>";

		LinkedHashSet<InetSocketAddress> addresses = new LinkedHashSet<InetSocketAddress>();
		List<String> lines = Files.readAllLines(Paths.get(args[1]),
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
		VSKeyValueReplica replica = new VSKeyValueReplica(
				addresses.toArray(new InetSocketAddress[addresses.size()])[Integer
						.parseInt(args[0])]);

		BufferedReader commandLine = new BufferedReader(new InputStreamReader(
				System.in));
		System.out.println("server commands:");
		System.out.println("===================");
		System.out.println("exit");
		System.out.println("status");
		System.out.print("===================\n");
		System.out.println("additional commands:");
		System.out.print("===================\n>");
		System.out.flush();
		try {
			String input = commandLine.readLine();
			while (!input.equals("exit")) {
				switch (input) {
				case "status":
					System.out.println("RUNNING");
					replica.printStore();
					break;
				default:
					processCommand(input.toLowerCase().split(" "));
				}
				System.out.print(">");
				input = commandLine.readLine();
			}
		} catch (Exception e) {
		}

		System.out.print("shutting down server");
		replica.shutdown();
		System.out.println("-->[STOPPED]");
		System.exit(0);
	}
}
