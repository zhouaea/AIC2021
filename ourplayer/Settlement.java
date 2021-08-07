package ourplayer;

import aic2021.user.*;

public class Settlement extends MyUnit {

    Team team = uc.getTeam();
    Location location = uc.getLocation();

    boolean hasAnnouncedCreation = false;

    boolean waterMode = false;
    final int WATER_TILE_THRESHOLD = 1;

    final int ResourcesPerWorkerSpawnThreshold = 400;

    boolean broadcastEnemyBaseLocation = false;
    final int CHANCE_OF_BROADCASTING_ENEMY_BASE_LOCATION = 100;

    Settlement(UnitController uc){
        super(uc);
    }

    void playRound() {
        if (!hasAnnouncedCreation) {
            announceCreation();
            checkForWater();
        }


        // Only spawn workers before a certain threshold
        if (uc.getRound() < 500 || uc.getTechLevel(team) == 0) {
            // Make sure that spawned workers can reach resources
            if (!waterMode || uc.hasResearched(Technology.RAFTS, team))
                spawnWorkers();
        }
        decodeMessages();
        sendEnemyBaseLocation();
    }

    void announceCreation() {
        if (uc.canMakeSmokeSignal()) {
            uc.makeSmokeSignal(encodeSmokeSignal(0, SETTLEMENT_CREATED, 0));
            hasAnnouncedCreation = true;
            uc.println("presence announced");
        }
    }

    void checkForWater() {
        Location[] waterTiles = uc.senseWater(25);
        if (waterTiles.length >= WATER_TILE_THRESHOLD) {
            waterMode = true;
            uc.println("water mode activated");
        }
    }

    /**
     * Spawn a worker if the ratio of resources to workers is high.
     */
    void spawnWorkers() {
        UnitInfo[] allyUnits = uc.senseUnits(16, team);
        int workersInArea = 0;
        for (UnitInfo unit: allyUnits) {
            if (unit.getType() == UnitType.WORKER) {
                workersInArea++;
            }
        }


        ResourceInfo[] resources = uc.senseResources();
        double totalResourceAmount = 0;

        for (ResourceInfo resource: resources) {
            // Don't count a resource under a settlement.
            if (!locationHasSettlement(resource.location))
                totalResourceAmount += resource.amount;
        }

        if (workersInArea == 0) {
            // If there are no workers in the area but there are resources, spawn a worker.
            if (totalResourceAmount != 0) {
                spawnRandom(UnitType.WORKER);
            }
        } else {
            // if there are resources in the area, and not enough workers per resource, spawn a worker.
            if (totalResourceAmount / workersInArea > ResourcesPerWorkerSpawnThreshold) {
                spawnRandom(UnitType.WORKER);
            }
        }
    }

    private boolean locationHasSettlement(Location resourceLocation) {
        if (uc.canSenseLocation(resourceLocation)) {
            UnitInfo unitAtLocation = uc.senseUnitAtLocation(resourceLocation);
            // Worker does not exist.
            if (unitAtLocation == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    void decodeMessages() {
        int[] smokeSignals = uc.readSmokeSignals();
        Location currentLocation = uc.getLocation();
        DecodedMessage message;

        for (int smokeSignal : smokeSignals) {
            message = decodeSmokeSignal(currentLocation, smokeSignal);
            if (message != null) {
                if (message.unitCode == ENEMY_BASE)
                    enemyBaseLocation = message.location;
            }
        }
    }

    /**
     * If the settlement knows the location of the enemy base, broadcast it every 1/100 turns.
     */
    void sendEnemyBaseLocation() {
        if (enemyBaseLocation != null) {
            int randomNumber = (int) (uc.getRandomDouble() * CHANCE_OF_BROADCASTING_ENEMY_BASE_LOCATION);
            if (randomNumber == 0) {
                broadcastEnemyBaseLocation = true;
            }

            if (broadcastEnemyBaseLocation) {
                if (uc.canMakeSmokeSignal()) {
                    uc.makeSmokeSignal(encodeSmokeSignal(enemyBaseLocation, ENEMY_BASE, 1));
                    broadcastEnemyBaseLocation = false;
                    uc.println("relaying enemy base location");
                }
            }
        }
    }
}