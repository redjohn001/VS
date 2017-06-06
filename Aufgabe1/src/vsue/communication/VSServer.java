package vsue.communication;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import vsue.rpc.VSRemoteMethodReply;
import vsue.rpc.VSRemoteMethodRequest;
import vsue.rpc.VSRemoteObjectManager;

public class VSServer implements Runnable{

	public static final int PORT = 5555;

	private ExecutorService socketThreads = null;
	private final ServerSocket serverSocket;
	private boolean running;

	public VSServer(final ServerSocket socket) {
		serverSocket = socket;
		running = false;
	}
	

	@Override
	public void run() {
		if(socketThreads == null){
			 socketThreads = Executors.newCachedThreadPool();
		}
		try {
			running = true;
			// accept socket connections and add them to the executor service that can reuse old threads...
			while (running) {
				final Socket clientSocket = serverSocket.accept();
				socketThreads.execute(new VSServerWorker(clientSocket));
			}
			
		} catch (IOException e) {
			System.err.println(e.getMessage()); 
		} catch(Exception e){
			System.err.println(e.getMessage()); //most likely that a socket connection should be established while shutting down the server 
		}
	}
	
	public void shutdown(){
		running = false;
		if(!socketThreads.isShutdown()){
			socketThreads.shutdown();
			try {
				socketThreads.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {}
			
		}
		socketThreads = null;
		try {
			serverSocket.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	

	private static class VSServerWorker implements Runnable {

		private Socket clientSocket;

		public VSServerWorker(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			VSObjectConnection conn = VSConnectionFactory.newConnection(clientSocket);	
			
			try {
				while (true) {
					
					Serializable object = null;
					
					try{
						object = conn.receiveObject();
					}catch (IOException ex){
						break;
					}
					
					// close connection if requested
					if (object instanceof String && VSObjectConnection.EOT_EOF.equals(object)) {
						break;
					}
					
					// analyze VSRemoteMethodRequest and send reply
					if (object instanceof VSRemoteMethodRequest) {
						VSRemoteMethodRequest request = (VSRemoteMethodRequest)object;
						Serializable reply;
						Object result;
						
						// invoke method
						try {
							result = VSRemoteObjectManager.getInstance().invokeMethod(request.getObjectId(), request.getMethod(), request.getParams());
							reply = new VSRemoteMethodReply(request.getMessageId(), result);
						}
						catch (InvocationTargetException e) {
							reply = new VSRemoteMethodReply(request.getMessageId(),e.getTargetException());
						}
						catch (Throwable e) {
							reply = new VSRemoteMethodReply(request.getMessageId(),e);
						}
						
						// send reply
						if(conn.isConnected())
						conn.sendObject(reply);
					}
					else {
						throw new Exception("Server can't process object of type " + object.getClass() + ". Expecting VSRemoteMethodRequest.");
					}
				}
			} catch (Exception e) {
				//e.printStackTrace();
			} finally {
				//conn.close();//TODO: to check!
				try {
					if (!clientSocket.isClosed()) {
						clientSocket.close();
					}
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
		}

	}
	
	
	public static void main(String args[]) throws IOException {
		new VSServer(new ServerSocket(PORT));
	}



}
