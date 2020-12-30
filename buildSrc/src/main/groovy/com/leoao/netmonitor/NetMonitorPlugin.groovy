package com.leoao.netmonitor

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 进行目标类、jar中class扫描
 */
class NetMonitorPlugin implements Plugin<Project> {

    //apply方法 gradle 插件入口方法
    @Override
    void apply(Project project) {
        Loger.printLogLine("NetMonitorPlugin start")
        project.repositories {
            google() //添加两个依赖；
            mavenCentral() //不必显式的进行依赖
        }


        Properties properties = new Properties()
        boolean netMonitorEnable = false
        //包含gradle文件 进行配置读取；
        if (project.rootProject.file('gradle.properties').exists()) {
            properties.load(project.rootProject.file('gradle.properties').newDataInputStream())
            netMonitorEnable = Boolean.parseBoolean(properties.getProperty('netMonitorEnable', "false"))
            Loger.printLogLine("netMonitorEnable 设置：" + netMonitorEnable)
//            assert netMonitorEnable==true //断言某些条件
        }

//        Object extension = project.extensions.create("netmonitor",NetMonitorExtension)
        //gradle 插件
//        NetMonitorTransform transform = new NetMonitorTransform(project)

        //获取appextension 进行transform注入到打包过程
        if (project.plugins.hasPlugin(AppPlugin)) {
            Loger.printLogLine("app 插件")

            AppExtension android = project.extensions.getByType(AppExtension.class)
            android.registerTransform(new NetMonitorTransform(project)) //注入到appextension；

            //打印一下主工程的依赖
//            NetMonitorLoger.printLine(project.respositores)
        }else if(project.plugins.hasPlugin(AppExtension)){
            Loger.printLogLine("有app extension ")
        }else{
            Loger.printLogLine("其他逻辑")
        }
    }


}