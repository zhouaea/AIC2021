package ourplayer;

import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Base extends MyUnit {

    int explorers = 0;
    int workers = 0;
    int axemen = 0;

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        if (explorers < 1){
            if (spawnRandom(UnitType.EXPLORER)) ++explorers;
        }
        if (workers < 1){
            if (spawnRandom(UnitType.WORKER)) ++workers;
        }
        if (axemen < 1){
            if (spawnRandom(UnitType.AXEMAN)) ++axemen;
        }

        if (this.uc.canReadSmokeSignals()) {
            int[] signals = this.uc.readSmokeSignals();
            if (signals.length > 0) {
                this.uc.println("Base received " + signals.length + " signals: First signal is: " + signals[0]);
            }
        }
    }

}
