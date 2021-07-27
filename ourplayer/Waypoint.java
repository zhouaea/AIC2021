package ourplayer;

import aic2021.user.Location;

public class Waypoint {
  Location loc;
  int gCost, hCost;
  public Waypoint parent;
  int heapIndex;

  public Waypoint(Location loc) {
    this.loc = loc;
  }

  int getFCost() {
    return gCost + hCost;
  }

  public boolean equals(Waypoint itemToCompare) {
    return loc.isEqual(itemToCompare.loc);
  }

  public int compareTo(Waypoint itemToCompare) {
    if (getFCost() < itemToCompare.getFCost()) {
      return 1;
    } else if (getFCost() == itemToCompare.getFCost()) {
      if (hCost < itemToCompare.hCost) {
        return 1;
      } else {
        return -1;
      }
    } else {
      return -1;
    }
  }
}
