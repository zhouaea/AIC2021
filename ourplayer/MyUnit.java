package ourplayer;

import java.util.ArrayList;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.Resource;
import aic2021.user.ResourceInfo;
import aic2021.user.Team;
import aic2021.user.UnitController;
import aic2021.user.UnitType;

public abstract class MyUnit {
    // Have to use constants since enums use static variables.
    final int ENEMY_BASE = 0;
    final int WOOD = 1;
    final int STONE = 2;
    final int FOOD = 3;
    final int PLACEHOLDER_1 = 4;
    final int ENEMY_WORKER = 5;
    final int ENEMY_EXPLORER = 6;
    final int ENEMY_TRAPPER = 7;
    final int ENEMY_AXEMAN = 8;
    final int ENEMY_SPEARMAN=  9;
    final int ENEMY_WOLF = 10;
    final int ENEMY_SETTLEMENT = 11;
    final int ENEMY_BARRACKS = 12;
    final int ENEMY_RESOURCE_BUILDING = 13;
    final int BUILDING_LOCATION = 14;
    final int PLACEHOLDER_2 = 15;

    final int teamIdentifier = 74;

    Direction[] dirs = Direction.values();

    UnitController uc;

//    Team team = this.uc.getTeam();
//    Team enemyTeam = this.uc.getOpponent();
//    Team deerTeam = Team.NEUTRAL;

    Location closestLocation = null;
    Direction bugDirection = null;

    Location enemyBaseLocation;
    ArrayList<Location> dangerZone = new ArrayList<>();

    final Location teamBase = new Location(311, 317);
    final Location enemyBase = new Location(294, 317);

    MyUnit(UnitController uc){
        this.uc = uc;
    }

    abstract void playRound();

    boolean spawnRandom(UnitType t){
        for (Direction dir : dirs){
            if (uc.canSpawn(t, dir)){
                uc.spawn(t, dir);
                return true;
            }
        }
        return false;
    }

    boolean moveRandom(){
        int tries = 10;
        while (uc.canMove() && tries-- > 0){
            int random = (int)(uc.getRandomDouble()*8);
            if (uc.canMove(dirs[random])){
                uc.move(dirs[random]);
                return true;
            }
        }
        return false;
    }

    boolean lightTorch(){
        if (uc.canLightTorch()){
            uc.lightTorch();
            return true;
        }
        return false;
    }

    boolean randomThrow(){
        Location[] locs = uc.getVisibleLocations(uc.getType().getTorchThrowRange(), false);
        int index = (int)(uc.getRandomDouble()*locs.length);
        if (uc.canThrowTorch(locs[index])){
            uc.throwTorch(locs[index]);
            return true;
        }
        return false;
    }


    // TODO We can verify that the smoke signal is ours by making sure that unit code 14's have unit amount 0.
    int encodeBuildingLocation(Location buildingLocation) {
        return encodeSmokeSignal(buildingLocation, BUILDING_LOCATION, 0);
    }

    int encodeResourceMessage(ResourceInfo resourceInfo) {
        if (resourceInfo.resourceType == Resource.WOOD)
            return encodeSmokeSignal(resourceInfo.location, WOOD, resourceInfo.amount);
        else if (resourceInfo.resourceType == Resource.STONE)
            return encodeSmokeSignal(resourceInfo.location, STONE, resourceInfo.amount);
        else
            return encodeSmokeSignal(resourceInfo.location, FOOD, resourceInfo.amount);
    }

    /**
     * Send a location of interest and its contents.
     * @param location the location with a point of interest
     * @param unitCode an integer that corresponds to a unit in the game. See constants in the MyUnit class.
     * @param unitAmount an integer from 0-15 that signifies how many units there are. If the number is 15, it could
     *                   mean 15+ of that unit. If number is greater than 100, we will divide the number by 100 to stay
     *                   within the bit limit.
     * @return a 32 bit integer with the encoded information.
     */
    int encodeSmokeSignal(Location location, int unitCode, int unitAmount) {
        // Divide amount by 100 if unit is a resource.
        if (unitCode >= WOOD && unitCode <= FOOD) {
            unitAmount /= 100;
        }

        // Unit amount cannot pass 15.
        if (unitAmount > 15) {
            unitAmount = 15;
        }

        // Shift teamIdentifier 9 spaces, and shift unitAmount 5 spaces.
        int extra_info = teamIdentifier * 512 + (unitAmount & 511) * 32 + unitCode;
        // Shift extra info (teamIdentifier + unitAmount + unitCode) 16 spaces.
        int message = extra_info * 128 * 128 + encodeLocation(location);

        return message;
    }

