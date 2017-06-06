package vsue.distlock;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgroups.Event;
import org.jgroups.Global;
import org.jgroups.Header;
import org.jgroups.Message;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.stack.Protocol;

public class VSLogicalClockProtocol extends Protocol {

	private AtomicInteger currentClock = new AtomicInteger(0);

	public static class ClockHeader extends Header {
		public static final short header_id = 1501;
		private int lamportClock = -1;

		// Headers need a default constructor for instantiation via reflection.
		public ClockHeader(/* Don't add parameters here! */) {
		}

		public ClockHeader(int clock) {
			lamportClock = clock;
		}

		@Override
		public void writeTo(DataOutput out) throws IOException {
			out.writeInt(lamportClock);
		}

		@Override
		public void readFrom(DataInput in) throws IOException {
			lamportClock = in.readInt();
		}

		@Override
		public int size() {
			return Global.INT_SIZE;
		}

		public int getLamportTime() {
			return lamportClock;
		}
	}

	// --- Interface for LamportLockProtocol class ---

	public static int getMessageTime(Message m) {
		ClockHeader h = (ClockHeader) m.getHeader(ClockHeader.header_id);
		return h.getLamportTime();
	}

	// --- Protocol implementation ---

	@Override
	public synchronized Object down(Event evt) {
		switch(evt.getType()) {
		case Event.MSG:
			final Message m = (Message) evt.getArg();
			ClockHeader ch = new ClockHeader(currentClock.incrementAndGet());
			// System.out.println("down: " + currentClock);
			m.putHeader(ClockHeader.header_id, ch);
			return down_prot.down(evt);
		default:
			break;
		}

		return down_prot.down(evt);
	}

	@Override
	public Object up(Event evt) {
		switch (evt.getType()) {
		case Event.MSG:
			Message m = (Message) evt.getArg();
			synchronized (currentClock) {
				final int current = currentClock.get();
				currentClock.set(1 + Math.max(current, getMessageTime(m)));
			}
			// System.out.println("up: " + currentClock);
			return up_prot.up(evt);
		default:
			break;
		}

		return up_prot.up(evt);
	}

	@Override
	public void init() {
		ClassConfigurator.add(ClockHeader.header_id, ClockHeader.class);
	}

}
