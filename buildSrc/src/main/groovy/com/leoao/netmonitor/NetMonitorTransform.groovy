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
     * SCOPE_FULL_PROJECT：  contain
     *                     Scope.PROJECT,
     *                     Scope.SUB_PROJECTS,
     *                     Scope.EXTERNAL_LIBRARIES
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
        Loger.printLogLine("开始代码扫描")
        Loger.printLogLine("开始代码扫描 ")
        //进行类 jar class的处理；
        if (isIncremental) {
            Loger.printLine("代码扫描完成 增量编译 耗时： " + endTime + "s")
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
            Loger.printLine("代码扫描完成 非增量编译 耗时： " + endTime + "s")
        }

    }


    //处理目录中的class 目标文件
    static handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider) {
        //是否是目录
        if (directoryInput.file.isDirectory()) {
            //列出目录所有文件（包含子文件夹，子文件夹内文件）
            directoryInput.file.eachFileRecurse { File file ->
                def name = file.name
                //非android生成文件 .class文件
                if (name.endsWith(".class") && !name.startsWith("R\$") && !"R.class".equals(name) && !"BuildConfig.class".equals(name)) {
                    // && "android/support/v4/app/FragmentActivity.class".equals(name)
                    Loger.printLogLine("class 文件中  " + dealPath(directoryInput, file))
                    ClassReader classReader = new ClassReader(file.bytes)
                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
                    ClassVisitor cv = new NetMonitorHookClassVisitor(classWriter)
                    classReader.accept(cv, ClassReader.EXPAND_FRAMES)
                    /**
                     * ClassReader 类用于读取一个类文件的字节码，通过 accept 方法委托给 ClassVisitor 解析
                     * 其中有在 accept 方法中有 4 个重要的 parsingOptions：
                     * - ClassReader.SKIP_DEBUG：标识跳过调试内容，即 SourceFile(跳过源文件)、SourceDebugExtension(源码调试扩展)、LocalVariableTable(局部变量表)、
                     *      LocalVariableTypeTable(局部变量类型表)和 LineNumberTable(行号表属性)，
                     *      同时以下方法既不会被解析也不会被访问（ClassVisitor.visitSource，MethodVisitor.visitLocalVariable，MethodVisitor.visitLineNumber）。
                     *      使用此标识后，类文件调试信息会被去除，请警记
                     * - ClassReader.SKIP_CODE：跳过 Code attributes(代码属性)将不会被转换和访问，比如方法体代码不会进行解析和访问。
                     * - ClassReader.SKIP_FRAMES：跳过 StackMap(栈图)和 StackMapTable(栈图表) 属性，即 MethodVisitor.visitFrame 方法不会被转换和访问。
                     *      当使用 ClassWriter.COMPUTE_FRAMES 时，该标识会很有用，因为它避免了访问帧内容（这些内容会被忽略和重新计算，无需访问）。
                     * - ClassReader.EXPAND_FRAMES：表示扩展栈帧图。默认栈图以它们原始格式（V1_6以下使用扩展格式，其他使用压缩格式）被访问。
                     *      如果设置该标识，栈图则始终以扩展格式进行访问（此标识在 ClassReader 和 ClassWriter 中增加了解压/压缩步骤，会大幅度降低性能）
                     *
                     */
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
                        if (!NetMonitorUtils.isAndroidGenerated(entryName)
                                && !(NetMonitorUtils.isOtherFile(entryName))
                                && !jarEntry.isDirectory()
                            && entryName.endsWith(".class")
                            && entryName.contains("okhttp")
                        ) {
                            //class文件处理    "android/support/v4/app/FragmentActivity.class".equals(entryName)
                            Loger.printLogLine('----------- deal with "jar" class file <' + entryName + "   是否为文件夹： " + jarEntry.isDirectory() + "  file " + jarInput.file.isFile() + '> -----------')
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
            }
        );
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