package ourplayer;

import aic2021.user.*;
import sun.awt.UNIXToolkit;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    int farms = 0;
    int sawmills = 0;
    int quarries = 0;

    Team team = uc.getTeam();

    void playRound(){
        moveRandom();
        if (uc.hasResearched(Technology.JOBS, team)) {
            if (farms <= 7) {
                if (spawnRandom(UnitType.FARM)) farms++;
            }

            if (sawmills <= 7) {
                if (spawnRandom(UnitType.SAWMILL)) sawmills++;
            }

            if (quarries <= 7) {
                if (spawnRandom(UnitType.QUARRY)) quarries++;
            }
        }
    }

}
