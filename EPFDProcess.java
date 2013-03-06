public class EPFDProcess extends Process {

	public EPFDProcess(String name, int id, int size) {
		super(name, id, size);
		detector = new EventuallyPerfectFailureDetector(this);
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
		EPFDProcess p = new EPFDProcess(name, id, n);
		p.registeR();
		p.begin();
	}

}
