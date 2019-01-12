package unassignedBranchDeterminer;

//This class simply serves to setup the GUI, and also to get needed information from the user.

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

public class GUIDisplay
{
	private static JTextArea outputArea, branchArea, helpArea;
	private static String selectionRulesToUse, inputBranchType;
	private static GUIDisplay guiDisplay;
	private static ArrayList<Double> inputBranchArr;

	private JFrame frame;
	private JTabbedPane tabbedPane;
	private JFileChooser fileChooser;
	private JLabel energyFileLabel;
	private JLabel peakFileLabel;
	private JButton fileButton, clearFilesButton, runButton, saveButton;
	private JPanel filePanel;
	private GradientPanel inputPanel, outputPanel, helpPanel;
	private JScrollPane outputScroll, branchScroll, helpScroll;
	private JComboBox<Integer> matchBox, skipBox, numberOfBranchesBox;
	private JComboBox<String> branchBox, selectionRuleBox, transitionTypeBox, withinTolBox;
	private JTextField sheetFieldE, sheetFieldA, rowFieldE, rowFieldA, toleranceField;
	private GridBagConstraints gbc = new GridBagConstraints();
	private ArrayList<File> inputFileArray = new ArrayList<File>();
	private String transitionType, sheetNameE, sheetNameA, priorityForMatching;
	private JProgressBar progressBar;
	private JDialog dialog;
	private MultiThreading extraThread;
	
	private final int FONT_SIZE = 18;

	private int startingRowE, startingRowA, requiredMatches, maxSkips, numberOfBranchesFilter;
	private boolean outputIsDisplayed = false;
	private double tolerance;

