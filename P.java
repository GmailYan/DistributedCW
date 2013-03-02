
public class P extends Process {

	private IFailureDetector detector;
	
	public P(String name, int pid, int n) {
		super(name, pid, n);
		detector = new PerfectFailureDetector(this);
	}
	
	public void begin(){
		detector.begin();
	}

	public synchronized void receive (Message m){
		String type = m.getType();
		if(type.equals("heartbeat")){
			detector.receive(m);
		}
	}
	
	public static void main(String[] args){
		P p = new P ("P1", 1, 10);
		p.registeR();
		p.begin();
	}
}
