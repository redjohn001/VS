package vsue.faults;

import static org.junit.Assert.assertEquals;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import vsue.faults.VSBuggyObjectConnection.VSReceiveBehaviour;
import vsue.faults.VSBuggyObjectConnection.VSSendBehaviour;
import vsue.rmi.VSAuction;
import vsue.rmi.VSAuctionEventHandler;
import vsue.rmi.VSAuctionEventType;
import vsue.rmi.VSAuctionService;
import vsue.rpc.VSAuctionServer;
import vsue.rpc.VSInvocationHandler;
import vsue.rpc.VSRemoteObjectManager;

public class testConnection {

	private static String[] args = new String[] { "unitTestUser", "localhost",
			"1100" };
	private static VSAuctionService clientstub;
	private static VSAuctionServer server;

	private static ArrayList<VSReceiveBehaviour> alwaysPassRec = new ArrayList<VSBuggyObjectConnection.VSReceiveBehaviour>();
	private static ArrayList<VSSendBehaviour> alwaysPassSen = new ArrayList<VSBuggyObjectConnection.VSSendBehaviour>();
	private static ArrayList<VSReceiveBehaviour> alwaysDiscardRec = new ArrayList<VSBuggyObjectConnection.VSReceiveBehaviour>();
	private static ArrayList<VSReceiveBehaviour> alwaysDelayRec = new ArrayList<VSBuggyObjectConnection.VSReceiveBehaviour>();
	private static ArrayList<VSSendBehaviour> alwaysDiscardSen = new ArrayList<VSBuggyObjectConnection.VSSendBehaviour>();
	private static ArrayList<VSSendBehaviour> alwaysDelaySen = new ArrayList<VSBuggyObjectConnection.VSSendBehaviour>();

	@Before
	public void before() {
		System.out.println(".................................");
	}

	@AfterClass
	public static void tearDown() {

		server.shutdown();

	}

	@BeforeClass
	public static void setup() {
		for (int i = 0; i < VSInvocationHandler.MAX_MESSAGES; ++i) {
			alwaysDiscardRec.add(VSReceiveBehaviour.DISCARD);
			alwaysDelayRec.add(VSReceiveBehaviour.DELAYED);
			alwaysDiscardSen.add(VSSendBehaviour.DISCARD);
			alwaysDelaySen.add(VSSendBehaviour.DELAYED);
			alwaysPassRec.add(VSReceiveBehaviour.DIRECT);
			alwaysPassSen.add(VSSendBehaviour.DIRECT);
		}
		try {
			server = new VSAuctionServer();
			VSRemoteObjectManager.getInstance().exportObject(
					new VSAuctionEventHandler() {

						@Override
						public void handleEvent(VSAuctionEventType event,
								VSAuction auction) throws RemoteException {
							System.out.println(event);

						}
					});
			Registry registry = LocateRegistry.getRegistry(args[1],
					Integer.parseInt(args[2]));
			clientstub = (VSAuctionService) registry
					.lookup(VSAuctionServer.DEFAULT_SERVICE_NAME);

		} catch (RemoteException e) {
			Assert.fail();
		} catch (NotBoundException ex) {
			Assert.fail();
		}
	}

	@Test
	public void test1GetAuctions() {

		VSReceiveBehaviour.setDeterministicBehavior(alwaysDiscardRec);
		VSSendBehaviour.setDeterministicBehavior(alwaysPassSen);

		// test what happens if all messages are discarded
		try {
			clientstub.getAuctions();
			Assert.fail();
		} catch (RemoteException e) {
		}
	}

	@Test
	public void test2GetAuctions() {

		VSReceiveBehaviour.setDeterministicBehavior(alwaysDelayRec);
		VSSendBehaviour.setDeterministicBehavior(alwaysPassSen);

		// test what happens if all message receive is delayed by 1 second
		try {
			VSAuction[] result = clientstub.getAuctions();
			assertEquals(result.length, 0);
			System.out.println("number of auctions " + result.length);

		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void test3GetAuctions() {

		// test both again but when sending
		VSReceiveBehaviour.setDeterministicBehavior(alwaysPassRec);
		VSSendBehaviour.setDeterministicBehavior(alwaysDiscardSen);

		// test what happens if all messages are discarded
		try {
			clientstub.getAuctions();
			Assert.fail();
		} catch (RemoteException e) {
		}
	}

	@Test
	public void test4GetAuctions() {

		VSReceiveBehaviour.setDeterministicBehavior(alwaysPassRec);
		VSSendBehaviour.setDeterministicBehavior(alwaysDelaySen);

		// test what happens if all message receive is delayed by 1 second
		try {
			VSAuction[] result = clientstub.getAuctions();
			assertEquals(result.length, 0);
			System.out.println("number of auctions " + result.length);

		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void test5GetAuctions() {

		VSReceiveBehaviour
				.setDeterministicBehavior(Arrays
						.asList(new VSReceiveBehaviour[] { VSReceiveBehaviour.DISCARD }));
		VSSendBehaviour.setDeterministicBehavior(alwaysPassSen);

		// test what happens if all message receive is delayed by 1 second
		try {
			VSAuction[] result = clientstub.getAuctions();
			assertEquals(result.length, 0);
			System.out.println("number of auctions " + result.length);

		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void test6GetAuctions() {

		VSReceiveBehaviour
				.setDeterministicBehavior(Arrays
						.asList(new VSReceiveBehaviour[] { VSReceiveBehaviour.DELAYED }));
		VSSendBehaviour.setDeterministicBehavior(alwaysPassSen);

		try {
			VSAuction[] result = clientstub.getAuctions();
			assertEquals(result.length, 0);
			System.out.println("number of auctions " + result.length);

		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void test7GetAuctions() {

		VSReceiveBehaviour.DIRECT.setProb(0.5);
		VSReceiveBehaviour.DISCARD.setProb(0.25);
		VSReceiveBehaviour.DELAYED.setProb(0.25);

		VSSendBehaviour.DIRECT.setProb(0.5);
		VSSendBehaviour.DISCARD.setProb(0.25);
		VSSendBehaviour.DELAYED.setProb(0.25);

		for (int i = 0; i < 20; ++i) {
			try {
				System.out
						.println("new iteration ..................................."+i);
				VSAuction[] result = clientstub.getAuctions();
				assertEquals(result.length, 0);
				System.out.println("number of auctions " + result.length);

			} catch (Exception e) {
				if (!e.getMessage().startsWith("No respone")) {
					Assert.fail();
				} else {
					System.err.println("no response");
				}
			}
		}
	}

}
