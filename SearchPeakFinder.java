package unassignedBranchDeterminer;

import java.io.BufferedReader;

//Looks through the peak finder file and returns the observed line and its intensity if a match is found within the tolerance

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class SearchPeakFinder
{
	private static final int ENERGY_INDEX = 0, INTENSITY_INDEX = 1, LINES_TO_CHECK = 2;

	private static double tolerance;
	private static Workbook workbook;
	private static Sheet sheet;
	private static List<List<Object>> peakList = new ArrayList<List<Object>>();
	private static MultiThreading workerThread;
	private static String priorityForMatching;
	private static File peakFile;
	private static GUIDisplay guiObject;

	private static int startingRowIndex = 4, linesInFile, maxIndex;

	public SearchPeakFinder(File peakFinderFile, MultiThreading workerThreadIn, GUIDisplay guiObj) {

		guiObject = guiObj;
		peakFile = peakFinderFile;
		priorityForMatching = guiObject.getPriorityForMatching();
		tolerance = guiObject.getTolerance();

		workerThread = workerThreadIn;
		workerThread.myPublish("msg Loading in the Peak File...");

		if (getFileType(peakFile).equals("xlsx")) {
			readExcelFile();
		} else {
			readCSVFile();
		}
	}

	public static String getFileType(File file)
	{
		String fileName = file.getName();
		int dotIndex = fileName.lastIndexOf('.');
		return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
	}

	private static void readCSVFile()
	{
		try {
			FileReader fr = new FileReader(peakFile);
			BufferedReader br = new BufferedReader(fr);

			String fileLine;
			final int LINES_TO_SKIP = 4;
			
			for(int i = 0; i < LINES_TO_SKIP; i++) {
				br.readLine();
			}
			
			ArrayList<Object> line;
			
			while ((fileLine = br.readLine()) != null) {
				line = new ArrayList<Object>();
				String[] lineElements = fileLine.split(",");
				
				for(int i = 0; i < 2; i++) {
					lineElements[i] = lineElements[i].replace("\"", "");
				}
				
				line.add(Double.parseDouble(lineElements[0]));
				
				try {
					line.add(Double.parseDouble(lineElements[1]));
				}
				catch(Exception e) {
					line.add(lineElements[1]);
				}
				
				peakList.add(line);
			}
			
			br.close();
			guiObject.setToIndeterminate(false);
			
		} catch (FileNotFoundException ignored) {
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Could not read peak file: IO Exception");
		}
	}

	private static void readExcelFile()
	{
		try {
			workbook = WorkbookFactory.create(peakFile);
		} catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
			JOptionPane.showMessageDialog(null, "Issue reading the peak file...");
			System.exit(1);
		}

		workbook.setMissingCellPolicy(MissingCellPolicy.CREATE_NULL_AS_BLANK);
		sheet = workbook.getSheetAt(0);

		Row row = sheet.getRow(0);
		Cell cell = row.getCell(2);
		linesInFile = (int) cell.getNumericCellValue();
		maxIndex = linesInFile - startingRowIndex + 1;

		guiObject.setToIndeterminate(false);
		readExcelPeakListAsArray();

		try {
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void readExcelPeakListAsArray()
	{
		Row row;
		Cell cell;

		for (int currentIndex = startingRowIndex; currentIndex < maxIndex; currentIndex++) {
			if (currentIndex % 100 == 0) {
				int percent = (int) ((currentIndex / (double) maxIndex) * 20);
				workerThread.myPublish("msg Reading Peak List - Row: " + (currentIndex + 1) + "    " + percent + "%");
				workerThread.myPublish("val " + percent);
			}

			ArrayList<Object> line = new ArrayList<Object>();

			row = sheet.getRow(currentIndex);
			cell = row.getCell(ENERGY_INDEX);

			line.add(cell.getNumericCellValue());

			cell = row.getCell(INTENSITY_INDEX);

			if (cell.getCellTypeEnum() == CellType.NUMERIC) {
				line.add(cell.getNumericCellValue());
			} else {
				line.add(cell.getStringCellValue());
			}

			peakList.add(line);
		}
	}

	public static List<Object> binarySearchForLine(double lineValueToFind) throws CantFindLineException
	{
		List<Object> line = new ArrayList<Object>();

		int low = 0;
		int high = peakList.size() - 1;

		while (low <= high) {
			int mid = (low + high) / 2;
			double energyOfLine = (double) peakList.get(mid).get(ENERGY_INDEX);
			
			if (Math.abs((energyOfLine - lineValueToFind)) <= tolerance) {
				line.add(energyOfLine);
				line.add(peakList.get(mid).get(INTENSITY_INDEX));

				if (priorityForMatching.equals("Closest to Prediction")) {
					return findClosestMatch(mid, lineValueToFind);
				} else {
					return findMostIntenseMatch(mid, lineValueToFind);
				}
			}

			if (energyOfLine < lineValueToFind) {
				low = mid + 1;
			} else {
				high = mid - 1;
			}
		}

		throw new CantFindLineException();
	}

	public static List<Object> findMostIntenseMatch(int indexOfMatch, double lineValueToFind)
	{
		double highestIntensity = 0;
		int indexOfMostIntenseLine = indexOfMatch;

		if (ProduceAssignments.checkIfNumeric(peakList.get(indexOfMatch).get(INTENSITY_INDEX))) {
			highestIntensity = (double) peakList.get(indexOfMatch).get(INTENSITY_INDEX);
		}

		for (int i = 1; i <= LINES_TO_CHECK; i++) {

			if (indexOfMatch + i <= maxIndex) {
				double high = (double) peakList.get(indexOfMatch + i).get(ENERGY_INDEX);

				if (Math.abs(high - lineValueToFind) < tolerance) {
					Object intensity = peakList.get(indexOfMatch + i).get(INTENSITY_INDEX);

					if (ProduceAssignments.checkIfNumeric(intensity)) {
						if ((double) intensity > highestIntensity) {
							highestIntensity = (double) intensity;
							indexOfMostIntenseLine = indexOfMatch + i;
						}
					} else if (highestIntensity == 0) {
						// An intensity of type string is only assigned the largest if no numeric
						// intensities have been found
						indexOfMostIntenseLine = indexOfMatch + i;
					}
				}
			}

			if (indexOfMatch - i >= startingRowIndex) {
				double low = (double) peakList.get(indexOfMatch - i).get(ENERGY_INDEX);

				if (Math.abs(low - lineValueToFind) < tolerance) {
					Object intensity = peakList.get(indexOfMatch - i).get(INTENSITY_INDEX);

					if (ProduceAssignments.checkIfNumeric(intensity)) {
						if ((double) intensity > highestIntensity) {
							highestIntensity = (double) intensity;
							indexOfMostIntenseLine = indexOfMatch - i;
						}
					} else if (highestIntensity == 0) {
						indexOfMostIntenseLine = indexOfMatch - i;
					}
				}
			}
		}

		List<Object> mostIntenseLine = new ArrayList<Object>();
		mostIntenseLine.add(peakList.get(indexOfMostIntenseLine).get(ENERGY_INDEX));
		mostIntenseLine.add(peakList.get(indexOfMostIntenseLine).get(INTENSITY_INDEX));

		return mostIntenseLine;
	}

	public static List<Object> findClosestMatch(int indexOfMatch, double lineValueToFind)
	{
		double closestLine = (double) peakList.get(indexOfMatch).get(ENERGY_INDEX);
		double smallestDifference = Math.abs(closestLine - lineValueToFind);
		int indexOfClosestLine = indexOfMatch;

		for (int i = 1; i <= LINES_TO_CHECK; i++) {
			if (indexOfMatch + i <= maxIndex) {
				double lineTestHigh = (double) peakList.get(indexOfMatch + i).get(ENERGY_INDEX);
				double difference = Math.abs(lineTestHigh - lineValueToFind);

				if (difference < smallestDifference) {
					smallestDifference = difference;
					indexOfClosestLine = indexOfMatch + i;
				}
			}

			if (indexOfMatch - i >= startingRowIndex) {
				double lineTestLow = (double) peakList.get(indexOfMatch - i).get(ENERGY_INDEX);
				double difference = Math.abs(lineTestLow - lineValueToFind);

				if (difference < smallestDifference) {
					smallestDifference = difference;
					indexOfClosestLine = indexOfMatch - i;
				}
			}
		}

		List<Object> closestMatch = new ArrayList<Object>();
		closestMatch.add(peakList.get(indexOfClosestLine).get(ENERGY_INDEX));
		closestMatch.add(peakList.get(indexOfClosestLine).get(INTENSITY_INDEX));

		return closestMatch;
	}

	public static boolean areValidFileTypes(File energyFile, File peakFinderFile)
	{
		String fileName = energyFile.getName();
		int dotIndex = fileName.lastIndexOf('.');
		String energyFileExtension = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);

		fileName = peakFinderFile.getName();
		dotIndex = fileName.lastIndexOf('.');
		String peakFileExtension = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);

		if (energyFileExtension.equals("xlsx")
				&& (peakFileExtension.equals("xlsx") || peakFileExtension.equals("csv"))) {
			return true;
		} else {
			return false;
		}
	}
}
