package ourplayer;

import aic2021.user.Location;

public class DecodedMessage {
  Location location;
  int unitCode;
  int unitAmount;
  int unitId;

  DecodedMessage(Location location, int unitCode, int unitAmount) {
    this.location = location;
    this.unitCode = unitCode;
    this.unitAmount = unitAmount;
  }

  DecodedMessage(int unitId, int unitCode, int unitAmount) {
    this.unitId = unitId;
    this.unitCode = unitCode;
    this.unitAmount = unitAmount;
  }

  public String toString() {
    return "Location: " + this.location.toString() + " Unit code: " + unitCode + " Number of units: " + unitAmount;
  }
}
