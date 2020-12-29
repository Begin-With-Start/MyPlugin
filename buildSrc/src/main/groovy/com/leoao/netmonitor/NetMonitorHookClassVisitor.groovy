package com.leoao.netmonitor

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Attribute
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.TypePath


class NetMonitorHookClassVisitor extends ClassVisitor implements Opcodes{
    private ClassVisitor classVisitor

    private String mClassName
    private String mSuperName
    private String[] mInterfaces

    NetMonitorHookClassVisitor(final ClassVisitor classVisitor) {
        super(Opcodes.ASM6, classVisitor)
        this.classVisitor = classVisitor
    }

    /**
     *
     */
    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces)
        mClassName = name
        mSuperName = superName
        mInterfaces = interfaces

        NetMonitorLoger.printLogLine("扫描类：" +  mClassName + "   父类：  " + mSuperName )
    }

    /**
     * 该方法是当扫描器完成类扫描时才会调用，如果想在类中追加某些方法，可以在该方法中实现。
     */
    @Override
    void visitEnd() {
        NetMonitorLoger.printLogLine("visitEnd")
        super.visitEnd()
    }

    @Override
    void visitSource(String s, String s1) {
        super.visitSource(s, s1)
    }

    @Override
    AnnotationVisitor visitAnnotation(String s, boolean b) {
        return super.visitAnnotation(s, b)
    }

    @Override
    AnnotationVisitor visitTypeAnnotation(int i, TypePath typePath, String s, boolean b) {
        return super.visitTypeAnnotation(i, typePath, s, b)
    }

    @Override
    void visitAttribute(Attribute attribute) {
        super.visitAttribute(attribute)
    }

    @Override
    void visitInnerClass(String s, String s1, String s2, int i) {
        super.visitInnerClass(s, s1, s2, i)
    }

    @Override
    FieldVisitor visitField(int i, String s, String s1, String s2, Object o) {
        NetMonitorLoger.printLogLine("visitEnd s  " + s + "    s1  " + s1 + "  s2   " + s2)
        return super.visitField(i, s, s1, s2, o)
    }

    @Override
    MethodVisitor visitMethod(int i, String s, String s1, String s2, String[] strings) {
        NetMonitorLoger.printLogLine("visitMethod  s  " + s + "    s1  " + s1 + "  s2   " + s2)
        return super.visitMethod(i, s, s1, s2, strings)
    }
}