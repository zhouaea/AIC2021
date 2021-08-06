package demoplayer;

import aic2021.user.*;

public class Base extends MyUnit {

    int workers = 0;

    int maxWorkers = 10;

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        if (this.workers < this.maxWorkers){
            if (spawnRandom(UnitType.WORKER)) ++this.workers;
        }

        this.playDefense();
    }

    void playDefense() {
        // Sense enemy units in attack radius and shoot them (add prioritization algorithm later).
        UnitInfo[] shootable_enemies = uc.senseUnits(18, this.uc.getOpponent());
        for (UnitInfo enemy : shootable_enemies) {
            if (uc.canAttack()) {
                uc.attack(enemy.getLocation());
            }
        }
    }
}
