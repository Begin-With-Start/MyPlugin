package  com.leoao.netmonitor


class NetMonitorUtils{

    public static boolean isAndroidGenerated(String className) {
        //entryName.endsWith(".class") && !entryName.startsWith("R\$") && !"R.class".equals(entryName) && !"BuildConfig.class".equals(entryName)
        return className.contains('R$') ||
                className.contains('R2$') ||
                className.contains('R.class') ||
                className.contains('R2.class') ||
                className.contains('BuildConfig.class')
    }
}