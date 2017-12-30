package creator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.TreeMap;

import util.Pair;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class AigerCircuit {
	
	private static AigerCircuit instance = new AigerCircuit();
	
	private int m; //maximum variable index
	private int i; //number of inputs
	private int l; //number of latches
	private int o; //index of output bit
	private int a; //number of AND-gates
	private String outputLabel;
	
	private TreeMap<Integer,Input> inputs;
	private TreeMap<Integer,Latch> latches;
	private TreeMap<Integer,AndGate> andgates;
	private ArrayList<Integer> inputIndexes;
	private ArrayList<Integer> environmentIndexes;
	private ArrayList<Integer> controllerIndexes;
	private ArrayList<Integer> latchIndexes;
	
	public int getLatchBits() { return l; }
	public int getEnvBits() { return environmentIndexes.size(); }
	public int getContBits() { return controllerIndexes.size(); }
	public ArrayList<String> getSafetyLabels() {
		ArrayList<String> result = new ArrayList<String>(l+environmentIndexes.size()+controllerIndexes.size());
		for (int j=0; j<l; j++)
			result.add(new String(latches.get(latchIndexes.get(j)).label));
		for (int j=0; j<environmentIndexes.size(); j++)
			result.add(new String(inputs.get(environmentIndexes.get(j)).label));
		for (int j=0; j<controllerIndexes.size(); j++)
			result.add(new String(inputs.get(controllerIndexes.get(j)).label));
		return result;
	}
	
	public static AigerCircuit getInstance() { return instance; }
	
	private AigerCircuit() {
		inputs = new TreeMap<Integer,Input>();
		latches = new TreeMap<Integer,Latch>();
		andgates = new TreeMap<Integer,AndGate>();
		inputIndexes = new ArrayList<Integer>();
		environmentIndexes = new ArrayList<Integer>();
		controllerIndexes = new ArrayList<Integer>();
		latchIndexes = new ArrayList<Integer>();
		outputLabel = "";
	}
	
	/** Executes the circuit, updates the latches and obtains the output
	 * @param einput	Environment bits input
	 * @param cinput	Controller bits input
	 * @param oldstate	Previous latch bit values
	 * @return			Output bit and new latch bit values
	 * */
	public Pair<Boolean,ArrayList<Boolean>> compute(ArrayList<Boolean> einput, ArrayList<Boolean> cinput, ArrayList<Boolean> oldstate) 
			throws IllegalArgumentException {
		if (einput.size() != environmentIndexes.size())
			throw new IllegalArgumentException("Environment vector wrong dimension!");
		if (cinput.size() != controllerIndexes.size())
			throw new IllegalArgumentException("Controller vector wrong dimension!");		
		if (oldstate.size() != latchIndexes.size())
			throw new IllegalArgumentException("Old state vector wrong dimension!");
		
		for (int j=0; j<einput.size(); j++)
			inputs.get(environmentIndexes.get(j)).value = einput.get(j);
		for (int j=0; j<cinput.size(); j++)
			inputs.get(controllerIndexes.get(j)).value = cinput.get(j);
		for (int j=0; j<l; j++)
			latches.get(latchIndexes.get(j)).value = oldstate.get(j);
		
		for (int key : andgates.keySet())
			andgates.get(key).ready = false;
		
		AigerCircuit circuit = AigerCircuit.getInstance();
		
		Boolean result = false;
		if (o % 2 == 0) {
			if (circuit.inputs.containsKey(o))
				result = circuit.inputs.get(o).value;
			if (circuit.andgates.containsKey(o))
				result = circuit.andgates.get(o).getValue();
			if (circuit.latches.containsKey(o))
				result = circuit.latches.get(o).value;
		} else {
			if (o == 1) result = true;
			if (circuit.inputs.containsKey(o-1))
				result = !(circuit.inputs.get(o-1).value);
			if (circuit.andgates.containsKey(o-1))
				result = !(circuit.andgates.get(o-1).getValue());
			if (circuit.latches.containsKey(o-1))
				result = !(circuit.latches.get(o-1).value);
		}
		
		ArrayList<Boolean> newstate = new ArrayList<Boolean>(l);
		for (int j=0; j<l; j++)
			newstate.add(latches.get(latchIndexes.get(j)).getNewValue());
		
		return new Pair<Boolean,ArrayList<Boolean>>(result,newstate);
	}
	
	/** Creates a circuit from the AIGER file benchmarks/aiger/filename.aag
	 * @param filename	Name of the AIGER file
	 * */
	public void parse(String filename) throws FileNotFoundException, InputMismatchException, NoSuchElementException,
	IllegalArgumentException, NullPointerException, NumberFormatException {
		inputs.clear();
		latches.clear();
		andgates.clear();
		inputIndexes.clear();
		environmentIndexes.clear();
		controllerIndexes.clear();
		latchIndexes.clear();
		
		try (Scanner scanner = new Scanner(new File("benchmarks/aiger/"+filename+".aag"))) {
			if (!scanner.next().equals("aag")) throw new IllegalArgumentException("First token is not aag!");
			m = scanner.nextInt();
			if (m < 0) throw new IllegalArgumentException("M is negative!");
			i = scanner.nextInt();
			if (i < 0) throw new IllegalArgumentException("I is negative!");
			l = scanner.nextInt();
			if (i < 0) throw new IllegalArgumentException("L is negative!");
			if (scanner.nextInt() != 1) throw new IllegalArgumentException("O is not one!");
			a = scanner.nextInt();
			if (a < 0) throw new IllegalArgumentException("A is negative!");
			if (m != i+l+a) throw new IllegalArgumentException("M does not equal I+L+A!");
			
			inputIndexes.ensureCapacity(i);
			for (int j=0; j<i; j++) {
				int newindex = scanner.nextInt();
				if ((newindex > 2*m) || (newindex < 2) || (newindex % 2 != 0))
					throw new IllegalArgumentException("Input index out of range!");
				inputs.put(newindex, new Input());
				inputIndexes.add(newindex);
			}
			
			latchIndexes.ensureCapacity(l);
			for (int j=0; j<l; j++) {
				int newindex = scanner.nextInt();
				if ((newindex > 2*m) || (newindex < 2) || (newindex % 2 != 0))
					throw new IllegalArgumentException("Latch index out of range!");
				int newparent = scanner.nextInt();
				if ((newparent > 2*m+1) || (newparent < 0))
					throw new IllegalArgumentException("Latch parent out of range!");
				latches.put(newindex, new Latch(newparent));
				latchIndexes.add(newindex);
			}
			
			o = scanner.nextInt();
			if ((o > 2*m+1) || (o < 0))
				throw new IllegalArgumentException("Output index out of range!");
			
			for (int j=0; j<a; j++) {
				int newindex = scanner.nextInt();
				if ((newindex > 2*m) || (newindex < 2) || (newindex % 2 != 0))
					throw new IllegalArgumentException("Latch index out of range!");
				int newparent1 = scanner.nextInt();
				if ((newparent1 > 2*m+1) || (newparent1 < 0))
					throw new IllegalArgumentException("Latch parent1 out of range!");
				int newparent2 = scanner.nextInt();
				if ((newparent2 > 2*m+1) || (newparent2 < 0))
					throw new IllegalArgumentException("Latch parent2 out of range!");
				andgates.put(newindex, new AndGate(newparent1, newparent2));
			}
			
			Boolean done = false;
			while (scanner.hasNext() && !done) {
				String next = scanner.next();
				if (next.startsWith("i")) { //input label incoming
					int number = Integer.parseInt(next.substring(1));
					if ((number < 0) || (number >= i))
						throw new IllegalArgumentException("Input number out of range!");
					if (!scanner.hasNextLine())
						throw new IllegalArgumentException("Expected label for input!");
					String label = scanner.nextLine().substring(1);
					label = label.replace("controllable_", "controllable.");
					label = label.replace("_", "");
					label = label.replace("+", "");
					inputs.get(inputIndexes.get(number)).label = label;
				} else
				if (next.startsWith("l")) { //latch label incoming
					int number = Integer.parseInt(next.substring(1));
					if ((number < 0) || (number >= l))
						throw new IllegalArgumentException("Latch number out of range!");
					if (!scanner.hasNextLine())
						throw new IllegalArgumentException("Expected label for latch!");
					String label = scanner.nextLine().substring(1);
					label = label.replace("_", "");
					label = label.replace("+", "");
					latches.get(latchIndexes.get(number)).label = label;
				} else
				if (next.equals("o")) { //output label incoming
					if (!scanner.hasNextLine())
						throw new IllegalArgumentException("Expected label for output!");
					String label = scanner.nextLine().substring(1);
					label = label.replace("_", "");
					label = label.replace("+", "");
					outputLabel = label;
				} else done = true;
			}
			
			controllerIndexes.ensureCapacity(i);
			environmentIndexes.ensureCapacity(i);
			for (int j=0; j<i; j++) {
				if (inputs.get(inputIndexes.get(j)).label.startsWith("controllable"))
					controllerIndexes.add(inputIndexes.get(j));
				else environmentIndexes.add(inputIndexes.get(j));
			}
			controllerIndexes.trimToSize();
			environmentIndexes.trimToSize();
			
			for (int j=0; j<i; j++) {
				if (inputs.get(inputIndexes.get(j)).label.equals(""))
					inputs.get(inputIndexes.get(j)).label = "nolabel_"+inputIndexes.get(j);
			}
			
			for (int j=0; j<l; j++) {
				if (latches.get(latchIndexes.get(j)).label.equals(""))
					latches.get(latchIndexes.get(j)).label = "nolabel_"+latchIndexes.get(j);
			}
			
			if (outputLabel.equals("")) outputLabel = "nolabel_"+o;
		}
	}
	
	class Input {
		Boolean value;
		String label;
		Input() {
			value = false;
			label = "";
		}
	}
	
	class AndGate {
		Boolean value;
		Boolean ready;
		int parent1;
		int parent2;
		AndGate(int parent1, int parent2) {
			value = false;
			ready = false;
			this.parent1 = parent1;
			this.parent2 = parent2;
		}
		Boolean getValue() {
			if (ready) return value;
			AigerCircuit circuit = AigerCircuit.getInstance();
			Boolean result = false;
			if (parent1 % 2 == 0) {
				if (circuit.inputs.containsKey(parent1))
					result = circuit.inputs.get(parent1).value;
				if (circuit.andgates.containsKey(parent1))
					result = circuit.andgates.get(parent1).getValue();
				if (circuit.latches.containsKey(parent1))
					result = circuit.latches.get(parent1).value;
			} else {
				if (parent1 == 1) result = true;
				if (circuit.inputs.containsKey(parent1-1))
					result = !(circuit.inputs.get(parent1-1).value);
				if (circuit.andgates.containsKey(parent1-1))
					result = !(circuit.andgates.get(parent1-1).getValue());
				if (circuit.latches.containsKey(parent1-1))
					result = !(circuit.latches.get(parent1-1).value);
			}
			Boolean result2 = false;
			if (parent2 % 2 == 0) {
				if (circuit.inputs.containsKey(parent2))
					result2 = circuit.inputs.get(parent2).value;
				if (circuit.andgates.containsKey(parent2))
					result2 = circuit.andgates.get(parent2).getValue();
				if (circuit.latches.containsKey(parent2))
					result2 = circuit.latches.get(parent2).value;
			} else {
				if (parent2 == 1) result2 = true;
				if (circuit.inputs.containsKey(parent2-1))
					result2 = !(circuit.inputs.get(parent2-1).value);
				if (circuit.andgates.containsKey(parent2-1))
					result2 = !(circuit.andgates.get(parent2-1).getValue());
				if (circuit.latches.containsKey(parent2-1))
					result2 = !(circuit.latches.get(parent2-1).value);
			}
			result &= result2;
			value = result;
			ready = true;
			return result;
		}
	}
	
	class Latch {
		Boolean value;
		int parent;
		String label;
		Latch(int parent) {
			value = false;
			this.parent = parent;
			label = "";
		}
		Boolean getNewValue() {
			AigerCircuit circuit = AigerCircuit.getInstance();
			Boolean result = false;
			if (parent % 2 == 0) {
				if (circuit.inputs.containsKey(parent))
					result = circuit.inputs.get(parent).value;
				if (circuit.andgates.containsKey(parent))
					result = circuit.andgates.get(parent).getValue();
				if (circuit.latches.containsKey(parent))
					result = circuit.latches.get(parent).value;
			} else {
				if (parent == 1) result = true;
				if (circuit.inputs.containsKey(parent-1))
					result = !(circuit.inputs.get(parent-1).value);
				if (circuit.andgates.containsKey(parent-1))
					result = !(circuit.andgates.get(parent-1).getValue());
				if (circuit.latches.containsKey(parent-1))
					result = !(circuit.latches.get(parent-1).value);				
			}
			return result;
		}
	}
	
}
