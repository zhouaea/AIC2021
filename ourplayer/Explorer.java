package ourplayer;

import java.util.ArrayList;
import java.util.HashSet;

import aic2021.user.*;

public class Explorer extends MyUnit {

    boolean sentSignal = false;
    boolean baseFound = false;
    boolean sentWaterSignal = false;
    boolean settlementCreated = false;

    final int visitedID = 71920;
    final int waterTileThreshold = 5;

    Direction currentDir = Direction.NORTH;

    Location enemyBaseLocation;

    ArrayList<ResourceInfo> newResources = new ArrayList<>();

    HashSet<Location> seenResources = new HashSet<>();

    ArrayList<Location> dangerZone = new ArrayList<>();

    HashSet<Integer> seenEnemies = new HashSet<>();

    HashSet<Location> seenWater = new HashSet<>();

    Team team = this.uc.getTeam();
    Team enemyTeam = this.uc.getOpponent();
    Team deerTeam = Team.NEUTRAL;

    Explorer(UnitController uc) {
        super(uc);
    }

    void playRound() {
        decodeMessages();

        // try to send enemy base location smoke signal once a settlement has been created.
        if (this.settlementCreated) {
            if (this.uc.canMakeSmokeSignal() && this.enemyBaseLocation != null && !this.sentSignal) {
                int signal = encodeSmokeSignal(this.enemyBaseLocation, 0, 1);
                this.uc.makeSmokeSignal(signal);
                this.uc.println(
                        "enemy base smoke signal fired on round " + this.uc.getRound() + ". Signal: " + signal);
                this.sentSignal = true;
            }
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

        // Try to send army size smoke signal (at most every 49 rounds).
        if (this.uc.getRound() % 49 == 0) {
            if (this.uc.canMakeSmokeSignal()) {
                if (!this.seenEnemies.isEmpty()) {
                    int enemyArmySize = this.seenEnemies.size();
                    this.uc.makeSmokeSignal(encodeSmokeSignal(enemyArmySize, ENEMY_ARMY_COUNT_REPORT, 0));
                    this.uc.println(
                            "Enemy army size smoke signal fired on round "
                                    + this.uc.getRound()
                                    + ". Enemies: "
                                    + enemyArmySize);
                }
            }
        }

        // try to send water smoke signal
        if (!this.sentWaterSignal) {
            if (this.seenWater.size() >= this.waterTileThreshold) {
                if (uc.canMakeSmokeSignal()) {
                    this.uc.makeSmokeSignal(encodeSmokeSignal(0, BUY_RAFTS, 0));
                    this.sentWaterSignal = true;
                    this.uc.println("Build raft signal fired on round " + this.uc.getRound());
                }
            }
        }

        this.uc.println("Energy left after signals: " + this.uc.getEnergyLeft());

        // light torch
        this.keepTorchLit();

        // count water
        if (!this.sentWaterSignal) {
            this.countWater();
        }

        this.uc.println("Energy left after counting water: " + this.uc.getEnergyLeft());

        // handles movement
        if (!this.enemyReaction()) {
            this.betterMove3();
        }

        this.uc.println("Energy left after enemy reaction/movement: " + this.uc.getEnergyLeft());

        // did it find the base?
        if (!this.baseFound) {
            if (this.findBase()) {
                this.baseFound = true;
                //        this.calculateDangerZone();
            }
        }

        this.uc.println(
                "Energy left after finding enemy base and calculating danger zone: "
                        + this.uc.getEnergyLeft());

        // look for resources
        if (this.uc.getRound() > 100) {
            this.findResources();
        }

        this.uc.println("Energy left after finding resources (last step): " + this.uc.getEnergyLeft());
    }

    void decodeMessages() {
        int[] smokeSignals = uc.readSmokeSignals();
        Location currentLocation = uc.getLocation();
        DecodedMessage message;

        for (int smokeSignal : smokeSignals) {
            message = decodeSmokeSignal(currentLocation, smokeSignal);
            if (message != null) {
                if (message.unitCode == SETTLEMENT_CREATED) {
                    settlementCreated = true;
                    uc.println("a settlement has been created");
                }
            }
        }
    }

    void betterMove3() {
        int tries = 8;
        this.currentDir = this.optimalDirection();
        while (this.uc.canMove() && tries-- > 0) {
            if (this.uc.canMove(this.currentDir)
                    && this.validLocationCheck(this.uc.getLocation().add(this.currentDir))) {
                this.uc.move(this.currentDir);
                // record new visited location
                //        this.visited.add(this.uc.getLocation());

                // mark location with rock art
                if (this.uc.canDraw(this.visitedID)) {
                    this.uc.draw(this.visitedID);
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
        if (this.uc.canMove(this.currentDir) && this.lastResortValidLocationCheck()) {
            this.uc.move(this.currentDir);
        }
    }

    boolean lastResortValidLocationCheck() {
        Location loc = this.uc.getLocation().add(this.currentDir);
        if (uc.hasTrap(loc)) return false;

        // Factor in the attack radius of the enemy base, if its location is known.
        if (enemyBaseLocation != null) {
            if (loc.distanceSquared(enemyBaseLocation) <= UnitType.BASE.getAttackRange()) {
                return false;
            }
        }
        return true;
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
        if (this.uc.canSenseLocation(l)) {
            if (this.uc.read(l) == this.visitedID) {
                return false;
            }
            if (this.uc.hasTrap(l)) {
                return false;
            }
        }
        // Factor in the attack radius of the enemy base, if its location is known.
        if (this.enemyBaseLocation != null) {
            if (l.distanceSquared(this.enemyBaseLocation) <= UnitType.BASE.getAttackRange()) {
                return false;
            }
        }
        return true;
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

    // ignores stores with less than 100
    void findResources() {
        if (this.seenResources.size() < 60) {
            for (ResourceInfo info : this.uc.senseResources()) {
                Location l = info.location;
                if (info.amount >= 100 && !this.alreadySeenResourceCheck(l)) {
                    this.newResources.add(info);
                    this.seenResources.add(l);
                }
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
        boolean react = false;
        UnitInfo[] enemies = this.uc.senseUnits(this.uc.getOpponent());
        for (UnitInfo info : enemies) {
            // if the enemies are hostile and explorer is in range
            int attackRange = info.getType().attackRange;
            if (info.getAttack() > 0
                    && (info.getLocation().distanceSquared(this.uc.getLocation()) <= attackRange + 1)) {
                this.runAway(this.uc.getLocation().directionTo(info.getLocation()));
                react = true;
                break;
            }
        }
        return react;
    }

    void runAway(Direction enemy) {
        if (this.baseFound) {
            Direction optimal = enemy.opposite();
            this.currentDir = optimal;
        }
        this.betterMove3();
    }

    void countWater() {
        for (Location water : this.uc.senseWater(this.uc.getType().getVisionRange())) {
            if (!this.alreadySeenWaterCheck(water)) {
                this.seenWater.add(water);
            }
        }
    }

    boolean alreadySeenWaterCheck(Location l) {
        for (Location loc : this.seenWater) {
            if (l.isEqual(loc)) {
                return true;
            }
        }
        return false;
    }

    void calculateDangerZone() {
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
    }
}