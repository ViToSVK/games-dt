package model;

import java.util.ArrayList;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class State {
	
	//public int name;
	public byte player;
	public ArrayList<Boolean> values;
	
	public boolean target;
	public byte parity;
	
	public int rank;
	
	public State(byte player, ArrayList<Boolean> values) {
		this.player = (player<=1)?(byte)1:(byte)2;
		this.values = new ArrayList<Boolean>(values);
		this.target = false;
		this.rank = -1;
		this.parity = 0;
	}
	
	public State(byte player, ArrayList<Boolean> values, byte parity) {
		this.player = (player<=1)?(byte)1:(byte)2;
		this.values = new ArrayList<Boolean>(values);
		this.target = false;
		this.parity = parity;
		this.rank = -1;
	}

}
