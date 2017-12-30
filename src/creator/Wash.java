package creator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import model.Game;
import model.GameInfo;
import strategy.Ranks;
import util.Pair;
import util.Triplet;
import util.Util;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Wash {
	
	/* Wash Game Info:
	 * P1 is environment, plays reachability (first 'lost' bit is 1)
	 * P2 is controller, plays safety (first 'lost' bit stays 0)
	 * P1 states bitvector:
	 * - one 'lost' bit
	 * - for every tank, one 'req' bit, then d+1 bits specifying delay (0,1,..,d)
	 * - for every tank, one 'full' bit, then k bits specifying delay till emptying (0,1,..,k-1)
	 * P2 states: same as P1 bitvector
	 * P1 actions: push. During transitions only fill request are created
	 * P2 actions: fill, empty, lightmode. Here everything is checked plus time is advanced
	*/
	
	private static LinkedList<Integer> queue;
	private static LinkedList<Integer> queuenew;
	private static int distance;
	private static HashMap<ArrayList<Boolean>,Integer> valuesIntoNameP1;
	private static HashMap<ArrayList<Boolean>,Integer> valuesIntoNameP2;	
	
	/**
	 * Computes successor state given a state-action pair
	 * @param game		Compute in this game
	 * @param gameinfo	Info about the game
	 * @param state		Move from this state
	 * @param label		Move using this action
	 * @return			Values of successor state
	 */
	public static ArrayList<Boolean> successor(Game game, GameInfo gameinfo, int state, int label) {
		assert(gameinfo != null);
		assert(game.states.containsKey(state));
		int n = gameinfo.n; assert(n >= 2 && n <= 6);
		int d = gameinfo.d; assert(d >= 1 && d <= 9);
		int k = gameinfo.k; assert(k >= 1 && k <= d && k <= 4);
		int t = gameinfo.t; assert(t >= 1 && t <= n && t <= 4);
		boolean lightmode = gameinfo.lightmode;
		
		if (game.states.get(state).player == 1) {
			// Player1 state
			assert(label >= 0 && label < Util.bitpower(game.varActionP1no()));
			int help = label;
			boolean[] push = new boolean[n];
			for (int i=0; i<n; i++) {
				push[i] = (help % 2 == 1);
				help /= 2;
			}
			
			ArrayList<Boolean> result = new ArrayList<Boolean>(game.varStateP2no());
			result.addAll(game.states.get(state).values);
			for (int i=0; i<n; i++) {
				if (push[i] && game.states.get(state).values.get( reqbit(gameinfo,i,-1) ) == false) {
					result.set( reqbit(gameinfo,i,-1), true);
					for (int j=0; j<=d; j++)
						assert(result.get( reqbit(gameinfo,i,j) ) == false);
					result.set( reqbit(gameinfo,i,d), true);
				}
			}
			return result;
			
		} else {
			// Player2 state
			assert(label >= 0 && label < Util.bitpower(game.varActionP2no()));
			int help = label;
			boolean[] fill = new boolean[n];
			for (int i=0; i<n; i++) {
				fill[i] = (help % 2 == 1);
				help /= 2;
			}
			boolean[] empty = new boolean[n];
			for (int i=0; i<n; i++) {
				empty[i] = (help % 2 == 1);
				help /= 2;
			}
			boolean light = (help % 2 == 1);
			
			// First check if this action takes you to target
			boolean wrong = false;
			ArrayList<Boolean> cv = game.states.get(state).values;

			for (int i=0; i<n; i++) {
				if (cv.get(reqbit(gameinfo,i,-1)) && !fill[i]) {
					Boolean timeiszero = cv.get(reqbit(gameinfo,i,0));
					//Boolean timeiszero = true;
					//for (int j=0; j<=d; j++)
					//	if (cv.get(reqbit(gameinfo,i,j)))
					//		timeiszero = false;
					if (timeiszero) {
						wrong = true;
						break;
					}
				}
				
				if (!cv.get(reqbit(gameinfo,i,-1)) && fill[i]) {
					wrong = true;
					break;
				}
				if (cv.get(fullbit(gameinfo,i,-1)) && fill[i] && !empty[i]) { // allowed to empty and fill in the same step
					wrong = true;
					break;
				}
				if (cv.get(fullbit(gameinfo,i,-1))) {
					Boolean timeiszero = cv.get(fullbit(gameinfo,i,0));
					//Boolean timeiszero = true;
					//for (int j=0; j<k; j++)
					//	if (cv.get(fullbit(gameinfo,i,j)))
					//		timeiszero = false;
					if (timeiszero && !empty[i]) {
						wrong = true;
						break;
					}
					if (!timeiszero && empty[i]) {
						wrong = true;
						break;
					}
				}
				if (fill[i] && empty[i]) {
					wrong = true;
					break;
				}
			}
			
			if (!wrong) {
				for (int j=0; j<(n/t)+1; j++) {
					int numberoffills = 0;
					for (int l=0; l<t; l++)
						if (j*t+l < n) if (fill[j*t+l]) numberoffills++;
					if (numberoffills > 1) {
						wrong = true;
						break;
					}
				}
			}
			
			if (!wrong && !lightmode) {
				// light on when some tank is filled
				Boolean existsfill = false;
				for (int i=0; i<n; i++)
					if (fill[i]) existsfill = true;
				if (light && !existsfill) wrong = true;
				if (!light && existsfill) wrong = true;
			}
			
			if (!wrong && lightmode) {
				// light on when some tank contains water
				Boolean existswithwater = false;
				for (int i=0; i<n; i++) {
					if (cv.get(fullbit(gameinfo,i,-1)) && !empty[i]) existswithwater = true;
					if (fill[i]) existswithwater = true;
				}
				if (light && !existswithwater) wrong = true;
				if (!light && existswithwater) wrong = true;
			}
			
			if (wrong) {
				ArrayList<Boolean> result = new ArrayList<Boolean>(game.varStateP1no());
				result.add(true); for (int i=1; i<game.varStateP1no(); i++) result.add(false); // 'lost' state
				return result;
			}
			
			// This action doesn't take you to the target
			
			ArrayList<Boolean> result = new ArrayList<Boolean>(game.varStateP1no());
			result.add(false);
			
			for (int i=0; i<n; i++) {
				if (fill[i]) {
					result.add(false);
					for (int j=0; j<=d; j++)
						result.add(false);
				} else {
					result.add(cv.get(reqbit(gameinfo,i,-1)));
					int newtime = -1;
					for (int j=0; j<=d; j++)
						if (cv.get(reqbit(gameinfo,i,j)))
							newtime = j;
					// it could still be -1, that's ok
					if (newtime > -1) newtime -= 1;
					for (int j=0; j<=d; j++)
						result.add(j == newtime);
				}
			}
			
			for (int i=0; i<n; i++) {
				if (fill[i]) {
					result.add(true);
					for (int j=0; j<=k-2; j++)
						result.add(false);
					result.add(true);
				} else if (empty[i]) {
					result.add(false);
					for (int j=0; j<=k-1; j++)
						result.add(false);
				} else {
					result.add(cv.get(fullbit(gameinfo,i,-1)));
					int newtime = -1;
					for (int j=0; j<=k-1; j++)
						if (cv.get(fullbit(gameinfo,i,j)))
							newtime = j;
					// it could still be -1, that's ok
					if (newtime > -1) newtime -= 1;
					for (int j=0; j<=k-1; j++)
						result.add(j == newtime);
				}
			}
			
			assert(result.size() == game.varStateP1no());
			return result;
		}
	}
	
	/**
	 * Returns tank fill request bit or request delay bit
	 * @param gameinfo	Info about the game
	 * @param tank		Which tank is in question here
	 * @param delay		-1 for request bit, {0,..,d} for delay bits
	 * @return			Position in values vector
	 */
	private static int reqbit(GameInfo gameinfo, int tank, int delay) {
		assert(tank >= 0 && tank <= gameinfo.n);
		assert(delay >= -1 && delay <= gameinfo.d); // -1 for 'request' bit
		return 1 + tank*(1+gameinfo.d+1) + (1+delay);
	}

	/**
	 * Returns tank fullness status bit or empty delay bit
	 * @param gameinfo	Info about the game
	 * @param tank		Which tank is in question here
	 * @param delay		-1 for status bit, {0,..,k-1} for delay bits
	 * @return			Position in values vector
	 */	
	private static int fullbit(GameInfo gameinfo, int tank, int delay) {
		assert(tank >= 0 && tank <= gameinfo.n);
		assert(delay >= -1 && delay < gameinfo.k); // -1 for 'full' bit
		return 1 + gameinfo.n*(1+gameinfo.d+1) + tank*(1+gameinfo.k) + (1+delay);
	}
	
	/**
	 * Main game creation method, used by other methods that tune the parameters
	 * @param game				Can reuse previously created game to speed up creation
	 * @param gameinfo			Info about the game
	 * @param distancelimit		State this far away from initial state become targets / selfloopnontargets
	 * @param safety			True - targets, False - selfloopnontargets
	 * @param storetransitions	False when you just want to estimate state space size
	 * @return					(Game, Small enough to work with?)
	 */
	private static Pair<Game, Boolean> create(Game game, GameInfo gameinfo, int distancelimit, boolean storetransitions) {
		assert(gameinfo != null);
		int n = gameinfo.n; assert(n >= 2 && n <= 6);
		int d = gameinfo.d; assert(d >= 1 && d <= 9);
		int k = gameinfo.k; assert(k >= 1 && k <= d && k <= 4);
		int t = gameinfo.t; assert(t >= 1 && t <= n && t <= 4);
		//boolean lightmode = gameinfo.lightmode;
		
		boolean fromscratch = (game == null);
		if (fromscratch) {
			game = new Game();
			
			queue = new LinkedList<Integer>();
			queuenew = new LinkedList<Integer>();
			distance = 0;
			valuesIntoNameP1 = new HashMap<ArrayList<Boolean>,Integer>();
			valuesIntoNameP2 = new HashMap<ArrayList<Boolean>,Integer>();
			
			// P1 states --- lost, tankreq bits, tankfill bits
			game.varStateP1 = new ArrayList<String>( 1 + n * ( 1 + d+1 ) + n * (1 + k) );
			game.varStateP1.add("lost");
			for (int i=0; i<n; i++) {
				game.varStateP1.add("t"+i+"req");
				for (int j=0; j<=d; j++) // (0,1,..,d)
					game.varStateP1.add("t"+i+"reqd"+j);
			}
			for (int i=0; i<n; i++) {
				game.varStateP1.add("t"+i+"full");
				for (int j=0; j<k; j++) // (0,1,..,k-1)
					game.varStateP1.add("t"+i+"fulld"+j);
			}
			assert(game.varStateP1.size() == 1+n*(d+2)+n*(k+1));
			
			// P2 states --- lost, tankreq bits, tankfill bits
			game.varStateP2 = new ArrayList<String>( 1 + n * ( 1 + d+1 ) + n * (1 + k) );
			game.varStateP2.add("lost");
			for (int i=0; i<n; i++) {
				game.varStateP2.add("t"+i+"req");
				for (int j=0; j<=d; j++) // (0,1,..,d)
					game.varStateP2.add("t"+i+"reqd"+j);
			}
			for (int i=0; i<n; i++) {
				game.varStateP2.add("t"+i+"full");
				for (int j=0; j<k; j++) // (0,1,..,k-1)
					game.varStateP2.add("t"+i+"fulld"+j);
			}
			assert(game.varStateP2.size() == 1+n*(d+2)+n*(k+1));
			
			// P1 actions --- Apush0..n-1
			game.varActionP1 = new ArrayList<String>( n );
			for (int i=0; i<n; i++)
				game.varActionP1.add("Apush"+i);
			
			// P2 actions --- Afill0..n-1, Aempty0..n-1, Alight
			game.varActionP2 = new ArrayList<String>( n + n + 1 );
			for (int i=0; i<n; i++)
				game.varActionP2.add("Afill"+i);
			for (int i=0; i<n; i++)
				game.varActionP2.add("Aempty"+i);
			game.varActionP2.add("Alight");

			ArrayList<Boolean> newValues = new ArrayList<Boolean>(game.varStateP1no());
			for (int i=0; i<game.varStateP1no(); i++) newValues.add(false);
			game.addState(1, 1, newValues);
			game.initialState = 1; // Initial state
			valuesIntoNameP1.put(newValues, 1);
			
			newValues = new ArrayList<Boolean>(game.varStateP1no());
			newValues.add(true); for (int i=1; i<game.varStateP1no(); i++) newValues.add(false);
			game.addState(2, 1, newValues); 
			game.states.get(2).target = true; // 'Lost' state
			valuesIntoNameP1.put(newValues, 2);
			game.addTransition(2, 2);
			
			queue.add(game.initialState);
						
		} else {
			// check that game is in the state we expect it to be
			System.out.print("REUSE ");
			assert(game != null);
			assert(!queue.isEmpty());
			assert(queuenew.isEmpty());
			assert(distance > 0);
			assert(!valuesIntoNameP1.isEmpty());
			assert(!valuesIntoNameP2.isEmpty());
			assert(game.stateSize > 0);
			assert(game.transitionSize > 0);
			assert(game.varStateP1.size() == 1 + n * ( 1 + d+1 ) + n * (1 + k) );	// lost, tankreq bits, tankfill bits
			assert(game.varStateP2.size() == 1 + n * ( 1 + d+1 ) + n * (1 + k) );	// lost, tankreq bits, tankfill bits
			assert(game.varActionP1.size() == n );									// Apush0..n-1
			assert(game.varActionP2.size() == n + n + 1 );							// Afill0..n-1, Aempty0..n-1, Alight
			
			// remove 'fake' target status from states on the previous distance limit
			for (Integer key : queue) {
				if (!game.states.get(key).values.get(0)) // first bit set to true if you lost
					game.states.get(key).target = false;
				// and remove selfloops
				game.transitions.remove(key,key);
			}
		}
		
		assert((fromscratch && queue.size() == 1) || (!fromscratch));
		
		// generate state space until you reach the distance limit
		while (distance < distancelimit && !queue.isEmpty()) {
			distance++;
			while (!queue.isEmpty()) {
				Integer current = queue.remove();
				if (game.states.get(current).player == 1) {
					// Player1 state
					for (int i=0; i<Util.bitpower(game.varActionP1no()); i++) {
						ArrayList<Boolean> newValues = successor(game, gameinfo, current, i);
						
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
						
						// state space above 30k, too big to solve
						if (storetransitions && game.stateSize > 30000) // 30k or 10k when there is too many actions
							return new Pair<Game,Boolean>(game, false);
					}
				} else {
					// Player2 state
					for (int i=0; i<Util.bitpower(game.varActionP2no()); i++) {
						ArrayList<Boolean> newValues = successor(game, gameinfo, current, i);
						
						Integer newName = valuesIntoNameP1.get(newValues);
						if (newName == null) {
							newName = game.stateSize + 1;
							game.addState(newName, 1, newValues);
							valuesIntoNameP1.put(game.states.get(newName).values, newName);
							queuenew.add(newName);
						}
						
						if (storetransitions)
							game.addTransition(current, newName);
						
						// state space estimate is above million
						if (!storetransitions && game.stateSize > 1000000)
							return new Pair<Game,Boolean>(game, false);
						
						// state space above 30k, too big to solve
						if (storetransitions && game.stateSize > 30000) // 30k or 10k when there is too many actions
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
		
		if (!queue.isEmpty())
			for (Integer key : queue) {
				// make all states at the distance limit targets
				game.states.get(key).target = true;
				// and create selfloops (in both cases)
				game.addTransition(key, key);
			}
		
		return new Pair<Game, Boolean>(game, true);
	}
	
	/**
	 * Used for creating a game restricted by a distance limit from the initial state
	 * @param game				Can reuse previously created game to speed up creation
	 * @param gameinfo			Info about the game
	 * @param distancelimit		State this far away from initial state become targets / selfloopnontargets
	 * @return 					(Game, Small enough to work with?)
	 */
	public static Pair<Game, Boolean> create(Game game, GameInfo gameinfo, int distancelimit) {
		assert(distancelimit > 0);
		return create(game, gameinfo, distancelimit, true);
	}
	
	/**
	 * Used for estimating state space size
	 * @param gameinfo			Info about the game
	 * @param storetransitions	Has to be false
	 * @return					State space size or 1mil+1 if it's above 1mil
	 */
	public static int create(GameInfo gameinfo, Boolean storetransitions) {
		assert(!storetransitions);
		Pair<Game, Boolean> result = create(null, gameinfo, Integer.MAX_VALUE, false);
		if (result == null) return 1000001;
		return result.first().stateSize;
	}
	
	/**
	 * Create a game modelling the washing cycle scheduling problem
	 * @param gameinfo			Parameters of the problem (n,d,k,t,lightmode)
	 * @param writer			Here write the report
	 * @return					(Game, Distance restriction, Safety/Reachability)
	 * @throws IOException 		Propagate this to the method that gave you the writer handle
	 */
	public static Triplet<Game,Integer,Boolean> create(GameInfo gameinfo, BufferedWriter writer) throws IOException {
		int lowerbound = 0;   // this size is not enough
		int upperboundTT = Integer.MAX_VALUE; // this size is enough
		int upperboundFF = Integer.MAX_VALUE; // gave up, state space too big
		int safeorreach = -1; // 0 for safety, 1 for reachability
		
		int testsize = 4;
		boolean stop = false;
		Game game = null;
		String nl = System.getProperty("line.separator");
		
		while (!stop) {
			System.out.print("Distance limit = "+testsize+"... ");
			writer.write("Distance limit = "+testsize+"... ");
			Pair<Game, Boolean> result = create(game, gameinfo, testsize);
			
			if (!result.second()) { // state space too big
				stop = true;
				upperboundFF = testsize;
				System.out.println("state space too big.");
				writer.write("state space too big."+nl);
				break;
			}

			game = result.first();
			// try safety
			Ranks.entire(game, 1);
			
			if (game.states.get(game.initialState).rank == -1) {
				// Winning for P2 even with the distance constraint
				stop = true;
				upperboundTT = testsize;
				safeorreach = 0;
				System.out.println("winning for Player 2!");
				writer.write("winning for Player 2!"+nl);
			} else {
				// remove 'fake' target status from states on the previous distance limit
				for (Integer key : queue)
					if (!game.states.get(key).values.get(0)) // first bit set to true if you lost
						game.states.get(key).target = false;
				// but keep the self-loops
				// try reachability
				Ranks.entire(game, 1);
				if (game.states.get(game.initialState).rank != -1) {
					// Winning for P1 even with the distance constraint
					if (game.states.get(game.initialState).rank == 0) {
						System.out.println("INITIAL STATE IS TARGET!");
						writer.write("INITIAL STATE IS TARGET!"+nl);
						return new Triplet<Game, Integer, Boolean>(null, -1, false);
					}
					stop = true;
					upperboundTT = testsize;
					safeorreach = 1;
					System.out.println("winning for Player 1!");
					writer.write("winning for Player 1!"+nl);
				} else {
					lowerbound = testsize;
					System.out.println("distance too small.");
					writer.write("distance too small."+nl);
					testsize *= 2;
				}
			}
		}
		
		// got some upper bound, now do binary search for 'optimal' distance
		
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
			writer.write("Distance limit = "+testsize+"... ");
			Pair<Game, Boolean> result = create(game, gameinfo, testsize);
			
			if (!result.second()) { // state space too big
				assert(upperboundTT == Integer.MAX_VALUE);
				upperboundFF = testsize;
				System.out.println("state space too big.");
				writer.write("state space too big."+nl);
				
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
			boolean oneworked = false;
			
			
			if (safeorreach != 1) {
				// try safety
				Ranks.entire(game, 1);
				
				if (game.states.get(game.initialState).rank == -1) {
					// Winning for P2 even with the distance constraint
					upperboundTT = testsize;
					safeorreach = 0;
					oneworked = true;
					System.out.println("winning for Player 2!");
					writer.write("winning for Player 2!"+nl);
					
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
				}		
			}
			
			if (safeorreach != 0) {
				// try reachability
				// remove 'fake' target status from states on the previous distance limit
				for (Integer key : queue)
					if (!game.states.get(key).values.get(0)) // first bit set to true if you lost
						game.states.get(key).target = false;
				// but keep the self-loops
				Ranks.entire(game, 1);
				
				if (game.states.get(game.initialState).rank != -1) {
					// Winning for P1 even with the distance constraint
					if (game.states.get(game.initialState).rank == 0) {
						System.out.println("INITIAL STATE IS TARGET!");
						writer.write("INITIAL STATE IS TARGET!"+nl);
						return new Triplet<Game, Integer, Boolean>(null, -1, false);
					}
					upperboundTT = testsize;
					safeorreach = 1;
					oneworked = true;
					System.out.println("winning for Player 1!");
					writer.write("winning for Player 1!"+nl);
					
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
					
				}
			}
			
			if (!oneworked) {
				lowerbound = testsize;
				System.out.println("distance too small.");
				writer.write("distance too small."+nl);
				
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
			System.out.println("FAIL: Didn't find any winning strategy.\n");
			writer.write("FAIL: Didn't find any winning strategy."+nl+nl);
			return new Triplet<Game, Integer, Boolean>(null, -1, false);
		}
		
		assert(safeorreach != -1);
		System.out.println("SUCCESS: The smallest winning strategy is "+
				(safeorreach==0?"safety":"reachability")+" for distance "+upperboundTT+".");
		writer.write("SUCCESS: The smallest winning strategy is "+
				(safeorreach==0?"safety":"reachability")+" for distance "+upperboundTT+"."+nl);
		Pair<Game, Boolean> result = create(game, gameinfo, upperboundTT);
		assert(result.second());
		
		game = result.first();
		if (safeorreach == 1) {
			// remove 'fake' target status from states on the previous distance limit
			for (Integer key : queue)
				if (!game.states.get(key).values.get(0)) // first bit set to true if you lost
					game.states.get(key).target = false;
			// but keep the self-loops
			Ranks.entire(game, 1);
			assert(game.states.get(game.initialState).rank != -1);
		}
		
		
		return new Triplet<Game, Integer, Boolean>(game, upperboundTT, safeorreach==1);
	}
	
}