    int encodeLocation(Location location) {
        int encoded_x = location.x % 128;
        int encoded_y = location.y % 128;
        int message = encoded_x * 128 + encoded_y;

        return message;
    }

    /**
     * Decode a smoke signal.
     * @param currentLocation the location of the decoding unit
     * @param codedMessage 32 bit integer that came from the smoke signal
     * @return The contents of the message if we are 99% sure the message came from our team. Otherwise, null.
     */
    DecodedMessage decodeSmokeSignal(Location currentLocation, int codedMessage) {
        Location location = decodeLocation(currentLocation, codedMessage);

        // unitCode is bits 16 - 20
        codedMessage = codedMessage / 128 / 128;
        int unitCode = 31 & codedMessage;

        // unitAmount is bits 21-24
        codedMessage = codedMessage / 32;
        int unitAmount = 15 & codedMessage;

        // identifier is bits 25-31
        codedMessage = codedMessage / 16;
        int identifier = codedMessage;

        // Only decode message if we are 99% certain that the message is ours.
        DecodedMessage decodedMessage;
        if (identifier == teamIdentifier) {
            // Convert resource amount from 4 bit version to real amount.
            if (unitCode >= WOOD && unitCode <= FOOD) {
                unitAmount *= 100;
            }
            decodedMessage = new DecodedMessage(location, unitCode, unitAmount);
        } else {
            decodedMessage = null;
        }

        return decodedMessage;
    }

