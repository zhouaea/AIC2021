package ourplayer;

import aic2021.user.*;

import java.util.ArrayList;

public class Axeman extends MyUnit {

    Direction[] dirs = Direction.values();

    Axeman(UnitController uc){
        super(uc);
    }

    ArrayList<UnitInfo> foes = new ArrayList<>();
    private boolean fighting = false;
    private int distToEnemy = 0;
    private int distanceCounter = 0;
    Location baseLocation = null;

    public void fight() {
        if (foes.isEmpty()) {
            retreat();
        } else {
            Location target = foes.get(0).getLocation();
            int newDistance = uc.getLocation().distanceSquared(target);
            if (distToEnemy == 0)
                distToEnemy = newDistance;

            goToLocation(target);

            if (uc.canAttack(target) && uc.canSenseLocation(target) && uc.senseUnitAtLocation(target) != null) {
                if (foes.get(0).getHealth() <= uc.getInfo().getAttack()) {
                    foes.remove(0);
                    distanceCounter = 0;
                    distToEnemy = 0;
                }
                uc.attack(target);
            }

            if (newDistance >= distToEnemy)
                distanceCounter++;

            if (distanceCounter == 8) {
                retreat();
            }
        }
    }

    public void retreat(Location post) {

    }

    public void retreat() {
        fighting = false;
        goToLocation(baseLocation.add(uc.getLocation().directionTo(baseLocation).opposite()));
    }

    public void DetectUnits() {
        UnitInfo thisUnit = uc.getInfo();
        double friendlyAP = thisUnit.getAttack() / thisUnit.getAttackCooldown();
        double friendlyHealth = thisUnit.getHealth();
        double enemyAP = 0;
        double enemyHealth = 0;

        for (UnitInfo unit : uc.senseUnits(uc.getType().visionRange)) {
            if (unit.getTeam() == uc.getTeam() && unit.getType() == UnitType.AXEMAN) {

                friendlyAP += unit.getAttack() / unit.getAttackCooldown();
                friendlyHealth += unit.getHealth();
            } else {
                foes.add(unit);
                enemyAP += unit.getAttack() / unit.getAttackCooldown();
                enemyHealth += unit.getHealth();
            }
        }

        if (enemyHealth / friendlyAP < friendlyHealth / enemyAP) {
            fighting = true;
        }
    }

    void rememberBaseLocation() {
        UnitInfo[] surroundingUnits = uc.senseUnits(uc.getInfo().getTeam());
        for (UnitInfo unit : surroundingUnits) {
            if (unit.getType() == UnitType.BASE) {
                baseLocation = unit.getLocation();
            }
        }
    }

    void playRound(){
        lightTorch();
        if (baseLocation == null)
            rememberBaseLocation();

        if (!fighting)
            DetectUnits();
        else
            fight();

        FollowPath();
    }

}
