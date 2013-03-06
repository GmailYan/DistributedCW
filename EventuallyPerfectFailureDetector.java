
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class EventuallyPerfectFailureDetector implements IFailureDetector {

	Process p;
	private LinkedList<Integer> suspects;
	// storing a series of message delay for each process,  process is identified by a Integer(Table key)
	Hashtable<Integer,LinkedList<Long>> messageDelays;
	
	// one average delay for each process
	Hashtable<Integer,Long> timeoutUsingAvgDelay;
	Timer t;
	private Integer nextLeader;

	static final int Delta = 1000; /* 1sec */
	
	class PeriodicTask extends TimerTask {
		public void run() {
			p.broadcast("heartbeat", String.format("%d", System.currentTimeMillis()));
			//Utils.out(p.getName()+" broadcasted heart beat at "+ new Date());
		}
	}

	public EventuallyPerfectFailureDetector(Process p) {
		this.p = p;
		t = new Timer();
		setSuspects(new LinkedList<Integer>());
		messageDelays = new Hashtable<Integer,LinkedList<Long>>();
		timeoutUsingAvgDelay = new Hashtable<Integer,Long>();
	}

	@Override
	public void begin() {
		t.scheduleAtFixedRate(new PeriodicTask(), 0, Delta);
	}

	@Override
	public void receive(Message m) {
		// upon receiving a heart beat message, update the timeout
		int senderProcess = m.getSource();
		long realisedDelay = System.currentTimeMillis() - Long.parseLong(m.getPayload());
		
		// if the history does not exist then create empty list
		if(!messageDelays.containsKey(senderProcess)){
			messageDelays.put(senderProcess, new LinkedList<Long>());
		}
		LinkedList<Long> messageHistory = messageDelays.get(senderProcess);
		messageHistory.add(realisedDelay);
		Long sumMessageDelay = sum(messageHistory);
		Long average = (sumMessageDelay / messageHistory.size() );
		Long timeout = System.currentTimeMillis() + 2*average;
		timeoutUsingAvgDelay.put(senderProcess, timeout);
		
		//Utils.out("new timeout "+ new Date(timeout));
	}

	@Override
	public boolean isSuspect(Integer pid) {
		return getSuspects().contains(pid);
	}

	@Override
	public int getLeader() {
		return this.nextLeader;
	}

	@Override
	public void isSuspected(Integer process) {
		Long timeout = timeoutUsingAvgDelay.get(process);
		Long currentTime = System.currentTimeMillis();
		
		if(currentTime >= timeout){
			Utils.out(String.format("Process %s been suspected",process));
			addSuspects(process);
		}
		
		return;
	}
	
	private void addSuspects(Integer process) {
		LinkedList<Integer> sList = getSuspects();
		sList.add(process);
		setSuspects(sList);
		
		//suspect change trigger re calculating leader
		RecalculatingLeader();
	}

	private void RecalculatingLeader() {
		Enumeration<Integer> allProcess = messageDelays.keys();
		ArrayList<Integer> allProcessList = Collections.list(allProcess);
		allProcessList.removeAll(getSuspects());
		Integer processWithHighestID = Collections.max(allProcessList); 
		this.nextLeader = processWithHighestID;
	}

	private Long sum(List<Long> numbers) {
		Long retSum = 0L;
	    for(Number i : numbers) {
	        retSum += i.longValue();
	    }
	    return retSum;
	}

	LinkedList<Integer> getSuspects() {
		return suspects;
	}

	void setSuspects(LinkedList<Integer> suspects) {
		this.suspects = suspects;
	}

}
