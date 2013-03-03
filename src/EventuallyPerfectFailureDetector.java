package src;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class EventuallyPerfectFailureDetector implements IFailureDetector {

	Process p;
	LinkedList<Integer> suspects;
	LinkedList<Integer> timeouts;
	Timer t;

	static final int Delta = 1000; /* 1sec */
	
	class PeriodicTask extends TimerTask {
		public void run() {
			p.broadcast("heartbeat", "null");
		}
	}

	public EventuallyPerfectFailureDetector(Process p) {
		this.p = p;
		t = new Timer();
		suspects = new LinkedList<Integer>();
		timeouts = new LinkedList<Integer>();
	}

	@Override
	public void begin() {
		t.schedule(new PeriodicTask(), 0, Delta);
	}

	@Override
	public void receive(Message m) {
		long delay = System.currentTimeMillis() - Long.parseLong(m.getPayload());
		long timeout = Delta + delay;
		Utils.out(p.pid, m.toString());
	}

	@Override
	public boolean isSuspect(Integer pid) {
		return suspects.contains(pid);
	}

	@Override
	public int getLeader() {
		return -1;
	}

	@Override
	public void isSuspected(Integer process) {
		return;
	}

}
