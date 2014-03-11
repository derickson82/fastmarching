import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

/*	This will be the class that extends JPanel and implements MouseInputAdapter
 */
class DomainPanel extends JPanel implements MouseListener, MouseMotionListener {
  private DShape startObject; // starting object
  private DShape endObject; // ending object

  private DShape rectanglePalette; // rectangle palette
  private DShape ellipsePalette; // ellipse palette
  private DShape trianglePalette; // triangle palette

  private List<DShape> obstacles; // set of obstacles in domain space

  private static final int PALETTE_WIDTH = 150; // width of the obstacle palette
  private boolean isFirstPosition; // first time drawing?
  private boolean isInPanel; // is the mouse inside the panel
  private boolean doFmm;

  // Graphics2D g2d; //graphics handle

  // this is the stuff for the fast marching method
  private static final double INF = 999999;
  // private static final double straight = 1;
  // private static final double diagonal = 1.4142; // square root of 2

  // keep track of mesh/grid size, spacing, and padding
  private int dimX;
  private int dimY;
  private int spacing;
  private int padding;

  // sets for fmm
  private List<Node> Fn;
  private List<Node> Rn;
  private List<Node> An;

  private Node nodes[][]; // the actual node array

  private class Node {
    int x; // x coordinate
    int y; // y coordinate
    boolean isFeasible;
    double travelTime;

    // default constructor
    Node() {
      x = -1;
      y = -1;
      isFeasible = true;
      travelTime = INF;
    }

    // explicit value constructor
    Node(int newX, int newY) {
      x = newX;
      y = newY;
      isFeasible = true;
      travelTime = INF;
    }

    // to string
    public String toString() {
      return ("Node " + x + " " + y);
    }

    // paints node in given graphics context
    public void paintNode(Graphics2D g2d) {
      g2d.fillOval(x * spacing + PALETTE_WIDTH + padding,
          y * spacing + padding, 2, 2);
    }
  }

  public void setRectangleSize(ObjectSize size) {
    rectanglePalette.setSize(size.getWidth(), size.getHeight());
  }

  public void setCircleSize(ObjectSize size) {
    ellipsePalette.setSize(size.getWidth(), size.getHeight());
  }

  public void setTriangleSize(ObjectSize size) {
    trianglePalette.setSize(size.getWidth(), size.getHeight());
  }

  // constructor sets up the panel
  public DomainPanel() {
    // add this panel to the mouse listener
    addMouseListener(this);
    addMouseMotionListener(this);

    // start object initialization
    startObject = new DShape(DShape.ELLIPSE);
    startObject.setSize(4, 4);

    // end object initialization
    endObject = new DShape(DShape.ELLIPSE);
    endObject.setSize(4, 4);

    // shapes in palette
    rectanglePalette = new DShape(DShape.RECTANGLE);
    ellipsePalette = new DShape(DShape.ELLIPSE);
    trianglePalette = new DShape(DShape.TRIANGLE);

    // obstacle set
    obstacles = new ArrayList<DShape>();

    // initial mesh/grid size
    dimX = 32;
    dimY = 32;
    spacing = 16;
    padding = 6;

    isFirstPosition = true; // upon creation, start object is in first position
    isInPanel = true; // is the mouse inside the panel?
    doFmm = false;

    // sets for fmm
    Fn = new ArrayList<Node>();
    Rn = new ArrayList<Node>();
    An = new ArrayList<Node>();

    // initialize the nodes
    nodes = new Node[dimX][dimY];
    init();
  }

  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g; // cast graphics context
    g2d.setStroke(new BasicStroke(1.0f)); // basic paint stroke

    // initial painting
    if (isFirstPosition) {
      Dimension dim = getSize();
      int w = (int) dim.getWidth();
      int h = (int) dim.getHeight();

      startObject.setLocation(w / 2 - startObject.getWidth() / 2, h / 2
          - startObject.getHeight() / 2);

      // end object location and dimention
      endObject.setLocation(w - 30, h - 30);
      endObject.setSize(4, 4);

      rectanglePalette.setLocation(50, 75);
      ellipsePalette.setLocation(50, 200);
      trianglePalette.setLocation(75, 365);

      isFirstPosition = false;
    }

