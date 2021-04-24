package philosophersim;

import java.util.Random;
import java.io.*;

/**
 * Main simulation class. Outputs of the simulation will largely be written to "output.txt"
 * @author jgt31, acw112
 */
public class DiningPhilosophersSimulation {

	/* PARAMETERS THAT CAN BE CHANGED */
	final static int SIMULATION_TIME = 1000;
	final static double BASE_THINK_PROBABILITY = 0.8;			// if we're currently thinking right now, this is the base probability that we'll keep thinking
	final static double BASE_EAT_PROBABILITY = 0.75;			// if we're currently eating right now, this is the base probability that we'll keep eating
	final static double PROBABILITY_DECAY_RATE = 0.02;			// how much will probabilities change after each unit of time in the same state?
	
	/* DEADLOCK HANDLING PARAMETERS */
	final static boolean RANDOM_DROP = false;				// if false, we'll use delayed recovery instead
	final static int RECOVERY_DELAY = 16;					// if using delayed recovery, this is how many time steps to wait...
	final static double RANDOM_DROP_BASE_PROBABILITY = 0.02;		// if using random drop, this is the base probability of dropping a chopstick if not eating
	
	/* THINGS TO WRITE TO OUTPUT FILE */
	final static boolean WRITE_TIME_STATS = false;
	final static boolean WRITE_PHILOSOPHER_STATES = false;
	final static boolean WRITE_CHOPSTICK_STATES = false;
	
	static Philosopher[] philosophers;
	static Chopstick[] chopsticks;

	static Random gen = new Random();
	