    Location decodeLocation(Location current_location, int code) {
        int encoded_y = 255 & code; // encoded y coordinate is first 8 bits
        int encoded_x = 255 & (code / 128);  // encoded x coordinate is bits 8-15

        // Get close to the offset by getting rid of the remainder bits of the current location.
        int offsetX = (current_location.x / 128) * 128;
        int offsetY = (current_location.y / 128) * 128;

        Location possible_location = new Location(offsetX + encoded_x, offsetY + encoded_y);
        Location actual_location = possible_location;

        // Offset may be off. Not sure if this is necessary.
        Location alternate_location = possible_location.add(128, 0);
        if (current_location.distanceSquared(alternate_location) < current_location.distanceSquared(possible_location)) {
            actual_location = alternate_location;
        }

        alternate_location = possible_location.add(-128, 0);
        if (current_location.distanceSquared(alternate_location) < current_location.distanceSquared(possible_location)) {
            actual_location = alternate_location;
        }

        alternate_location = possible_location.add(0, 128);
        if (current_location.distanceSquared(alternate_location) < current_location.distanceSquared(possible_location)) {
            actual_location = alternate_location;
        }

        alternate_location = possible_location.add(0, -128);
        if (current_location.distanceSquared(alternate_location) < current_location.distanceSquared(possible_location)) {
            actual_location = alternate_location;
        }

        return actual_location;
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
        else if (torchRounds <= 1) {
            if (dropTorch())
                if (uc.canLightTorch()) {
                    uc.lightTorch();
                }
                else
                    uc.println("SOMEHOW TORCH CAN NOT BE LIT");
        }
        uc.println("Torch" + uc.getEnergyLeft());
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

    /** Left handed bug that attempts to stay in a straight line.
     * @param currentLocation
     * @param destination
     * @return true if at location, false if still moving towards it.
     */
    // TODO Avoid tiles within enemy attack radiuses and traps (for workers and explorers). Avoid the vision range of the base once location is known.
    boolean bug2(Location currentLocation, Location destination, boolean adjacentDestination) {
        if (!uc.canMove())
            return false;

        if (destinationIsReached(currentLocation, destination, adjacentDestination)) {
            uc.println("destination reached");
            closestLocation = null;
            return true;
        }

        // Show line from closest location to the destination when clicking on unit.
        if (closestLocation != null)
            uc.drawLineDebug(closestLocation, destination, 255, 255, 255);

        if (closestLocation != null)
            uc.println("closest location: " + closestLocation);

        // If the bot is on the closest location it has been in, attempt to move in a straight line from location to destination.
        if (closestLocation == null || currentLocation.distanceSquared(destination) < closestLocation.distanceSquared(destination)) {
            closestLocation = currentLocation;
            bugDirection = currentLocation.directionTo(destination);
            uc.move(bugDirection);
        }
        // If this is not possible, keep bot's left side on obstacle until it moves to a closer location than it had before.
        else {
            leftBug();
        }

        return false;
    }

    /** Keep bot's left side on obstacle.
     */
    private void leftBug() {
        uc.println("navigate around obstacle");
        // Rotate right until the bot can move forward
        int i = 0;
        for (i = 0; i < 8; i++) {
            if (uc.canMove(bugDirection)) {
                uc.move(bugDirection);
                break;
            }
            else
                bugDirection = bugDirection.rotateRight();
        }

        // Rotate 90 degrees left each turn to "push against the wall" and catch a potential curve of the wall.
        bugDirection = bugDirection.rotateLeft();
        bugDirection = bugDirection.rotateLeft();
    }

    boolean destinationIsReached(Location currentLocation, Location destination, boolean adjacentDestination) {
        // If pathfinding to a location adjacent to the destination, set the destination to be at an adjacent tile.
        if (adjacentDestination) {
            // If unit is on the destination instead of being adjacent to it, move to an adjacent tile, and the
            // destination is now reached.
            if (uc.getLocation().isEqual(destination)) {
                for (Direction dir : dirs) {
                    Location alternative = destination.add(dir);
                    if (uc.canMove(dir)) {
                        uc.move(dir);
                        return true;
                    }
                }
                // Otherwise, the destination is reached when the unit steps on a tile adjacent to the destination.
            } else {
                uc.println("base sensed");
                for (Direction dir : dirs) {
                    Location alternative = destination.add(dir);
                    if (currentLocation.isEqual(alternative)) {
                        uc.println("base reached");
                        destination = alternative;
                        break;
                    }
                }
            }
        }

        // Once at destination, reset for new location.
        if (currentLocation.isEqual(destination)) {
            return true;
        }

        return false;
    }

    void recordEnemyBase() {
        if (this.uc.canReadSmokeSignals()) {
            int[] signals = this.uc.readSmokeSignals();
            Location currentLoc = this.uc.getLocation();
            for (int signal : signals) {
                DecodedMessage message = this.decodeSmokeSignal(currentLoc, signal);
                if (message.unitCode == 0) {
                    this.enemyBaseLocation = message.location;
                    this.uc.println("Enemy base recorded");
                    return;
                }
            }
        }
    }

    void calculateDangerZone() {
        this.uc.println("Energy before danger zone: " + this.uc.getEnergyUsed());
        this.dangerZone.add(this.enemyBaseLocation.add(-1, 4));
        this.dangerZone.add(this.enemyBaseLocation.add(0, 4));
        this.dangerZone.add(this.enemyBaseLocation.add(1, 4));
        this.dangerZone.add(this.enemyBaseLocation.add(1, 3));
        this.dangerZone.add(this.enemyBaseLocation.add(2, 3));
        this.dangerZone.add(this.enemyBaseLocation.add(3, 3));
        this.dangerZone.add(this.enemyBaseLocation.add(3, 2));
        this.dangerZone.add(this.enemyBaseLocation.add(3, 1));
        this.dangerZone.add(this.enemyBaseLocation.add(4, 1));
        this.dangerZone.add(this.enemyBaseLocation.add(4, 0));
        this.dangerZone.add(this.enemyBaseLocation.add(4, -1));
        this.dangerZone.add(this.enemyBaseLocation.add(3, -1));
        this.dangerZone.add(this.enemyBaseLocation.add(3, -2));
        this.dangerZone.add(this.enemyBaseLocation.add(3, -3));
        this.dangerZone.add(this.enemyBaseLocation.add(2, -3));
        this.dangerZone.add(this.enemyBaseLocation.add(1, -3));
        this.dangerZone.add(this.enemyBaseLocation.add(1, 4));
        this.dangerZone.add(this.enemyBaseLocation.add(-1, -4));
        this.dangerZone.add(this.enemyBaseLocation.add(0, -4));
        this.dangerZone.add(this.enemyBaseLocation.add(-1, -4));
        this.dangerZone.add(this.enemyBaseLocation.add(-1, -3));
        this.dangerZone.add(this.enemyBaseLocation.add(-2, -3));
        this.dangerZone.add(this.enemyBaseLocation.add(-3, -3));
        this.dangerZone.add(this.enemyBaseLocation.add(-3, -2));
        this.dangerZone.add(this.enemyBaseLocation.add(-3, -1));
        this.dangerZone.add(this.enemyBaseLocation.add(-4, -1));
        this.dangerZone.add(this.enemyBaseLocation.add(-4, 0));
        this.dangerZone.add(this.enemyBaseLocation.add(-4, 1));
        this.dangerZone.add(this.enemyBaseLocation.add(-3, 1));
        this.dangerZone.add(this.enemyBaseLocation.add(-3, 2));
        this.dangerZone.add(this.enemyBaseLocation.add(-3, 3));
        this.dangerZone.add(this.enemyBaseLocation.add(-2, 3));
        this.dangerZone.add(this.enemyBaseLocation.add(-1, 3));
        this.uc.println("Danger zone calculated, energy after: " + this.uc.getEnergyUsed());
    }
}
