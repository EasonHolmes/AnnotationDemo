package com.cui.libapi;

import android.app.Activity;

import java.lang.reflect.Constructor;

/**
 * 用来实例化MainActivity$$ViewInjector
 */

public class InjectHelper {
    public static void inject(Activity host) {
        //获得 View 所在 Activity 的类路径，然后拼接一个字符串“$$ViewInjector”。
        // 这个是编译时动态生成的 Class 的完整路径，也就是我们需要实现的，同时也是最关键的部分；
        String classFullName = host.getClass().getName() + "$$ViewInjector";
        try {
//            根据 Class 路径，使用 Class.forName(classFullName) 生成 Class 对象；
            Class proxy = Class.forName(classFullName);
//            得到 Class 的构造函数 constructor 对象
            Constructor constructor = proxy.getConstructor(host.getClass());
//            使用 constructor.newInstance(host) new 出一个对象，这会执行对象的构造方法，方法内部是我们为 MainActivity 的 tv 赋值的地方。
            constructor.newInstance(host);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
