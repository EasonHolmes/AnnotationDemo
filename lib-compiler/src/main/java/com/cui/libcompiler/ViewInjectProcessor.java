package com.cui.libcompiler;

import com.cui.libannotation.BindView;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

/**
 * 表示一个程序元素，比如包、类或者方法，有如下几种子接口：
 * ExecutableElement：表示某个类或接口的方法、构造方法或初始化程序（静态或实例），包括注解类型元素 ；
 * PackageElement：表示一个包程序元素；
 * TypeElement：表示一个类或接口程序元素；简单来说就是一个class类
 * TypeParameterElement：表示一般类、接口、方法或构造方法元素的形式类型参数；
 * VariableElement：表示一个字段、enum 常量、方法或构造方法参数、局部变量或异常参数 简单来说就是变量
 * <p>
 * package com.example;        // PackageElement
 * public class Sample         // TypeElement
 * <T extends List> {  // TypeParameterElement
 * private int num;        // VariableElement
 * String name;            // VariableElement
 * public Sample() {}      // ExecuteableElement
 * public void setName(    // ExecuteableElement
 * String name     // VariableElement
 * ) {}
 * }
 * <p>
 * http://blog.csdn.net/crazy1235/article/details/51876192 javapoet使用 用来生成java文件源代码的
 */
@AutoService(Processor.class)//生成 META-INF 信息；
@SupportedSourceVersion(SourceVersion.RELEASE_7)//声明支持的源码版本
@SupportedAnnotationTypes({"com.cui.libannotation.BindView"})//指定要处理的注解路径
//声明 Processor 处理的注解，注意这是一个数组，表示可以处理多个注解；
public class ViewInjectProcessor extends AbstractProcessor {
    // 存放同一个Class下的所有注解
    Map<String, List<VariableInfo>> classMap = new HashMap<>();
    // 存放Class对应的TypeElement
    Map<String, TypeElement> classTypeElement = new HashMap<>();

    private Filer filer;
    Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        collectInfo(roundEnvironment);
        writeToFile();
        return true;
    }

    void collectInfo(RoundEnvironment roundEnvironment) {
        classMap.clear();
        classTypeElement.clear();

        //获取所有使用到bindView的类
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(BindView.class);
        for (Element element : elements) {
            //获取bindvView注解的值
            int viewId = element.getAnnotation(BindView.class).value();

            //代表被注解的元素 variableElemet是element的子类
            VariableElement variableElement = (VariableElement) element;

            //被注解元素所在的class
            TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
            //class的完整路径
            String classFullName = typeElement.getQualifiedName().toString();

            // 收集Class中所有被注解的元素
            List<VariableInfo> variablist = classMap.get(classFullName);
            if (variablist == null) {
                variablist = new ArrayList<>();

                VariableInfo variableInfo = new VariableInfo();
                variableInfo.setVariableElement(variableElement);
                variableInfo.setViewId(viewId);
                variablist.add(variableInfo);

                classMap.put(classFullName, variablist);

                //保存class对应要素（完整路径，typeElement）
                classTypeElement.put(classFullName, typeElement);
            }
        }
    }

    /**
     * http://blog.csdn.net/crazy1235/article/details/51876192 javapoet使用 用来生成java文件源代码的
     * 底下创建java源代码的可以看这个链接
     */
    void writeToFile() {
        try {
            for (String classFullName : classMap.keySet()) {
                TypeElement typeElement = classTypeElement.get(classFullName);

                //使用构造函数绑定数据 创建一个构造函数public类型添加参数(参数类型全路径如Bundle android.os.Bundle,"参数名")
                MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(TypeName.get(typeElement.asType()), "activity").build());

                List<VariableInfo> variableList = classMap.get(classFullName);
                for (VariableInfo variableInfo : variableList) {
                    VariableElement variableElement = variableInfo.getVariableElement();
                    // 变量名称(比如：TextView tv 的 tv)
                    String variableName = variableElement.getSimpleName().toString();
                    // 变量类型的完整类路径（比如：android.widget.TextView）
                    String variableFullName = variableElement.asType().toString();
                    // 在构造方法中增加赋值语句，例如：activity.tv = (android.widget.TextView)activity.findViewById(215334);
                    constructor.addStatement("activity.$L=($L)activity.findViewById($L)", variableName, variableFullName, variableInfo.getViewId());
                }

                //创建class
                TypeSpec typeSpec = TypeSpec.classBuilder(typeElement.getSimpleName() + "$$ViewInjector")
                        .addModifiers(Modifier.PUBLIC)
                        .addMethod(constructor.build())
                        .build();

                //与目标class放在同一个包下，解决class属性的可访问性
                String packageFullname = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
                JavaFile javaFile = JavaFile.builder(packageFullname, typeSpec).build();
                //生成 class文件
                javaFile.writeTo(filer);

            }
        } catch (Exception e) {

        }
    }
}
