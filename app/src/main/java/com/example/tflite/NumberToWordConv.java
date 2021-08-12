package com.example.tflite;

import java.util.HashMap;

public class NumberToWordConv {
    HashMap<Integer, String> map;
    public NumberToWordConv () {
        map = new HashMap<Integer, String>();
        fillMap();
    }
    public String numberToWords(int num) {
        StringBuilder sb = new StringBuilder();

        if(num==0){
            return map.get(0);
        }

        if(num >= 1000000000){
            int extra = num/1000000000;
            sb.append(convert(extra) + " میلیارد");
            num = num%1000000000;
        }

        if(num >= 1000000){
            int extra = num/1000000;
            sb.append(convert(extra) + " میلیون");
            num = num%1000000;
        }

        if(num >= 1000){
            int extra = num/1000;
            sb.append(convert(extra) + " هزار");
            num = num%1000;
        }

        if(num > 0){
            sb.append(convert(num));
        }

        return sb.toString().trim();
    }

    public String convert(int num){

        StringBuilder sb = new StringBuilder();

        if(num>=100){
            int numHundred = num/100;
            sb.append(" " +map.get(numHundred)+ " هزار");
            num=num%100;
        }

        if(num > 0){
            if(num>0 && num<=20){
                sb.append(" "+map.get(num));
            }else{
                int numTen = num/10;
                sb.append(map.get(numTen*10)+" و ");

                int numOne=num%10;
                if(numOne>0){
                    sb.append(" " + map.get(numOne));
                }
            }
        }

        return sb.toString();
    }

    public void fillMap(){
        map.put(0, "صفر");
        map.put(1, "یک");
        map.put(2, "دو");
        map.put(3, "سه");
        map.put(4, "چهار");
        map.put(5, "پنج");
        map.put(6, "شش");
        map.put(7, "هفت");
        map.put(8, "هشت");
        map.put(9, "نه");
        map.put(10, "ده");
        map.put(11, "یازده");
        map.put(12, "دوازده");
        map.put(13, "سیزده");
        map.put(14, "چهارده");
        map.put(15, "پانزده");
        map.put(16, "شانزده");
        map.put(17, "هفده");
        map.put(18, "هجده");
        map.put(19, "نوزده");
        map.put(20, "بیست");
        map.put(30, "سی");
        map.put(40, "چهل");
        map.put(50, "پنچاه");
        map.put(60, "شصت");
        map.put(70, "هفتاد");
        map.put(80, "هشتاد");
        map.put(90, "نود");
    }
}