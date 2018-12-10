package strategy;

import java.util.ArrayList;
import java.util.HashMap;

import model.Game;
import util.Pair;
import util.Util;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Strategy {
	
	public final byte player;
	public final char objective; // 's'afety 'r'eachability 'p'arity
	
	public HashMap<Pair<ArrayList<Boolean>,ArrayList<Boolean>>,Boolean> bitv;
	public int bitvYES;
	public int bitvNO;
	
	public Strategy(int player, char objective) {
		assert(player == 1 || player == 2);
		assert(objective == 's' || objective == 'r' || objective == 'p');
		
		this.player = (byte) player;
		this.objective = objective;
		
		bitv = new HashMap<Pair<ArrayList<Boolean>,ArrayList<Boolean>>,Boolean>();
		bitvYES = 0;
		bitvNO = 0;
	}
	
	/**
	 * Clears the bitvector map
	 */
	public void clearBitv() {
		bitv.clear();
		bitvYES = 0;
		bitvNO = 0;
	}
	
	/**
	 * Adds a state-action pair into the bitvector map
	 * @param game		Game we work with
	 * @param state		State
	 * @param label		Action
	 * @param allowed	Allowed/Disallowed
	 */
	public void addBitv(Game game, int state, int label, boolean allowed) {
		ArrayList<Boolean> newS = new ArrayList<Boolean>(game.states.get(state).values);
		ArrayList<Boolean> newA = Util.binary(
				(player==1)?game.varActionP1.size():game.varActionP2.size(), label);
		if (allowed) {
			bitvYES++;
			bitv.put(new Pair<ArrayList<Boolean>,ArrayList<Boolean>>(newS, newA), true);
		} else {
			bitvNO++;
			bitv.put(new Pair<ArrayList<Boolean>,ArrayList<Boolean>>(newS, newA), false);
		}
	}
}
