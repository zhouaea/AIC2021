package ourplayer;

import aic2021.user.*;
import java.util.ArrayList; // TODO switch to priority queue later


public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    Team team = uc.getTeam();

    ArrayList<Location> broadcasted_locations;
    ArrayList<Location> found_locations;

    int torch_rounds = 0;

    int farms = 0;
    int sawmills = 0;
    int quarries = 0;

    void playRound(){
        // If just created, remember base location.
        senseEnemies(); // All bytecode goes to running from enemies to base or fighting as a first priority.
        decodeMessages(); // get locations of resources from other units
        senseResources();
        lightTorch();
        gatherResources(); // movement options
        spawnBuildings(); // once jobs is unlocked, spawn 20 buildings max

        torch_rounds--;
    }

    void senseEnemies() {

    }

    void senseResources() {
        // UnitInfo[] deer = uc.senseUnits(neutral_team);
    }

    void decodeMessages() {}

    void lightTorch() {
        // Relight torch if possible after half of torch is burned.
        if (uc.canLightTorch() ) {
            uc.lightTorch();
            torch_rounds = 75;
        }
    }

    void gatherResources() {
        // If next to base, deposit any resources collected. Light a torch as well.
        if (uc.canDeposit()) {
            uc.deposit();
        }

        // If on resource, collect it, and send a smoke signal on found resources in the area.
        // Alternatively, send a smoke signal on a location where the resources are depleted.
        // Only send if there are enough resources to sit for 10 turns?
        if (uc.canGatherResources()) {
            uc.gatherResources();
            // send_signal()
        }

        Location resource_location;

        // If carrying enough resources, pathfind to base and deposit resource.
        if (passesResourceThreshold()) {
            //pathfind(base_location)
        }
        // If resource is in vision, move towards it. If this location hasn't been broadcasted, store it as found.
        else if ((resource_location = scanForResources()) != null) {
            //pathfind(resource_location)
        }
        // If looking for resources and none in sight, pathfind to the closest resource communicated or found earlier.
        else if (broadcasted_locations.size() != 0) {
            //pathfind(broadcasted_locations.get(0))
        }
        else if (found_locations.size() != 0) {
            //pathfind(found_locations.get(0))
        }
        // If no broadcasted or recently found locations, and no resources in sight, explore.
        else
            //explore();

    }

    boolean passesResourceThreshold() {
        // For now, if they fill up on one resource, workers go back to base.
        for (int resource_count : uc.getResourcesCarried()) {
            if (resource_count == 100)
                return true;
        }
        return false;
    }

    // Returns location with most resources in vision radius, if any. If none, return null.
    Location scanForResources() {
        ResourceInfo[] sensed_resources = uc.senseResources();

        if (sensed_resources.length == 0) {
            return null;
        }

        int max_resource = 0;
        Location resource_location = null;
        for (ResourceInfo resource: sensed_resources) {
            if (resource.amount > max_resource) {
                max_resource = resource.amount;
                resource_location = resource.location;
            }
        }

        return resource_location;
    }


    void spawnBuildings() {
        if (uc.hasResearched(Technology.JOBS, team)) {
            if (farms <= 7) {
                if (spawnRandom(UnitType.FARM)) farms++;
            }

            if (sawmills <= 7) {
                if (spawnRandom(UnitType.SAWMILL)) sawmills++;
            }

            if (quarries <= 7) {
                if (spawnRandom(UnitType.QUARRY)) quarries++;
            }
        }
    }

}
