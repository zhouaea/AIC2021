package ourplayer;

import aic2021.user.Location;
import aic2021.user.UnitController;

public class Explorer extends MyUnit {
    Location testLocation = new Location(295, 317);

    Explorer(UnitController uc){
        super(uc);
    }

    void playRound(){
        lightTorch();
        goToLocation(testLocation);

        FollowPath();
    }

}
