package ourplayer;

import java.util.ArrayList;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.Resource;
import aic2021.user.ResourceInfo;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Explorer extends MyUnit {

  Explorer(UnitController uc) {
    super(uc);
  }

  boolean sentSignal = false;
  boolean torchLit = false;
  boolean sentryMode = false;
  boolean baseFound = false;
  ArrayList<Location> visited = new ArrayList<>();

  void playRound() {

    // try to send enemy base location smoke signal
    if (this.uc.canMakeSmokeSignal() && this.enemyBaseLocation != null && !this.sentSignal) {
      int signal = encodeSmokeSignal(this.enemyBaseLocation, 0, 1);
      this.uc.makeSmokeSignal(signal);
      this.uc.println(
          "enemy base smoke signal fired on round " + this.uc.getRound() + ". Signal: " + signal);
      this.sentSignal = true;
    }

    if (this.nuevoResources.get(WOOD).size() > 0) {
//      this.uc.println("Resource at: " + this.newResources.remove(0));

      if (this.uc.canMakeSmokeSignal()) {
        int signal = encodeSmokeSignal(this.nuevoResources.get(WOOD).remove(0).getLocation(), WOOD, 1);
        this.uc.makeSmokeSignal(signal);
        this.uc.println(
                "resource smoke signal fired on round " + this.uc.getRound() + ". Signal: " + signal);
      }
    }

    // light torch
    if (!torchLit && this.uc.getInfo().getTorchRounds() <= 0) {
      lightTorch();
    }

    // handles movement
    if (this.baseFound) {
      if (!this.enemyReaction() && !this.sentryMode) {
        //            this.betterMove();
        //            this.betterMove2();
        this.betterMove3();
      }
    } else {
      if (!this.sentryMode) {
        this.betterMove3();
      }
    }

    // clean up visited memory
    if (this.visited.size() > 50) {
      this.visited.remove(0);
    }

    // did it find the base?
    if (!this.baseFound) {
      if (this.findBase()) {
        this.sentryMode = true;
        this.baseFound = true;
        this.uc.println("Enemy base found at " + this.enemyBaseLocation);
      }
    }

    // look for resources
    this.findResources2();
  }

  /** @return true if there are dangerous enemies, false otherwise */
  boolean enemyReaction() {
    UnitInfo thisInfo = this.uc.getInfo();
    UnitInfo[] enemies =
        this.uc.senseUnits(thisInfo.getType().getVisionRange(), this.uc.getOpponent());
    for (UnitInfo info : enemies) {
      // if the enemies are hostile
      if (info.getAttack() > 0) {
        this.sentryMode = false;
        this.runAway(this.uc.getLocation().directionTo(info.getLocation()));
        break;
      }
    }
    return enemies.length != 0;
  }

  void runAway(Direction enemy) {
    Direction optimal = enemy.opposite();
    this.currentDir = optimal;
    this.visited.clear();
    this.betterMove3();
  }

  // moves to the tile farthest from all the previously visited ones
  void betterMove() {
    int tries = 8;
    this.currentDir = this.optimalDirection();
    while (this.uc.canMove() && tries-- > 0) {
      if (this.uc.canMove(this.currentDir)) {
        this.uc.move(this.currentDir);
        this.visited.add(this.uc.getLocation());
      } else {
        double randomTurn = this.uc.getRandomDouble();
        if (randomTurn < 0.5) {
          this.currentDir = this.currentDir.rotateLeft().rotateLeft().rotateLeft();
        } else {
          this.currentDir.rotateRight().rotateRight().rotateRight();
        }
      }
    }
  }

  void betterMove2() {
    int tries = 8;
    this.currentDir = this.optimalDirection();
    while (this.uc.canMove() && tries-- > 0) {
      if (this.uc.canMove(this.currentDir) && !this.alreadySeenCheck()) {
        this.uc.move(this.currentDir);
        this.visited.add(this.uc.getLocation());
      } else {
        double randomTurn = this.uc.getRandomDouble();
        if (randomTurn < 0.5) {
          this.currentDir = this.currentDir.rotateLeft().rotateLeft().rotateLeft();
        } else {
          this.currentDir.rotateRight().rotateRight().rotateRight();
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

  void betterMove3() {
    int tries = 8;
    this.currentDir = this.optimalDirection();
    while (this.uc.canMove() && tries-- > 0) {
      if (this.uc.canMove(this.currentDir) && !this.alreadySeenCheck()) {
        this.uc.move(this.currentDir);
        this.visited.add(this.uc.getLocation());
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
            this.currentDir.rotateRight();
          } else {
            this.currentDir.rotateRight().rotateRight().rotateRight();
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
    //    Direction go = Direction.NORTH;
    double score = 0;
    double tempScore = 0;
    Location targetLoc;
    for (Direction dir : Direction.values()) {
      targetLoc = this.uc.getLocation().add(dir);
      for (Location l : this.visited) {
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

  boolean alreadySeenCheck() {
    Location considering = this.uc.getLocation().add(this.currentDir);
    for (Location temp : this.visited) {
      if (considering.isEqual(temp)) {
        return true;
      }
    }
    return false;
  }

  boolean findBase() {
    for (UnitInfo info : this.uc.senseUnits(this.uc.getOpponent())) {
      if (info.getType() == UnitType.BASE) {
        this.enemyBaseLocation = info.getLocation();
        return true;
      }
    }
    return false;
  }

  boolean findResources2() {
    for (ResourceInfo info : this.uc.senseResources()) {
      Location l = info.getLocation();
      if (info.amount > 50 && !this.alreadySeenResourceCheck(l)) {
        if (info.resourceType == Resource.WOOD) {
          this.nuevoResources.get(WOOD).add(info);
        } else if (info.resourceType == Resource.STONE) {
          this.nuevoResources.get(STONE).add(info);
        } else {
          this.nuevoResources.get(FOOD).add(info);
        }
        this.seenResources.add(l);
        return true;
      }
    }
    return false;
  }

  boolean alreadySeenResourceCheck(Location l) {
    for (Integer i : this.nuevoResources.keySet()) {
      for (ResourceInfo info : this.nuevoResources.get(i)) {
        if (l.isEqual(info.getLocation())) {
          return true;
        }
      }
    }
    return false;
  }

  // ignores low resource stores (less than 50)
  void findResources() {
    for (ResourceInfo info : this.uc.senseResources()) {
      if (this.uc.isAccessible(info.getLocation())
          && info.amount > 50
          && !this.seenResources.contains(info.getLocation())) {
        this.newResources.add(info.getLocation());
      }
    }
  }
}
