// TODO some workers may kill themselves for some reason...
package ourplayer;

import aic2021.engine.Unit;
import aic2021.user.*;
import java.util.ArrayList;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    Team team = uc.getTeam();
    Team deerTeam = Team.NEUTRAL;

    ArrayList<ResourceInfo> found_resources = new ArrayList<>(); //ArrayList instead of queue in case we want to prioritize certain resources
    ArrayList<ResourceInfo> messagesToSend = new ArrayList<>();
    int currentFoundResourceIndex = -1;

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
        uc.println(uc.getEnergyLeft());
        decodeResourceMessages(); // get locations of resources from other units
        uc.println(uc.getEnergyLeft());
        senseResources(); // add/remove resource locations to/from memory
        senseDeer(); //  check local area for deer to hunt
        uc.println(uc.getEnergyLeft());
        keepTorchLit();
        uc.println(uc.getEnergyLeft());

        // Hunting takes precedence over depositing (can kill deer on the way).
        if (isHunting) {
            hunt();
            uc.println("hunt");
        } else if (isDepositing) {
            deposit();
            uc.println("deposit");
        } else if (isMining) {
            mine();
            uc.println("mine");
        } else {
            moveToResource(); // movement options
            uc.println("movetoresource");
        }

        uc.println(uc.getEnergyLeft());

        spawnBuildings(); // once jobs is unlocked, spawn 20 buildings max
        uc.println(uc.getEnergyLeft());
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
        if (currentFoundResourceIndex > -1) {
            Location currentResourceLocation = found_resources.get(currentFoundResourceIndex).location;

            // If the target resource location can be sensed and has no resources or another worker is currently
            // on the resource, a new target needs to be found.
            if (locationHasNoResources(currentResourceLocation) || locationHasAnotherWorker(currentResourceLocation)) {
                found_resources.remove(currentFoundResourceIndex);
                currentFoundResourceIndex = -1;
                closestLocation = null; // Reset pathfinding manually after worker changes target.
            }
        }

        // If sensed resources haven't been added yet, add them to the list, and if there is a large amount of
        // resources, save it as a message to send.
        ResourceInfo[] resources = uc.senseResources();
        for (ResourceInfo resource : resources) {
            if (!alreadyRecordedResource(resource)) {
                found_resources.add(resource);

                if (resource.amount > 200) {
                    messagesToSend.add(resource);
                }
            }
        }
    }

    /**
     * Helper function for senseResources().
     * @param resourceLocation is the location to be checked for resources
     * @return true if the location can be sensed by the unit and has no resources
     */
    boolean locationHasNoResources(Location resourceLocation) {
        if (uc.canSenseLocation(resourceLocation)) {
            ResourceInfo[] resourcesAtLocation = uc.senseResourceInfo(resourceLocation);
            for (ResourceInfo resource : resourcesAtLocation) {
                // Resource exists.
                if (resource != null) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    boolean locationHasAnotherWorker(Location resourceLocation) {
        if (uc.canSenseLocation(resourceLocation) && !uc.getLocation().isEqual(resourceLocation)) {
            UnitInfo unitAtLocation = uc.senseUnitAtLocation(resourceLocation);
            // Worker does not exist.
            if (unitAtLocation == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean alreadyRecordedResource(ResourceInfo resource) {
        for (ResourceInfo info : this.found_resources) {
            if (info.resourceType == resource.resourceType && sameLocation(info.location, resource.location)) {
                return true;
            }
        }
        return false;
    }

    boolean sameLocation(Location loc1, Location loc2) {
        return loc1.x == loc2.x && loc1.y == loc2.y;
    }

    /**
     * If deer is/are found, engage in hunting mode on the closest one in sight.
     */
    void senseDeer() {
        UnitInfo[] deer = uc.senseUnits(deerTeam);

        if (deer.length != 0) {
            currentDeerLocation = deer[0].getLocation();
            isHunting = true;
            bug2(uc.getLocation(), deer[0].getLocation());
        } else {
            if (isHunting) {
                isHunting = false; // Deer was killed
                closestLocation = null; // Reset pathfinding manually after worker is done chasing deer
            }
        }
    }


    /**
     * When torch is almost finished, throw it on an adjacent square and light another torch with it.
     */
    void keepTorchLit() {
        int torchRounds = uc.getInfo().getTorchRounds();
        // Light torch when spawned.
        if (torchRounds == 0) {
            if (uc.canLightTorch())
                uc.lightTorch();
        }
        // After initial torch light, throw torch on ground and light a new one when the torch is almost depleted.
        // TODO: Check limit
        else if (torchRounds <= 5) {
            if (dropTorch())
                if (uc.canLightTorch()) {
                    uc.lightTorch();
                }
                else
                    uc.println("SOMEHOW TORCH CAN NOT BE LIT");
        }
    }

    /**
     * Attempt to throw the unit's torch at an adjacent tile.
     * @return whether or not unit was able to throw a torch on an adjacent tile
     */
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

    /**
     * Helper function for drop torch.
     * @param location to throw torch
     * @return whether or not torch was able to be thrown
     */
    private boolean tryToThrowTorch(Location location) {
        if (uc.canThrowTorch(location)) {
            uc.throwTorch(location);
            return true;
        }
        return false;
    }

    /**
     * Pathfind to the unit's chosen deposit location, and deposit resources once arrived.
     * Additionally, spawn a barrack next to the deposit location if it doesn't exist already.
     */
    void deposit() {
        // Unit can only deposit once unit has reached its deposit location.
        if (bug2(uc.getLocation(), depositLocation)) {
            if (uc.canDeposit()) {
                // Ensure that one barrack is placed near the unit's deposit location once barracks are unlocked.
                if (uc.hasResearched(Technology.MILITARY_TRAINING, team)) {
                    if (!checkForBarrack())
                        spawnRandom(UnitType.BARRACKS);
                }

                uc.deposit();
                isDepositing = false;
            }
        }
    }

    /**
     *  Helper function for deposit
     *  @return if there is a barrack in the unit's vision radius
     */
    private boolean checkForBarrack() {
        UnitInfo[] unitsNearby = uc.senseUnits();
        for (UnitInfo unit : unitsNearby) {
            if (unit.getTeam() == team) {
                if (unit.getType() == UnitType.BARRACKS)
                    return true;
            }
        }

        return false;
    }

    /**
     * Attempt to attack and move to deer in vision radius.
     */
    void hunt() {
        if (uc.canAttack(currentDeerLocation)) {
            uc.attack(currentDeerLocation);
        }

        bug2(uc.getLocation(), currentDeerLocation);

        if (uc.canAttack(currentDeerLocation)) {
            uc.attack(currentDeerLocation);
        }
    }

    /**
     *
     */
    void mine() {
        // TODO come up with better mechanism for units to deposit
        // For now, if workers fills up on one resource, they go back to base.
        int maxResourcesCarried = max(uc.getResourcesCarried());

        uc.println("can gather resources is -->");
        uc.println(uc.canGatherResources());
        // Gather resources as long as worker has not filled up on resource and there are still resources left.
        if (uc.canGatherResources() && max(uc.senseResourceInfo(uc.getLocation())) > 0 && ((maxResourcesCarried < 100 && !uc.hasResearched(Technology.BOXES, team)) || ((maxResourcesCarried < 200 && uc.hasResearched(Technology.BOXES, team))))) {
            int maxResourceAmountAtLocation = max((uc.senseResourceInfo(uc.getLocation())));

            // If the unit can sit on a resource for 10 turns and gather, send a smoke signal about a location it has seen.
            // TODO the logic of the if statement doesn't cover all cases...
            if (!messagesToSend.isEmpty()) {
                if (maxResourcesCarried == 0 && ((maxResourceAmountAtLocation >= 100 && !uc.hasResearched(Technology.BOXES, team)) || (maxResourceAmountAtLocation >= 200 && uc.hasResearched(Technology.BOXES, team)))) {
                    if (uc.canMakeSmokeSignal())
                        // right now we just send with FIFO principle.
                        uc.makeSmokeSignal(encodeResourceMessage(messagesToSend.remove(0)));
                }
            }

            uc.gatherResources();
            uc.println("gathering resources");
        // If the unit can't get resources, deposit resources at base.
        } else {
            isMining = false;
            isDepositing = true;
            uc.println("at carrying capacity");
        }
    }

    /**
     * Helper function for mine().
     * Exists since multiple resources can be stacked on top of eachother (e.g. deer dies on a forest).
     * @param array of resources on a tile
     * @return the amount of the most plentiful reosurce on the tile
     */
    private int max(ResourceInfo[] array) {
        int max = 0;
        for (ResourceInfo i : array) {
            if (i != null && i.amount > max)
                max = i.amount;
        }

        return max;
    }

    /**
     * Helper function for mine().
     * Exists since we are determining whether a worker goes to deposit if it fills up on one of its resources.
     * @param array of resources a worker has
     * @return the amount of the most plentiful resource in the unit's possession
     */
    private int max(int[] array) {
        int max = 0;
        for (int i : array) {
            if (i > max)
                max = i;
        }

        return max;
    }

    void moveToResource() {
        // Location is already set, so just pathfind to it.
        if (currentFoundResourceIndex > -1) {
            uc.println("target location: " + found_resources.get(currentFoundResourceIndex).location);
            if (bug2(uc.getLocation(), found_resources.get(currentFoundResourceIndex).location))
                isMining = true;
            return;
        }

        // If no resource locations stored, just explore.
        if (found_resources.isEmpty()) {
            moveRandom();
            //explore();
            return;
        }

        // If the list of found locations is not empty and a location isn't set, set target location to the first one
        // in the resource_list.
        currentFoundResourceIndex = 0;
        bug2(uc.getLocation(), found_resources.get(currentFoundResourceIndex).location);
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
