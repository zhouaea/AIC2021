package ourplayer;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitType;

public abstract class MyUnit {

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

    int encodeLocation(Location location) {
        int encoded_x = location.x % 128;
        int encoded_y = location.y % 128;
        int message = encoded_x * 128 + encoded_y;

        uc.println("x location: " + location.x);
        uc.println("y location: " + location.y);
        uc.println("encoded x location: " + encoded_x);
        uc.println("encoded y location: " + encoded_y);
        uc.println("message: " + message);
        return message;
    }

    int encodeLocation(Location location, int extra_info) {
        int encoded_x = location.x % 128;
        int encoded_y = location.y % 128;
        int message = extra_info * 128 * 128 + encoded_x * 128 + encoded_y ;

        uc.println("extra info");
        uc.println("x location: " + location.x);
        uc.println("y location: " + location.y);
        uc.println("encoded x location: " + encoded_x);
        uc.println("encoded y location: " + encoded_y);
        uc.println("message: " + message);
        return message;
    }

    Location decodeLocation(Location current_location, int code) {
        int encoded_y = 255 & code; // encoded y coordinate is first 8 bits
        int encoded_x = 255 & (code / 128);  // encoded x coordinate is bits 8-15
        int rest_of_message = (code / 128 / 128);

        // Get close to the offset by getting rid of the remainder bits of the current location.
        int offsetX = (current_location.x / 128) * 128;
        int offsetY = (current_location.y / 128) * 128;

        Location possible_location = new Location(offsetX + encoded_x, offsetY + encoded_y);
        Location actual_location = possible_location;
        uc.println("try 1: " + actual_location);

        // Offset may be off.
        Location alternate_location = possible_location.add(128, 0);
        if (current_location.distanceSquared(alternate_location) < current_location.distanceSquared(possible_location)) {
            actual_location = alternate_location;
        }
        uc.println("try 2: " + actual_location);

        alternate_location = possible_location.add(-128, 0);
        if (current_location.distanceSquared(alternate_location) < current_location.distanceSquared(possible_location)) {
            actual_location = alternate_location;
        }
        uc.println("try 3: " + actual_location);

        alternate_location = possible_location.add(0, 128);
        if (current_location.distanceSquared(alternate_location) < current_location.distanceSquared(possible_location)) {
            actual_location = alternate_location;
        }
        uc.println("try 4: " + actual_location);

        alternate_location = possible_location.add(0, -128);
        if (current_location.distanceSquared(alternate_location) < current_location.distanceSquared(possible_location)) {
            actual_location = alternate_location;
        }
        uc.println("try 5: " + actual_location);

        return actual_location;
    }

}
