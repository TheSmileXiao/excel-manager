/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sis_2;

import java.math.BigInteger;

/**
 *
 * @author MARTIN
 */
public class Validator {
    
    public Validator() {
        
    }
  
    public String calculoIBAN(String cuenta, String pais){

        String aux = cuenta;
        
        for(int i = 0; i < 2; i++){
            int valor = (int) pais.charAt(i)-55;
            if(valor >= 10 && valor <=35){
                aux = aux + Integer.toString(valor);
            }
            else{
                System.out.println("Pais IBAN incorrecto");
            }
        }
        aux = aux + "00";
        int resto = (new BigInteger(aux).mod(new BigInteger("97")).intValue());
        int control = 98-resto;
        if(control <= 10){
            cuenta = "0" + Integer.toString(control) + cuenta;
        }else{
            cuenta = Integer.toString(control) + cuenta;
        }
//        String verific = aux.substring(0, 24) + Integer.toString(control);        
        String IBAN = pais + cuenta;

        return IBAN;
    }
  
    public String nifValid(String nif) {
        
        char c = nif.charAt(8);
        char first;
        char comp;
        String result;
        
        switch(nif.charAt(0)) {
            case 'X':
                first = '0';
                break;
            case 'Y':
                first = '1';
                break;
            case 'Z':
                first = '2';
                break;
            default:
                first = nif.charAt(0);
                break;
        }
        int num = Integer.valueOf(first + nif.substring(1, 8));
        int resto = num%23;
        
        switch(resto) {
            case 0:
                comp = 'T';
                break;
            case 1:
                comp = 'R';
                break;
            case 2:
                comp = 'W';
                break;
            case 3:
                comp = 'A';
                break;
            case 4:
                comp = 'G';
                break;
            case 5:
                comp = 'M';
                break;
            case 6:
                comp = 'Y';
                break;
            case 7:
                comp = 'F';
                break;
            case 8:
                comp = 'P';
                break;
            case 9:
                comp = 'D';
                break;
            case 10:
                comp = 'X';
                break;
            case 11:
                comp = 'B';
                break;
            case 12:
                comp = 'N';
                break;
            case 13:
                comp = 'J';
                break;
            case 14:
                comp = 'Z';
                break;
            case 15:
                comp = 'S';
                break;
            case 16:
                comp = 'Q';
                break;
            case 17:
                comp = 'V';
                break;
            case 18:
                comp = 'H';
                break;
            case 19:
                comp = 'L';
                break;
            case 20:
                comp = 'C';
                break;
            case 21:
                comp = 'K';
                break;
            case 22:
                comp = 'E';
                break;
            default:
                return null;
        }
        result = (nif.substring(0, 8) + comp);
        return result;
    }

    public int cccValid(String ccc) {
        int sum = 0;
        for(int i=0; i<10; i++) {
            sum += (int) (Math.pow(2, i) % 11) * Integer.valueOf(ccc.charAt(i));
        }
        sum = sum % 11;
        sum = 11 - sum;
        if(sum == 10) {
            return 1;
        }else if(sum == 11) {
            return 0;
        }else {
            return sum;
        }
    }
}
