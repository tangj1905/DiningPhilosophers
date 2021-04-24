package philosophersim;

import java.util.Random;

/**
 * Class that holds information and states about a philosopher.
 * @author jgt31, acw112
 */
public class Philosopher {

	Random gen = new Random();
	private boolean isCrazy;
	private int position;
	private Status status;
	private int timer; // the amount of time that a philosopher has spent in a particular state

	// initializes a new philosopher with an index, crazy indicator, and status
	public Philosopher(int number, boolean crazy) {
		position = number;
		isCrazy = crazy;
		status = Status.THINKING;
		timer = 0;
	}
	
	public int getPosition() {
		return position;
	}

	public int getFirstChopstick() {
		if (!isCrazy) return position;
		return gen.nextInt(5);
	}

	public int getSecondChopstick() {
		if (!isCrazy) return (position + 1) % 5;
		return gen.nextInt(5);
	}

	public void updateStatus(Status s) {
		status = s;
	}

	public Status getStatus() {
		return status;
	}
	
	public int getTime() {
		return timer;
	}
	
	public void incrementTimer() {
		timer++;
	}
	
	public void resetTimer() {
		timer = 0;
	}
}