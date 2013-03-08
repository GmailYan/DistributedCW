import java.util.Hashtable;


public class PFDProcess extends Process {
	int id;
	public PFDProcess(String name, int id, int size) {
		super(name, id, size);
		this.id = id;
		detector = new PerfectFailureDetector(this);
	}

	private IFailureDetector detector;
	private Hashtable<Integer,Message> lastConsensusMessageHolder;

	public void begin() {
		detector.begin();
	}

	public synchronized void receive(Message m) {
		String type = m.getType();
		if (type.equals("heartbeat")) {
			detector.receive(m);
		}else if (type.equals("consensus") && lastConsensusMessageHolder != null) {
			// if holder is null, 
			// then consensus method is not invoked yet and it is not ready to receive the latest consensus message
			lastConsensusMessageHolder.put(m.getSource(), m);
		}
	}

	public static void main(String[] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		PFDProcess p = new PFDProcess(name, id, n);
		p.registeR();
		p.begin();
		p.consensus();
		
	}

	// consensus using rotating coordinator algorithm
	private void consensus() {
		String x = "default"; // the default action value
		PerfectFailureDetector failureDetector = (PerfectFailureDetector)detector;
		lastConsensusMessageHolder = new Hashtable<Integer,Message>();
		
		for(int each:failureDetector.processes ){
			if(each == this.id){
				this.broadcast("consensus", x+","+each);
			}
			
			Message lastConsensusMessage = lastConsensusMessageHolder.get(each);
			if(lastConsensusMessage != null && lastConsensusMessage.getSource() == each){
				String payloadMessage = lastConsensusMessage.getPayload();
				String[] parser = payloadMessage.split(",", 2);
				
				// parser = [VAL: v,r]
				x = parser[0];
				
			}
			
		}
		
		//TODO: how to do the "decide x" ? and at least reset lastConsensusMessage here
		
	}
	
}
