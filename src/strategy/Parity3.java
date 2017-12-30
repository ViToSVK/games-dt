package strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import machinelearning.DecisionTree;
import model.Game;
import model.GameInfo;
import util.Pair;
import util.Tarjan;
import util.Util;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Parity3 {

	/**
	 * Computes classical (one action per state) parity3 "min odd" strategy for player 2<br>
	 * Returns null if the initial state is not part of the winning region for player 2
	 * @param game			Compute for this game
	 * @param gameinfo		Info about the game
	 * @return				The strategy
	 */
	public static Strategy classical(Game game, GameInfo gameinfo) {
		assert(game != null);
		assert(gameinfo.type == 'r');
		
		TreeMap<Integer,TreeSet<Integer>> allowed = solve(game);
		if (allowed == null) return null;
		
		// Turn the strategy into bitvector format
		
		Strategy result = new Strategy(2,'p');
		
		LinkedList<Integer> queue = new LinkedList<Integer>();
		TreeSet<Integer> flag = new TreeSet<Integer>();
		queue.add(game.initialState);
		flag.add(game.initialState);
		
		TreeMap<Integer,AtomicInteger> used = new TreeMap<Integer,AtomicInteger>();
		
		while (queue.size() > 0) {
			Integer from = queue.remove();
			if (game.states.get(from).player == 1) { // the opponent is allowed to move anywhere
				for (Integer into : game.transitions.get(from))
					if (!flag.contains(into)) {
						assert(allowed.containsKey(into));
						queue.add(into);
						flag.add(into);
					}
			} else { // restricted by the strategy
				assert(allowed.containsKey(from));
				
				int choicefrom = -1;
				int valuefrom = -1;
				int succfrom = -1;
				int choicenew = -1;
				int valuenew = -1;
				int succnew = -1;

				for (int i=0; i<Util.bitpower(game.varActionP2no()); i++) {
					int into = game.successor(gameinfo, from, i, 1);
					if (allowed.get(from).contains(into)) { // We can consider this label
						if (used.containsKey(i)) {
							if (used.get(i).intValue() > valuefrom) {
								choicefrom = i;
								valuefrom = used.get(i).intValue();
								succfrom = into;
								choicenew = -1;
								valuenew = -1;
								succnew = -1;
							}
						}
						else if (choicefrom == -1 && Util.numberOfOnes(i) > valuenew) {
								choicenew = i;
								valuenew = Util.numberOfOnes(i);
								succnew = into;
						}
					}
				}
				
				if (choicefrom != -1) {
					assert(valuefrom > -1 && succfrom > -1);
					used.get(choicefrom).incrementAndGet();
					result.addBitv(game, from, choicefrom, true);
					
					for (int i=0; i<Util.bitpower(game.varActionP2no()); i++)
						if (i != choicefrom)
							result.addBitv(game, from, i, false);
					
					if (!flag.contains(succfrom)) {
						queue.add(succfrom);
						flag.add(succfrom);							
					}
				} else {
					assert(choicenew > -1 && valuenew > -1 && succnew > -1);
					used.put(choicenew, new AtomicInteger(1));
					result.addBitv(game, from, choicenew, true);
					
					for (int i=0; i<Util.bitpower(game.varActionP2no()); i++)
						if (i != choicenew)
							result.addBitv(game, from, i, false);
					
					if (!flag.contains(succnew)) {
						queue.add(succnew);
						flag.add(succnew);
					}					
				}
				
			}
		}
				
		return result;
	}
	
	/**
	 * Plays according the given P2 strategy, checks if it is truly winning
	 * @param game			Play in this game
	 * @param gameinfo		Info about the game
	 * @param strategy		Strategy for player 2
	 * @return				Is the strategy winning?
	 */
	public static boolean checkBV(Game game, GameInfo gameinfo, Strategy strategy) {
		assert(game != null);
		
		TreeMap<Integer, TreeSet<Integer>> allowed = new TreeMap<Integer, TreeSet<Integer>>();
		HashMap<ArrayList<Boolean>,Integer> valuesIntoNames = new HashMap<ArrayList<Boolean>,Integer>();
		
		for (Pair<ArrayList<Boolean>,ArrayList<Boolean>> statebv : strategy.bitv.keySet())
			if (strategy.bitv.get(statebv)) { // only work with YES state-action pairs
				int from = -1;
				if (valuesIntoNames.containsKey(statebv.first()))
					from = valuesIntoNames.get(statebv.first());
				else {
					for (Integer key : game.states.keySet())
						if (game.states.get(key).values.equals(statebv.first()) && game.states.get(key).player == 2) {
							valuesIntoNames.put(statebv.first(), key);
							from = key;
							break;
						}
				}
				assert(from != -1);
				
				int action = Util.decimal(statebv.second());
				int into = game.successor(gameinfo, from, action, 1);
				
				if (!allowed.containsKey(from))
					allowed.put(from, new TreeSet<Integer>());
				allowed.get(from).add(into);
			}
		
		// we parsed the strategy, now we play by it
		
		LinkedList<Integer> queue = new LinkedList<Integer>();
		TreeSet<Integer> flag = new TreeSet<Integer>();
		queue.add(game.initialState);
		flag.add(game.initialState);
		
		TreeMap<Integer, TreeSet<Integer>> E = new TreeMap<Integer, TreeSet<Integer>>();
		
		while (queue.size() > 0) {
			Integer from = queue.remove();
			E.put(from, new TreeSet<Integer>());
			if (game.states.get(from).player == 1) { // the opponent is allowed to move anywhere
				for (Integer into : game.transitions.get(from)) {
					E.get(from).add(into);
					if (!allowed.containsKey(into)) return false;
					if (!flag.contains(into)) {
						queue.add(into);
						flag.add(into);
					}					
				}
			} else { // restricted by the strategy
				if (!allowed.containsKey(from)) return false;
				assert(!allowed.get(from).isEmpty());
				for (Integer into : allowed.get(from)) {
					E.get(from).add(into);
					if (!flag.contains(into)) {
						queue.add(into);
						flag.add(into);
					}
				}
			}
		}
		
		return checkBadSCC(game, E);
	}
	
	/**
	 * Plays according to the strategy represented by the decision tree
	 * @param game		Play in this game
	 * @param gameinfo	Info about the game
	 * @param tree		Play according to this decision tree
	 * @return			Is the strategy winning?
	 */
	public static boolean checkDT(Game game, GameInfo gameinfo, DecisionTree tree) {
		assert(game != null);
		
		LinkedList<Integer> queue = new LinkedList<Integer>();
		TreeSet<Integer> flag = new TreeSet<Integer>();
		queue.add(game.initialState);
		flag.add(game.initialState);
		
		TreeMap<Integer, TreeSet<Integer>> E = new TreeMap<Integer, TreeSet<Integer>>();
		
		while (queue.size() > 0) {
			Integer from = queue.remove();
			E.put(from, new TreeSet<Integer>());
			if (game.states.get(from).player == 1) { // the opponent is allowed to move anywhere
				for (Integer into : game.transitions.get(from)) {
					E.get(from).add(into);
					if (!flag.contains(into)) {
						queue.add(into);
						flag.add(into);
					}					
				}
			} else { // restricted by the strategy
				assert(E.get(from).isEmpty());
				boolean somethingallowed = false;
				for (int i=0; i<Util.bitpower(game.varActionP2no()); i++) {
					ArrayList<Boolean> sample = new ArrayList<Boolean>(game.varStateP2no()+game.varActionP2no());
					sample.addAll(game.states.get(from).values);
					sample.addAll(Util.binary(game.varActionP2no(), i));
					
					boolean classification = tree.classify(sample);
					if (classification) {
						somethingallowed = true;
						int into = game.successor(gameinfo, from, i, 1);
						assert(game.transitions.get(from).contains(into));
						E.get(from).add(into);
						if (!flag.contains(into)) {
							queue.add(into);
							flag.add(into);
						}						
					}
				}
				if (!somethingallowed)
					return false;
			}
		}
		
		return checkBadSCC(game, E);
	}
	
	/**
	 * Helper method to find if there are any bad sccs present
	 * @param game	Game we work with
	 * @param E		Find in this graph
	 * @return		Any witness scc that the strategy is losing?
	 */
	private static boolean checkBadSCC(Game game, TreeMap<Integer, TreeSet<Integer>> E) {
		// we have the graph created by restricting P2 by the strategy
		// in this graph, everything is reachable from the initial state
		
		byte badminparity = -2;
		
		while (true) {
			badminparity += 2;
			
			// remove all vertices with parity < badminparity
			TreeSet<Integer> delete = new TreeSet<Integer>();
			for (Integer key : E.keySet())
				if (game.states.get(key).parity < badminparity)
					delete.add(key);
			for (Integer key : delete)
				E.remove(key);
			for (Integer key : E.keySet())
				E.get(key).removeAll(delete);
			
			if (E.keySet().isEmpty()) return true;
			
			TreeMap<Integer, ArrayList<Integer>> sccs = Tarjan.recursive(E);
			
			for (Integer key : sccs.keySet()) {
				if (sccs.get(key).size() == 1 &&
					game.transitions.get( sccs.get(key).get(0) ).contains( sccs.get(key).get(0) ) &&
					game.states.get( sccs.get(key).get(0) ).parity % 2 == 0) {
					return false;
				}
						
				
				if (sccs.get(key).size() > 1)
					for (int i=0; i<sccs.get(key).size(); i++)
						if (game.states.get( sccs.get(key).get(i) ).parity == badminparity) {
							return false;
						}
							
			}
		}
	}
	
	/**
	 * Solves the game and gives the strategy to the main method <br>
	 * as long as the initial state is in the winning regions
	 * @param game	The entire game
	 * @return		Player 2 strategy
	 */
	private static TreeMap<Integer,TreeSet<Integer>> solve(Game game) {
		TreeMap<Integer,TreeSet<Integer>> allowed = new TreeMap<Integer,TreeSet<Integer>>();
		
		TreeSet<Integer> remaining = new TreeSet<Integer>(game.states.keySet());
		Boolean wrnonzero;
		Boolean winning = false;
		
		do {
			wrnonzero = false;
			TreeSet<Integer> onetwosubgame = findOneTwoSubgame(game, remaining);
			TreeSet<Integer> p2winningregion = solveOneTwoSubgame(game, onetwosubgame, allowed);
			
			if (p2winningregion.size() > 0) {
				wrnonzero = true;
				TreeSet<Integer> p2reacheswr = p2reachesWR(game, remaining, p2winningregion, allowed);
				if (p2winningregion.contains(game.initialState) || p2reacheswr.contains(game.initialState))
					winning = true;
				remaining.removeAll(p2winningregion);
				remaining.removeAll(p2reacheswr);
			}
		} while (wrnonzero && remaining.size() > 0);

		if (winning)
			return allowed;
		else
			return null;
	}
	
	/**
	 * Finds a 1/2 subgame within a subgame
	 * @param game		The entire game
	 * @param findhere	Find a 1/2 subgame in this subgame
	 * @return			State space of the 1/2 subgame
	 */
	private static TreeSet<Integer> findOneTwoSubgame(Game game, TreeSet<Integer> findhere) {
		
		TreeSet<Integer> targets = new TreeSet<Integer>();
		for (Integer key : findhere)
			if (game.states.get(key).parity == 0)
				targets.add(key);
		
		Ranks.subgame(game, findhere, targets, 1);
		TreeSet<Integer> result = new TreeSet<Integer>();
		for (Integer key : findhere)
			if (game.states.get(key).rank == -1)
				result.add(key);
		
		return result;
	}
	
	/**
	 * Solves a 1/2 subgame
	 * @param game		The entire game
	 * @param states	State space of the 1/2 subgame
	 * @param allowed	Update P2 strategy here
	 * @return			Player 2 winning region
	 */
	private static TreeSet<Integer> solveOneTwoSubgame(Game game, TreeSet<Integer> states, TreeMap<Integer,TreeSet<Integer>> allowed) {
		if (states.size() == 0) return new TreeSet<Integer>();
		
		TreeSet<Integer> remaining = new TreeSet<Integer>(states);
		Boolean wrnonzero;
		do {
			wrnonzero = false;
			TreeSet<Integer> targets = new TreeSet<Integer>();
			for (Integer key : remaining)
				if (game.states.get(key).parity == 1)
					targets.add(key);
			
			Ranks.subgame(game, remaining, targets, 2);
			
			TreeSet<Integer> p1winningregion = new TreeSet<Integer>();
			for (Integer key : remaining)
				if (game.states.get(key).rank == -1)
					p1winningregion.add(key);
			
			if (p1winningregion.size() > 0) {
				// From some states P2 can't ensure reaching parity1 state
				// Find P1 attractor to these states
				// Don't update the P2 strategy
				wrnonzero = true;
				TreeSet<Integer> p1reacheswr = p1reachesWR(game, remaining, p1winningregion);
				remaining.removeAll(p1winningregion);
				remaining.removeAll(p1reacheswr);
			} else if (remaining.size() > 0) {
				// From all states P2 can ensure reaching parity1 state
				// Add the behavior of P2 in this region into the strategy
				for (Integer from : remaining)
					if (game.states.get(from).player == 2) {
						assert(!allowed.containsKey(from));
						
						if (game.states.get(from).rank > 0) { // nonzero, noninfinity
							for (Integer into : game.transitions.get(from))
								if (remaining.contains(into) && game.states.get(into).rank != -1 &&
									game.states.get(into).rank < game.states.get(from).rank) {
									if (!allowed.containsKey(from))
										allowed.put(from, new TreeSet<Integer>());
									allowed.get(from).add(into);
								}
						}
						
						if (game.states.get(from).rank == 0) { // zero
							for (Integer into : game.transitions.get(from))
								if (remaining.contains(into) && game.states.get(into).rank != -1) {
									if (!allowed.containsKey(from))
										allowed.put(from, new TreeSet<Integer>());
									allowed.get(from).add(into);
								}					
						}
						
						assert(allowed.containsKey(from));
					}
			}
		} while (wrnonzero && remaining.size() > 0);
				
		return remaining;
	}
	
	/**
	 * Computes the set of states where player 2 can reach the WR
	 * @param game				The entire game
	 * @param subgame			State space of the subgame
	 * @param winningregion		Player 2 wants to reach this
	 * @param allowed			Update P2 strategy here
	 * @return					States where player 2 can reach the WR
	 */
	private static TreeSet<Integer> p2reachesWR(Game game, TreeSet<Integer> subgame,
			TreeSet<Integer> winningregion, TreeMap<Integer,TreeSet<Integer>> allowed) {
		Ranks.subgame(game, subgame, winningregion, 2);
		
		TreeSet<Integer> result = new TreeSet<Integer>();
		for (Integer from : subgame)
			if (game.states.get(from).rank > 0) { // nonzero noninfinity
				result.add(from);
				if (game.states.get(from).player == 2) {
					// update P2 strategy
					assert(!allowed.containsKey(from));
					
					for (Integer into : game.transitions.get(from))
						if (subgame.contains(into) && game.states.get(into).rank != -1 &&
							game.states.get(into).rank < game.states.get(from).rank) {
							if (!allowed.containsKey(from))
								allowed.put(from, new TreeSet<Integer>());
							allowed.get(from).add(into);
						}
					
					assert(allowed.containsKey(from));					
				}
			}
		return result;
	}
	
	/**
	 * Computes the set of states where player 1 can reach the WR
	 * @param game				The entire game
	 * @param subgame			State space of the subgame
	 * @param winningregion		Player 1 wants to reach this
	 * @return					States where player 1 can reach the WR
	 */
	private static TreeSet<Integer> p1reachesWR(Game game, TreeSet<Integer> subgame, TreeSet<Integer> winningregion) {
		Ranks.subgame(game, subgame, winningregion, 1);
		
		TreeSet<Integer> result = new TreeSet<Integer>();
		for (Integer key : subgame)
			if (game.states.get(key).rank > 0) // nonzero noninfinity
				result.add(key);
		return result;
	}
	
}
