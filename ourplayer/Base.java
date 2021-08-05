package ourplayer;

import java.util.ArrayList;
import java.util.HashSet;

import aic2021.user.Location;
import aic2021.user.ResourceInfo;
import aic2021.user.Team;
import aic2021.user.Technology;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Base extends MyUnit {

  int workers = 0;
  int explorers = 0;
  final int MAX_WORKERS = 5;
  final int MAX_EXPLORERS = 1;

  Team team = uc.getTeam();
  Team enemyTeam = uc.getOpponent();

  final int WATER_TILE_THRESHOLD = 15;
  boolean waterMode = false;

  boolean newEnemiesSeen = false;
  int enemyArmySize = 0;
  HashSet<Integer> seenEnemies = new HashSet<>();

  boolean hasAssignedWorkerAsBarrackBuilder = false;
  int barrackBuilderId = 0;
  boolean hasAssignedWorkerAsBuilder = false;
  int builderId = 0;

  Base(UnitController uc){
    super(uc);
  }

  // TODO If base sees traps, spawn trapper
  void playRound(){
    checkForWater();
    playDefense();
    countEnemies();
    spawnTroops();
    researchTech(); // TODO change research path based on different states like "normal", "water", etc.
    makeBuilders();


    uc.println("energy used: " + uc.getEnergyUsed());
    uc.println("energy left: " + uc.getEnergyLeft());
  }

  void checkForWater() {
    if (uc.getRound() == 0) {
      Location[] waterTiles = uc.senseWater(50);
      if (waterTiles.length >= WATER_TILE_THRESHOLD) {
        waterMode = true;
        uc.println("water mode activated");
      }
    }
  }

  void playDefense() {
    // Sense enemy units in attack radius and shoot them (add prioritization algorithm later).
    UnitInfo[] shootable_enemies = uc.senseUnits(18, enemyTeam);
    for (UnitInfo enemy : shootable_enemies) {
      if (uc.canAttack()) {
        uc.attack(enemy.getLocation());
      }
    }
  }

  void countEnemies() {
    for (UnitInfo enemyInfo : this.uc.senseUnits(this.enemyTeam)) {
      if (!this.seenEnemies.contains(enemyInfo.getID()) && (enemyInfo.getType() == UnitType.AXEMAN || enemyInfo.getType() == UnitType.SPEARMAN)) {
        this.enemyArmySize++;
        this.seenEnemies.add(enemyInfo.getID());
        newEnemiesSeen = true;
      }
    }

    // Try to send army size smoke signal every round new enemies appear in range.
    if (newEnemiesSeen) {
      if (this.uc.canMakeSmokeSignal()) {
        this.uc.makeSmokeSignal(encodeSmokeSignal(enemyArmySize, ENEMY_ARMY_COUNT_REPORT, 0));
        this.uc.println(
                "Enemy army size smoke signal fired on round "
                        + this.uc.getRound()
                        + ". Enemies: "
                        + this.enemyArmySize);
        newEnemiesSeen = false;
      }
    }
  }

  void senseEnemies() {
    // Sense enemy units in vision radius and alert team of how many are attacking and what kind.
    // Prioritize: enemy base -> scouts -> buildings -> wolves -> axemen -> spearmen -> workers -> trappers
    // UnitInfo[] enemies = uc.senseUnits(enemy_team);

  }

  void senseResources() {
    // Sense resources in range and alert team of each.
    ResourceInfo[] nearby_resources = uc.senseResources();
    for (ResourceInfo resource : nearby_resources) {
      // Communicate each resource's location, type, and amount via smoke signalling
    }
  }

  void researchTech() {
    if (uc.getTechLevel(team) == 0)
      researchTechLevel0();
    else {
      if (workers < 1)
        spawnRandom(UnitType.WORKER);

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
    if (waterMode) {
      if (uc.canResearchTechnology(Technology.RAFTS)) {
        uc.researchTechnology(Technology.RAFTS);
        return;
      }

      if (uc.canResearchTechnology(Technology.MILITARY_TRAINING) && uc.hasResearched(Technology.RAFTS, team)) {
        uc.researchTechnology(Technology.MILITARY_TRAINING);
        return;
      }
      if (uc.canResearchTechnology(Technology.UTENSILS) && uc.hasResearched(Technology.RAFTS, team) && uc.hasResearched(Technology.MILITARY_TRAINING, team)) {
        uc.researchTechnology(Technology.UTENSILS);
      }
    } else {
      if (uc.canResearchTechnology(Technology.UTENSILS)) {
        uc.researchTechnology(Technology.UTENSILS);
        return;
      }
      if (uc.canResearchTechnology(Technology.MILITARY_TRAINING) && uc.hasResearched(Technology.UTENSILS, team)) {
        uc.researchTechnology(Technology.MILITARY_TRAINING);
        return;
      }
      if (uc.canResearchTechnology(Technology.BOXES) && uc.hasResearched(Technology.UTENSILS, team) && uc.hasResearched(Technology.MILITARY_TRAINING, team)) {
        uc.researchTechnology(Technology.BOXES);
      }
    }
  }

  private void researchTechLevel1() {
    if (uc.canResearchTechnology(Technology.TACTICS)) {
      uc.researchTechnology(Technology.TACTICS);
      return;
    }

    if (uc.canResearchTechnology(Technology.JOBS) && uc.hasResearched(Technology.TACTICS, team)) {
      uc.researchTechnology(Technology.JOBS);
      return;
    }

    if (uc.canResearchTechnology(Technology.COOKING) && uc.hasResearched(Technology.TACTICS, team) && uc.hasResearched(Technology.JOBS, team)) {
      uc.researchTechnology(Technology.COOKING);
    }
  }

  private void researchTechLevel2() {
    if (uc.canResearchTechnology(Technology.CRYSTALS)) {
      uc.researchTechnology(Technology.CRYSTALS);
      return;
    }

    if (uc.canResearchTechnology(Technology.COMBUSTION)) {
      uc.researchTechnology(Technology.COMBUSTION);
      return;
    }

    if (uc.canResearchTechnology(Technology.POISON)) {
      uc.researchTechnology(Technology.POISON);
    }
  }

  void spawnTroops() {
    if (explorers < MAX_EXPLORERS) {
      if (spawnRandom(UnitType.EXPLORER))
        explorers++;
    }

    if (workers < MAX_WORKERS) {
      if (spawnRandom(UnitType.WORKER))
        workers++;
    }
  }

  // TODO send smoke signals to workers about resources in range
  void senseTerrain() {
    // Sense terrain in range, and maybe let troops know about important spots.
    Location[] water_tiles = uc.senseWater(50);
    Location[] mountain_tiles = uc.senseMountains(50);
  }


  void makeBuilders() {
    if (uc.hasResearched(Technology.MILITARY_TRAINING, team)) {
      if (!hasAssignedWorkerAsBarrackBuilder) {
        makeBarrackBuilder();
      }
    }

    if (uc.hasResearched(Technology.JOBS, team)) {
      if (!hasAssignedWorkerAsBuilder) {
        makeBuilder();
      }
    }
  }

  /**
   * Spawn a worker, get its id, and tell it to enter building mode.
   */
  private void makeBarrackBuilder() {
    // Cannot proceed unless worker is built.
    if (!spawnRandom(UnitType.WORKER)) {
      return;
    }

    UnitInfo[] allyUnits = uc.senseUnits(2, team);
    for (UnitInfo unit : allyUnits) {
      if (unit.getType() == UnitType.WORKER) {
        barrackBuilderId = unit.getID();
        break;
      }
    }

    // Builder is not assigned until smoke signal is sent.
    if (uc.canMakeSmokeSignal()) {
      uc.println(barrackBuilderId);
      uc.makeSmokeSignal(encodeSmokeSignal(barrackBuilderId, ASSIGN_BARRACK_BUILDER, 1));
      hasAssignedWorkerAsBarrackBuilder = true;
      uc.println("worker is now a builder");
    }
  }

  /**
   * Spawn a worker, get its id, and tell it to enter building mode.
   */
  private void makeBuilder() {
    // Cannot proceed unless worker is built.
    if (!spawnRandom(UnitType.WORKER)) {
      return;
    }

    UnitInfo[] allyUnits = uc.senseUnits(2, team);
    for (UnitInfo unit : allyUnits) {
      if (unit.getType() == UnitType.WORKER) {
        builderId = unit.getID();
        break;
      }
    }

    // Builder is not assigned until smoke signal is sent.
    if (uc.canMakeSmokeSignal()) {
      uc.println(builderId);
      uc.makeSmokeSignal(encodeSmokeSignal(builderId, ASSIGN_BUILDER, 1));
      hasAssignedWorkerAsBuilder = true;
      uc.println("worker is now a builder");
    }
  }
}
