/* class definition for the possible resolutions of the domain space */
public class Resolution {
  private final int dimX;
  private final int dimY;
  private final int spacing;
  private final int padding;

  public Resolution(int newDimX, int newDimY, int spacing, int padding) {
    dimX = newDimX;
    dimY = newDimY;
    this.spacing = spacing;
    this.padding = padding;
  }

  public int getDimX() {
    return dimX;
  }

  public int getDimY() {
    return dimY;
  }

  public String toString() {
    return "Resolution: " + dimX + "x" + dimY;
  }

  public int getSpacing() {
    return spacing;
  }

  public int getPadding() {
    return padding;
  }
}