package ourplayer;

import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Base extends MyUnit {

    int workers = 0;

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        decodeSmokeSignals();
        if (workers < 1){
            if (spawnRandom(UnitType.WORKER)) ++workers;
        }

        if(uc.getRound() == 100) {
            uc.println("base location:" + uc.getLocation());
            uc.killSelf();
        }
    }

    void decodeSmokeSignals() {
        int[] smokeSignals = uc.readSmokeSignals();
        for (int smokeSignal : smokeSignals) {
            uc.println(decodeSmokeSignal(uc.getLocation(), smokeSignal));
        }
    }

}
