package vsue.faults;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import vsue.communication.VSObjectConnection;

public class VSBuggyObjectConnection extends VSObjectConnection {

	public static final String COMMANDSTRING = "conn-prop";
	private static final long MAX_DELAY_TIME = 1001;

	public static void commandLineProcessor(String[] args) {
		if (args.length != 3) {
			System.out
					.println("command pattern is: 'conn-prob' <behavior> <prob [%]>");
			for (VSReceiveBehaviour b : VSReceiveBehaviour.values()) {
				System.out.println(b + " " + b.prob);
			}
			return;
		}
		VSBuggyObjectConnection.VSReceiveBehaviour b = VSBuggyObjectConnection.VSReceiveBehaviour
				.valueOf(args[1].toUpperCase());
		if (b == null) {
			System.out.println("unknown behavior: " + args[1].toUpperCase());
			return;
		}
		try {
			b.setProb(Double.parseDouble(args[2]));
			System.out.println("setting prop of behavior " + b + " to value "
					+ args[2]);
		} catch (NumberFormatException nfe) {
			System.out.println("unknown value format for value: " + args[2]);
		}
	}

	private final Random distribution = new Random();

	public static enum VSSendBehaviour {
		DISCARD(0), DELAYED(0), DIRECT(1);

		public static void setDeterministicBehavior(
				final Collection<VSSendBehaviour> c) {
			deterministic.addAll(c);
		}

		public static VSSendBehaviour getBehaviour(final double propValue) {
			double psum = 0;
			if (!deterministic.isEmpty()) {
				return deterministic.poll();
			}
			for (VSSendBehaviour b : values()) {
				psum += b.prob;
				if (propValue < psum) {
					return b;
				}
			}
			return DIRECT;
		}

		public void setProb(final double p) {
			double psum = 0;
			for (VSSendBehaviour b : values()) {
				psum += b.prob;
			}
			if ((psum - prob) + p > 1) {
				throw new IllegalStateException("probability greater than 1");
			}
			prob = p;
		}

		private VSSendBehaviour(final double p) {
			prob = p;
		}

		private static Queue<VSSendBehaviour> deterministic = new ConcurrentLinkedQueue<VSBuggyObjectConnection.VSSendBehaviour>();
		private double prob = 0;
	}

	public static enum VSReceiveBehaviour {
		DISCARD(0), DELAYED(0), DIRECT(1);

		public static void setDeterministicBehavior(
				final Collection<VSReceiveBehaviour> c) {
			deterministic.addAll(c);
		}

		public static VSReceiveBehaviour getBehaviour(final double propValue) {
			double psum = 0;
			if (!deterministic.isEmpty()) {
				return deterministic.poll();
			}
			for (VSReceiveBehaviour b : values()) {
				psum += b.prob;
				if (propValue < psum) {
					return b;
				}
			}
			return DIRECT;
		}

		public void setProb(final double p) {
			double psum = 0;
			for (VSReceiveBehaviour b : values()) {
				psum += b.prob;
			}
			if ((psum - prob) + p > 1) {
				throw new IllegalStateException("probability greater than 1");
			}
			prob = p;
		}

		private VSReceiveBehaviour(final double p) {
			prob = p;
		}

		private static Queue<VSReceiveBehaviour> deterministic = new ConcurrentLinkedQueue<VSBuggyObjectConnection.VSReceiveBehaviour>();

		private double prob = 0;
	}

	public VSBuggyObjectConnection(Socket socket) {
		super(socket);
	}

	private void superSendObject(Serializable object) {
		super.sendObject(object);
	}

	@Override
	public void sendObject(Serializable object) {
		// get send behavior to sabotage the sending process
		final VSSendBehaviour b = VSSendBehaviour.getBehaviour(distribution
				.nextDouble());
		System.out.println("send behaviour for current message is " + b);
		switch (b) {
		case DISCARD:
			return;
		case DELAYED:
			//if delayed create a timer that sends the message after the delay time
			new Timer().schedule(new DelayedSend(this, object),
					(long) MAX_DELAY_TIME);
			return;
		case DIRECT:
		default:
		}
		super.sendObject(object);
	}

	@Override
	public Serializable receiveObject() throws IOException {
		final VSReceiveBehaviour b = VSReceiveBehaviour
				.getBehaviour(distribution.nextDouble());
		///get receive behavior
		System.out.println("receive behaviour for current message is " + b);
		Serializable result = super.receiveObject();
		switch (b) {
		case DISCARD:
			// wait for the socket to timeout and throw exception
			try {
				Thread.sleep((long) getSocket().getSoTimeout());
			} catch (InterruptedException e) {
			}

			throw new IOException("discarded object");
		case DELAYED:
			try {
				//sleep either the time of the socket timeout or the delay time. if the socket time out is 
				//smaller than the delay time an exception is thrown.
				if (MAX_DELAY_TIME > getSocket().getSoTimeout()) {
					Thread.sleep(getSocket().getSoTimeout());
					throw new SocketTimeoutException("delayed receive");
				} else
					Thread.sleep((long) MAX_DELAY_TIME);
			} catch (InterruptedException e) {
			}

			break;
		case DIRECT:
		default:
		}
		return result;
	}

	private static final class DelayedSend extends TimerTask {

		private final VSBuggyObjectConnection parent;
		private final Serializable object;

		public DelayedSend(VSBuggyObjectConnection p, Serializable o) {
			parent = p;
			object = o;
		}

		@Override
		public void run() {

			if(!parent.isConnected()){
				return;
			}
			try{
				parent.superSendObject(object);
			}catch(Exception e){
				// no-op as the socket might be closed 
			}
		}

	}

}
