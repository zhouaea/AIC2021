package ourplayer;

import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Base extends MyUnit {

    int explorers = 0;

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        if (explorers < 1){
            if (spawnRandom(UnitType.EXPLORER)) ++explorers;
        }
    }

}
