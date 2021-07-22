package ourplayer;

import aic2021.user.Location;
import aic2021.user.UnitController;

public class Explorer extends MyUnit {

    Explorer(UnitController uc){
        super(uc);
    }

    void playRound(){
        Location testLocation = new Location(313, 310);
        goToLocation(testLocation);
    }

}
