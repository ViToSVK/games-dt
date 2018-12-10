package model;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class GameInfo {
	
	public char type; // 'a'iger 'w'ash 'r'abinizer
	
	public String filename;
	
	public int n;
	public int d;
	public int k;
	public int t;
	public boolean lightmode;
	
	public String write() {
		assert(type == 'w');
		return ("wash_"+n+"_"+d+"_"+k+"_"+t+"_"+(lightmode?"t":"f"));
	}
	
	public void fill(int n, int d, int k, int t, boolean lightmode) {
		this.n = n;
		this.d = d;
		this.k = k;
		this.t = t;
		this.lightmode = lightmode;
	}
}
