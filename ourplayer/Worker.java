package ourplayer;

import aic2021.user.*;
import java.util.ArrayList; // TODO switch to priority queue later


public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    Team team = uc.getTeam();
    Team deerTeam = Team.NEUTRAL;

    ArrayList<ResourceInfo> found_locations;

    int currentResourceLocationIndex;

    boolean isMining = false;

    boolean isHunting = false;
    Location currentDeerLocation;

    boolean isDepositing = false;
    boolean knowsPlaceToDeposit = false;
    Location depositLocation;


    int torch_rounds = 0;

    int farms = 0;
    int sawmills = 0;
    int quarries = 0;

    void playRound(){
        // If just created, store team's base location.
        if (!knowsPlaceToDeposit) {
            rememberBaseLocation();
        }

        senseEnemies(); // All bytecode goes to running from enemies to base or fighting as a first priority.
        decodeMessages(); // get locations of resources from other units
        senseResources(); // check local area and hunt a deer or add/remove resource locations to/from memory
        keepTorchLit();

        // Hunting takes precedence over depositing (can kill deer on the way).
        if (isHunting)
            hunt();
        else if (isDepositing)
            deposit();
        else if (isMining)
            mine();
        else
            moveToResource(); // movement options

        spawnBuildings(); // once jobs is unlocked, spawn 20 buildings max

        torch_rounds--;
    }

    void rememberBaseLocation() {
        UnitInfo[] surroundingUnits = uc.senseUnits(team);
        for (UnitInfo unit : surroundingUnits) {
            if (unit.getType() == UnitType.BASE) {
                depositLocation = unit.getLocation();
            }
        }

        knowsPlaceToDeposit = true;
    }

    void senseEnemies() {

    }

    void decodeMessages() {

    }

    void senseResources() {
        // Look at resources in vision radius, if the current resource should be in sight (assuming unit is holding a torch) and it is not sensed, remove it.
        ResourceInfo[] resources = uc.senseResources();
        if (currentResourceLocationIndex > -1)
            if (uc.canSenseLocation(found_locations.get(currentResourceLocationIndex).location))
                uc.println(uc.senseResourceInfo(found_locations.get(currentResourceLocationIndex).location));
                if (uc.senseResourceInfo(found_locations.get(currentResourceLocationIndex).location) == null)
                    found_locations.remove(currentResourceLocationIndex);

        // If the found resources haven't been added yet, add them to the list.
        for (ResourceInfo resource : resources) {
            if (!found_locations.contains(resource))
                found_locations.add(resource);
        }

        // If deer is found, engage in hunting mode and kill the nearest one in sight.
        UnitInfo[] deer = uc.senseUnits(deerTeam);

        if (deer != null) {
            currentDeerLocation = deer[0].getLocation();
            isHunting = true;
        } else {
            isHunting = false; // Deer was killed
        }
    }

    void keepTorchLit() {
        // Relight torch if possible after half of torch is burned.
        if (uc.canLightTorch() ) {
            uc.throwTorch(uc.);
            uc.lightTorch();
            torch_rounds = 75;
        }
    }

    void deposit() {
        bug2(baseLocation);
        if (uc.canDeposit()) {
            uc.deposit();
            isDepositing = false;
        }
    }

    void hunt() {
        if (uc.canAttack(currentDeerLocation)) {
            uc.attack(currentDeerLocation);
        }

        goToLocation(currentDeerLocation);

        if (uc.canAttack(currentDeerLocation)) {
            uc.attack(currentDeerLocation);
        }
    }

    void moveToResource() {
        // Location already set.
        if (currentResourceLocationIndex > -1) {
            bug2(found_locations.get(currentResourceLocationIndex).location);
            return;
        }

        // If no resource locations stored, just explore.
        if (found_locations.isEmpty()) {
            explore();
            return;
        }

        // If the list of found locations is not empty, set target location to the first one in the list.
        currentResourceLocationIndex = 0;
    }

    void mine() {
        // TODO If on resource, collect it, and send a smoke signal on found resources in the area.
        // Alternatively, send a smoke signal on a location where the resources are depleted.
        // Only send if there are enough resources to sit for 10 turns?

        // For now, if they fill up on one resource, workers go back to base.
        if (uc.canGatherResources()) {
            uc.gatherResources();
        // If the unit can't get resources because there are no more left, the location is no longer valid, so remove it
        // from the list of found locations and as the currentLocation being travelled to.
        } else if (max(uc.getResourcesCarried()) < GameConstants.MAX_RESOURCE_CAPACITY ||
                (max(uc.getResourcesCarried()) < GameConstants.MAX_RESOURCE_CAPACITY_BOXES && uc.hasResearched(Technology.BOXES, team))) {
                found_locations.remove(currentResourceLocationIndex);

            currentResourceLocationIndex = -1;
        // If the unit can't get resources because it is at its carrying capacity, deposit resources at base.
        } else {
            isMining = false;
            isDepositing = true;
        }
    }

    private int max(int[] array) {
        int max = 0;
        for (int i : array) {
            if (i > max)
                max = i;
        }

        return max;
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
