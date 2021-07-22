package ourplayer;

import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    boolean torchLighted = false;
    boolean smoke = false;

    void playRound(){
        if (uc.canLightTorch()) {
            uc.lightTorch();
            uc.println("torch lit");
        }

        if (uc.canMakeSmokeSignal()) {
            Location location = uc.getLocation();
            uc.println("Worker location: " + location);
            uc.makeSmokeSignal(encodeSmokeSignal(location, 2, 2000));
        }

        moveRandom();
    }
}
