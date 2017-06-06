package vsue.rpc;

import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vsue.communication.VSServer;

public class VSConnectionManager {

	public static final int BASE_PORT = 5555;
	private final ExecutorService vsserverHandler = Executors.newCachedThreadPool();
	private final Map<Integer,VSServer> serverConnections = new ConcurrentHashMap<Integer,VSServer>();
	
	public VSConnectionManager() {}
	
	
	public int enableRemoteConnection() throws VSRemoteException{
		try{
		final ServerSocket socket = new ServerSocket(0);
		final VSServer connection = new VSServer(socket);
		serverConnections.put(socket.getLocalPort(),connection);
		vsserverHandler.execute(connection);
		return socket.getLocalPort();
		}catch(Exception e){
			throw new VSRemoteException(e.getMessage());
		}
	}
	
	public void shutdown(){
		vsserverHandler.shutdown();
		for(final VSServer server : serverConnections.values()){
			server.shutdown();
		}
	}
}
