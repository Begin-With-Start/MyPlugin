package com.leoao.netmonitor

class NetMonitorLoger {
    public static final String lineChar = "\033[40;32m" + "=====================       %s       =====================" + "\033[0m"

    static String getLineChar() {
        return lineChar
    }

    static printLine (String destStr){
        println(lineChar.replace("%s" , destStr))
    }
}