	//constructor. Creates the JFrame object
	public GUIDisplay() {
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Unassigned Branch Determiner");
		frame.setResizable(false);
		frame.setIconImage(new ImageIcon(getClass().getResource("64x64Methanol.png")).getImage());
		
		initComponents();
		setComponentSettings();
		addComponents();
		
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	//creates the components in the GUI
	private void initComponents()
	{
		inputBranchArr = new ArrayList<Double>();
		progressBar = new JProgressBar(0, 100);
		fileChooser = new JFileChooser();
		
		branchArea = new JTextArea();
		outputArea = new JTextArea();
		helpArea = new JTextArea();
		
		numberOfBranchesBox = new JComboBox<Integer>();
		matchBox = new JComboBox<Integer>();
		skipBox = new JComboBox<Integer>();
		branchBox = new JComboBox<String>();
		transitionTypeBox = new JComboBox<String>();
		selectionRuleBox = new JComboBox<String>();
		withinTolBox = new JComboBox<String>();
		
		fileButton = new JButton("Select files");
		clearFilesButton = new JButton("Clear Selected Files");
		runButton = new JButton("Run Program");
		saveButton = new JButton("Save Output...");
		
		toleranceField = new JTextField("0.001");
		sheetFieldE = new JTextField("E Levels, gd vibrational");
		rowFieldE = new JTextField("5");
		sheetFieldA = new JTextField("A Levels, gd vibrational");
		rowFieldA = new JTextField("5");
		
		peakFileLabel = new JLabel("  Peak File: ");
		energyFileLabel = new JLabel("Energy File: ");
		
		tabbedPane = new JTabbedPane();
		filePanel = new JPanel();
		inputPanel = new GradientPanel(new Color(83,120,149), new Color(9,32,63));
		outputPanel = new GradientPanel(new Color(83,120,149), new Color(9,32,63));
		helpPanel = new GradientPanel(new Color(83,120,149), new Color(9,32,63));
	}

	//sets how the components look and act
	private void setComponentSettings()
	{
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Excel files or CSV", "xlsx", "csv");
		fileChooser.setPreferredSize(new Dimension(1000, 618));
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setFileFilter(filter);
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.home"), "Desktop"));

		inputPanel.setLayout(new GridBagLayout());
		outputPanel.setLayout(new GridBagLayout());
		helpPanel.setLayout(new GridBagLayout());
		
		tabbedPane.setFont(new Font("Arial", Font.PLAIN, 18));
		tabbedPane.setForeground(Color.RED);
		tabbedPane.setUI(new BasicTabbedPaneUI());
		tabbedPane.setFocusable(false);

		filePanel.setBackground(Color.DARK_GRAY);
		filePanel.setPreferredSize(new Dimension(200, 80));
		filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
		TitledBorder b = BorderFactory.createTitledBorder("Selected files");
		b.setTitleFont(new Font("Arial", Font.PLAIN, 14));
		b.setBorder(BorderFactory.createEmptyBorder());
		filePanel.setBorder(b);

		setTextAreaSettings(helpArea);
		helpArea.setEditable(false);
		helpScroll = new JScrollPane(helpArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		setTextAreaDefault();
		helpScroll.setPreferredSize(new Dimension(800, 800));
		helpScroll.getVerticalScrollBar().setUnitIncrement(16);
		
		setTextAreaSettings(outputArea);
		defaultOutputTextArea();
		outputArea.setEditable(false);
		outputScroll = new JScrollPane(outputArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		setTextAreaDefault();
		outputScroll.setPreferredSize(new Dimension(800, 800));
		outputScroll.getVerticalScrollBar().setUnitIncrement(20);
		
		setTextAreaSettings(branchArea);
		branchScroll = new JScrollPane(branchArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		branchScroll.setPreferredSize(new Dimension(220, 800));
		branchScroll.getVerticalScrollBar().setUnitIncrement(8);

		setButtonSettings(fileButton);
		setButtonSettings(clearFilesButton);
		setButtonSettings(saveButton);
		setButtonSettings(runButton);
		runButton.setForeground(new Color(32, 244, 9));

		setBoxSettings(matchBox);
		setBoxSettings(branchBox);
		setBoxSettings(selectionRuleBox);
		setBoxSettings(transitionTypeBox);
		setBoxSettings(skipBox);
		setBoxSettings(withinTolBox);
		setBoxSettings(numberOfBranchesBox);

		for (int i = 1; i <= 50; i++)
			matchBox.addItem(i);
		matchBox.setSelectedItem(20);

		for (int i = 0; i <= 15; i++)
			skipBox.addItem(i);
		skipBox.setSelectedItem(2);

		for (int i = 1; i <= 8; i++)
			numberOfBranchesBox.addItem(i);
		numberOfBranchesBox.setSelectedItem(2);

		branchBox.addItem("");
		branchBox.addItem("Q");
		branchBox.addItem("P");
		branchBox.addItem("R");
		selectionRuleBox.addItem("");
		selectionRuleBox.addItem("Even ΔVt");
		selectionRuleBox.addItem("Odd ΔVt");
		transitionTypeBox.addItem("");
		transitionTypeBox.addItem("a-type");
		transitionTypeBox.addItem("b-type");
		transitionTypeBox.addItem("c-type*");

		withinTolBox.addItem("Higher Intensity");
		withinTolBox.addItem("Closest to Prediction");

		setFieldSettings(sheetFieldE);
		setFieldSettings(rowFieldE);
		setFieldSettings(sheetFieldA);
		setFieldSettings(rowFieldA);
		setFieldSettings(toleranceField);

		energyFileLabel.setPreferredSize(new Dimension(200, 30));
		energyFileLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
		energyFileLabel.setFocusable(false);
		energyFileLabel.setForeground(Color.WHITE);

		peakFileLabel.setPreferredSize(new Dimension(200, 30));
		peakFileLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
		peakFileLabel.setFocusable(false);
		peakFileLabel.setForeground(Color.WHITE);
	}
	
	private void setTextAreaSettings(JTextArea area) {
		area.setBackground(Color.WHITE);
		area.setCaretColor(Color.BLACK);
		area.setEditable(true);
		area.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
		area.setForeground(Color.BLACK);
		area.setBorder(BorderFactory.createBevelBorder(0));
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
	}

	//settings for the JTextField's
	private void setFieldSettings(JTextField field)
	{
		field.addActionListener(new EventHandling());
		field.setPreferredSize(new Dimension(200, 30));
		field.setFont(new Font("Arial", Font.PLAIN, FONT_SIZE));
		field.setFocusable(true);
		field.setBorder(BorderFactory.createRaisedBevelBorder());
	}

	//settings for the JComboBox's
	private void setBoxSettings(JComboBox<?> box)
	{
		box.addActionListener(new EventHandling());
		box.setPreferredSize(new Dimension(200, 30));
		box.setFont(new Font("Arial", Font.PLAIN, FONT_SIZE));
		box.setFocusable(true);
		box.setBorder(BorderFactory.createRaisedBevelBorder());
		((JLabel) box.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
		box.setMaximumRowCount(11);
	}

	//settings for the JButton's
	private void setButtonSettings(JButton button)
	{
		button.addActionListener(new EventHandling());
		button.setPreferredSize(new Dimension(180, 30));
		button.setFont(new Font("Arial", Font.BOLD, FONT_SIZE));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setFocusable(false);
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setBackground(new Color(34, 62, 74));
		button.setForeground(new Color(253, 253, 253));
		button.setContentAreaFilled(false);
		button.setOpaque(true);
	}

	//adds the components to the panels
	private void addComponents()
	{
		JLabel label1 = new JLabel("Select the unassigned branch type", SwingConstants.LEFT);
		JLabel label2 = new JLabel("Number of lines needed to display branch", SwingConstants.LEFT);
		JLabel label3 = new JLabel("Enter the energy sheet name (E type)", SwingConstants.LEFT);
		JLabel label4 = new JLabel("Enter the starting row (E type)", SwingConstants.LEFT);
		JLabel label5 = new JLabel("Enter the energy sheet name (A type)", SwingConstants.LEFT);
		JLabel label6 = new JLabel("Enter the starting row (A type)", SwingConstants.LEFT);
		JLabel label7 = new JLabel("Select the kind of selection rules to be used", SwingConstants.LEFT);
		JLabel label8 = new JLabel("Maximum number of skipped lines in a branch", SwingConstants.LEFT);
		JLabel label9 = new JLabel("Select the transition type", SwingConstants.LEFT);
		JLabel label10 = new JLabel("Tolerance for Peak Matching", SwingConstants.LEFT);
		JLabel label11 = new JLabel("For Peak Matching Prioritize", SwingConstants.LEFT);
		JLabel label12 = new JLabel("Number of Branches Required", SwingConstants.LEFT);

		setLabelSettings(label1);
		setLabelSettings(label2);
		setLabelSettings(label3);
		setLabelSettings(label4);
		setLabelSettings(label5);
		setLabelSettings(label6);
		setLabelSettings(label7);
		setLabelSettings(label8);
		setLabelSettings(label9);
		setLabelSettings(label10);
		setLabelSettings(label11);
		setLabelSettings(label12);

		filePanel.add(energyFileLabel);
		filePanel.add(peakFileLabel);

		JLabel branchHeader = new JLabel("Unassigned Branch Information", JLabel.CENTER);
		JLabel filterHeader = new JLabel("Output Filters", JLabel.CENTER);
		JLabel energyFileHeader = new JLabel("Energy file information", JLabel.CENTER);
		JLabel branchInputHeader = new JLabel("Unassigned Branch", JLabel.CENTER);
		setHeaderSettings(branchHeader);
		setHeaderSettings(filterHeader);
		setHeaderSettings(energyFileHeader);
		setHeaderSettings(branchInputHeader);
		branchInputHeader.setPreferredSize(new Dimension(220, 25));

		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.gridwidth = 1;
		
		setLocationOfComponenet(inputPanel, transitionTypeBox, 2, 7);
		setLocationOfComponenet(inputPanel, sheetFieldA, 2, 2);
		setLocationOfComponenet(inputPanel, rowFieldA, 2, 3);
		setLocationOfComponenet(inputPanel, sheetFieldE, 2, 4);
		setLocationOfComponenet(inputPanel, skipBox, 2, 14);
		setLocationOfComponenet(inputPanel, matchBox, 2, 13);
		setLocationOfComponenet(inputPanel, selectionRuleBox, 2, 8);
		setLocationOfComponenet(inputPanel, toleranceField, 2, 11);
		setLocationOfComponenet(inputPanel, withinTolBox, 2, 12);
		setLocationOfComponenet(inputPanel, numberOfBranchesBox, 2, 15);

		gbc.insets = new Insets(5, 5, 25, 5);
		setLocationOfComponenet(inputPanel, branchBox, 2, 9);
		setLocationOfComponenet(inputPanel, rowFieldE, 2, 5);
		gbc.insets = new Insets(5, 5, 5, 5);

		setLocationOfComponenet(inputPanel, runButton, 0, 17);
		setLocationOfComponenet(inputPanel, clearFilesButton, 2, 17);
		setLocationOfComponenet(inputPanel, fileButton, 1, 17);
	
		gbc.gridwidth = 2;

		setLocationOfComponenet(inputPanel, label8, 0, 14);
		setLocationOfComponenet(inputPanel, label5, 0, 2);
		setLocationOfComponenet(inputPanel, label6, 0, 3);
		setLocationOfComponenet(inputPanel, label3, 0, 4);
		setLocationOfComponenet(inputPanel, label4, 0, 5);
		setLocationOfComponenet(inputPanel, label9, 0, 7);
		setLocationOfComponenet(inputPanel, label1, 0, 9);
		setLocationOfComponenet(inputPanel, label2, 0, 13);
		setLocationOfComponenet(inputPanel, label7, 0, 8);
		setLocationOfComponenet(inputPanel, label10, 0, 11);
		setLocationOfComponenet(inputPanel, label11, 0, 12);
		setLocationOfComponenet(inputPanel, label12, 0, 15);

		gbc.gridwidth = 1;
		gbc.gridheight = 17;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.BOTH;
		
		setLocationOfComponenet(outputPanel, outputScroll, 0, 1);
		gbc.gridheight = 16;
		setLocationOfComponenet(inputPanel, branchScroll, 3, 2);

		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.SOUTHEAST;
		gbc.fill = GridBagConstraints.NONE;

		setLocationOfComponenet(outputPanel, saveButton, 0, 0);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.insets = new Insets(135, 5, 5, 5);
		gbc.gridwidth = 3;
		gbc.gridheight = 2;
		
		setLocationOfComponenet(inputPanel, filePanel, 0, 15);

		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.gridheight = 1;
		setLocationOfComponenet(inputPanel, branchHeader, 0, 6);
		setLocationOfComponenet(inputPanel, filterHeader, 0, 10);
		setLocationOfComponenet(inputPanel, energyFileHeader, 0, 1);
		
		gbc.gridwidth = 1;
		setLocationOfComponenet(inputPanel, branchInputHeader, 3, 1);
		
		gbc.fill = GridBagConstraints.BOTH;
		setLocationOfComponenet(helpPanel, helpScroll, 0, 0);

		//Tabbed pane stuff
		tabbedPane.add("Input", inputPanel);
		tabbedPane.add("Output", outputPanel);
		tabbedPane.add("Help", helpPanel);
		
		for(int i = 0; i < tabbedPane.getTabCount(); i++)
			tabbedPane.setBackgroundAt(i, Color.DARK_GRAY);
		
		JLabel tab1 = new JLabel("Input");
		setTabSettings(tab1, 0);
		
		JLabel tab2 = new JLabel("Output");
		setTabSettings(tab2, 1);
		
		JLabel tab3 = new JLabel("Help");
		setTabSettings(tab3, 2);
		
		frame.add(tabbedPane);
		frame.revalidate();
	}
	
	private void setTabSettings(JLabel label, int position) {
		label.setFont(new Font("Arial", Font.PLAIN, 20));
		label.setBackground(Color.RED);
		label.setPreferredSize(new Dimension(150, 25));
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.setFocusable(false);
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setBackground(Color.DARK_GRAY);
		label.setForeground(Color.WHITE);
		label.setOpaque(true);
		tabbedPane.setTabComponentAt(position, label);
	}

	private void setHeaderSettings(JLabel header)
	{
		header.setFont(new Font("Arial", Font.BOLD, FONT_SIZE));
		header.setForeground(new Color(253, 253, 253));
		header.setPreferredSize(new Dimension(300, 25));
		header.setOpaque(true);
		header.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.BLACK));
		header.setBackground(new Color(34, 62, 74));
	}

	private void setLocationOfComponenet(JPanel panel, JComponent component, int x, int y)
	{
		gbc.gridx = x;
		gbc.gridy = y;
		panel.add(component, gbc);
	}

	private void setLabelSettings(JLabel label)
	{
		label.setForeground(new Color(253, 253, 253));
		label.setFont(new Font("Arial", Font.PLAIN, FONT_SIZE));
		label.setPreferredSize(new Dimension(300, 30));
	}
	
	private ArrayList<Double> readInputBranch() {
		String input = branchArea.getText();
		
		if(input.equals("") || input.trim().length() == 0) {
			JOptionPane.showMessageDialog(null, "Please first enter in the unassigned branch."
					+ "\nThe field is currently empty.");
			return null;
		}
		
		//read in the branch
		ArrayList<Double> unassignedBranchArr = new ArrayList<Double>();
		String [] inputLinesArr = input.split("\n");
		
		for(String s: inputLinesArr) {
			try {
				double line = Double.parseDouble(s);
				unassignedBranchArr.add(line);
			}catch(NumberFormatException e) {
				JOptionPane.showMessageDialog(null, "Formatting issue with the unassigned branch field.");
				return null;
			}

		}
		
		return unassignedBranchArr;
	}

	private final void setTextAreaDefault()
	{
		String sep = "----------------------------------------------------------------\n";
		
		helpArea.setText(
				  "GENERAL INFO:\r\n"
				+ "This program helps to uncover information about an unassigned spectral branch. It can help determine the symmetry type,"
				+ " J numbering, as well as the K values associated with the transition.\r\n\n"
				
				+ sep
				+ "HOW IT WORKS:"
				+ "\nThis program uses brute force to test all possible choses of J ranges, K, and symmetry types. "
				+ "It works by using known lower state energies and combination differences. Since Q, R, and P branches all share the same"
				+ " \"ladder\" of energy levels for a given K value, it is possible to get all three branches given just one. Additionally, b-type transitions"
				+ " are allowed to have delta K = 1 or -1, which allows additional combination difference checks if the corresponding lower energy levels"
				+ " are known. So, b-type transitions can have up to 8 branches which correspond to the input branch (2 for the current K, and"
				+ " 6 more for the delta K = 1 or -1 combination differences). Conversely, a-type transitions will have at most 2 branches through its "
				+ "combination difference checks as delta K must be zero.\n\n"
				
				+sep
				+ "INPUT:\r\n"
				+ "Enter in the sheet names which contains the lower state energies for the unassigned branch."
				+ " The \"starting row "
				+ "number\" is the row where the K values are listed across. For example, in the energy file for CD3SH there are "
				+ "K values listed across for vt=0 at row 5. If vt=1 was desired, then row 52 would be entered as this is where "
				+ "the K values are listed for that particular torsional state. The program reads across until it reaches a K value"
				+ " with a blank cell for its energy origin."
				+ " \n\nCopy and paste the unassigned branch into the textbox located under the \"input\" tab"
				+ " (put the line values only, eg. no intensities!).\n\n" 
				
				+sep
				+ "OUPUT:"
				+ "\nThe output is displayed in the output tab. It can be saved to a text file once the program finishes running."
				+ " If the output is saved, then the program will create a text file on the desktop called \"Combination difference results.txt\""
				+ " and add the results to there. Output is only shown for data which doesn't get cut out due to the selected filters."
				+ " Each unique chose of J range, K, and symmetry is seperated by a dividing line (===); Areas seperated"
				+ " by dividing lines are referred to as \"blocks\".\r\n" 
				+ "\nThe output filters work as following:\n\n"
				+ "\"Tolerance for Peak Matching\": How close a prediction needs to be to a given line in the peak finder in order to"
				+ " assign it. \r\n\n"
				+ "\"For Peak Matching Prioritize\": For when theres multiple lines within the tolerance range.\r\n\n"
				+ "\"Number of lines needed to display branch\": Determines how many lines a given branch needs in order to be displayed.\r\n\n"
				+ "\"Enter the maximum number of skips\": Determines the maximum number of skipped lines a branch can have, if it exceeds this it will not"
				+ " be displayed.\r\n\n"
				+ "\"Number of Branches Required\": Filters out blocks of output if they do not have the specified number of branches"
				+ " (Branches only count towards this number if they pass the other filters first).\n\n"
				
				+sep
				+ "NOTE:\r\n"
				+ "Close the excel files that are passed to this program as input, otherwise the program won't run.");

		helpArea.setCaretPosition(0);
	}
	
	private void defaultOutputTextArea() {
		outputArea.setText("\n\n\n\n\n\n\n\n\n\n\n\n\t\t\tOutput will go here...");
	}

	private class EventHandling implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() == fileButton) {
				switch (inputFileArray.size())
				{
				case 0:
					fileChooser.setDialogTitle("Open a ground state energy file");
					break;
				case 1:
					fileChooser.setDialogTitle("Open a peak list");
					break;
				default:
					JOptionPane.showMessageDialog(null, "Already two files selected!");
					return;
				}

				if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					inputFileArray.add(fileChooser.getSelectedFile());

					switch (inputFileArray.size())
					{
					case 1:
						energyFileLabel.setText(("Energy File: " + inputFileArray.get(0).getName()));
						break;
					case 2:
						peakFileLabel.setText(("  Peak File: " + inputFileArray.get(1).getName()));
					}
				}
			}

			if (e.getSource() == clearFilesButton) {
				reset();
			}

			if (e.getSource() == saveButton) {
				if (outputIsDisplayed) {

					try {
						ExcelIO.createOutputFile();
						ExcelIO.outputToFile(outputArea);
						JOptionPane.showMessageDialog(null, "Save successful, output placed on the Desktop");
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(null, "Save failed: IO Exception");
					}
					
					return;
				} else {
					JOptionPane.showMessageDialog(null, "Save failed: No Output is Currently Displayed");
					return;
				}
			}

			if (e.getSource() == runButton) {
				runProgram();
			}
		}
	}

	private void reset()
	{
		energyFileLabel.setText("Energy File: ");
		peakFileLabel.setText("  Peak File: ");
		inputFileArray.clear();
	}
	
	private void runProgram()
	{
		if (inputFileArray.size() != 2) {
			JOptionPane.showMessageDialog(null, "Please first select the energy file and then the peak list");
			return;
		}

		if (branchBox.getSelectedItem().equals("")) {
			JOptionPane.showMessageDialog(null, "Select the type of branch first!");
			return;
		}

		if (selectionRuleBox.getSelectedItem().equals("")) {
			JOptionPane.showMessageDialog(null, "Select the kind of selection rules to be used first!");
			return;
		}

		if (transitionTypeBox.getSelectedItem().equals("")) {
			JOptionPane.showMessageDialog(null, "Select the transition type first!");
			return;
		}

		if (transitionTypeBox.getSelectedItem().equals("a-type")
				&& selectionRuleBox.getSelectedItem().equals("Odd ΔVt")) {
			int result = JOptionPane.showConfirmDialog(null,
					"You have selected a-type transition and odd selection rules...\nContinue?", null,
					JOptionPane.YES_NO_OPTION);
			if (result != JOptionPane.YES_OPTION) {
				return;
			}
		}
		
		if(transitionTypeBox.getSelectedItem().equals("c-type*")) {
			JOptionPane.showMessageDialog(null, "\"c-type\" is not currently supported");
			return;
		}
		
		if( !SearchPeakFinder.areValidFileTypes(inputFileArray.get(0), inputFileArray.get(1)) ){
			JOptionPane.showMessageDialog(null, "Invalid file type(s):\nEnergy file must be .xlsx\n"
					+ "Peak file must be either .xlsx or .csv");
			reset();
			return;
		}
		
		if((inputBranchArr = readInputBranch()) == null) {
			return;
		}

		try {
			startingRowE = Integer.parseInt(rowFieldE.getText());
			startingRowA = Integer.parseInt(rowFieldA.getText());
			tolerance = Double.parseDouble(toleranceField.getText());
			
			numberOfBranchesFilter = (int) numberOfBranchesBox.getSelectedItem();
			requiredMatches = (int) matchBox.getSelectedItem();
			maxSkips = (int) skipBox.getSelectedItem();
			
			sheetNameE = sheetFieldE.getText();
			sheetNameA = sheetFieldA.getText();
			
			transitionType = (String) transitionTypeBox.getSelectedItem();
			priorityForMatching = (String) withinTolBox.getSelectedItem();
			selectionRulesToUse = (String) selectionRuleBox.getSelectedItem();
			inputBranchType = (String) branchBox.getSelectedItem();
		} catch (NumberFormatException nfe) {
			JOptionPane.showMessageDialog(null, "Formatting issue with one or more of the entered fields...");
			rowFieldA.setText("");
			rowFieldE.setText("");
			toleranceField.setText("");
			return;
		}
		
		tabbedPane.setSelectedIndex(1);
		clearTextArea();
		outputArea.update(outputArea.getGraphics());
		outputArea.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
		outputArea.setForeground(Color.BLACK);
		outputArea.setCaretPosition(0);

		outputToGUIDisplay("(Applied settings)" +
				"\n\nTransition type:\t\t\t\t" + transitionType + 
				"\nSelection rules:\t\t\t\t" + selectionRulesToUse +
				"\nUnassigned branch type:\t\t\t\t" + inputBranchType +
				"\nRequired number of lines:\t\t\t" + requiredMatches +
				"\nMaximum number of skips in J:\t\t\t" + maxSkips +
				"\nMinimum number of branches:\t\t\t" + numberOfBranchesFilter +
				System.lineSeparator());

		frame.revalidate();
		progressBarSetup();

		MultiThreading multiThreadingObj = new MultiThreading(guiDisplay);
		extraThread = multiThreadingObj;
		extraThread.execute();

		outputIsDisplayed = true;
	}

	//getter and setter methods
	public double getTolerance()
	{
		return tolerance;
	}

	public int getNumberOfBranchesFilter()
	{
		return numberOfBranchesFilter;
	}

	public String getPriorityForMatching()
	{
		return priorityForMatching;
	}

	public static String getSelectionRulesToUse()
	{
		return selectionRulesToUse;
	}

	public static String getInputBranchType()
	{
		return inputBranchType;
	}

	public static GUIDisplay getGuiDisplay()
	{
		return guiDisplay;
	}

	public int getStartingRowE()
	{
		return startingRowE;
	}

	public int getStartingRowA()
	{
		return startingRowA;
	}

	public int getRequiredMatches()
	{
		return requiredMatches;
	}
	
	public ArrayList<Double> getInputBranchArray(){
		return inputBranchArr;
	}

	public int getMaxSkips()
	{
		return maxSkips;
	}

	public ArrayList<File> getInputFileArray()
	{
		return inputFileArray;
	}

	public String getTransitionType()
	{
		return transitionType;
	}

	public String getSheetNameE()
	{
		return sheetNameE;
	}

	public String getSheetNameA()
	{
		return sheetNameA;
	}

	public MultiThreading getThreadObject()
	{
		return extraThread;
	}

	public static void outputToGUIDisplay(String output)
	{
		outputArea.append(output + "\n");
	}

	public static void clearTextArea()
	{
		outputArea.setText("");
	}

	private void progressBarSetup()
	{
		progressBar.setString("");
		progressBar.setStringPainted(true);
		progressBar.setFont(new Font("Arial", Font.BOLD, 20));
		progressBar.setValue(0);
		progressBar.setPreferredSize(new Dimension(500, 40));
		progressBar.setIndeterminate(true);

		JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		progressPanel.setPreferredSize(new Dimension(520, 60));
		progressPanel.add(progressBar);

		dialog = new JDialog((JFrame) null, "Program Loading");
		dialog.add(progressPanel);
		dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	public void updateProgressBar(String valueOrMessage)
	{
		List<String> list = new ArrayList<String>();
		list.add(valueOrMessage);
		process(list);
	}

	public void disposeProgressBar()
	{
		dialog.dispose();
	}

	public void setToIndeterminate(boolean change)
	{
		progressBar.setIndeterminate(change);
	}

	public void process(List<String> chunks)
	{
		for (String chunk : chunks) {
			if (chunk.substring(0, 3).equals("msg")) {
				progressBar.setString(chunk.substring(4));
				continue;
			} else if (chunk.substring(0, 3).equals("val")) {
				progressBar.setValue(Integer.parseInt(chunk.substring(4)));
				continue;
			}

			outputToGUIDisplay(chunk);
		}
	}

	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					UIManager.put("ProgressBar.cycleTime", new Integer(2000));
				} catch (Exception ignored) {
				}

				guiDisplay = new GUIDisplay();
			}
		});
	}
}