package ourplayer;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitType;

public abstract class MyUnit {
    // Have to use constants since enums use static variables.
    final int ENEMY_BASE = 0;
    final int WOOD = 1;
    final int STONE = 2;
    final int FOOD = 3;
    final int DEER = 4;
    final int ENEMY_WORKER = 5;
    final int ENEMY_EXPLORER = 6;
    final int ENEMY_TRAPPER = 7;
    final int ENEMY_AXEMAN = 8;
    final int ENEMY_SPEARMAN=  9;
    final int ENEMY_WOLF = 10;
    final int ENEMY_SETTLEMENT = 11;
    final int ENEMY_BARRACKS = 12;
    final int ENEMY_FARM = 13;
    final int ENEMY_SAWMILL = 14;
    final int ENEMY_QUARRY = 15;

    final int teamIdentifier = 74;

    Direction[] dirs = Direction.values();

    UnitController uc;

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

        uc.println("Encoding Energy Used: " + uc.getEnergyUsed());

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

        uc.println("Decoding Energy Used: " + uc.getEnergyUsed());
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
}
