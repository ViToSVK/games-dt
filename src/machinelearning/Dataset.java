package machinelearning;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

import model.Game;
import strategy.Strategy;
import util.Pair;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Dataset {

	public ArrayList<Integer> positions;
	public ArrayList<String> attributes;
	public ArrayList<Instance> instances;
	public byte player;
	public char objective; // 's'afety 'r'eachability 'p'arity
	public int numYES;
	public int numNO;

	/** Constructor which creates a dataset from given attributes and strategy
	 * @param  game       Game instance
	 * @param  strategy   Use this strategy to create instances
	 * */
	public Dataset(Game game, Strategy strategy) {
		this.player = strategy.player;
		this.objective = strategy.objective;
		this.numYES = strategy.bitvYES;
		this.numNO = strategy.bitvNO;

		attributes = new ArrayList<String>();
		if (strategy.player == 1) {
			for (int j=0; j<game.varStateP1no(); j++)
				attributes.add(game.varStateP1.get(j));
			for (int j=0; j<game.varActionP1no(); j++)
				attributes.add(game.varActionP1.get(j));
		} else {
			for (int j=0; j<game.varStateP2no(); j++)
				attributes.add(game.varStateP2.get(j));
			for (int j=0; j<game.varActionP2no(); j++)
				attributes.add(game.varActionP2.get(j));
		}
		attributes.trimToSize();

		positions = new ArrayList<Integer>(attributes.size());
		for (int i=0; i<attributes.size(); i++)
			positions.add(i);

		instances = new ArrayList<Instance>(strategy.bitv.keySet().size());
		int keysizeS = (strategy.player == 1)?game.varStateP1no():game.varStateP2no();
		int keysizeA = (strategy.player == 1)?game.varActionP1no():game.varActionP2no();
		for (Pair<ArrayList<Boolean>,ArrayList<Boolean>> key : strategy.bitv.keySet()) {
			assert(keysizeS == key.first().size() && keysizeA == key.second().size());
			instances.add(new Instance(key, strategy.bitv.get(key)));
		}
	}

	/**
	 * Creates a dataset from an ARFF file
	 * @param filename		Read this ARFF file
	 * @param writer		Here write the report
	 * @throws IOException	Propagate this to the method that gave you the writer handle
	 */
	public Dataset(String filename, BufferedWriter writer) throws IOException {
		try (Scanner sc = new Scanner(new File("results/datasets/"+filename+".arff"))) {
			String token;
			int test;

			this.positions = new ArrayList<Integer>();
			this.attributes = new ArrayList<String>();
			this.instances = new ArrayList<Instance>();

			boolean now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("FILE:")) now = true;
			}
			token = sc.next();
			token = token.substring(6);
			writer.write(String.format("%-30s",token));

			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("ESTIMATED:")) now = true;
			}
			test = sc.nextInt();
			writer.write(String.format("%12d",test));

			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("ACTIONVARP1:")) now = true;
			}
			test = sc.nextInt();
			writer.write(String.format("%6d",test));

			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("ACTIONVARP2:")) now = true;
			}
			test = sc.nextInt();
			writer.write(String.format("%6d",test));

			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("PLAYER:")) now = true;
			}
			test = sc.nextInt();
			assert(test == 2 || test == 1);
			this.player = (byte) test;

			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("OBJECTIVE:")) now = true;
			}
			token = sc.next();
			assert(token.equals("s") || token.equals("r") || token.equals("p"));
			this.objective = token.charAt(0);

			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("NUMYES:")) now = true;
			}
			test = sc.nextInt();
			this.numYES = test;

			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("NUMNO:")) now = true;
			}
			test = sc.nextInt();
			this.numNO = test;

			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("NUMTOT:")) now = true;
			}
			int samples = sc.nextInt();
			writer.write(String.format("%16d",samples));

			now = false;
			while (!now) {
				token = sc.next();
				if (token.equals("@RELATION")) now = true;
			}
			token = sc.next();
			assert(token.equals("strategy"));

			int attNumber = 0;
			while (!token.equals("@DATA")) {
				token = sc.next();
				if (token.equals("@ATTRIBUTE")) {
					token = sc.next();
					token = token.replace("\"", "");
					if (!token.equals("class")) {
						this.positions.add(attNumber);
						this.attributes.add(token);
						attNumber++;
					}
				}
			}

			// now begins the strategy

			for (int i=0; i<samples; i++) {
				token = sc.next();
				ArrayList<Boolean> newb = new ArrayList<Boolean>();
				for (int j=0; j<attNumber; j++) {
					assert(token.charAt(2*j) == '0' || token.charAt(2*j) == '1');
					newb.add(token.charAt(2*j) == '1');
				}
				assert(token.charAt(2*attNumber) == 'y' || token.charAt(2*attNumber) == 'n');
				this.instances.add(new Instance(newb, (token.charAt(2*attNumber) == 'y')));
			}

			assert(this.instances.size() == this.numNO + this.numYES);
		}
	}

	/** Private constructor used in method split for creating a dataset baseline
	 * @param  source  	 Source dataset
	 * @param  predicate Disjunction (or a single atom) used to split the dataset
	 * */
	private Dataset(Dataset source, HashSet<Pair<Boolean,Integer>> predicate) {
		assert(predicate.size() > 0);
		for (Pair<Boolean,Integer> atom : predicate)
			assert(atom.second() >= 0 && atom.second() < source.attributes.size());
		assert(source.attributes.size() == source.positions.size());

		this.player = source.player;
		this.objective = source.objective;
		this.numYES = 0;
		this.numNO = 0;

		this.attributes = new ArrayList<String>(source.attributes.size()-predicate.size());
		this.positions = new ArrayList<Integer>(source.positions.size()-predicate.size());
		for (int i=0; i<source.attributes.size(); i++) {
			boolean contains = false;
			for (Pair<Boolean, Integer> atom : predicate)
				if (atom.second().equals(i)) {
					contains = true;
					break;
				}
			if (!contains) {
				this.attributes.add(new String(source.attributes.get(i)));
				this.positions.add(Integer.valueOf(source.positions.get(i)));
			}
		}


		this.instances = new ArrayList<Instance>(source.instances.size());
	}

	/** Splits the dataset based on the values of the chosen attribute
	 * @param  predicate Disjunction (or a single atom) used to split the dataset
	 * @return Two datasets, first doesn't satisfy the predicate, second does
	 * */
	protected Pair<Dataset,Dataset> split(HashSet<Pair<Boolean,Integer>> predicate) {
		assert(predicate.size() > 0);
		for (Pair<Boolean,Integer> atom : predicate)
			assert(atom.second() >= 0 && atom.second() < attributes.size());
		assert(attributes.size() == positions.size());

		Dataset unsat = new Dataset(this, predicate);
		Dataset sat = new Dataset(this, predicate);

		for (int i=0; i<instances.size(); i++) {
			boolean satisfies = false;
			for (Pair<Boolean,Integer> atom : predicate)
				if (atom.first().equals(instances.get(i).attValues.get(atom.second()))) {
					satisfies = true;
					break;
				}

			if (!satisfies) {
				unsat.instances.add(new Instance(instances.get(i), predicate));
				if (instances.get(i).classValue)
					unsat.numYES++;
				else
					unsat.numNO++;
			} else {
				sat.instances.add(new Instance(instances.get(i), predicate));
				if (instances.get(i).classValue)
					sat.numYES++;
				else
					sat.numNO++;
			}
		}

		unsat.instances.trimToSize();
		sat.instances.trimToSize();

		return new Pair<Dataset,Dataset>(unsat, sat);
	}

	/**
	 * Computes which predicate is best for splitting the dataset
	 * @param allowdisjunction	False - only atoms as predicates, True - disjunctions allowed
	 * @return
	 */
	protected HashSet<Pair<Boolean,Integer>> bestInfoGain(boolean allowdisjunction) {
		assert(instances.size() == numYES + numNO);
		assert(numYES > 0 && numNO > 0);
		assert(attributes.size() >= 1);
		if (attributes.size() == 1) {
			HashSet<Pair<Boolean,Integer>> result = new HashSet<Pair<Boolean,Integer>>();
			result.add(new Pair<Boolean,Integer>(true,0));
			return result;
		}

		TreeMap<Integer,int[]> numYESatt = new TreeMap<Integer,int[]>();
		TreeMap<Integer,int[]> numNOatt = new TreeMap<Integer,int[]>();
		TreeMap<Integer,int[]> numTOTatt = new TreeMap<Integer,int[]>();

		for (int i=0; i<attributes.size(); i++) {
			numYESatt.put(i, new int[2]);
			numNOatt.put(i, new int[2]);
			numTOTatt.put(i, new int[2]);
			for (int j=0; j<2; j++) {
				numYESatt.get(i)[j] = 0;
				numNOatt.get(i)[j] = 0;
			}
		}

		for (Instance instance : instances) {
			if (instance.classValue) {
				for (int i=0; i<attributes.size(); i++)
					numYESatt.get(i)[(instance.attValues.get(i)?1:0)]++;
			} else {
				for (int i=0; i<attributes.size(); i++)
					numNOatt.get(i)[(instance.attValues.get(i)?1:0)]++;
			}
		}

		int numTOT = numYES + numNO;
		for (int i=0; i<attributes.size(); i++)
			for (int j=0; j<2; j++)
				numTOTatt.get(i)[j] = numYESatt.get(i)[j] + numNOatt.get(i)[j];

		TreeMap<Integer,float[]> pAtt = new TreeMap<Integer,float[]>();
		TreeMap<Integer,float[]> pYESgAtt = new TreeMap<Integer,float[]>();
		TreeMap<Integer,float[]> pNOgAtt = new TreeMap<Integer,float[]>();
		float pYES = ((float) numYES) / numTOT;
		float pNO = ((float) numNO) / numTOT;

		for (int i=0; i<attributes.size(); i++) {
			pAtt.put(i, new float[2]);
			pYESgAtt.put(i, new float[2]);
			pNOgAtt.put(i, new float[2]);
			for (int j=0; j<2; j++) {
				pAtt.get(i)[j] = ((float) numTOTatt.get(i)[j]) / numTOT;
				if (numTOTatt.get(i)[j] == 0) {
					pYESgAtt.get(i)[j] = 0;
					pNOgAtt.get(i)[j] = 0;
				} else {
					pYESgAtt.get(i)[j] = ((float) numYESatt.get(i)[j]) / numTOTatt.get(i)[j];
					pNOgAtt.get(i)[j] = ((float) numNOatt.get(i)[j]) / numTOTatt.get(i)[j];
				}
			}
		}

		float best = (float) 0.001;
		HashSet<Pair<Boolean,Integer>> bestpred = null;

		float originalentropy = 0;
		assert(pYES > 0 && pNO > 0);
		originalentropy -= ( pYES * (Math.log(pYES) / Math.log(2)) );
		originalentropy -= ( pNO  * (Math.log(pNO)  / Math.log(2)) );

		for (int i=0; i<attributes.size(); i++) {
			float current = originalentropy;
			for (int j=0; j<2; j++) {
				float conditionedentropy = 0;
				if (pYESgAtt.get(i)[j] > 0)
					conditionedentropy -= ( pYESgAtt.get(i)[j] * (Math.log(pYESgAtt.get(i)[j]) / Math.log(2)) );
				if (pNOgAtt.get(i)[j] > 0)
					conditionedentropy -= ( pNOgAtt.get(i)[j]  * (Math.log(pNOgAtt.get(i)[j])  / Math.log(2)) );
				current -= pAtt.get(i)[j] * conditionedentropy;
			}
			if (current > best) {
				best = current;
				bestpred = new HashSet<Pair<Boolean,Integer>>();
				bestpred.add(new Pair<Boolean,Integer>(true, i));
			}
		}


		// Take two special disjunction into consideration

		if (allowdisjunction) {
			TreeSet<Integer> zeroOnlyYes = new TreeSet<Integer>();
			TreeSet<Integer> zeroOnlyNo = new TreeSet<Integer>();
			TreeSet<Integer> oneOnlyYes = new TreeSet<Integer>();
			TreeSet<Integer> oneOnlyNo = new TreeSet<Integer>();

			for (int i=0; i<attributes.size(); i++) {
				if (numYESatt.get(i)[0] == numTOTatt.get(i)[0] && numTOTatt.get(i)[0] > 0) {
					assert(numNOatt.get(i)[0] == 0);
					assert(numNOatt.get(i)[1] > 0);
					zeroOnlyYes.add(i);
				}
				if (numNOatt.get(i)[0] == numTOTatt.get(i)[0] && numTOTatt.get(i)[0] > 0) {
					assert(numYESatt.get(i)[0] == 0);
					assert(numYESatt.get(i)[1] > 0);
					zeroOnlyNo.add(i);
				}
				if (numYESatt.get(i)[1] == numTOTatt.get(i)[1] && numTOTatt.get(i)[1] > 0) {
					assert(numNOatt.get(i)[1] == 0);
					assert(numNOatt.get(i)[0] > 0);
					oneOnlyYes.add(i);
				}
				if (numNOatt.get(i)[1] == numTOTatt.get(i)[1] && numTOTatt.get(i)[1] > 0) {
					assert(numYESatt.get(i)[1] == 0);
					assert(numYESatt.get(i)[0] > 0);
					oneOnlyNo.add(i);
				}
			}

			if (zeroOnlyYes.size() + oneOnlyYes.size() <= 1 &&
					zeroOnlyNo.size() + oneOnlyNo.size() <= 1)
				return bestpred;

			HashSet<Pair<Integer,Integer>> couldbesame = new HashSet<Pair<Integer,Integer>>();
			for (int at1 : zeroOnlyYes) for (int at2 : zeroOnlyYes) if (at1 < at2)
				couldbesame.add(new Pair<Integer,Integer>(at1,at2));
			for (int at1 : zeroOnlyNo) for (int at2 : zeroOnlyNo) if (at1 < at2)
				couldbesame.add(new Pair<Integer,Integer>(at1,at2));
			for (int at1 : oneOnlyYes) for (int at2 : oneOnlyYes) if (at1 < at2)
				couldbesame.add(new Pair<Integer,Integer>(at1,at2));
			for (int at1 : oneOnlyNo) for (int at2 : oneOnlyNo) if (at1 < at2)
				couldbesame.add(new Pair<Integer,Integer>(at1,at2));

			for (Instance instance : instances) {
				if (couldbesame.isEmpty()) break;
				for (Iterator<Pair<Integer,Integer>> i = couldbesame.iterator(); i.hasNext();) {
					Pair<Integer,Integer> atom = i.next();
				    if (!instance.attValues.get(atom.first()).equals(instance.attValues.get(atom.second()))) {
				        i.remove();
				    }
				}
			}

			for (Pair<Integer,Integer> atom : couldbesame) {
				zeroOnlyYes.remove(atom.second());
				zeroOnlyNo.remove(atom.second());
				oneOnlyYes.remove(atom.second());
				oneOnlyNo.remove(atom.second());
			}

			if (zeroOnlyYes.size() + oneOnlyYes.size() <= 1 &&
					zeroOnlyNo.size() + oneOnlyNo.size() <= 1)
				return bestpred;

			// Disjunction: satisfying it leads to NO
			int numTOTunsatInNODISJ = 0;
			int numYESunsatInNODISJ = 0;
			int numNOunsatInNODISJ = 0;

			// Disjunction: satisfying it leads to YES
			int numTOTunsatInYESDISJ = 0;
			int numYESunsatInYESDISJ = 0;
			int numNOunsatInYESDISJ = 0;


			for (Instance instance : instances) {
				boolean unsatNODISJ = true;
				boolean unsatYESDISJ = true;

				for (int i=0; i<attributes.size(); i++)
					if (!instance.attValues.get(i)) {
						// attribute value is 0
						if (unsatYESDISJ && zeroOnlyYes.contains(i)) {
							unsatYESDISJ = false;
							assert(instance.classValue);
						}
						if (unsatNODISJ && zeroOnlyNo.contains(i)) {
							unsatNODISJ = false;
							assert(!instance.classValue);
						}
					} else {
						// attribute value is 1
						if (unsatYESDISJ && oneOnlyYes.contains(i)) {
							unsatYESDISJ = false;
							assert(instance.classValue);
						}
						if (unsatNODISJ && oneOnlyNo.contains(i)) {
							unsatNODISJ = false;
							assert(!instance.classValue);
						}
					}

				// note: both unsat-s can be true
				if (unsatNODISJ) {
					numTOTunsatInNODISJ++;
					if (instance.classValue)
						numYESunsatInNODISJ++;
					else
						numNOunsatInNODISJ++;
				}

				if (unsatYESDISJ) {
					numTOTunsatInYESDISJ++;
					if (instance.classValue)
						numYESunsatInYESDISJ++;
					else
						numNOunsatInYESDISJ++;
				}
			}

			float pUnsatOfNODISJ = ((float) numTOTunsatInNODISJ) / numTOT;
			float pUnsatOfYESDISJ = ((float) numTOTunsatInYESDISJ) / numTOT;

			float pYESwhenUnsatOfNODISJ = ((float) numYESunsatInNODISJ) / numTOTunsatInNODISJ;
			float pNOwhenUnsatOfNODISJ = ((float) numNOunsatInNODISJ) / numTOTunsatInNODISJ;

			float pYESwhenUnsatOfYESDISJ = ((float) numYESunsatInYESDISJ) / numTOTunsatInYESDISJ;
			float pNOwhenUnsatOfYESDISJ = ((float) numNOunsatInYESDISJ) / numTOTunsatInYESDISJ;


			// Compute Information gain when using the NO disjunction
			// In sat-part everything is NO so the entropy of
			// this part is 0, therefore the information gain
			// will be H(original) - p(unsat) * H(unsat-part)
			float current = originalentropy;
			float conditionedentropy = 0;
			if (pYESwhenUnsatOfNODISJ > 0)
				conditionedentropy -= ( pYESwhenUnsatOfNODISJ * (Math.log(pYESwhenUnsatOfNODISJ) / Math.log(2)) );
			if (pNOwhenUnsatOfNODISJ > 0)
				conditionedentropy -= ( pNOwhenUnsatOfNODISJ  * (Math.log(pNOwhenUnsatOfNODISJ)  / Math.log(2)) );
			current -= pUnsatOfNODISJ * conditionedentropy;

			if (current > best) {
				best = current;
				bestpred = new HashSet<Pair<Boolean,Integer>>();
				for (Integer key : zeroOnlyNo)
					bestpred.add(new Pair<Boolean,Integer>(false, key));
				for (Integer key : oneOnlyNo)
					bestpred.add(new Pair<Boolean,Integer>(true, key));
			}

			// Compute Information gain when using the YES disjunction
			// Again, H(original) - p(unsat) * H(unsat-part)
			current = originalentropy;
			conditionedentropy = 0;
			if (pYESwhenUnsatOfYESDISJ > 0)
				conditionedentropy -= ( pYESwhenUnsatOfYESDISJ * (Math.log(pYESwhenUnsatOfYESDISJ) / Math.log(2)) );
			if (pNOwhenUnsatOfYESDISJ > 0)
				conditionedentropy -= ( pNOwhenUnsatOfYESDISJ  * (Math.log(pNOwhenUnsatOfYESDISJ)  / Math.log(2)) );
			current -= pUnsatOfYESDISJ * conditionedentropy;

			if (current > best) {
				best = current;
				bestpred = new HashSet<Pair<Boolean,Integer>>();
				for (Integer key : zeroOnlyYes)
					bestpred.add(new Pair<Boolean,Integer>(false, key));
				for (Integer key : oneOnlyYes)
					bestpred.add(new Pair<Boolean,Integer>(true, key));
			}
		}

		return bestpred;
	}

	/**
	 * Computes which attribute is the best for <br>
	 * splitting the dataset, uses the Lookahead approach
	 * @return The index of attribute which is best for splitting, <br>
	 * 		   or (-1) if there is no attribute good for splitting
	 */
	protected int bestInfoGainLA() {
		assert(instances.size() == numYES + numNO);
		assert(numYES > 0 && numNO > 0);
		assert(attributes.size() >= 2);
		if (instances.size() < 4) return -1;


		int numAtt = attributes.size();
		TreeMap<Integer,int[][]> n2attT = new TreeMap<Integer,int[][]>();
		TreeMap<Integer,int[][]> n2attY = new TreeMap<Integer,int[][]>();
		TreeMap<Integer,int[][]> n2attN = new TreeMap<Integer,int[][]>();
		for (int i=0; i<numAtt-1; i++)
			for (int j=i; j<numAtt; j++) {
				n2attT.put(i*numAtt+j, new int[2][2]);
				n2attY.put(i*numAtt+j, new int[2][2]);
				n2attN.put(i*numAtt+j, new int[2][2]);
				for (int k=0; k<2; k++) // values of i
					for (int l=0; l<2; l++) { // values of j
						n2attY.get(i*numAtt+j)[k][l] = 0;
						n2attN.get(i*numAtt+j)[k][l] = 0;
					}
			}

		TreeMap<Integer,int[]> n1attT = new TreeMap<Integer,int[]>();
		TreeMap<Integer,int[]> n1attY = new TreeMap<Integer,int[]>();
		TreeMap<Integer,int[]> n1attN = new TreeMap<Integer,int[]>();
		for (int i=0; i<numAtt; i++) {
			n1attT.put(i, new int[2]);
			n1attY.put(i, new int[2]);
			n1attN.put(i, new int[2]);
			for (int j=0; j<2; j++) {
				n1attY.get(i)[j] = 0;
				n1attN.get(i)[j] = 0;
			}
		}

		for (Instance instance : instances) {
			if (instance.classValue) {
				for (int i=0; i<numAtt-1; i++) {
					for (int j=i; j<numAtt; j++)
						n2attY.get(i*numAtt+j)[instance.attValues.get(i)?1:0][instance.attValues.get(j)?1:0]++;
					n1attY.get(i)[instance.attValues.get(i)?1:0]++;
				}
				n1attY.get(numAtt-1)[instance.attValues.get(numAtt-1)?1:0]++;
			} else {
				for (int i=0; i<numAtt-1; i++) {
					for (int j=i; j<numAtt; j++)
						n2attN.get(i*numAtt+j)[instance.attValues.get(i)?1:0][instance.attValues.get(j)?1:0]++;
					n1attN.get(i)[instance.attValues.get(i)?1:0]++;
				}
				n1attN.get(numAtt-1)[instance.attValues.get(numAtt-1)?1:0]++;
			}
		}

		int numTOT = numYES + numNO;
		for (int i=0; i<numAtt; i++)
			for (int j=0; j<2; j++)
				n1attT.get(i)[j] = n1attY.get(i)[j] + n1attN.get(i)[j];
		for (int i=0; i<numAtt-1; i++)
			for (int j=i; j<numAtt; j++)
				for (int k=0; k<2; k++) // values of i
					for (int l=0; l<2; l++) // values of j
						n2attT.get(i*numAtt+j)[k][l] =
						n2attY.get(i*numAtt+j)[k][l] + n2attN.get(i*numAtt+j)[k][l];

		// We have all the necessary numbers from the dataset

		float pY = ((float) numYES) / numTOT;
		float pN = ((float) numNO) / numTOT;
		float h0att = 0;
		if (pY > 0) h0att -= ( pY * (Math.log(pY) / Math.log(2)) );
		if (pN > 0) h0att -= ( pN * (Math.log(pN) / Math.log(2)) );

		float best = (float) 0.001;
		int bestpoz = -1;
		float bestpozowninfogain = (float) 0.001;

		for (int a1=0; a1<numAtt; a1++) {

		// optAtt: a1values -> attributes
		// optAtt( a1v ) = a2
		// where a2 is optimal for splitting dataset_a1v
		ArrayList<Integer> optAtt = new ArrayList<Integer>(2); // values of a1
		for (int a1v=0; a1v<2; a1v++) { // values of a1

			pY = n1attT.get(a1)[a1v] == 0?0:
				((float) n1attY.get(a1)[a1v]) / n1attT.get(a1)[a1v];
			pN = n1attT.get(a1)[a1v] == 0?0:
				((float) n1attN.get(a1)[a1v]) / n1attT.get(a1)[a1v];
			float h1att = 0;
			if (pY > 0) h1att -= ( pY * (Math.log(pY) / Math.log(2)) );
			if (pN > 0) h1att -= ( pN * (Math.log(pN) / Math.log(2)) );

			float best1 = (float) 0.001;
			int bestpoz1 = -1;
			for (int a2=0; a2<numAtt; a2++)
			if (a2 != a1) {
				float current1 = h1att;
				for (int a2v=0; a2v<2; a2v++) { // values of a2
					if (a1<a2) {
						pY = n2attT.get(a1*numAtt+a2)[a1v][a2v] == 0?0:
							((float) n2attY.get(a1*numAtt+a2)[a1v][a2v]) / n2attT.get(a1*numAtt+a2)[a1v][a2v];
						pN = n2attT.get(a1*numAtt+a2)[a1v][a2v] == 0?0:
							((float) n2attN.get(a1*numAtt+a2)[a1v][a2v]) / n2attT.get(a1*numAtt+a2)[a1v][a2v];
					} else {
						pY = n2attT.get(a2*numAtt+a1)[a2v][a1v] == 0?0:
							((float) n2attY.get(a2*numAtt+a1)[a2v][a1v]) / n2attT.get(a2*numAtt+a1)[a2v][a1v];
						pN = n2attT.get(a2*numAtt+a1)[a2v][a1v] == 0?0:
							((float) n2attN.get(a2*numAtt+a1)[a2v][a1v]) / n2attT.get(a2*numAtt+a1)[a2v][a1v];
					}
					float h2att = 0;
					if (pY > 0) h2att -= ( pY * (Math.log(pY) / Math.log(2)) );
					if (pN > 0) h2att -= ( pN * (Math.log(pN) / Math.log(2)) );
					float pA2VgA1V = 0;
					if (n1attT.get(a1)[a1v] != 0) {
						if (a1<a2) pA2VgA1V = ((float) n2attT.get(a1*numAtt+a2)[a1v][a2v]) / n1attT.get(a1)[a1v];
						else	   pA2VgA1V = ((float) n2attT.get(a2*numAtt+a1)[a2v][a1v]) / n1attT.get(a1)[a1v];
					}
					current1 -= pA2VgA1V * h2att;
				}
				if (current1>best1) {
					best1 = current1; bestpoz1 = a2;
				}
			}

			optAtt.add(bestpoz1);
		}

		float current = h0att;
		// We have optAtt: a1values -> attributes
		// For each a1v and a2v    where a2 = optAtt( a1v )
		// Subtract p( a1v,a2v ) * entropy( dataset_a1v,a2v )
		for (int a1v=0; a1v<2; a1v++) { // values of a1
			int a2 = optAtt.get(a1v);
			if (a2 == -1) {
				pY = n1attT.get(a1)[a1v] == 0?0:
					((float) n1attY.get(a1)[a1v]) / n1attT.get(a1)[a1v];
				pN = n1attT.get(a1)[a1v] == 0?0:
					((float) n1attN.get(a1)[a1v]) / n1attT.get(a1)[a1v];
				float h2att = 0;
				if (pY > 0) h2att -= ( pY * (Math.log(pY) / Math.log(2)) );
				if (pN > 0) h2att -= ( pN * (Math.log(pN) / Math.log(2)) );
				float pA1V = ((float) n1attT.get(a1)[a1v]) / numTOT;
				current -= pA1V * h2att;
			}
			else
			for (int a2v=0; a2v<2; a2v++) { // values of a2
				if (a1<a2) {
					pY = n2attT.get(a1*numAtt+a2)[a1v][a2v] == 0?0:
						((float) n2attY.get(a1*numAtt+a2)[a1v][a2v]) / n2attT.get(a1*numAtt+a2)[a1v][a2v];
					pN = n2attT.get(a1*numAtt+a2)[a1v][a2v] == 0?0:
						((float) n2attN.get(a1*numAtt+a2)[a1v][a2v]) / n2attT.get(a1*numAtt+a2)[a1v][a2v];
				} else {
					pY = n2attT.get(a2*numAtt+a1)[a2v][a1v] == 0?0:
						((float) n2attY.get(a2*numAtt+a1)[a2v][a1v]) / n2attT.get(a2*numAtt+a1)[a2v][a1v];
					pN = n2attT.get(a2*numAtt+a1)[a2v][a1v] == 0?0:
						((float) n2attN.get(a2*numAtt+a1)[a2v][a1v]) / n2attT.get(a2*numAtt+a1)[a2v][a1v];
				}
				float h2att = 0;
				if (pY > 0) h2att -= ( pY * (Math.log(pY) / Math.log(2)) );
				if (pN > 0) h2att -= ( pN * (Math.log(pN) / Math.log(2)) );
				float pA1VA2V = 0;
				if (a1<a2) pA1VA2V = ((float) n2attT.get(a1*numAtt+a2)[a1v][a2v]) / numTOT;
				else	   pA1VA2V = ((float) n2attT.get(a2*numAtt+a1)[a2v][a1v]) / numTOT;
				current -= pA1VA2V * h2att;
			}
		}

		if (current > best) {
			best = current; bestpoz = a1;
			bestpozowninfogain = h0att;
			for (int a1v=0; a1v<2; a1v++) { // values of a1
				pY = n1attT.get(a1)[a1v] == 0?0:
					((float) n1attY.get(a1)[a1v]) / n1attT.get(a1)[a1v];
				pN = n1attT.get(a1)[a1v] == 0?0:
					((float) n1attN.get(a1)[a1v]) / n1attT.get(a1)[a1v];
				float h2att = 0;
				if (pY > 0) h2att -= ( pY * (Math.log(pY) / Math.log(2)) );
				if (pN > 0) h2att -= ( pN * (Math.log(pN) / Math.log(2)) );
				float pA1V = ((float) n1attT.get(a1)[a1v]) / numTOT;
				bestpozowninfogain -= pA1V * h2att;
			}
		} else
		if (current - best + 0.00001 > 0 && current - best - 0.00001 < 0) { // current == best
			float a1owninfogain = h0att;
			for (int a1v=0; a1v<2; a1v++) { // values of a1
				pY = n1attT.get(a1)[a1v] == 0?0:
					((float) n1attY.get(a1)[a1v]) / n1attT.get(a1)[a1v];
				pN = n1attT.get(a1)[a1v] == 0?0:
					((float) n1attN.get(a1)[a1v]) / n1attT.get(a1)[a1v];
				float h2att = 0;
				if (pY > 0) h2att -= ( pY * (Math.log(pY) / Math.log(2)) );
				if (pN > 0) h2att -= ( pN * (Math.log(pN) / Math.log(2)) );
				float pA1V = ((float) n1attT.get(a1)[a1v]) / numTOT;
				a1owninfogain -= pA1V * h2att;
			}
			if (a1owninfogain > bestpozowninfogain) {
				bestpoz = a1;
				bestpozowninfogain = a1owninfogain;
			}
		}

		}

		return bestpoz;
	}

	/**
	 * Computes which attribute is the best for <br>
	 * splitting the dataset based on a simple heuristic
	 * @return The index of attribute which is best for splitting, <br>
	 * 		   or (-1) if there is no attribute good for splitting
	 */
	protected int heuristicSplit() {
		assert(instances.size() == numYES + numNO);
		assert(numYES > 0 && numNO > 0);
		assert(attributes.size() >= 2);

		TreeMap<Integer,int[]> numYESatt = new TreeMap<Integer,int[]>();
		TreeMap<Integer,int[]> numNOatt = new TreeMap<Integer,int[]>();
		TreeMap<Integer,int[]> numTOTatt = new TreeMap<Integer,int[]>();

		for (int i=0; i<attributes.size(); i++) {
			numYESatt.put(i, new int[2]);
			numNOatt.put(i, new int[2]);
			numTOTatt.put(i, new int[2]);
			for (int j=0; j<2; j++) {
				numYESatt.get(i)[j] = 0;
				numNOatt.get(i)[j] = 0;
				numTOTatt.get(i)[j] = 0;
			}
		}

		for (Instance instance : instances) {
			for (int i=0; i<attributes.size(); i++)
				numTOTatt.get(i)[instance.attValues.get(i)?1:0]++;
			if (instance.classValue) {
				for (int i=0; i<attributes.size(); i++)
					numYESatt.get(i)[instance.attValues.get(i)?1:0]++;
			} else {
				for (int i=0; i<attributes.size(); i++)
					numNOatt.get(i)[instance.attValues.get(i)?1:0]++;
			}
		}

		int bestpoz = 0;
		double best = 0;
		if (numTOTatt.get(0)[0] > 0 && numTOTatt.get(0)[1] > 0) {
			double candidate = ((double) numYESatt.get(0)[0] / numTOTatt.get(0)[0]) +
					((double) numNOatt.get(0)[1] / numTOTatt.get(0)[1]);
			if (candidate > best)
				best = candidate;
			candidate = ((double) numNOatt.get(0)[0] / numTOTatt.get(0)[0]) +
					((double) numYESatt.get(0)[1] / numTOTatt.get(0)[1]);
			if (candidate > best)
				best = candidate;
		}

		for (int i=1; i<attributes.size(); i++)
			if (numTOTatt.get(i)[0] > 0 && numTOTatt.get(i)[1] > 0) {
				double candidate = ((double) numYESatt.get(i)[0] / numTOTatt.get(i)[0]) +
						((double) numNOatt.get(i)[1] / numTOTatt.get(i)[1]);
				if (candidate > best) {
					best = candidate;
					bestpoz = i;
				}
				candidate = ((double) numNOatt.get(i)[0] / numTOTatt.get(i)[0]) +
						((double) numYESatt.get(i)[1] / numTOTatt.get(i)[1]);
				if (candidate > best) {
					best = candidate;
					bestpoz = i;
				}
			}

		if (best == 0) return -1;

		return bestpoz;
	}

	/** Creates an arff file with the dataset in the Attribute-Relation File Format <br>
	 *  Location: results/datasets/filename.arff
	 * @param filename The name of the arff file
	 * @param game     Game instance
	 * @param estimate Estimate size of state space
	 * */
	public void arffFile(String filename, Game game, int estimate) {
		File directory = new File("results/datasets/");
		if (!directory.exists())
			directory.mkdirs();
		File outputFile = new File("results/datasets/"+filename+".arff");
		String nl = System.getProperty("line.separator");

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
        	writer.write("% AUTHOR:           Viktor Toman"+nl);
			writer.write("% FILE:             "+filename+nl);
        	writer.write("% STATES ESTIMATED: "+estimate+nl);
        	writer.write("% STATES GENERATED: "+game.stateSize+nl);
        	writer.write("% STATEVARP1:       "+game.varStateP1no()+nl);
        	writer.write("% STATEVARP2:       "+game.varStateP2no()+nl);
        	writer.write("% ACTIONVARP1:      "+game.varActionP1no()+nl);
        	writer.write("% ACTIONVARP2:      "+game.varActionP2no()+nl);
        	writer.write("% PLAYER:           "+player+nl);
        	writer.write("% OBJECTIVE:        "+objective+nl);
        	writer.write("% NUMYES:           "+numYES+nl);
        	writer.write("% NUMNO:            "+numNO+nl);
        	writer.write("% NUMTOT:           "+((int)(numNO+numYES))+nl);
        	writer.write("@RELATION strategy"+nl+nl);

        	for (int i=0; i<attributes.size(); i++) {
            	writer.write("@ATTRIBUTE \""+attributes.get(i)+"\" {0,1}"+nl);
        	}
        	writer.write("@ATTRIBUTE \"class\" {yes,no}"+nl+nl);

        	writer.write("@DATA"+nl);
        	for (int i=0; i<instances.size(); i++) {
        		for (int j=0; j<instances.get(i).attValues.size(); j++)
        			writer.write((instances.get(i).attValues.get(j)?1:0)+",");
        		writer.write((instances.get(i).classValue?"yes":"no")+nl);
        	}
        } catch (Exception e) {
        	System.out.println(e);
        }
	}

}
