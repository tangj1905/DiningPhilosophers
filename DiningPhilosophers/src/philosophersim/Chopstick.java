package philosophersim;

/**
 * Basic chopstick class that tracks which philosopher is holding onto a given chopstick.
 * @author jgt31, acw112
 */
public class Chopstick {
	private int owner; // index of philosopher who is currently holding the chopstick

	public Chopstick() {
		owner = -1; // default: -1 if the chopstick is unused
	}
	
	public int getOwner() {
		return owner;
	}
	
	// sets the new owner of the chopstick, only if it's not already being used
	public boolean setOwner(int i) {
		if (owner != -1) return false;
		owner = i;
		return true;
	}
	
	// drops the specified chopstick
	public void drop() {
		owner = -1;
	}
}