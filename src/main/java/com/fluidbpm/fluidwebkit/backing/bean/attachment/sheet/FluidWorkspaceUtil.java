/*
 * Koekiebox CONFIDENTIAL
 *
 * [2012] - [2017] Koekiebox (Pty) Ltd
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property
 * of Koekiebox and its suppliers, if any. The intellectual and
 * technical concepts contained herein are proprietary to Koekiebox
 * and its suppliers and may be covered by South African and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material is strictly
 * forbidden unless prior written permission is obtained from Koekiebox Innovations.
 */
package com.fluidbpm.fluidwebkit.backing.bean.attachment.sheet;

import com.fluidbpm.fluidwebkit.exception.ClientDashboardException;
import com.fluidbpm.program.api.util.UtilGlobal;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.util.CellReference;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Fluid workspace class for extracting excel based content.
 *
 * @author jasonbruwer on 8/1/17.
 * @since 1.1
 */
public class FluidWorkspaceUtil {
	public List<Worksheet> getWorksheetsFromCSV(InputStream inputStreamParam) {
		return this.getWorksheetsFromCSV(
				inputStreamParam,',', '"');
	}

	public List<Worksheet> getWorksheetsFromCSV(InputStream inputStreamParam, char separator, char quoteChar) {
		CSVReader csvReader = new CSVReader(new InputStreamReader(inputStreamParam));
		List<Worksheet> returnVal = new ArrayList();
		try {
			List<String[]> rows = csvReader.readAll();
			if (rows != null && !rows.isEmpty()) {
				List<Row> returnValRows = new ArrayList();

				for (String[] columns : rows) {
					List<Cell> cellsToAdd = new ArrayList();

					for (String column : columns) cellsToAdd.add(new Cell(column));
					if (cellsToAdd.isEmpty()) continue;

					returnValRows.add(new Row(cellsToAdd));
				}

				returnVal.add(new Worksheet(returnValRows, "[Not Set]"));
			}
		} catch (IOException ioExcept) {
			throw new ClientDashboardException(
					String.format("Unable to extract CSV Values. %s", ioExcept.getMessage()),
					ioExcept, ClientDashboardException.ErrorCode.IO_ERROR);
		} catch (CsvException csvExcept) {
			throw new ClientDashboardException(
					String.format(
							"Unable to extract CSV Values Line[%d]. %s",
							csvExcept.getLineNumber(), csvExcept.getMessage()),
					csvExcept, ClientDashboardException.ErrorCode.IO_ERROR);
		}
		return returnVal;
	}

	public List<Worksheet> getWorksheetsFromExcel(InputStream inputStreamParam) {
		return this.getWorksheetsFromExcel(inputStreamParam, null);
	}

