package ourplayer;

import aic2021.user.Location;
import aic2021.user.UnitController;

public class Explorer extends MyUnit {
    Location testLocation = new Location(627, 406);

    Explorer(UnitController uc){
        super(uc);
    }

    void playRound(){
        lightTorch();
        goToLocation(testLocation);

        FollowPath();
    }

}
