package ourplayer;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitType;
import aic2021.user.GameConstants;
import java.lang.Math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

class Waypoint {
    Location loc;
    int gCost, hCost;
    public Waypoint parent;
    int heapIndex;

    public Waypoint(Location loc) {
        this.loc = loc;
    }

    int getFCost() {
        return gCost + hCost;
    }

    public boolean equals(Waypoint itemToCompare) {
        return loc.isEqual(itemToCompare.loc);
    }

    public int compareTo(Waypoint itemToCompare) {
        if (getFCost() < itemToCompare.getFCost()) {
            return 1;
        } else if (getFCost() == itemToCompare.getFCost()) {
            if (hCost < itemToCompare.hCost) {
                return 1;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }
}

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
                        break;
                    }
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
}


