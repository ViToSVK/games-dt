package model;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import creator.Aiger;
import creator.Rabinizer;
import creator.Wash;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Game {
	
	public ArrayList<String> varStateP1, varStateP2, varActionP1, varActionP2;
	public int varStateP1no() { return varStateP1.size(); }
	public int varStateP2no() { return varStateP2.size(); }
	public int varActionP1no() { return varActionP1.size(); }
	public int varActionP2no() { return varActionP2.size(); }
	
	public int initialState;
	public TreeMap<Integer, State> states;
	public TreeMap<Integer, TreeSet<Integer>> transitions;
	
	public int stateSize;
	public long transitionSize;
	
	public Game() {
		varStateP1 = new ArrayList<String>();
		varStateP2 = new ArrayList<String>();
		varActionP1 = new ArrayList<String>();
		varActionP2 = new ArrayList<String>();
		initialState = 0;
		states = new TreeMap<Integer,State>();
		transitions = new TreeMap<Integer, TreeSet<Integer>>();
		stateSize = 0;
		transitionSize = 0;
	}
	
	public boolean addState(int name, int player, ArrayList<Boolean> values) {
		State old = states.put(name, new State((byte) player, values));
		if (old==null) stateSize++;
		return old==null;
	}
	
	public boolean addState(int name, int player, ArrayList<Boolean> values, int parity) {
		State old = states.put(name, new State((byte) player, values, (byte) parity));
		if (old==null) stateSize++;
		return old==null;
	}
	
	public boolean addTransition(int from, int into) {
		if (!transitions.containsKey(from))
			transitions.put(from, new TreeSet<Integer>());
		boolean result = transitions.get(from).add(into);
		if (result) transitionSize++;
		return result;
	}
	
	public boolean removeTransition(int from, int into) {
		if (!transitions.containsKey(from))
			return false;
		boolean result = transitions.get(from).remove(into);
		if (result) transitionSize--;
		if (transitions.get(from).size() == 0)
			transitions.remove(from);
		return result;
	}
	
	/**
	 * Returns the successor for a given state-action pair
	 * @param gameinfo		Info about the game
	 * @param state			State label
	 * @param label			Action label
	 * @param succowner		Which player should own the successor
	 * @return				Successor state label
	 */
	public int successor(GameInfo gameinfo, int state, int label, int succowner) {
		assert(gameinfo.type == 'a' || gameinfo.type == 'w' || gameinfo.type == 'r');
		assert(succowner == 1 || succowner == 2);
		
		if (gameinfo.type == 'a') {
			ArrayList<Boolean> result = Aiger.successor(this, state, label);
			for (Integer key: states.keySet()) {
				if (states.get(key).values.equals(result) && states.get(key).player == succowner)
					return key;
			}
			assert(false);
		}
		
		if (gameinfo.type == 'w') {
			ArrayList<Boolean> result = Wash.successor(this, gameinfo, state, label);
			for (Integer key: states.keySet()) {
				if (states.get(key).values.equals(result) && states.get(key).player == succowner)
					return key;
			}
			assert(false);
		}
		
		if (gameinfo.type == 'r') {
			int key = Rabinizer.successor(this, gameinfo, state, label);
			assert(this.states.get(key).player == succowner);
			return key;
		}
		
		assert(false);
		return 0;
	}
	
}
