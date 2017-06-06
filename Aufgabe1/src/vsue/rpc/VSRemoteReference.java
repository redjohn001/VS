package vsue.rpc;

import java.io.Serializable;

public class VSRemoteReference implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/** on which host is the object*/
	private String host;
	
	/** on which port is the server*/
	private int port;
	
	/**which object is referenced*/
	private int objectId;

	public VSRemoteReference(final String host, final int port, final int objectId) {
		this.host = host;
		this.port = port;
		this.objectId = objectId;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getObjectId() {
		return objectId;
	}

	public void setObjectId(int objectId) {
		this.objectId = objectId;
	}

}
