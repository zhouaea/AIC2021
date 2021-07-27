package ourplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.Resource;
import aic2021.user.ResourceInfo;
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

    //Pathfinder:
    ArrayList<Waypoint> currentPath = new ArrayList<>();
    Location currentDestination;

    public boolean inClosedList(Waypoint item, HashSet<Waypoint> closedList) {
        for (Waypoint w : closedList) {
            if (w.equals(item)) {
                return true;
            }
        }

        return false;
    }

    public Location lastResortEstimate(Location dest) {
        Location[] visibleTiles = uc.getVisibleLocations(true);

        Location closest = null;
        for (Location tile : visibleTiles) {
            if (closest == null) {
                closest = tile;
                continue;
            }

            if (computeH(tile, dest) < computeH(closest, dest) && uc.isAccessible(tile) && !tile.isEqual(uc.getLocation())) {
                closest = tile;
            }
        }

        return closest;
    }

    /**
     * computeEstimate provides an alternative location if the original is
     * undetectable. It arrives at coordinate by finding the intersections of
     * the line connecting the unit and the destination and the unit's circle of
     * detection.
     * @param dest The destination the unit is expected to travel to.
     * @return An alternative location.
     */
    public Location computeEstimate(Location dest) {
        float x0, x1, y0, y1, r, m;
        x0 = uc.getLocation().x;
        y0 = uc.getLocation().y;
        x1 = dest.x;
        y1 = dest.y;
        m = (y1 - y0) / (x1 - x0);
        r = (16 - uc.getInfo().getDetectionLevel()) / 2;

        double xCalc = r/Math.sqrt(1 + m * m);
        double yCalc = r/Math.sqrt(1 + 1 / (m * m));

        int xPos = (int)(x0 + xCalc);
        int yPos = (int)(y0 + yCalc);
        int xNeg = (int)(x0 - xCalc);
        int yNeg = (int)(y0 - yCalc);

        Location one = new Location(xPos, yPos);
        Location two = new Location(xNeg, yNeg);

        if (one.distanceSquared(dest) < two.distanceSquared(dest)) {
            return one;
        } else {
            return two;
        }
    }

    public int computeH(Location start, Location end) {
        int dx = Math.abs(start.x - end.x);
        int dy = Math.abs(start.y - end.y);
        int D = 1;
        double D2 = 1.414;

        return (int)(D * (dx + dy) + (D2 - 2 * D) * Math.min(dx, dy));
    }

    /**
     * computeAStar uses the AStar algorithm to generate the shortest feasible
     * path to a location.
     * @param dest The destination the unit is expected to travel to.
     * @return A list of waypoints.
     */
    public ArrayList<Waypoint> computeAStar(Location dest) {
        int energyStart = uc.getEnergyUsed();
        Heap openList = new Heap(2500);
        HashSet<Waypoint> closedList = new HashSet<>();
        ArrayList<Waypoint> waypoints = new ArrayList<>();

        Waypoint start = new Waypoint(uc.getLocation());
        openList.add(start);

        while (openList.count() > 0) {
            Waypoint currentWaypoint = openList.removeFirst();
            closedList.add(currentWaypoint);

            if (currentWaypoint.loc.isEqual(dest)) {
                Waypoint temp = currentWaypoint;
                while (temp != start) {
                    waypoints.add(temp);
                    temp = temp.parent;
                }
                Collections.reverse(waypoints);

                int energyEnd = uc.getEnergyUsed() - energyStart;
                System.out.println(energyEnd);

                return waypoints;
            }

            for (Direction dir : dirs) {
                Waypoint neighbor = new Waypoint(currentWaypoint.loc.add(dir));
                if (!uc.canSenseLocation(neighbor.loc) || !uc.isAccessible(neighbor.loc) || inClosedList(neighbor, closedList) || uc.senseUnitAtLocation(neighbor.loc) != null) {
                    continue;
                }

                int movCost = currentWaypoint.gCost + computeH(currentWaypoint.loc, neighbor.loc);
                if (movCost < neighbor.gCost || !openList.heapContains(neighbor)) {
                    neighbor.gCost = movCost;
                    neighbor.hCost = computeH(neighbor.loc, dest);
                    neighbor.parent = currentWaypoint;

                    if (!openList.heapContains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }

        return waypoints;
    }

    /**
     * getPath generates waypoints for the unit to follow. Currently,
     * only AStar is used - but support for different algorithms will be added.
     * NOTE: if the unit can't see the destination, getPath will pass a location
     * closest to the destination and within the detection range (using algebra).
     * @param dest The destination the unit is expected to travel to.
     * @return A list of waypoints.
     */
    public ArrayList<Waypoint> getPath(Location dest) {
        Location estimate;

        estimate = dest;

        if (!uc.canSenseLocation(dest) || !uc.isAccessible(dest)) {
            estimate = computeEstimate(dest);

            if (!uc.isAccessible(estimate)) {
                for (Direction dir : dirs) {
                    Location neighbor = estimate.add(dir);
                    if (uc.canSenseLocation(neighbor) && uc.isAccessible(neighbor)) {
                        estimate = neighbor;
                        return computeAStar(estimate);
                    }
                }
                estimate = lastResortEstimate(dest);
            }
        }

        if (uc.senseUnitAtLocation(estimate) != null) {
            for (Direction dir : dirs) {
                Location alternative = estimate.add(dir);
                if (uc.canSenseLocation(alternative) && uc.isAccessible(alternative) && uc.senseUnitAtLocation(alternative) == null) {
                    estimate = alternative;
                    break;
                }
            }
        }

        return computeAStar(estimate);
    }

    /**
     * goToLocation controls the movement objective of the unit.
     * It updates the target destination and provides an initial path.
     * @param dest This is the destination the unit is expected to travel to.
     */
    public boolean goToLocation(Location dest) {
        if (!dest.isEqual(currentDestination)) {
            currentDestination = dest;
            currentPath = getPath(dest);
            return false;
        }

        return uc.getLocation().isEqual(dest);
    }

    /**
     * FollowPath should be called every round. It moves the unit
     * along the current path in storage. If the end is reached and
     * the unit isn't at the target destination, a new path will be generated.
     */
    public void FollowPath() {
        if (uc.canMove() && !currentPath.isEmpty()) {
            uc.move(uc.getLocation().directionTo(currentPath.get(0).loc));
            currentPath.remove(0);

            if (currentPath.isEmpty() && !uc.getLocation().isEqual(currentDestination)) {
                currentPath = getPath(currentDestination);
            }
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
}
