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

    public void fight() {
        Location target = foes.get(0).getLocation();

        goToLocation(target);

        if (uc.canAttack(target) && uc.canSenseLocation(target) && uc.senseUnitAtLocation(target) != null) {
            if (foes.get(0).getHealth() <= uc.getInfo().getAttack()) {
                foes.remove(0);
            }
            uc.attack(target);
        }
    }

    public void retreat(Location post) {

    }

    public void retreat() {

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
            fight();
        }
    }

    void playRound(){
        lightTorch();
        FollowPath();
    }

}