	public static void main(String[] args) {

		philosophers = new Philosopher[5];
		chopsticks = new Chopstick[5];

		// populating the table with five chopsticks
		for (int i = 0; i < 5; i++) {
			chopsticks[i] = new Chopstick();
		}

		// populating the table with five philosophers
		for (int j = 0; j < 5; j++) {
			switch (j) {
				case 2: // crazy philosopher sits at index 2
					philosophers[j] = new Philosopher(j, true);
					break;

				default: // every other philosopher is normal
					philosophers[j] = new Philosopher(j, false);
			}
		}

		// let's start our simulation!
		try {
			simulate();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void simulate() throws IOException {
		int timeEating = 0;
		int timeThinking = 0;
		int timeWaiting = 0;
		
		FileWriter writer = new FileWriter("output.txt");
		
		for (int time = 1; time <= SIMULATION_TIME; time++) {
			
			writer.write("============ CURRENT TIME: " + time + " ============\n");
			
			// first, we need to randomize the order in which the philosophers do their things
			shuffle();

			// for each philosopher...
			for (Philosopher p : philosophers) {
				
				int position = p.getPosition();
				
				// check how long the philosopher's been in this state (this will affect many probabilities)
				int timeSpent = p.getTime();
				
				switch(p.getStatus()) {
					case THINKING:
						timeThinking++;
						
						// the actual probability we're dealing with, taking the amount of time spent into account
						double thinkProbability = BASE_THINK_PROBABILITY - PROBABILITY_DECAY_RATE * timeSpent;
						
						if (Math.random() < thinkProbability) {
							// the do-nothing event: we'll keep thinking
							p.incrementTimer();
						} else {
							// philosopher's become hungry! requesting first chopstick...
							int chopIndex = p.getFirstChopstick();
							
							if (chopsticks[chopIndex].setOwner(position)) {
								// philosopher successfully picked up chopstick!
								p.updateStatus(Status.WAITING_2);
							} else {
								// sorry, the chopstick's in use right now.
								p.updateStatus(Status.WAITING_1);
							}
							p.resetTimer();
						}
						
						break;
						
					case WAITING_1:
						timeWaiting++;
						
						// attempting to pick up the first chopstick...
						int chop1 = p.getFirstChopstick();
						
						if (chopsticks[chop1].setOwner(position)) {
							// successful!
							p.updateStatus(Status.WAITING_2);
							p.resetTimer();
						} else {
							p.incrementTimer();
						}
						break;
						
					case WAITING_2:
						timeWaiting++;
						
						// attempting to pick up second chopstick...
						int chop2 = p.getSecondChopstick();
						
						if (chopsticks[chop2].setOwner(position)) {
							// successful! we can start eating now
							p.updateStatus(Status.EATING);
							p.resetTimer();
						} else {
							// our drop probability increases by 0.01 for every time step the philosopher has been waiting
							double dropProbability = RANDOM_DROP_BASE_PROBABILITY + 0.01 * timeSpent;
							
							// if we're still waiting, we have a random drop chance!
							if (RANDOM_DROP && Math.random() < dropProbability) {
								int ch = dropChopstick(p.getPosition());
								
								p.updateStatus(Status.THINKING);
								p.resetTimer();
								writer.write("PHILOSOPHER " + position + " RANDOMLY DROPPED CHOPSTICK " + ch + "\n");
							} else
								p.incrementTimer();
						}
						
						break;
						
					case EATING:
						timeEating++;
						
						// works the same way as thinkProbability above...
						double eatProbability = BASE_EAT_PROBABILITY - PROBABILITY_DECAY_RATE * timeSpent;
						
						if (Math.random() < eatProbability )
							// we'll just keep eating...
							p.incrementTimer();
						else {
							// philosopher is no longer hungry, so we'll drop a chopstick
							
							dropChopstick(p.getPosition());
							
							p.updateStatus(Status.STOPPED_EATING);
							p.resetTimer();
						}
						
						break;
						
					case STOPPED_EATING:
						timeThinking++;
						
						// philosopher has already dropped one of their chopsticks, and they're dropping the other one now.
						dropChopstick(p.getPosition());
						
						p.updateStatus(Status.THINKING);
				}
			}
			
			// delayed recovery method:
			if (!RANDOM_DROP) {
				boolean isDeadlocked = true;
				
				// checking if all of the philosophers have been waiting longer than our recovery delay threshold...
				for (int i = 0; i < 5; i++) {
					Philosopher ph = philosophers[i];
					if (ph.getStatus() != Status.WAITING_2 || ph.getTime() < RECOVERY_DELAY)
						isDeadlocked = false;
				}
				
				// if all of them have been waiting longer, then we'll force all of the chopsticks to be dropped.
				// philosophers will now be thinking as well.
				if (isDeadlocked) {
					for (int j = 0; j < 5; j++) {
						Chopstick ch = chopsticks[j];
						Philosopher ph = philosophers[j];
						ch.drop();
						ph.updateStatus(Status.THINKING);
						ph.resetTimer();
					}
					writer.write("DELAYED RECOVERY OCCURRED, USING WAIT TIME = " + RECOVERY_DELAY + "\n");
				}
			}
			
			// time to write an update about what happened at this time step to the file:
			
			if (WRITE_TIME_STATS) {
				writer.write("TOTAL TIME SPENT THINKING: " + timeThinking + " (" + Math.round((double)timeThinking / (5 * time) * 10000) / 100.0 + "%)\n");
				writer.write("TOTAL TIME SPENT WAITING: " + timeWaiting + " (" + Math.round((double)timeWaiting / (5 * time) * 10000) / 100.0 + "%)\n");
				writer.write("TOTAL TIME SPENT EATING: " + timeEating  + " (" + Math.round((double)timeEating / (5 * time) * 10000) / 100.0 + "%)\n");
			}
			
			if (WRITE_PHILOSOPHER_STATES) {
				for (int i = 0; i < 5; i++) {
					Philosopher ph = philosophers[i];
					writer.write("PHILOSOPHER " + ph.getPosition() + " STATUS: " + ph.getStatus().name() + " FOR " + ph.getTime() + " TIME STEPS\n");
				}
			}
			
			if (WRITE_CHOPSTICK_STATES) {
				for (int i = 0; i < 5; i++) {
					Chopstick c = chopsticks[i];
					int owner = c.getOwner();
					
					if (owner == -1) 
						writer.write("CHOPSTICK " + i + " OWNER: EMPTY\n");
					else
						writer.write("CHOPSTICK " + i + " OWNER: " + owner + "\n");
				}
			}
			
			writer.write("\n");
		}
		// printing out some final information
		System.out.println("SIMULATION TIME: " + SIMULATION_TIME);
		System.out.println("TOTAL TIME SPENT THINKING: " + timeThinking + " (" + Math.round((double)timeThinking / (5 * SIMULATION_TIME) * 10000) / 100.0 + "%)");
		System.out.println("TOTAL TIME SPENT WAITING: " + timeWaiting + " (" + Math.round((double)timeWaiting / (5 * SIMULATION_TIME) * 10000) / 100.0 + "%)");
		System.out.println("TOTAL TIME SPENT EATING: " + timeEating  + " (" + Math.round((double)timeEating / (5 * SIMULATION_TIME) * 10000) / 100.0 + "%)");
		
		writer.close();
	}

	// shuffles the philosopher array using the Fisher-Yates method (not that it matters or anything...)
	public static void shuffle() {
		for (int i = 4; i >= 0; i--) {
			int swpIndex = gen.nextInt(i + 1);

			// swaps the indexes..
			Philosopher temp = philosophers[swpIndex];
			philosophers[swpIndex] = philosophers[i];
			philosophers[i] = temp;
		}
	}
	
	// drops a chopstick that the specified philosopher is holding. not a great way to do this, but there's only 5 chopsticks so it's okay
	public static int dropChopstick(int philosopherIndex) {
		for (int i = 0; i < 5; i++) {
			if (chopsticks[i].getOwner() == philosopherIndex) {
				chopsticks[i].drop();
				return i;
			}
		}
		return -1;
	}
}
