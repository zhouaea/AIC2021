package ourplayer;

import java.util.ArrayList;

import aic2021.user.Team;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Base extends MyUnit {

    int explorers = 0;

    Team team = this.uc.getTeam();
    Team enemy_team = this.uc.getOpponent();

    ArrayList<Integer> messages = new ArrayList<>();

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        if (this.explorers < 1){
            if (spawnRandom(UnitType.EXPLORER)) ++this.explorers;
        }

        // read and record smoke signals
        if (this.uc.canReadSmokeSignals()) {
            int[] signals = this.uc.readSmokeSignals();
            for (int signal : signals) {
                this.messages.add(signal);
            }
            if (signals.length > 0) {
                this.uc.println("Base received " + signals.length + " signals: First signal is: " + signals[0]);
            }
        }

        this.playDefense();
    }

    void playDefense() {
        // Sense enemy units in attack radius and shoot them (add prioritization algorithm later).
        UnitInfo[] shootable_enemies = uc.senseUnits(18, enemy_team);
        for (UnitInfo enemy : shootable_enemies) {
            if (uc.canAttack()) {
                uc.attack(enemy.getLocation());
            }
        }
    }

}
