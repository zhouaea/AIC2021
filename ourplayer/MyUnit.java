package ourplayer;

import aic2021.user.*;

import java.util.*;

public abstract class MyUnit {
    // Have to use constants since enums use static variables.
    final int ENEMY_BASE = 0;
    final int WOOD = 1;
    final int STONE = 2;
    final int FOOD = 3;
    final int ASSIGN_BARRACK_BUILDER = 4;
    final int ENEMY_ARMY_COUNT_REPORT = 5;
    final int BUY_RAFTS = 6;
    final int SETTLEMENT_CREATED = 7;
    final int ENEMY_AXEMAN = 8;
    final int ENEMY_SPEARMAN = 9;
    final int ENEMY_WOLF = 10;
    final int ENEMY_SETTLEMENT = 11;
    final int ENEMY_BARRACKS = 12;
    final int ENEMY_RESOURCE_BUILDING = 13;
    final int BUILDING_LOCATION = 14;
    final int ASSIGN_BUILDER = 15;

    final int teamIdentifier = 74;

    Direction[] dirs = Direction.values();

    UnitController uc;

    Location closestLocation = null;
    Direction bugDirection = null;

    Location enemyBaseLocation = null;

    MyUnit(UnitController uc) {
        this.uc = uc;
    }

    abstract void playRound();

    boolean spawnRandom(UnitType t) {
        Location currentLocation = uc.getLocation();
        for (Direction dir : dirs) {
            if (uc.canSpawn(t, dir) && isSafeToMove(currentLocation.add(dir))) {
                uc.spawn(t, dir);
                return true;
            }
        }
        return false;
    }

    boolean moveRandom() {
        int tries = 10;
        while (uc.canMove() && tries-- > 0) {
            int random = (int) (uc.getRandomDouble() * 8);
            if (uc.canMove(dirs[random])) {
                uc.move(dirs[random]);
                return true;
            }
        }
        return false;
    }

    boolean lightTorch() {
        if (uc.canLightTorch()) {
            uc.lightTorch();
            return true;
        }
        return false;
    }

    boolean randomThrow() {
        Location[] locs = uc.getVisibleLocations(uc.getType().getTorchThrowRange(), false);
        int index = (int) (uc.getRandomDouble() * locs.length);
        if (uc.canThrowTorch(locs[index])) {
            uc.throwTorch(locs[index]);
            return true;
        }
        return false;
    }

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
     *
     * @param location   the location with a point of interest
     * @param unitCode   an 4 bit integer (from 0-15) that corresponds to a unit in the game. See constants in the MyUnit class.
     * @param unitAmount a 4 bit integer that signifies how many units there are. If the number is 15, it could
     *                   mean 15+ of that unit. If number is greater than 100, we will divide the number by 100 to stay
     *                   within the bit limit.
     * @return a 32 bit integer with the encoded information.
     * bits 0-13 are for location, with 7 bits used for each coordinate (0-127)
     * bit 14-17 are for the unit code
     * bits 18-21 are for the unit amount
     * bits 22-31 are for the identifier (leaving a a 1 / 2^10 chance of mistaking an enemy smoke signal for an
     * ally smoke signal, or a 0.09% chance that an enemy smoke signal is ours)
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

        // Shift teamIdentifier 8 spaces, and shift unitAmount 4 spaces.
        int extra_info = teamIdentifier * 256 + unitAmount * 16 + unitCode;
        // Shift extra info (teamIdentifier + unitAmount + unitCode) 14 spaces.
        int message = extra_info * 128 * 128 + encodeLocation(location);
        uc.println("location to encode: " + location);

