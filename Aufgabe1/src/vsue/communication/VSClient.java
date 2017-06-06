package vsue.communication;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

import vsue.rmi.VSAuction;
import vsue.rpc.VSRemoteException;

public class VSClient {

	private Socket socket;

	public static void main(String[] args) throws VSRemoteException {
		String host = args[0];
		int port = Integer.parseInt(args[1]);

		new VSClient(host, port);
	}

	public VSClient(String host, int port) throws VSRemoteException {

		try {

			socket = new Socket(host, port);

			VSObjectConnection objConn = new VSObjectConnection(socket);

			// Daten für die Übertragung
			Integer integer = 1337;
			String string = "Hello VS!";
			VSAuction auction = new VSAuction("Luft", "Horst", 100);
			Integer[] integerArr = { 1, 3, 3, 7 };
			String[] stringArr = { "Hello", "VS", "!" };
			VSAuction[] auctionArr = { new VSAuction("Luft", "Horst", 100), new VSAuction("Erde", "Gott", 99) };

			// Daten in ein gemeinsames Array packen
			Serializable[] objArr = { integer, string, auction, integerArr, stringArr, auctionArr };

			// Senden und Empfangen der Beispiele
			for (Serializable obj : objArr) {
				
				// Senden
				System.out.println("Senden: " + obj);
				objConn.sendObject(obj);
				
				// Empfangen
				Object out = (Object) objConn.receiveObject();
				System.out.print("Empangen: ");
//				if (out instanceof Array) {
//					Arrays.toString((Object[]) out);
//
//					x = (Array) out;
//					//for (int i = 0; i < x.)
//					Object o = x.;
//					for (Object o : x) {
//						System.out.println(" - " + o);
//					}
//				} else {
					 System.out.println(out);
//				}
				
				System.out.println("###########################");
			}

			objConn.close();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
