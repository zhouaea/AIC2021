package demoplayer;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    boolean torchLighted = false;
    boolean smoke = false;

    void playRound(){
        UnitInfo myInfo = uc.getInfo();
        if (uc.getRound() > 300 + myInfo.getID()%200 && !smoke){
            if (uc.canMakeSmokeSignal()){
                uc.makeSmokeSignal(0);
                smoke = true;
            }
        }
        moveRandom();
        if (!torchLighted && myInfo.getTorchRounds() <= 0){
            lightTorch();
        }
        myInfo = uc.getInfo();
        if (myInfo.getTorchRounds() < 70){
            randomThrow();
        }
        int[] signals = uc.readSmokeSignals();
        if (signals.length > 0){
            uc.drawPointDebug(uc.getLocation(), 0, 0, 0);
        }
    }
}
