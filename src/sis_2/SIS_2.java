/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sis_2;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.Style;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.VerticalAlignment;
import java.net.MalformedURLException;
import java.text.DateFormatSymbols;

import POJOS.Categorias;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import java.text.DecimalFormat;
import POJOS.Empresas;
import POJOS.Nomina;
import POJOS.Trabajadorbbdd;
import com.itextpdf.kernel.colors.ColorConstants;
import modelo.HibernateUtil;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 *
 * @author MARTIN
 */
public class SIS_2 {
    
    private static String imagen = "resources/TecnoProyect.png";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ParseException {

        DecimalFormat df = new DecimalFormat("0.00");

        SessionFactory sf = null;
        Session session = null;
        sf = HibernateUtil.getSessionFactory();
        session = sf.openSession();
        Transaction tx = session.beginTransaction();//        

        boolean done = false;

        ExcelManager ex = new ExcelManager("SistemasInformacionII.xlsx");
        List<HashMap> hojas = ex.readFile();
        
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
        Calendar cal = Calendar.getInstance();

        List<String> nifs = new ArrayList<>();
        List<Map.Entry> duplicates = new ArrayList<>();

        List<Map.Entry> errorsCCC = new ArrayList<>();

        List<String> empresas = new ArrayList<>();
        List<List<String>> correos = new ArrayList<>();
        
        HashMap map = hojas.get(4);
        Set set = map.entrySet();
        
        HashMap h0 = hojas.get(0);
        Set set0 = h0.entrySet();
        
        HashMap h1 = hojas.get(1);
        Set set1 = h1.entrySet();
        
        HashMap h2 = hojas.get(2);
        Set set2 = h2.entrySet();
        
        HashMap h3 = hojas.get(3);
        Set set3 = h3.entrySet();
        
        //OBTENER FECHA DE LA CONSOLA
        Scanner sc = new Scanner(System.in);
        System.out.println("Introduzca la fecha de las nóminas");
        String date = sc.next();
        int month = Integer.parseInt(date.substring(0, 2));
        int year = Integer.parseInt(date.substring(3));
        
        Iterator iter = set.iterator();
        List<Nomina> nominas = new ArrayList<Nomina>();
        List<Integer> indexNominas = new ArrayList<Integer>();
        List<String> extras = new ArrayList<String>();
        while(iter.hasNext()) {
            Map.Entry<Integer, Object> entry = (Map.Entry) iter.next();
            List<String> row = (List) entry.getValue();
            
            String nif = row.get(0);
            String name = row.get(1);
            String surName1 = row.get(2);
            String surName2 = row.get(3);
            String cif = row.get(4);
            String company = row.get(5);
            String entryDate = row.get(6);
            String category = row.get(7);
            String proration = row.get(8);
            String ccc = row.get(9);
            String country = row.get(10);
            String IBAN = row.get(11);
            String email = row.get(12);
            
            //Comprobar NIF correcto
            Validator validator = new Validator();
            if(nif.length() == 9) {
                String correctNIF = validator.nifValid(nif);
                if(correctNIF.compareTo(nif) != 0) {
                    ex.setNIF(entry.getKey(), correctNIF);
                    nif = correctNIF;
                }
            }
        
            //Comprobar NIF duplicado
            if(nif.length() == 9) {
                if(nifs.contains(nif)) {
                    duplicates.add(entry);
                }
                nifs.add(nif);
            }else {
                duplicates.add(entry);
            }

            //Comprobar CCC
            boolean validCode = true;
            
            //Obtener digitos de control del CCC
            int[] cDigits = new int[2];
            cDigits[0] = Integer.parseInt(String.valueOf(ccc.charAt(8)));
            cDigits[1] = Integer.parseInt(String.valueOf(ccc.charAt(9)));
            
            //Calcular digitos de control del CCC
            int[] compDigits = new int[2];
            compDigits[0] = validator.cccValid("00" + ccc.substring(0, 8));
            compDigits[1] = validator.cccValid(ccc.substring(10));

            //Comprobar si los digitos se corresponden
            for(int i=0; i<2; i++) {
                if(cDigits[i] != compDigits[i]) {
                    validCode = false;
                }
            }

            //Corregir CCC en caso de error
            if(!validCode) {
                ccc = ccc.substring(0, 8) + compDigits[0] + compDigits[1] + ccc.substring(10);
                ex.setCCC(entry.getKey(), ccc);
                errorsCCC.add(entry);
            }

            //GENERAR IBAN
            IBAN = validator.calculoIBAN(ccc, country);
            row.set(11, IBAN);
            ex.setIBAN(entry.getKey(), IBAN);


            //GENERAR CORREOS
            if(email.compareTo(" ") == 0) {
                //Comprobar si no hay ningun correo de la empresa actual
                if(empresas.indexOf(company) == -1) {
                    empresas.add(company);
                    correos.add(new ArrayList<>());
                }
                
                int count = 0;
                
                String initial2 = "";
                String initial1;
                String initialName;
                String repNum;
                
                if(!surName2.equals(" ")) {
                    initial2 = String.valueOf(surName2.charAt(0));
                }
                initial1 = String.valueOf(surName1.charAt(0));
                initialName = String.valueOf(name.charAt(0));
                
                String initials = initial2 + initial1 + initialName;
                for(int i=0; i<correos.get(empresas.indexOf(company)).size(); i++) {
                    if(correos.get(empresas.indexOf(company)).get(i).equals(initials)) {
                        count ++;
                    }
                }
                
                if(count < 10) {
                    repNum = "0" + count;
                }else {
                    repNum = String.valueOf(count);
                }
                
                String newEmail = initial2 + initial1 + initialName + repNum + "@" + company + ".es";
                correos.get(empresas.indexOf(company)).add(initials);
                ex.setEmail(entry.getKey(), newEmail);
                email = newEmail;
            }else {
                if(empresas.indexOf(cif) == -1) {
                    empresas.add(cif);
                    correos.add(new ArrayList<>());
                }
                correos.get(empresas.indexOf(cif)).add(email.substring(0,3));
            }
            
            /////////////////////////
            //GENERACION DE NOMINAS//
            /////////////////////////
            int monthEmployee;
            int yearEmployee;
                
            try {
                //Obtener fecha del trabajador
                Date d = formatter.parse(entryDate);
                cal.setTime(d);
                monthEmployee = cal.get(Calendar.MONTH)+1;
                yearEmployee = cal.get(Calendar.YEAR);

                //Comprobar si se ha de generar nomina del empleado
                if((yearEmployee < year) || ((yearEmployee == year) && (monthEmployee <= month))) {
                    double base = 0, comp = 0, brutoAnual, importeTrienios;
                    //Calcular trienios correspondientes
                    int trienio = 0;
                    if(year >= cal.get(Calendar.YEAR)){
                        trienio = (year-cal.get(Calendar.YEAR))/3;
                        if(trienio != 0) {
                            importeTrienios = (double) h1.get((double) trienio);
                        }else {
                            importeTrienios = 0;
                        }
                    }else {
                        importeTrienios = 0;
                    }
                    //Calcular bruto anual
                    List<Double> values = (List) h2.get(category);
                    base = values.get(1);
                    comp = values.get(0);
                    double bruto = base + comp + (importeTrienios * 14);
                    int monthsWorked;
                    if(yearEmployee == year) {
                        monthsWorked = 13 - monthEmployee;
                        if(proration.equals("SI")) {
                            brutoAnual = (bruto * monthsWorked) / 12;
                        }else {
                            brutoAnual = (bruto/14) * monthsWorked + (bruto/14) * 2 * (monthsWorked - 1) / 12;
                        }
                    }else {
                        monthsWorked = 12;
                        brutoAnual = bruto;
                    }
                    
                    //Comprobar cambio de trienio
                    double prorrateoMes = 0.0;
                    int trienioSig = (year + 1 - cal.get(Calendar.YEAR)) / 3;
                    int trienioActual = (year - cal.get(Calendar.YEAR)) / 3;
                    if (proration.equals("SI")) {    
                        if(trienioSig != trienioActual) {
                            double importeSig = (double) h1.get((double) trienioSig);
                            brutoAnual = (bruto - (importeTrienios/6) + (importeSig/6));
                            if(month == 12) {
                                prorrateoMes = (base/14 + comp/14 + importeSig)/6;
                            }
                        }
                    }

                    double brutoMesPro = bruto / 12;
                    double brutoMensual;
                    if(proration.equals("SI")) {
                        if(prorrateoMes != 0.0) {
                            brutoMesPro = base/14 + comp/14 + importeTrienios + prorrateoMes;
                        }
                        brutoMensual = brutoMesPro;
                    }else {
                        if(trienioSig != trienioActual && month == 12) {
                            brutoMesPro = brutoMesPro - importeTrienios/6 + (double) h1.get((double) trienioSig)/6;
                        }
                        brutoMensual = bruto/14;
                    }
                    
                    //Importes Trabajador
                    double baseMes = base / 14;
                    double compMes = comp / 14;
                    double antMes = importeTrienios;
                    if(proration.equals("SI")) {
                        if(prorrateoMes == 0.0) {
                            prorrateoMes = (base/14 + comp/14 + antMes)/6;
                        }
                    }

                    //Descuentos Trabajador
                    double [] porcentajes = new double[3];
                    porcentajes[0] = (double) h0.get("Cuota obrera general TRABAJADOR");
                    porcentajes[1] = (double) h0.get("Cuota desempleo TRABAJADOR");
                    porcentajes[2] = (double) h0.get("Cuota formación TRABAJADOR");

                    double segSocial = brutoMesPro * porcentajes[0] / 100;
                    double desempleo = brutoMesPro * porcentajes[1] / 100;
                    double formacion = brutoMesPro * porcentajes[2] / 100;
                    double descIRPF = 0;
                    double importeIRPF;
                    Iterator iter3 = set3.iterator();
                    while(iter3.hasNext()) {
                        Map.Entry<Double, Double> entry3 = (Map.Entry) iter3.next();
                        if(brutoAnual <= entry3.getKey()) {
                            descIRPF = entry3.getValue();
                            break;
                        }
                    }
                    
                    double totDeducciones, netoNomina;
                    if(proration.equals("SI")) {
                        importeIRPF = brutoMesPro * descIRPF / 100;
                    }else {
                        importeIRPF = brutoMensual * descIRPF / 100;
                    }

                    totDeducciones = segSocial + desempleo + formacion + importeIRPF;
                    netoNomina = brutoMensual - segSocial - desempleo - formacion - importeIRPF;

                    //Pagos Empresario
                    double segSocialEmp, desempleoEmp, fogasaEmp, formacionEmp, accidentesEmp;
                    double [] porcentajesEmp = new double[5];
                    //Seguridad Social
                    porcentajesEmp[0] = (double) h0.get("Contingencias comunes EMPRESARIO");
                    //Desempleo
                    porcentajesEmp[1] = (double) h0.get("Desempleo EMPRESARIO");
                    //FOGASA
                    porcentajesEmp[2] = (double) h0.get("Fogasa EMPRESARIO");
                    //Formacion
                    porcentajesEmp[3] = (double) h0.get("Formacion EMPRESARIO");
                    //Accidentes
                    porcentajesEmp[4] = (double) h0.get("Accidentes trabajo EMPRESARIO");

                    segSocialEmp = brutoMesPro * porcentajesEmp[0] / 100;
                    desempleoEmp = brutoMesPro * porcentajesEmp[1] / 100;
                    fogasaEmp = brutoMesPro * porcentajesEmp[2] / 100;
                    formacionEmp = brutoMesPro * porcentajesEmp[3] / 100;
                    accidentesEmp = brutoMesPro * porcentajesEmp[4] / 100;

                    double totalEmp = segSocialEmp + desempleoEmp + fogasaEmp + formacionEmp + accidentesEmp;
                    double costeTotal = totalEmp + brutoMensual;
                    
                    /////////////////////////
                    /////ACTUALIZAR BBDD/////
                    /////////////////////////
                    
                    if(!nif.equals(" ")){
                        //CATEGORIASBBDD
                        String HQL = "SELECT c FROM Categorias c WHERE c.nombreCategoria=:param1";
                        Query query = session.createQuery(HQL);
                        query.setParameter("param1", category);
                        List<Categorias> resultado = query.list();
                        Categorias categoriabbdd = null;
                        if(resultado.isEmpty()){
                            categoriabbdd = new Categorias();
                        }else{
                            categoriabbdd = resultado.get(0);
                        }
                        
                        categoriabbdd.setComplementoCategoria(comp);
                        categoriabbdd.setNombreCategoria(category);
                        categoriabbdd.setSalarioBaseCategoria(base);
                        session.saveOrUpdate(categoriabbdd);
                        
                        //EMPRESASBBDD
                        HQL = "SELECT e FROM Empresas e WHERE e.cif=:param1";
                        query = session.createQuery(HQL);
                        query.setParameter("param1", cif);
                        List<Empresas> resultadoEmpresas = query.list();
                        Empresas empresabbdd = null;
                        if(resultadoEmpresas.isEmpty()){
                            empresabbdd = new Empresas();
                        }else{
                            empresabbdd = resultadoEmpresas.get(0);
                        }
                        empresabbdd.setNombre(company);
                        empresabbdd.setCif(cif);
                        session.saveOrUpdate(empresabbdd);
              
                        //TRABAJADORBBDD
                        HQL = "SELECT t FROM Trabajadorbbdd t WHERE (t.nombre=:param1 AND t.nifnie=:param2 AND t.fechaAlta=:param3)";
                        query = session.createQuery(HQL);
                        query.setParameter("param1", name);
                        query.setParameter("param2", nif);
                        query.setParameter("param3", formatter.parse(entryDate));
                        
                        List<Trabajadorbbdd> resultadoTrabajador = query.list();
                        Trabajadorbbdd trabajadorbbdd;
                        if(resultadoTrabajador.isEmpty()){
                            trabajadorbbdd = new Trabajadorbbdd();
                        }else{
                            trabajadorbbdd = resultadoTrabajador.get(0);
                        }
                        trabajadorbbdd.setApellido1(surName1);
                        trabajadorbbdd.setApellido2(surName2);
                        trabajadorbbdd.setCodigoCuenta(ccc);
                        trabajadorbbdd.setEmail(email);
                        Date dt = formatter.parse(entryDate);
                        trabajadorbbdd.setFechaAlta(dt);
                        trabajadorbbdd.setIban(IBAN);
                        trabajadorbbdd.setNifnie(nif);
                        trabajadorbbdd.setNombre(name);
                        trabajadorbbdd.setCategorias(categoriabbdd);
                        trabajadorbbdd.setEmpresas(empresabbdd);

                        session.saveOrUpdate(trabajadorbbdd);

                        HQL = "SELECT n FROM Nomina n WHERE (n.mes=:param1 AND n.anio=:param2 AND n.trabajadorbbdd=:param3 AND n.brutoNomina=:param4 AND n.liquidoNomina=:param5)";
                        query = session.createQuery(HQL);
                        query.setParameter("param1", month);
                        query.setParameter("param2", year);
                        query.setParameter("param3", trabajadorbbdd);
                        query.setParameter("param4", brutoMensual);
                        query.setParameter("param5", netoNomina);
                        List<Nomina> resultadoNomina = query.list();
                        Nomina nominabbdd;
                        if(resultadoNomina.isEmpty()){
                            nominabbdd = new Nomina();
                        }else{
                            nominabbdd = resultadoNomina.get(0);
                        }
                        nominabbdd.setAccidentesTrabajoEmpresario(porcentajesEmp[4]);
                        nominabbdd.setAnio(year);
                        nominabbdd.setBaseEmpresario(brutoMesPro);
                        nominabbdd.setBrutoAnual(brutoAnual);
                        nominabbdd.setBrutoNomina(brutoMensual);
                        nominabbdd.setCosteTotalEmpresario(totalEmp);
                        nominabbdd.setDesempleoEmpresario(porcentajesEmp[1]);
                        nominabbdd.setDesempleoTrabajador(porcentajes[1]);
                        nominabbdd.setFogasaempresario(porcentajesEmp[2]);
                        nominabbdd.setFormacionEmpresario(porcentajesEmp[3]);
                        nominabbdd.setFormacionTrabajador(porcentajes[2]);
                        nominabbdd.setIrpf(descIRPF);
                        nominabbdd.setImporteSeguridadSocialEmpresario(segSocialEmp);
                        nominabbdd.setImporteTrienios(antMes);
                        nominabbdd.setImporteSalarioMes(baseMes);
                        nominabbdd.setImporteAccidentesTrabajoEmpresario(accidentesEmp);
                        nominabbdd.setImporteComplementoMes(compMes);
                        nominabbdd.setImporteDesempleoEmpresario(desempleoEmp);
                        nominabbdd.setImporteDesempleoTrabajador(desempleo);
                        nominabbdd.setImporteFogasaempresario(fogasaEmp);
                        nominabbdd.setImporteFormacionEmpresario(formacionEmp);
                        nominabbdd.setImporteFormacionTrabajador(formacion);
                        nominabbdd.setImporteSeguridadSocialTrabajador(segSocial);
                        nominabbdd.setImporteIrpf(importeIRPF);
                        nominabbdd.setLiquidoNomina(netoNomina);
                        nominabbdd.setMes(month);
                        nominabbdd.setNumeroTrienios(trienio);
                        nominabbdd.setSeguridadSocialEmpresario(porcentajesEmp[0]);
                        nominabbdd.setSeguridadSocialTrabajador(porcentajes[0]);
                        nominabbdd.setTrabajadorbbdd(trabajadorbbdd);
                        nominabbdd.setValorProrrateo(prorrateoMes);

                        createNominaPdf(nominabbdd, false);
                        nominas.add(nominabbdd);
                        indexNominas.add(entry.getKey());
                        extras.add("N");

                        session.saveOrUpdate(nominabbdd);
                    
                        if(proration.equals("NO") && ((month == 6) || (month == 12))) {
                            if(yearEmployee == year) {
                                double proportion = ((double) (month - monthEmployee)) / 6;
                                if(proportion > 6) {
                                    proportion = 6;
                                }
                                brutoMensual = brutoMensual * proportion;
                                baseMes = baseMes * proportion;
                                compMes = compMes * proportion;
                                antMes = antMes * proportion;
                            }
                            importeIRPF = brutoMensual * descIRPF / 100;
                            totDeducciones = importeIRPF;
                            netoNomina = brutoMensual - importeIRPF;

                            /////////////////////////
                            /////ACTUALIZAR BBDD/////
                            /////////////////////////
                            
                            HQL = "SELECT n FROM Nomina n WHERE (n.mes=:param1 AND n.anio=:param2 AND n.trabajadorbbdd=:param3 AND n.brutoNomina=:param4 AND n.liquidoNomina=:param5)";
                            query = session.createQuery(HQL);
                            query.setParameter("param1", month);
                            query.setParameter("param2", year);
                            query.setParameter("param3", trabajadorbbdd);
                            query.setParameter("param4", brutoMensual);
                            query.setParameter("param5", netoNomina);
                            resultadoNomina = query.list();
                            Nomina nominaExtrabbdd;
                            if(resultadoNomina.isEmpty()){
                                nominaExtrabbdd = new Nomina();
                            }else{
                                nominaExtrabbdd = resultadoNomina.get(0);
                            }
                            nominaExtrabbdd.setAccidentesTrabajoEmpresario(porcentajesEmp[4]);
                            nominaExtrabbdd.setAnio(year);
                            nominaExtrabbdd.setBaseEmpresario(0.0);
                            nominaExtrabbdd.setBrutoAnual(brutoAnual);
                            nominaExtrabbdd.setBrutoNomina(brutoMensual);
                            nominaExtrabbdd.setCosteTotalEmpresario(0.0);
                            nominaExtrabbdd.setDesempleoEmpresario(porcentajesEmp[1]);
                            nominaExtrabbdd.setDesempleoTrabajador(porcentajes[1]);
                            nominaExtrabbdd.setFogasaempresario(porcentajesEmp[2]);
                            nominaExtrabbdd.setFormacionEmpresario(porcentajesEmp[3]);
                            nominaExtrabbdd.setFormacionTrabajador(porcentajes[2]);
                            nominaExtrabbdd.setIrpf(descIRPF);
                            nominaExtrabbdd.setImporteSeguridadSocialEmpresario(0.0);
                            nominaExtrabbdd.setImporteTrienios(antMes);
                            nominaExtrabbdd.setImporteSalarioMes(baseMes);
                            nominaExtrabbdd.setImporteAccidentesTrabajoEmpresario(0.0);
                            nominaExtrabbdd.setImporteComplementoMes(compMes);
                            nominaExtrabbdd.setImporteDesempleoEmpresario(0.0);
                            nominaExtrabbdd.setImporteDesempleoTrabajador(0.0);
                            nominaExtrabbdd.setImporteFogasaempresario(0.0);
                            nominaExtrabbdd.setImporteFormacionEmpresario(0.0);
                            nominaExtrabbdd.setImporteFormacionTrabajador(0.0);
                            nominaExtrabbdd.setImporteSeguridadSocialTrabajador(0.0);
                            nominaExtrabbdd.setImporteIrpf(importeIRPF);
                            nominaExtrabbdd.setLiquidoNomina(netoNomina);
                            nominaExtrabbdd.setMes(month);
                            nominaExtrabbdd.setNumeroTrienios(trienio);
                            nominaExtrabbdd.setSeguridadSocialEmpresario(porcentajesEmp[0]);
                            nominaExtrabbdd.setSeguridadSocialTrabajador(porcentajes[0]);
                            nominaExtrabbdd.setTrabajadorbbdd(trabajadorbbdd);
                            nominaExtrabbdd.setValorProrrateo(0.0);

                            createNominaPdf(nominaExtrabbdd, true);

                            nominas.add(nominaExtrabbdd);
                            indexNominas.add(entry.getKey());
                            extras.add("S");

                            session.saveOrUpdate(nominaExtrabbdd);
                        }
                    }
                }
                } catch (ParseException ex1) {
                    Logger.getLogger(SIS_2.class.getName()).log(Level.SEVERE, null, ex1);
                }
        }
        
        tx.commit();
        HibernateUtil.shutdown();
        
        createXML(duplicates, 0);
        createXML(errorsCCC, 1);
        createXMLNominas(nominas, indexNominas, extras, date);
        
        ex.saveChanges();
    }
    
    
    private static void createNominaPdf(Nomina nomina, boolean isExtra) {
        PdfWriter writer;
        SimpleDateFormat newFormatter = new SimpleDateFormat("dd/MM/yyyy");
        
        Trabajadorbbdd t = nomina.getTrabajadorbbdd();
        Empresas e = t.getEmpresas();
        
        DecimalFormat df = new DecimalFormat("00.00");
        
        String m = new DateFormatSymbols().getMonths()[nomina.getMes()-1];
        String month = m.substring(0,1).toUpperCase() + m.substring(1);
        StringBuilder path = new StringBuilder("resources/nominas/"+ t.getNifnie() + t.getNombre() + t.getApellido1() + t.getApellido2() + month + nomina.getAnio());
        if(isExtra) {
            path.append("EXTRA");
        }
        path.append(".pdf");
        try {
            writer = new PdfWriter(path.toString().trim());
            PdfDocument pdfDoc = new PdfDocument(writer);
            com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdfDoc, PageSize.LETTER);

            Style styleCell = new Style().setBorder(Border.NO_BORDER).setFontSize(10f);
            Style stylePar = new Style().setPaddingBottom(3);

            Paragraph empty = new Paragraph("");
            Table tabla1 = new Table(2);
            tabla1.setWidth(520);

            Paragraph nom = new Paragraph(e.getNombre());
            Paragraph cifPdf = new Paragraph("CIF: "+ e.getCif());

            Paragraph dir1 = new Paragraph("Avenida de la facultad - 6");
            Paragraph dir2 = new Paragraph("24001 León");

            Cell cell1 = new Cell();
            cell1.setBorder(new SolidBorder(1));
            cell1.setWidth(200);
            cell1.setTextAlignment(TextAlignment.CENTER);
            cell1.setVerticalAlignment(VerticalAlignment.MIDDLE);

            cell1.add(nom);
            cell1.add(cifPdf);
            cell1.add(dir1);
            cell1.add(dir2);
            tabla1.addCell(cell1);

            Cell cell2 = new Cell();
            cell2.setBorder(Border.NO_BORDER);
            cell2.setPadding(10);
            cell2.setTextAlignment(TextAlignment.RIGHT);
            cell2.add(new Paragraph("IBAN: "+ t.getIban()));
            cell2.add(new Paragraph("Bruto anual: "+ df.format(nomina.getBrutoAnual())));
            cell2.add(new Paragraph("Categoría: "+ t.getCategorias().getNombreCategoria()));
            cell2.add(new Paragraph("Fecha de alta: "+ newFormatter.format(t.getFechaAlta())));
            tabla1.addCell(cell2);


            Table tabla2 = new Table(2);
            tabla2.setWidth(520);
            Image img = new Image(ImageDataFactory.create(imagen));
            img.setBorder(Border.NO_BORDER);
            img.setPadding(10);

            Cell cell3 = new Cell();
            cell3.add(img);
            cell3.setBorder(Border.NO_BORDER);
            cell3.setPaddingLeft(23);
            cell3.setPaddingTop(20);
            cell3.setWidth(250);

            tabla2.addCell(cell3);

            Cell c4 = new Cell();
            c4.setBorder(new SolidBorder(1));
            c4.setPadding(10);
            c4.setTextAlignment(TextAlignment.RIGHT);
            c4.add(new Paragraph("Destinatario:").setBold().setTextAlignment(TextAlignment.LEFT));
            c4.add(new Paragraph(t.getNombre() +" "+ t.getApellido1() +" "+ t.getApellido2()));
            c4.add(new Paragraph("DNI: "+ t.getNifnie()));
            c4.add(new Paragraph("Avenida de la facultad"));
            c4.add(new Paragraph("24001 León"));

            tabla2.addCell(c4);

            Cell c5 = new Cell();
            Paragraph p1;
            if(isExtra) {
                p1 = new Paragraph("Nómina: Extra de "+ month + " de " + nomina.getAnio());
            }else {
                p1 = new Paragraph("Nómina: "+ month + " de " + nomina.getAnio());
            }
            c5.add(p1.setTextAlignment(TextAlignment.CENTER).setItalic().setBold());
            c5.setPaddingTop(20);

            Table tabla3 = new Table(1);
            Cell c6 = new Cell();
            c6.setBorder(Border.NO_BORDER);
            c6.setWidth(520);
            tabla3.addCell(c6);


            //Primera fila
            Table t4 = new Table(5);
            t4.setWidth(520);
            t4.setBorderTop(new SolidBorder(1));
            t4.setFontSize(10f);

            t4.addCell(new Cell().add(new Paragraph("Conceptos")).setWidth(140).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(1)).setFontSize(12f));
            t4.addCell(new Cell().add(new Paragraph("Cantidad")).setWidth(100).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(1)).setTextAlignment(TextAlignment.CENTER).setFontSize(12f));
            t4.addCell(new Cell().add(new Paragraph("Imp. Unitario")).setWidth(90).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(1)).setTextAlignment(TextAlignment.CENTER).setFontSize(12f));
            t4.addCell(new Cell().add(new Paragraph("Devengo")).setWidth(85).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(1)).setTextAlignment(TextAlignment.CENTER).setFontSize(12f));
            t4.addCell(new Cell().add(new Paragraph("Deducción")).setWidth(75).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(1)).setTextAlignment(TextAlignment.RIGHT).setFontSize(12f));


            //Primera columna
            Cell c7 = new Cell();
            c7.addStyle(styleCell);
            c7.setWidth(140);
            c7.add(new Paragraph("Salario base").addStyle(stylePar));
            c7.add(new Paragraph("Prorrateo").addStyle(stylePar));
            c7.add(new Paragraph("Complemento").addStyle(stylePar));
            c7.add(new Paragraph("Antigüedad").addStyle(stylePar));
            c7.add(new Paragraph("Contigencias generales").addStyle(stylePar));
            c7.add(new Paragraph("Desempleo").addStyle(stylePar));
            c7.add(new Paragraph("Cuota formación").addStyle(stylePar));
            c7.add(new Paragraph("IRPF").addStyle(stylePar));
            t4.addCell(c7);

            //Segunda columna
            Cell c8 = new Cell();
            c8.addStyle(styleCell);
            c8.setWidth(110);
            c8.setTextAlignment(TextAlignment.CENTER);
            c8.add(new Paragraph("30 días").addStyle(stylePar));
            c8.add(new Paragraph("30 días").addStyle(stylePar));
            c8.add(new Paragraph("30 días").addStyle(stylePar));
            c8.add(new Paragraph(nomina.getNumeroTrienios() + "Trienios").addStyle(stylePar));
            c8.add(new Paragraph(df.format(nomina.getSeguridadSocialTrabajador()) +"% de "+ df.format(nomina.getBaseEmpresario())).addStyle(stylePar));
            c8.add(new Paragraph(df.format(nomina.getDesempleoTrabajador()) +"% de "+ df.format(nomina.getBaseEmpresario())).addStyle(stylePar));
            c8.add(new Paragraph(df.format(nomina.getFormacionTrabajador()) +"% de "+ df.format(nomina.getBaseEmpresario())).addStyle(stylePar));
            c8.add(new Paragraph(df.format(nomina.getIrpf()) +"% de "+ df.format(nomina.getBrutoNomina())).addStyle(stylePar));
            t4.addCell(c8);


            //Tercera columna
            Cell c9 = new Cell();
            c9.addStyle(styleCell);
            c9.setWidth(90);
            c9.setTextAlignment(TextAlignment.CENTER);
            c9.add(new Paragraph(df.format(nomina.getImporteSalarioMes()/30)).addStyle(stylePar));
            c9.add(new Paragraph(df.format(nomina.getValorProrrateo()/30)).addStyle(stylePar));
            c9.add(new Paragraph(df.format(nomina.getImporteComplementoMes()/30)).addStyle(stylePar));
            c9.add(new Paragraph(df.format(nomina.getImporteTrienios()/nomina.getNumeroTrienios())).addStyle(stylePar));
            t4.addCell(c9);           

            //Cuarta columna
            Cell c10 = new Cell();
            c10.addStyle(styleCell);
            c10.setWidth(85);
            c10.setTextAlignment(TextAlignment.RIGHT);
            c10.add(new Paragraph(df.format(nomina.getImporteSalarioMes())).addStyle(stylePar));
            c10.add(new Paragraph(df.format(nomina.getValorProrrateo())).addStyle(stylePar));
            c10.add(new Paragraph(df.format(nomina.getImporteComplementoMes())).addStyle(stylePar));
            c10.add(new Paragraph(df.format(nomina.getImporteTrienios())).addStyle(stylePar));
            t4.addCell(c10);

            //Quinta columna
            Cell c11 = new Cell();
            c11.addStyle(styleCell);
            c11.setWidth(85);
            c11.setTextAlignment(TextAlignment.RIGHT);
            c11.setVerticalAlignment(VerticalAlignment.BOTTOM);
            c11.add(new Paragraph(df.format(nomina.getImporteSeguridadSocialTrabajador())).addStyle(stylePar));
            c11.add(new Paragraph(df.format(nomina.getImporteDesempleoTrabajador())).addStyle(stylePar));
            c11.add(new Paragraph(df.format(nomina.getImporteFormacionTrabajador())).addStyle(stylePar));
            c11.add(new Paragraph(df.format(nomina.getImporteIrpf())).addStyle(stylePar));
            t4.addCell(c11); 

            //Fila abajo
            Cell c12 = new Cell();
            c12.setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(1)).setFontSize(10);
            c12.add(new Paragraph("Total deducciones"));
            c12.add(new Paragraph("Total devengos"));
            t4.addCell(c12);

            //Celdas vacias
            t4.addCell(new Cell().setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(1)));
            t4.addCell(new Cell().setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(1)));

            Cell c13 = new Cell();
            c13.setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(1)).setFontSize(10).setVerticalAlignment(VerticalAlignment.BOTTOM).setTextAlignment(TextAlignment.RIGHT);
            c13.add(new Paragraph(df.format(nomina.getBrutoNomina())));
            t4.addCell(c13);

            Cell c14 = new Cell();
            c14.setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(1)).setFontSize(10).setTextAlignment(TextAlignment.RIGHT);
            c14.add(new Paragraph(df.format(nomina.getBrutoNomina() - nomina.getLiquidoNomina())));
            t4.addCell(c14);

            //Final fila de abajp                            
            t4.addCell(new Cell().setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(1)));
            t4.addCell(new Cell().setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(1)));
            t4.addCell(new Cell().add(new Paragraph("Líquido a percibir")).setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(1)).setTextAlignment(TextAlignment.RIGHT));
            t4.addCell(new Cell().setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(1)));
            t4.addCell(new Cell().add(new Paragraph(df.format(nomina.getLiquidoNomina()))).setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(1)).setTextAlignment(TextAlignment.RIGHT));

            c6.add(t4);

            //////////////////////
            ////Segunda tabla////
            /////////////////////
            Cell c15 = new Cell();
            c15.setBorder(Border.NO_BORDER);
            c15.setWidth(520);
            c15.setFontColor(ColorConstants.GRAY);
            tabla3.addCell(c15);
            c15.setPaddingTop(15);


            //Primera fila
            Table t5 = new Table(2);
            t5.setWidth(520);
            t5.setBorderTop(new SolidBorder(ColorConstants.GRAY, 1));
            t5.setFontSize(10f);


            t5.addCell(new Cell().add(new Paragraph("Cálculo empresario: BASE")).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.GRAY, 1)).setTextAlignment(TextAlignment.LEFT));
            t5.addCell(new Cell().add(new Paragraph(df.format(nomina.getBaseEmpresario()))).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.GRAY, 1)).setTextAlignment(TextAlignment.RIGHT));

            //Primera columna
            Cell c16 = new Cell();
            c16.addStyle(styleCell);
            c16.setTextAlignment(TextAlignment.LEFT);
            c16.add(new Paragraph("Contigencias comunes empresario "+ df.format(nomina.getSeguridadSocialEmpresario()) + "%").addStyle(stylePar));
            c16.add(new Paragraph("Desempleo "+ df.format(nomina.getDesempleoEmpresario()) + "%").addStyle(stylePar));
            c16.add(new Paragraph("Formación "+ df.format(nomina.getFormacionEmpresario()) + "%").addStyle(stylePar));
            c16.add(new Paragraph("Accidentes de trabajo "+ df.format(nomina.getAccidentesTrabajoEmpresario()) + "%").addStyle(stylePar));
            c16.add(new Paragraph("FOGASA "+ df.format(nomina.getFogasaempresario()) + "%").addStyle(stylePar));
            t5.addCell(c16);


            //Segunda columna
            Cell c17 = new Cell();
            c17.addStyle(styleCell);
            c17.setTextAlignment(TextAlignment.RIGHT);
            c17.add(new Paragraph(df.format(nomina.getImporteSeguridadSocialEmpresario())).addStyle(stylePar));
            c17.add(new Paragraph(df.format(nomina.getImporteDesempleoEmpresario())).addStyle(stylePar));
            c17.add(new Paragraph(df.format(nomina.getImporteFormacionEmpresario())).addStyle(stylePar));
            c17.add(new Paragraph(df.format(nomina.getImporteAccidentesTrabajoEmpresario())).addStyle(stylePar));
            c17.add(new Paragraph(df.format(nomina.getImporteFogasaempresario())).addStyle(stylePar));
            t5.addCell(c17); 

            //Final fila de abajo
            t5.addCell(new Cell().add(new Paragraph("Total empresario")).setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(ColorConstants.GRAY, 1)).setTextAlignment(TextAlignment.LEFT));
            t5.addCell(new Cell().add(new Paragraph(df.format(nomina.getCosteTotalEmpresario()))).setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(ColorConstants.GRAY, 1)).setTextAlignment(TextAlignment.RIGHT));

            c15.add(t5);

            Cell c18 = new Cell();
            c18.setPaddingTop(20);
            c18.setWidth(520);
            c18.setBorder(Border.NO_BORDER);

            Table t6 = new Table(2);
            t6.setBorder(new SolidBorder(3));
            t6.setWidth(520);

            t6.setFontColor(ColorConstants.RED);
            t6.addCell(new Cell().add(new Paragraph("COSTE TOTAL TRABAJADOR:")).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.LEFT));
            t6.addCell(new Cell().add(new Paragraph(df.format(nomina.getCosteTotalEmpresario() + nomina.getBrutoNomina()))).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));

            c18.add(t6);
            tabla3.addCell(c18);

            doc.add(tabla1);
            doc.add(tabla2);
            doc.add(c5);
            doc.add(tabla3);
            doc.close();
        } catch (FileNotFoundException | MalformedURLException ex1) {
            Logger.getLogger(SIS_2.class.getName()).log(Level.SEVERE, null, ex1);
        }
    }

    
    private static void createXML(List<Map.Entry> errors, int type) {

        List<String> elements = new ArrayList<>();
        List<Integer> index = new ArrayList<>();
        String fileName;
        switch(type) {
            //NIF
            case 0:
                fileName = "resources/errores.xml";
                    
                elements.add("Trabajadores");
                elements.add("Trabajador");
                elements.add("id");
                elements.add("Nombre");
                elements.add("PrimerApellido");
                elements.add("SegundoApellido");
                elements.add("Empresa");
                elements.add("Categoria");

                index.add(1);
                index.add(2);
                index.add(3);
                index.add(5);
                index.add(7);
                break;
            //CCC
            case 1:
                fileName = "resources/ErroresCCC.xml";
                
                elements.add("Cuentas");
                elements.add("Cuenta");
                elements.add("id");
                elements.add("Nombre");
                elements.add("Apellidos");
                elements.add("Empresa");
                elements.add("CCCErroneo");
                elements.add("IBANCorrecto");

                index.add(1);
                index.add(2);
                index.add(5);
                index.add(9);
                index.add(11);
                index.add(3);
                break;
            default:
                fileName = null;
                break;
        }
        
        try {
            
            DocumentBuilderFactory dF = DocumentBuilderFactory.newInstance();
            DocumentBuilder dB = dF.newDocumentBuilder();
            Document doc = dB.newDocument();

            Element rootNode = doc.createElement(elements.get(0));
            doc.appendChild(rootNode);

            for(Map.Entry<Integer, List<String>> entry : errors) {
                List<String> row = entry.getValue();

                Element trabajador = doc.createElement(elements.get(1));
                rootNode.appendChild(trabajador);

                Attr attr = doc.createAttribute(elements.get(2));
                attr.setValue(String.valueOf(entry.getKey()));
                trabajador.setAttributeNode(attr);

                for(int i=3; i<elements.size(); i++) {
                    if(!(type == 1 && i == 5)){
                        String text;
                        Element e = doc.createElement(elements.get(i));
                        text = row.get(index.get(i-3));
                        if(type == 1 && i == 4) {
                            text = text + " " + row.get(index.get(index.size()-1));
                        }
                        e.appendChild(doc.createTextNode(text));
                        trabajador.appendChild(e);
                    }
                }
            }
        
            TransformerFactory tF = TransformerFactory.newInstance();
            Transformer transformer  = tF.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        
            DOMSource dSource = new DOMSource(doc);
            StreamResult sRes = new StreamResult(new File(fileName));
            
            transformer.transform(dSource, sRes);
        
        } catch (ParserConfigurationException | TransformerException | DOMException e) {
        }
    }

    private static void createXMLNominas(List<Nomina> nominas, List<Integer> indexNominas, List<String> extras, String date ) {
        List<String> elements = new ArrayList<>();
        List<Integer> index = new ArrayList<>();
        String fileName = "resources/Nominas.xml";
        
        try {
            DocumentBuilderFactory dF = DocumentBuilderFactory.newInstance();
            DocumentBuilder dB = dF.newDocumentBuilder();
            Document doc = dB.newDocument();

            Element rootNode = doc.createElement("Nominas");
            rootNode.setAttribute("fechaNomina", date);
            doc.appendChild(rootNode);

            for(int i=0; i<nominas.size(); i++) {
                Trabajadorbbdd t = nominas.get(i).getTrabajadorbbdd();
                
                
                Element nomina = doc.createElement("Nomina");
                rootNode.appendChild(nomina);

                Attr attr = doc.createAttribute("idNomina");
                attr.setValue(String.valueOf(nominas.get(i).getIdNomina()));
                nomina.setAttributeNode(attr);
                
                Element extra = doc.createElement("Extra");
                extra.appendChild(doc.createTextNode(extras.get(i)));
                nomina.appendChild(extra);
                
                Element idExcel = doc.createElement("idFilaExcel");
                idExcel.appendChild(doc.createTextNode(indexNominas.get(i).toString()));
                nomina.appendChild(idExcel);
                
                Element nombre = doc.createElement("Nombre");
                nombre.appendChild(doc.createTextNode(t.getNombre()));
                nomina.appendChild(nombre);
                
                Element nif = doc.createElement("NIF");
                nif.appendChild(doc.createTextNode(t.getNifnie()));
                nomina.appendChild(nif);
                
                Element iban = doc.createElement("IBAN");
                iban.appendChild(doc.createTextNode(t.getIban()));
                nomina.appendChild(iban);
                
                Element cat = doc.createElement("Categoría");
                cat.appendChild(doc.createTextNode(t.getCategorias().getNombreCategoria()));
                nomina.appendChild(cat);
                
                Element brutoAnual = doc.createElement("BrutoAnual");
                brutoAnual.appendChild(doc.createTextNode(nominas.get(i).getBrutoAnual().toString()));
                nomina.appendChild(brutoAnual);
                
                Element irpf = doc.createElement("ImporteIRPF");
                irpf.appendChild(doc.createTextNode(nominas.get(i).getImporteIrpf().toString()));
                nomina.appendChild(irpf);
                
                Element baseEmp = doc.createElement("BaseEmpresario");
                baseEmp.appendChild(doc.createTextNode(nominas.get(i).getBaseEmpresario().toString()));
                nomina.appendChild(baseEmp);
                
                Element brutoNom = doc.createElement("BrutoNomina");
                brutoNom.appendChild(doc.createTextNode(nominas.get(i).getBrutoNomina().toString()));
                nomina.appendChild(brutoNom);
                
                Element neto = doc.createElement("LiquidoNomina");
                neto.appendChild(doc.createTextNode(nominas.get(i).getLiquidoNomina().toString()));
                nomina.appendChild(neto);
                
                Element totalEmp = doc.createElement("CosteTotalEmprsario");
                totalEmp.appendChild(doc.createTextNode(nominas.get(i).getCosteTotalEmpresario().toString()));
                nomina.appendChild(totalEmp);                
            }
        
            TransformerFactory tF = TransformerFactory.newInstance();
            Transformer transformer  = tF.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        
            DOMSource dSource = new DOMSource(doc);
            StreamResult sRes = new StreamResult(new File(fileName));
            
            transformer.transform(dSource, sRes);
        
        } catch (ParserConfigurationException | TransformerException | DOMException e) {
        }
    }
}
