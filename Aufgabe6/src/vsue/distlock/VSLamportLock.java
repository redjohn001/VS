package vsue.distlock;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jgroups.JChannel;

public class VSLamportLock {

	private final VSLamportLockProtocol protocol;
	private final Lock lock = new ReentrantLock();
	private final Condition awaitResponse = lock.newCondition();

	public VSLamportLock(JChannel channel) {
		protocol = (VSLamportLockProtocol) channel.getProtocolStack().findProtocol(VSLamportLockProtocol.class);
		protocol.register(this);
	}

	private TimerTask builddebugTask() {
		return new TimerTask() {
			@Override
			public void run() {
				System.out.println("failed");
				protocol.printDeadlockState();
			}
		};

	}

	private Timer debugWatchDog;
	private TimerTask debugtask;

	private void waitForSignal() throws InterruptedException {
		debugWatchDog = new Timer();
		debugtask = builddebugTask();
		debugWatchDog.schedule(debugtask, 5000);
		awaitResponse.await(); // TODO signal könnte vor await bereits aufgerufen worden sein
	}

	public void signalResponse() {
		lock.lock();
		
		try {
			debugtask.cancel();
			debugWatchDog.cancel();
			awaitResponse.signal();
		} finally {
			lock.unlock();
		}
	}

	public void lock() {
		lock.lock();
		
		try {
			try {
				protocol.aquireLock();
				waitForSignal();
			} catch (InterruptedException e) {
				System.err.println("lock awaiting failed" + e);
			} catch (Exception e) {
				System.err.println("lock message failed" + e);
			}
		} finally {
			lock.unlock();
		}
	}

	public void unlock() {
		lock.lock();

		try {
			try {
				protocol.releaseLock();
			} catch (Exception e) {
			}
		} finally {
			lock.unlock();
		}
	}

}
