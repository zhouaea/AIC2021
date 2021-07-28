package ourplayer;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    boolean travelledToEnemyBase = false;

    void playRound() {
        if (!travelledToEnemyBase) {
            if (bug2(uc.getLocation(), enemyBase))
                travelledToEnemyBase = true;
        }
        else {
            if (bug2(uc.getLocation(), teamBase))
                return;
        }
        uc.println(uc.getEnergyLeft());
    }
}
