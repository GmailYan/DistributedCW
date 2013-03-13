import java.util.ArrayList;
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
	
	// first integer is round number, second integer key is process ID, third is decision value
	Hashtable<Integer,Hashtable<Integer,Integer>> m = new Hashtable<Integer,Hashtable<Integer,Integer>>();
	
	public Hashtable<Integer,Message> outcome = new Hashtable<Integer,Message>();
	
	static final int Delta = 1000; /* 1sec */

	class PeriodicTask extends TimerTask {
		public void run() {
			p.broadcast("heartbeat",
					String.format("%d", System.currentTimeMillis()));
//			System.out.println(timeout);
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
			delay = Math.max(delay, System.currentTimeMillis()
					- Long.parseLong(m.getPayload()));
			timeout = Delta + 2 * delay;
		}
		processes.add(m.getSource());
		alives.add(m.getSource());
		Utils.out(p.pid, m.toString());
		//Utils.out(p.pid, Integer.toString(suspects.size()));
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
	
	public int r = 0;// consensus round number
	
	public int Consensus() {
		int x = p.getX();;// initial decision value
		r = 0;
		int m = 0;
		int n = p.getNo(); // number of process
		int i = 1; // pid of this process
		while (true) {
			r++;
			int c = (r % n) + 1; // pick the candidate of this round

			SendMessage(p, "VAL", x, r, c);
			
			// if current process is coordinator
			if (i == c) {
				Hashtable<Integer, Integer> decisions = uponReceiveMessages(r);
				Integer majority = Majority(decisions);
				
				// everyone decide the same value, so only 1 entry 
				boolean d = decisions.values().size()==1;
				
				String payload = String.format("%b,%i,%i", d, majority, r );
				p.broadcast("OUTCOME", payload );
			}
			
			Message outcome = collectMessageFromR(c,r);
			if(outcome != null){
				// if null then suspected process, outcome is null
				String payload = outcome.getPayload();
				x = getDecideFromPayload(payload);
				boolean d = getBoolFromPayload(payload);
				if(d){
					//decide x
					p.setX(x);
					if(m==0){
						m=c;
					}else if(m==c){
						return x;
					}
				}
				
			}
			
		}

	}
	
	private Message collectMessageFromR(int c, int r) {
		int messageRound = 0;
		Message m;
		
		do 
		{
			m = outcome.get(c);
			if(m != null){
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
		}
		while(m==null || messageRound < r);
			
		return m;
	}
	
	// only for OUTCOME message, that has 3 elements in the payload 
	private int getRoundFromPayload(String payload){
		String[] token = payload.split(",",3);
		return Integer.parseInt(token[2]);
	}
	// only for OUTCOME message, that has 3 elements in the payload 
	private int getDecideFromPayload(String payload){
		String[] token = payload.split(",",3);
		return Integer.parseInt(token[2]);
	}
	// only for OUTCOME message, that has 3 elements in the payload 
	private boolean getBoolFromPayload(String payload){
		String[] token = payload.split(",",3);
		return Boolean.getBoolean(token[1]);
	}
	
	private Integer Majority(Hashtable<Integer, Integer> decisions) {
		Hashtable<Integer, Integer> frequency = new Hashtable<Integer, Integer>();
		
		for(Integer each:decisions.values()){
			int f = frequency.get(each);
			frequency.put(each, f+1);
		}
		
		Integer maxKey = null;
		Integer maxValue = 0; 
		for( int key : frequency.keySet() ) {
		     if( frequency.get(key) > maxValue) {
		         maxValue = frequency.get(key);
		         maxKey = key;
		     }
		}
		
		return maxKey;
	}

	private Hashtable<Integer, Integer> uponReceiveMessages(int r) {
		
		
		boolean isEnd = false;
		
		Hashtable<Integer, Integer> decisionValues = new Hashtable<Integer, Integer>();
		while ( !isEnd ) {

			int F = suspects.size();
			int n = p.getNo();
			
			if(m.contains(r)){
				decisionValues = m.get(r);
				if(decisionValues.size() >= n-F){		
					break;
				}
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

	private void SendMessage(Process parent, String string, int x, int r, int c) {
		// send [VAL: x,r] to G[c]
		Message m = new Message();
		m.setSource(parent.getPid());
		m.setDestination(c);
		m.setType("VAL");
		m.setPayload(x + "," + r);
		parent.unicast(m);
	}

	public void addConsensusMessage(Message m) {
		String payloadMessage = m.getPayload();
		String[] parser = payloadMessage.split(",", 2);
		int decisionValue = Integer.parseInt(parser[0]);
		int roundNumber = Integer.parseInt(parser[1]);
		
		// get list of message list under roundNumber 
		if(!this.m.contains(roundNumber)){
			Hashtable<Integer,Integer> round = new Hashtable<Integer,Integer>();
			round.put(m.getSource(), decisionValue);
			this.m.put(roundNumber, round);
		}else{
			Hashtable<Integer,Integer> round = this.m.get(roundNumber);
			round.put(m.getSource(), decisionValue);
		}
	
	}

	public void addOutcomeMessage(Message m) {
		outcome.put(m.getSource(), m);
	}
	
}
