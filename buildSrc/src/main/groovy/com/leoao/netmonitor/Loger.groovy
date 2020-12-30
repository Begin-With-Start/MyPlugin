package com.leoao.netmonitor

class Loger {
    public static final String lineChar = "\033[40;32m" + "=====================       %s       =====================" + "\033[0m"
    public static final String logLineChar = "\033[0;30;47m" + "=====================       %s       =====================" + "\033[0m"

    static String getLineChar() {
        return lineChar
    }

    static printLine (String destStr){
        println(lineChar.replace("%s" , destStr))
    }

    static printLogLine(String destStr){
        println(logLineChar.replace("%s" , destStr))
    }
}