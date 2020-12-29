package com.leoao.netmonitor


class NetMonitorUtils {

    public static boolean isAndroidGenerated(String className) {
        //entryName.endsWith(".class") && !entryName.startsWith("R\$") && !"R.class".equals(entryName) && !"BuildConfig.class".equals(entryName)
        return className.contains('R$') ||
                className.contains('R2$') ||
                className.contains('R.class') ||
                className.contains('R2.class') ||
                className.contains('BuildConfig.class')
    }

    static boolean isOtherFile(String entryName) {
        return entryName.contains("META-INF") ||
                entryName.endsWith(".DSA") ||
                entryName.endsWith(".SF") ||
                entryName.endsWith(".gz") ||
                entryName.endsWith(".xml") ||
                entryName.endsWith(".properties") ||
                entryName.endsWith(".MF")
    }
}