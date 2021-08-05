package ourplayer;

import aic2021.engine.Unit;
import aic2021.user.*;

import java.util.*;

public class Base extends MyUnit {

    HashMap<UnitType, Integer> buildings = new HashMap<>();
    String[] priorities = {"Research", "Economy", "Army"};
    String currentPriority = "";
    Queue<Technology> techTree = new LinkedList<>();
    String gamePeriod = "Early";
    int workers = 0;

    Direction directionToEnemy;

    Base(UnitController uc){
        super(uc);

        directionToEnemy = getPreferredDirection();
    }
/*
    void research() {
        Technology targetTech = techTree.peek();

        if (uc.canResearchTechnology(targetTech)) {
            uc.researchTechnology(targetTech);

            if (targetTech == Technology.JOBS)
                gamePeriod = "Mid";

            if (targetTech == Technology.SCHOOLS)
                gamePeriod = "Late";

            techTree.poll();
        }
    }

    void growEconomy() {
        if (gamePeriod.equals("Early")) {
            while (workers < 6 && spawnRandom(UnitType.WORKER)) {
                workers++;
            }
        } else if (gamePeriod.equals("Mid")) {

        } else {

        }
    }
*/
    boolean jump(Location tile, Direction dir) {
        Location testTile = tile.add(dir);

        if (!uc.canSenseLocation(testTile))
            return uc.isOutOfMap(testTile);

        return jump(testTile, dir);
    }

    Direction getPreferredDirection() {
        boolean rightEdge = jump(uc.getLocation(), Direction.EAST);
        boolean leftEdge = jump(uc.getLocation(), Direction.WEST);
        boolean bottomEdge = jump(uc.getLocation(), Direction.SOUTH);
        boolean topEdge = jump(uc.getLocation(), Direction.NORTH);

        if (rightEdge && topEdge && bottomEdge)
            return Direction.WEST;
        else if (leftEdge && topEdge && bottomEdge)
            return Direction.EAST;
        else if (topEdge && leftEdge && rightEdge)
            return Direction.SOUTH;
        else if (bottomEdge && leftEdge && rightEdge)
            return Direction.NORTH;
        else if (rightEdge && topEdge)
            return Direction.SOUTHWEST;
        else if (leftEdge && topEdge)
            return Direction.SOUTHEAST;
        else if (rightEdge && bottomEdge)
            return Direction.NORTHWEST;
        else if (leftEdge && bottomEdge)
            return Direction.NORTHEAST;
        else if (rightEdge)
            return Direction.WEST;
        else if (leftEdge)
            return Direction.EAST;
        else if (topEdge)
            return Direction.SOUTH;
        else if (bottomEdge)
            return Direction.NORTH;
        else
            return Direction.ZERO;
    }

    void playRound(){
        if (uc.getRound() == 0) {
            spawnRandom(UnitType.EXPLORER);
        }


    }

}
