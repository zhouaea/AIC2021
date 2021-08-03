package ourplayer;

import aic2021.user.Direction;
import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Barracks extends MyUnit {

    int axemen = 0;
    int spearmen = 0;

    Barracks(UnitController uc) {
        super(uc);
    }

    void playRound() {
        if (this.axemen < 10) {
            if (this.uc.canSpawn(UnitType.AXEMAN, Direction.NORTH)) {
                this.uc.spawn(UnitType.AXEMAN, Direction.NORTH);
                this.uc.println("spawned axeman north");
                this.axemen++;
//        return;
            } else {
                this.uc.println("can't spawn axeman north");
            }
            if (this.uc.canSpawn(UnitType.AXEMAN, Direction.SOUTH)) {
                this.uc.spawn(UnitType.AXEMAN, Direction.SOUTH);
                this.uc.println("spawned axeman south");
                this.axemen++;
//        return;
            } else {
                this.uc.println("can't spawn axeman south");
            }
        }

        if (this.spearmen < 10) {
            if (this.uc.canSpawn(UnitType.SPEARMAN, Direction.NORTH)) {
                this.uc.spawn(UnitType.SPEARMAN, Direction.NORTH);
                this.uc.println("spawned spearman north");
                this.spearmen++;
//        return;
            } else {
                this.uc.println("can't spawn spearman north");
            }
            if (this.uc.canSpawn(UnitType.SPEARMAN, Direction.SOUTH)) {
                this.uc.spawn(UnitType.SPEARMAN, Direction.SOUTH);
                this.uc.println("spawned spearman south");
                this.spearmen++;
//        return;
            } else {
                this.uc.println("can't spawn spearman south");
            }
        }
    }
}