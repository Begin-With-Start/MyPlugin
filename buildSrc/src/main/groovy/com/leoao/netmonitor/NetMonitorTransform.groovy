package com.leoao.netmonitor

import aj.org.objectweb.asm.ClassWriter
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.google.common.collect.ImmutableSet
import jdk.internal.org.objectweb.asm.ClassVisitor
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class NetMonitorTransform extends Transform {

    Project project;

    public NetMonitorTransform(Project project) {
        this.project = project;
    }

    @Override
    String getName() {
        return "netmonitor-transform"
    }

    //transform需要扫描的类型
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        // 输入类型，可以使class文件，也可以是源码文件 ，这是表示输入的class文件   RESOURCES:java 标准文件
        return ImmutableSet.<QualifiedContent.ContentType> of(QualifiedContent.DefaultContentType.CLASSES)
    }

    //transform需要扫描的范围 SCOPE_FULL_PROJECT:整个工程，扫描范围按照业务进行定义
    /**
     * Scope类型	说明
     * PROJECT	只处理当前的项目
     * SUB_PROJECTS	只处理子项目
     * EXTERNAL_LIBRARIES	只处理外部的依赖库
     * TESTED_CODE	只处理测试代码
     * PROVIDED_ONLY	只处理provided-only的依赖库
     * @return
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    //transform是否支持增量编译 增量编译方面设置
    @Override
    boolean isIncremental() {
        return false
    }


    /**
     * 根据以上重写方法的指定条件真正扫描工程class文件的方法。其中主要的2个参数：
     * Collection inputs参数：
     * 输入流参数，根据该参数可以获取输入流的路径、jar包及其内在的class文件
     * TransformOutputProvider outputProvider参数：
     * 输出流参数，根据该参数可以获取输出流路径，把经过改造后的class文件拷贝到该路径，最终打包完成的apk将执行你该路径下的class文件
     * @param context
     * @param inputs
     * @param referencedInputs
     * @param outputProvider
     * @param isIncremental
     * @throws IOException* @throws TransformException* @throws InterruptedException
     */
    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        def statrtTime = System.currentTimeMillis()
        NetMonitorLoger.printLine("开始代码扫描")
        //进行类 jar class的处理；
        if (isIncremental) {
            NetMonitorLoger.printLine("代码扫描完成 增量编译 耗时： " + endTime + "s")
        } else {
            if (outputProvider != null) {
                outputProvider.deleteAll() //之前输出目录不为空直接删除所有；
            }

            inputs.each {
                TransformInput transformInput ->
//                    directoryInputs为文件夹中的class文件
                    transformInput.directoryInputs.each {
                        DirectoryInput directoryInput ->
                            handleDirectoryInput(directoryInput, outputProvider)

                    }
//                    jarInputs为jar包中的class文件
                    transformInput.jarInputs.each {
                        JarInput jarInput ->
                            handleJarInput(context, jarInput, outputProvider)
                    }


            }

            def endTime = (System.currentTimeMillis() - statrtTime) / 1000
            NetMonitorLoger.printLine("代码扫描完成 非增量编译 耗时： " + endTime + "s")
        }

    }


    //处理目录中的class 目标文件
    static handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) {
        if (directoryInput.file.isDirectory()) {
            directoryInput.file.eachFileRecurse {
                File file ->
                    def name = file.name
                    if (!NetMonitorUtils.isAndroidGenerated(name)) {
                        String classPath = dealPath(directoryInput, file)

                        if (NetMonitorClassFilter.filterClass(classPath, file)) {
                        } else {
                            NetMonitorLoger.printLine("class 文件中  " + classPath.substring(1, classPath.length()))
                        }
                    }


                    //一般性处理，不论是否进行类的匹配都需要对文件进行拷贝到目标文件；
                    File dest = outputProvider.getContentLocation(directoryInput.getName(),
                            directoryInput.getContentTypes(),
                            directoryInput.getScopes(),
                            Format.DIRECTORY)
                    try {
                        FileUtils.copyDirectory(directoryInput.getFile(), dest)
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }

        }
    }


    //处理jar文件中的目标文件；
    static handleJarInput(Context context, JarInput jarInputs, TransformOutputProvider outputProvider) {
        //单个jar文件循环；
        jarInputs.each(
                { jarInput ->
                    def name = jarInput.getName();
                    if (!NetMonitorUtils.isAndroidGenerated(name)) {
                        NetMonitorLoger.printLine("jar文件内：  " + name) //没做什么处理，直接打印了文件名；
                    }

                    File jarInputFile = jarInput.getFile()
                    JarFile jarFile = new JarFile(jarInputFile) //原jar文件
                    def newJarPath = "" //新的jar文件目录
                    newJarPath = DigestUtils.md5Hex(jarInputFile.getAbsolutePath()).substring(0, 8) //让开老包的名字

//                    def outputJarFile = new File(context.getTemporaryDir(), newJarPath + jarInputFile.getName())
//                    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJarFile))

                    Enumeration enumeration = jarFile.entries()
                    while (enumeration.hasMoreElements()) {
                        JarEntry jarEntry = enumeration.nextElement()
//                        jarOutputStream.putNextEntry(jarEntry)

                        String entryName = replaceSeparator(jarEntry.getName())
                        //处理掉不必要的文件夹
                        if (NetMonitorClassFilter.filterClass(entryName, jarInputFile)) {
//                            NetMonitorLoger.printLine("命中目标." + entryName)
                        } else {
                            NetMonitorLoger.printLine(" entryName:  " + entryName)

//                            //执行具体的改动类的操作
//                            byte [] modifiedBytes = null
//                            byte [] sourceBytes = null
//                            sourceBytes = IOUtils.toByteArray(jarFile.getInputStream(jarEntry))
////
//                            if(entryName.contains("com.minifly.myplugin.MainActivity")){
//                                modifiedBytes = modifyclass(sourceBytes)
//                            }
//
//                            if (modifiedBytes == null) {
//                                jarOutputStream.write(sourceBytes)
//                            } else {
//                                jarOutputStream.write(modifiedBytes)
//                            }
//                            jarOutputStream.closeEntry()
                        }
                    }

                    File dest = outputProvider.getContentLocation(newJarPath,
                            jarInput.getContentTypes(),
                            jarInput.getScopes(),
                            Format.JAR);
                    try {
                        FileUtils.copyFile(jarInputFile, dest)
                    } catch (IOException e) {
                        e.printStackTrace()
                    }
//                    jarOutputStream.close()
                    jarFile.close()
                })
    }

    //asm 改动类
    private static byte [] modifyclass(byte [] sourceBytes){
        try{
            ClassWriter classWriter  = new ClassWriter(ClassWriter.COMPUTE_MAXS)
            ClassVisitor classVisitor = new NetMonitorHookClassVisitor(0,classWriter)
            ClassReader classReader = new ClassReader(sourceBytes)
            classReader.accept(classVisitor,ClassReader.EXPAND_FRAMES)
            return classWriter.toByteArray()
        }catch(Exception ex){

            return sourceBytes
        }
    }


    private static String dealPath(DirectoryInput directoryInput, File file) {
        def absolutePath = directoryInput.file.absolutePath
        //绝对路径转化成相对文件路径；
        def path = file.getCanonicalPath().replace(absolutePath, "")
        //getCanonicalPath 规范路径名字符串表示与此抽象路径名相同的文件或目录
        //转化 / 为标准 .
        String classPath = replaceSeparator(path)
        classPath
    }

    private static String replaceSeparator(String path) {
        def classPath = path.replace(File.separator, ".")
        classPath
    }

/**
 * 打印提示信息
 */
    private void printCopyRight() {
        println()
        println("\033[40;32m" + "####################################################################" + "\033[0m")
        println("\033[40;32m" + "########                                                    ########" + "\033[0m")
        println("\033[40;32m" + "########                                                    ########" + "\033[0m")
        println("\033[40;32m" + "########                 netMonitor 编译插件                 ########" + "\033[0m")
        println("\033[40;32m" + "########                                                    ########" + "\033[0m")
        println("\033[40;32m" + "########                                                    ########" + "\033[0m")
        println("\033[40;32m" + "####################################################################" + "\033[0m")
        println()
    }

}