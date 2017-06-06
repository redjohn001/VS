package vsue.rmi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class VSAuctionRMIServer {

	public static final String DEFAULT_REGISTRY_URL = "localhost";
	public static final String DEFAULT_SERVICE_NAME = "AuctionBroker";
	public static final int DEFAULT_PORT = 1100;

	private final VSAuctionService remoteServer;
	private final Registry registry;

	public VSAuctionRMIServer() {
		VSAuctionServiceImpl serviceImpl = new VSAuctionServiceImpl();
		try {
			remoteServer = (VSAuctionService) UnicastRemoteObject.exportObject(serviceImpl, DEFAULT_PORT);
		} catch (RemoteException ex) {
			throw new RuntimeException("could not create the service:\n" + ex.getMessage());
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

	}

	public static void main(String[] args) {
		System.out.println("--------starting server-------");
		System.out.print("startup service");
		VSAuctionRMIServer server;
		try {
			 server = new VSAuctionRMIServer();
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
		System.out.print("===================\n>");
		try {
			String input = commandLine.readLine();
			while (!input.equals("exit")) {
				switch (input){
				case "status":
					System.out.println("RUNNING");
					break;
				default:
					System.out.println("unknown command");
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