    // prints the existing obstacles
    for (DShape temp : obstacles) {
      temp.draw(g2d);
    }

    // draw start object in blue
    g2d.setColor(Color.blue);
    startObject.draw(g2d);

    // draw end object in magenta
    g2d.setColor(Color.magenta);
    endObject.draw(g2d);

    // draw the obstacles in black
    g2d.setColor(Color.black);
    rectanglePalette.draw(g2d);
    ellipsePalette.draw(g2d);
    trianglePalette.draw(g2d);

    // draw a dividing line between paletted and domain space
    g2d.drawLine(PALETTE_WIDTH, (int) getSize().getHeight(), PALETTE_WIDTH, 0);

    // draw a rectangle as a border
    g2d.drawRect(0, 0, (int) getSize().getWidth() - 1, (int) getSize()
        .getHeight() - 1);

    // draw each node in level sets
    for (int nodeX = 0; nodeX < dimX; nodeX++) {
      for (int nodeY = 0; nodeY < dimY; nodeY++) {

        double tempTime = nodes[nodeX][nodeY].travelTime;

        if (Double.compare(tempTime, INF) == 0) {
          g2d.setColor(Color.black);
        } else {
          g2d.setColor(tempTime % 10 < 5 ? Color.red : Color.blue);
        }

        nodes[nodeX][nodeY].paintNode(g2d);
      }
    }

    if (doFmm)
      findPath(g2d);

