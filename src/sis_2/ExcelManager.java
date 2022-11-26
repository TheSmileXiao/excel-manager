/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sis_2;

import POJOS.Trabajadorbbdd;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author MARTIN
 */
public class ExcelManager {
    
    private XSSFWorkbook wb;
    private String file;
    
    public ExcelManager(String fileName) {
        try {
            wb = new XSSFWorkbook(new FileInputStream(new File("resources/" + fileName)));
            file = "resources/" + fileName;
        } catch (IOException ex) {
            Logger.getLogger(ExcelManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public List<HashMap> readFile() {
        
        List<HashMap> hojas = new ArrayList<>();

        XSSFSheet sheet1 = wb.getSheetAt(0);
        HashMap hoja1 = new HashMap();

        XSSFSheet sheet2 = wb.getSheetAt(1);
        HashMap hoja2 = new HashMap();       

        XSSFSheet sheet3 = wb.getSheetAt(2);
        HashMap hoja3 = new HashMap();

        XSSFSheet sheet4 = wb.getSheetAt(3);
        HashMap hoja4 = new HashMap();

        XSSFSheet sheet5 = wb.getSheetAt(4);
        HashMap hoja5 = new HashMap();


        //Hoja 1
        Iterator rows1 = sheet1.rowIterator();
        while(rows1.hasNext()) {
            Row row = (Row) rows1.next();
            Iterator cells = row.cellIterator();

            while(cells.hasNext()) {
                Cell cell = (Cell) cells.next();
                String key = cell.getStringCellValue();
                cell = (Cell) cells.next();
                double val = cell.getNumericCellValue();
                hoja1.put(key, val);
            }
        }

        //Hoja 2
        Iterator rows2 = sheet2.rowIterator();
        if(rows2.hasNext()){
            rows2.next();
        }
        while(rows2.hasNext()) {
            Row row = (Row) rows2.next();
            Iterator cells = row.cellIterator();

            while(cells.hasNext()) {
                Cell cell = (Cell) cells.next();
                double key = cell.getNumericCellValue();
                cell = (Cell) cells.next();
                double val = cell.getNumericCellValue();
                hoja2.put(key, val);
            }
        }

        //Hoja 3
        Iterator rows3 = sheet3.rowIterator();
        if(rows3.hasNext()){
            rows3.next();
        }

        while(rows3.hasNext()) {
            Row row = (Row) rows3.next();
            Iterator cells = row.cellIterator();

            while(cells.hasNext()) {
                Cell cell = (Cell) cells.next();
                String key = cell.getStringCellValue();

                List<Double> values = new ArrayList<>();
                cell = (Cell) cells.next();
                values.add(cell.getNumericCellValue());
                cell = (Cell) cells.next();
                values.add(cell.getNumericCellValue());

                hoja3.put(key, values);
            }
        }

        //Hoja 4
        Iterator rows4 = sheet4.rowIterator();
        if(rows4.hasNext()){
            rows4.next();
        }

        while(rows4.hasNext()) {
            Row row = (Row) rows4.next();
            Iterator cells = row.cellIterator();

            while(cells.hasNext()) {
                Cell cell = (Cell) cells.next();
                double key = cell.getNumericCellValue();
                cell = (Cell) cells.next();
                double val = cell.getNumericCellValue();
                hoja4.put(key, val);
            }
        }       

        //Hoja 5
        for(int i=1; i<=sheet5.getLastRowNum(); i++) {
            Row row = sheet5.getRow(i);
            if(row != null) {
                if(!isEmpty(row)) {
                    Cell cell;
                    int key = i+1;
                    List<String> values = new ArrayList<>();
                    for(int j=0; j<13; j++) {
                        cell = row.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        if(cell == null) {
                            
                            values.add(" ");
//                            if(j==12) {
//                                System.out.print(i + "\t");
//                                System.out.println(values.size());
//                            }
                        }else {
                            
                            values.add(cell.toString());
//                            if(j==12){
//                                System.out.print(i + "\t");
//                                System.out.print(values.size() + "\t");
//                                System.out.println(cell.toString());
//                            }
                        }
                    }
                    hoja5.put(key, values);
                }
            }
        }

        hojas.add(hoja1);
        hojas.add(hoja2);
        hojas.add(hoja3);
        hojas.add(hoja4);
        hojas.add(hoja5);

        return hojas;
    }
    
    public void setNIF(int index, String newNIF) {
        XSSFSheet sheet = wb.getSheetAt(4);
        Row row = sheet.getRow(index-1);
        Cell cell = row.getCell(0);
        if(cell != null) {
            cell.setCellValue(newNIF);
        }else {
            cell = row.createCell(0);
            cell.setCellValue(newNIF);
        }
    }

    public void setIBAN(int index, String IBAN){
        XSSFSheet sheet = wb.getSheetAt(4);
        Row row = sheet.getRow(index-1);
        Cell cell = row.getCell(11);
        if(cell != null){
            cell.setCellValue(IBAN);
        }else {
            cell = row.createCell(11);
            cell.setCellValue(IBAN);
        }
    }

    public void setCCC(int index, String newCCC) {
        XSSFSheet sheet = wb.getSheetAt(4);
        Row row = sheet.getRow(index-1);
        Cell cell = row.getCell(9);
        if(cell != null) {
            cell.setCellValue(newCCC);
        }else {
            cell = row.createCell(9);
            cell.setCellValue(newCCC);
        }
    }

    public void setEmail(int index, String newEmail) {
        XSSFSheet sheet = wb.getSheetAt(4);
        Row row = sheet.getRow(index-1);
        Cell cell = row.getCell(12);
        if(cell != null) {
            cell.setCellValue(newEmail);
        }else {
            cell = row.createCell(12);
            cell.setCellValue(newEmail);
        }
    }
    
    public void saveChanges() {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
            wb.write(fos);
            fos.flush();
            fos.close();
        } catch (Exception ex) {
            Logger.getLogger(ExcelManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            
        }
    }
    
    private boolean isEmpty(Row row) {
        Iterator iter = row.iterator();
        while(iter.hasNext()) {
            Cell cell = (Cell) iter.next();
            if(cell.getCellType() != CellType.BLANK){
                return false;
            }
        }
        return true;
    }
}
