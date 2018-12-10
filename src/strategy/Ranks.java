package strategy;

import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import model.Game;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Ranks {

	/** Computes the ranks for the entire game arena where P1 is restricted by a strategy
	 *  @param game    Game instance
	 *  @param reacher Which player wants to reach the targets
	 *  @param str	   Player 1 plays according to this strategy
	 * */
	public static void entireP1restricted(Game game, int reacher, TreeMap<Integer, TreeSet<Integer>> str) {
		assert(game != null);
		assert(reacher == 1 || reacher == 2);
		int opponent = (reacher==1)?2:1;
		LinkedList<Integer> queue1 = new LinkedList<Integer>();
		LinkedList<Integer> queue2 = new LinkedList<Integer>();
		
		TreeMap<Integer,TreeSet<Integer>> tForw = new TreeMap<Integer,TreeSet<Integer>>();
		TreeMap<Integer,TreeSet<Integer>> tBackw = new TreeMap<Integer,TreeSet<Integer>>();
		for (Integer key : game.states.keySet()) {
			tForw.put(key, new TreeSet<Integer>());
			tBackw.put(key, new TreeSet<Integer>());
		}
		for (Integer from : game.transitions.keySet())
			for (Integer into : game.transitions.get(from)) {
				if (game.states.get(from).player == 2 || !str.containsKey(from)) {
					tForw.get(from).add(into);
					tBackw.get(into).add(from);
				} else {
					assert(!str.get(from).isEmpty());
					if (str.get(from).contains(into)) {
						tForw.get(from).add(into);
						tBackw.get(into).add(from);
					}
				}

			}
		
		for (Integer key : game.states.keySet()) {
			game.states.get(key).rank = -1;
			if (game.states.get(key).target) {
				queue1.add(key);
				queue2.add(null);
			}
		}
		
		while (queue1.size() > 0) {
			Integer current = queue1.remove();
			Integer currentHead = queue2.remove();
			
			if (game.states.get(current).rank == -1) { //Only handle states with rank -1
				if ((currentHead != null) && (game.states.get(current).player == opponent)
						&& (tForw.get(current).size()>1)) {
					//Nontarget states of opponent player where he still has an alternative transition
					tForw.get(current).remove(currentHead);
				} else {
					//Targets, states of reaching player, states of opponent player with no alternative transition
					game.states.get(current).rank = 
					((currentHead == null) ? 0 : (game.states.get(currentHead).rank + 1));
					for (Integer tail : tBackw.get(current)) {
						if (game.states.get(tail).rank == -1) { //Only add states with rank -1
							queue1.add(tail);
							queue2.add(current);
						}
					}
				}				
			}
		}
	}

	/** Computes the ranks for the entire game arena
	 *  @param game    Game instance
	 *  @param reacher Which player wants to reach the targets
	 * */
	public static void entire(Game game, int reacher) {
		assert(game != null);
		assert(reacher == 1 || reacher == 2);
		int opponent = (reacher==1)?2:1;
		LinkedList<Integer> queue1 = new LinkedList<Integer>();
		LinkedList<Integer> queue2 = new LinkedList<Integer>();
		
		TreeMap<Integer,TreeSet<Integer>> tForw = new TreeMap<Integer,TreeSet<Integer>>();
		TreeMap<Integer,TreeSet<Integer>> tBackw = new TreeMap<Integer,TreeSet<Integer>>();
		for (Integer key : game.states.keySet()) {
			tForw.put(key, new TreeSet<Integer>());
			tBackw.put(key, new TreeSet<Integer>());
		}
		for (Integer from : game.transitions.keySet())
			for (Integer into : game.transitions.get(from)) {
				tForw.get(from).add(into);
				tBackw.get(into).add(from);
			}
		
		for (Integer key : game.states.keySet()) {
			game.states.get(key).rank = -1;
			if (game.states.get(key).target) {
				queue1.add(key);
				queue2.add(null);
			}
		}
		
		while (queue1.size() > 0) {
			Integer current = queue1.remove();
			Integer currentHead = queue2.remove();
			
			if (game.states.get(current).rank == -1) { //Only handle states with rank -1
				if ((currentHead != null) && (game.states.get(current).player == opponent)
						&& (tForw.get(current).size()>1)) {
					//Nontarget states of opponent player where he still has an alternative transition
					tForw.get(current).remove(currentHead);
				} else {
					//Targets, states of reaching player, states of opponent player with no alternative transition
					game.states.get(current).rank = 
					((currentHead == null) ? 0 : (game.states.get(currentHead).rank + 1));
					for (Integer tail : tBackw.get(current)) {
						if (game.states.get(tail).rank == -1) { //Only add states with rank -1
							queue1.add(tail);
							queue2.add(current);
						}
					}
				}				
			}
		}
	}
	
	/** Computes the ranks in a subgame
	 *  @param game     Game instance
	 *  @param states	State space of the subgame
	 *  @param targets	Target states in this subgame
	 *  @param reacher	Which player wants to reach the targets
	 * */
	public static void subgame(Game game, TreeSet<Integer> states, TreeSet<Integer> targets, int reacher) {
		assert(game != null);
		assert(states != null);
		assert(targets != null);
		assert(reacher == 1 || reacher == 2);
		int opponent = (reacher==1)?2:1;
		
		//Set the targets
		for (Integer key : states)
			if (targets.contains(key))
				game.states.get(key).target = true;
			else
				game.states.get(key).target = false;
		
		//Create the tBackw restricted to the subgame
		//I don't need tForw but I have to remember the sizes
		//(for each state, how many forward transitions it has left)
		TreeMap<Integer,AtomicInteger> tForwSize = new TreeMap<Integer,AtomicInteger>();
		TreeMap<Integer,TreeSet<Integer>> tBackw = new TreeMap<Integer,TreeSet<Integer>>();
		for (Integer key : states) {
			tForwSize.put(key, new AtomicInteger(0));
			tBackw.put(key, new TreeSet<Integer>());			
		}
		for (Integer from : states)
			for (Integer into : game.transitions.get(from))
				if (states.contains(into)) {
					tBackw.get(into).add(from);
					tForwSize.get(from).getAndIncrement();					
				}
		
		
		//Transitions and targets are ready, compute the ranks
		LinkedList<Integer> queue1 = new LinkedList<Integer>();
		LinkedList<Integer> queue2 = new LinkedList<Integer>();
		for (Integer key : states) {
			game.states.get(key).rank = -1;
			if (game.states.get(key).target) {
				queue1.add(key);
				queue2.add(null);			
			}
		}
		
		while (queue1.size() > 0) {
			Integer current = queue1.remove();
			Integer currentHead = queue2.remove();
			
			if (game.states.get(current).rank == -1) //Only handle states with rank -1
				if ((currentHead != null) && (game.states.get(current).player == opponent)
						&& (tForwSize.get(current).get() > 1)) {
					//Nontarget states of opponent player where he still has an alternative transition
					tForwSize.get(current).getAndDecrement();
					tBackw.get(currentHead).remove(current);
				} else {
					//Targets, states of reaching player, states of opponent player with no alternative transition
					game.states.get(current).rank =
					((currentHead == null) ? 0 : (game.states.get(currentHead).rank + 1));
					for (Integer tail : tBackw.get(current))
						if (game.states.get(tail).rank == -1) { //Only add states with rank -1
							queue1.add(tail);
							queue2.add(current);
						}
				}
		}
	}
	
}
