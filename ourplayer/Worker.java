package ourplayer;

import aic2021.engine.Unit;
import aic2021.user.*;
import java.util.ArrayList; // TODO switch to priority queue later


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
        senseResources(); // check local area for deer to hunt or add/remove resource locations to/from memory
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
        // Look at resources in vision radius.
        ResourceInfo[] resources = uc.senseResources();


        if (currentFoundResourceIndex > -1) {
            Location currentResourceLocation = found_resources.get(currentFoundResourceIndex).location;
            if (uc.canSenseLocation(currentResourceLocation)) {
                // If the current resource should be in sight (assuming unit is holding a torch) and it is not sensed, remove it.
                if (uc.senseResourceInfo(currentResourceLocation) == null)
                    found_resources.remove(currentFoundResourceIndex);
                // If the current resource is in sight, and the unit is on the location of the resource, begin mining.
                else if (currentResourceLocation.isEqual(uc.getLocation()))
                    isMining = true;
            }
        }

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

        if (deer.length != 0) {
            currentDeerLocation = deer[0].getLocation();
            isHunting = true;
            goToLocation(deer[0].getLocation());
        } else {
            isHunting = false; // Deer was killed
        }
    }

    // When torch is almost finished, throw it on an adjacent square and light another torch with it.
    void keepTorchLit() {
        int torchRounds = uc.getInfo().getTorchRounds();
        // Light torch when spawned.
        if (torchRounds == 0) {
            if (uc.canLightTorch())
                uc.lightTorch();
        }
        // After initial torch light, throw torch on ground and light a new one when the torch is almost depleted.
       else if (torchRounds <= 5) {
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
        followPath();
        if (uc.canDeposit()) {
            if (uc.hasResearched(Technology.MILITARY_TRAINING, team))
                if (!checkForBarrack())
                    spawnRandom(UnitType.BARRACKS);

            uc.deposit();
            isDepositing = false;
        }
    }

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

    void hunt() {
        if (uc.canAttack(currentDeerLocation)) {
            uc.attack(currentDeerLocation);
        }

        followPath();

        if (uc.canAttack(currentDeerLocation)) {
            uc.attack(currentDeerLocation);
        }
    }

    void mine() {
        int maxResourcesCarried = max(uc.getResourcesCarried());

        // For now, if workers fills up on one resource, they go back to base.
        if (uc.canGatherResources() && ((maxResourcesCarried < 100 && !uc.hasResearched(Technology.BOXES, team)) || ((maxResourcesCarried < 200 && uc.hasResearched(Technology.BOXES, team))))) {
            int maxResourceAmountAtLocation = max((uc.senseResourceInfo(uc.getLocation())));

            // If the unit can sit on the resource for 10 turns, send a smoke signal about a location it has seen.
            if ((maxResourceAmountAtLocation >= 100 && !uc.hasResearched(Technology.BOXES, team)) || (maxResourceAmountAtLocation >= 200 && uc.hasResearched(Technology.BOXES, team))) {
                if (!messagesToSend.isEmpty()) {
                    if (uc.canMakeSmokeSignal())
                        // right now we just send with FIFO principle.
                        uc.makeSmokeSignal(encodeResourceMessage(messagesToSend.remove(0)));
                }
            }

            uc.gatherResources();
        // If the unit can't get resources because there are no more left, the location is no longer valid, so remove it
        // from the list of found locations and as the currentLocation being travelled to.
        } else if (maxResourcesCarried < GameConstants.MAX_RESOURCE_CAPACITY || (maxResourcesCarried < GameConstants.MAX_RESOURCE_CAPACITY_BOXES && uc.hasResearched(Technology.BOXES, team))) {
            uc.println(currentFoundResourceIndex);
            found_resources.remove(currentFoundResourceIndex);
            currentFoundResourceIndex = -1;
            isMining = false;
        // If the unit can't get resources because it is at its carrying capacity, deposit resources at base.
        } else {
            isMining = false;
            isDepositing = true;
            goToLocation(depositLocation);
        }
    }

    private int max(ResourceInfo[] array) {
        int max = 0;
        for (ResourceInfo i : array) {
            if (i != null && i.amount > max)
                max = i.amount;
        }

        return max;
    }

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
            followPath();
            uc.println("current location: " + uc.getLocation());
            uc.println("destination: " + found_resources.get(currentFoundResourceIndex).location);
            uc.println("current location is equal to destination: " + uc.getLocation().isEqual(found_resources.get(currentFoundResourceIndex).location));
            if (uc.getLocation().isEqual(found_resources.get(currentFoundResourceIndex).location))
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
        goToLocation(found_resources.get(currentFoundResourceIndex).location);
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
