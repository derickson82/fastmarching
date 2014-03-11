/*
 * 	File name:	OptimalPath.java
 *	Author:		Daniel Erickson
 *	Created:	March 15, 2005
 *	Updated:	Aprel 7, 2005
 *	Version:	0.5
 *
 */

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

class OptimalPath implements ActionListener {
  DomainPanel domainSpacePanel; // contains the domain space
  JPanel shapePalettePanel; // contains the shapes that can be placed as
                            // obstacles
  JPanel buttonComboPanel; // contains buttons and combo boxes
  JPanel logPanel; // contains logged information about application status
  JPanel mainPanel; // contains all other panels

  JComboBox triangleSizes = null; // select the size of the triangle object to
                                  // be added
  JComboBox rectangleSizes = null; // select the size of the rectangle object to
                                   // be added
  JComboBox circleSizes = null; // select the size of the circle object to be
                                // added !!!!Maybe do ovals with major and minor
                                // axes?
  JComboBox resolutionChoices = null; // combo box to select resolution
  JButton fmmButton = null; // button to find optimal path using FMM
  JTextArea log = null; // text field where a log is made of application status
  JScrollPane logScrollPane = null;

  // constructor
  public OptimalPath() {
    domainSpacePanel = new DomainPanel();
    shapePalettePanel = new JPanel();
    buttonComboPanel = new JPanel();
    logPanel = new JPanel();
    mainPanel = new JPanel();

    addWidgets();

    mainPanel.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();

    c.gridx = 0;
    c.gridy = 0;
    c.gridheight = 3;
    c.fill = GridBagConstraints.BOTH;
    mainPanel.add(shapePalettePanel, c);

    c.gridheight = 1;
    c.gridx = 1;
    c.gridy = 0;

    mainPanel.add(domainSpacePanel, c);

    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 2;
    mainPanel.add(buttonComboPanel, c);

    c.gridx = 1;
    c.gridy = 2;
    c.gridwidth = 1;
    mainPanel.add(logPanel, c);
  }

  private void addWidgets() {
    // Domain space
    domainSpacePanel.setPreferredSize(new Dimension(662, 512));

    // shape palette
    shapePalettePanel.setBorder(BorderFactory
        .createTitledBorder("Shape Palette"));
    shapePalettePanel.setPreferredSize(new Dimension(200, 400));
    shapePalettePanel.setLayout(new GridLayout(5, 2)); // three rows, two
                                                       // columns

    // these are the sizes and resolutions
    ObjectSize[] sizes = new ObjectSize[4];
    Resolution[] resolutions = new Resolution[4];
    for (int i = 0; i < 4; i++) {
      sizes[i] = new ObjectSize((i + 1) * 25 + 15, (i + 1) * 25 + 15);
      resolutions[i] = new Resolution(32 * (int) Math.pow(2, i),
          32 * (int) Math.pow(2, i), 16 / (int) Math.pow(2, i),
          4 / (int) Math.pow(2, i) + 2);
    }

    // triangles
    triangleSizes = new JComboBox(sizes);
    triangleSizes.setActionCommand("triangleSizeChanged");
    triangleSizes.addActionListener(this);

    // rectangles
    rectangleSizes = new JComboBox(sizes);
    rectangleSizes.setActionCommand("rectangleSizeChanged");
    rectangleSizes.addActionListener(this);

    // circles
    circleSizes = new JComboBox(sizes);
    circleSizes.setActionCommand("circleSizeChanged");
    circleSizes.addActionListener(this);

    // add labels and boxes to the shape palette panel
    shapePalettePanel.add(new JLabel("Rectangles"));
    shapePalettePanel.add(rectangleSizes);
    shapePalettePanel.add(new JLabel("Circles"));
    shapePalettePanel.add(circleSizes);
    shapePalettePanel.add(new JLabel("Triangles"));
    shapePalettePanel.add(triangleSizes);

    // buttons and combo boxes
    buttonComboPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    fmmButton = new JButton("Fast Marching Method"); // fast marching method
    fmmButton.addActionListener(this);
    buttonComboPanel.add(fmmButton);
    // shapePalettePanel.add(fmmButton);

    resolutionChoices = new JComboBox(resolutions);
    resolutionChoices.setActionCommand("resolutionChanged"); // name command for
                                                             // this combo box
    resolutionChoices.addActionListener(this);

    buttonComboPanel.add(resolutionChoices);
    // shapePalettePanel.add(resolutionChoices);

    // log panel
    logPanel.setBorder(BorderFactory.createTitledBorder("Actions Log"));
    logPanel.setPreferredSize(new Dimension(400, 100));
    log = new JTextArea();
    logScrollPane = new JScrollPane(log);
    logScrollPane.setPreferredSize(new Dimension(620, 65));

    logPanel.add(logScrollPane);
  }

  /**
   * Create the GUI and show it. For thread safety, this method should be
   * invoked from the event-dispatching thread.
   */
  private static void createAndShowGUI() {
    OptimalPath path = new OptimalPath();

    // Create and set up the window.
    JFrame.setDefaultLookAndFeelDecorated(true);
    JFrame frame = new JFrame("Optimal Path Planning");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setResizable(false);

    frame.setContentPane(path.mainPanel);

    // Display the window.
    frame.pack();
    frame.setVisible(true);
  }

  // Handle events here
  public void actionPerformed(ActionEvent e) {
    if ("resolutionChanged".equals(e.getActionCommand())) {
      log.append("\nChanging resolution...");
      Resolution newRes = (Resolution) resolutionChoices.getSelectedItem();
      domainSpacePanel.setResolution(newRes);
      log.append("\nResolution Changed.  New "
          + resolutionChoices.getSelectedItem());
    }

    else if ("Fast Marching Method".equals(e.getActionCommand())) {
      log.append("\nStarting Fast Marching Method. Please wait...");
      log.repaint();
      domainSpacePanel.resetNodes();
      domainSpacePanel.performFMM();
      log.append("\nFinished Fast Marching Method.");
    }

    else if ("rectangleSizeChanged".equals(e.getActionCommand())) {
      log.append("\nNew Rectangle Size: " + rectangleSizes.getSelectedItem());
      ObjectSize newSize = (ObjectSize) rectangleSizes.getSelectedItem();
      domainSpacePanel.setRectangleSize(newSize);
    }

    else if ("circleSizeChanged".equals(e.getActionCommand())) {
      log.append("\nNew Circle Size: " + circleSizes.getSelectedItem());
      ObjectSize newSize = (ObjectSize) circleSizes.getSelectedItem();
      domainSpacePanel.setCircleSize(newSize);
    }

    else if ("triangleSizeChanged".equals(e.getActionCommand())) {
      log.append("\nNew Triangle Size: " + triangleSizes.getSelectedItem());
      ObjectSize newSize = (ObjectSize) triangleSizes.getSelectedItem();
      domainSpacePanel.setTriangleSize(newSize);
    }
  }

  public static void main(String[] args) {
    // Schedule a job for the event-dispatching thread:
    // creating and showing this application's GUI.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
  }
}