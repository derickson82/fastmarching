import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class DShape {
  private Shape shape;
  Point2D.Double position;
  Dimension dim;
  boolean isPressed;

  int xpnts[];
  int ypnts[];
  int pntCount;

  int type; // rectangle, ellipse, or triangle

  // additions
  static final int RECTANGLE = 1;
  static final int ELLIPSE = 2;
  static final int TRIANGLE = 3;

  final int DEFAULT_WIDTH = 40;
  final int DEFAULT_HEIGHT = 40;
  final Dimension DEFAULT_DIM = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);

  // default constructor creates a rectangle
  DShape() {
    position = new Point2D.Double(75, 325);
    dim = new Dimension(DEFAULT_DIM);
    isPressed = false;

    shape = new Rectangle2D.Double();

    ((Rectangle2D.Double) shape).setFrame(position, dim);
  }

  // copy constructor
  DShape(DShape obj) {
    position = new Point2D.Double(obj.getX(), obj.getY());
    dim = new Dimension(obj.dim);
    isPressed = obj.isPressed;
    type = obj.type;

    // create a rectangle
    if (type == RECTANGLE) {
      shape = new Rectangle2D.Double();
      ((Rectangle2D.Double) shape).setFrame(position, dim);
    }

    // create an ellipse
    else if (type == ELLIPSE) {
      shape = new Ellipse2D.Double();
      ((Ellipse2D.Double) shape).setFrame(position, dim);
    }

    else if (type == TRIANGLE) {
      // three vertices for a triangle
      pntCount = 3;

      xpnts = new int[pntCount];
      ypnts = new int[pntCount];

      xpnts[0] = (int) position.getX();
      ypnts[0] = (int) (position.getY() - dim.getHeight() / 2.0);

      xpnts[1] = (int) (position.getX() - dim.getWidth() / 2.0);
      ypnts[1] = (int) (position.getY() + dim.getHeight() / 2.0);

      xpnts[2] = (int) (position.getX() + dim.getWidth() / 2.0);
      ypnts[2] = (int) (position.getY() + dim.getHeight() / 2.0);

      shape = new Polygon(xpnts, ypnts, pntCount);
    }

    this.setFrame(position, dim);
  }

  // overloaded constructor creates a default rectangle
  DShape(double x, double y, int width, int height) {
    position = new Point2D.Double(x, y);
    dim = new Dimension(width, height);
    isPressed = false;

    shape = new Rectangle2D.Double();

    ((Rectangle2D.Double) shape).setFrame(position, dim);
  }

  // overloaded constructor creates specified shape
  DShape(int newType) {
    position = new Point2D.Double(75, 325);
    dim = new Dimension(DEFAULT_DIM);
    isPressed = false;
    type = newType;

    // create a rectangle
    if (type == RECTANGLE) {
      shape = new Rectangle2D.Double();

      ((Rectangle2D.Double) shape).setFrame(position, dim);
    }

    // create an ellipse
    else if (type == ELLIPSE) {
      shape = new Ellipse2D.Double();

      ((Ellipse2D.Double) shape).setFrame(position, dim);
    }

    else if (type == TRIANGLE) {
      // three vertices for a triangle
      pntCount = 3;

      xpnts = new int[pntCount];
      ypnts = new int[pntCount];

      xpnts[0] = (int) position.getX();
      ypnts[0] = (int) (position.getY() - dim.getHeight() / 2.0);

      xpnts[1] = (int) (position.getX() - dim.getWidth() / 2.0);
      ypnts[1] = (int) (position.getY() + dim.getHeight() / 2.0);

      xpnts[2] = (int) (position.getX() + dim.getWidth() / 2.0);
      ypnts[2] = (int) (position.getY() + dim.getHeight() / 2.0);

      shape = new Polygon(xpnts, ypnts, pntCount);
    }
  }

  public void setLocation(double x, double y) {
    position.setLocation(x, y);
    this.setFrame(position, dim);
  }

  public void setSize(double width, double height) {
    dim.setSize(width, height);
    this.setFrame(position, dim);
  }

  public void setFrame(Point2D.Double newPos, Dimension newDim) {
    if (type == RECTANGLE) {
      ((Rectangle2D.Double) shape).setFrame(position, dim);
    } else if (type == ELLIPSE) {
      ((Ellipse2D.Double) shape).setFrame(position, dim);
    } else if (type == TRIANGLE) {
      xpnts[0] = (int) position.getX();
      ypnts[0] = (int) (position.getY() - dim.getHeight() / 2.0);

      xpnts[1] = (int) (position.getX() - dim.getWidth() / 2.0);
      ypnts[1] = (int) (position.getY() + dim.getHeight() / 2.0);

      xpnts[2] = (int) (position.getX() + dim.getWidth() / 2.0);
      ypnts[2] = (int) (position.getY() + dim.getHeight() / 2.0);

      shape = new Polygon(xpnts, ypnts, pntCount);
    }
  }

  public double getX() {
    return position.getX();
  }

  public double getY() {
    return position.getY();
  }

  public double getWidth() {
    return dim.getWidth();
  }

  public double getHeight() {
    return dim.getHeight();
  }

  public boolean contains(int x, int y) {
    return shape.contains(x, y);
  }

  public Object clone() {
    return new DShape(getX(), getY(), (int) getWidth(), (int) getHeight());
  }

  public void draw(Graphics2D g2d) {
    g2d.draw(shape);
  }

  public Rectangle2D getBoundingBox() {
    return shape.getBounds2D();
  }
}