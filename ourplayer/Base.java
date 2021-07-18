package ourplayer;

import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Base extends MyUnit {

    int workers = 0;

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        if (workers < 5){
            if (spawnRandom(UnitType.WORKER)) ++workers;
        }
    }

}
