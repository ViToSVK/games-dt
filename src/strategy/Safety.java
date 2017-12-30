package strategy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import machinelearning.DecisionTree;
import model.Game;
import model.GameInfo;
import util.Util;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Safety {

	/**
	 * Computes classical (one action per state) safety strategy for player 2
	 * @param game		Compute for this game
	 * @param gameinfo  Info about the game
	 * @return          The strategy
	 */
	public static Strategy classical(Game game, GameInfo gameinfo) {
		assert(game != null);
		Ranks.entire(game, 1);
		if (game.states.get(game.initialState).rank != -1)
			return null;

		Strategy result = new Strategy(2,'s');
		
        LinkedList<Integer> queue = new LinkedList<Integer>();
		TreeSet<Integer> flag = new TreeSet<Integer>();
		queue.add(game.initialState);
		flag.add(game.initialState);

		TreeMap<Integer,AtomicInteger> used = new TreeMap<Integer,AtomicInteger>();
		
		while (queue.size() > 0) {
			Integer from = queue.remove();
			if (game.states.get(from).player == 2) { // restricted by the strategy
				int choicefrom = -1;
				int valuefrom = -1;
				int succfrom = -1;
				int choicenew = -1;
				int valuenew = -1;
				int succnew = -1;

				for (int i=0; i<Util.bitpower(game.varActionP2no()); i++) {
					int into = game.successor(gameinfo, from, i, 1);
					if (game.states.get(into).rank == -1) {
						// We can consider this label
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
				
			} else { // the opponent is allowed to move anywhere
				for (Integer into : game.transitions.get(from))
					if (!flag.contains(into)) {
						queue.add(into);
						flag.add(into);
					}
			}
		}
		
		return result;
	}
	
	/**
	 * Computes permissive (multiple actions per state) safety strategy for player 2
	 * @param game		Compute for this game
	 * @param gameinfo  Info about the game
	 * @return          The strategy
	 */
	public static Strategy permissive(Game game, GameInfo gameinfo) {
		assert(game != null);
		Ranks.entire(game, 1);
		if (game.states.get(game.initialState).rank != -1)
			return null;

		Strategy result = new Strategy(2,'s');
		
        LinkedList<Integer> queue = new LinkedList<Integer>();
		TreeSet<Integer> flag = new TreeSet<Integer>();
		queue.add(game.initialState);
		flag.add(game.initialState);
		
		while (queue.size() > 0) {
			Integer from = queue.remove();
			if (game.states.get(from).player == 2) { // restricted by the strategy
				boolean something = false;
				for (int i=0; i<Util.bitpower(game.varActionP2no()); i++) {
					int into = game.successor(gameinfo, from, i, 1);
					if (game.states.get(into).rank == -1) {
						// allow this action
						something = true;
						result.addBitv(game, from, i, true);
						if (!flag.contains(into)) {
							queue.add(into);
							flag.add(into);
						}
					} else {
						// disallow this action
						result.addBitv(game, from, i, false);
					}
				}
				assert(something);
			} else { // the opponent is allowed to move anywhere
				for (Integer into : game.transitions.get(from))
					if (!flag.contains(into)) {
						queue.add(into);
						flag.add(into);
					}
			}
		}
		
		return result;
	}
	
	/**
	 * Reads out an ARFF file and plays according to the strategy written in there
	 * @param game     	Play in this game
	 * @param gameinfo	Info about the game
	 * @param filename 	Read this ARFF file
	 * @return			Is the strategy winning?
	 */
	public static boolean check(Game game, GameInfo gameinfo, String filename) {
		assert(game != null);
		
		try (Scanner sc = new Scanner(new File("results/datasets/"+filename+".arff"))) {
			String token;
			int test;
			
			boolean now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("GENERATED:")) now = true;
			}
			test = sc.nextInt();
			assert(test == game.stateSize);
			
			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("STATEVARP1:")) now = true;
			}
			test = sc.nextInt();
			assert(test == game.varStateP1no());
			
			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("STATEVARP2:")) now = true;
			}
			test = sc.nextInt();
			assert(test == game.varStateP2no());			
			
			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("ACTIONVARP1:")) now = true;
			}
			test = sc.nextInt();
			assert(test == game.varActionP1no());
			
			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("ACTIONVARP2:")) now = true;
			}
			test = sc.nextInt();
			assert(test == game.varActionP2no());			
			
			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("PLAYER:")) now = true;
			}
			test = sc.nextInt();
			assert(test == 2);
			
			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("OBJECTIVE:")) now = true;
			}
			token = sc.next();
			assert(token.equals("s"));
			
			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("NUMTOT:")) now = true;
			}
			int samples = sc.nextInt();
			
			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("@RELATION")) now = true;
			}
			token = sc.next();
			assert(token.equals("strategy"));
			
			for (int i=0; i<game.varStateP2no()+game.varActionP2no(); i++) {
				token = sc.next();
				assert(token.equals("@ATTRIBUTE"));
				sc.next(); token = sc.next();
				assert(token.equals("{0,1}"));
			}
			
			token = sc.next();
			assert(token.equals("@ATTRIBUTE"));
			token = sc.next();
			assert(token.equals("\"class\""));
			token = sc.next();
			assert(token.equals("{yes,no}"));
			token = sc.next();
			assert(token.equals("@DATA"));
			
			// now begins the strategy
			
			TreeMap<Integer, TreeSet<Integer>> str = new TreeMap<Integer, TreeSet<Integer>>();
			HashMap<ArrayList<Boolean>,Integer> valuesIntoNames = new HashMap<ArrayList<Boolean>,Integer>();
			
			for (int i=0; i<samples; i++) {
				token = sc.next();
				ArrayList<Boolean> newb = new ArrayList<Boolean>(game.varStateP2no());
				for (int j=0; j<game.varStateP2no(); j++) {
					assert(token.charAt(2*j) == '0' || token.charAt(2*j) == '1');
					newb.add(token.charAt(2*j) == '1');
				}
				
				int from = -1;
				if (valuesIntoNames.containsKey(newb))
					from = valuesIntoNames.get(newb);
				else {
					for (Integer key : game.states.keySet())
						if (game.states.get(key).values.equals(newb) && game.states.get(key).player == 2) {
							valuesIntoNames.put(newb, key);
							from = key;
							break;
						}
				}
				
				assert(from != -1);
				
				newb = new ArrayList<Boolean>(game.varActionP2no());
				for (int j=0; j<game.varActionP2no(); j++)
					newb.add(token.charAt(2*game.varStateP2no()+2*j) == '1');
				
				if (token.charAt(2*game.varStateP2no()+2*game.varActionP2no()) == 'y') {
					int label = Util.decimal(newb);
					int into = game.successor(gameinfo, from, label, 1);
					if (!str.containsKey(from))
						str.put(from, new TreeSet<Integer>());
					str.get(from).add(into);
				} else
					assert(token.charAt(2*game.varStateP2no()+2*game.varActionP2no()) == 'n');
			}
			
			// we parsed the strategy, now we play by it
			
	        LinkedList<Integer> queue = new LinkedList<Integer>();
			TreeSet<Integer> flag = new TreeSet<Integer>();
			queue.add(game.initialState);
			flag.add(game.initialState);
			
			while (queue.size() > 0) {
				Integer from = queue.remove();
				if (game.states.get(from).player == 2) { // restricted by the strategy
					if (str.containsKey(from)) {
						assert(!str.get(from).isEmpty());
						for (Integer into : str.get(from)) {
							if (game.states.get(into).target) return false;
							if (!flag.contains(into)) {
								queue.add(into);
								flag.add(into);
							}
						}
					} else { // everything forbidden means the same as everything allowed
						for (Integer into : game.transitions.get(from)) {
							if (game.states.get(into).target) return false;
							if (!flag.contains(into)) {
								queue.add(into);
								flag.add(into);
							}							
						}
					}
				} else { // the opponent is allowed to move anywhere
					for (Integer into : game.transitions.get(from)) {
						if (game.states.get(into).target) return false;
						if (!flag.contains(into)) {
							queue.add(into);
							flag.add(into);
						}							
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Plays according to the strategy represented by the decision tree
	 * @param game		Play in this game
	 * @param gameinfo	Info about the game
	 * @param tree		Play according to this decision tree
	 * @return			Is the strategy winning?
	 */
	public static boolean checkDT(Game game, GameInfo gameinfo, DecisionTree tree) {
		
        LinkedList<Integer> queue = new LinkedList<Integer>();
		TreeSet<Integer> flag = new TreeSet<Integer>();
		queue.add(game.initialState);
		flag.add(game.initialState);
		
		while (queue.size() > 0) {
			Integer from = queue.remove();
			if (game.states.get(from).player == 2) { // restricted by the strategy
				
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
						if (game.states.get(into).target) return false;
						if (!flag.contains(into)) {
							queue.add(into);
							flag.add(into);
						}						
					}
				}
				
				if (!somethingallowed)
					for (Integer into : game.transitions.get(from)) {
						if (game.states.get(into).target) return false;
						if (!flag.contains(into)) {
							queue.add(into);
							flag.add(into);
						}							
					}
				
			} else { // the opponent is allowed to move anywhere
				for (Integer into : game.transitions.get(from)) {
					if (game.states.get(into).target) return false;
					if (!flag.contains(into)) {
						queue.add(into);
						flag.add(into);
					}							
				}				
			}
		}
		
		return true;
	}
	
}
