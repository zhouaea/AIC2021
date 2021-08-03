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
    int zoneMaxX;
    int zoneMinX;
    int zoneMaxY;
    int zoneMinY;

    Location currentTargetLocation;


    boolean fighting = false;

    void playRound() {
        // remember base location
        if (this.baseLocation == null) {
            this.rememberBaseLocation();
        }

        this.keepTorchLit();

        this.senseEnemies();

        if (fighting) {
            this.attack();
        } else {
            // if not in zone
            if (!this.inZone(this.uc.getLocation())) {
                // return to base
                bug2(uc.getLocation(), this.baseLocation, true);
            } else {
                // move randomly in zone
                this.moveRandomInZone();
            }
        }
    }

    void rememberBaseLocation() {
        UnitInfo[] surroundingUnits = this.uc.senseUnits(this.team);
        for (UnitInfo unit : surroundingUnits) {
            if (unit.getType() == UnitType.BASE) {
                this.baseLocation = unit.getLocation();
                this.calculateZone();
            }
        }
    }

    void senseEnemies() {
        UnitInfo[] enemies = uc.senseUnits(enemyTeam);

        if (enemies.length != 0) {
            currentTargetLocation = enemies[0].getLocation();
            fighting = true;
        } else {
            if (fighting) {
                fighting = false; // Enemy was killed
                closestLocation = null; // Reset pathfinding manually after axeman is done chasing enemy
            }
        }
    }

    void attack() {
        if (uc.canAttack(currentTargetLocation)) {
            uc.attack(currentTargetLocation);
        }

        bug2(uc.getLocation(), currentTargetLocation, true);

        if (uc.canAttack(currentTargetLocation)) {
            uc.attack(currentTargetLocation);
        }
    }

    boolean moveRandomInZone() {
        int tries = 10;
        while (uc.canMove() && tries-- > 0) {
            int random = (int) (uc.getRandomDouble() * 8);
            if (uc.canMove(dirs[random]) && this.inZone(uc.getLocation().add(dirs[random]))) {
                uc.move(dirs[random]);
                return true;
            }
        }
        return false;
    }

    boolean inZone(Location location) {
        return location.x <= this.zoneMaxX
                && location.x >= this.zoneMinX
                && location.y <= this.zoneMaxY
                && location.y >= this.zoneMinY;
    }

    void calculateZone() {
        this.zoneMaxX = this.baseLocation.x + 8;
        this.zoneMinX = this.baseLocation.x - 8;
        this.zoneMaxY = this.baseLocation.y + 8;
        this.zoneMinY = this.baseLocation.y - 8;
    }
}
