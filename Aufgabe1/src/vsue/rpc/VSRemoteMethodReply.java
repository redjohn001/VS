package vsue.rpc;

import java.io.Serializable;

public class VSRemoteMethodReply implements Serializable {

	private static final long serialVersionUID = 1L;

	private int messageId;
	private Object reply;
	
	public VSRemoteMethodReply(int messageId, Object reply) {
		this.messageId = messageId;
		this.reply = reply;
	}
	
	public int getMessageId() {
		return messageId;
	}
	
	public void setMessageId(int messageId) {
		this.messageId = messageId;
	}

	public Object getReply() throws Throwable {
		if(reply instanceof Exception){
			throw (Throwable)reply;
		}
		return reply;
	}

	public void setReply(Object reply) {
		this.reply = reply;
	}

}
