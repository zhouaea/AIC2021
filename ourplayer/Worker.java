package ourplayer;

import aic2021.user.*;
import java.util.ArrayList;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    Team team = uc.getTeam();
    Team enemy = uc.getOpponent();
    Team deerTeam = Team.NEUTRAL;

    ArrayList<ResourceInfo> found_resources = new ArrayList<>(); //ArrayList instead of queue in case we want to prioritize certain resources
    ArrayList<ResourceInfo> low_priority_resources = new ArrayList<>();
    ArrayList<ResourceInfo> messagesToSend = new ArrayList<>();
    int currentFoundResourceIndex = -1;
    final int MAX_RESOURCES_TO_REMEMBER = 5;

    final int RESOURCE_THRESHOLD_FOR_SETTLEMENT = 300;

    final int MAX_BUILDINGS_PLACED = 20;
    boolean hasCalculatedBuildingLocations = false;
    ArrayList<Location> buildingLocations = new ArrayList<>();
    int buildingLocationsAdded = 0;

    boolean isFightingEnemyWorkers = false;
    Location currentEnemyWorkerLocation;

    boolean isHunting = false;
    Location currentDeerLocation;

    boolean isMining = false;

    boolean isBuilding = false;
    boolean isBarrackBuilding = false;

    boolean isDepositing = false;
    boolean knowsPlaceToDeposit = false;
    Location depositLocation;

    int randomDirectionIndex = (int) (uc.getRandomDouble() * 9);
    Direction currentDir = dirs[randomDirectionIndex];

    void playRound(){
        if (uc.getInfo().getID() == 4543 && uc.getRound() == 192) {
            uc.println("hello");
        }
        // TODO lattice structure can mess with bug2, we need a different pathfinding algorithm for that
        // TODO use DFS for builder pathfinding instead of bug2, since bug2 will go around entire map to reach location sometimes
        // TODO come up with faster way for builder to place buildings along lattice structure
        // TODO Don't add resources in the range of the enemy base.
        // If just created, store team's base location.
        if (!knowsPlaceToDeposit) {
            rememberDepositLocation();
        }

        senseEnemies(); // All bytecode goes to running from enemies to base or fighting as a first priority.
        decodeMessages(); // get locations of resources from other units or a place to build a building from the base
        senseResources(); // add/remove resource locations to/from memory
        senseEnemyWorkers();
        senseDeer(); //  check local area for deer to hunt
        keepTorchLit();

        if (isBarrackBuilding) {
            spawnBarrack();
        } else if (isBuilding) {
            spawnBuildings(); // once jobs is unlocked, spawn 20 buildings max
            uc.println("building");
        }
        else if (isFightingEnemyWorkers) {
            fightEnemyWorkers();
            uc.println("fighting enemy workers");
        }
        // Hunting takes precedence over depositing (can kill deer on the way).
        else if (isHunting) {
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
    }

    void rememberDepositLocation() {
        UnitInfo[] surroundingUnits = uc.senseUnits(team);
        for (UnitInfo unit : surroundingUnits) {
            if (unit.getType() == UnitType.BASE || unit.getType() == UnitType.SETTLEMENT) {
                depositLocation = unit.getLocation();
            }
        }

        knowsPlaceToDeposit = true;
    }

    void senseEnemies() {

    }

    /**
     * If a smoke signal is from an ally and is for a resource location, add the encoded resource to broadcasted_resources.
     * Alternatively, if the signal is for a building location, add the encoded location to buildingLocations.
     */
     void decodeMessages() {
        int[] smokeSignals = uc.readSmokeSignals();
        Location currentLocation = uc.getLocation();
        DecodedMessage message;

        for (int smokeSignal : smokeSignals) {
            message = decodeSmokeSignal(currentLocation, smokeSignal);
            if (message != null) {
                if (message.unitCode == WOOD)
                    low_priority_resources.add(new ResourceInfo(Resource.WOOD, message.unitAmount, message.location));
                else if (message.unitCode == STONE)
                    low_priority_resources.add(new ResourceInfo(Resource.STONE, message.unitAmount, message.location));
                else if (message.unitCode == FOOD)
                    low_priority_resources.add(new ResourceInfo(Resource.FOOD, message.unitAmount, message.location));
                else if (message.unitCode == ENEMY_BASE)
                    enemyBaseLocation = message.location;
                else if (message.unitCode == ASSIGN_BARRACK_BUILDER) {
                    if (uc.getInfo().getID() == message.unitId) {
                        isBarrackBuilding = true;
                    }
                } else if (message.unitCode == ASSIGN_BUILDER) {
                    // If the assign builder message refers to the worker's id, they are the designated builder.
                    if (uc.getInfo().getID() == message.unitId) {
                        isBuilding = true;
                    }
                } else if (message.unitCode == BUILDING_LOCATION) {
                    buildingLocations.add(message.location);
                }
            }
        }
    }

    void senseResources() {
        // If there is a deposit in range that is closer than the current deposit, set a new deposit location.
        updateDeposit();

        // If sensed resources haven't been added yet, add them to the list, and if there is a large amount of
        // resources, save it as a message to send.
        int resourceInArea = 0;
        ResourceInfo[] resources = uc.senseResources();
        for (ResourceInfo resource : resources) {
            resourceInArea += resource.amount;
            if (found_resources.size() < MAX_RESOURCES_TO_REMEMBER) {
                if (!alreadyRecordedResource(resource)) {
                    // Only add the resource if there isn't a worker on it already, and it is safe to move to.
                    if (!locationHasAnotherWorker(resource.location) && isSafeToMove(resource.location)) {
                        found_resources.add(resource);
                    }

                    if (resource.amount >= 200) {
                        messagesToSend.add(resource);
                    }
                }
            }
        }

        // If the total amount of resources in sight exceeds a threshold, build a settlement, if a deposit isn't nearby.
        if (resourceInArea >= RESOURCE_THRESHOLD_FOR_SETTLEMENT && !checkForDeposit()) {
            spawnSettlement();
        }

        // If the worker is set on a resource target
        if (currentFoundResourceIndex > -1) {
            // Constantly adapt resource target to the resource closest to the worker.
            currentFoundResourceIndex = findClosestResourceIndex();
            uc.println("size of found resources: " + found_resources.size());

            Location currentResourceLocation = found_resources.get(currentFoundResourceIndex).location;
            // If the target resource location can be sensed and has no resources,another worker is currently
            // on the resource, or it is unsafe to move to, a new target needs to be found.
            if (locationHasNoResources(currentResourceLocation) || locationHasAnotherWorker(currentResourceLocation) || !isSafeToMove(currentResourceLocation)) {
                found_resources.remove(currentFoundResourceIndex);
                currentFoundResourceIndex = -1;
                closestLocation = null; // Reset pathfinding manually after worker changes target.
            }
        }
    }

    /**
     * Helper function for senseResources().
     * @param resourceLocation is the location to be checked for resources
     * @return true if the location can be sensed by the unit and has no resources
     */
    private boolean locationHasNoResources(Location resourceLocation) {
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

    /**
     * Randomly place settlement, prioritizing a location with no resources on it.
     */
    private void spawnSettlement() {
        Location currentLocation = uc.getLocation();
        Location potentialLocation;

        for (Direction dir : dirs) {
            potentialLocation = currentLocation.add(dir);
            if (uc.canSpawn(UnitType.SETTLEMENT, dir)) {
                // Try to place settlement on a location with no resources.
                if (max(uc.senseResourceInfo(potentialLocation)) == 0) {
                    uc.spawn(UnitType.SETTLEMENT, dir);
                    return;
                }
            }
        }

        // If there are no adjacent locations without resources, place the settlement anywhere.
        for (Direction dir : dirs){
            if (!uc.isOutOfMap(currentLocation.add(dir)) && uc.canSpawn(UnitType.SETTLEMENT, dir)){
                uc.spawn(UnitType.SETTLEMENT, dir);
                return;
            }
        }
    }

    private boolean locationHasAnotherWorker(Location resourceLocation) {
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

    private boolean sameLocation(Location loc1, Location loc2) {
        return loc1.x == loc2.x && loc1.y == loc2.y;
    }

    private void updateDeposit() {
        UnitInfo[] unitsNearby = uc.senseUnits();
        for (UnitInfo unit : unitsNearby) {
            if (unit.getTeam() == team) {
                if (unit.getType() == UnitType.SETTLEMENT) {
                    if (uc.getLocation().distanceSquared(unit.getLocation()) < uc.getLocation().distanceSquared(depositLocation)) {
                        depositLocation = unit.getLocation();
                    }
                }
            }
        }
    }

    private boolean checkForDeposit() {
        UnitInfo[] unitsNearby = uc.senseUnits();
        for (UnitInfo unit : unitsNearby) {
            if (unit.getTeam() == team) {
                if (unit.getType() == UnitType.SETTLEMENT || unit.getType() == UnitType.BASE)
                    return true;
            }
        }

        return false;
    }

    /**
     * If deer is/are found, engage in hunting mode on the closest one in sight.
     */
    void senseEnemyWorkers() {
        UnitInfo[] enemyUnits = uc.senseUnits(enemy);

        for (UnitInfo enemyUnit : enemyUnits) {
            if (enemyUnit.getTeam() == enemy) {
                if (enemyUnit.getType() == UnitType.WORKER) {
                    currentEnemyWorkerLocation = enemyUnit.getLocation();
                    isFightingEnemyWorkers = true;
                    return;
                }
            }
        }

        if (isFightingEnemyWorkers) {
            isFightingEnemyWorkers = false; // Enemy worker was killed
            closestLocation = null; // Reset pathfinding manually after worker is done chasing deer
        }
    }

    /**
     * If deer is/are found, engage in hunting mode on the closest one in sight.
     */
    void senseDeer() {
        UnitInfo[] deer = uc.senseUnits(deerTeam);

        if (deer.length != 0) {
            currentDeerLocation = deer[0].getLocation();
            isHunting = true;
        } else {
            if (isHunting) {
                isHunting = false; // Deer was killed
                closestLocation = null; // Reset pathfinding manually after worker is done chasing deer
            }
        }
    }


     void spawnBarrack() {
        Location potentialLocation;
        for (Direction dir : dirs) {
            if (uc.canSpawn(UnitType.BARRACKS, dir)) {
                potentialLocation = uc.getLocation().add(dir);
                // If potential location is within lattice structure, place a barrack there.
                if ((potentialLocation.x % 2 == depositLocation.x % 2 && potentialLocation.y % 2 == depositLocation.y % 2) || (potentialLocation.x % 2 != depositLocation.x % 2 && potentialLocation.y % 2 != depositLocation.y % 2)) {
                    uc.spawn(UnitType.BARRACKS, dir);
                    isBarrackBuilding = false;
                    break;
                }
            }
        }
    }

    // Move adjacent to building location and place a building there. Choose a different build location if a building
    // has already been placed. If a unit that is not a building is currently on the location, wait until it moves.
    void spawnBuildings() {
        if (!hasCalculatedBuildingLocations) {
            calculateBuildingLocations();
        }

        if (!buildingLocations.isEmpty()) {
            Location currentBuildingLocation = buildingLocations.get(0);
            uc.println("current building location: " + currentBuildingLocation);

            // If the target building location can be sensed and already has a building on it, a new target needs to be moved to.
            if (!canPlaceBuilding(currentBuildingLocation)) {
                buildingLocations.remove(0);
                closestLocation = null; // Reset pathfinding manually after worker changes target.
            }
            // If the location is not in range or there is no building on the location, move toward building location and spawn a building there.
            else if (bug2(uc.getLocation(), currentBuildingLocation, true)) {
                Direction directionToBuilding = uc.getLocation().directionTo(currentBuildingLocation);

                int buildingType = (int) (uc.getRandomDouble() * 3);
                if (buildingType == 0 && uc.canSpawn(UnitType.FARM, directionToBuilding)) {
                    uc.spawn(UnitType.FARM, directionToBuilding);
                } else if (buildingType == 1 && uc.canSpawn(UnitType.SAWMILL, directionToBuilding)) {
                    uc.spawn(UnitType.SAWMILL, directionToBuilding);
                } else if (buildingType == 2 && uc.canSpawn(UnitType.QUARRY, directionToBuilding)) {
                    uc.spawn(UnitType.QUARRY, directionToBuilding);
                }
            }
        } else {
            isBuilding = false;
        }
    }

    /**
     * Check to make sure that the location does not already have a building on it and is not a water or mountain tile.
     */
    private boolean canPlaceBuilding(Location buildingLocation) {
        if (uc.canSenseLocation(buildingLocation)) {
            UnitInfo unitAtLocation = uc.senseUnitAtLocation(buildingLocation);
            // If there is an immovable unit on the location, a building can not be placed.
            if (unitAtLocation != null && (unitAtLocation.getType() == UnitType.QUARRY || unitAtLocation.getType() == UnitType.SAWMILL || unitAtLocation.getType() == UnitType.FARM || unitAtLocation.getType() == UnitType.SETTLEMENT || unitAtLocation.getType() == UnitType.BARRACKS || unitAtLocation.getType() == UnitType.BASE)) {
                return false;
            }

            if (uc.hasMountain(buildingLocation) || uc.hasWater(buildingLocation)) {
                return false;
            }

            return true;
        }
        // Assume that a building can be placed on the location until seen otherwise.
        return true;
    }

    private void calculateBuildingLocations() {
        Location baseLocation = depositLocation;
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
        uc.println("building locations: " + buildingLocations);
    }

    void fightEnemyWorkers() {
        if (uc.canAttack(currentEnemyWorkerLocation)) {
            uc.attack(currentEnemyWorkerLocation);
        }

        bug2(uc.getLocation(), currentEnemyWorkerLocation, true);

        if (uc.canAttack(currentEnemyWorkerLocation)) {
            uc.attack(currentEnemyWorkerLocation);
        }
    }

    /**
     * Attempt to attack and move to deer in vision radius.
     */
    void hunt() {
        if (uc.canAttack(currentDeerLocation)) {
            uc.attack(currentDeerLocation);
        }

        bug2(uc.getLocation(), currentDeerLocation, true);

        if (uc.canAttack(currentDeerLocation)) {
            uc.attack(currentDeerLocation);
        }
    }

    /**
     * Pathfind to the unit's chosen deposit location, and deposit resources once arrived.
     * Additionally, spawn a barrack next to the deposit location if it doesn't exist already.
     */
    void deposit() {
        if (depositLocation != null) {
            // Unit can only deposit once it has reached its deposit location.
            if (bug2(uc.getLocation(), depositLocation, true)) {
                if (uc.canDeposit()) {
                    uc.deposit();
                    isDepositing = false;
                }
            }
        // If the unit's target deposit is destroyed and no longer exists, hope it finds another one close by.
        } else {
            isDepositing = false;
        }
    }

    /**
     *
     */
    void mine() {
        // For now, if workers fills up on one resource, they go back to base.
        int maxResourcesCarried = max(uc.getResourcesCarried());

        uc.println("can gather resources is -->");
        uc.println(uc.canGatherResources());
        // Gather resources as long as worker has not filled up on a resource and there are still resources left.
        if (uc.canGatherResources() && max(uc.senseResourceInfo(uc.getLocation())) > 0 && ((maxResourcesCarried < 100 && !uc.hasResearched(Technology.BOXES, team)) || ((maxResourcesCarried < 200 && uc.hasResearched(Technology.BOXES, team))))) {
            int maxResourceAmountAtLocation = max((uc.senseResourceInfo(uc.getLocation())));

            // If the unit can sit on a resource for 10 turns and gather, send a smoke signal about a location it has seen.
            // TODO the logic of the if statement doesn't cover all cases...
            if (!messagesToSend.isEmpty()) {
                if (maxResourcesCarried == 0 && ((maxResourceAmountAtLocation >= 100 && !uc.hasResearched(Technology.BOXES, team)) || (maxResourceAmountAtLocation >= 200 && uc.hasResearched(Technology.BOXES, team)))) {
                    if (uc.canMakeSmokeSignal()) {
                        // right now we just send with FIFO principle.
                        uc.makeSmokeSignal(encodeResourceMessage(messagesToSend.remove(0)));
                    }
                }
            }

            uc.gatherResources();
            uc.println("gathering resources");
        // If there are no resources left, but the unit hasn't filled up on resources, find other resources.
        } else if ((maxResourcesCarried < 100 && !uc.hasResearched(Technology.BOXES, team)) || ((maxResourcesCarried < 200 && uc.hasResearched(Technology.BOXES, team)))) {
            isMining = false;
        // If the unit can't get resources because it's at carrying capacity, deposit resources at base.
        } else {
            isMining = false;
            isDepositing = true;
            uc.println("at carrying capacity");
        }
    }

    /**
     * Helper function for mine()
     * Helper function for spawnRandomSettlement()
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
            if (bug2(uc.getLocation(), found_resources.get(currentFoundResourceIndex).location, false))
                isMining = true;
            return;
        }

        // If no resource locations stored, pathfind to a broadcasted resource or simply explore.
        if (found_resources.isEmpty()) {
            if (!low_priority_resources.isEmpty()) {
                found_resources.add(low_priority_resources.remove(0));
            } else {
                explore();
            }
            return;
        }

        // If the list of found locations is not empty and a location isn't set, set target location to the closest one
        // in the resource_list.

        currentFoundResourceIndex = findClosestResourceIndex();
        bug2(uc.getLocation(), found_resources.get(currentFoundResourceIndex).location, false);
    }

    private int findClosestResourceIndex() {
        int closestResourceDistanceSquared = Integer.MAX_VALUE;
        int distanceSquared;
        int closestLocationIndex = 0;

        for (int i = 0; i < found_resources.size(); i++) {
            distanceSquared = uc.getLocation().distanceSquared(found_resources.get(i).location);
            if (distanceSquared < closestResourceDistanceSquared) {
                closestResourceDistanceSquared = distanceSquared;
                closestLocationIndex = i;
            }
        }

        return closestLocationIndex;
    }

    private void explore() {
        int tries = 8;
        while (this.uc.canMove() && tries-- > 0) {
            if (this.uc.canMove(this.currentDir)) {
                this.uc.move(this.currentDir);
            } else {
                this.currentDir = this.currentDir.rotateRight().rotateRight();
            }
        }
    }
}

