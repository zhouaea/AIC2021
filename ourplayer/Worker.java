package ourplayer;

import java.util.ArrayList;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.Resource;
import aic2021.user.ResourceInfo;
import aic2021.user.Team;
import aic2021.user.Technology;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Worker extends MyUnit {

  Worker(UnitController uc) {
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

  boolean isBuilding = false;
  ArrayList<Location> buildingLocations = new ArrayList<>();

  boolean isDepositing = false;
  boolean knowsPlaceToDeposit = false;
  Location depositLocation;

  int farms = 0;
  int sawmills = 0;
  int quarries = 0;

  void playRound(){
    // TODO workers run out of energy when there are too many resources to sense
    // TODO lattice structure can mess with bug2, we need a different pathfinding algorithm for that
    // TODO prioritize closest targets rather than going in order for more efficient collection
    // TODO Build settlements near far away resources
    // TODO build a barrack in lattice structure
    // If just created, store team's base location.
    if (!knowsPlaceToDeposit) {
      rememberBaseLocation();
    }

    senseEnemies(); // All bytecode goes to running from enemies to base or fighting as a first priority.
    uc.println(uc.getEnergyLeft());
    decodeMessages(); // get locations of resources from other units or a place to build a building from the base
    uc.println(uc.getEnergyLeft());
    senseResources(); // add/remove resource locations to/from memory
    senseDeer(); //  check local area for deer to hunt
    uc.println(uc.getEnergyLeft());
    keepTorchLit();
    uc.println(uc.getEnergyLeft());

    if (isBuilding) {
      spawnBuilding(); // once jobs is unlocked, spawn 20 buildings max
      uc.println("building");
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

  /**
   * If a smoke signal is from an ally and is for a resource location, add the encoded resource to found_resources.
   * Alternatively, if the signal is for a building location, add the encoded location to buildingLocations.
   */
  private void decodeMessages() {
    int[] smokeSignals = uc.readSmokeSignals();
    Location currentLocation = uc.getLocation();
    DecodedMessage message;

    for (int smokeSignal : smokeSignals) {
      message = decodeSmokeSignal(currentLocation, smokeSignal);
      if (message != null)
        if (message.unitCode == WOOD)
          found_resources.add(new ResourceInfo(Resource.WOOD, message.unitAmount, message.location));
        else if (message.unitCode == STONE)
          found_resources.add(new ResourceInfo(Resource.STONE, message.unitAmount, message.location));
        else if (message.unitCode == FOOD)
          found_resources.add(new ResourceInfo(Resource.FOOD, message.unitAmount, message.location));
        else if (message.unitCode == BUILDING_LOCATION) {
          buildingLocations.add(message.location);
          uc.println("building location received");
        }
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

        if (resource.amount >= 100) {
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

  // Move adjacent to building location and place a building there. Choose a different build location if a building
  // has already been placed. If a unit that is not a building is currently on the location, wait until it moves.
  // TODO prevent workers from waiting too long to place a building
  void spawnBuilding() {
    // Only switch off of building mode if there are no buildings to place
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

        int buildingType = (int)(uc.getRandomDouble() * 3);
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
    // Unit can only deposit once unit has reached its deposit location.
    if (bug2(uc.getLocation(), depositLocation, true)) {
      if (uc.canDeposit()) {
        uc.deposit();
        isDepositing = false;

        if (uc.hasResearched(Technology.MILITARY_TRAINING, team)) {
          // Ensure that one barrack is placed near the unit's deposit location once barracks are unlocked.
          if (!checkForBarrack())
            spawnBarrack();
        }

        // TODO activate building mode if the base is in the worker's vision range?
        // Once worker has deposited resources, jobs is unlocked, and it has a build location, isBuilding = true.
        if (uc.hasResearched(Technology.JOBS, team) && !buildingLocations.isEmpty()) {
          isBuilding = true;
          uc.println("switching to building mode");
        }
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
   * Helper function for deposit
   * Spawn a barrack on a location that would fit a lattice structur.
   */
  private void spawnBarrack() {
    Location potentialLocation;
    for (Direction dir : dirs) {
      if (uc.canSpawn(UnitType.BARRACKS, dir)) {
        potentialLocation = uc.getLocation().add(dir);
        // If potential location is within lattice structure, place a barrack there.
        if ((potentialLocation.x % 2 == depositLocation.x % 2 && potentialLocation.y % 2 == depositLocation.y % 2) || potentialLocation.x % 2 != depositLocation.x % 2 && potentialLocation.y % 2 != depositLocation.y % 2) {
          uc.spawn(UnitType.BARRACKS, dir);
          break;
        }
      }
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
      if (bug2(uc.getLocation(), found_resources.get(currentFoundResourceIndex).location, false))
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
    bug2(uc.getLocation(), found_resources.get(currentFoundResourceIndex).location, false);
  }

  //  void explore() {
  //    int tries = 8;
  //    while (this.uc.canMove() && tries-- > 0) {
  //      if (this.uc.canMove(this.currentDir)) {
  //        this.uc.move(this.currentDir);
  //      } else {
  //        this.currentDir = this.currentDir.rotateRight().rotateRight();
  //      }
  //    }
  //  }
}
