package creator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import model.Game;
import strategy.Ranks;
import util.Pair;
import util.Util;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Aiger {
	
	/* Aiger Game Info:
	 * P1 is environment, plays reachability (output bit is 1)
	 * P2 is controller, plays safety (output bit stays 0)
	 * P1 states: latchbits+outputbit
	 * P2 states: latchbits+envbits
	 * Transitions 1->2: envbits
	 * Transitions 2->1: contbits
	 * Start state: P1, 0..0, 0
	 * All the states reachable from the start state
	 * and only those states are valid
	*/
	
	private static int latchbits;
	private static int envbits;
	private static int contbits;
	
	private static LinkedList<Integer> queue;
	private static LinkedList<Integer> queuenew;
	private static int distance;
	private static HashMap<ArrayList<Boolean>,Integer> valuesIntoNameP1;
	private static HashMap<ArrayList<Boolean>,Integer> valuesIntoNameP2;
	
	/**
	 * Computes successor state given a state-action pair
	 * @param game	Compute in this game
	 * @param state	Move from this state
	 * @param label	Move using this action
	 * @return		Values of successor state
	 */
	public static ArrayList<Boolean> successor(Game game, int state, int label) {
		assert(game != null);
		assert(game.states.containsKey(state));
		if (game.states.get(state).player == 1) {
			// Player1 state
			assert(label >= 0 && label < Util.bitpower(game.varActionP1no()));
			ArrayList<Boolean> result = new ArrayList<Boolean>(game.varStateP2no());
			for (int j=0; j<latchbits; j++) result.add(game.states.get(state).values.get(j));
			int lhelp = label;
			for (int j=0; j<envbits; j++) {
				result.add((lhelp % 2) == 1);
				lhelp /= 2;
			}
			assert(lhelp == 0);
			return result;
			
		} else {
			// Player2 state
			assert(label >= 0 && label < Util.bitpower(game.varActionP2no()));
			ArrayList<Boolean> oldstate = new ArrayList<Boolean>(latchbits);
			for (int j=0; j<latchbits; j++) oldstate.add(game.states.get(state).values.get(j));
			ArrayList<Boolean> einput = new ArrayList<Boolean>(envbits);
			for (int j=latchbits; j<latchbits+envbits; j++) einput.add(game.states.get(state).values.get(j));
			
			ArrayList<Boolean> cinput = new ArrayList<Boolean>(contbits);
			int lhelp = label;
			for (int j=0; j<contbits; j++) {
				cinput.add((lhelp % 2) == 1);
				lhelp /= 2;
			}
			assert(lhelp == 0);
			
			assert(AigerCircuit.getInstance() != null);
			Pair<Boolean,ArrayList<Boolean>> circuitr = AigerCircuit.getInstance().compute(einput, cinput, oldstate);
			ArrayList<Boolean> result = new ArrayList<Boolean>(game.varStateP1no());
			for (int j=0; j<latchbits; j++) result.add(circuitr.second().get(j));
			result.add(circuitr.first());
			return result;
		}
	}
	
	/**
	 * Main game creation method, used by other methods that tune the parameters
	 * @param game				Can reuse previously created game to speed up creation
	 * @param filename			Name of the AIGER file
	 * @param distancelimit		State this far away from initial state become targets
	 * @param storetransitions	False when you just want to estimate state space size
	 * @return 					(Game, Small enough to work with?)
	 */
	private static Pair<Game, Boolean> create(Game game, String filename, int distancelimit, boolean storetransitions) {
		boolean fromscratch = (game == null);
		if (fromscratch) {
			game = new Game();
			
			try { AigerCircuit.getInstance().parse(filename); }
			catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				System.out.println("Aiger game creation failed.");
				return null;
			}
			
			latchbits = AigerCircuit.getInstance().getLatchBits();
			envbits = AigerCircuit.getInstance().getEnvBits();
			contbits = AigerCircuit.getInstance().getContBits();

			queue = new LinkedList<Integer>();
			queuenew = new LinkedList<Integer>();
			distance = 0;
			valuesIntoNameP1 = new HashMap<ArrayList<Boolean>,Integer>();
			valuesIntoNameP2 = new HashMap<ArrayList<Boolean>,Integer>();
			
			//latchbits+1, latchbits+envbits, envbits, contbits
			ArrayList<String> labels = AigerCircuit.getInstance().getSafetyLabels();
			assert(labels.size() == latchbits+envbits+contbits);
			
			for (int i=0; i<latchbits; i++)
				game.varStateP1.add(labels.get(i));
			game.varStateP1.add("outputBit");
			for (int i=0; i<latchbits+envbits; i++)
				game.varStateP2.add(labels.get(i));
			for (int i=0; i<envbits; i++)
				game.varActionP1.add(labels.get(latchbits + i));
			for (int i=0; i<contbits; i++)
				game.varActionP2.add(labels.get(latchbits + envbits + i));
			
			ArrayList<Boolean> initialValues = new ArrayList<Boolean>(game.varStateP1no());
			for (int i=0; i<game.varStateP1no(); i++) initialValues.add(false);
			valuesIntoNameP1.put(initialValues, game.stateSize+1);
			game.addState(game.stateSize+1, 1, initialValues);
			game.initialState = 1;
			queue.add(1);
			
		} else {
			// check that game is in the state we expect it to be
			System.out.print("REUSE ");
			assert(game != null);
			assert(latchbits > 0);
			assert(envbits > 0);
			assert(contbits > 0);
			assert(!queue.isEmpty());
			assert(queuenew.isEmpty());
			assert(distance > 0);
			assert(!valuesIntoNameP1.isEmpty());
			assert(!valuesIntoNameP2.isEmpty());
			assert(game.stateSize > 0);
			assert(game.transitionSize > 0);
			assert(game.varStateP1.size() == latchbits + 1);
			assert(game.varStateP2.size() == latchbits + envbits);
			assert(game.varActionP1.size() == envbits);
			assert(game.varActionP2.size() == contbits);
			
			// remove 'fake' target status from states on the previous distance limit
			for (Integer key : queue) {
				assert(game.states.get(key).target);
				if (game.states.get(key).player == 2 || 
						!game.states.get(key).values.get( game.states.get(key).values.size() - 1 ))
					game.states.get(key).target = false;
				// and remove selfloops
				game.transitions.remove(key,key);
			}
		}
		
		// generate state space until you reach the distance limit
		while (distance < distancelimit && !queue.isEmpty()) {
			distance++;
			while (!queue.isEmpty()) {
				Integer current = queue.remove();
				if (game.states.get(current).player == 1) {
					// Player1 state
					for (int i=0; i<Util.bitpower(envbits); i++) {
						ArrayList<Boolean> newValues = successor(game, current, i);
						
						Integer newName = valuesIntoNameP2.get(newValues);
						if (newName == null) {
							newName = game.stateSize + 1;
							game.addState(newName, 2, newValues);
							valuesIntoNameP2.put(game.states.get(newName).values, newName);
							queuenew.add(newName);
						}
						
						if (storetransitions)
							game.addTransition(current, newName);
						
						// state space estimate is above million
						if (!storetransitions && game.stateSize > 1000000)
							return new Pair<Game,Boolean>(game, false);
						
						// state space above 300k, too big to solve
						if (storetransitions && game.stateSize > 300000)
							return new Pair<Game,Boolean>(game, false);
					}
				} else {
					// Player2 state
					for (int i=0; i<Util.bitpower(contbits); i++) {
						ArrayList<Boolean> newValues = successor(game, current, i);
						
						Integer newName = valuesIntoNameP1.get(newValues);
						if (newName == null) {
							newName = game.stateSize + 1;
							game.addState(newName, 1, newValues);
							valuesIntoNameP1.put(game.states.get(newName).values, newName);
							queuenew.add(newName);
							if (newValues.get(newValues.size()-1))
								game.states.get(newName).target = true;
						}
						
						if (storetransitions)
							game.addTransition(current, newName);
						
						// state space estimate is above million
						if (!storetransitions && game.stateSize > 1000000)
							return new Pair<Game,Boolean>(game, false);
						
						// state space above 300k, too big to solve
						if (storetransitions && game.stateSize > 300000)
							return new Pair<Game,Boolean>(game, false);
					}
				}
			}
			
			assert(queue.isEmpty());
			if (!queuenew.isEmpty()) {
				queue = queuenew;
				queuenew = new LinkedList<Integer>();
				assert(!queue.isEmpty());
			}
		}
		
		// make all states at the distance limit targets
		if (!queue.isEmpty())
			for (Integer key : queue) {
				game.states.get(key).target = true;
				// and create selfloops
				game.addTransition(key, key);
			}
		
		return new Pair<Game, Boolean>(game, true);
	}
	
	/**
	 * Used for creating a game restricted by a distance limit from the initial state
	 * @param game				Can reuse previously created game to speed up creation
	 * @param filename			Name of the AIGER file
	 * @param distancelimit		State this far away from initial state become targets
	 * @return 					(Game, Small enough to work with?)
	 */
	public static Pair<Game, Boolean> create(Game game, String filename, int distancelimit) {
		assert(distancelimit > 0);
		return create(game, filename, distancelimit, true);
	}

	/**
	 * Used for estimating state space size
	 * @param filename			Name of the AIGER file
	 * @param storetransitions	Has to be false
	 * @return 					State space size or 1mil+1 if it's above 1mil
	 */
	public static int create(String filename, Boolean storetransitions) {
		assert(!storetransitions);
		Pair<Game, Boolean> result = create(null, filename, Integer.MAX_VALUE, false);
		if (result == null) return -1;
		return result.first().stateSize;
	}
	
	/** Creates a game from the AIGER file benchmarks/aiger/filename.aag
	 * @param filename	Name of the AIGER file
	 * @return 			(Game, Distance restriction)
	 * */
	public static Pair<Game,Integer> create(String filename) {
		int lowerbound = 0;   // this size is not enough
		int upperboundTT = Integer.MAX_VALUE; // this size is enough
		int upperboundFF = Integer.MAX_VALUE; // gave up, state space too big
		
		int testsize = 4;
		boolean stop = false;
		Game game = null;
		while (!stop) {
			System.out.print("Distance limit = "+testsize+"... ");
			Pair<Game, Boolean> result = create(game, filename, testsize);
			if (result == null) return null;
			
			if (!result.second()) { // state space too big
				stop = true;
				upperboundFF = testsize;
				System.out.println("state space too big.");
				break;
			}
			
			game = result.first();
			Ranks.entire(game, 1);
			
			if (game.states.get(game.initialState).rank == -1) {
				// Winning for P2 even with the distance constraint
				stop = true;
				upperboundTT = testsize;
				System.out.println("winning for Player 2!");
			} else {
				lowerbound = testsize;
				System.out.println("distance too small.");
				testsize *= 2;
			}
		}
		
		game = null;
		System.gc();
		
		assert(Math.min(upperboundTT, upperboundFF) < Integer.MAX_VALUE);
		assert((lowerbound == 0 && Math.min(upperboundTT, upperboundFF) == 4)
				|| 2 * lowerbound == Math.min(upperboundTT, upperboundFF));
		
		int change = Math.min(upperboundTT, upperboundFF) - lowerbound;
		assert((lowerbound == 0 && change == 4) || change == lowerbound);
		assert(change % 2 == 0);
		change /= 2;
		testsize = Math.min(upperboundTT, upperboundFF);
		testsize -= change;
		assert(testsize - lowerbound == Math.min(upperboundTT, upperboundFF) - testsize);
		
		while (lowerbound + 1 != Math.min(upperboundTT, upperboundFF)) {
			System.out.print("Distance limit = "+testsize+"... ");
			Pair<Game, Boolean> result = create(game, filename, testsize);
			if (result == null) return null;
			
			if (!result.second()) { // state space too big
				assert(upperboundTT == Integer.MAX_VALUE);
				upperboundFF = testsize;
				System.out.println("state space too big.");
				
				game = null;
				System.gc();
				
				if (change == 1) {
					assert(lowerbound + 1 == upperboundFF);
					continue;
				}
				
				assert(change % 2 == 0);
				change /= 2;
				testsize -= change;
				assert(testsize - lowerbound == upperboundFF - testsize);
				
				continue;
			}
			
			game = result.first();
			Ranks.entire(game, 1);
			
			if (game.states.get(game.initialState).rank == -1) {
				// Winning for P2 even with the distance constraint
				upperboundTT = testsize;
				System.out.println("winning for Player 2!");
				
				game = null;
				System.gc();				
				
				if (change == 1) {
					assert(lowerbound + 1 == upperboundTT);
					continue;
				}
				
				assert(change % 2 == 0);
				change /= 2;
				testsize -= change;
				assert(testsize - lowerbound == upperboundTT - testsize);
				
			} else {
				lowerbound = testsize;
				System.out.println("distance too small.");
				
				// dont garbage collect, keep the game for reuse
				
				if (change == 1) {
					assert(lowerbound + 1 == Math.min(upperboundTT, upperboundFF));
					continue;
				}
				
				assert(change % 2 == 0);
				change /= 2;
				testsize += change;
				assert(testsize - lowerbound == Math.min(upperboundTT, upperboundFF) - testsize);
			}
		}
		
		game = null;
		System.gc();
		
		if (upperboundTT == Integer.MAX_VALUE) {
			System.out.println("FAIL: Didn't find any winning strategy.");
			return new Pair<Game, Integer>(null, -1);
		}
		
		System.out.println("SUCCESS: The smallest winning strategy is for distance "+upperboundTT+".");
		Pair<Game, Boolean> result = create(game, filename, upperboundTT);
		assert(result.second());
		
		return new Pair<Game, Integer>(result.first(), upperboundTT);
	}
	
}
