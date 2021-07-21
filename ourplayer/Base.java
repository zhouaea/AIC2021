package ourplayer;

import aic2021.user.*;

public class Base extends MyUnit {

    int workers = 0;
    Team team = uc.getTeam();
    Team enemy_team = uc.getOpponent();

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        playDefense();
        senseEnemies();
        senseResources();
        researchTech();
        if (uc.getRound() == 0)
            senseTerrain();

        uc.println("energy used: " + uc.getEnergyUsed());
        uc.println("energy left: " + uc.getEnergyLeft());
    }

    void playDefense() {
        // Sense enemy units in attack radius and shoot them (add prioritization algorithm later).
        UnitInfo[] shootable_enemies = uc.senseUnits(18, enemy_team);
        for (UnitInfo enemy : shootable_enemies) {
            if (uc.canAttack()) {
                uc.attack(enemy.getLocation());
            }
        }
    }

    void senseEnemies() {
        // Sense enemy units in vision radius and alert team of how many are attacking and what kind.
        // Prioritize: enemy base -> scouts -> buildings -> wolves -> axemen -> spearmen -> workers -> trappers
        // UnitInfo[] enemies = uc.senseUnits(enemy_team);

    }

    void senseResources() {
        // Sense resources in range and alert team of each.
        ResourceInfo[] nearby_resources = uc.senseResources();
        for (ResourceInfo resource : nearby_resources) {
            // Communicate each resource's location, type, and amount via smoke signalling
        }
    }

    void researchTech() {
        if (uc.getTechLevel(team) == 0)
            researchTechLevel0();
        else {
            if (workers < 1)
                spawnRandom(UnitType.WORKER);

            if (uc.getTechLevel(team) == 1)
                researchTechLevel1();
            else if (uc.getTechLevel(team) == 2)
                researchTechLevel2();
            else
                if (uc.canResearchTechnology(Technology.WHEEL))
                    uc.researchTechnology(Technology.WHEEL);
        }
    }

    private void researchTechLevel0() {
        if (uc.canResearchTechnology(Technology.COIN)) {
            uc.researchTechnology(Technology.COIN);
            return;
        }
        if (uc.canResearchTechnology(Technology.BOXES) && uc.hasResearched(Technology.COIN, team)) {
            uc.researchTechnology(Technology.BOXES);
            return;
        }
        if (uc.canResearchTechnology(Technology.UTENSILS) && uc.hasResearched(Technology.COIN, team)) {
            uc.researchTechnology(Technology.UTENSILS);
            return;
        }
    }

    private void researchTechLevel1() {
        if (uc.canResearchTechnology(Technology.JOBS)) {
            uc.researchTechnology(Technology.JOBS);
            return;
        }

        if (uc.canResearchTechnology(Technology.HUTS) && uc.hasResearched(Technology.JOBS, team)) {
            uc.researchTechnology(Technology.HUTS);
            return;
        }

        if (uc.canResearchTechnology(Technology.NAVIGATION) && uc.hasResearched(Technology.JOBS, team)) {
            uc.researchTechnology(Technology.NAVIGATION);
            return;
        }
    }

    private void researchTechLevel2() {
        if (uc.canResearchTechnology(Technology.EXPERTISE)) {
            uc.researchTechnology(Technology.EXPERTISE);
            return;
        }

        // Switch this with stone or wood tech if resources are widely available.
        if (uc.canResearchTechnology(Technology.POISON)) {
            uc.researchTechnology(Technology.POISON);
            return;
        }

        if (uc.canResearchTechnology(Technology.HOUSES)) {
            uc.researchTechnology(Technology.HOUSES);
            return;
        }
    }

    void senseTerrain() {
        // Sense terrain in range, and maybe let troops know about important spots.
        Location[] water_tiles = uc.senseWater(50);
        Location[] mountain_tiles = uc.senseMountains(50);
    }

    boolean spawnRandom(UnitType t){
        for (Direction dir : dirs){
            if (uc.canSpawn(t, dir)){
                uc.spawn(t, dir);
                workers++;
                return true;
            }
        }
        return false;
    }
}
