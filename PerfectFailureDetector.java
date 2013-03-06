
import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class PerfectFailureDetector implements IFailureDetector {

	Process p;
	LinkedList<Integer> suspects;
	Timer t;
	long timeout = Delta + Utils.DELAY;
	
	static final int Delta = 1000; /* 1sec */
	
	class PeriodicTask extends TimerTask{
		public void run() {
			p.broadcast("heartbeat", String.format("%d", System.currentTimeMillis()));
			//Utils.out(p.getName()+" broadcasted heart beat at "+ new Date());
		}	
	}
	
	public PerfectFailureDetector (Process p){
		this.p = p;
		t = new Timer();
		suspects = new LinkedList<Integer>();
	}
	
	@Override
	public void begin() {
		t.scheduleAtFixedRate(new PeriodicTask(), 0, Delta);
	}

	@Override
	public void receive(Message m) {
		// Assume no mesage loss, so ignore case that message of a process never received
		// if timeout < delta + delay, then suspect
		long realisedDelay = System.currentTimeMillis() - Long.parseLong(m.getPayload());
		if(timeout < Delta + realisedDelay){
			suspects.add(m.getSource(), m.getSource());
		}
		Utils.out(p.getName()+" receive heart beat at "+ new Date());
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