        return message;
    }

    int encodeSmokeSignal(int unitId, int unitCode, int unitAmount) {
        // Divide amount by 100 if unit is a resource.
        if (unitCode >= WOOD && unitCode <= FOOD) {
            unitAmount /= 100;
        }

        // Unit amount cannot pass 15.
        if (unitAmount > 15) {
            unitAmount = 15;
        }

        // Shift teamIdentifier 9 spaces, and shift unitAmount 5 spaces.
        int extra_info = teamIdentifier * 256 + unitAmount * 16 + unitCode;
        // Shift extra info (teamIdentifier + unitAmount + unitCode) 16 spaces.
        int message = extra_info * 128 * 128 + unitId;

        return message;
    }

    int encodeLocation(Location location) {
        int encodedX = location.x % 128;
        int encodedY = location.y % 128;
        int message = encodedX * 128 + encodedY;

        return message;
    }

    /**
     * Decode a smoke signal.
     *
     * @param currentLocation the location of the decoding unit
     * @param codedMessage    32 bit integer that came from the smoke signal
     * @return The contents of the message if we are 99% sure the message came from our team. Otherwise, null.
     */
    DecodedMessage decodeSmokeSignal(Location currentLocation, int codedMessage) {
        // Quick fix for the case that the message is for assigning a builder.
        int originalCodedMessage = codedMessage;

        // unitCode is bits 14-17
        codedMessage = codedMessage / 128 / 128;
        int unitCode = 15 & codedMessage;

        // unitAmount is bits 18-21
        codedMessage = codedMessage / 16;
        int unitAmount = 15 & codedMessage;

        // identifier is bits 21-31
        codedMessage = codedMessage / 16;
        int identifier = codedMessage;

        // Only decode message if we are 99% certain that the message is ours.
        DecodedMessage decodedMessage;
        if (identifier == teamIdentifier) {
            // If this message is for assigning a builder, the location field is now used to mention a unit id.
            if (unitCode == ASSIGN_BUILDER || unitCode == ASSIGN_BARRACK_BUILDER || unitCode == ENEMY_ARMY_COUNT_REPORT) {
                int unitId = originalCodedMessage % (128 * 128);
                decodedMessage = new DecodedMessage(unitId, unitCode, unitAmount);
            } else {
                Location location = decodeLocation(currentLocation, originalCodedMessage);
                // Convert resource amount from 4 bit version to real amount.
                if (unitCode == WOOD || unitCode == FOOD || unitCode == STONE) {
                    unitAmount *= 100;
                }
                uc.println("decoded location: " + location);
                decodedMessage = new DecodedMessage(location, unitCode, unitAmount);
            }
        } else {
            decodedMessage = null;
        }

        return decodedMessage;
    }

    Location decodeLocation(Location current_location, int code) {
        int encodedY = 127 & code; // encoded y coordinate is first 7 bits
        int encodedX = 127 & (code / 128);  // encoded x coordinate is bits 8-15

        // Get close to the offset by getting rid of the remainder bits of the decoder's current location.
        int offsetX = (current_location.x / 128) * 128;
        int offsetY = (current_location.y / 128) * 128;

        Location possibleLocation = new Location(offsetX + encodedX, offsetY + encodedY);
        Location bestLocationSoFar = possibleLocation;

        // Offset may be off by 128 squares in either the x or y axes.
        Location alternateLocation = possibleLocation.add(128, 128);
        if (current_location.distanceSquared(alternateLocation) < current_location.distanceSquared(bestLocationSoFar)) {
            bestLocationSoFar = alternateLocation;
        }

        alternateLocation = alternateLocation.add(0, -128);
        if (current_location.distanceSquared(alternateLocation) < current_location.distanceSquared(bestLocationSoFar)) {
            bestLocationSoFar = alternateLocation;
        }

        alternateLocation = alternateLocation.add(0, -128);
        if (current_location.distanceSquared(alternateLocation) < current_location.distanceSquared(bestLocationSoFar)) {
            bestLocationSoFar = alternateLocation;
        }

        alternateLocation = alternateLocation.add(-128, 0);
        if (current_location.distanceSquared(alternateLocation) < current_location.distanceSquared(bestLocationSoFar)) {
            bestLocationSoFar = alternateLocation;
        }

        alternateLocation = alternateLocation.add(-128, 0);
        if (current_location.distanceSquared(alternateLocation) < current_location.distanceSquared(bestLocationSoFar)) {
            bestLocationSoFar = alternateLocation;
        }

        alternateLocation = alternateLocation.add(0, 128);
        if (current_location.distanceSquared(alternateLocation) < current_location.distanceSquared(bestLocationSoFar)) {
            bestLocationSoFar = alternateLocation;
        }

        alternateLocation = alternateLocation.add(0, 128);
        if (current_location.distanceSquared(alternateLocation) < current_location.distanceSquared(bestLocationSoFar)) {
            bestLocationSoFar = alternateLocation;
        }

        alternateLocation = alternateLocation.add(128, 0);
        if (current_location.distanceSquared(alternateLocation) < current_location.distanceSquared(bestLocationSoFar)) {
            bestLocationSoFar = alternateLocation;
        }

        return bestLocationSoFar;
    }

    /**
     * Left handed bug that attempts to stay in a straight line, avoiding attacks if it can.
     *
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
        // Don't do this if moving to a tile would cause their death
        if (closestLocation == null || currentLocation.distanceSquared(destination) < closestLocation.distanceSquared(destination)) {
            closestLocation = currentLocation;

            bugDirection = currentLocation.directionTo(destination);

            if (isSafeToMove(currentLocation.add(bugDirection)))
                uc.move(bugDirection);
            else
                leftBug();
        }
        // If this is not possible, keep bot's left side on obstacle until it moves to a closer location than it had before.
        else {
            leftBug();
        }

        return false;
    }

    /**
     * Keep bot's left side on obstacle.
     */
    private void leftBug() {
        uc.println("navigate around obstacle");
        // Rotate right until the bot can move forward without dying.
        int i = 0;
        for (i = 0; i < 8; i++) {
            if (uc.canMove(bugDirection) && isSafeToMove(uc.getLocation().add(bugDirection))) {
                uc.move(bugDirection);
                break;
            } else
                bugDirection = bugDirection.rotateRight();
        }

        // Rotate 90 degrees left each turn to "push against the wall" and catch a potential curve of the wall.
        bugDirection = bugDirection.rotateLeft();
        bugDirection = bugDirection.rotateLeft();
    }

    boolean destinationIsReached(Location currentLocation, Location destination, boolean adjacentDestination) {
        if (currentLocation == null || destination == null) {
            return false;
        }

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
                for (Direction dir : dirs) {
                    Location alternative = destination.add(dir);
                    if (currentLocation.isEqual(alternative)) {
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

    boolean isSafeToMove(Location loc) {
        if (uc.hasTrap(loc))
            return false;

        // Factor in the attack radius of the enemy base, if its location is known.
        if (enemyBaseLocation != null)
            if (loc.distanceSquared(enemyBaseLocation) <= UnitType.BASE.getAttackRange())
                return false;

        // Factor in the attack radius of surrounding enemy troops, if the unit is not a defensive troop.
        if (uc.getType() != UnitType.WORKER || uc.getType() == UnitType.EXPLORER || uc.getType() == UnitType.TRAPPER) {
            UnitInfo[] enemyUnits = uc.senseUnits(uc.getTeam().getOpponent());
            for (UnitInfo unit : enemyUnits) {
                int attackRange = unit.getType().getAttackRange();

                if (loc.distanceSquared(unit.getLocation()) <= attackRange)
                    return false;
            }
        }

        return true;
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
                } else
                    uc.println("SOMEHOW TORCH CAN NOT BE LIT");
        }
    }

    /**
     * Attempt to throw the unit's torch at an adjacent tile.
     *
     * @return whether or not unit was able to throw a torch on an adjacent tile
     */
    boolean dropTorch() {
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
        temp_x -= 2;
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
     *
     * @param location to throw torch
     * @return whether or not torch was able to be thrown
     */
    boolean tryToThrowTorch(Location location) {
        if (uc.canThrowTorch(location)) {
            uc.throwTorch(location);
            return true;
        }
        return false;
    }
}