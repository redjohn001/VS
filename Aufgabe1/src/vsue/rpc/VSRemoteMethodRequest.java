package vsue.rpc;

import java.io.Serializable;

public class VSRemoteMethodRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	int messageId;
	int objectId;
	String method;
	Object[] params;
	
	public VSRemoteMethodRequest(int messageId, int objectId, String method, Object[] params) {
		this.messageId = messageId;
		this.objectId = objectId;
		this.method = method;
		this.params = params;
	}
	
	public int getMessageId() {
		return messageId;
	}
	
	public void setMessageId(int messageId) {
		this.messageId = messageId;
	}

	public int getObjectId() {
		return objectId;
	}

	public void setObjectId(int objectId) {
		this.objectId = objectId;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Object[] getParams() {
		return params;
	}

	public void setParams(Object[] params) {
		this.params = params;
	}

}
