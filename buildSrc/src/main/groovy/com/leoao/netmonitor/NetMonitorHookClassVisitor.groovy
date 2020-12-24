package  com.leoao.netmonitor

import jdk.internal.org.objectweb.asm.AnnotationVisitor
import jdk.internal.org.objectweb.asm.Attribute
import jdk.internal.org.objectweb.asm.ClassVisitor
import jdk.internal.org.objectweb.asm.FieldVisitor
import jdk.internal.org.objectweb.asm.MethodVisitor
import jdk.internal.org.objectweb.asm.TypePath

class NetMonitorHookClassVisitor extends ClassVisitor{

    NetMonitorHookClassVisitor(int i) {
        super(i)
    }

    NetMonitorHookClassVisitor(int i, ClassVisitor classVisitor) {
        super(i, classVisitor)
    }

    @Override
    void visit(int i, int i1, String s, String s1, String s2, String[] strings) {
        super.visit(i, i1, s, s1, s2, strings)
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
        println("visitEnd s  " + s + "    s1  " +  s1 + "  s2   " +  s2)
        return super.visitField(i, s, s1, s2, o)
    }

    @Override
    MethodVisitor visitMethod(int i, String s, String s1, String s2, String[] strings) {
        println("visitMethod  s  " + s + "    s1  " +  s1 + "  s2   " +  s2 )
        return super.visitMethod(i, s, s1, s2, strings)
    }

    @Override
    void visitEnd() {
        println("visitEnd")
        super.visitEnd()
    }

    @Override
    Object invokeMethod(String s, Object o) {
        return super.invokeMethod(s, o)
    }
}