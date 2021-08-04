package ourplayer;

import java.util.ArrayList;
import java.util.HashSet;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.ResourceInfo;
import aic2021.user.Team;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Explorer extends MyUnit {

  boolean initDirSet = false;
  boolean sentSignal = false;
  boolean baseFound = false;

  int visitedID = 71920;

  Direction currentDir;

  Location enemyBaseLocation;

  ArrayList<ResourceInfo> newResources = new ArrayList<>();

  ArrayList<Location> seenResources = new ArrayList<>();

  ArrayList<Location> dangerZone = new ArrayList<>();

  int enemyArmySize = 0;

  HashSet<Integer> seenEnemies = new HashSet<>();

  Team team = this.uc.getTeam();
  Team enemyTeam = this.uc.getOpponent();
  Team deerTeam = Team.NEUTRAL;

  Explorer(UnitController uc) {
    super(uc);
  }

  void playRound() {
    // sets initial direction at spawn
    if (!this.initDirSet) {
      this.currentDir = Direction.NORTH;
      this.initDirSet = true;
    }

    // try to send enemy base location smoke signal
    if (this.uc.canMakeSmokeSignal() && this.enemyBaseLocation != null && !this.sentSignal) {
      int signal = encodeSmokeSignal(this.enemyBaseLocation, 0, 1);
      this.uc.makeSmokeSignal(signal);
      this.uc.println(
          "enemy base smoke signal fired on round " + this.uc.getRound() + ". Signal: " + signal);
      this.sentSignal = true;
    }

    // try to send resource location smoke signal (at most every 30 rounds)
    if (this.uc.getRound() % 30 == 0 && this.uc.getRound() > 100) {
      if (this.newResources.size() > 0) {
        if (this.uc.canMakeSmokeSignal()) {
          int signal = this.encodeResourceMessage(this.newResources.remove(0));
          this.uc.makeSmokeSignal(signal);
          this.uc.println(
              "resource smoke signal fired on round " + this.uc.getRound() + ". Signal: " + signal);
        }
      }
    }

    // try to send resource location smoke signal (at most every 49 rounds)
    if (this.uc.getRound() % 49 == 0) {
      if (this.uc.canMakeSmokeSignal()) {
        int signal = this.enemyArmySize * 719;
        this.uc.makeSmokeSignal(signal);
        this.uc.println(
            "Enemy army size smoke signal fired on round "
                + this.uc.getRound()
                + ". Signal: "
                + signal
                + ". Enemies: "
                + this.enemyArmySize);
      }
    }

    // light torch
    this.keepTorchLit();

    // count enemies
    this.countEnemies();
    this.uc.println("Number of counted enemies: " + this.enemyArmySize);

    this.uc.println("Energy used before moving: " + this.uc.getEnergyUsed());

    // handles movement
    if (!this.enemyReaction()) {
      this.betterMove3();
    }

    this.uc.println("Energy used after moving: " + this.uc.getEnergyUsed());

    //     clean up visited memory
    //    if (this.visited.size() > 30) {
    //      this.visited.remove(0);
    //    }

    this.uc.println("Energy used after clearing memory: " + this.uc.getEnergyUsed());

    // did it find the base?
    if (!this.baseFound) {
      if (this.findBase()) {
        this.baseFound = true;
        this.calculateDangerZone();
        this.uc.println("Enemy base found at " + this.enemyBaseLocation);
      }
    }

    this.uc.println("Energy used after finding base: " + this.uc.getEnergyUsed());

    // look for resources
    if (this.uc.getRound() > 100) {
      this.findResources();
    }

    this.uc.println("Energy used after finding resources: " + this.uc.getEnergyUsed());
  }

  void betterMove3() {
    int tries = 8;
    this.uc.println("Energy used before optimal direction calculation: " + this.uc.getEnergyUsed());
    this.currentDir = this.optimalDirection();
    this.uc.println("Energy used after optimal direction calculation: " + this.uc.getEnergyUsed());
    while (this.uc.canMove() && tries-- > 0) {
      if (this.uc.canMove(this.currentDir)
          && this.validLocationCheck(this.uc.getLocation().add(this.currentDir))) {
        this.uc.move(this.currentDir);
        // record new visited location
        //        this.visited.add(this.uc.getLocation());

        // mark location with rock art
        if (this.uc.canDraw(this.visitedID)) {
          this.uc.draw(this.visitedID);
          this.uc.println("Rock art drawn: " + this.visitedID);
        }
        return;
      } else {
        double randomTurn = this.uc.getRandomDouble();
        if (randomTurn < 0.5) {
          if (randomTurn < 0.4) {
            this.currentDir = this.currentDir.rotateLeft();
          } else {
            this.currentDir = this.currentDir.rotateLeft().rotateLeft().rotateLeft();
          }
        } else {
          if (randomTurn < 0.9) {
            this.currentDir = this.currentDir.rotateRight();
          } else {
            this.currentDir = this.currentDir.rotateRight().rotateRight().rotateRight();
          }
        }
      }
    }
    // in case explorer is stuck (all surrounding tiles have been visited)
    if (this.uc.canMove()) {
      if (this.uc.canMove(this.currentDir)) {
        this.uc.move(this.currentDir);
      }
    }
  }

  // determines the direction that will bring the explorer farthest from all previous tiles
  Direction optimalDirection() {
    Direction go = this.currentDir;
    double score = 0;
    double tempScore = 0;
    Location targetLoc;
    ArrayList<Location> visited = this.findVisitedLocations();
    for (Direction dir : Direction.values()) {
      targetLoc = this.uc.getLocation().add(dir);
      for (Location l : visited) {
        tempScore += targetLoc.distanceSquared(l);
      }
      if (tempScore > score) {
        go = dir;
        score = tempScore;
      }
      tempScore = 0;
    }
    return go;
  }

  ArrayList<Location> findVisitedLocations() {
    ArrayList<Location> visited = new ArrayList<>();
    for (Location l : this.uc.getVisibleLocations(4, true)) {
      if (this.uc.canRead(l) && this.uc.read(l) == this.visitedID) {
        visited.add(l);
      }
    }
    return visited;
  }

  boolean validLocationCheck(Location l) {
    if (this.uc.canRead(l)) {
      if (this.uc.read(l) == this.visitedID) {
        return false;
      }
    }
    for (Location loc : this.dangerZone) {
      if (l.isEqual(loc)) {
        return false;
      }
    }
    return true;
  }

  //  boolean validLocationCheck(Location l) {
  //    for (Location location : this.visited) {
  //      if (l.isEqual(location)) {
  //        return false;
  //      }
  //    }
  //    for (Location loc : this.dangerZone) {
  //      if (l.isEqual(loc)) {
  //        return false;
  //      }
  //    }
  //    return true;
  //  }

  boolean findBase() {
    for (UnitInfo info : this.uc.senseUnits(this.uc.getOpponent())) {
      if (info.getType() == UnitType.BASE) {
        this.enemyBaseLocation = info.getLocation();
        return true;
      }
    }
    return false;
  }

  // ignores stores with less than 100
  void findResources() {
    for (ResourceInfo info : this.uc.senseResources()) {
      Location l = info.location;
      if (info.amount >= 100 && !this.alreadySeenResourceCheck(l)) {
        this.newResources.add(info);
        this.seenResources.add(l);
      }
    }
  }

  boolean alreadySeenResourceCheck(Location l) {
    for (Location loc : this.seenResources) {
      if (l.isEqual(loc)) {
        return true;
      }
    }
    return false;
  }

  /** @return true if there are dangerous enemies, false otherwise */
  boolean enemyReaction() {
    UnitInfo[] enemies = this.uc.senseUnits(this.uc.getOpponent());
    for (UnitInfo info : enemies) {
      // if the enemies are hostile
      if (info.getAttack() > 0) {
        //        this.sentryMode = false;
        this.runAway(this.uc.getLocation().directionTo(info.getLocation()));
        break;
      }
    }
    return enemies.length != 0;
  }

  void runAway(Direction enemy) {
    if (this.baseFound) {
      this.uc.println("running away");
      Direction optimal = enemy.opposite();
      this.currentDir = optimal;
      //      this.visited.clear();
      this.visitedID++;
    }
    this.betterMove3();
  }

  void countEnemies() {
    for (UnitInfo enemyInfo : this.uc.senseUnits(this.enemyTeam)) {
      if (!this.seenEnemies.contains(enemyInfo.getID())
          && (enemyInfo.getType() == UnitType.AXEMAN || enemyInfo.getType() == UnitType.SPEARMAN)) {
        this.enemyArmySize++;
        this.seenEnemies.add(enemyInfo.getID());
      }
    }
  }

  void calculateDangerZone() {
    this.uc.println("Energy before danger zone: " + this.uc.getEnergyUsed());
    this.dangerZone.add(this.enemyBaseLocation.add(-1, 4));
    this.dangerZone.add(this.enemyBaseLocation.add(0, 4));
    this.dangerZone.add(this.enemyBaseLocation.add(1, 4));
    this.dangerZone.add(this.enemyBaseLocation.add(1, 3));
    this.dangerZone.add(this.enemyBaseLocation.add(2, 3));
    this.dangerZone.add(this.enemyBaseLocation.add(3, 3));
    this.dangerZone.add(this.enemyBaseLocation.add(3, 2));
    this.dangerZone.add(this.enemyBaseLocation.add(3, 1));
    this.dangerZone.add(this.enemyBaseLocation.add(4, 1));
    this.dangerZone.add(this.enemyBaseLocation.add(4, 0));
    this.dangerZone.add(this.enemyBaseLocation.add(4, -1));
    this.dangerZone.add(this.enemyBaseLocation.add(3, -1));
    this.dangerZone.add(this.enemyBaseLocation.add(3, -2));
    this.dangerZone.add(this.enemyBaseLocation.add(3, -3));
    this.dangerZone.add(this.enemyBaseLocation.add(2, -3));
    this.dangerZone.add(this.enemyBaseLocation.add(1, -3));
    this.dangerZone.add(this.enemyBaseLocation.add(1, 4));
    this.dangerZone.add(this.enemyBaseLocation.add(-1, -4));
    this.dangerZone.add(this.enemyBaseLocation.add(0, -4));
    this.dangerZone.add(this.enemyBaseLocation.add(-1, -4));
    this.dangerZone.add(this.enemyBaseLocation.add(-1, -3));
    this.dangerZone.add(this.enemyBaseLocation.add(-2, -3));
    this.dangerZone.add(this.enemyBaseLocation.add(-3, -3));
    this.dangerZone.add(this.enemyBaseLocation.add(-3, -2));
    this.dangerZone.add(this.enemyBaseLocation.add(-3, -1));
    this.dangerZone.add(this.enemyBaseLocation.add(-4, -1));
    this.dangerZone.add(this.enemyBaseLocation.add(-4, 0));
    this.dangerZone.add(this.enemyBaseLocation.add(-4, 1));
    this.dangerZone.add(this.enemyBaseLocation.add(-3, 1));
    this.dangerZone.add(this.enemyBaseLocation.add(-3, 2));
    this.dangerZone.add(this.enemyBaseLocation.add(-3, 3));
    this.dangerZone.add(this.enemyBaseLocation.add(-2, 3));
    this.dangerZone.add(this.enemyBaseLocation.add(-1, 3));
    this.uc.println("Danger zone calculated, energy after: " + this.uc.getEnergyUsed());
  }
}
