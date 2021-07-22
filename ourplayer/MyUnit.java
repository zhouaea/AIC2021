package ourplayer;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitType;

public abstract class MyUnit {
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

    int encodeSmokeSignal(Location location, int unitCode, int unitAmount) {
        int extra_info = (unitAmount & 511) * 32 + unitCode;
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

    DecodedMessage decodeSmokeSignal(Location currentLocation, int codedMessage) {
        int unitCode = 31 & (codedMessage / 128 / 128); // unitCode is bits 16 - 20
        int unitAmount = codedMessage / 128 / 128 / 32; // unitAmount is bits 21-31
        Location location = decodeLocation(currentLocation, codedMessage);
        DecodedMessage decodedMessage = new DecodedMessage(location, unitCode, unitAmount);

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
