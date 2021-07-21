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

    boolean sentryMode = false;
    boolean exploringDirection = false;

    void playRound() {

        // communicate resource locations (by smoke signals or rock art)

        if (!this.sentryMode) {
            this.handleMove();
        }

        if (this.findBase()) {
            this.sentryMode = true;
        }

        this.findResources();
    }

    // handles moving the explorer around the map
    void handleMove() {
        int tries = 8;
        while (this.uc.canMove() && tries-- > 0) {
            double randomTurn = Math.random();
            if (randomTurn < 0.1 && !this.exploringDirection) {
                if (randomTurn < 0.05) {
                    this.currentDir = this.currentDir.rotateRight().rotateRight();
                } else {
                    this.currentDir = this.currentDir.rotateLeft().rotateLeft();
                }
            }

            if (this.uc.canMove(this.currentDir)) {
                this.uc.move(this.currentDir);
            } else {
                this.exploringDirection = false;
                if (randomTurn < 0.5) {
                    this.currentDir = this.currentDir.rotateRight().rotateRight().rotateRight();
                } else {
                    this.currentDir = this.currentDir.rotateLeft().rotateLeft().rotateLeft();
                }
            }
        }

        ArrayList<Direction> toExplore = new ArrayList<>();

        // update known max and min coordinates
        for (Location loc : this.uc.getVisibleLocations(true)) {
            if (loc.x > this.xMax) {
                this.xMax = loc.x;
                toExplore.add(Direction.EAST);
            }
            if (loc.x < this.xMin) {
                this.xMin = loc.x;
                toExplore.add(Direction.WEST);
            }
            if (loc.y > this.yMax) {
                this.yMax = loc.y;
                toExplore.add(Direction.SOUTH);
            }
            if (loc.y < this.yMin) {
                this.yMin = loc.y;
                toExplore.add(Direction.NORTH);
            }
        }

        // choose new direction to try moving in (can be further explored)
        if (!toExplore.isEmpty() && !this.exploringDirection) {
            this.currentDir = toExplore.get((int) (Math.random() * toExplore.size()));
            this.exploringDirection = true;
        }
    }

  /*
  Use the sense methods in UnitController to determine paths (avoid obstacles and units)
   */

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
            if (locs.contains(info.getLocation()) && this.uc.isAccessible(info.getLocation())
                    && info.amount > 50
                    && !this.resources.contains(info.getLocation())) {
                this.resources.add(info.getLocation());
            }
        }
    }

    static Direction turnAround(Direction dir) {
        return dir.rotateRight().rotateRight().rotateRight().rotateRight();
    }

    protected void handleSmokeSignal() {
        if (this.uc.canMakeSmokeSignal()) {}
    }

    protected void handleRockArt() {}

    //  public void play() {
    //    //We want to claim at most 3 adjacent food tiles for ourselves and keep mining them
    //    assignFood();
    //
    //    //We broadcast that those food tiles are ours to avoid having other ants mining from them as
    // well
    //    claimFood();
    //
    //    tryCollectFood();
    //    move();
    //    tryCollectFood();
    //    micro.tryGenericAttack();
    //  }
    //
    //  public void tryCollectFood() {
    //
    //    FoodInfo bestFood = null;
    //
    //    FoodInfo[] foodsInfo = uc.senseFood();
    //    for(FoodInfo currentFoodInfo: foodsInfo) {
    //      // ignore locations with less than 3 food
    //      if (currentFoodInfo.getFood() < 3) continue;
    //
    //      // we keep track of the best food location with our custom comparison method
    //      if (isBetterFoodAThanB(currentFoodInfo, bestFood)) bestFood = currentFoodInfo;
    //    }
    //
    //    if(bestFood != null && uc.canMine(bestFood)) {
    //      uc.mine(bestFood);
    //    }
    //  }
    //
    //  public boolean isBetterFoodAThanB(FoodInfo a, FoodInfo b) {
    //    // first we try with our assigned food
    //    // if we can't, we then try to get any food
    //
    //    // check if one is null and return the other
    //    if(a == null) return false;
    //    if(b == null) return true;
    //
    //    Location locationA = a.getLocation();
    //    Location locationB = b.getLocation();
    //
    //    // if one is mine and the other isn't, we return the one that is mine no matter what
    //    if(foodTracker.isMine(locationA) && !foodTracker.isMine(locationB)) {
    //      return true;
    //    }
    //
    //    if(!foodTracker.isMine(locationA) && foodTracker.isMine(locationB)) {
    //      return false;
    //    }
    //
    //    // else, we get from the one that has more
    //    return a.getFood() > b.getFood();
    //  }
    //
    //  public void assignFood() {
    //    if (firstUnassignedIndex >= myFood.length) return;
    //
    //    //if we discover any available food, we claim it and try again!
    //    myFood[firstUnassignedIndex] =
    // foodTracker.getNearestUnclaimedDiscoveredFood(uc.getLocation());
    //    if (myFood[firstUnassignedIndex] != null){
    //      foodTracker.claimMine(myFood[firstUnassignedIndex++]);
    //      assignFood();
    //    }
    //  }
    //
    //  public void claimFood(){
    //    for (Location foodLocation: myFood) {
    //      if (foodLocation != null) {
    //        foodTracker.claimMine(foodLocation);
    //      }
    //    }
    //  }
    //
    //  public void move() {
    //    UnitInfo[] enemies = uc.senseUnits(uc.getOpponent());
    //
    //    if(enemies.length == 0) {
    //      /* if there are no enemies we go to our objective location  */
    //      pathfinding.moveTo(myFood[0]);
    //    }
    //    else {
    //      micro.doMicro();
    //    }
    //  }
}
