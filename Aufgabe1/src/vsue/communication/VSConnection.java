package vsue.communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class VSConnection {

	private InputStream in;
	private OutputStream out;
	
	public static final String CHARSET_ENCODING ="UTF-8";// "COMPOUND_TEXT";
	

	public VSConnection(Socket socket) {
		try {
			in = socket.getInputStream();
			out = socket.getOutputStream();

		} catch (UnknownHostException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			//e.printStackTrace();
		}

	}

	public void sendChunk(byte[] chunk) throws IOException {

		try {
			//System.out.println("sendChunk(" + new String(chunk, CHARSET_ENCODING) + ")");//TODO: exception as unsupported char set -> recommended is "UTF-8"
			final byte[] header = ByteBuffer.allocate(4).putInt(chunk.length).array();
			out.write(header); // integer header contains the content length
			out.write(chunk);
			out.flush();

		} catch (UnsupportedEncodingException e) {
			System.err.println("sendChunk(): Fehler beim Ausgeben der zu sendenden Daten.");
			//e.printStackTrace();
		} catch (IOException e) {
			System.err.println("sendChunk(): Fehler beim Senden der Daten.");
			//e.printStackTrace();
			throw e;
		}
	}

	public byte[] receiveChunk() throws IOException {

		// Use length integer from header
		byte[] header = new byte[4];
		in.read(header, 0, 4);
		final int byteArrayLength = ByteBuffer.wrap(header).getInt();
		
		byte[] chunk = new byte[byteArrayLength];
		
		// Initialize byte buffer with header size
		ByteBuffer bb = ByteBuffer.allocate(byteArrayLength);

		try {
			int len = 0;
			do{
				int readBytes = in.read(chunk, len, byteArrayLength-len);
				if(readBytes == -1){
					break;
				}
				bb.put(chunk,len,readBytes);
				len += readBytes;
				
			}while(len != byteArrayLength);
			
			// Hinweis: in.read(); ganz ohne parameter ist sehr
			// schlecht, da so viele SysCalls - SysCalls "teuer"!

			//System.out.println("receiveChunk(): " + new String(bb.array(), CHARSET_ENCODING));

		} catch (UnsupportedEncodingException e) {
			System.err.println("receiveChunk(): Fehler beim Ausgeben der empfangenen Daten.");
			//e.printStackTrace();
		} catch (IOException e) {
			System.err.println("receiveChunk(): Fehler beim Empfangen der Daten.");
			//e.printStackTrace();
			throw e;
		}

		return bb.array();
	}

}
