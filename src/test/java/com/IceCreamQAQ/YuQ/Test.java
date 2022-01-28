package com.IceCreamQAQ.YuQ;

import com.icecreamqaq.yuq.YuQ;
import com.icecreamqaq.yuq.message.At;
import com.icecreamqaq.yuq.message.Message;
import com.icecreamqaq.yuq.rainCode.RainCode;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Test {

    static class Time {
        private boolean t3;
        private Integer t1;
        private int t2;
        private String format;

        private String t4;
        private String t5;

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    public static void main(String[] args) {
        Message message = new Message();

        message.getBody().stream().filter( it -> it instanceof At).forEach(it -> ((At) it).getUser());
//        RainCode.registerRainCodeDecoder(
//                "SF",
//                "time",
//                Time.class, (item) -> YuQ.getMif().text(new SimpleDateFormat(item.getParas().getFormat()).format(new Date()))
//        );

        Arrays.stream(Time.class.getDeclaredFields()).forEach(field -> System.out.println(field.getName()));
    }
}
