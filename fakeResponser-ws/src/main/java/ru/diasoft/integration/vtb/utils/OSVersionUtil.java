package ru.diasoft.integration.vtb.utils;

public class OSVersionUtil {

    private static String OS = System.getProperty("os.name").toLowerCase();

    private static boolean isWindows() {
        return (OS.contains("win"));
    }

    static boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
    }

    public static void main(String[] args) {
        System.out.println(OS);
        if (isWindows()) {
            System.out.println("This is Windows");
        } else if (isUnix()) {
            System.out.println("This is Unix or Linux");
        } else {
            System.out.println("Your OS is not support!!");
        }
    }
}
