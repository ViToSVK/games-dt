package util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Tarjan {
	
	public static TreeSet<Integer> marked;
	public static TreeMap<Integer, Integer> id;
	public static TreeMap<Integer, Integer> lowlink;
	public static int pre;
	public static int count;
	public static LinkedList<Integer> stack;
	
	public static TreeMap<Integer, ArrayList<Integer>> recursive(TreeMap<Integer, TreeSet<Integer>> E) {
	    marked = new TreeSet<Integer>();
	    id = new TreeMap<Integer, Integer>();
	    lowlink = new TreeMap<Integer, Integer>();
	    pre = 0;
	    count = 0;
	    stack = new LinkedList<Integer>();
	    
	    for (Integer v : E.keySet())
	    	if (!marked.contains(v))
	    		dfs(E, v);
		
	    assert(stack.isEmpty());
	    
	    TreeMap<Integer, ArrayList<Integer>> result = new TreeMap<Integer, ArrayList<Integer>>();
		for (int i=0; i<count; i++)
			result.put(i, new ArrayList<Integer>());
		
		for (Integer v : E.keySet()) {
			int xx = id.get(v);
			ArrayList<Integer> xxx = result.get(xx);
			xxx.add(v);
		}
			//result.get(id.get(v)).add(v);
		
		return result;
	}
	
	private static void dfs(TreeMap<Integer, TreeSet<Integer>> E, int v) {
		marked.add(v);
		assert(!lowlink.containsKey(v));
		lowlink.put(v, pre);
		pre++;
        int min = lowlink.get(v);
        stack.push(v);
        
        if (E.containsKey(v))
        	for (Integer w : E.get(v)) {
                if (!marked.contains(w))
                	dfs(E, w);
                if (lowlink.get(w) < min)
                	min = lowlink.get(w);
            }
        
        if (min < lowlink.get(v)) {
            lowlink.put(v, min);
            return;
        }
        
        int w;
        do {
            w = stack.pop();
            assert(!id.containsKey(w));
            id.put(w, count);
            lowlink.put(w, E.keySet().size());
        } while (w != v);
        count++;
		
	}
}
