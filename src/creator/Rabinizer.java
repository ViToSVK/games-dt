package creator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

import model.Game;
import model.GameInfo;
import util.Pair;
import util.Util;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Rabinizer {
	
	private static TreeMap<Integer, TreeMap<Byte, Integer>> labeledTransitions;
	
	/**
	 * Returns successor state given a state-action pair
	 * @param game		The entire game
	 * @param gameinfo	Info about the game
	 * @param state		State
	 * @param label		Action
	 * @return			Successor
	 */
	public static int successor(Game game, GameInfo gameinfo, int state, int label) {
		assert(gameinfo != null);
		assert(gameinfo.type == 'r');
		assert(game.states.containsKey(state));
		
		return labeledTransitions.get(state).get((byte) label);
	}
	
	/**
	 * Finds out the number of atomic propositions for a given parity automaton
	 * @param filename 	Name of the parity automaton file (filename.hoa)
	 * @return			Number of atomic propositions
	 */
	public static int apnumber(String filename) {
		String token;
		try (Scanner sc = new Scanner(new File("benchmarks/rabinizer/"+filename+".hoa"))) {
			token = sc.next();
			while (!token.equals("AP:"))
				token = sc.next();
			return sc.nextInt();
		} catch (Exception e) {
    		//e.printStackTrace();
    		return -1;
		}
	}
	
	/**
	 * Creates a parity game with encoded state information
	 * @param filename	Name of the parity automaton file (filename.hoa)
	 * @param apinfo	Assignment of AP ownership for players
	 * @return			Encoded Rabinizer parity game
	 */
	public static Game createEncoded(String filename, Boolean[] apinfo) {
		Game game = new Game();
		labeledTransitions = new TreeMap<Integer, TreeMap<Byte, Integer>>();
		
		ArrayList<String> first = Rabinizer.firstSubformulas(filename);
		assert(first != null);
		HashMap<String, ArrayList<String>> second = Rabinizer.secondSubformulas(filename);
		assert(second != null);
		ArrayList<String> perm = Rabinizer.permutations(filename);
		assert(perm != null);
		
		String token;
		try (Scanner sc = new Scanner(new File("benchmarks/rabinizer/"+filename+".hoa"))) {
			int numberofP1states = 0;
			int numberofParities = 0;
			
			token = sc.next();
			while (!token.equals("--BODY--")) {
				
				if (token.equals("States:"))
					numberofP1states = sc.nextInt();
				
				if (token.equals("Start:"))
					game.initialState = sc.nextInt();
				
				if (token.equals("Acceptance:"))
					numberofParities = sc.nextInt();
				
				if (token.equals("AP:")) {
					assert(sc.nextInt() == apinfo.length);
					game.varActionP1 = new ArrayList<String>();
					game.varActionP2 = new ArrayList<String>();
					for (int i=0; i<apinfo.length; i++) {
						token = sc.next();
						token = token.replace("\"", "");
						if (apinfo[i])
							game.varActionP2.add(new String(token));
						else
							game.varActionP1.add(new String(token));
					}
					game.varActionP1.trimToSize();
					game.varActionP2.trimToSize();
				}
				
				token = sc.next();
			}
			
			// ENCODED PART START
			
			int firstsublength = first.size();
			int secondsublength = 0;
			for (String key : second.keySet())
				secondsublength += second.get(key).size()+1; // last bit says if anything is there
			int permlength = 1;
			while (Util.bitpower(permlength) <= perm.size()) permlength++;
			int parlength = numberofParities+1; // {0} {1} {2} {3} ... {}
			int p1statebitvectorsize = 2;
			while (Util.bitpower(p1statebitvectorsize) < numberofP1states)
				p1statebitvectorsize++;
			
			game.varStateP1 = new ArrayList<String>(firstsublength + 
					secondsublength + permlength + parlength + p1statebitvectorsize);
			for (int i=0; i<firstsublength; i++)
				game.varStateP1.add("Sfsub"+i);
			for (int i=0; i<secondsublength; i++)
				game.varStateP1.add("Sssub"+i);
			for (int i=0; i<permlength; i++)
				game.varStateP1.add("Sperm"+i);
			for (int i=0; i<parlength; i++)
				game.varStateP1.add("Spar"+i);
			for (int i=0; i<p1statebitvectorsize; i++)
				game.varStateP1.add("Sbitv"+i);
			
			// ENCODED PART FINISH
			
			game.varStateP2 = new ArrayList<String>(game.varStateP1no()+game.varActionP1no());
			game.varStateP2.addAll(game.varStateP1);
			for (int i=0; i<game.varActionP1no(); i++)
				game.varStateP2.add("ENV"+game.varActionP1.get(i));
			
			int p1counter = -1;
			int p2counter = numberofP1states-1;
			while (!sc.hasNext("--END--")) {
				
				// deal with new P1 state
				assert(sc.hasNext("State:"));
				p1counter++;
				sc.next();
				assert(sc.nextInt() == p1counter);
				
				token = sc.nextLine(); // we ignore the state information
				int newparity = numberofParities-1;
				char paritychar = token.charAt(token.length()-2);
				if (paritychar != '{') {
					newparity = Character.getNumericValue(paritychar);
					assert(newparity >= 0 && newparity < numberofParities);
				}
				
				// ENCODED PART START
				
				// PARITY
				
				ArrayList<Boolean> parbitv = new ArrayList<Boolean>(parlength);
				for (int i=0; i<parlength; i++)
					parbitv.add(false);
				
				if (paritychar == '{')
					parbitv.set(parlength-1, true);
				else parbitv.set(Character.getNumericValue(paritychar), true);
				
				// PERMUTATION
				
				int end = token.length()-1;
				while (token.charAt(end) != ']') end--;
				int start = end;
				while (token.charAt(start) != ')') start--;
				start += 2;
				end++;
				
				String cand = token.substring(start, end);
				int there = -1;
				for (int i=0; i<perm.size(); i++)
					if (perm.get(i).equals(cand)) {
						there = i;
						break;
					}
				assert(there != -1);
				ArrayList<Boolean> permbitv = Util.binary(permlength, there);
				
				// SECONDSUB
				
				start = 2;
				while (token.charAt(start-2) != ':' || token.charAt(start-1) != ':') start++;
				assert(token.charAt(start) == '{');
				end = start-1;
				int brackets = 0;
				
				do {
					end++;
					
					if (token.charAt(end) == '{') brackets++;
					if (token.charAt(end) == '}') brackets--;
					
				} while (token.charAt(end) != '}' || brackets > 0);
				end++;
				
				String secondsubtoken = token.substring(start, end);
				ArrayList<Boolean> secondsubbitv = whichSecondSubformulas(secondsubtoken, second);
				
				// FIRSTSUB
				
				start = 0;
				while (token.charAt(start) != '{') start++;
				start++;
				end = start;
				while (token.charAt(end) != ':' || token.charAt(end+1) != ':') end++;
				String firstsubtoken = token.substring(start, end);
				
				ArrayList<Boolean> firstsubbitv = new ArrayList<Boolean>(firstsublength);
				for (int i=0; i<firstsublength; i++)
					firstsubbitv.add(firstsubtoken.contains(first.get(i)));
				
				ArrayList<Boolean> p1statevalues = new ArrayList<Boolean>();
				p1statevalues.addAll(firstsubbitv);
				p1statevalues.addAll(secondsubbitv);
				p1statevalues.addAll(permbitv);
				p1statevalues.addAll(parbitv);
				p1statevalues.addAll(Util.binary(p1statebitvectorsize, p1counter));
				p1statevalues.trimToSize();
				assert(p1statevalues.size() == game.varStateP1no());
				
				// ENCODED PART FINISH
				
				game.addState(p1counter, 1, p1statevalues, newparity);
				
				// deal with all P1->P2->P1 transitions
				TreeMap<Integer, Integer> transitions = new TreeMap<Integer, Integer>();
				while (!sc.hasNext("State:") && !sc.hasNext("--END--")) {
					token = sc.nextLine();
					int j = token.length()-1;
					while (token.charAt(j) != ' ')
						j--;
					int nextp1state = Integer.parseInt(token.substring(j+1));
					
					if (token.charAt(1) == 't') {
						for (int i=0; i<Util.bitpower(apinfo.length); i++) {
							assert(!transitions.containsKey(i));
							transitions.put(i, nextp1state);
						}
					} else {
						int tformulaend = 1;
						while (token.charAt(tformulaend) != ']')
							tformulaend++;
						TransitionFnode tformula = new TransitionFnode(token.substring(0, tformulaend+1));
						
						for (int i=0; i<Util.bitpower(apinfo.length); i++) {
							ArrayList<Boolean> assignment = Util.binary(apinfo.length, i);
							if (tformula.evaluate(assignment)) {
								assert(!transitions.containsKey(i));
								transitions.put(i, nextp1state);
							}
						}
					}
				}
				assert(transitions.keySet().size() == Util.bitpower(apinfo.length));
				
				// add corresponding P2 states, transitions into them and from them
				labeledTransitions.put(p1counter, new TreeMap<Byte,Integer>());
				for (int P1decimal=0; P1decimal<Util.bitpower(game.varActionP1no()); P1decimal++) {
					
					// add the P2 state
					p2counter++;
					ArrayList<Boolean> p2statevalues = new ArrayList<Boolean>();
					p2statevalues.addAll(game.states.get(p1counter).values);
					ArrayList<Boolean> P1binary = Util.binary(game.varActionP1no(), P1decimal);
					p2statevalues.addAll(P1binary);
					p2statevalues.trimToSize();
					
					game.addState(p2counter, 2, p2statevalues, game.states.get(p1counter).parity);
					labeledTransitions.get(p1counter).put((byte) P1decimal, p2counter);
					game.addTransition(p1counter, p2counter);
					
					// add P2->P1 transitions
					labeledTransitions.put(p2counter, new TreeMap<Byte,Integer>());
					for (int P2decimal=0; P2decimal<Util.bitpower(game.varActionP2no()); P2decimal++) {
						ArrayList<Boolean> P2binary = Util.binary(game.varActionP2no(), P2decimal);
						ArrayList<Boolean> TOTALbinary = new ArrayList<Boolean>(game.varActionP1no() + game.varActionP2no());
						int indexP1 = 0;
						int indexP2 = 0;
						for (int l=0; l<apinfo.length; l++)
							if (apinfo[l]) {
								TOTALbinary.add(P2binary.get(indexP2));
								indexP2++;
							} else {
								TOTALbinary.add(P1binary.get(indexP1));
								indexP1++;
							}
						
						int TOTALdecimal = Util.decimal(TOTALbinary);
						assert(transitions.containsKey(TOTALdecimal));
						int nextP1state = transitions.get(TOTALdecimal);
						
						labeledTransitions.get(p2counter).put((byte) P2decimal, nextP1state);
						game.addTransition(p2counter, nextP1state);
					}
				}
				
			}
		} catch (Exception e) {
    		e.printStackTrace();
    		return null;
		}
		
		return game;
		
	}
	
	/**
	 * State-information encoding: permutations in third part
	 * @param filename	Name of the parity automaton file (filename.hoa)
	 * @return			State-information
	 */
	private static ArrayList<String> permutations(String filename) {
		ArrayList<String> result = new ArrayList<String>();
		String token;
		
		try (Scanner sc = new Scanner(new File("benchmarks/rabinizer/"+filename+".hoa"))) {
			do {
				token = sc.nextLine();
				if (token.startsWith("State: ")) {
					int end = token.length()-1;
					while (token.charAt(end) != ']') end--;
					int start = end;
					while (token.charAt(start) != ')') start--;
					start += 2;
					end++;
					
					String cand = token.substring(start, end);
					boolean there = false;
					for (int i=0; i<result.size(); i++)
						if (result.get(i).equals(cand)) {
							there = true;
							break;
						}
					if (!there)
						result.add(cand);
				}
			} while (!token.startsWith("--END--"));
		} catch (Exception e) {
    		//e.printStackTrace();
    		return null;
		}
		
		return result;
	}
	
	/**
	 * State-information encoding: subformulas in second part
	 * @param filename	Name of the parity automaton file (filename.hoa)
	 * @return			State-information
	 */
	private static HashMap<String, ArrayList<String>> secondSubformulas(String filename) {
		HashMap<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
		String token;
		
		try (Scanner sc = new Scanner(new File("benchmarks/rabinizer/"+filename+".hoa"))) {
			do {
				token = sc.nextLine();
				if (token.startsWith("State: ")) {
					
					int start = 2;
					while (token.charAt(start-2) != ':' || token.charAt(start-1) != ':') start++;
					assert(token.charAt(start) == '{');
					int end = start-1;
					int brackets = 0;
					
					do {
						end++;
						
						if (token.charAt(end) == '{') brackets++;
						if (token.charAt(end) == '}') brackets--;
						
					} while (token.charAt(end) != '}' || brackets > 0);
					end++;
					
					token = token.substring(start, end);
					
					// have my token
					
					start = 0;
					brackets = 0;
					while (start < token.length()) {
						if (token.charAt(start) == 'G') {
							int gcandstart = start;
							while (token.charAt(start) != '=') start++;
							int gcandend = start;
							String gcand = token.substring(gcandstart, gcandend);
							
							if (!result.containsKey(gcand))
								result.put(gcand, new ArrayList<String>());
							
							// get gtoken
							assert(token.charAt(start+1) == '{');
							end = start; start++;
							brackets = 0;
							do {
								end++;
								if (token.charAt(end) == '{') brackets++;
								if (token.charAt(end) == '}') brackets--;
							} while (brackets > 0 || token.charAt(end) != '}');
							end++;
							
							String gtoken = token.substring(start, end);
							int gtokenend = end;
							
							
							// find formulae in gtoken, add into vector associated with gcand
							boolean stop;
							assert(gtoken.charAt(0) == '{' && gtoken.charAt(gtoken.length()-1) == '}');
							start = 1;
							do {
								end = start;
								while (gtoken.charAt(end) != '=') end++;
								
								String cand = gtoken.substring(start, end);
								boolean there = false;
								for (int i=0; i<result.get(gcand).size(); i++)
									if (result.get(gcand).get(i).equals(cand)) {
										there = true;
										break;
									}
								if (!there)
									result.get(gcand).add(cand);
								
								stop = true;
								for (int i=end; i<gtoken.length(); i++)
									if (gtoken.charAt(i) == ',') {
										stop = false;
										assert(gtoken.charAt(i+1) == ' ');
										start = i+2;
										break;
									}
							} while (!stop);
							
							start = gtokenend;
						} else start++;
					}
				}
				
			} while (!token.startsWith("--END--"));
		} catch (Exception e) {
    		//e.printStackTrace();
    		return null;
		}
		
		return result;
	}

	/**
	 * State-information encoding: subformulas in second part
	 * @param filename	Name of the parity automaton file (filename.hoa)
	 * @return			State-information
	 */	
	private static ArrayList<Boolean> whichSecondSubformulas(String token, HashMap<String, ArrayList<String>> candidates) {
		ArrayList<Boolean> result = new ArrayList<Boolean>();
		
		ArrayList<String> keys = new ArrayList<String>(candidates.keySet().size());
		for (String key : candidates.keySet())
			keys.add(key);
		
		ArrayList<ArrayList<Boolean>> subr = new ArrayList<ArrayList<Boolean>>(keys.size());
		for (int i=0; i<keys.size(); i++) {
			subr.add(new ArrayList<Boolean>(candidates.get(keys.get(i)).size()+1));
			for (int j=0; j<candidates.get(keys.get(i)).size()+1; j++) // one bit for each candidate plus one bit if this Gcand was present
				subr.get(i).add(false);
		}
		
		// find
		
		int start = 0;
		int brackets = 0;
		int end;
		while (start < token.length()) {
			if (token.charAt(start) == 'G') {
				int gcandstart = start;
				while (token.charAt(start) != '=') start++;
				int gcandend = start;
				String gcand = token.substring(gcandstart, gcandend);
				
				int gcandindex = -1;
				for (int i=0; i<keys.size(); i++)
					if (gcand.equals(keys.get(i))) {
						gcandindex = i;
						break;
					}
				assert(gcandindex != -1);
				
				// set bit that this Gcand was found
				subr.get(gcandindex).set( subr.get(gcandindex).size()-1 , true);
				
				// get gtoken
				assert(token.charAt(start+1) == '{');
				end = start; start++;
				brackets = 0;
				do {
					end++;
					if (token.charAt(end) == '{') brackets++;
					if (token.charAt(end) == '}') brackets--;
				} while (brackets > 0 || token.charAt(end) != '}');
				end++;
				
				String gtoken = token.substring(start, end);
				int gtokenend = end;
				
				// find formulae in gtoken, make corresponding bits true
				boolean stop;
				assert(gtoken.charAt(0) == '{' && gtoken.charAt(gtoken.length()-1) == '}');
				start = 1;
				do {
					end = start;
					while (gtoken.charAt(end) != '=') end++;
					
					String cand = gtoken.substring(start, end);
					int there = -1;
					for (int i=0; i<candidates.get(gcand).size(); i++)
						if (candidates.get(gcand).get(i).equals(cand)) {
							there = i;
							break;
						}
					assert(there != -1);
					assert(subr.get(gcandindex).get(there).equals(false));
					subr.get(gcandindex).set(there, true); // set bit
					
					stop = true;
					for (int i=end; i<gtoken.length(); i++)
						if (gtoken.charAt(i) == ',') {
							stop = false;
							assert(gtoken.charAt(i+1) == ' ');
							start = i+2;
							break;
						}
				} while (!stop);
				
				start = gtokenend;
			} else start++;
		}
		
		// end-find
		
		for (int i=0; i<subr.size(); i++)
			result.addAll(subr.get(i));
		result.trimToSize();
		
		return result;
	}
	
	/**
	 * State-information encoding: subformulas in first part
	 * @param filename	Name of the parity automaton file (filename.hoa)
	 * @return			State-information
	 */
	private static ArrayList<String> firstSubformulas(String filename) {
		String token;
		try (Scanner sc = new Scanner(new File("benchmarks/rabinizer/"+filename+".hoa"))) {
			do {
				token = sc.nextLine();
			} while (!token.startsWith("name: \"Automaton for"));
		} catch (Exception e) {
    		//e.printStackTrace();
    		return null;
		}
		
		ArrayList<String> result = new ArrayList<String>();
		HashMap<String, HashSet<Pair<Integer, Integer>>> positions = new HashMap<String, HashSet<Pair<Integer, Integer>>>();
		TreeSet<Integer> untils = new TreeSet<Integer>();
		
		int i = 0;
		while (token.charAt(i) != '(') i++;
		int j = i;
		while (token.charAt(j) != ':') j++;
		assert(token.charAt(j+1) == ':'); j++;
		
		token = token.substring(i, j);
		
		for (i=0; i<token.length(); i++) {
			// does some subformula start at this location?
			
			// atom
			if (token.charAt(i) == 'p') {
				int start = i;
				if (token.charAt(i-1) == '!') start--;
				int end = i+2;
				assert(Character.getNumericValue(token.charAt(end-1)) >= 0);
				
				String cand = token.substring(start, end);
				
				boolean there = false;
				for (j=0; j<result.size(); j++)
					if (result.get(j).equals(cand)) {
						there = true;
						break;
					}
				if (!there) {
					result.add(cand);
					positions.put(cand, new HashSet<Pair<Integer, Integer>>());
				}
				positions.get(cand).add(new Pair<Integer, Integer>(start, end));
			}
			
			// X,F,G
			if (token.charAt(i) == 'X' || token.charAt(i) == 'F' || token.charAt(i) == 'G') {
				int start = i;
				int end = start;
				int brackets = 0;
				
				boolean stop = false;
				boolean error = false;
				do {
					end++;
					boolean hadbrackets = false;
					
					if (token.charAt(end) == '(') {
						brackets++;
						hadbrackets = true;
					}
						
					
					if (token.charAt(end) == ')')
						brackets--;
					
					stop = (token.charAt(end) == 'U' || token.charAt(end) == '|' || token.charAt(end) == '&') && brackets == 0;
					stop |= (token.charAt(end) == ')') && brackets == -1;
					stop |= (token.charAt(end) == ':');
					
					error = ((token.charAt(end) == 'X' || token.charAt(end) == 'F' || token.charAt(end) == 'G') && brackets == 0 && hadbrackets)
							|| (token.charAt(end) == '(' && brackets == 0 && end != start+1);
					if (error)
						System.out.println("FILE: "+filename+"  start: "+start+"  end: "+end+"  brackets: "+brackets+" form:"
								+token.substring(start, end+1));
					assert(!error);
				} while (!stop);
				
				String cand = token.substring(start, end);
				
				boolean there = false;
				for (j=0; j<result.size(); j++)
					if (result.get(j).equals(cand)) {
						there = true;
						break;
					}
				if (!there) {
					result.add(cand);
					positions.put(cand, new HashSet<Pair<Integer, Integer>>());
				}
				positions.get(cand).add(new Pair<Integer, Integer>(start, end));
			}
			
			// U
			if (token.charAt(i) == 'U')
				untils.add(i);
			
		}
		
		// now handle the Untils
		// need a loop to deal with nested Untils
		
		i = 0;
		do {
			i++; assert(i < 5);
			//System.out.println(positions);
			//System.out.println(untils);
			TreeSet<Integer> newuntils = new TreeSet<Integer>();
			
			for (Integer until : untils) {
				int minbegin = Integer.MAX_VALUE;
				int maxend = -1;
				
				for (String key : positions.keySet())
					for (Pair<Integer, Integer> pos : positions.get(key)) {
						if (pos.second().equals(until) && pos.first() < minbegin)
							minbegin = pos.first();
						if (pos.first().equals(until+1) && pos.second() > maxend)
							maxend = pos.second();
					}
				
				
				if (minbegin == Integer.MAX_VALUE) {
					// handle cases like (p2&p3)Up1 - not a subformula on the left of U
					if (token.charAt(until-1) == ')') {
						int helpbegin = until;
						int brackets = 0;
						boolean stop = false;
						//boolean error = false;
						do {
							helpbegin--;
							
							if (token.charAt(helpbegin) == ')')
								brackets++;
							
							if (token.charAt(helpbegin) == '(')
								brackets--;
							
							stop = (token.charAt(helpbegin) == '&' || token.charAt(helpbegin) == '|') && brackets == 0;
							stop |= token.charAt(helpbegin) == '(' && brackets == -1;
							
						} while (!stop);
						minbegin = helpbegin+1; // the last char you encountered should not be a part of cand
					}
				}
				
				if (maxend == -1) {
					// handle cases like p1U(p2&p3) - not a subformula on the right of U
					if (token.charAt(until+1) == '(') {
						int helpend = until;
						int brackets = 0;
						boolean stop = false;
						//boolean error = false;
						do {
							helpend++;
							
							if (token.charAt(helpend) == '(')
								brackets++;
							
							if (token.charAt(helpend) == ')')
								brackets--;
							
							stop = (token.charAt(helpend) == '&' || token.charAt(helpend) == '|') && brackets == 0;
							stop |= token.charAt(helpend) == ')' && brackets == -1;
							
						} while (!stop);
						maxend = helpend; // the last char you encountered won't be a part of cand this way
					}
				}
				
				if (minbegin == Integer.MAX_VALUE || maxend == -1)
					newuntils.add(until);
				else {
					String cand = token.substring(minbegin, maxend);
					
					boolean there = false;
					for (j=0; j<result.size(); j++)
						if (result.get(j).equals(cand)) {
							there = true;
							break;
						}
					if (!there) {
						result.add(cand);
						positions.put(cand, new HashSet<Pair<Integer, Integer>>());
					}
					positions.get(cand).add(new Pair<Integer, Integer>(minbegin, maxend));
					
				}
			}
			
			untils = newuntils;
		} while (untils.size() > 0);
		
		return result;
	}

	/**
	 * Creates a parity game with naive state information
	 * @param filename	Name of the parity automaton file (filename.hoa)
	 * @param apinfo	Assignment of AP ownership for players
	 * @return			Naive Rabinizer parity game
	 */	
	public static Game createNaive(String filename, Boolean[] apinfo) {
		Game game = new Game();
		labeledTransitions = new TreeMap<Integer, TreeMap<Byte, Integer>>();
		
		String token;
		try (Scanner sc = new Scanner(new File("benchmarks/rabinizer/"+filename+".hoa"))) {
			int numberofP1states = 0;
			int numberofParities = 0;
			
			token = sc.next();
			while (!token.equals("--BODY--")) {
				
				if (token.equals("States:"))
					numberofP1states = sc.nextInt();
				
				if (token.equals("Start:"))
					game.initialState = sc.nextInt();
				
				if (token.equals("Acceptance:"))
					numberofParities = sc.nextInt();
				
				if (token.equals("AP:")) {
					assert(sc.nextInt() == apinfo.length);
					game.varActionP1 = new ArrayList<String>();
					game.varActionP2 = new ArrayList<String>();
					for (int i=0; i<apinfo.length; i++) {
						token = sc.next();
						token = token.replace("\"", "");
						if (apinfo[i])
							game.varActionP2.add(new String(token));
						else
							game.varActionP1.add(new String(token));
					}
					game.varActionP1.trimToSize();
					game.varActionP2.trimToSize();
				}
				
				token = sc.next();
			}
			
			int p1statebitvectorsize = 2;
			while (Util.bitpower(p1statebitvectorsize) < numberofP1states)
				p1statebitvectorsize++;
			
			game.varStateP1 = new ArrayList<String>(p1statebitvectorsize);
			for (int i=0; i<p1statebitvectorsize; i++)
				game.varStateP1.add("s"+i);
			
			game.varStateP2 = new ArrayList<String>(p1statebitvectorsize+game.varActionP1no());
			game.varStateP2.addAll(game.varStateP1);
			for (int i=0; i<game.varActionP1no(); i++)
				game.varStateP2.add("ENV"+game.varActionP1.get(i));
			
			int p1counter = -1;
			int p2counter = numberofP1states-1;
			while (!sc.hasNext("--END--")) {
				
				// deal with new P1 state
				assert(sc.hasNext("State:"));
				p1counter++;
				sc.next();
				assert(sc.nextInt() == p1counter);
				
				token = sc.nextLine(); // we ignore the state information
				int newparity = numberofParities-1;
				char paritychar = token.charAt(token.length()-2);
				if (paritychar != '{') {
					newparity = Character.getNumericValue(paritychar);
					assert(newparity >= 0 && newparity < numberofParities);
				}
				
				game.addState(p1counter, 1, Util.binary(p1statebitvectorsize, p1counter), newparity);
				
				
				// deal with all P1->P2->P1 transitions
				TreeMap<Integer, Integer> transitions = new TreeMap<Integer, Integer>();
				while (!sc.hasNext("State:") && !sc.hasNext("--END--")) {
					token = sc.nextLine();
					int j = token.length()-1;
					while (token.charAt(j) != ' ')
						j--;
					int nextp1state = Integer.parseInt(token.substring(j+1));
					
					if (token.charAt(1) == 't') {
						for (int i=0; i<Util.bitpower(apinfo.length); i++) {
							assert(!transitions.containsKey(i));
							transitions.put(i, nextp1state);
						}
					} else {
						int tformulaend = 1;
						while (token.charAt(tformulaend) != ']')
							tformulaend++;
						TransitionFnode tformula = new TransitionFnode(token.substring(0, tformulaend+1));
						
						for (int i=0; i<Util.bitpower(apinfo.length); i++) {
							ArrayList<Boolean> assignment = Util.binary(apinfo.length, i);
							if (tformula.evaluate(assignment)) {
								assert(!transitions.containsKey(i));
								transitions.put(i, nextp1state);
							}
						}
					}
				}
				assert(transitions.keySet().size() == Util.bitpower(apinfo.length));
				
				// add corresponding P2 states, transitions into them and from them
				labeledTransitions.put(p1counter, new TreeMap<Byte,Integer>());
				for (int P1decimal=0; P1decimal<Util.bitpower(game.varActionP1no()); P1decimal++) {
					
					// add the P2 state
					p2counter++;
					ArrayList<Boolean> p2statevalues = new ArrayList<Boolean>();
					p2statevalues.addAll(game.states.get(p1counter).values);
					ArrayList<Boolean> P1binary = Util.binary(game.varActionP1no(), P1decimal);
					p2statevalues.addAll(P1binary);
					p2statevalues.trimToSize();
					
					game.addState(p2counter, 2, p2statevalues, game.states.get(p1counter).parity);
					labeledTransitions.get(p1counter).put((byte) P1decimal, p2counter);
					game.addTransition(p1counter, p2counter);
					
					// add P2->P1 transitions
					labeledTransitions.put(p2counter, new TreeMap<Byte,Integer>());
					for (int P2decimal=0; P2decimal<Util.bitpower(game.varActionP2no()); P2decimal++) {
						ArrayList<Boolean> P2binary = Util.binary(game.varActionP2no(), P2decimal);
						ArrayList<Boolean> TOTALbinary = new ArrayList<Boolean>(game.varActionP1no() + game.varActionP2no());
						int indexP1 = 0;
						int indexP2 = 0;
						for (int l=0; l<apinfo.length; l++)
							if (apinfo[l]) {
								TOTALbinary.add(P2binary.get(indexP2));
								indexP2++;
							} else {
								TOTALbinary.add(P1binary.get(indexP1));
								indexP1++;
							}
						
						int TOTALdecimal = Util.decimal(TOTALbinary);
						assert(transitions.containsKey(TOTALdecimal));
						int nextP1state = transitions.get(TOTALdecimal);
						
						labeledTransitions.get(p2counter).put((byte) P2decimal, nextP1state);
						game.addTransition(p2counter, nextP1state);
					}
				}
				
			}
		} catch (Exception e) {
    		e.printStackTrace();
    		return null;
		}
		
		return game;
	}
	
	
	static class TransitionFnode {
		
		ArrayList<TransitionFnode> children;
		int ap;
		Boolean conjunction;
		Boolean disjunction;
		Boolean atom;
		Boolean negatedatom;
		
		TransitionFnode(String formula) {
			children = new ArrayList<TransitionFnode>();
			// set up the string
			if (formula.charAt(0) == '[' && formula.charAt(formula.length()-1) == ']')
				formula = formula.substring(1, formula.length()-1);
			if (formula.charAt(0) == ' ')
				formula = formula.substring(1);
			if (formula.charAt(formula.length()-1) == ' ')
				formula = formula.substring(0, formula.length()-1);
			if (formula.charAt(0) == '(' && formula.charAt(formula.length()-1) == ')')
				formula = formula.substring(1, formula.length()-1);
			// check for disjunctions
			ArrayList<Integer> delimiters = new ArrayList<Integer>();
			delimiters.add(-1);
			int inbrackets = 0;
			for (int i=0; i<formula.length(); i++) {
				if (formula.charAt(i) == '(')
					inbrackets++;
				if (formula.charAt(i) == ')')
					inbrackets--;
				if (formula.charAt(i) == '|' && inbrackets == 0)
					delimiters.add(i);
			}
			if (delimiters.size() > 1) {
				disjunction = true; conjunction = false; atom = false; negatedatom = false;
				delimiters.add(formula.length());
				for (int i=0; i<delimiters.size()-1; i++)
					children.add(new TransitionFnode(formula.substring
							(delimiters.get(i)+1, delimiters.get(i+1))));
				return;
			}
			// check for conjunctions
			inbrackets = 0;
			for (int i=0; i<formula.length(); i++) {
				if (formula.charAt(i) == '(')
					inbrackets++;
				if (formula.charAt(i) == ')')
					inbrackets--;
				if (formula.charAt(i) == '&' && inbrackets == 0)
					delimiters.add(i);
			}
			if (delimiters.size() > 1) {
				disjunction = false; conjunction = true; atom = false; negatedatom = false;
				delimiters.add(formula.length());
				for (int i=0; i<delimiters.size()-1; i++)
					children.add(new TransitionFnode(formula.substring
							(delimiters.get(i)+1, delimiters.get(i+1))));
				return;
			}
			// atom or a negatedatom
			disjunction = false; conjunction = false;
			if (formula.charAt(0) == '!') {
				atom = false; negatedatom = true;
				formula = formula.substring(1);
			} else {
				atom = true; negatedatom = false;
			}
			ap = Integer.parseInt(formula);
		}
		
		Boolean evaluate(ArrayList<Boolean> assignment) {
			if (disjunction) {
				for (int i=0; i<children.size(); i++)
					if (children.get(i).evaluate(assignment))
						return true;
				return false;
			} else
			if (conjunction) {
				for (int i=0; i<children.size(); i++)
					if (!children.get(i).evaluate(assignment))
						return false;
				return true;
			} else
			if (atom)
				return (assignment.get(ap));
			else
				return (!assignment.get(ap));
		}
	}	
}
