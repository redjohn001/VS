package vsue.rpc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import vsue.faults.VSBuggyObjectConnection;
import vsue.rmi.VSAuctionService;
import vsue.rmi.VSAuctionServiceImpl;

public class VSAuctionServer {

	public static final String DEFAULT_REGISTRY_URL = "localhost";
	public static final String DEFAULT_SERVICE_NAME = "AuctionBroker";
	public static final int DEFAULT_PORT = 1100;

	private final VSAuctionService remoteServer;
	private final Registry registry;

	public VSAuctionServer() {
		VSAuctionServiceImpl serviceImpl = new VSAuctionServiceImpl();
		
		try {
			//remoteServer = (VSAuctionService) UnicastRemoteObject.exportObject(serviceImpl, DEFAULT_PORT);
			remoteServer = (VSAuctionService) VSRemoteObjectManager.getInstance().exportObject(serviceImpl);
			
		} catch (Exception ex) {
			throw new RuntimeException("could not create the service:\nException message was:" + ex.getMessage());
		}
		try {
			registry = LocateRegistry.createRegistry(DEFAULT_PORT);
			registry.bind(DEFAULT_SERVICE_NAME, remoteServer);
		} catch (RemoteException e) {
			throw new RuntimeException("unexpected error happened while setting up the server");
		} catch (AlreadyBoundException e) {
			throw new RuntimeException("there is already a service with the name '" + DEFAULT_SERVICE_NAME + "'");
		}
	}

	public void shutdown() {
		try {
			registry.unbind(DEFAULT_SERVICE_NAME);
		} catch (RemoteException e) {
			System.err.println("shutting down service failed");
			System.out.println(e);
		} catch (NotBoundException e) {
			System.err.println("no such service");
		}
		VSRemoteObjectManager.getInstance().getConnections().shutdown();
	}

	
	private static void processCommand(String[] args){
		switch (args[0]){
		case VSBuggyObjectConnection.COMMANDSTRING:
			VSBuggyObjectConnection.commandLineProcessor(args);
			break;
		default:
			System.out.println("unknown command");
		}
		
		
		
	}
	
	
	public static void main(String[] args) {
		System.out.println("--------starting server-------");
		System.out.print("startup service");
		VSAuctionServer server;
		try {
			 server = new VSAuctionServer();
			 System.out.println("-->[RUNNING]");
		} catch (RuntimeException ex) {
			System.out.println("-->[FAILED]");
			System.err.println(ex.getMessage());
			System.exit(0);
			return;//to prevent uninitialized variable 'server' error
		}

		BufferedReader commandLine = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("server commands:");
		System.out.println("===================");
		System.out.println("exit");
		System.out.println("status");
		System.out.print("===================\n");
		System.out.println("additional commands:");
		System.out.println("conn-prop <behavior> <prop>");
		System.out.print("===================\n>");
		try {
			String input = commandLine.readLine();
			while (!input.equals("exit")) {
				switch (input){
				case "status":
					System.out.println("RUNNING");
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
		server.shutdown();
		System.out.println("-->[STOPPED]");
		System.exit(0);
	}

}
