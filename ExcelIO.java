package unassignedBranchDeterminer;

import java.io.BufferedWriter;

//Used to read the energy excel file

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ExcelIO
{
	private Workbook workbook;
	private Sheet sheet;

	private static LinkedHashMap<Integer, LinkedHashMap<Integer, Double>> energiesE, energiesA;
	private static BufferedWriter bw;
	private static int startingIndexE, startingIndexA;
	private static String energySheetNameE, energySheetNameA;

	private final List<File> INPUT_FILE_ARRAY;
	private final boolean IS_EVEN_SELECTION_RULES;

	private static final String OUTPUT_FILE_NAME = "Combination difference results.txt";
	private static final int COLUMN_INDEX_J = 0, STARTING_CELL_INDEX = 1;

	private final MultiThreading workerThread;

	public ExcelIO(GUIDisplay guiObject) {
		workerThread = guiObject.getThreadObject();
		INPUT_FILE_ARRAY = guiObject.getInputFileArray();

		energySheetNameE = guiObject.getSheetNameE();
		energySheetNameA = guiObject.getSheetNameA();
		IS_EVEN_SELECTION_RULES = convertSelectionRuleToBoolean(GUIDisplay.getSelectionRulesToUse());

		// The -1 converts them to an index
		startingIndexE = guiObject.getStartingRowE() - 1;
		startingIndexA = guiObject.getStartingRowA() - 1;

		energiesE = new LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>();
		energiesA = new LinkedHashMap<Integer, LinkedHashMap<Integer, Double>>();

		try {
			new SearchPeakFinder(INPUT_FILE_ARRAY.get(1), workerThread, guiObject);
			workbook = WorkbookFactory.create(INPUT_FILE_ARRAY.get(0));
			workbook.setMissingCellPolicy(MissingCellPolicy.CREATE_NULL_AS_BLANK);
		} catch (EncryptedDocumentException | InvalidFormatException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,
					"Trouble reading one of the files, make sure both are closed before running");
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
		}

		workerThread.myPublish("msg Reading E symmetry energy values");

		selectSheet(energySheetNameE);
		readInAllEnergies(startingIndexE, true);

		workerThread.myPublish("msg Reading A symmetry energy values");

		selectSheet(energySheetNameA);
		readInAllEnergies(startingIndexA, false);

		try {
			workbook.close();
		} catch (IOException ignored) {
		}

		new ProduceAssignments(guiObject, energiesE, energiesA, IS_EVEN_SELECTION_RULES);
	}

	private boolean convertSelectionRuleToBoolean(String selectionRuleType)
	{
		if (selectionRuleType.equals("Even ΔVt")) {
			return true;
		} else {
			return false;
		}

	}

	/*
	 * Reads in the energies (both A and E) from the energy file, it starts at the
	 * row with K values (which is entered by the user). For each column, it goes
	 * down cell by cell until it reaches a number, and then it starts to reads in
	 * all the numbers until it reaches a blank cell. Once a blank cell is reached,
	 * it assigns the k value in that column to those energies using a linked hash
	 * map. This process repeats for other until a non numeric "origin" row is
	 * reached (eg. the row one below the K row)
	 */
	private void readInAllEnergies(int kRow, boolean isEType)
	{
		int originRow = kRow + 1;
		int currentColumnIndex = STARTING_CELL_INDEX;

		Row row = sheet.getRow(originRow);
		Cell cell = row.getCell(STARTING_CELL_INDEX);

		// read through all K values, stop when theres no more origin data
		while (cell.getCellTypeEnum() == CellType.NUMERIC) {
			int currentRowIndex = originRow + 1;
			int k;
			row = sheet.getRow(kRow);
			cell = row.getCell(currentColumnIndex);

			// gets the K value for the column
			if (cell.getCellTypeEnum() == CellType.STRING) {
				k = Integer.parseInt(cell.getStringCellValue());
			} else {
				k = (int) cell.getNumericCellValue();
			}

			row = sheet.getRow(currentRowIndex);
			cell = row.getCell(currentColumnIndex);

			// skips first few rows before the energies
			while (cell.getCellTypeEnum() != CellType.NUMERIC) {
				currentRowIndex++;
				row = sheet.getRow(currentRowIndex);
				cell = row.getCell(currentColumnIndex);

				// stops reading this sheet if after 46 rows nothing is found
				// (46 because more than this flows into the next block of data)
				if (currentRowIndex > 46 + currentRowIndex)
					return;
			}

			LinkedHashMap<Integer, Double> energiesWithJ = new LinkedHashMap<Integer, Double>();

			// reads the energies for a given k
			while (cell.getCellTypeEnum() == CellType.NUMERIC) {
				double energy = cell.getNumericCellValue();

				cell = row.getCell(COLUMN_INDEX_J);
				int jValue = (int) cell.getNumericCellValue();

				energiesWithJ.put(jValue, energy);

				currentRowIndex++;
				row = sheet.getRow(currentRowIndex);
				cell = row.getCell(currentColumnIndex);
			}

			// map the k value to its energies
			if (isEType) {
				energiesE.put(k, energiesWithJ);
			} else {
				energiesA.put(k, energiesWithJ);
			}

			currentColumnIndex++;
			row = sheet.getRow(originRow);
			cell = row.getCell(currentColumnIndex);
		}
	}

	private void selectSheet(String sheetName)
	{
		sheet = workbook.getSheet(sheetName);

		if (sheet == null) {
			JOptionPane.showMessageDialog(null, "No sheet named \"" + sheetName + "\" in the energy file \""
					+ INPUT_FILE_ARRAY.get(0).getName() + "\".\nThis is required for the program to run!");
			System.exit(1);
		}
	}

	public static void createOutputFile() throws IOException
	{
		File file = new File(System.getProperty("user.home"), "Desktop");

		if (!file.exists())
			file.createNewFile();
		else {
			PrintWriter pwTemp = new PrintWriter(file + "\\" + OUTPUT_FILE_NAME);
			pwTemp.close();
		}

		OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(file + "\\" + OUTPUT_FILE_NAME, true),
				"UTF8");
		bw = new BufferedWriter(fw);
	}

	public static void outputToFile(JTextArea textArea) throws IOException
	{
		textArea.write(bw);
		bw.newLine();
		bw.flush();
		bw.close();
	}
}
