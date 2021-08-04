package demoplayer;

import aic2021.user.*;

public class Base extends MyUnit {

    int workers = 0;

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        if (workers < 10){
            if (spawnRandom(UnitType.WORKER)) ++workers;
        }
    }

}
