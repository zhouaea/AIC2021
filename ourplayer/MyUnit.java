package ourplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import aic2021.user.Direction;
import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitType;

public abstract class MyUnit {

    Direction[] dirs = Direction.values();

    UnitController uc;

    Location enemyBaseLocation;

    Map<String, ArrayList<Integer>> memory;

    ArrayList<Location> resources = new ArrayList<>();

    Direction currentDir;

    int roundSpawned;

    int xMax = -1;
    int xMin = -1;
    int yMax = -1;
    int yMin = -1;

    MyUnit(UnitController uc){
        this.uc = uc;
        this.memory = new HashMap<>();
        this.memory.put("resources", new ArrayList<>());
        this.memory.put("traps", new ArrayList<>());
        this.memory.put("mountains", new ArrayList<>());
        this.memory.put("water", new ArrayList<>());
        this.memory.put("enemy", new ArrayList<>());
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
            if (uc.canMove(dirs[random]) && dirs[random] != this.currentDir){
                uc.move(dirs[random]);
                this.currentDir = dirs[random];
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

}