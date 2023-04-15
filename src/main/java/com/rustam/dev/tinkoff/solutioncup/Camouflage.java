package com.rustam.dev.tinkoff.solutioncup;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

class Camouflage {
    public static void main(String args[]) throws UnsupportedEncodingException {
        var scanner = new Scanner(System.in);
        var input = "РҐРѕС‰СѓСЏ СЋСЉРЅР¶СѓР±, Рѕ С‘РѕС€СЊС‡С‰ СЋСЉРѕС‘СѓР±.";


        byte[] bytes = input.getBytes("windows-1251");

        String utf8 = new String(bytes, StandardCharsets.UTF_8);


        String result = utf8;
        System.out.println(result);
    }
}
