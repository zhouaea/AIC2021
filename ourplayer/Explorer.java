package ourplayer;

import java.util.ArrayList;
import java.util.Arrays;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.ResourceInfo;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Explorer extends MyUnit {

  Explorer(UnitController uc) {
    super(uc);
  }

  boolean torchLit = false;
  boolean sentryMode = false;
  ArrayList<Location> visited = new ArrayList<>();

  /*
  Bait
  BalancedStrats
  Basic1
  Basic2
  Basic3
  Basic4
  HairyDiscord
  HappyClown
  ILoveAIC
  Resourceful
  TestIvanFilter
  TooBigForTheMeta
  TowerOfGod
  UnworthyTrees
  WhoNeedsAMap
  YetAnotherBasicMap
   */

  void playRound() {

    // communicate resource locations (by smoke signals or rock art)

    // light torch
    if (!torchLit && this.uc.getInfo().getTorchRounds() <= 0) {
      lightTorch();
    }

    if (!this.sentryMode) {
      //            this.betterMove();
      //            this.betterMove2();
      this.betterMove3();
    }

    if (this.visited.size() > 50) {
      this.visited.remove(0);
    }

    if (this.findBase()) {
      this.sentryMode = true;
    }

    this.findResources();
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
    Direction go = Direction.NORTH;
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

  // ignores low resource stores (less than 50)
  void findResources() {
    for (ResourceInfo info : this.uc.senseResources()) {
      ArrayList<Location> locs = new ArrayList<>(Arrays.asList(this.uc.getVisibleLocations(true)));
      if (locs.contains(info.getLocation())
          && this.uc.isAccessible(info.getLocation())
          && info.amount > 50
          && !this.resources.contains(info.getLocation())) {
        this.resources.add(info.getLocation());
      }
    }
  }
}
