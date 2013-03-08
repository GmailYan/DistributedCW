<<<<<<< HEAD
public class EventuallyLeaderElector implements IFailureDetector {

	Process p;
	
	public EventuallyLeaderElector(Process p) {
		this.p = p;
	}

	@Override
	public void begin() {
		// TODO Auto-generated method stub

	}

	@Override
	public void receive(Message m) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSuspect(Integer process) {
		return false;
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
=======
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class EventuallyLeaderElector implements IFailureDetector {

	Process p;
	HashSet<Integer> processes;
	HashSet<Integer> alives;
	LinkedList<Integer> suspects;
	Timer heartbeatTimer;
	Timer timeoutTimer;
	long timeout = Delta;
	long delay = 0L;
	int leader;

	static final int Delta = 1000; /* 1sec */

	class PeriodicTask extends TimerTask {
		public void run() {
			p.broadcast("heartbeat",
					String.format("%d", System.currentTimeMillis()));
			timeoutTimer.schedule(new Timeout(), timeout);
		}
	}

	class Timeout extends TimerTask {
		public void run() {
			for (Integer p : processes) {
				if (!alives.contains(p) && !isSuspect(p)) {
					suspects.add(p);
				}
				if (alives.contains(p) && isSuspect(p)) {
					suspects.remove(p);
				}
			}

			alives = new HashSet<Integer>();
		}
	}

	public EventuallyLeaderElector(Process p) {
		this.p = p;
		this.leader = p.pid;
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
		processes.add(m.getSource());
		alives.add(m.getSource());
		Utils.out(p.pid, m.toString());
		// Utils.out(p.pid, Integer.toString(suspects.size()));
	}

	@Override
	public boolean isSuspect(Integer pid) {
		return suspects.contains(pid);
	}

	@Override
	public int getLeader() {
		int res = this.isSuspect(leader) ? p.pid : leader;
		for (int pid : alives){
			res = Math.max(res, pid);
		}
		
		return res;
	}

	@Override
	public void isSuspected(Integer process) {
		return;
	}
}
>>>>>>> 5ebf875e7de32293fe292f73afb53cfb7972ca1a
