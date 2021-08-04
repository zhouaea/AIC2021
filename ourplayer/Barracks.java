package ourplayer;

import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Barracks extends MyUnit {

    int axemen = 0;
    int spearmen = 0;

    int maxTroops = -1;

    int maxAxemen = 2;
    int maxSpearmen = 2;

    Barracks(UnitController uc) {
        super(uc);
    }

    void playRound() {

        // read and record smoke signals
        if (this.uc.canReadSmokeSignals()) {
            int[] signals = this.uc.readSmokeSignals();
            for (int signal : signals) {
                if (signal % 719 == 0) {
                    this.maxTroops = signal / 719;
                    this.calculateMaxTroops();
                }
            }
            this.uc.println(
                    "Base received enemy army size signal. Size: " + this.maxTroops);
        }

        if (this.axemen < this.maxAxemen) {
            if (spawnRandom(UnitType.AXEMAN))
                this.axemen++;
        }

        if (this.spearmen < this.maxSpearmen) {
            if (spawnRandom(UnitType.SPEARMAN))
                this.spearmen++;
        }


//    if (this.axemen < 10) {
//      if (this.uc.canSpawn(UnitType.AXEMAN, Direction.NORTH)) {
//        this.uc.spawn(UnitType.AXEMAN, Direction.NORTH);
//        this.uc.println("spawned axeman north");
//        this.axemen++;
////        return;
//      } else {
//        this.uc.println("can't spawn axeman north");
//      }
//      if (this.uc.canSpawn(UnitType.AXEMAN, Direction.SOUTH)) {
//        this.uc.spawn(UnitType.AXEMAN, Direction.SOUTH);
//        this.uc.println("spawned axeman south");
//        this.axemen++;
////        return;
//      } else {
//        this.uc.println("can't spawn axeman south");
//      }
//    }
//
//    if (this.spearmen < 10) {
//      if (this.uc.canSpawn(UnitType.SPEARMAN, Direction.NORTH)) {
//        this.uc.spawn(UnitType.SPEARMAN, Direction.NORTH);
//        this.uc.println("spawned spearman north");
//        this.spearmen++;
////        return;
//      } else {
//        this.uc.println("can't spawn spearman north");
//      }
//      if (this.uc.canSpawn(UnitType.SPEARMAN, Direction.SOUTH)) {
//        this.uc.spawn(UnitType.SPEARMAN, Direction.SOUTH);
//        this.uc.println("spawned spearman south");
//        this.spearmen++;
////        return;
//      } else {
//        this.uc.println("can't spawn spearman south");
//      }
//    }
    }

    void calculateMaxTroops() {
        int tempAxemen = (this.maxTroops * 2 / 3);
        int tempSpearmen = this.maxTroops - tempAxemen;
        if (tempAxemen > this.maxAxemen) {
            this.maxAxemen = tempAxemen;
        }
        if (tempSpearmen > this.maxSpearmen) {
            this.maxSpearmen = tempSpearmen;
        }
    }
}