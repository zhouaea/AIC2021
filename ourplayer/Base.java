package ourplayer;

import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Base extends MyUnit {

    int workers = 0;

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        baseDecodeSmokeSignals();
        if (workers < 1){
            if (spawnRandom(UnitType.WORKER)) ++workers;
        }

        if(uc.getRound() == 100) {
            uc.println("base location:" + uc.getLocation());
            uc.killSelf();
        }
    }

    void baseDecodeSmokeSignals() {
        int[] smokeSignals = uc.readSmokeSignals();
        DecodedMessage decodedSmokeSignal;
        for (int smokeSignal : smokeSignals) {
            // Make sure message is from our team.
            if ((decodedSmokeSignal = decodeSmokeSignal(uc.getLocation(), smokeSignal)) != null) {
                uc.println(decodedSmokeSignal);
            }
        }
    }

}
