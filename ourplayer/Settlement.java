package ourplayer;

import aic2021.user.*;

public class Settlement extends MyUnit {

    Team team = uc.getTeam();

    final int ResourcesPerWorkerSpawnThreshold = 400;

    Settlement(UnitController uc){
        super(uc);
    }

    void playRound() {
        spawnWorkers();
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
        // TODO make sure resource is not under
        for (ResourceInfo resource: resources) {
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
}