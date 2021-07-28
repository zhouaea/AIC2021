package ourplayer;

import aic2021.user.*;

public abstract class MyUnit {

    Direction[] dirs = Direction.values();

    UnitController uc;

    MyUnit(UnitController uc){
        this.uc = uc;
    }

    abstract void playRound();

    Location closestLocation = null;
    Direction bugDirection = null;

    final Location teamBase = new Location(876, 739);
    final Location enemyBase = new Location(908, 771);

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

    /** Left handed bug that attempts to stay in a straight line.
     * @param currentLocation
     * @param destination
     * @return true if at location, false if still moving towards it.
     */
    boolean bug2(Location currentLocation, Location destination) {
        if (!uc.canMove())
            return false;

        if (destinationIsReached(currentLocation, destination)) {
            uc.println("destination reached");
            closestLocation = null;
            return true;
        }

        uc.drawLineDebug(closestLocation, destination, 255, 255, 255);

        // If the bot is on the closest location it has been in, attempt to move in a straight line from location to destination.
        if (closestLocation == null || currentLocation.distanceSquared(destination) < closestLocation.distanceSquared(destination)) {
            uc.println("closest location");
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
        uc.println("rotated left");
        bugDirection = bugDirection.rotateLeft();
        bugDirection = bugDirection.rotateLeft();
    }

    boolean destinationIsReached(Location currentLocation, Location destination) {
        // If pathfinding to base, set the destination to be at an adjacent tile.
        if (uc.senseUnitAtLocation(destination) != null && uc.senseUnitAtLocation(destination).getType() == UnitType.BASE) {
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

        // Once at destination, reset for new location.
        if (currentLocation.isEqual(destination)) {
            return true;
        }

        return false;
    }
}
