package unassignedBranchDeterminer;

import java.text.DecimalFormat;

//Runs through all K, J, symmetry combinations and calculates two other branches by combination differences,
//outputs the results to an output file if the results pass two different "filters".

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ProduceAssignments
{
	private final boolean IS_EVEN_SELECTION_RULES;
	private final int MAX_SKIPS, REQUIRED_MATCHES;
	private final String INPUT_BRANCH_TYPE, TRANSITION_TYPE;
	private final LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> ENERGIES_E, ENERGIES_A;

	private static int currentK, lowestJValue, highestJValue;
	private static boolean isEType, headerShown;
	private static String mainHeader, energyVals;
	// First string is the header; second is the formatted branch.
	private static LinkedHashMap<String, String> toBeOutputBranches = new LinkedHashMap<String, String>();

	private MultiThreading multiThreadingObj;
	private double numberOfKIterations, iterationsGoneThrough = 0;
	private boolean inputIsFlipped = false;

	private GUIDisplay guiObject;
	private ArrayList<Double> inputBranchArray;
	private Map<Integer, Double> associatedR, associatedP, associatedQ, triangularQ, triangularR, triangularP,
			inputBranchWithJ, energiesForCurrentK, energiesForCurrentKOppParity, upperEnergyValuesWithJ;

	public ProduceAssignments(GUIDisplay guiObject, LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> energiesE,
			LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> energiesA, boolean isEvenDeltaVt) {
		ENERGIES_E = energiesE;
		ENERGIES_A = energiesA;
		IS_EVEN_SELECTION_RULES = isEvenDeltaVt;
		inputBranchArray = guiObject.getInputBranchArray();
		this.guiObject = guiObject;

		TRANSITION_TYPE = guiObject.getTransitionType();
		MAX_SKIPS = guiObject.getMaxSkips();
		REQUIRED_MATCHES = guiObject.getRequiredMatches();
		multiThreadingObj = guiObject.getThreadObject();
		INPUT_BRANCH_TYPE = GUIDisplay.getInputBranchType();
		numberOfKIterations = determineNumberOfIterations();
		
		initMaps();
		
		isEType = true;
		testDifferentKValues();

		isEType = false;
		testDifferentKValues();

		inputBranchArray = reverseInputBranch(inputBranchArray);

		isEType = true;
		testDifferentKValues();

		isEType = false;
		testDifferentKValues();
	}

	// runs through all the k values in the energy hash maps
	private void testDifferentKValues()
	{
		LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> energies;

		if (isEType) {
			energies = ENERGIES_E;
		} else {
			energies = ENERGIES_A;
		}

		for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> entry : energies.entrySet()) {
			
			iterationsGoneThrough++;
			int percent = (int) ((iterationsGoneThrough / numberOfKIterations) * 80 + 20);
			multiThreadingObj.myPublish("msg Generating assignments: " + percent + "%");
			multiThreadingObj.myPublish("val " + percent);
			
			currentK = entry.getKey();
			energiesForCurrentK = entry.getValue();
			
			testDifferentJRangesForConstantK();
		}
	}

	// For a given K value it runs through all possible ranges of J
	private void testDifferentJRangesForConstantK()
	{
		int minJ = energiesForCurrentK.entrySet().iterator().next().getKey();
		int maxJ = 0;

		for (Map.Entry<Integer, Double> entry : energiesForCurrentK.entrySet()) {
			maxJ = entry.getKey();
		}

		int shiftsOfJPossible = (maxJ - minJ) + inputBranchArray.size() - 1;

		for (int i = 0; i < shiftsOfJPossible; i++) {
			resetArraysAndHeader();
			setHeaderDisplayState(false);
			lowestJValue = maxJ - i;
			highestJValue = lowestJValue + inputBranchArray.size() - 1;
			assignJValuesToInputBranch();
			publishResults();
		}
	}

	private void publishResults()
	{
		//filter for the number of branches in a block
		if (toBeOutputBranches.size() >= guiObject.getNumberOfBranchesFilter()) {
			
			//prints the default header each block gets
			multiThreadingObj.myPublish(mainHeader);
			
			//prints out all of the branches (of a block) which passed the filters
			for (Entry<String, String> branch : toBeOutputBranches.entrySet()) {
				String branchHeader = branch.getKey();
				String formattedBranch = branch.getValue();
				multiThreadingObj.myPublish(branchHeader + formattedBranch);
			}
			
			//prints the upper state energies at the end of the block
			multiThreadingObj.myPublish(energyVals);
		}
	}

	// assigns a given range of J to the input branch
	private void assignJValuesToInputBranch()
	{
		for (int j = lowestJValue; j <= highestJValue; j++) {
			inputBranchWithJ.put(j, inputBranchArray.get(j - lowestJValue));
		}

		if (isEType) {
			setupAssociatedBranchesEType(inputBranchWithJ);
		} else {
			setupAssociatedBranchesAType(inputBranchWithJ);
		}

	}

	private void setupAssociatedBranchesEType(Map<Integer, Double> inputBranchWithJ)
	{
		ArrayList<Map<Integer, Double>> branches = new ArrayList<Map<Integer, Double>>();
		int jOffset = 0;

		if (INPUT_BRANCH_TYPE.equals("Q")) {
			calculateUpperEnergiesLevelsWithJ(inputBranchWithJ, jOffset);
			
			calculateAssociatedETypeBranches(associatedP, 1);
			calculateAssociatedETypeBranches(associatedR, -1);
			branches.add(associatedP);
			branches.add(associatedR);
			findLineMatchesFromPredictions(branches, "P", "R");
		}

		if (INPUT_BRANCH_TYPE.equals("P")) {
			jOffset = -1;
			calculateUpperEnergiesLevelsWithJ(inputBranchWithJ, jOffset);
			
			calculateAssociatedETypeBranches(associatedQ, -1);
			calculateAssociatedETypeBranches(associatedR, -2);
			branches.add(associatedQ);
			branches.add(associatedR);
			findLineMatchesFromPredictions(branches, "Q", "R");
		}

		if (INPUT_BRANCH_TYPE.equals("R")) {
			jOffset = 1;
			calculateUpperEnergiesLevelsWithJ(inputBranchWithJ, jOffset);
			
			calculateAssociatedETypeBranches(associatedQ, 1);
			calculateAssociatedETypeBranches(associatedP, 2);
			branches.add(associatedQ);
			branches.add(associatedP);
			findLineMatchesFromPredictions(branches, "Q", "P");
		}

		if (TRANSITION_TYPE.equals("b-type")) {
			setupTriangularTypes(2);
			setupTriangularTypes(-2);
		}
	}

	/*
	 * Checks the selection rules and the parity of the lower state. Based on that,
	 * it does the appropriate addition and subtraction to get the predicted branch
	 * values
	 */
	private void setupAssociatedBranchesAType(Map<Integer, Double> inputBranchWithJ)
	{
		getOppositeParityEnergies(currentK);
		ArrayList<Map<Integer, Double>> branches = new ArrayList<Map<Integer, Double>>();
		int jOffset = 0;

		if (IS_EVEN_SELECTION_RULES) {
			if (INPUT_BRANCH_TYPE.equals("Q")) {
				calculateUpperEnergiesLevelsWithJ(inputBranchWithJ, jOffset);
				
				if (currentK >= 0) {
					calculateAssociatedATypeBranches(associatedR, -1, 1, 0, -1, true, false);
					calculateAssociatedATypeBranches(associatedP, 1, -1, 1, 0, false, true);
					branches.add(associatedP);
					branches.add(associatedR);
					findLineMatchesFromPredictions(branches, "P-<-", "R-<-");
				} else {
					calculateAssociatedATypeBranches(associatedR, -1, 1, 0, -1, false, true);
					calculateAssociatedATypeBranches(associatedP, 1, -1, 1, 0, true, false);
					branches.add(associatedP);
					branches.add(associatedR);
					findLineMatchesFromPredictions(branches, "P+<+", "R+<+");
				}
			}

			if (INPUT_BRANCH_TYPE.equals("R")) {
				jOffset = 1;
				calculateUpperEnergiesLevelsWithJ(inputBranchWithJ, jOffset);

				if (currentK >= 0) {
					calculateAssociatedATypeBranches(associatedQ, 1, -1, 1, 0, false, true);
					calculateAssociatedATypeBranches(associatedP, 2, -1, 2, 0, true, true);
					branches.add(associatedQ);
					branches.add(associatedP);
					findLineMatchesFromPredictions(branches, "Q+<-", "P+<+");
				} else {
					calculateAssociatedATypeBranches(associatedQ, 1, -1, 1, 0, true, false);
					calculateAssociatedATypeBranches(associatedP, 2, -1, 2, 0, false, false);
					branches.add(associatedQ);
					branches.add(associatedP);
					findLineMatchesFromPredictions(branches, "Q-<+", "P-<-");
				}
			}

			if (INPUT_BRANCH_TYPE.equals("P")) {
				jOffset = -1;
				calculateUpperEnergiesLevelsWithJ(inputBranchWithJ, jOffset);

				if (currentK >= 0) {
					calculateAssociatedATypeBranches(associatedQ, -1, 1, 0, -1, true, false);
					calculateAssociatedATypeBranches(associatedR, -2, 1, 0, -2, true, true);
					branches.add(associatedQ);
					branches.add(associatedR);
					findLineMatchesFromPredictions(branches, "Q+<-", "R+<+");
				} else {
					calculateAssociatedATypeBranches(associatedQ, -1, 1, 0, -1, false, true);
					calculateAssociatedATypeBranches(associatedR, -2, 1, 0, -2, false, false);
					branches.add(associatedQ);
					branches.add(associatedR);
					findLineMatchesFromPredictions(branches, "Q-<+", "R-<-");
				}
			}
		}

		else {
			if (INPUT_BRANCH_TYPE.equals("Q")) {
				calculateUpperEnergiesLevelsWithJ(inputBranchWithJ, jOffset);
				
				if (currentK >= 0) {
					calculateAssociatedATypeBranches(associatedR, -1, 1, 0, -1, true, false);
					calculateAssociatedATypeBranches(associatedP, 1, -1, 1, 0, false, true);
					branches.add(associatedP);
					branches.add(associatedR);
					findLineMatchesFromPredictions(branches, "P+<-", "R+<-");
				} else {
					calculateAssociatedATypeBranches(associatedR, -1, 1, 0, -1, false, true);
					calculateAssociatedATypeBranches(associatedP, 1, -1, 1, 0, true, false);
					branches.add(associatedP);
					branches.add(associatedR);
					findLineMatchesFromPredictions(branches, "P-<+", "R-<+");
				}
			}

			if (INPUT_BRANCH_TYPE.equals("R")) {
				jOffset = 1;
				calculateUpperEnergiesLevelsWithJ(inputBranchWithJ, jOffset);

				if (currentK >= 0) {
					calculateAssociatedATypeBranches(associatedQ, 1, -1, 1, 0, false, true);
					calculateAssociatedATypeBranches(associatedP, 2, -1, 2, 0, true, true);
					branches.add(associatedQ);
					branches.add(associatedP);
					findLineMatchesFromPredictions(branches, "Q-<-", "P-<+");
				} else {
					calculateAssociatedATypeBranches(associatedQ, 1, -1, 1, 0, true, false);
					calculateAssociatedATypeBranches(associatedP, 2, -1, 2, 0, false, false);
					branches.add(associatedQ);
					branches.add(associatedP);
					findLineMatchesFromPredictions(branches, "Q+<+", "P+<-");
				}
			}

			if (INPUT_BRANCH_TYPE.equals("P")) {
				jOffset = -1;
				calculateUpperEnergiesLevelsWithJ(inputBranchWithJ, jOffset);
				
				if (currentK >= 0) {
					calculateAssociatedATypeBranches(associatedQ, -1, 1, 0, -1, true, false);
					calculateAssociatedATypeBranches(associatedR, -2, 1, 0, -2, true, true);
					branches.add(associatedQ);
					branches.add(associatedR);
					findLineMatchesFromPredictions(branches, "Q-<-", "R-<+");
				} else {
					calculateAssociatedATypeBranches(associatedQ, -1, 1, 0, -1, false, true);
					calculateAssociatedATypeBranches(associatedR, -2, 1, 0, -2, false, false);
					branches.add(associatedQ);
					branches.add(associatedR);
					findLineMatchesFromPredictions(branches, "Q+<+", "R+<-");
				}
			}
		}

		if (TRANSITION_TYPE.equals("b-type")) {
			//The 2 and -2 represent "get lower energies with K +2 / -2 offset from the current K value, if it exists"
			setupTriangularTypes(2);
			setupTriangularTypes(-2);
		}
	}

	// Calculates the predicted branch values for E type symmetry
	private void calculateAssociatedETypeBranches(Map<Integer, Double> associatedBranch, int jOffset)
	{
		for (Map.Entry<Integer, Double> entry : inputBranchWithJ.entrySet()) {
			int jForBranch = entry.getKey() + jOffset;
			double energyForBranch;

			try {
				energyForBranch = entry.getValue() + (energyAt(entry.getKey(), energiesForCurrentK)
						- energyAt(entry.getKey() + jOffset, energiesForCurrentK));
				associatedBranch.put(jForBranch, energyForBranch);
			} catch (NoSuchEnergyException e) {
				continue;
			}
		}
	}

	/*
	 * Calculates the predicted branch values and adds them to the appropriate hash
	 * map (which is then sent off to be checked in the peak finder and possibly
	 * output to the output file). All the parameters in the method are just to set
	 * up the terms correctly.
	 */
	private void calculateAssociatedATypeBranches(Map<Integer, Double> associatedBranchforInput, int jOffsetForOutput,
			int plusMinus, int jOffset1stTerm, int jOffset2ndTerm, boolean parity1stTerm, boolean parity2ndTerm)
	{
		for (Map.Entry<Integer, Double> entry : inputBranchWithJ.entrySet()) {
			int jForBranch = entry.getKey() + jOffsetForOutput;
			double energyForBranch;

			try {
				double term1, term2, term3;

				term1 = entry.getValue();
				term2 = (plusMinus) * (energyAt(entry.getKey() + jOffset1stTerm, parity1stTerm));
				term3 = (-1 * plusMinus) * (energyAt(entry.getKey() + jOffset2ndTerm, parity2ndTerm));

				energyForBranch = term1 + term2 + term3;
				associatedBranchforInput.put(jForBranch, energyForBranch);
			} catch (NoSuchEnergyException e) {
				continue;
			}
		}
	}

	private void calculateUpperEnergiesLevelsWithJ(Map<Integer, Double> inputBranchWithJ, int jOffset)
	{
		for (Map.Entry<Integer, Double> branchWithJ : inputBranchWithJ.entrySet()) {
			for (Map.Entry<Integer, Double> knownEnergyLevels : energiesForCurrentK.entrySet()) {
				if (branchWithJ.getKey() == knownEnergyLevels.getKey()) {
					double upperEnergyLevel = branchWithJ.getValue() + knownEnergyLevels.getValue();
					upperEnergyValuesWithJ.put(knownEnergyLevels.getKey() + jOffset, upperEnergyLevel);
					break;
				}
			}
		}
		
		createUpperStateEnergiesString();
	}

	private void setupTriangularTypes(int kOffset)
	{
		ArrayList<Map<Integer, Double>> branches = new ArrayList<Map<Integer, Double>>();

		int kToCheck = currentK + kOffset;
		LinkedHashMap<Integer, Double> lowerKnownEnergies;
		LinkedHashMap<Integer, Double> lowerKnownEnergiesOppParity = null;

		if (isEType) {
			lowerKnownEnergies = getEnergiesAtK(kToCheck, ENERGIES_E);
		} else {
			lowerKnownEnergies = getEnergiesAtK(kToCheck, ENERGIES_A);
			lowerKnownEnergiesOppParity = getEnergiesAtK(-1 * kToCheck, ENERGIES_A);
		}

		if (lowerKnownEnergies != null) {
			calculateTriangularType(lowerKnownEnergies, triangularR, 1);
			calculateTriangularType(lowerKnownEnergies, triangularP, -1);

			// if its A type get the opposite parities as this is what Q needs
			if (!isEType && lowerKnownEnergiesOppParity != null) {
				calculateTriangularType(lowerKnownEnergiesOppParity, triangularQ, 0);
			} else if (isEType) {
				calculateTriangularType(lowerKnownEnergies, triangularQ, 0);
			}

			branches.add(triangularR);
			branches.add(triangularP);
			branches.add(triangularQ);
			giveTriangularBranchesHeaders(branches, kOffset);
		}

		triangularR.clear();
		triangularQ.clear();
		triangularP.clear();
	}

	// calculates the predicted values for the delta k != 0 transitions
	private void calculateTriangularType(Map<Integer, Double> lowerKnownEnergiesTriangular,
			Map<Integer, Double> triangularBranch, int jOffset)
	{
		for (Map.Entry<Integer, Double> upperEnergy : upperEnergyValuesWithJ.entrySet()) {
			for (Map.Entry<Integer, Double> lowerKnownEnergyLevels : lowerKnownEnergiesTriangular.entrySet()) {
				double energyForBranch;

				if (lowerKnownEnergyLevels.getKey() == upperEnergy.getKey()) {
					int jForBranch = lowerKnownEnergyLevels.getKey();

					try {
						energyForBranch = energyAt(jForBranch + jOffset, upperEnergyValuesWithJ)
								- energyAt(jForBranch, lowerKnownEnergiesTriangular);
						triangularBranch.put(jForBranch, energyForBranch);
					} catch (NoSuchEnergyException e) {
						break;
					}
				}
			}
		}
	}

	// used for the triangular branches
	private void giveTriangularBranchesHeaders(ArrayList<Map<Integer, Double>> branches, int kOffset)
	{
		int upperKValue;
		int lowerKValue = Math.abs(currentK + kOffset);

		if (kOffset > 0) {
			upperKValue = Math.abs(currentK + 1);
		} else {
			upperKValue = Math.abs(currentK - 1);
		}

		if (isEType) {
			findLineMatchesFromPredictions(branches,
					"R (Upper state K=" + upperKValue + ", Lower state K=" + lowerKValue + ")",
					"P (Upper state K=" + upperKValue + ", Lower state K=" + lowerKValue + ")",
					"Q (Upper state K=" + upperKValue + ", Lower state K=" + lowerKValue + ")");
		} else {
			if (currentK >= 0) {
				if (IS_EVEN_SELECTION_RULES) {
					findLineMatchesFromPredictions(branches,
							"R (Upper state K=" + upperKValue + "+, Lower state K=" + lowerKValue
									+ "+)",
							"P (Upper state K=" + upperKValue + "+, Lower state K=" + lowerKValue
									+ "+)",
							"Q (Upper state K=" + upperKValue + "-, Lower state K=" + lowerKValue
									+ "+)");
				} else {
					findLineMatchesFromPredictions(branches,
							"R (Upper state K=" + upperKValue + "-, Lower state K=" + lowerKValue
									+ "+" + ")",
							"P (Upper state K=" + upperKValue + "-, Lower state K=" + lowerKValue
									+ "+" + ")",
							"Q (Upper state K=" + upperKValue + "-, Lower state K=" + lowerKValue
									+ "-)");
				}
			} else {
				if (IS_EVEN_SELECTION_RULES) {
					findLineMatchesFromPredictions(branches,
							"R (Upper state K=" + upperKValue + "-, Lower state K=" + lowerKValue
									+ "-)",
							"P (Upper state K=" + upperKValue + "-, Lower state K=" + lowerKValue
									+ "-)",
							"Q (Upper state K=" + upperKValue + "+, Lower state K=" + lowerKValue
									+ "-)");
				} else {
					findLineMatchesFromPredictions(branches,
							"R (Upper state K=" + upperKValue + "+, Lower state K=" + lowerKValue
									+ "-)",
							"P (Upper state K=" + upperKValue + "+, Lower state K=" + lowerKValue
									+ "-)",
							"Q (Upper state K=" + upperKValue + "+, Lower state K=" + lowerKValue
									+ "+)");
				}
			}
		}
	}

	// Get observed lines from predictions and format
	// them--------------------------------------------------------------------------------------------------------------------------------

	/*
	 * Gets passed a variable number of predicted branches, along with the type of
	 * the branch Looks through the predicted line values one by one to see if they
	 * match anything in the peak finder, if it does, it gets the observed line and
	 * its intensity and adds it to a hash map (which will be output to the output
	 * file if the filters don't stop that from happening)
	 */
	public void findLineMatchesFromPredictions(ArrayList<Map<Integer, Double>> branches, String... types)
	{
		List<List<Object>> observedLines;
		ArrayList<Integer> jValues;

		for (Map<Integer, Double> branch : branches) {
			observedLines = new ArrayList<List<Object>>();
			jValues = new ArrayList<Integer>();

			for (Map.Entry<Integer, Double> linePrediction : branch.entrySet()) {
				try {
					List<Object> observedLine = SearchPeakFinder.binarySearchForLine(linePrediction.getValue());

					observedLines.add(observedLine);
					jValues.add(linePrediction.getKey());

				} catch (CantFindLineException e) {
					continue;
				}
			}

			// checks the filter settings
			if (observedLines.size() >= REQUIRED_MATCHES && notTooManySkips(jValues)) {
				if (!getHeaderDisplayState()) {
					createDisplayHeader();
				}

				int positionOfCurrentBranch = branches.indexOf(branch);
				formatObservedBranch(jValues, observedLines, types[positionOfCurrentBranch]);
			}
		}
	}
	
	//Displays the upper state energies to the screen
	private void createUpperStateEnergiesString() {
		DecimalFormat df1 = new DecimalFormat("0.00000000");
		int firstJVal = upperEnergyValuesWithJ.entrySet().iterator().next().getKey();
		energyVals = System.lineSeparator() + "Upper State Energies" + " (First value has J=" +
		firstJVal + ")" + System.lineSeparator();
		
		for (Map.Entry<Integer, Double> linePrediction : upperEnergyValuesWithJ.entrySet()) {
			//rightPadding(String.valueOf(linePrediction.getKey()), 6)
			energyVals += df1.format(linePrediction.getValue()) + "\n";
		}
	}

	// formats how the observed lines will look, also adds in delta one and two
	private static void formatObservedBranch(ArrayList<Integer> jValues, List<List<Object>> observedLines,
			String branchType)
	{
		List<List<Object>> formattedOutput = new ArrayList<List<Object>>();
		List<Object> rowOfOutput;
		String deltaOne, deltaTwo;
		DecimalFormat df1 = new DecimalFormat("0.00000");
		DecimalFormat df2 = new DecimalFormat("0.000");

		final int ENERGY_INDEX = 1, DELTA_ONE_INDEX = 3;

		// General setup of output
		for (int i = 0; i < jValues.size(); i++) {
			rowOfOutput = new ArrayList<Object>();

			List<Object> currentLine = observedLines.get(i);
			Object energy = currentLine.get(0);
			Object intensity = currentLine.get(1);

			int currentJ = jValues.get(i);
			int nextJ;

			if ((i + 1) < jValues.size()) {
				nextJ = jValues.get(i + 1);

				rowOfOutput.add(currentJ);

				if (checkIfNumeric(energy)) {
					rowOfOutput.add(df1.format(energy));
				} else {
					rowOfOutput.add(energy);
				}

				if (checkIfNumeric(intensity)) {
					rowOfOutput.add(df2.format(intensity));
				} else {
					rowOfOutput.add(intensity);
				}

				formattedOutput.add(rowOfOutput);

				if (nextJ - currentJ != 1) {
					for (int j = 0; j < nextJ - currentJ - 1; j++) {
						rowOfOutput = new ArrayList<Object>();
						rowOfOutput.add(currentJ + j + 1);
						rowOfOutput.add("---------"); // energy
						rowOfOutput.add("-----"); // intensity
						formattedOutput.add(rowOfOutput);
					}

					continue;
				}
			} else {
				rowOfOutput.add(currentJ);

				if (checkIfNumeric(energy)) {
					rowOfOutput.add(df1.format(energy));
				} else {
					rowOfOutput.add(energy);
				}

				if (checkIfNumeric(intensity)) {
					rowOfOutput.add(df2.format(intensity));
				} else {
					rowOfOutput.add(intensity);
				}

				formattedOutput.add(rowOfOutput);
			}
		}

		// Delta 1
		for (int i = 0; i < formattedOutput.size(); i++) {
			List<Object> currentRow = formattedOutput.get(i);
			List<Object> nextRow;

			if ((i + 1) < formattedOutput.size()) {
				if (currentRow.get(ENERGY_INDEX).equals("---------")
						|| formattedOutput.get(i + 1).get(ENERGY_INDEX).equals("---------")) {
					deltaOne = "--------";
					currentRow.add(deltaOne);
					updateRow(formattedOutput, currentRow, i);
				} else {
					nextRow = formattedOutput.get(i + 1);

					double currentEnergy = Double.parseDouble((String) currentRow.get(ENERGY_INDEX));
					double nextEnergy = Double.parseDouble((String) nextRow.get(ENERGY_INDEX));
					deltaOne = df1.format(nextEnergy - currentEnergy) + "";

					currentRow.add(deltaOne);
					updateRow(formattedOutput, currentRow, i);
				}
			} else {
				deltaOne = "--------";
				currentRow.add(deltaOne);
				updateRow(formattedOutput, currentRow, i);
			}
		}

		// Delta 2
		for (int i = 0; i < formattedOutput.size(); i++) {
			List<Object> currentRow = formattedOutput.get(i);
			List<Object> previousRow;

			if ((i - 1) >= 0) {
				if (currentRow.get(DELTA_ONE_INDEX).equals("--------")
						|| formattedOutput.get(i - 1).get(DELTA_ONE_INDEX).equals("--------")
						|| i == (formattedOutput.size() - 1)) {
					deltaTwo = "--------";
					currentRow.add(deltaTwo);
					updateRow(formattedOutput, currentRow, i);
				} else {
					previousRow = formattedOutput.get(i - 1);

					double previousDeltaOne = Double.parseDouble(previousRow.get(DELTA_ONE_INDEX) + "");
					double currentDeltaOne = Double.parseDouble(currentRow.get(DELTA_ONE_INDEX) + "");
					deltaTwo = df1.format(currentDeltaOne - previousDeltaOne) + "";
					currentRow.add(deltaTwo);
					updateRow(formattedOutput, currentRow, i);
				}
			} else {
				deltaTwo = "--------";
				currentRow.add(deltaTwo);
				updateRow(formattedOutput, currentRow, i);
			}
		}

		buildOutputBranch(formattedOutput, branchType);
	}

	// Outputs the observed lines to the output file and formats how it looks
	// (places it under the header)
	public static void buildOutputBranch(List<List<Object>> formattedOutput, String type)
	{
		final int J_INDEX = 0, ENERGY_INDEX = 1, INTENSITY_INDEX = 2, DELTA_ONE_INDEX = 3, DELTA_TWO_INDEX = 4;

		String subHeader, branchInformation = "";
		subHeader = System.lineSeparator() + "Branch type: " + type + System.lineSeparator()
				+ "J     Wavenumber    Intensity         Δ1           Δ2\n";

		for (int i = 0; i < formattedOutput.size(); i++) {
			List<Object> currentRow = formattedOutput.get(i);

			String jVal = String.valueOf(currentRow.get(J_INDEX));
			String energyVal = String.valueOf(currentRow.get(ENERGY_INDEX));
			String intensityVal = String.valueOf(currentRow.get(INTENSITY_INDEX));
			String deltaOne = String.valueOf(currentRow.get(DELTA_ONE_INDEX));
			String deltaTwo = String.valueOf(currentRow.get(DELTA_TWO_INDEX));

			if(!deltaOne.equals("--------")) {
				double d1Val = Double.parseDouble(deltaOne);
				if(d1Val >= 0) {
					deltaOne = leftPadOneSpace(deltaOne);
				}
			}
			
			if(!deltaTwo.equals("--------")) {
				double d2Val = Double.parseDouble(deltaTwo);
				if(d2Val >= 0) {
					deltaTwo = leftPadOneSpace(deltaTwo);
				}
			}
			
			
			branchInformation += rightPadding(jVal, 6)
					+ rightPadding(energyVal, 16) 
					+ rightPadding(intensityVal, 13)
					+ ProduceAssignments.rightPadding(deltaOne, 13) 
					+ deltaTwo + "\n";
		}

		toBeOutputBranches.put(subHeader, branchInformation);
	}

	private static String rightPadding(String str, int num)
	{
		return String.format("%1$-" + num + "s", str);
	}
	
	private static String leftPadOneSpace(String str)
	{
		return " " + str;
	}

	// Displays the header that goes above every set of branches in the output.
	public void createDisplayHeader()
	{
		boolean isEType;
		int currentK, lowestJValue, highestJValue;

		// redundant now
		isEType = getIsEType();
		currentK = getCurrentK();
		lowestJValue = getLowestJValue();
		highestJValue = getHighestJValue();

		String symmetry;
		String kOutput;

		if (isEType) {
			if (currentK >= 0) {
				kOutput = currentK + "";
				symmetry = "E1";
			} else {
				kOutput = Math.abs(currentK) + "";
				symmetry = "E2";
			}
		} else {
			kOutput = Math.abs(currentK) + "";
			symmetry = "A";

			if (currentK >= 0) {
				kOutput += "+";
			} else {
				kOutput += "-";
			}
		}

		String ls = System.lineSeparator();
		mainHeader = "================================================================"
				+ ls + "Symmetry type:\t\t\t\t\t" + symmetry + ls
				+ "The K of the lower energy is:\t\t\t" + kOutput + ls + "The input "
				+ INPUT_BRANCH_TYPE + " branch was flipped:\t\t\t" + inputIsFlipped + ls
				+ "The input " + INPUT_BRANCH_TYPE + " branch used a J range of:\t\t"
				+ lowestJValue + " to " + highestJValue
				+ ls + ls + "Results for these selections are..." + ls;

		setHeaderDisplayState(true);
	}

	// Utility
	// methods----------------------------------------------------------------------------------------------------------------------------------

	// reverses the order of the input branch so it can be tested in reversed order
	private ArrayList<Double> reverseInputBranch(ArrayList<Double> inputBranchArray)
	{
		ArrayList<Double> reversedArray = new ArrayList<Double>();

		for (int i = 0; i < inputBranchArray.size(); i++) {
			reversedArray.add(inputBranchArray.get(inputBranchArray.size() - 1 - i));
		}

		inputIsFlipped = true;
		return reversedArray;
	}

	// gets the energy at a particular J value for the current K (Used for E type)
	private double energyAt(Integer jValueToFind, Map<Integer, Double> energyList) throws NoSuchEnergyException
	{
		for (Map.Entry<Integer, Double> entry : energyList.entrySet()) {
			if (entry.getKey() == jValueToFind) {
				return entry.getValue();
			}
		}

		throw new NoSuchEnergyException("Could not find the requested energy with J=" + jValueToFind);
	}

	/*
	 * gets the energy at a particular J value for the current K, if isPlusParity is
	 * true, then it gets the plus parity otherwise it gets the negative parity.
	 * (Used for A type)
	 */
	private double energyAt(Integer jValueToFind, boolean isPlusParity) throws NoSuchEnergyException
	{
		if (isPlusParity) {
			if (currentK >= 0) {
				for (Map.Entry<Integer, Double> entry : energiesForCurrentK.entrySet()) {
					if (entry.getKey() == jValueToFind) {
						return entry.getValue();
					}
				}
			} else {
				if (energiesForCurrentKOppParity == null) {
					throw new NoSuchEnergyException("There is no opposite parity for k=" + currentK);
				}

				for (Map.Entry<Integer, Double> entry : energiesForCurrentKOppParity.entrySet()) {
					if (entry.getKey() == jValueToFind) {
						return entry.getValue();
					}
				}
			}
		} else {
			if (currentK < 0) {
				for (Map.Entry<Integer, Double> entry : energiesForCurrentK.entrySet()) {
					if (entry.getKey() == jValueToFind) {
						return entry.getValue();
					}
				}
			} else {
				if (energiesForCurrentKOppParity == null) {
					throw new NoSuchEnergyException("There is no opposite parity for k=" + currentK);
				}

				for (Map.Entry<Integer, Double> entry : energiesForCurrentKOppParity.entrySet()) {
					if (entry.getKey() == jValueToFind) {
						return entry.getValue();
					}
				}
			}
		}

		throw new NoSuchEnergyException("Could not find the requested energy with J=" + jValueToFind);
	}

	// gets the set of energies at a particular K value
	@SuppressWarnings("unchecked")
	private LinkedHashMap<Integer, Double> getEnergiesAtK(int kValue,
			LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> listOfEnergies)
	{
		for (Map.Entry<Integer, LinkedHashMap<Integer, Double>> entry : listOfEnergies.entrySet()) {
			if (kValue == entry.getKey()) {
				return (LinkedHashMap<Integer, Double>) entry.getValue().clone();
			}
		}
		return null;
	}

	// gets the opposite parity for the current energies (if it exists)
	private void getOppositeParityEnergies(int kValue)
	{
		energiesForCurrentKOppParity = getEnergiesAtK(kValue * -1, ENERGIES_A);
	}

	// clears the arrays (happens every new J range or new K)
	private void resetArraysAndHeader()
	{
		mainHeader = "";
		energyVals = "";
		toBeOutputBranches.clear();
		associatedR.clear();
		associatedP.clear();
		associatedQ.clear();
		triangularP.clear();
		triangularQ.clear();
		triangularR.clear();
		inputBranchWithJ.clear();
		upperEnergyValuesWithJ.clear();

		if (energiesForCurrentKOppParity != null)
			energiesForCurrentKOppParity.clear();
	}

	// Some line intensities are strings, most are double, this checks which it is.
	public static boolean checkIfNumeric(Object toBeChecked)
	{
		try {
			Double.parseDouble((String) toBeChecked);
			return true;
		} catch (ClassCastException e) {
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static void updateRow(List<List<Object>> formattedOutput, List<Object> currentRow, int rowIndex)
	{
		formattedOutput.set(rowIndex, currentRow);
	}

	// filter. Calculates how many skips or jumps in the observed J values there is
	private boolean notTooManySkips(ArrayList<Integer> jValues)
	{
		int length = jValues.size();
		int span = jValues.get(jValues.size() - 1) - jValues.get(0) + 1;

		return span - length <= MAX_SKIPS;
	}

	// used for the loading bar
	private int determineNumberOfIterations()
	{
		int number = 0;

		for (@SuppressWarnings("unused")
		Map.Entry<Integer, LinkedHashMap<Integer, Double>> entry : ENERGIES_E.entrySet()) {
			number++;
		}

		for (@SuppressWarnings("unused")
		Map.Entry<Integer, LinkedHashMap<Integer, Double>> entry : ENERGIES_A.entrySet()) {
			number++;
		}

		return number * 2; // *2 is to account for the reversed branch iterations
	}

	// create the hash maps
	private void initMaps()
	{
		energiesForCurrentKOppParity = new LinkedHashMap<Integer, Double>();
		inputBranchWithJ = new LinkedHashMap<Integer, Double>();
		associatedR = new LinkedHashMap<Integer, Double>();
		associatedP = new LinkedHashMap<Integer, Double>();
		associatedQ = new LinkedHashMap<Integer, Double>();
		triangularP = new LinkedHashMap<Integer, Double>();
		triangularQ = new LinkedHashMap<Integer, Double>();
		triangularR = new LinkedHashMap<Integer, Double>();
		upperEnergyValuesWithJ = new LinkedHashMap<Integer, Double>();
	}

	public static void setHeaderDisplayState(boolean state)
	{
		headerShown = state;
	}

	public static boolean getHeaderDisplayState()
	{
		return headerShown;
	}

	public static boolean getIsEType()
	{
		return isEType;
	}

	public static int getCurrentK()
	{
		return currentK;
	}

	public static int getLowestJValue()
	{
		return lowestJValue;
	}

	public static int getHighestJValue()
	{
		return highestJValue;
	}
}
