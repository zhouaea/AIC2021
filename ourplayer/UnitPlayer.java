package ourplayer;

import aic2021.user.Direction;
import aic2021.user.UnitController;
import aic2021.user.UnitType;
import ourplayer.Base;
import ourplayer.Explorer;
import ourplayer.MyUnit;

public class UnitPlayer {

	public void run(UnitController uc) {
		// Code to be executed only at the beginning of the unit's lifespan

		//    while (true) {
		//      // Code to be executed every round
		//      uc.yield(); // End of turn
		//    }

		UnitType t = uc.getType();
		MyUnit u;

		if (t == UnitType.BASE) {
			u = new Base(uc);
		} else {
			u = new Explorer(uc);
			u.currentDir = Direction.NORTH;
		}
		u.roundSpawned = uc.getRound();

		while (true) {
			u.playRound();
			uc.yield();
		}
	}
}

