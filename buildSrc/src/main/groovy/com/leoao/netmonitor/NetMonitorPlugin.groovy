package com.leoao.netmonitor

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 进行目标类、jar中class扫描
 */
class NetMonitorPlugin implements Plugin<Project> {

    //apply方法 gradle 插件入口方法
    @Override
    void apply(Project project) {
        NetMonitorLoger.printLine("NetMonitorPlugin start")
        project.repositories {
            google()
            mavenCentral()
        }


        Properties properties = new Properties()
        boolean netMonitorEnable = false
        //包含gradle文件 进行配置读取；
        if (project.rootProject.file('gradle.properties').exists()) {
            properties.load(project.rootProject.file('gradle.properties').newDataInputStream())
            netMonitorEnable = Boolean.parseBoolean(properties.getProperty('netMonitorEnable', "false"))
            NetMonitorLoger.printLine("netMonitorEnable 设置：" + netMonitorEnable)
        }

//        Object extension = project.extensions.create("netmonitor",NetMonitorExtension)
        //gradle 插件
//        NetMonitorTransform transform = new NetMonitorTransform(project)

        //获取appextension 进行transform注入到打包过程
        if (project.plugins.hasPlugin(AppPlugin)) {
            NetMonitorLoger.printLine("进入逻辑 1 ")

            AppExtension android = project.extensions.getByType(AppExtension.class)
            android.registerTransform(new NetMonitorTransform(project)) //注入到appextension；

            //打印一下主工程的依赖
//            NetMonitorLoger.printLine(project.respositores)
        }else if(project.plugins.hasPlugin(AppExtension)){
            NetMonitorLoger.printLine("进入逻辑 2 ")
        }else{
            NetMonitorLoger.printLine("进入逻辑 3")
        }

        NetMonitorLoger.printLine("NetMonitorPlugin end" + project.plugins.toString())
    }


}