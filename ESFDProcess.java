
public class ESFDProcess extends Process {

	private int x;

	public ESFDProcess(String name, int id, int size, int x) {
		super(name, id, size);
		this.setX(x);
		detector = new EventuallyStrongFailureDetector(this);
	}

	private EventuallyStrongFailureDetector detector;
	private boolean verbose = false;

	public void begin() {
		detector.begin();
	}

	public synchronized void receive(Message m) {
		String type = m.getType();
		if (type.equals("heartbeat")) {
			detector.receive(m);
		} else if(type.equals("VAL")){
			detector.addConsensusMessage(m);
		} else if(type.equals("OUTCOME")){
			detector.addOutcomeMessage(m);
		}
	}

	public static void main(String[] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		int x = Integer.parseInt(args[3]);
		// for consensus: read x implies that there is an extra command line argument for each process
		
		ESFDProcess p = new ESFDProcess(name, id, n, x);
		p.registeR();
		p.begin();
		p.Consensus();

		
	}

	private void Consensus() {
		int x = detector.Consensus();
		if(verbose)Utils.out("End of consensus, "+x+" is decided");
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}
	
}
