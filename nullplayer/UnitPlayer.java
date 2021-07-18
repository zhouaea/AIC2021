package nullplayer;

import aic2021.user.*;

public class UnitPlayer {

	public void run(UnitController uc) {
		// Code to be executed only at the beginning of the unit's lifespan

		while (true) {
			// Code to be executed every round
			uc.yield(); // End of turn
		}
	}
}
