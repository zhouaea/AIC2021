package ourplayer;

import aic2021.user.Location;
import aic2021.user.Team;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Axeman extends MyUnit {

  Axeman(UnitController uc) {
    super(uc);
  }

  Team team = this.uc.getTeam();
  Team enemyTeam = this.uc.getOpponent();
  Team deerTeam = Team.NEUTRAL;

  Location baseLocation = null;

  UnitInfo target = null;
  Location currentTargetLocation;

  int targetID;

  boolean fighting = false;
  boolean targetLocked = false;

  void playRound() {
    // remember base location
    if (this.baseLocation == null) {
      this.rememberBaseLocation();
    }

    this.keepTorchLit();

    this.senseEnemies();
    if (fighting) {
      this.attack();
    }
    this.moveRandom();

    //    // find target
    //    if (this.target == null) {
    //      for (UnitInfo target : this.uc.senseUnits(this.ourTeam.getOpponent())) {
    //        this.target = target;
    //        this.targetID = this.target.getID();
    //        break;
    //      }
    //    }
    //
    //    // attack
    //    this.attack();
    //
    //    // light torch
    //    this.keepTorchLit();
  }

  void rememberBaseLocation() {
    UnitInfo[] surroundingUnits = this.uc.senseUnits(this.team);
    for (UnitInfo unit : surroundingUnits) {
      if (unit.getType() == UnitType.BASE) {
        this.baseLocation = unit.getLocation();
      }
    }
  }

  //  void attack() {
  //    if (this.uc.canAttack(this.target.getLocation())) {
  //      this.fighting = true;
  //      // if enemy is killed, remove it
  //      if (this.target.getHealth() <= 15) {
  //        this.target = null;
  //      }
  //      this.uc.attack(this.target.getLocation());
  //    }
  //  }

  void followTarget() {
    // find target again, track location
    for (UnitInfo target : this.uc.senseUnits(this.enemyTeam)) {
      if (target.getID() == this.targetID) {
        this.target = target;
        this.targetLocked = true;
      }
    }
    if (this.uc.canMove()) {}
  }

  void senseEnemies() {
    UnitInfo[] enemies = uc.senseUnits(enemyTeam);

    if (enemies.length != 0) {
      currentTargetLocation = enemies[0].getLocation();
      fighting = true;
      bug2(uc.getLocation(), enemies[0].getLocation());
    } else {
      UnitInfo[] deer = uc.senseUnits(deerTeam);

      if (deer.length != 0) {
        currentTargetLocation = deer[0].getLocation();
        fighting = true;
        bug2(uc.getLocation(), deer[0].getLocation());
      } else {
        fighting = false; // Enemy was killed
      }
    }
  }

  void attack() {
    if (uc.canAttack(currentTargetLocation)) {
      uc.attack(currentTargetLocation);
    }

    bug2(uc.getLocation(), currentTargetLocation);

    if (uc.canAttack(currentTargetLocation)) {
      uc.attack(currentTargetLocation);
    }
  }
}
