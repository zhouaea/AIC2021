package ourplayer;

import aic2021.user.*;
import java.util.ArrayList; // TODO switch to priority queue later


public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    Team team = uc.getTeam();
    Team deerTeam = Team.NEUTRAL;

    ArrayList<ResourceInfo> found_resources; //ArrayList instead of queue in case we want to prioritize certain resources
    ArrayList<ResourceInfo> messagesToSend;

    int currentFoundResourceIndex;

    boolean isHunting = false;
    Location currentDeerLocation;

    boolean isMining = false;

    boolean isDepositing = false;
    boolean knowsPlaceToDeposit = false;
    Location depositLocation;

    int farms = 0;
    int sawmills = 0;
    int quarries = 0;

    void playRound(){
        // If just created, store team's base location.
        if (!knowsPlaceToDeposit) {
            rememberBaseLocation();
        }

        senseEnemies(); // All bytecode goes to running from enemies to base or fighting as a first priority.
        decodeResourceMessages(); // get locations of resources from other units
        senseResources(); // check local area for deer to hunt or add/remove resource locations to/from memory
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

    private void decodeResourceMessages() {
        int[] smokeSignals = uc.readSmokeSignals();
        Location currentLocation = uc.getLocation();
        DecodedMessage message;

        // If a smoke signal is from an ally and is for a resource location, add the resource to the found_resources.
        for (int smokeSignal : smokeSignals) {
            message = decodeSmokeSignal(currentLocation, smokeSignal);
            if (message != null)
                if (message.unitCode == WOOD)
                    found_resources.add(new ResourceInfo(Resource.WOOD, message.unitAmount, message.location));
                else if (message.unitCode == STONE)
                    found_resources.add(new ResourceInfo(Resource.STONE, message.unitAmount, message.location));
                else if (message.unitCode == FOOD)
                    found_resources.add(new ResourceInfo(Resource.FOOD, message.unitAmount, message.location));
        }
    }

    void senseResources() {
        // Look at resources in vision radius, if the current resource should be in sight (assuming unit is holding a torch) and it is not sensed, remove it.
        ResourceInfo[] resources = uc.senseResources();
        if (currentFoundResourceIndex > -1)
            if (uc.canSenseLocation(found_resources.get(currentFoundResourceIndex).location))
                uc.println(uc.senseResourceInfo(found_resources.get(currentFoundResourceIndex).location));
                if (uc.senseResourceInfo(found_resources.get(currentFoundResourceIndex).location) == null)
                    found_resources.remove(currentFoundResourceIndex);

        // If the found resources haven't been added yet, add them to the list, and if there is a large amount of
        // resources, save it as a message to send.
        for (ResourceInfo resource : resources) {
            if (!found_resources.contains(resource)) {
                found_resources.add(resource);

                if (resource.amount > 200) {
                    messagesToSend.add(resource);
                }
            }
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

    // When torch is almost finished, throw it on an adjacent square and light another torch with it.
    void keepTorchLit() {
        if (uc.getInfo().getTorchRounds() == 1) {
            if (dropTorch())
                if (uc.canLightTorch()) {
                    uc.lightTorch();
                }
                else
                    uc.println("SOMEHOW TORCH CAN NOT BE LIT");
        }
    }

    private boolean dropTorch() {
        Location current_location = uc.getLocation();
        int temp_x = current_location.x;
        int temp_y = current_location.y;

        // Directly above and below.
        temp_y++;
        if (tryToThrowTorch(new Location(temp_x, temp_y))) {
            return true;
        }

        temp_y -= 2;
        if (tryToThrowTorch(new Location(temp_x, temp_y))) {
            return true;
        }

        // Drop torch to the right.
        temp_x++;
        if (tryToThrowTorch(new Location(temp_x, temp_y))) {
            return true;
        }

        temp_y++;
        if (tryToThrowTorch(new Location(temp_x, temp_y))) {
            return true;
        }

        temp_y++;
        if (tryToThrowTorch(new Location(temp_x, temp_y))) {
            return true;
        }

        // Drop torch to the left.
        temp_x -=2;
        if (tryToThrowTorch(new Location(temp_x, temp_y))) {
            return true;
        }

        temp_y--;
        if (tryToThrowTorch(new Location(temp_x, temp_y))) {
            return true;
        }

        temp_y--;
        return tryToThrowTorch(new Location(temp_x, temp_y));
    }

    private boolean tryToThrowTorch(Location location) {
        if (uc.canThrowTorch(location)) {
            uc.throwTorch(location);
            return true;
        }
        return false;
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

    void mine() {
        // For now, if workers fills up on one resource, they go back to base.
        if (uc.canGatherResources()) {
            uc.gatherResources();

            // If the unit can sit on the resource for 10 turns, send a smoke signal on a location it has seen.
            if ((found_resources.get(0).amount > 100 && uc.hasResearched(Technology.BOXES, team)) || (found_resources.get(0).amount > 200 && uc.hasResearched(Technology.BOXES, team)))
                if (!messagesToSend.isEmpty())
                    if (uc.canMakeSmokeSignal())
                        // right now we just send with FIFO principle.
                        uc.makeSmokeSignal(encodeResourceMessage(messagesToSend.remove(0)));

        // If the unit can't get resources because there are no more left, the location is no longer valid, so remove it
        // from the list of found locations and as the currentLocation being travelled to.
        } else if (max(uc.getResourcesCarried()) < GameConstants.MAX_RESOURCE_CAPACITY || (max(uc.getResourcesCarried()) < GameConstants.MAX_RESOURCE_CAPACITY_BOXES && uc.hasResearched(Technology.BOXES, team))) {
            found_resources.remove(currentFoundResourceIndex);
            currentFoundResourceIndex = -1;
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

    private int encodeResourceMessage(ResourceInfo resourceInfo) {
        if (resourceInfo.resourceType == Resource.WOOD)
            return encodeSmokeSignal(resourceInfo.location, WOOD, resourceInfo.amount);
        else if (resourceInfo.resourceType == Resource.STONE)
            return encodeSmokeSignal(resourceInfo.location, STONE, resourceInfo.amount);
        else
            return encodeSmokeSignal(resourceInfo.location, FOOD, resourceInfo.amount);
    }

    void moveToResource() {
        // Location is already set, so just pathfind to it.
        if (currentFoundResourceIndex > -1) {
            bug2(found_resources.get(currentFoundResourceIndex).location);
            return;
        }

        // If no resource locations stored, just explore.
        if (found_resources.isEmpty()) {
            explore();
            return;
        }

        // If the list of found locations is not empty and a location isn't set, set target location to the first one
        // in the resource_list.
        currentFoundResourceIndex = 0;
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
