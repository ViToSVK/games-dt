package machinelearning;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import util.Pair;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class DecisionTree {
	
	public Node root;
	public int numberOfNodes;
	public int numberOfInnerNodes;
	public int lookaheadFiredUp;
	public int heuristicFiredUp;
	public ArrayList<String> attNames;
	
	/**
	 * Builds the decision tree using the improved ID3 algorithm
	 * @param data				Dataset from which the decision tree is built
	 * @param uselookahead		If not, we jump straight to the heuristic if stuck
	 * @param allowdisjunction	False - only atoms as predicates, True - disjunctions allowed
	 */
	public DecisionTree(Dataset data, boolean uselookahead, boolean allowdisjunction) {
		assert(data.instances.size() >= 1);
		assert(data.attributes.size() >= 1);
		
		root = new Node(data, null, 0);
		numberOfNodes = 1;
		numberOfInnerNodes = 0;
		lookaheadFiredUp = 0;
		heuristicFiredUp = 0;
		attNames = new ArrayList<String>(data.attributes);
		
		LinkedList<Node> queue = new LinkedList<Node>();
		queue.add(root);
		
		while (queue.size() > 0) {
			Node current = queue.remove();
			
			HashSet<Pair<Boolean,Integer>> splitPredicate = null;
			
			if (current.dataset.numYES > 0 && current.dataset.numNO > 0) {
				splitPredicate = current.dataset.bestInfoGain(allowdisjunction);
				
				if (splitPredicate == null && uselookahead) {
					int LAsplit = current.dataset.bestInfoGainLA();
					lookaheadFiredUp++;
					if (LAsplit > -1) {
						splitPredicate = new HashSet<Pair<Boolean,Integer>>();
						splitPredicate.add(new Pair<Boolean,Integer>(true, LAsplit));
					}
				}
				
				if (splitPredicate == null) {
					int HeuristicSplit = current.dataset.heuristicSplit();
					heuristicFiredUp++;
					if (HeuristicSplit > -1) {
						splitPredicate = new HashSet<Pair<Boolean,Integer>>();
						splitPredicate.add(new Pair<Boolean,Integer>(true, HeuristicSplit));						
					}
				}
			}
			
			if (splitPredicate == null) { // current becomes a leaf node
				current.label = current.classification?"YES":"NO";
			} else { // current becomes an inner node
				numberOfInnerNodes++;
				current.predicate = new HashSet<Pair<Boolean,Integer>>(splitPredicate.size());
				for (Pair<Boolean,Integer> atom : splitPredicate)
					current.predicate.add(new Pair<Boolean,Integer>(atom.first(), 
							current.dataset.positions.get(atom.second())));
				
				StringBuilder newlabel = new StringBuilder("");
				int size = 0;
				for (int i=0; i<attNames.size(); i++)
					for (Pair<Boolean,Integer> atom : current.predicate)
						if (atom.second() == i) {
							size++;
							newlabel.append((size>1?"\n":"")+(atom.first()?"":"! ")+attNames.get(i));
							break;
						}
				current.label = newlabel.toString();
				
				Pair<Dataset,Dataset> subdatasets = current.dataset.split(splitPredicate);
				current.children = new ArrayList<Node>(2);
				current.children.add(new Node(subdatasets.first(), current, numberOfNodes));
				numberOfNodes++;
				queue.add(current.children.get(0));
				current.children.add(new Node(subdatasets.second(), current, numberOfNodes));
				numberOfNodes++;
				queue.add(current.children.get(1));
			}
			
			current.dataset = null;
		}
	}
	
	/**
	 * Classifies a given sample using this tree
	 * @param sample 	The sample to be classified
	 * @return			TRUE - yes, FALSE - no
	 */
	public Boolean classify(ArrayList<Boolean> sample) {
		assert(sample.size() == attNames.size());
		
		Node current = root;
		while (!current.isLeaf()) {
			boolean satisfied = false;
			for (Pair<Boolean,Integer> atom : current.predicate)
				if (atom.first() == sample.get(atom.second())) {
					satisfied = true;
					break;
				}
			
			if (satisfied)
				current = current.children.get(1);
			else
				current = current.children.get(0);
		}
		
		return current.classification;
	}
	
	/**
	 * @return The decision tree as a string
	 */
	private String graph() {
		StringBuilder result = new StringBuilder();
		LinkedList<Node> stack = new LinkedList<Node>();
		LinkedList<String> stackLabel = new LinkedList<String>();
		stack.addFirst(root);
	    result.append("digraph DecisionTree {\n");
		
		while (stack.size() > 0) {
			Node current = stack.remove();
			
			if (current.parent != null) {
				String parentlabel = stackLabel.remove();
			    result.append("N" + current.parent.id
			    		+ "->" + "N" + current.id +
			    		" [label=\"= " + parentlabel + "\"]\n");
			}
			
			if (current.isLeaf()) {
				result.append("N" + current.id +
						" [label=\"" + current.label + "\" " +
						"shape=box style=\"rounded,filled\"]\n");
			} else {
				result.append("N" + current.id +
						" [label=\"" + current.label + "\" " +
						"shape=box]\n");
				for (int i=1; i>=0; i--) {
					stack.addFirst(current.children.get(i));
					stackLabel.addFirst((i == 1)?"1":"0");
				}
			}
		}
		
		result.append("}\n");
		
		return result.toString();
	}
	
	/**
	 * Creates a dot file with the string representation of the decision tree <br>
	 * Location: results/treesDOT/filename.dot
	 * @param filename The name of the dot file
	 */
	public void dotFile(String filename) {
		File outputFile = new File("results/treesDOT/"+filename+".dot");
        
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
        	writer.write(graph());
        } catch (Exception e) {
        	System.out.println("Exception when saving the Decision tree into a dot file.");
        	e.printStackTrace();        	
        }
	}
	
	static class Node {
		int id;
		String label;
		Dataset dataset;
		Boolean classification;
		HashSet<Pair<Boolean,Integer>> predicate;
		ArrayList<Node> children;
		Node parent;
		
		private Node(Dataset dataset, Node parent, int id) {
			this.id = id;
			this.label = null;
			this.dataset = dataset;
			if (dataset.instances.size() == 0)
				classification = parent.classification;
			else classification = (dataset.numYES > dataset.numNO);
			predicate = null;
			children = null;
			this.parent = parent;
		}
		
		Boolean isLeaf() { return (predicate == null); }
	}
}
