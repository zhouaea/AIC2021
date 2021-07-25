package ourplayer;

import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Axeman extends MyUnit {

  boolean knowsBase = false;
  boolean basePatrol = true;
  boolean atBase = true;
  boolean handlingDistress = false;
  Location base;
  Location target;

  Axeman(UnitController uc) {
    super(uc);
  }

  void playRound() {
    // checks if next to base
    this.nextToBaseCheck();

    // record base location
    if (!this.knowsBase) {
      this.rememberBaseLocation();
      this.knowsBase = true;
    }

    // if should be patrolling base and not at base, go to base
    if (this.basePatrol) {
      if (!this.atBase) {
        this.goToLocation(this.base);
        this.FollowPath();
      }
    }

    // if distress signal is sent
    if (this.uc.canReadSmokeSignals()) {
      int[] signals = this.uc.readSmokeSignals();
      for (int signal : signals) {
        // not implemented yet
        if (isDistressSignal(signal)) {
          this.handlingDistress = true;
          // determine target
          this.target = decodeSmokeSignal(this.uc.getLocation(), signal).location;
          this.goToLocation(this.target);
          this.FollowPath();
        }
      }
    }

    //        if (this.uc.getRound() > 50) {
    //            if (this.uc.getRound() < 70) {
    //                this.basePatrol = false;
    //                this.moveRandom();
    //            } else {
    //                this.basePatrol = true;
    //            }
    //        }
  }

  // determine if a signal is a distress call
  boolean isDistressSignal(int signal) {
    return true;
  }

  void rememberBaseLocation() {
    UnitInfo[] surroundingUnits = uc.senseUnits(this.uc.getInfo().getTeam());
    for (UnitInfo unit : surroundingUnits) {
      if (unit.getType() == UnitType.BASE) {
        this.base = unit.getLocation();
      }
    }
  }

  void nextToBaseCheck() {
    UnitInfo[] surroundingUnits = uc.senseUnits(this.uc.getInfo().getTeam());
    for (UnitInfo unit : surroundingUnits) {
      if (unit.getType() == UnitType.BASE) {
        this.atBase = this.uc.getInfo().getLocation().distanceSquared(this.base) < 1.5;
      }
    }
  }
}
