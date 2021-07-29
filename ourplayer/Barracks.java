package ourplayer;

import aic2021.user.Direction;
import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Barracks extends MyUnit {

  int axemen = 0;

  Barracks(UnitController uc) {
    super(uc);
  }

  void playRound() {
    if (this.axemen < 5) {
      if (this.uc.canSpawn(UnitType.AXEMAN, Direction.NORTH)) {
        this.uc.spawn(UnitType.AXEMAN, Direction.NORTH);
        this.uc.println("spawned axeman north");
        this.axemen++;
        return;
      } else {
        this.uc.println("can't spawn axeman north");
      }
      if (this.uc.canSpawn(UnitType.AXEMAN, Direction.SOUTH)) {
        this.uc.spawn(UnitType.AXEMAN, Direction.SOUTH);
        this.uc.println("spawned axeman south");
        this.axemen++;
        return;
      } else {
        this.uc.println("can't spawn axeman south");
      }
    }
  }
}
