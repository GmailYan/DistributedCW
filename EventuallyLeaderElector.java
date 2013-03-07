public class EventuallyLeaderElector implements IFailureDetector {

	Process p;
	
	public EventuallyLeaderElector(Process p) {
		this.p = p;
	}

	@Override
	public void begin() {
		// TODO Auto-generated method stub

	}

	@Override
	public void receive(Message m) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSuspect(Integer process) {
		return false;
	}

	@Override
	public int getLeader() {
		return -1;
	}

	@Override
	public void isSuspected(Integer process) {
		return;
	}

}
