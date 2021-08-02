package ourplayer;

import aic2021.user.*;

import java.util.ArrayList;

public class Base extends MyUnit {

    int workers = 0;
    int explorers = 0;
    final int MAX_WORKERS = 5;
    final int MAX_EXPLORERS = 0;

    Team team = uc.getTeam();
    Team enemy_team = uc.getOpponent();

    final int MAX_BUILDINGS_PLACED = 20;
    boolean hasCalculatedBuildingLocations = false;
    ArrayList<Location> buildingLocations = new ArrayList<>();
    int buildingLocationsAdded = 0;
    int buildingLocationIndex = 0;

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        spawnTroops();
        playDefense();
        senseEnemies();
        senseResources();
        researchTech();
        sendBuildingLocations();


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
        if (uc.canResearchTechnology(Technology.UTENSILS)) {
            uc.researchTechnology(Technology.UTENSILS);
            return;
        }
        if (uc.canResearchTechnology(Technology.MILITARY_TRAINING) && uc.hasResearched(Technology.UTENSILS, team)) {
            uc.researchTechnology(Technology.MILITARY_TRAINING);
            return;
        }
        if (uc.canResearchTechnology(Technology.BOXES) && uc.hasResearched(Technology.UTENSILS, team) && uc.hasResearched(Technology.MILITARY_TRAINING, team)) {
            uc.researchTechnology(Technology.BOXES);
            return;
        }
    }

    private void researchTechLevel1() {
        if (uc.canResearchTechnology(Technology.TACTICS)) {
            uc.researchTechnology(Technology.TACTICS);
            return;
        }

        if (uc.canResearchTechnology(Technology.JOBS) && uc.hasResearched(Technology.TACTICS, team)) {
            uc.researchTechnology(Technology.JOBS);
            return;
        }

        if (uc.canResearchTechnology(Technology.COOKING) && uc.hasResearched(Technology.TACTICS, team) && uc.hasResearched(Technology.JOBS, team)) {
            uc.researchTechnology(Technology.COOKING);
            return;
        }
    }

    private void researchTechLevel2() {
        if (uc.canResearchTechnology(Technology.SCHOOLS)) {
            uc.researchTechnology(Technology.SCHOOLS);
            return;
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

    // TODO send smoke signals to workers about resources in range
    void senseTerrain() {
        // Sense terrain in range, and maybe let troops know about important spots.
        Location[] water_tiles = uc.senseWater(50);
        Location[] mountain_tiles = uc.senseMountains(50);
    }


    void sendBuildingLocations() {
        if (!hasCalculatedBuildingLocations) {
            calculateBuildingLocations();
        }

        // Send a building location to all workers when possible.
        if (uc.canMakeSmokeSignal() && buildingLocationIndex < buildingLocations.size()) {
            uc.makeSmokeSignal(encodeBuildingLocation(buildingLocations.get(buildingLocationIndex)));
            buildingLocationIndex++;
            uc.println("Building Location Sent");
        }
    }

    private void calculateBuildingLocations() {
        Location baseLocation = uc.getLocation();
        int base_x_parity = baseLocation.x % 2;
        int base_y_parity = baseLocation.y % 2;

        Location[] visibleLocations = uc.getVisibleLocations();
        for (Location location : visibleLocations) {
            // Limit the number of buildings that will be placed.
            if (buildingLocationsAdded > MAX_BUILDINGS_PLACED) {
                break;
            }

            // Building location is not the base location
            if (!location.isEqual(baseLocation)) {
                // Building locations must be part of the lattice structure.
                if ((location.x % 2 == base_x_parity && location.y % 2 == base_y_parity) || location.x % 2 != base_x_parity && location.y % 2 != base_y_parity) {
                    // Only select locations where buildings can be placed.
                    if (!uc.hasMountain(location) && !uc.hasWater(location) && !uc.isOutOfMap(location)) {
                        buildingLocations.add(location);
                        buildingLocationsAdded++;
                    }
                }
            }
        }
        hasCalculatedBuildingLocations = true;
    }
}
