package vsue.communication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

public class VSObjectConnection {

	private VSConnection connection;
	private Socket socket;
	public static final String EOT_EOF = "ende-aus-vorbei";

	public VSObjectConnection(Socket socket) {
		this.socket = socket;
		connection = new VSConnection(socket);
	}

	public void sendObject(Serializable object) {

		try (ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(byteOutput)) {

			oos.writeObject(object);
			final byte[] chunk = byteOutput.toByteArray();

			connection.sendChunk(chunk);

		} catch (IOException e) {
			//e.printStackTrace();
			throw new RuntimeException("sending objects failed");
		}

	}

	public Serializable receiveObject() throws IOException {

		Serializable object = null;

		try {
			final byte[] chunk = connection.receiveChunk();

			try(ByteArrayInputStream byteInput = new ByteArrayInputStream(chunk);
				ObjectInputStream ois = new ObjectInputStream(byteInput))
			{
				object = (Serializable) ois.readObject();
			} 
			catch (Exception e) {
				throw e;
			}

		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}

		return object;
	}

	public void close() {
		if(isConnected())
		sendObject(VSObjectConnection.EOT_EOF);
	}

	public Socket getSocket() {
		return socket;
	}

	public boolean isConnected() {
		return !socket.isClosed() && socket.isConnected() && !socket.isInputShutdown() && !socket.isOutputShutdown();
	}

}
