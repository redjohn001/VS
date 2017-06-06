package vsue.rpc;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketTimeoutException;

import vsue.communication.VSConnectionFactory;
import vsue.communication.VSObjectConnection;
import vsue.rmi.VSAuctionException;

public class VSInvocationHandler implements InvocationHandler, Serializable {

	private static final long serialVersionUID = 1L;

	public static final int MAX_MESSAGES = 3;
	private final VSRemoteReference reference;

	public VSInvocationHandler(final VSRemoteReference remoteReference) {
		reference = remoteReference;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		// Aufgabe 2.4 - Hole Remote-Objekt, falls verfügbar
		if (args != null) {
			final VSRemoteObjectManager remoteObjectManager = VSRemoteObjectManager.getInstance();

			for (int i = 0; i < args.length; i++) {
				args[i] = remoteObjectManager.getStubIfExported(args[i]);
			}
		}

	
		int messageIdCounter = 0;
				
			
		VSObjectConnection objConn = null;
		VSRemoteMethodReply reply = null;
		//create a client factory that might be buggy
		objConn = VSConnectionFactory.newClientFromReference(reference); //here a new socket is created.
			
		do {
			System.out.println("Send + receive");
			final Socket s = objConn.getSocket();
			messageIdCounter++;
			//set the socket time out to an increasing time.
			s.setSoTimeout(messageIdCounter * 500);
			reply = sendAndReceive(objConn, messageIdCounter, method, args); // do the last-of-many send		
		}
		while (reply == null && messageIdCounter < MAX_MESSAGES); //while we have no reply retry
		
		if (reply == null) {
			throw new VSRemoteException("No respone after " + MAX_MESSAGES + " trials.");
		}
		
		
		
		// Verbindung beenden; könnte in einen finally-Block
		objConn.close();
		

		return reply.getReply();
	}
	
	private VSRemoteMethodReply sendAndReceive(VSObjectConnection objConn, int messageIdCounter, Method method, Object[] args) throws VSAuctionException, VSRemoteException {
		// send object
		objConn.sendObject(new VSRemoteMethodRequest(messageIdCounter, reference.getObjectId(), method.toGenericString(), args));

		// try to get a response
		try{
			Object response = null;
			while (true){
				final long start = System.currentTimeMillis();
				response = objConn.receiveObject(); //get object
				final long end = System.currentTimeMillis();
				//if the reply was received before the time-out change the timeout of the socket to the remaining time.
				objConn.getSocket().setSoTimeout(Math.max(1,(int) (objConn.getSocket().getSoTimeout() - (end - start))));
				
				VSRemoteMethodReply reply = null;
				
				if (response instanceof VSRemoteMethodReply) {
					reply = (VSRemoteMethodReply) response;
				} else {
					throw new VSRemoteException("Server response is incorrect.");
				}
				if(messageIdCounter == reply.getMessageId()){
					//if the message has the same id -> correct reply
					objConn.getSocket().close();
					return reply;
				}
				//else retry with the remaining socket timeout. 
			}
			
		}
		catch(IOException e) {
			//if there was a socketTimeoutException or any other IOException return null as this states that there was no result in the given time.
			return null;
		}
		catch(Exception e){
			//everything else results in a remote exception
			throw new VSRemoteException(e.getMessage());
		}
	}
	
	
}
