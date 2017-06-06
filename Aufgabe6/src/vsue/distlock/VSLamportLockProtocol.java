package vsue.distlock;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.jgroups.Event;
import org.jgroups.Global;
import org.jgroups.Header;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.stack.Protocol;

public final class VSLamportLockProtocol extends Protocol {

	// MAP verwenden statt List
	// alle zeitspempel rein schreiben und nicht nur die vom Ack-Header
	// => bei zwei locks => deadlock, weil hinten an der Liste und das unlock nicht aufgerufen werden kann

	private static final short ACK = 1;
	private static final short REQUEST = 2;
	private static final short RELEASE = 3;

	private VSLamportLock lamportLock;

	private int memberCount;
	private Message lockRequest;
	private List<Integer> lamportAckTimes = new ArrayList<>();
	private PriorityQueue<Message> lockRequests = new PriorityQueue<>(5, comparator);

	private static Comparator<Message> comparator = new Comparator<Message>() {
		@Override
		public int compare(Message m1, Message m2) {
			final int lamportTime1 = VSLogicalClockProtocol.getMessageTime(m1);
			final int lamportTime2 = VSLogicalClockProtocol.getMessageTime(m2);

			// falls die lamport time gleich ist, werden die hostnamen
			// vergleicht - ansonsten die lamport time
			if (lamportTime1 == lamportTime2) {
				return m1.getSrc().compareTo(m2.getSrc());
			} else if (lamportTime1 > lamportTime2) {
				return 1;
			} else {
				return -1;
			}
		}
	};

	// --- Additional header for Lock request messages ---

	public static class LockProtocolHeader extends Header {

		public static final short header_id = 1500;

		private short type;

		// Headers need a default constructor for instantiation via reflection.
		public LockProtocolHeader(/* Don't add parameters here! */) { }

		public LockProtocolHeader(short type) {
			this.type = type;
		}

		@Override
		public void readFrom(DataInput in) throws IOException {
			this.type = in.readShort();
		}

		@Override
		public void writeTo(DataOutput out) throws IOException {
			out.writeShort(type);
		}

		@Override
		public int size() {
			return Global.SHORT_SIZE;
		}

		public short getType() {
			return type;
		}

	}

	// --- Interface to LamportLock class ---

	public void register(VSLamportLock lamportLock) {
		this.lamportLock = lamportLock;
	}

	public void aquireLock() {
//		System.out.print("\n------------\na");
		Message m = new Message(null, new Integer(0));
		m.putHeader(LockProtocolHeader.header_id, new LockProtocolHeader(
				REQUEST));
		down(new Event(Event.MSG, m));
	}

	public void releaseLock() {
//		System.out.print("r");
		Message m = new Message(null, new Integer(0));
		m.putHeader(LockProtocolHeader.header_id, new LockProtocolHeader(RELEASE));
		down(new Event(Event.MSG, m));
	}

	// --- Protocol implementation ---

	private final Object discardEvent() {
		return null;
	}

	@Override
	public Object down(Event evt) {
		switch (evt.getType()) {
		case Event.MSG:
			Message m = (Message) evt.getArg();
			Header header = m.getHeader(LockProtocolHeader.header_id);
			if (header != null) {
				LockProtocolHeader lPHeader = (LockProtocolHeader) header;

				switch (lPHeader.getType()) {
				case REQUEST:
					lockRequest = m;
					lamportAckTimes.clear();
//					System.out.println("[dRq]");
					break;

				case RELEASE:
					break;

				default:
					System.out.println("HÄ? Wasn da los? (in down())");
					break;
				}
			}
			break;

		default:
			break;
		}

		return down_prot.down(evt);
	}

	private boolean tryToAquireTheLock() {
		if (lamportAckTimes.size() == memberCount) {
			if (lockRequest != null && lockRequests.peek().equals(lockRequest)) {
				int myTime = VSLogicalClockProtocol.getMessageTime(lockRequest);

				for (int time : lamportAckTimes) {
					if (time < myTime) {
						System.out.println(time + " " + myTime);
						return false;
					}
				}
//				System.out.print("s");

				lockRequest = null;
				lamportAckTimes.clear();
//				System.out.println("sign");
				lamportLock.signalResponse();
				return true;
			}
		}

		return false;
	}

	@Override
	public Object up(Event evt) {
		switch (evt.getType()) {
		case Event.VIEW_CHANGE:
			memberCount = ((View) evt.getArg()).getMembers().size();
			break;

		case Event.MSG:
			Message m = (Message) evt.getArg();
			Header header = m.getHeader(LockProtocolHeader.header_id);

			if (header != null) {
				LockProtocolHeader lPHeader = (LockProtocolHeader) header;

				switch (lPHeader.getType()) {
				case REQUEST:
					// add lock request
					lockRequests.add(m);

//					System.out.println("[uRq<-" + m.getSrc() + "]");
//					for (Message r : lockRequests) {
//						System.out.println(r + " "
//								+ r.getHeaders().values().toString());
//					}

					// notify "winning" client
					Message reqMessage = new Message(m.getSrc(), new Integer(0));
					reqMessage.putHeader(LockProtocolHeader.header_id,
							new LockProtocolHeader(ACK));
//					System.out.println("[dAc->" + m.getSrc() + "]");
					down_prot.down(new Event(Event.MSG, reqMessage));
					// discard message for upper stack

					return discardEvent();

				case ACK:
					lamportAckTimes.add(VSLogicalClockProtocol
							.getMessageTime(m));
//					for (int r : lamportAckTimes) {
//						System.out.println(r);
//					}
//					System.out.println("[uAc<-" + m.getSrc() + "]");

					tryToAquireTheLock();

					return discardEvent();

				case RELEASE:
//					System.out.println("[uRl<-" + m.getSrc() + "]");
					lockRequests.poll();
					tryToAquireTheLock();
					return discardEvent();

				default:
					System.out.println("HÄ? Wasn da los? (in up())");
					break;
				}
			}

			break;

		default:
			break;
		}

		return up_prot.up(evt);
	}

	@Override
	public void init() {
		ClassConfigurator.add(LockProtocolHeader.header_id, LockProtocolHeader.class);
	}

	public void printDeadlockState() {
		System.out.println("\n deadlock detection");
		System.out.println("memberCount:    " + memberCount);
		System.out.println("current queue:  " + lockRequests.size());

		for (Message m : lockRequests) {
			System.out.println(m + " " + m.getHeaders().values().toString());
		}

		System.out.println("own request:    " + lockRequest);
		System.out.println("aquired acks:   " + lamportAckTimes.size());

		for (int m : lamportAckTimes) {
			System.out.println(m);
		}
	}

}
