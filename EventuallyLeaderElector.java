import java.util.HashSet;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class EventuallyLeaderElector implements IFailureDetector {

	Process p;
	HashSet<Integer> processes;
	HashSet<Integer> alives;
	private LinkedList<Integer> suspects;
	Timer heartbeatTimer;
	Timer timeoutTimer;
	long timeout = Delta;
	long delay = 0;
	int leader = 0;

	static final int Delta = 1000; /* 1sec */

	class PeriodicTask extends TimerTask {
		public void run() {
			// printing timeout or leader causes random changes in timeout value
			p.broadcast("heartbeat",
					String.format("%d", System.currentTimeMillis()));
			timeoutTimer.schedule(new Timeout(), timeout);
		}
	}

	class Timeout extends TimerTask {
		public void run() {
			for (Integer p : processes) {
				if (!alives.contains(p) && !isSuspect(p)) {
					addSuspect(p);
				}
				if (alives.contains(p) && isSuspect(p)) {
					removeSuspect(p);
				}
			}

			alives = new HashSet<Integer>();
		}
	}

	public EventuallyLeaderElector(Process p) {
		this.p = p;
		leader = p.pid;
		heartbeatTimer = new Timer();
		timeoutTimer = new Timer();
		processes = new HashSet<Integer>();
		suspects = new LinkedList<Integer>();
		alives = new HashSet<Integer>();
	}

	@Override
	public void begin() {
		heartbeatTimer.schedule(new PeriodicTask(), 0, Delta);
	}

	@Override
	public void receive(Message m) {

		delay = Math.max(delay,
				System.currentTimeMillis() - Long.parseLong(m.getPayload()));
		timeout = Delta + 2 * delay;

		alives.add(m.getSource());
		if(!processes.contains(m.getSource())){
			processes.add(m.getSource());
			leader = getLeader();
		}
		Utils.out(p.pid, m.toString());
		// Utils.out(p.pid, Integer.toString(suspects.size()));
	}

	@Override
	public boolean isSuspect(Integer pid) {
		return suspects.contains(pid);
	}

	@Override
	public int getLeader() {
		// Utils.out(p.pid, Integer.toString(suspects.size()));
		int res = this.isSuspect(leader) ? p.pid : leader;
		for (int pid : alives) {
			res = Math.max(res, pid);
		}

		if (leader != res) {
			Utils.out(p.pid, "leader : " + Integer.toString(res));
		}

		leader = res;
		return res;
	}

	@Override
	public void isSuspected(Integer process) {
		return;
	}

	private void addSuspect(Integer p) {
		suspects.add(p);
		leader = getLeader();
	}

	private void removeSuspect(Integer p) {
		suspects.remove(p);
		leader = getLeader();
	}
}
