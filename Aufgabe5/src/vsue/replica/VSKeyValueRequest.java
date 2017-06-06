package vsue.replica;

import java.io.Serializable;
import java.rmi.Remote;

import vsue.replica.VSKeyValueReplica.VSOperation;

public class VSKeyValueRequest implements Serializable {

	/**
	 * 
	 */
	private static final transient long serialVersionUID = 1L;

	/*
	 * TODO: Implement request
	 */
	// key
	// value
	private final VSKeyValueReplyHandler clientStub;
	private final String key;
	private final String value;
	private final int id;
	private long timestamp;
	private final VSKeyValueReplica.VSOperation operation;

	public VSKeyValueRequest(int id,VSKeyValueReplyHandler clientStub, String key, String value,
			VSOperation operation) {
		this.clientStub = clientStub;
		this.key = key;
		this.id = id;
		this.value = value;
		this.operation = operation;
	}

	public VSKeyValueReplyHandler getClientStub() {
		return clientStub;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public VSKeyValueReplica.VSOperation getOperation() {
		return operation;
	}

	public int getId() {
		return id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}
