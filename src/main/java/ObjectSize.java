/* class definition for the possible size of the obstacles */
public class ObjectSize {
  private final double width;
  private final double height;

  ObjectSize(double x, double y) {
    this.width = x;
    this.height = y;
  }

  public double getWidth() {
    return width;
  }

  public double getHeight() {
    return height;
  }

  public String toString() {
    return (width + "x" + height);
  }
}