package ourplayer;

import aic2021.user.*;

public class Settlement extends MyUnit {

    Team team = uc.getTeam();
    Location location = uc.getLocation();

    final int ResourcesPerWorkerSpawnThreshold = 400;

    boolean broadcastEnemyBaseLocation = false;
    final int CHANCE_OF_BROADCASTING_ENEMY_BASE_LOCATION = 100;

    Settlement(UnitController uc){
        super(uc);
    }

    void playRound() {
        spawnWorkers();
        decodeMessages();
        sendEnemyBaseLocation();
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
            if (location != resource.location)
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
            int randomNumber = (int) (uc.getRandomDouble() * 100);
            if (randomNumber == 0) {
                broadcastEnemyBaseLocation = true;
            }

            if (broadcastEnemyBaseLocation) {
                if (uc.canMakeSmokeSignal()) {
                    uc.makeSmokeSignal(encodeSmokeSignal(enemyBaseLocation, ENEMY_BASE, 1));
                    broadcastEnemyBaseLocation = false;
                }
            }
        }
    }
}