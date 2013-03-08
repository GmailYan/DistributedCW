
public class SFDProcess extends Process {

	int pid;
	private int x;
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
			detector.m.put(m.getSource(),m);
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
		x = detector.Consensus();
		Utils.out("End of consensus, "+x+" is decided");
		
	}

	int getX() {
		return x;
	}

	void setX(int x) {
		this.x = x;
	}

}