	public List<Worksheet> getWorksheetsFromExcel(InputStream inputStreamParam, String passwordParam) {
		final byte[] workbookBytes;
		try {
			workbookBytes = readAllBytes(inputStreamParam);
		} catch (IOException ioExcept) {
			throw new ClientDashboardException(
					"Unable to extract Worksheets. " + ioExcept.getMessage(), ioExcept,
					ClientDashboardException.ErrorCode.IO_ERROR);
		}

		final String password = UtilGlobal.isBlank(passwordParam) ? null : passwordParam;
		try {
			org.apache.poi.ss.usermodel.Workbook workbook =
					this.createWorkbook(workbookBytes, password);

			List<Worksheet> sheets = new ArrayList();
			for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
				org.apache.poi.ss.usermodel.Sheet sheetAtIndex = workbook.getSheetAt(sheetIndex);
				if (sheetAtIndex == null) continue;

				Iterator<org.apache.poi.ss.usermodel.Row> rowIterator = sheetAtIndex.rowIterator();
				if (rowIterator == null) continue;

				List<Row> rows = new ArrayList();
				while (rowIterator.hasNext()) {
					org.apache.poi.ss.usermodel.Row rowAtIndex = rowIterator.next();

					short cellCounter = rowAtIndex.getLastCellNum();

					List<Cell> cells = new ArrayList();

					for (short cellIndex = 0;cellIndex < cellCounter;cellIndex++) {
						org.apache.poi.ss.usermodel.Cell cellAtIndex = rowAtIndex.getCell(cellIndex);

						String upperCaseLetter = CellReference.convertNumToColString(cellIndex).toUpperCase();
						String headerText =
								(""+ (cellIndex + 1) + " - " + upperCaseLetter);
						cells.add(new Cell(this.getCellValueAsString(workbook,cellAtIndex), headerText));
					}
					rows.add(new Row(cells));
				}

				sheets.add(new Worksheet(rows, sheetAtIndex.getSheetName()));
			}
			return sheets;
		} catch (Exception eParam) {

			throw new ClientDashboardException(
					"Unable to extract Worksheets. "+eParam.getMessage(),eParam,
					ClientDashboardException.ErrorCode.IO_ERROR);
		}
	}

	/**
	 * Open a workbook from the supplied bytes.
	 *
	 * Some {@code .xlsx} files (typically produced by non-Microsoft tooling) declare
	 * hyperlinks on a sheet that reference a relationship id which is missing from the
	 * sheet's {@code .rels} part. POI raises
	 * {@code IllegalStateException: The hyperlink for cell ... references relation ...}
	 * while opening the workbook, before any value can be read. As we only extract cell
	 * values here, hyperlinks are irrelevant, so on that specific failure we strip the
	 * offending hyperlink definitions from the sheet XML and retry once.
	 */
	private org.apache.poi.ss.usermodel.Workbook createWorkbook(byte[] workbookBytesParam, String passwordParam) throws IOException {
		try {
			return org.apache.poi.ss.usermodel.WorkbookFactory.create(
					new ByteArrayInputStream(workbookBytesParam), passwordParam);
		} catch (IllegalStateException stateExcept) {
			byte[] repaired = stripHyperlinksFromXlsx(workbookBytesParam);
			if (repaired == null) throw stateExcept;

			return org.apache.poi.ss.usermodel.WorkbookFactory.create(
					new ByteArrayInputStream(repaired), passwordParam);
		}
	}

	/** Matches the {@code <hyperlinks>...</hyperlinks>} block (and its self-closing form) in a worksheet part. */
	private static final Pattern HYPERLINKS_BLOCK =
			Pattern.compile("<hyperlinks\\b[^>]*?/>|<hyperlinks\\b.*?</hyperlinks>", Pattern.DOTALL);

	/**
	 * Rebuild an {@code .xlsx} (zip) in memory with every {@code <hyperlinks>} block removed
	 * from the worksheet parts. Returns {@code null} when the bytes are not a readable zip
	 * (e.g. a legacy {@code .xls}/OLE2 or encrypted container) or when nothing was changed,
	 * so the caller can surface the original failure.
	 */
	private byte[] stripHyperlinksFromXlsx(byte[] workbookBytesParam) {
		ByteArrayOutputStream repaired = new ByteArrayOutputStream(workbookBytesParam.length);
		boolean modified = false;
		try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(workbookBytesParam));
			 ZipOutputStream zipOut = new ZipOutputStream(repaired)) {
			ZipEntry entry;
			int entryCount = 0;
			while ((entry = zipIn.getNextEntry()) != null) {
				entryCount++;
				byte[] entryBytes = readAllBytes(zipIn);

				String name = entry.getName();
				if (name.startsWith("xl/worksheets/") && name.endsWith(".xml")) {
					String xml = new String(entryBytes, StandardCharsets.UTF_8);
					String cleaned = HYPERLINKS_BLOCK.matcher(xml).replaceAll("");
					if (!cleaned.equals(xml)) {
						entryBytes = cleaned.getBytes(StandardCharsets.UTF_8);
						modified = true;
					}
				}

				zipOut.putNextEntry(new ZipEntry(name));
				zipOut.write(entryBytes);
				zipOut.closeEntry();
			}
			if (entryCount == 0) return null;
		} catch (IOException ioExcept) {
			return null;
		}
		return modified ? repaired.toByteArray() : null;
	}

	private static byte[] readAllBytes(InputStream inputStreamParam) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] chunk = new byte[8192];
		int read;
		while ((read = inputStreamParam.read(chunk)) != -1) buffer.write(chunk, 0, read);
		return buffer.toByteArray();
	}

	private String getCellValueAsString(org.apache.poi.ss.usermodel.Workbook workbookParam, org.apache.poi.ss.usermodel.Cell cellParam) {

		if (cellParam == null) return UtilGlobal.EMPTY;

		if (cellParam.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
			boolean its = false;
			if (its) return cellParam.getCellFormula();

			org.apache.poi.ss.usermodel.FormulaEvaluator evaluator = null;
			if (workbookParam instanceof org.apache.poi.hssf.usermodel.HSSFWorkbook) {
				evaluator = new
						org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator(
								(org.apache.poi.hssf.usermodel.HSSFWorkbook)workbookParam);
			} else if (workbookParam instanceof org.apache.poi.xssf.usermodel.XSSFWorkbook) {
				evaluator = new org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator(
						(org.apache.poi.xssf.usermodel.XSSFWorkbook)workbookParam);
			} else {
				return cellParam.getCellFormula();
			}

			String[] workbookNames = {"Workbook"};

			//XLS
			if (workbookParam instanceof org.apache.poi.hssf.usermodel.HSSFWorkbook) {
				org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator[] evaluators =
						{(org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator)evaluator};
				org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator.setupEnvironment(workbookNames, evaluators);
			}
			//XlSX - XML
			else if(workbookParam instanceof org.apache.poi.xssf.usermodel.XSSFWorkbook) {
				org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator.create(
						(org.apache.poi.xssf.usermodel.XSSFWorkbook)workbookParam, null,null);
			}

			org.apache.poi.ss.usermodel.CellValue cellValue = null;
			try {
				cellValue = evaluator.evaluate(cellParam);
			} catch (Exception except) {
				return cellParam.getCellFormula();
			}

			final org.apache.poi.ss.usermodel.CellType cellType = cellValue.getCellType();

			if (cellType == org.apache.poi.ss.usermodel.CellType.BLANK) {
				return cellValue.getStringValue();
			} else if (cellType == org.apache.poi.ss.usermodel.CellType.BOOLEAN) {
				return Boolean.toString(cellValue.getBooleanValue());
			} else if (cellType == org.apache.poi.ss.usermodel.CellType.ERROR) {
				return Integer.toHexString(cellValue.getErrorValue());
			} else if (cellType == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
				return Double.toString(cellValue.getNumberValue());
			} else if (cellType == org.apache.poi.ss.usermodel.CellType.STRING) {
				return cellValue.getStringValue();
			}
		}

		//Set to String value if not String.
		if (cellParam.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
			return Double.valueOf(cellParam.getNumericCellValue()).toString();
		} else if (cellParam.getCellType() != org.apache.poi.ss.usermodel.CellType.STRING) {
			cellParam.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
		}

		return cellParam.getStringCellValue();
	}
}
