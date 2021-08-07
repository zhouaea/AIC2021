package ourplayer;

import aic2021.user.*;

import java.util.HashSet;

public class Base extends MyUnit {

    int workers = 0;
    int explorers = 0;
    final int MAX_WORKERS = 5;
    final int MAX_EXPLORERS = 1;

    Team team = uc.getTeam();
    Team enemyTeam = uc.getOpponent();

    final int WATER_TILE_THRESHOLD = 15;
    boolean waterMode = false;

    boolean newEnemiesSeen = false;
    int enemyArmySize = 0;
    HashSet<Integer> seenEnemies = new HashSet<>();

    int tierZeroResearch = 0;
    int tierOneResearch = 0;
    int tierTwoResearch = 0;

    boolean hasAssignedWorkerAsBarrackBuilder = false;
    int barrackBuilderId = 0;
    boolean hasAssignedWorkerAsBuilder = false;
    int builderId = 0;

    Base(UnitController uc){
        super(uc);
    }

    // TODO If base sees traps, spawn trapper
    void playRound(){
        if (uc.getRound() == 0) {
            earlyGameCheck();
        }
        playDefense();
        countEnemies();
        spawnTroops();
        decodeMessages();
        researchTech(); // TODO change research path based on different states like "normal", "water", etc.
        makeBuilders();


        uc.println("energy used: " + uc.getEnergyUsed());
        uc.println("energy left: " + uc.getEnergyLeft());
    }

    void earlyGameCheck() {
        checkForWater();
    }

    void checkForWater() {
        if (uc.getRound() == 0) {
            Location[] waterTiles = uc.senseWater(50);
            if (waterTiles.length >= WATER_TILE_THRESHOLD) {
                waterMode = true;
                uc.println("water mode activated");
            }
        }
    }

    void playDefense() {
        // Sense enemy units in attack radius and shoot them (add prioritization algorithm later).
        UnitInfo[] shootable_enemies = uc.senseUnits(18, enemyTeam);
        for (UnitInfo enemy : shootable_enemies) {
            if (uc.canAttack()) {
                uc.attack(enemy.getLocation());
            }
        }
    }

