package com.leoao.netmonitor

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.ImmutableSet
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

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
        //是否是目录
        if (directoryInput.file.isDirectory()) {
            //列出目录所有文件（包含子文件夹，子文件夹内文件）
            directoryInput.file.eachFileRecurse { File file ->
                def name = file.name
                if (!NetMonitorUtils.isAndroidGenerated(name)) {
                    String classPath = dealPath(directoryInput, file)

                    if (NetMonitorClassFilter.filterClass(classPath, file)) {

                    } else {
//                        NetMonitorLoger.logLineChar("class 文件中  " + classPath.substring(1, classPath.length()))
                        NetMonitorLoger.printLogLine("class 文件中 规则之外的文件：  " + classPath)

                    }
                }

                if (name.endsWith(".class") && !name.startsWith("R\$") && !"R.class".equals(name) && !"BuildConfig.class".equals(name)) {
                    // && "android/support/v4/app/FragmentActivity.class".equals(name)
                    NetMonitorLoger.printLogLine("class 文件中  " + dealPath(directoryInput, file))
                    ClassReader classReader = new ClassReader(file.bytes)
                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
                    ClassVisitor cv = new NetMonitorHookClassVisitor(classWriter)
                    classReader.accept(cv, ClassReader.EXPAND_FRAMES)
                    byte[] code = classWriter.toByteArray()
                    FileOutputStream fos = new FileOutputStream(
                            file.parentFile.absolutePath + File.separator + name)
                    fos.write(code)
                    fos.close()
                }
            }
        }
        //处理完输入文件之后，要把输出给下一个任务
        def dest = outputProvider.getContentLocation(directoryInput.name,
                directoryInput.contentTypes, directoryInput.scopes,
                Format.DIRECTORY)
        FileUtils.copyDirectory(directoryInput.file, dest)
    }


    //处理jar文件中的目标文件；
    static handleJarInput(Context context, JarInput jarInputs, TransformOutputProvider outputProvider) {
        //单个jar文件循环；
        jarInputs.each(
                { jarInput ->
                    if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
                        //重名名输出文件,因为可能同名,会覆盖
                        def jarName = jarInput.name
                        def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                        if (jarName.endsWith(".jar")) {
                            jarName = jarName.substring(0, jarName.length() - 4)
                        }
                        JarFile jarFile = new JarFile(jarInput.file)
                        Enumeration enumeration = jarFile.entries()
                        File tmpFile = new File(jarInput.file.getParent() + File.separator + "classes_temp.jar")
                        //避免上次的缓存被重复插入
                        if (tmpFile.exists()) {
                            tmpFile.delete()
                        }
                        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile))
                        //用于保存
                        while (enumeration.hasMoreElements()) {
                            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                            String entryName = jarEntry.getName()
                            ZipEntry zipEntry = new ZipEntry(entryName)
                            InputStream inputStream = jarFile.getInputStream(jarEntry)
                            //插桩class
                            if (!entryName.startsWith("R\$") && !"R.class".equals(entryName) && !"BuildConfig.class".equals(entryName)
                                    && !(entryName.contains("META-INF") || entryName.endsWith(".DSA") || entryName.endsWith(".SF") || entryName.endsWith(".gz"))
                                && !jarEntry.isDirectory()
                            ) {
                                //class文件处理    "android/support/v4/app/FragmentActivity.class".equals(entryName)

                                NetMonitorLoger.printLogLine('----------- deal with "jar" class file <' + entryName + "   是否为文件夹： " + jarEntry.isDirectory() + "  file " + jarInput.file.isFile() + '> -----------')
                                jarOutputStream.putNextEntry(zipEntry)
                                ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
                                ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
                                ClassVisitor cv = new NetMonitorHookClassVisitor(classWriter)
                                classReader.accept(cv, ClassReader.EXPAND_FRAMES)
                                byte[] code = classWriter.toByteArray()
                                jarOutputStream.write(code)
                            } else {
                                jarOutputStream.putNextEntry(zipEntry)
                                jarOutputStream.write(IOUtils.toByteArray(inputStream))
                            }
                            jarOutputStream.closeEntry()
                        }
                        //结束
                        jarOutputStream.close()
                        jarFile.close()
                        def dest = outputProvider.getContentLocation(jarName + md5Name,
                                jarInput.contentTypes, jarInput.scopes, Format.JAR)
                        FileUtils.copyFile(tmpFile, dest)
                        tmpFile.delete()
                    }
                })
    }

    //asm 改动类
    private static byte[] modifyclass(byte[] sourceBytes) {
        try {
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
            ClassVisitor classVisitor = new NetMonitorHookClassVisitor(classWriter)
            ClassReader classReader = new ClassReader(sourceBytes)
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
            return classWriter.toByteArray()
        } catch (Exception ex) {
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