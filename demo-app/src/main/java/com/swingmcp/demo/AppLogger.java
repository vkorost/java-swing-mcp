package com.swingmcp.demo;

import java.awt.Color;
import java.util.Date;

public class AppLogger {

    static ResourceManager resourceManager = new ResourceManager();

    public static void info(String info) {
        System.out.println("[Info] [" + new Date() + "]  " + info);
    }

    public static void showStatus(String info) {
        resourceManager.getStatusBar().setStatusText(info);
    }

    public static void showStatus(String info, Color color) {
        resourceManager.getStatusBar().setStatusColor(color);
        resourceManager.getStatusBar().setStatusText(info);
    }

    public static void error(String error) {
        System.out.println("[ERROR] [" + new Date() + "]  " + error);
    }

    public static void error(String error, Exception e) {
        System.out.println("[ERROR] [" + new Date() + "]  " + error);
        System.out.println("___________ STACK TRACE __________________________");
        e.printStackTrace(System.out);
        System.out.println("__________________________________________________");
    }

    public static void info(int info) {
        info("" + info);
    }

    public static void info(StringBuffer info) {
        info(info.toString());
    }

    public static void info(Object info) {
        info(info.toString());
    }
}
