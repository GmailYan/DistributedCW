
public class SFDProcess extends Process {

	int pid;
	private int x;
	
	// enable debug output to console by set this to true
	private boolean verbose = false;
	public SFDProcess(String name, int pid, int n, int x) {
		super(name, pid, n);
		this.pid = pid;
		this.setX(x);
		detector = new StrongFailureDetector(this);
	}

	private static StrongFailureDetector detector;
	
	public void begin() {
		detector.begin();
	}
	
	public synchronized void receive(Message m) {
		String type = m.getType();
		if (type.equals("heartbeat")) {
			detector.receive(m);
		} else if (type.equals("VAL") ) {
			detector.addVALMessage(m);
		}
	}
	
	public static void main(String[] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		int x = Integer.parseInt(args[3]);
		// for consensus: read x implies that there is an extra command line argument for each process
		
		SFDProcess p = new SFDProcess(name, id, n, x);
		p.registeR();
		p.begin();
		p.Consensus();
	}

	private void Consensus() {
		int x = detector.Consensus();
		if(verbose )Utils.out("End of consensus, "+x+" is decided");
	}

	int getX() {
		return x;
	}

	void setX(int x) {
		this.x = x;
	}

}
