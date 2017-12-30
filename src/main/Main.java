package main;

/**
 * @author vtoman - Viktor Toman
 * viktor.toman@ist.ac.at
 *
 */
public class Main {
	
	public static void main(String[] args) {
		if (args.length != 1) {
			Routine.message();
			return;
		}
		
		switch (args[0]) {
			case "aTOTAL" :
				Routine.aiger();
				Routine.Raiger();
				break;
			case "wTOTAL" :
				Routine.wash(2);
				Routine.wash(3);
				Routine.wash(4);
				Routine.Rwash(0);
				Routine.Rwash(2);
				Routine.Rwash(3);
				Routine.Rwash(4);
				break;
			case "rTOTAL" :
				Routine.rabinizer(false);
				Routine.rabinizer(true);
				break;
			case "a"  	:
				Routine.aiger();
				break;
			case "w2"	:
				Routine.wash(2);
				break;
			case "w3"	:
				Routine.wash(3);
				break;			
			case "w4"	:
				Routine.wash(4);
				break;
			case "Ra"	:
				Routine.Raiger();
				break;
			case "Rw0"	:
				Routine.Rwash(0);
				break;
			case "Rw2"	:
				Routine.Rwash(2);
				break;
			case "Rw3"	:
				Routine.Rwash(3);
				break;
			case "Rw4"	:
				Routine.Rwash(4);
				break;
			case "rabN"	:
				Routine.rabinizer(false);
				break;
			case "rabE"	:
				Routine.rabinizer(true);
				break;
			default		:	Routine.message();
		}
			
	}

}
