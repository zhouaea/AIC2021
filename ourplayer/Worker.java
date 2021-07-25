package ourplayer;

import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Worker extends MyUnit {

  Worker(UnitController uc) {
    super(uc);
  }

  //    boolean torchLighted = false;
  //    boolean smoke = false;

  boolean knowsBase = false;
  boolean basePatrol = true;
  boolean atBase = true;
  Location base;

  void playRound() {
    //        UnitInfo myInfo = uc.getInfo();
    //        if (uc.getRound() > 300 + myInfo.getID()%200 && !smoke){
    //            if (uc.canMakeSmokeSignal()){
    //                uc.makeSmokeSignal(0);
    //                smoke = true;
    //            }
    //        }
    //        moveRandom();
    //        if (!torchLighted && myInfo.getTorchRounds() <= 0){
    //            lightTorch();
    //        }
    //        myInfo = uc.getInfo();
    //        if (myInfo.getTorchRounds() < 70){
    //            randomThrow();
    //        }
    //        int[] signals = uc.readSmokeSignals();
    //        if (signals.length > 0){
    //            uc.drawPointDebug(uc.getLocation(), 0, 0, 0);
    //        }

    if (this.uc.canReadSmokeSignals()) {
      int[] signals = this.uc.readSmokeSignals();
      if (signals.length > 0) {
        this.uc.println("Received signals: " + signals[0]);
      }
    }

    //        // record base location
    //        if (!this.knowsBase) {
    //            this.rememberBaseLocation();
    //        }
    //
    //        // if should be patrolling base and not at base, go to base
    //        if (this.basePatrol) {
    //            if (!this.atBase) {
    //                this.goToLocation(this.base);
    //                this.FollowPath();
    ////                this.uc.println("going back to base");
    //            }
    //        } else {
    //            this.goToLocation(new Location(320, 333));
    //            // 315,323 -> 311,320
    //            this.FollowPath();
    //        }
    //
    //        if (this.uc.getRound() > 20) {
    //            if (this.uc.getRound() < 70) {
    //                this.basePatrol = false;
    //            } else {
    //                this.basePatrol = true;
    //            }
    //        }
    //
    //        this.nextToBaseCheck();
  }

  void rememberBaseLocation() {
    UnitInfo[] surroundingUnits = uc.senseUnits(this.uc.getInfo().getTeam());
    for (UnitInfo unit : surroundingUnits) {
      if (unit.getType() == UnitType.BASE) {
        this.base = unit.getLocation();
        this.knowsBase = true;
        this.uc.println("Base remembered");
      }
    }
  }

  void nextToBaseCheck() {
    UnitInfo[] surroundingUnits = uc.senseUnits(this.uc.getInfo().getTeam());
    for (UnitInfo unit : surroundingUnits) {
      if (unit.getType() == UnitType.BASE) {
        this.atBase = this.uc.getInfo().getLocation().distanceSquared(this.base) < 1.5;
        if (this.atBase) {
          this.uc.println("At base");
        }
      }
    }
  }
}
