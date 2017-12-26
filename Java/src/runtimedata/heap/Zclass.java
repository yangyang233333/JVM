package runtimedata.heap;

import classfile.ClassFile;
import runtimedata.Slots;

/**
 * Author: zhangxin
 * Time: 2017/5/19 0019.
 * Desc: 如何设法保证同一个对象的 class == 返回 true
 */
public class Zclass {
    private int accessFlags;        // 表示当前类的访问标志
    public String thisClassName;   //当前类名字(完全限定名)
    public String superClassName;  //父类名字(完全限定名)
    public String[] interfaceNames;//接口名字(完全限定名,不可以为null,若为实现接口,数组大小为0)
    private RuntimeConstantPool runtimeConstantPool;//运行时常量池,注意和class文件中常量池区别;
    Zfield[] fileds;        //字段表,包括静态和非静态，此时并不分配 slotId；下面的staticVars 是其子集
    Zmethod[] methods;      //方法表，包括静态和非静态
    ZclassLoader loader;    //类加载器
    Zclass superClass;      //当前类的父类class,由类加载时,给父类赋值;
    Zclass[] interfaces;    //当前类的接口class,由类加载时,给父类赋值;
    int instanceSlotCount;  //非静态变量占用slot大小,这里只是统计个数(从顶级父类Object开始算起)
    int staticSlotCount;    // 静态变量所占空间大小
    Slots staticVars;      // 存放静态变量

    public Zclass(ClassFile classFile) {
        accessFlags = classFile.getAccessFlags();
        thisClassName = classFile.getClassName();
        superClassName = classFile.getSuperClassName();
        interfaceNames = classFile.getInterfaceNames();
        runtimeConstantPool = new RuntimeConstantPool(this, classFile.getConstantPool());
        fileds = Zfield.makeFields(this, classFile.getFields());
        methods = Zmethod.makeMethods(this, classFile.getMethods());

    }

    public RuntimeConstantPool getRuntimeConstantPool() {
        return runtimeConstantPool;
    }

    public boolean isPublic() {
        return 0 != (accessFlags & AccessFlag.ACC_PUBLIC);
    }

    public boolean isFinal() {
        return 0 != (accessFlags & AccessFlag.ACC_FINAL);
    }

    public boolean isSuper() {
        return 0 != (accessFlags & AccessFlag.ACC_SUPER);
    }

    public boolean isInterface() {
        return 0 != (accessFlags & AccessFlag.ACC_INTERFACE);
    }

    public boolean isAbstract() {
        return 0 != (accessFlags & AccessFlag.ACC_ABSTRACT);
    }

    public boolean isSynthetic() {
        return 0 != (accessFlags & AccessFlag.ACC_SYNTHETIC);
    }

    public boolean isAnnotation() {
        return 0 != (accessFlags & AccessFlag.ACC_ANNOTATION);
    }

    public boolean isEnum() {
        return 0 != (accessFlags & AccessFlag.ACC_ENUM);
    }

    public boolean isAccessibleTo(Zclass other) {
        return isPublic() || getPackageName().equals(other.getPackageName());
    }

    public Slots getStaticVars() {
        return staticVars;
    }

    public String getPackageName() {
        int i = thisClassName.lastIndexOf("/");
        if (i > 0) {
            return thisClassName.substring(0, i);
        }
        return "";
    }

    public boolean isSubClassOf(Zclass iface) {
        for (Zclass c = superClass; c != null; c = c.superClass) {
            if (c == iface) {
                return true;
            }
        }
        return false;
    }

    //这里不太好理解，该方法是在下面的 isImplements 方法中被调用的，调用方是类的接口
    //因此下面的 interfaces 数组表明的不是 source 的接口，而是 source 的某一个接口的接口
    //虽然接口 sub 在java 语法中是用 extends 继承父接口 parent，但是其字节码中，parent 是 sub 的接口而不是父类
    public boolean isSubInterfaceOf(Zclass iface) {
        for (Zclass superInterface : interfaces) {
            if (superInterface == iface || superInterface.isSubInterfaceOf(iface)) {
                return true;
            }
        }
        return false;
    }


    public boolean isImplements(Zclass iface) {
        for (Zclass c = this; c != null; c = c.superClass) {
            for (int i = 0; i < c.interfaces.length; i++) {
                if (c.interfaces[i] == iface || c.interfaces[i].isSubInterfaceOf(iface)) {
                    return true;
                }
            }
        }
        return false;
    }


    public boolean isAssignableFrom(Zclass source) {
        // source 是否由 target 扩展而来（子类）
        Zclass target = this;
        if (source == target) {
            return true;
        }
        //TODO:还要判断是否是数组的情况：
        if (target.isInterface()) {
            return source.isImplements(target);
        } else {
            return source.isSubClassOf(target);
        }
    }

    public Zobject newObject() {
        return new Zobject(this);
    }
}
