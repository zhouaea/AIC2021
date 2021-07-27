package ourplayer;

public class Heap {
  Waypoint[] waypoints;
  int itemCount;

  public Heap(int maxSize) {
    waypoints = new Waypoint[maxSize];
  }

  public void add(Waypoint waypoint) {
    waypoint.heapIndex = itemCount;
    waypoints[itemCount] = waypoint;
    sortUp(waypoint);
    itemCount++;
  }

  public Waypoint removeFirst() {
    Waypoint first = waypoints[0];
    itemCount--;
    waypoints[0] = waypoints[itemCount];
    waypoints[0].heapIndex = 0;
    sortDown(waypoints[0]);
    return first;
  }

  public void updateItem(Waypoint item) {
    sortUp(item);
  }

  public int count() {
    return itemCount;
  }

  public boolean heapContains(Waypoint item) {
    for (int i = 0; i < itemCount; i++) {
      if (waypoints[i].equals(item)) {
        return true;
      }
    }
    return false;
  }

  void sortDown(Waypoint item) {
    while (true) {
      int childIndexLeft = item.heapIndex * 2 + 1;
      int childIndexRight = item.heapIndex * 2 + 2;
      int swapIndex = 0;

      if (childIndexLeft < itemCount) {
        swapIndex = childIndexLeft;

        if (childIndexRight < itemCount) {
          if (waypoints[childIndexLeft].compareTo(waypoints[childIndexRight]) < 0) {
            swapIndex = childIndexRight;
          }
        }

        if (item.compareTo(waypoints[swapIndex]) < 0) {
          swap(item, waypoints[swapIndex]);
        } else {
          return;
        }
      } else {
        return;
      }
    }
  }

  void sortUp(Waypoint item) {
    int parentIndex = (item.heapIndex - 1) / 2;

    while(true) {
      Waypoint parentItem = waypoints[parentIndex];
      if (item.compareTo(parentItem) > 0) {
        swap(item, parentItem);
      } else {
        break;
      }

      parentIndex = (item.heapIndex - 1) / 2;
    }
  }

  void swap(Waypoint w1, Waypoint w2) {
    waypoints[w1.heapIndex] = w2;
    waypoints[w2.heapIndex] = w1;
    int w1Index = w1.heapIndex;
    w1.heapIndex = w2.heapIndex;
    w2.heapIndex = w1Index;
  }
}
