package   com.leoao.netmonitor

class NetMonitorClassFilter{
    //对已经扫描的类进行过滤，拿到目标类即可；
    static boolean filterClass(String entryName , File file){
        if(entryName.contains("META-INF") || entryName.endsWith(".DSA") || entryName.endsWith(".SF") || entryName.startsWith("androidx") || !file.isFile() ){
            return true
        }else{
            return false
        }
    }

}