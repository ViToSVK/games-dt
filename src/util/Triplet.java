package util;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Triplet<A,B,C> {
	
    private final A first;
    private final B second;
    private final C third;
    
    public Triplet(A first, B second, C third) {
    	this.first = first;
    	this.second = second;
    	this.third = third;
    }
    
    public int hashCode() {
    	int hashFirst = first != null ? first.hashCode() : 0;
    	int hashSecond = second != null ? second.hashCode() : 0;
    	int hashThird = third != null ? third.hashCode() : 0;

    	return ((100 * hashThird) % 1000000) + ((10 * hashSecond) % 1000000) + (hashFirst % 1000000);
    }
    
    public boolean equals(Object other) {
    	if (other instanceof Triplet<?,?,?>) {
    		Triplet<?,?,?> otherTriplet = (Triplet<?,?,?>) other;
    		return 
    		((  this.first == otherTriplet.first ||
    			( this.first != null && otherTriplet.first != null &&
    			  this.first.equals(otherTriplet.first))) &&
    		 (	this.second == otherTriplet.second ||
    			( this.second != null && otherTriplet.second != null &&
    			  this.second.equals(otherTriplet.second))) &&
    		 (	this.third == otherTriplet.third ||
    			( this.third != null && otherTriplet.third != null &&
    			  this.third.equals(otherTriplet.third))));
    	}
    	return false;
    }
    
    public A first() { return first; }
    
    public B second() { return second; }
    
    public C third() { return third; }
    
}
