package ourplayer;

import aic2021.user.*;

public class Base extends MyUnit {

    int workers = 0;
    Team team = uc.getTeam();

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        researchTech();
    }

    void researchTech() {
        if (uc.getTechLevel(team) == 0) {
            researchTechLevel0();
        } else {
            if (workers < 1) {
                spawnRandom(UnitType.WORKER);
            }

            if (uc.getTechLevel(team) == 1)
                researchTechLevel1();
            else if (uc.getTechLevel(team) == 2)
                researchTechLevel2();
            else
                if (uc.canResearchTechnology(Technology.WHEEL))
                    uc.researchTechnology(Technology.WHEEL);
        }
    }

    private void researchTechLevel0() {
        if (uc.canResearchTechnology(Technology.COIN)) {
            uc.researchTechnology(Technology.COIN);
            return;
        }
        if (uc.canResearchTechnology(Technology.BOXES) && uc.hasResearched(Technology.COIN, team)) {
            uc.researchTechnology(Technology.BOXES);
            return;
        }
        if (uc.canResearchTechnology(Technology.UTENSILS) && uc.hasResearched(Technology.COIN, team)) {
            uc.researchTechnology(Technology.UTENSILS);
            return;
        }
    }

    private void researchTechLevel1() {
        if (uc.canResearchTechnology(Technology.JOBS)) {
            uc.researchTechnology(Technology.JOBS);
            return;
        }

        if (uc.canResearchTechnology(Technology.HUTS) && uc.hasResearched(Technology.JOBS, team)) {
            uc.researchTechnology(Technology.HUTS);
            return;
        }

        if (uc.canResearchTechnology(Technology.NAVIGATION) && uc.hasResearched(Technology.JOBS, team)) {
            uc.researchTechnology(Technology.NAVIGATION);
            return;
        }
    }

    private void researchTechLevel2() {
        if (uc.canResearchTechnology(Technology.EXPERTISE)) {
            uc.researchTechnology(Technology.EXPERTISE);
            return;
        }

        // Switch this with stone or wood tech if resources are widely available.
        if (uc.canResearchTechnology(Technology.POISON)) {
            uc.researchTechnology(Technology.POISON);
            return;
        }

        if (uc.canResearchTechnology(Technology.HOUSES)) {
            uc.researchTechnology(Technology.HOUSES);
            return;
        }
    }

    boolean spawnRandom(UnitType t){
        for (Direction dir : dirs){
            if (uc.canSpawn(t, dir)){
                uc.spawn(t, dir);
                workers++;
                return true;
            }
        }
        return false;
    }
}
