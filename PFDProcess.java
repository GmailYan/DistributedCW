
public class PFDProcess extends Process {
	public PFDProcess(String name, int id, int size) {
		super(name, id, size);
		detector = new PerfectFailureDetector(this);
	}

	private IFailureDetector detector;

	public void begin() {
		detector.begin();
	}

	public synchronized void receive(Message m) {
		String type = m.getType();
		if (type.equals("heartbeat")) {
			detector.receive(m);
		}
	}

	public static void main(String[] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		PFDProcess p = new PFDProcess(name, id, n);
		p.registeR();
		p.begin();
	}
	
}
