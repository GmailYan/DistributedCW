import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class EventuallyStrongFailureDetector implements IFailureDetector {

	ESFDProcess p;
	HashSet<Integer> processes;
	HashSet<Integer> alives;
	LinkedList<Integer> suspects;
	Timer heartbeatTimer;
	Timer timeoutTimer;
	long timeout = Delta;
	long delay = 0;

	// first integer is round number, second integer key is process ID, third is
	// decision value
	Hashtable<Integer, Hashtable<Integer, Integer>> valMessages = new Hashtable<Integer, Hashtable<Integer, Integer>>();

	public Hashtable<Integer, Message> outcome = new Hashtable<Integer, Message>();

	static final int Delta = 1000; /* 1sec */

	class PeriodicTask extends TimerTask {
		public void run() {
			p.broadcast("heartbeat",
					String.format("%d", System.currentTimeMillis()));
			// System.out.println(timeout);
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

	public EventuallyStrongFailureDetector(ESFDProcess p) {
		this.p = p;
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

		if (isSuspect(m.getSource())) {
			delay = Math
					.max(delay,
							System.currentTimeMillis()
									- Long.parseLong(m.getPayload()));
			timeout = Delta + 2 * delay;
		}
		processes.add(m.getSource());
		alives.add(m.getSource());
		// Utils.out(p.pid, m.toString());
		// Utils.out(p.pid, Integer.toString(suspects.size()));
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

	public int Consensus() {
		int x = p.getX();// initial decision value
		int r = 0;// consensus round number
		int m = 0;
		int n = p.getNo(); // number of process
		int i = p.getPid(); // pid of this process
		while (true) {
			r++;
			int c = (r % n) + 1; // pick the candidate of this round

			SendMessage(p, "VAL", x, r, c);

			// if current process is coordinator
			if (i == c) {

				Hashtable<Integer, Integer> decisions = uponReceiveMessages(r);

				Integer majority = Majority(decisions);

				// everyone decide the same value, so only 1 entry
				HashSet<Integer> uniqueValues = new HashSet<Integer>();
				for(Integer each: decisions.values()){
					uniqueValues.add(each);
				}
				boolean d = uniqueValues.size() == 1;

				String payload = String.format("%b,%d,%d", d, majority, r);
				p.broadcast("OUTCOME", payload);

				if (d) {
					p.setX(majority);
					x = majority;
					if (m == 0) {
						m = c;
					} else if (m == c) {
						break;
					}
				}

			}

			// broadcast set does not include itself, do not block wait for
			// message from itself
			if (c != i) {

				Message outcome = collectMessageFromR(c, r);
				if (outcome != null) {
					// if null then suspected process, outcome is null
					String payload = outcome.getPayload();
					int decide = getDecideFromPayload(payload);
					boolean d = getBoolFromPayload(payload);
					p.setX(x);
					x = decide;

					if (d) {
						// decide x
						p.setX(x);
						x = decide;
						if (m == 0) {
							m = c;
						} else if (m == c) {
							break;
						}
					}

				}

			}
		}
		return x;
	}

	private Message collectMessageFromR(int c, int r) {
		int messageRound = 0;
		Message m;

		do {
			m = outcome.get(c);
			if (m != null) {
				messageRound = getRoundFromPayload(m.getPayload());
			}
			if (isSuspect(c)) {
				// r is been suspected, no need to block any further
				break;
			}

			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} while (m == null || messageRound < r);

		return m;
	}

	// only for OUTCOME message, that has 3 elements in the payload
	private int getRoundFromPayload(String payload) {
		String[] token = payload.split(",", 3);
		return Integer.parseInt(token[2]);
	}

	// only for OUTCOME message, that has 3 elements in the payload
	private int getDecideFromPayload(String payload) {
		String[] token = payload.split(",", 3);
		return Integer.parseInt(token[1]);
	}

	// only for OUTCOME message, that has 3 elements in the payload
	private boolean getBoolFromPayload(String payload) {
		String[] token = payload.split(",", 3);
		return token[0].equalsIgnoreCase("true");
	}

	// the decisions are from other process, majority must include itself
	private Integer Majority(Hashtable<Integer, Integer> decisions) {
		HashMap<Integer, Integer> frequency = new HashMap<Integer, Integer>();

		// include process itself in deciding majority
		frequency.put(p.getX(), 1);

		for (Integer each : decisions.values()) {
			int f = 0;
			if (frequency.containsKey(each)) {
				f = frequency.get(each);
			}
			frequency.put(each, f + 1);
		}

		Integer maxKey = null;
		Integer maxValue = 0;
		for (int key : frequency.keySet()) {
			if (frequency.get(key) > maxValue) {
				maxValue = frequency.get(key);
				maxKey = key;
			}
		}

		return maxKey;
	}

	private Hashtable<Integer, Integer> uponReceiveMessages(int r) {

		Hashtable<Integer, Integer> decisionValues = new Hashtable<Integer, Integer>();
		while (true) {

			int F = suspects.size();
			int n = p.getNo();

			synchronized (valMessages) {
				decisionValues = valMessages.get(r);
			}
			if (decisionValues != null && decisionValues.size() >= n - F - 1) {
				Utils.out(p.getPid(), "upon Receive enough VAL Messages");
				break;

			}

			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return decisionValues;
	}

	private void SendMessage(Process parent, String type, int x, int r, int c) {
		// send [VAL: x,r] to G[c]
		Message m = new Message();
		m.setSource(parent.getPid());
		m.setDestination(c);
		m.setType(type);
		m.setPayload(x + "," + r);
		parent.unicast(m);
		Utils.out(
				parent.getPid(),
				String.format("send out VAL message to %d of round %d",
						m.getDestination(), r));
	}

	public void addConsensusMessage(Message m) {
		synchronized (valMessages) {
			Utils.out(p.pid, String.format(
					"receive VAL message from %d with payload: %s",
					m.getSource(), m.getPayload()));

			String payloadMessage = m.getPayload();
			String[] parser = payloadMessage.split(",", 2);
			int decisionValue = Integer.parseInt(parser[0]);
			int roundNumber = Integer.parseInt(parser[1]);

			// get list of message list under roundNumber
			if (!this.valMessages.containsKey(roundNumber)) {
				Hashtable<Integer, Integer> round = new Hashtable<Integer, Integer>();
				round.put(m.getSource(), decisionValue);
				this.valMessages.put(roundNumber, round);
			} else {
				Hashtable<Integer, Integer> round = this.valMessages
						.get(roundNumber);
				round.put(m.getSource(), decisionValue);
			}

		}
	}

	public void addOutcomeMessage(Message m) {
		synchronized (outcome) {
			Utils.out(p.pid, String.format(
					"receive OUTCOME message from %d with payload: %s",
					m.getSource(), m.getPayload()));
			outcome.put(m.getSource(), m);
		}
	}

}