    void countEnemies() {
        for (UnitInfo enemyInfo : this.uc.senseUnits(this.enemyTeam)) {
            if (!this.seenEnemies.contains(enemyInfo.getID()) && (enemyInfo.getType() == UnitType.AXEMAN || enemyInfo.getType() == UnitType.SPEARMAN)) {
                this.enemyArmySize++;
                this.seenEnemies.add(enemyInfo.getID());
                newEnemiesSeen = true;
            }
        }

        // Try to send army size smoke signal every round new enemies appear in range.
        if (newEnemiesSeen) {
            if (this.uc.canMakeSmokeSignal()) {
                this.uc.makeSmokeSignal(encodeSmokeSignal(enemyArmySize, ENEMY_ARMY_COUNT_REPORT, 0));
                this.uc.println(
                        "Enemy army size smoke signal fired on round "
                                + this.uc.getRound()
                                + ". Enemies: "
                                + this.enemyArmySize);
                newEnemiesSeen = false;
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
        if (waterMode) {
            if (uc.canResearchTechnology(Technology.RAFTS) && uc.getTechLevel(team) == 0) {
                uc.researchTechnology(Technology.RAFTS);
                return;
            }

            if (uc.canResearchTechnology(Technology.UTENSILS) && uc.hasResearched(Technology.RAFTS, team) && uc.getTechLevel(team) == 0) {
                uc.researchTechnology(Technology.UTENSILS);
            }

            if (uc.canResearchTechnology(Technology.COIN) && uc.hasResearched(Technology.RAFTS, team) && uc.getTechLevel(team) == 0) {
                uc.researchTechnology(Technology.COIN);
            }

            if (uc.canResearchTechnology(Technology.ROCK_ART) && uc.hasResearched(Technology.RAFTS, team) && uc.getTechLevel(team) == 0) {
                uc.researchTechnology(Technology.ROCK_ART);
            }

            if (uc.canResearchTechnology(Technology.BOXES) && uc.hasResearched(Technology.RAFTS, team) && uc.getTechLevel(team) == 0) {
                uc.researchTechnology(Technology.BOXES);
            }
        } else {
            if (uc.canResearchTechnology(Technology.UTENSILS) && uc.getTechLevel(team) == 0) {
                uc.researchTechnology(Technology.UTENSILS);
            }

            if (uc.canResearchTechnology(Technology.COIN) && uc.getTechLevel(team) == 0) {
                uc.researchTechnology(Technology.COIN);
            }

            if (uc.canResearchTechnology(Technology.ROCK_ART) && uc.getTechLevel(team) == 0) {
                uc.researchTechnology(Technology.ROCK_ART);
            }

            if (uc.canResearchTechnology(Technology.BOXES) && uc.getTechLevel(team) == 0) {
                uc.researchTechnology(Technology.BOXES);
            }
        }
    }

    private void researchTechLevel1() {
        if (uc.canResearchTechnology(Technology.JOBS)) {
            uc.researchTechnology(Technology.JOBS);
        }

        if (uc.canResearchTechnology(Technology.TACTICS) && uc.hasResearched(Technology.JOBS, team ) && uc.getTechLevel(team) == 1) {
            uc.researchTechnology(Technology.TACTICS);
        }

        if (uc.canResearchTechnology(Technology.VOCABULARY) && uc.hasResearched(Technology.JOBS, team) && uc.getTechLevel(team) == 1) {
            uc.researchTechnology(Technology.VOCABULARY);
        }

        if (uc.canResearchTechnology(Technology.OIL) && uc.hasResearched(Technology.JOBS, team) && uc.getTechLevel(team) == 1) {
            uc.researchTechnology(Technology.OIL);
        }

        if (uc.canResearchTechnology(Technology.HUTS) && uc.hasResearched(Technology.JOBS, team) && uc.getTechLevel(team) == 1) {
            uc.researchTechnology(Technology.HUTS);
        }

        if (uc.canResearchTechnology(Technology.COOKING) && uc.hasResearched(Technology.JOBS, team) && uc.getTechLevel(team) == 1) {
            uc.researchTechnology(Technology.COOKING);
        }

        if (uc.canResearchTechnology(Technology.EUGENICS) && uc.hasResearched(Technology.JOBS, team) && uc.getTechLevel(team) == 1) {
            uc.researchTechnology(Technology.EUGENICS);
        }

        if (uc.canResearchTechnology(Technology.NAVIGATION) && uc.hasResearched(Technology.JOBS, team) && uc.getTechLevel(team) == 1) {
            uc.researchTechnology(Technology.NAVIGATION);
        }

        if (uc.canResearchTechnology(Technology.SHARPENERS) && uc.hasResearched(Technology.JOBS, team) && uc.getTechLevel(team) == 1) {
            uc.researchTechnology(Technology.SHARPENERS);
        }
    }

    private void researchTechLevel2() {
        if (uc.canResearchTechnology(Technology.SCHOOLS) && uc.getTechLevel(team) == 2) {
            uc.researchTechnology(Technology.SCHOOLS);
            return;
        }

        if (uc.canResearchTechnology(Technology.CRYSTALS) && uc.getTechLevel(team) == 2) {
            uc.researchTechnology(Technology.CRYSTALS);
            return;
        }

        if (uc.canResearchTechnology(Technology.COMBUSTION) && uc.getTechLevel(team) == 2) {
            uc.researchTechnology(Technology.COMBUSTION);
            return;
        }

        if (uc.canResearchTechnology(Technology.POISON) && uc.getTechLevel(team) == 2) {
            uc.researchTechnology(Technology.POISON);
        }

        if (uc.canResearchTechnology(Technology.FLINT) && uc.getTechLevel(team) == 2) {
            uc.researchTechnology(Technology.FLINT);
        }

        if (uc.canResearchTechnology(Technology.HOUSES) && uc.getTechLevel(team) == 2) {
            uc.researchTechnology(Technology.HOUSES);
        }

        if (uc.canResearchTechnology(Technology.EXPERTISE) && uc.getTechLevel(team) == 2) {
            uc.researchTechnology(Technology.EXPERTISE);
        }
    }

    void spawnTroops() {
        if (explorers < MAX_EXPLORERS) {
            if (spawnRandom(UnitType.EXPLORER))
                explorers++;
        }

        if (workers < MAX_WORKERS) {
            if (spawnRandom(UnitType.WORKER))
                workers++;
        }
    }

    void decodeMessages() {
        int[] smokeSignals = uc.readSmokeSignals();
        Location currentLocation = uc.getLocation();
        DecodedMessage message;

        for (int smokeSignal : smokeSignals) {
            message = decodeSmokeSignal(currentLocation, smokeSignal);
            if (message != null) {
                if (message.unitCode == BUY_RAFTS) {
                    waterMode = true;
                    uc.println("water mode activated");
                }
            }
        }
    }



    void makeBuilders() {
        if (uc.hasResearched(Technology.JOBS, team)) {
            if (!hasAssignedWorkerAsBuilder) {
                makeBuilder();
                uc.println("making builder");
            }
        }
    }

    /**
     * Spawn a worker, get its id, and tell it to enter building mode.
     */
    private void makeBarrackBuilder() {
        // Cannot proceed unless worker is built.
        if (!spawnRandom(UnitType.WORKER)) {
            return;
        }

        UnitInfo[] allyUnits = uc.senseUnits(2, team);
        for (UnitInfo unit : allyUnits) {
            if (unit.getType() == UnitType.WORKER) {
                barrackBuilderId = unit.getID();
                break;
            }
        }

        // Builder is not assigned until smoke signal is sent.
        if (uc.canMakeSmokeSignal()) {
            uc.println(barrackBuilderId);
            uc.makeSmokeSignal(encodeSmokeSignal(barrackBuilderId, ASSIGN_BARRACK_BUILDER, 1));
            hasAssignedWorkerAsBarrackBuilder = true;
            uc.println("worker is now a builder");
        }
    }

    /**
     * Spawn a worker, get its id, and tell it to enter building mode.
     */
    private void makeBuilder() {
        // Cannot proceed unless worker is built.
        if (!spawnRandom(UnitType.WORKER)) {
            return;
        }

        UnitInfo[] allyUnits = uc.senseUnits(2, team);
        for (UnitInfo unit : allyUnits) {
            if (unit.getType() == UnitType.WORKER) {
                builderId = unit.getID();
                break;
            }
        }

        // Builder is not assigned until smoke signal is sent.
        if (uc.canMakeSmokeSignal()) {
            uc.println(builderId);
            uc.makeSmokeSignal(encodeSmokeSignal(builderId, ASSIGN_BUILDER, 1));
            hasAssignedWorkerAsBuilder = true;
            uc.println("worker is now a builder");
        }
    }
}
