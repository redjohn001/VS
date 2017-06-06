package vsue.replica;

import java.io.Serializable;

public class VSKeyValueReply implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Object result;
	private final int id;
	public VSKeyValueReply(final int id,final Object res) {
		result = res;
		this.id = id;
	}
	

	public Object getResult() throws Throwable{
		if(result instanceof Throwable){
			throw (Throwable)result;
		}
		return result;
	}


	public int getId() {
		return id;
	}
}
