public class ELEProcess extends Process {

	public ELEProcess(String name, int pid, int n) {
		super(name, pid, n);
		elector = new EventuallyLeaderElector(this);
	}

	private IFailureDetector elector;
	
	public void begin() {
	}

	public static void main(String[] args) {
		String name = args[0];
		int id = Integer.parseInt(args[1]);
		int n = Integer.parseInt(args[2]);
		ELEProcess p = new ELEProcess(name, id, n);
		p.registeR();
		p.begin();
	}
}
