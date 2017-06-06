package vsue.distlock;

import java.io.DataInput;
import java.io.DataOutput;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.Header;
import org.jgroups.Message;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.stack.Protocol;
import org.jgroups.util.Util;

public class FIFOUnicast extends Protocol {
	private Address my_address;

	// --- Additional header for Lock request messages ---
	public static class SimulatedUnicastProtocolHeader extends Header {
		public static final short header_id = 1800;
		public Address real_destination;
		
		public Address getRealDestination() { return real_destination; }
		
		public SimulatedUnicastProtocolHeader() {}
		public SimulatedUnicastProtocolHeader(Address real_dest) {
			real_destination = real_dest;
		}
		
		@Override
		public void	readFrom(DataInput in) throws Exception {
			real_destination = Util.readAddress(in);
		}
		
		@Override
		public void	writeTo(DataOutput out) throws Exception {
			Util.writeAddress(real_destination, out);
		}
		
		@Override
		public int size() {
			return 0;
		}
	}
	
	@Override
	public Object down(Event evt) {
		switch (evt.getType()) {
		case Event.MSG:
			Message m = (Message)evt.getArg();
			if (m.getDest() != null) {
				Message realmsg = new Message();
				if (m.getSrc() == null) {
					m.setSrc(my_address);
				}
				realmsg.setObject(m);
				
				SimulatedUnicastProtocolHeader ph = 
						new SimulatedUnicastProtocolHeader(m.getDest());
				realmsg.putHeader(SimulatedUnicastProtocolHeader.header_id, ph);
				return down_prot.down(new Event(Event.MSG, realmsg));
			}
			break;
		}
		return down_prot.down(evt);
	}
	
	@Override
	public Object up(Event evt) {
		switch (evt.getType()) {
		case Event.MSG:
			Message m = (Message)evt.getArg();
			SimulatedUnicastProtocolHeader ph = (SimulatedUnicastProtocolHeader)
				m.getHeader(SimulatedUnicastProtocolHeader.header_id);
			if (ph != null) {
				if (! ph.getRealDestination().equals(my_address)) return null;
				Message realmsg = (Message)m.getObject();
				return up_prot.up(new Event(Event.MSG, realmsg));
			}
			break;
		case Event.VIEW_CHANGE:
			my_address = getProtocolStack().getChannel().getAddress();
			break;
		}
		return up_prot.up(evt);
	}
	
	@Override
	public void init() {
		ClassConfigurator.add(
				SimulatedUnicastProtocolHeader.header_id,
				SimulatedUnicastProtocolHeader.class);
	}
}
