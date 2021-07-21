package ourplayer;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitType;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.ArrayList;

class FComparator implements Comparator<Waypoint> {
    public int compare(Waypoint w1, Waypoint w2) {
        if (w1.getFCost() == w2.getFCost()) {
            return 0;
        } else if (w1.getFCost() < w2.getFCost()) {
            return -1;
        } else {
            return 1;
        }
    }
}

class Waypoint {
    Location loc;
    int gCost, hCost;
    public Waypoint parent;

    Waypoint(Location loc) {
        this.loc = loc;
    }

    int getFCost() {
        return gCost + hCost;
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
    public PriorityQueue<Waypoint> ComputePath(Location dest) {
        ArrayList<Waypoint> openList = new ArrayList<>();
        ArrayList<Waypoint> closedList = new ArrayList<>();
        PriorityQueue<Waypoint> waypoints = new PriorityQueue<>(new FComparator());

        Waypoint start = new Waypoint(uc.getLocation());
        openList.add(start);

        while (!openList.isEmpty()) {
            Waypoint currentWaypoint = openList.get(0);
            for (Waypoint w : openList) {
                if (w.getFCost() < currentWaypoint.getFCost() || w.getFCost() == currentWaypoint.getFCost() && w.hCost < currentWaypoint.hCost) {
                    currentWaypoint = w;
                }
            }

            openList.remove(currentWaypoint);
            closedList.add(currentWaypoint);

            if (currentWaypoint.loc.isEqual(dest)) {
                Waypoint temp = currentWaypoint;
                while (temp != start) {
                    waypoints.add(temp);
                    temp = temp.parent;
                }

                return waypoints;
            }

            for (Direction dir : dirs) {
                Waypoint neighbor = new Waypoint(currentWaypoint.loc.add(dir));
                if (!uc.canSenseLocation(neighbor.loc) || !uc.isAccessible(neighbor.loc) || closedList.contains(neighbor) || uc.senseUnitAtLocation(neighbor.loc) != null) {
                    continue;
                }

                int movCost = currentWaypoint.gCost + currentWaypoint.loc.distanceSquared(neighbor.loc);
                if (movCost < neighbor.gCost || !openList.contains(neighbor)) {
                    neighbor.gCost = movCost;
                    neighbor.hCost = neighbor.loc.distanceSquared(dest);
                    neighbor.parent = currentWaypoint;

                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }

        return waypoints;
    }

    public boolean goToLocation(Location dest) {
        if (uc.canMove()) {
            PriorityQueue<Waypoint> path = ComputePath(dest);

            while (!path.isEmpty()) {

            }
        }

        return false;
    }
}


