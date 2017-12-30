package machinelearning;

import java.util.ArrayList;
import java.util.HashSet;

import util.Pair;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Instance {
	
	public ArrayList<Boolean> attValues;
	public Boolean classValue;
	
	/** 
	 * Constructor which creates an instance from given values and yes/no information
	 * @param  attValues  Attribute values (state, action)
	 * @param  classValue TRUE - yes, FALSE - no
	 * */
	public Instance(Pair<ArrayList<Boolean>,ArrayList<Boolean>> attValues, Boolean classValue) {
		this.attValues = new ArrayList<Boolean>(attValues.first().size()+attValues.second().size());
		this.attValues.addAll(attValues.first());
		this.attValues.addAll(attValues.second());
		this.classValue = classValue;
	}
	
	/**
	 * Constructor which creates an instance from given values and yes/no information
	 * @param attValues	  Attribute values in one list
	 * @param classValue  TRUE - yes, FALSE - no
	 */
	public Instance(ArrayList<Boolean> attValues, Boolean classValue) {
		this.attValues = new ArrayList<Boolean>(attValues);
		this.classValue = classValue;
	}
	
	/** 
	 * Constructor which creates a copy of a given instance without the values concerned by the predicate
	 * @param  source  	 Source instance
	 * @param  predicate Disjunction (or a single atom) used to split the dataset
	 * */
	protected Instance(Instance source, HashSet<Pair<Boolean,Integer>> predicate) {
		assert(predicate.size() > 0);
		for (Pair<Boolean,Integer> atom : predicate)
			assert(atom.second() >= 0 && atom.second() < source.attValues.size());
		
		this.attValues = new ArrayList<Boolean>(source.attValues.size());
		for (int i=0; i<source.attValues.size(); i++) {
			boolean contains = false;
			for (Pair<Boolean, Integer> atom : predicate)
				if (atom.second().equals(i)) {
					contains = true;
					break;
				}
			if (!contains)
				this.attValues.add(new Boolean(source.attValues.get(i)));
		}
		this.attValues.trimToSize();
		this.classValue = source.classValue;
	}
	
}
