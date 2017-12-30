package util;

import java.util.ArrayList;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Util {
	
	/** Returns 2 to the power of exp
	 * @param exp Exponent
	 * @return 2^exp
	 * */
	public static int bitpower(int exp) {
		int result = 1;
		int base = 2;
	    while (exp != 0)
	    {
	        if ((exp & 1) == 1)
	            result *= base;
	        exp >>= 1;
	        base *= base;
	    }
		return result;
	}
	
	/** Returns the decimal representation given a binary representation<br>
	 *  Binary:  b0 b1 b2 b3 ...<br>
	 *  Decimal: 1*b0 + 2*b1 + 4*b2 + 8*b3 ... 
	 * @param binary	Binary representation
	 * @return 			Decimal representation
	 * */
	public static int decimal(ArrayList<Boolean> binary) {
		int result = 0;
		int multiplier = 1;
		for (int i=0; i<binary.size(); i++) {
			if (binary.get(i)) result += multiplier;
			multiplier *= 2;
		}
		return result;
	}
	
	/** Returns the binary representation given a decimal representation<br>
	 *  Binary:  b0 b1 b2 b3 ...<br>
	 *  Decimal: 1*b0 + 2*b1 + 4*b2 + 8*b3 ...
	 * @param length 	Array Length
	 * @param decimal 	Decimal representation
	 * @return 			Binary representation
	 * */
	public static ArrayList<Boolean> binary(int length, int decimal) {
		ArrayList<Boolean> result = new ArrayList<Boolean>(length);
		int help = decimal;
		for (int i=0; i<length; i++) {
			result.add((help % 2)==1);
			help /= 2;
		}		
		return result;
	}
		
	/** Returns the binary representation of minus one (or zero if arg is zero)
	 * @param arg 	Argument in binary
	 * @return 		Argument-1 (or zero) in binary
	 * */	
	public static ArrayList<Boolean> binaryMinusOne(ArrayList<Boolean> arg) {
		int decimal = decimal(arg);
		if (decimal == 0) return arg;
		else return binary((byte) arg.size(), decimal-1);
	}
	
	public static int numberOfOnes(int decimal) {
		int length = 1;
		while (bitpower(length) <= decimal) length++;
		ArrayList<Boolean> binary = binary(length, decimal);
		int score = 0;
		for (int i=0; i<binary.size(); i++)
			if (binary.get(i)) score++;
		return score;
	}
	
}

