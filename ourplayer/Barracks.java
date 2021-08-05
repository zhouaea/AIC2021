package ourplayer;

import aic2021.user.*;

public class Barracks extends MyUnit {

    int axemen = 0;

    int maxTroops = -1;

    int maxAxemen = 2;

    Barracks(UnitController uc) {
        super(uc);
    }

    void playRound() {
        decodeMessages();
        spawnTroops();
    }

    private void decodeMessages() {
        int[] smokeSignals = uc.readSmokeSignals();
        Location currentLocation = uc.getLocation();
        DecodedMessage message;

        for (int smokeSignal : smokeSignals) {
            message = decodeSmokeSignal(currentLocation, smokeSignal);
            if (message != null) {
                if (message.unitCode == ENEMY_ARMY_COUNT_REPORT) {
                    this.maxTroops = message.unitId;
                    this.calculateMaxTroops();
                    this.uc.println("Barracks received enemy army size signal. Size: " + this.maxTroops);
                }
            }
        }
    }

    void spawnTroops() {
        if (this.axemen < this.maxAxemen) {
            if (spawnRandom(UnitType.AXEMAN))
                this.axemen++;
        }
    }

    void calculateMaxTroops() {
        if (this.maxTroops > this.maxAxemen) {
            this.maxAxemen = this.maxTroops;
        }
    }
}