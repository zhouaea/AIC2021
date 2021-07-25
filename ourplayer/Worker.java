package ourplayer;

import aic2021.user.Location;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;

public class Worker extends MyUnit {
    Location testLocation = new Location(295, 317);

    Worker(UnitController uc){
        super(uc);
    }

    void playRound(){
        lightTorch();
        if (uc.getRound() >= 10) {
            goToLocation(testLocation);
        }

        FollowPath();
    }
}
