package machinelearning;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class BinaryDecisionDiagram {
	
	private BDDFactory B;
	public BDD root;
	
	/**
	 * Constructor which creates a binary decision diagram from a given dataset
	 * @param dataset The given dataset
	 * @param reorder Reorder the variables?
	 * @param seed	  Seed used for reorder
	 */
	public BinaryDecisionDiagram(Dataset dataset, boolean reorder, int seed) {
		assert(dataset.attributes.size() >= 1);
		assert(dataset.instances.size() >= 1);
		
		PrintStream old = System.out;
	    System.setOut(new PrintStream(new OutputStream() {
            public void write(int b) { // Could not load BDD package buddy: Could not initialize class net.sf.javabdd.BuDDyFactory
            	}}));
		B = BDDFactory.init(1000, 1000);
		B.setVarNum(dataset.attributes.size());
		System.setOut(old);
		
		ArrayList<Integer> order = new ArrayList<Integer>(dataset.attributes.size());
		for(int i=0; i<dataset.attributes.size(); i++) order.add(i);
		if (reorder) Collections.shuffle(order, new Random(seed));
		
		BDD[] v = new BDD[dataset.attributes.size()];
		BDD[] nv = new BDD[dataset.attributes.size()];
		for (int i=0; i<dataset.attributes.size(); i++) {
			v[i] = B.ithVar(order.get(i));
			nv[i] = B.nithVar(order.get(i));
		}
		
		BDD[] result = new BDD[dataset.numYES];
		int k=0;
		for (int i=0; i<dataset.instances.size(); i++)
			if (dataset.instances.get(i).classValue) {
				Instance sample = dataset.instances.get(i);
				if (!sample.attValues.get(0))
					result[k] = nv[0]; else result[k] = v[0];
				for (int j=1; j<dataset.attributes.size(); j++)
					if (!sample.attValues.get(j))
						result[k] = result[k].and(nv[j]);
					else result[k] = result[k].and(v[j]);
				if (k > 0) result[k] = result[k].or(result[k-1]);
				k++;
			}
		
		root = result[k-1];
		//B.done();
	}	
	
	/**
	 * Creates a dot file with the string representation of the BDD <br>
	 * Location: results/bddsDOT/filename.dot
	 * @param filename The name of the dot file
	 */
	public void dotFile(String filename) {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    PrintStream ps = new PrintStream(baos);
	    PrintStream old = System.out;
	    System.setOut(ps);
	    root.printDot();
	    System.out.flush();
	    System.setOut(old);
		
	    String dot = baos.toString();
	    
		File outputFile = new File("results/bddsDOT/"+filename+".dot");

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
        	writer.write(dot);
        } catch (Exception e) {
        	System.out.println("Exception when saving the BDD into a dot file.");
        	e.printStackTrace();
        }
	}
	
	public int numberOfInnerNodes() { return root.nodeCount(); }
	
}
