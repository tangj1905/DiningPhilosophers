# DiningPhilosophers

A simulation of the Dining Philosophers problem. Done in collaboration with acw112.

## Code description

Generally, the program begins by setting up the five chopsticks and the five philosophers. The 3rd philosopher (or the one sitting at index 2) is initialized as a "crazy philosopher", and we'll discuss what this means in a bit. Then, the simulation starts, running a large loop that corresponds with each time step of the Dining Philosophers simulation. For each time step, the program will do these three things:

1. Shuffle the array that holds the philosophers. This way, the philosophers will be accessed in a randomized order.
2. For each philosopher in the shuffled array, we will have the philosopher perform some action based on their current state.
3. If applicable, the delayed recovery method is invoked to clear up any potential deadlocks.
4. Update the output file with cumulative run statistics.

Step 2 here is the most important part, and we will definitely explain this more in detail.

First of all, the chopsticks are described by the `Chopstick.java` class. They are very simple, only storing information about which philosopher is currently holding on to them. There are some helper methods specifically for dropping or having a philosopher pick up a chopstick as well.

Next, the `Philosopher.java` class. This class describes two types of philosophers: normal ones, and crazy ones. Normal philosophers can only pick up chopsticks that are adjacent to them, and they always pick up the left chopstick first before picking up the right one next. Crazy philosophers have no regard for such order; they are able to pick up whichever chopstick they'd like. Here, we just made it so that the crazy philosopher picks up a random chopstick for every attempt.

Philosophers also have a status, which is described by the `Status.java` enumerated class. There are five statuses, namely:

1. **THINKING:** The philosopher is currently thinking and is not attempting to pick up a chopstick.
1. **WAITING_1:** The philosopher has attempted to pick up their first chopstick but is currently unable to. They are waiting for the chopstick to become available.
1. **WAITING_2:** The philosopher has picked up a chopstick and is currently waiting to pick up their second chopstick.
1. **EATING:** The philosopher has picked up both chopsticks and is currently eating.
1. **STOPPED_EATING:** The philosopher has finished eating and has put down one of their chopsticks. They will then put down their other chopstick and return to their **THINKING** status.

In addition to their statuses, each philosopher also has an internal timer. This timer essentially keeps track of how long they have been in a particular status, and is meant to reset whenever the status changes. This timer is relevant because the probability of some event occurring partially depends on this timer: in general, if a philosopher has occupied a state for a longer time, it is more likely that they will switch their state.

Now that we've covered what chopsticks and philosophers look like, let's discuss what happens during step 2 mentioned above, as this is the bulk of our simulation. Depending on the philosopher's state, this is what the philosopher does at each time step:

1. **THINKING:** The philosopher first calculates the probability that they continue to think. This probability starts at **BASE_THINK_PROBABILITY**, but for every time step that the philosopher is thinking, this value decreases by **PROBABILITY_DECAY_RATE**, meaning that the philosopher is less likely to continue thinking if they have thought for a longer time. If the philosopher decides to continue thinking, then its counter is incremented and the next philosopher's turn is up. If the philosopher stops thinking, then they will reset their timer and attempt to pick up their first chopstick, based on the rules of what kind of philosopher they are.
   1. If they can successfully pick up their first chopstick, their status will be updated to **WAITING_2**.
   1. If not, their status is updated to **WAITING_1**.
1. **WAITING_1:** The philosopher attempts to pick up their first chopstick. If they're successful, their status is updated to **WAITING_2**. If not, they'll continue to wait in the same status.
1. **WAITING_2:** The philosopher attempts to pick up their second chopstick. If they are successful, their status is updated to **EATING** and their timer is reset. Otherwise, one of the following things can happen:
   1. If the **RANDOM_DROP** flag is set, then the philosopher has a chance of randomly dropping their chopstick. This probability starts at **RANDOM_DROP_BASE_PROBABILITY**, and increases by 0.01 for every time step that the philosopher is waiting. If the chopstick is dropped, the philosopher's timer is reset and their status is updated to **THINKING**.
   1. If this flag is not set, then the philosopher will continue to wait.
1. **EATING:** The philosopher calculates the probability that they continue to eat, in the same way that the thinking probability is determined. **BASE_EAT_PROBABILITY** is the starting point here, and decreases by **PROBABILITY_DECAY_RATE** every time step. If the philosopher decides to stop eating, then they will drop one of their chopsticks and update their state to **STOPPED_EATING**.
1. **STOPPED_EATING:** The philosopher has dropped one of their chopsticks and proceeds to drop their second. Their status is immediately updated to **THINKING**.

Finally, let's discuss our two methods of addressing deadlocks: random drops and delayed recovery. Random drops are already covered in the above description, and occurs during a philosopher's **WAITING_2** stage. If the **RANDOM_DROP** flag is set to false, then the delayed recovery method is invoked after every philosopher has performed their action for the time step.

The delayed recovery method looks for two things:

1. Every philosopher is in the **WAITING_2** stage. We can guarantee that deadlocks will only happen if the philosophers are in this stage, because if there are any philosophers not in this stage, then there will always be at least one available chopstick on the table. Since philosopher 3 is a crazy philosopher, they're guaranteed to be able to grab this chopstick at some point and proceed.
2. Every philosopher has been waiting in this stage for at least a certain number of time steps, determined by the **RECOVERY_DELAY** parameter. This can be thought of as a kind of penalty for recovery.

Once both of these things can be found, then every philosopher drops their chopstick. Their statuses are updated to **THINKING**, and their timers are reset.

The only other aspect about this program is the amount of information that is printed to the output file. However, that's fairly self-explanatory from the code, so I won't discuss it much here :)