    // clean up
    g2d.dispose();
  }

  /* this function computes the feasibility of each node based on the obstacles */
  public void computeFeasible() {
    Rectangle2D box; // bounding box of each shape
    int boxX; // coordinates of box
    int boxY;
    int boxWidth; // dimensions of box
    int boxHeight;

    for (DShape tempShape : obstacles) {
      // get the dimensions of the bounding box
      box = tempShape.getBoundingBox();
      boxX = (int) box.getX();
      boxY = (int) box.getY();
      boxWidth = (int) box.getWidth();
      boxHeight = (int) box.getHeight();

      // convert to indicies of nodes in a certain range
      boxWidth += boxX;
      boxHeight += boxY;

      // find the nodes
      boxX = (boxX - PALETTE_WIDTH - padding) / spacing;
      boxY = (boxY - padding) / spacing;
      boxWidth = (boxWidth - PALETTE_WIDTH - padding) / spacing;
      boxHeight = (boxHeight - padding) / spacing;

      // restrict the dimensions to within the range of indicies
      if (boxWidth > dimX - 1)
        boxWidth = dimX - 1;
      if (boxHeight > dimY - 1)
        boxHeight = dimY - 1;
      if (boxX < 1)
        boxX = 1;
      if (boxY < 1)
        boxY = 1;

      for (int x = boxX - 1; x < boxWidth + 1; x++) {
        for (int y = boxY - 1; y < boxHeight + 1; y++) {
          if (tempShape.contains(x * spacing + PALETTE_WIDTH + padding, y
              * spacing + padding)) {
            nodes[x][y].isFeasible = false;
            Fn.remove(nodes[x][y]);
          }
        }
      }
    }
  }

  /* this function resets the feasibility of all the points in the domain */
  public void resetFeasible() {
    for (int x = 0; x < dimX; x++) {
      for (int y = 0; y < dimY; y++) {
        nodes[x][y].isFeasible = true;
      }
    }
  }

  public void setResolution(Resolution resolution) {
    dimX = resolution.getDimX();
    dimY = resolution.getDimY();
    spacing = resolution.getSpacing();
    padding = resolution.getSpacing();

    nodes = new Node[dimX][dimY];
    init();
    repaint();
  }

  // Handles the event of the user pressing down the mouse button.
  public void mousePressed(MouseEvent e) {
    if (startObject.contains(e.getX(), e.getY())) {
      startObject.setLocation(e.getX() - startObject.getWidth() / 2, e.getY()
          - startObject.getHeight() / 2);
      startObject.isPressed = true;
    } else if (endObject.contains(e.getX(), e.getY())) {
      endObject.setLocation(e.getX() - endObject.getWidth() / 2, e.getY()
          - endObject.getHeight() / 2);
      endObject.isPressed = true;
    } else if (rectanglePalette.contains(e.getX(), e.getY())) {
      DShape temp = new DShape(rectanglePalette);
      double tempX = temp.getWidth() / 2;
      double tempY = temp.getHeight() / 2;

      temp.setLocation(e.getX() - tempX, e.getY() - tempY);
      rectanglePalette.isPressed = true;
      temp.isPressed = true;
      obstacles.add(temp);
    } else if (ellipsePalette.contains(e.getX(), e.getY())) {
      DShape temp = new DShape(ellipsePalette);
      double tempX = temp.getWidth() / 2;
      double tempY = temp.getHeight() / 2;

      temp.setLocation(e.getX() - tempX, e.getY() - tempY);
      ellipsePalette.isPressed = true;
      temp.isPressed = true;
      obstacles.add(temp);
    } else if (trianglePalette.contains(e.getX(), e.getY())) {
      DShape temp = new DShape(trianglePalette);
      double tempX = temp.getWidth() / 2;
      double tempY = temp.getHeight() / 2;

      temp.setLocation(e.getX(), e.getY());
      trianglePalette.isPressed = true;
      temp.isPressed = true;
      obstacles.add(temp);
    } else {
      DShape temp;
      for (int count = 0; count < obstacles.size(); count++) {
        temp = ((DShape) obstacles.get(count));

        if (temp.contains(e.getX(), e.getY())) {
          temp.isPressed = true;

          if (temp.type == DShape.TRIANGLE) {
            temp.setLocation(e.getX(), e.getY());
          } else {
            temp.setLocation(e.getX() - temp.getWidth() / 2,
                e.getY() - temp.getHeight() / 2);
          }
        }
      }
      resetFeasible();
    }
    repaint();
  }

  // Handles the event of a user dragging the mouse while holding
  // down the mouse button.
  public void mouseDragged(MouseEvent e) {
    if (startObject.isPressed && isInPanel) {
      startObject.setLocation(e.getX() - startObject.getWidth() / 2, e.getY()
          - startObject.getHeight() / 2);
    } else if (endObject.isPressed && isInPanel) {
      endObject.setLocation(e.getX() - endObject.getWidth() / 2, e.getY()
          - endObject.getHeight() / 2);
    } else {
      for (int count = 0; count < obstacles.size(); count++) {
        DShape temp = ((DShape) obstacles.get(count));
        if (temp.isPressed) {
          if (temp.type == DShape.TRIANGLE) {
            temp.setLocation(e.getX(), e.getY());
          } else {
            temp.setLocation(e.getX() - temp.getWidth() / 2,
                e.getY() - temp.getHeight() / 2);
          }
        }
      }
    }
    repaint();
  }

  // Handles the event of a user releasing the mouse button.
  public void mouseReleased(MouseEvent e) {
    startObject.isPressed = false;
    endObject.isPressed = false;
    rectanglePalette.isPressed = false;
    ellipsePalette.isPressed = false;
    trianglePalette.isPressed = false;

    for (int count = 0; count < obstacles.size(); count++) {
      DShape temp = ((DShape) obstacles.get(count));
      if (temp.isPressed) {
        // released outside the appropriate range
        if (e.getX() < PALETTE_WIDTH || !isInPanel) {
          // rectangleObstacles.remove(count);
          obstacles.remove(count);
        }
      }
      temp.isPressed = false;
    }
    // computeFeasible();
    repaint();
  }

  // This method is required by MouseListener.
  public void mouseMoved(MouseEvent e) {

  }

  // These methods are required by MouseMotionListener.
  public void mouseClicked(MouseEvent e) {
    System.out.println(printTravelTime(e.getX(), e.getY()));

  }

  // mouse exits the panel
  public void mouseExited(MouseEvent e) {
    isInPanel = false;
    startObject.isPressed = false;
    endObject.isPressed = false;
  }

  // mouse enters the panel
  public void mouseEntered(MouseEvent e) {
    isInPanel = true;
  }

  public String printTravelTime(double x, double y) {
    int tempX = (int) x;
    int tempY = (int) y;

    tempX = (tempX - PALETTE_WIDTH) / spacing;
    tempY = (tempY) / spacing;

    if (tempX > 0 && tempY > 0)
      return ("Travel time down and right of node: " + nodes[tempX][tempY]
          + " " + interpoTime(x, y));
    else
      return ("Out of bounds: " + INF);
  }

  // ///////////// This is the fmm code ////////////////////////////

  // find node neighbors to a node in the An set.
  public void neighbor(Node current) {
   
    // right
    if (current.x < dimX - 1) {
      moveFeasibleToRn(current.x + 1, current.y);
    }

    // left
    if (current.x > 0) {
      moveFeasibleToRn(current.x - 1, current.y);
    }

    // down
    if (current.y < dimY - 1) {
      moveFeasibleToRn(current.x, current.y + 1);
    }

    // up
    if (current.y > 0) {
      moveFeasibleToRn(current.x, current.y - 1);
    }
  }

  private void moveFeasibleToRn(int x, int y) {
    Node testnode;
    testnode = nodes[x][y];
    if (testnode.isFeasible == true && Fn.contains(testnode)) {
      Rn.add(testnode);
      Fn.remove(testnode);
    }
  }

  /* Initializes the nodes */
  public void init() {
    // initialize nodes
    for (int xloc = 0; xloc < dimX; xloc++) {
      for (int yloc = 0; yloc < dimY; yloc++) {
        nodes[xloc][yloc] = new Node(xloc, yloc);
      }
    }
  }

  /* Reset the nodes before performing the fmm. */
  public void resetNodes() {
    // clear out the sets
    Fn.clear();
    An.clear();
    Rn.clear();

    /* Start all nodes with infinite travel times. Add to Fn */
    for (int x = 0; x < dimX; x++) {
      for (int y = 0; y < dimY; y++) {
        nodes[x][y].travelTime = INF;
        Fn.add(nodes[x][y]);
      }
    }

    // compute the feasibility of the nodes
    computeFeasible();
  }

  public double interpoTime(double x, double y) {
    // find the node position
    int nodeX = (int) (x - PALETTE_WIDTH - padding) / spacing;
    int nodeY = (int) (y - padding) / spacing;

    // find ratio from starting node position
    double tempX = (x - (nodeX * spacing) - PALETTE_WIDTH - padding) / spacing;
    double tempY = (y - nodeY * spacing - padding) / spacing;

    double a;
    double b;
    double c;
    double d;

    if (nodeX > 0 && nodeY > 0)
      d = nodes[nodeX][nodeY].travelTime;
    else
      return INF;

    if (nodeX + 1 < dimX)
      b = nodes[nodeX + 1][nodeY].travelTime - d;
    else
      return INF;

    if (nodeY + 1 < dimY)
      c = nodes[nodeX][nodeY + 1].travelTime - d;
    else
      return INF;
    if (nodeX + 1 < dimX && nodeY + 1 < dimY)
      a = nodes[nodeX + 1][nodeY + 1].travelTime - b - c - d;
    else
      return INF;

    // return interpolated time
    return a * tempX * tempY + b * tempX + c * tempY + d;
  }

  public Point2D.Double nextPoint(int x, int y, double deltaT) {
    // find the node position
    int nodeX = (int) (x - PALETTE_WIDTH - padding) / spacing;
    int nodeY = ((int) y - padding) / spacing;

    // find ratio from starting node position
    double tempX = (x - (nodeX * spacing) - PALETTE_WIDTH - padding) / spacing;
    double tempY = (y - nodeY * spacing - padding) / spacing;

    // calculate coefficients
    double a;
    double b;
    double c;
    double d;

    if (nodeX > 0 && nodeY > 0) {
      d = nodes[nodeX][nodeY].travelTime;
    }
    else {
      d = INF;
    }

    if (nodeX + 1 < dimX) {
      b = nodes[nodeX + 1][nodeY].travelTime - d;
    }
    else {
      b = INF;
    }

    if (nodeY + 1 < dimY) {
      c = nodes[nodeX][nodeY + 1].travelTime - d;
    }
    else {
      c = INF;
    }

    if (nodeX + 1 < dimX && nodeY + 1 < dimY) {
      a = nodes[nodeX + 1][nodeY + 1].travelTime - b - c - d;
    }
    else {
      a = INF;
    }

    tempX = (a * tempX + b);
    tempY = (a * tempY + c);

    return new Point2D.Double(tempX * deltaT
        / Math.sqrt(tempX * tempX + tempY * tempY), tempY * deltaT
        / Math.sqrt(tempX * tempX + tempY * tempY));
  }

  public void findPath(Graphics2D g2d) {
    // keep drawing lines until we make it this far
    double tolerance = spacing;
    double deltaT = spacing / 2;
    Point2D.Double point;
    int iter = 0;
    int maxIter = 5000;

    int nextX = (int) endObject.getX();
    int nextY = (int) endObject.getY();

    int currX = nextX;
    int currY = nextY;

    while (interpoTime((double) nextX, (double) nextY) > tolerance
        && iter < maxIter) {
      currX = nextX;
      currY = nextY;

      point = nextPoint(currX, currY, deltaT);

      nextX = currX - (int) point.getX();
      nextY = currY - (int) point.getY();

      g2d.setColor(Color.MAGENTA);
      g2d.setStroke(new BasicStroke(2.0f));
      g2d.drawLine(nextX, nextY, currX, currY);
      iter++;
    }
  }

  public void computeTravelTime(Node current) {
    Node left = new Node();
    Node right = new Node();
    Node up = new Node();
    Node down = new Node();
    Node minX = new Node();
    Node minY = new Node();

    double a = 0;
    double b = 0;
    double c = 0;

    // find the neighbor with the smallest travel time
    if (current.x > 0) {
      left = nodes[current.x - 1][current.y];
    }
    if (current.x < dimX - 1) {
      right = nodes[current.x + 1][current.y];
    }
    if (current.y > 0) {
      up = nodes[current.x][current.y - 1];
    }
    if (current.y < dimY - 1) {
      down = nodes[current.x][current.y + 1];
    }

    if (An.contains(left) && left.travelTime < right.travelTime) {
      minX = left;
    } else if (An.contains(right)) {
      minX = right;
    }

    if (An.contains(up) && up.travelTime < down.travelTime) {
      minY = up;
    } else if (An.contains(down)) {
      minY = down;
    }

    // there is a neighbor whose travel time we can compute
    if (minX.travelTime < INF) {
      a = (double) 1 / (spacing * spacing);
      b = 2.0 * minX.travelTime / (double) (spacing * spacing);
      c = (minX.travelTime * minX.travelTime) / (double) (spacing * spacing);
    }

    if (minY.travelTime < INF) {
      a = a + (double) 1 / (spacing * spacing);
      b = b + 2.0 * minY.travelTime / (double) (spacing * spacing);
      c = c + (minY.travelTime * minY.travelTime)
          / (double) (spacing * spacing);
    }

    c = c - 1;
    if (a != 0) {
      current.travelTime = (b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
    } else
      current.travelTime = INF;
  }

  /* finds the minimum candidate travel time from the set in Rn */
  public Node findMinTravelTime() {
    double min = INF;
    int index = -1;
    Node quickestRoute = null;
    Node temp = null;

    for (int i = 0; i < Rn.size(); i++) {
      temp = (Node) Rn.get(i);
      computeTravelTime(temp);
      if (temp.travelTime < min) {
        index = i;
        min = temp.travelTime;
      }
    }

    quickestRoute = (Node) Rn.get(index);
    if (quickestRoute != null) {
      An.add(Rn.remove(index));
      neighbor(quickestRoute);
    }

    return quickestRoute;
  }

  /* Performs the fast marching method to calculate travel times of each node */
  public void performFMM() {
    doFmm = true;

    // find the starting point
    int startX = (int) startObject.getX();
    int startY = (int) startObject.getY();

    // convert to indicies
    startX = (startX - PALETTE_WIDTH) / spacing;
    startY = (startY) / spacing;

    // put the first node into Rn and remove it from Fn
    Node begin = nodes[startX][startY];

    // if the beginning node is inside of an object, quit.
    if (!begin.isFeasible) {
      repaint();
      return;
    }

    Fn.remove(begin);
    An.add(begin);
    begin.travelTime = 0; // initial travel time
    neighbor(begin);

    Node temp = findMinTravelTime();

    // continue calculating travel times until these sets are empty
    while (!Fn.isEmpty() || !Rn.isEmpty()) {
      temp = findMinTravelTime();

      if (temp == null) {
        System.out.println("There is a problem");
        break;
      }

    }

    // draw it after completion
    repaint();
  }
}