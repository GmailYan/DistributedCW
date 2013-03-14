import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class StrongFailureDetector implements IFailureDetector {

	SFDProcess process;
	
	// the key integer is round number
	public Hashtable<Integer, Message> valMessages = new Hashtable<Integer, Message>();
	Process p;
	public HashSet<Integer> processes;
	public HashSet<Integer> alives;
	public LinkedList<Integer> suspects;
	Timer heartbeatTimer;
	Timer timeoutTimer;
	long timeout = Delta + 2 * Utils.DELAY;

	// enable debug output to console by set this to true
	private boolean verbose = false;

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
			}

			alives = new HashSet<Integer>();
		}
	}

	public StrongFailureDetector(Process p) {
		this.p = p;
		process = (SFDProcess) p;
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
		processes.add(m.getSource());
		alives.add(m.getSource());
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

	// consensus using rotating coordinator algorithm
	public int Consensus() {
		for (int r = 1; r <= process.getNo(); r++) {
			if (r == process.pid) {
				process.broadcast("VAL", process.getX() + "," + r);
				if(verbose )Utils.out(p.getPid(),"broadcast VAL message with payload:"+process.getX() + "," + r);
			} else {
				// process ID each is the coordinator of this round,
				// either it is correct or eventually, it will be suspected
				Message cM = collectMessageFromR(r);

				if (cM != null) {
					if(verbose)Utils.out(p.getPid(),"receive VAL message from coordinator with payload: "+cM.getPayload());
					String payloadMessage = cM.getPayload();
					String[] parser = payloadMessage.split(",", 2);

					// parser = [VAL: v,r]
					process.setX(Integer.parseInt(parser[0]));
				}
			}
		}
		// decide x
		return process.getX();
	}

	private Message collectMessageFromR(int r) {
		while (true) {
			if (isSuspect(r)) {
				// r is been suspected, no need to block any further
				return null;
			}
			// try to receive the message
			synchronized (valMessages) {
				if(valMessages.containsKey(r)){
					return valMessages.get(r);
				}
			}

			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void addVALMessage(Message m2) {
		synchronized (valMessages) {
			valMessages.put(m2.getSource(), m2);
		}
	}

}
