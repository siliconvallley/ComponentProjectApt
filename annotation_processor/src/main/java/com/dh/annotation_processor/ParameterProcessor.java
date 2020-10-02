package com.dh.annotation_processor;

import com.dh.annotation.Parameter;
import com.dh.annotation_processor.factory.ParameterFactory;
import com.dh.annotation_processor.utils.Constants;
import com.dh.annotation_processor.utils.EmptyUtils;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * @author 86351
 * @date 2020/9/26
 * @description
 */
// AutoService则是固定的写法，加个注解即可
// 通过auto-service中的@AutoService可以自动生成AutoService注解处理器，用来注册
// 用来生成 META-INF/services/javax.annotation.processing.Processor 文件
@AutoService(Processor.class)
// 允许/支持的注解类型，让注解处理器处理
@SupportedAnnotationTypes({Constants.PARAMETER_ANNOTATION_TYPES})
// 指定JDK编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ParameterProcessor extends AbstractProcessor {
    // 操作Element工具类 (类、函数、属性都是Element)
    private Elements elementUtils;

    // type(类信息)工具类，包含用于操作TypeMirror的工具方法
    private Types typeUtils;

    // Messager用来报告错误，警告和其他提示信息
    private Messager messager;

    // 文件生成器 类/资源，Filter用来创建新的源文件，class文件以及辅助文件
    private Filer filer;

    // 临时map存储，用来存放被@Parameter注解的属性集合，生成类文件时遍历
    // key:类节点, value:被@Parameter注解的属性集合
    private Map<TypeElement, List<Element>> tempParameterMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 一旦有类之上使用@Parameter注解
        if (!EmptyUtils.isEmpty(annotations)) {
            // 获取所有被 @Parameter 注解的 元素（属性）集合
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Parameter.class);

            if (!EmptyUtils.isEmpty(elements)) {
                // 赋值临时map存储，用来存放被注解的属性集合
                valueOfParameterMap(elements);
                try {
                    // 生成类文件，如：
                    createParameterFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return true;
        }

        return false;
    }

    private void createParameterFile() throws IOException {
        // 判断是否有需要生成的类文件
        if (EmptyUtils.isEmpty(tempParameterMap)) return;
        // 通过Element工具类，获取Parameter类型ACTIVITY
        TypeElement activityType = elementUtils.getTypeElement(Constants.ACTIVITY);
        TypeElement parameterType = elementUtils.getTypeElement(Constants.PARAMETER_PATH);

        // 参数体配置(Object target)
        ParameterSpec parameterSpec = ParameterSpec.builder(TypeName.OBJECT, Constants.PARAMETER_NAME).build();
        for (Map.Entry<TypeElement, List<Element>> entry : tempParameterMap.entrySet()) {
            // Map集合中的key是类名，如：MainActivity
            TypeElement typeElement = entry.getKey();
            // 如果类名的类型和Activity类型不匹配
            if (!typeUtils.isSubtype(typeElement.asType(), activityType.asType())) {
                throw new RuntimeException("@Parameter注解目前仅限用于Activity类之上");
            }

            // 获取类名
            ClassName className = ClassName.get(typeElement);
            // 方法体内容构建
            ParameterFactory factory = new ParameterFactory.Builder(parameterSpec)
                    .setMessager(messager)
                    .setClassName(className)
                    .build();

            // 添加方法体内容的第一行
            factory.addFirstStatement();

            // 遍历类里面所有属性
            for (Element fieldElement : entry.getValue()) {
                factory.buildStatement(fieldElement);
            }

            // 最终生成的类文件名（类名$$Parameter）
            String finalClassName = typeElement.getSimpleName() + Constants.PARAMETER_FILE_NAME;
            messager.printMessage(Diagnostic.Kind.NOTE, "APT生成获取参数类文件：" +
                    className.packageName() + "." + finalClassName);

            messager.printMessage(Diagnostic.Kind.NOTE, "APT生成的方法体:: \n" + factory.build());
            // MainActivity$$Parameter
            JavaFile.builder(className.packageName(), // 包名
                    TypeSpec.classBuilder(finalClassName) // 类名
                            .addSuperinterface(ClassName.get(parameterType)) // 实现ParameterLoad接口
                            .addModifiers(Modifier.PUBLIC) // public修饰符
                            .addMethod(factory.build()) // 方法的构建（方法参数 + 方法体）
                            .build()) // 类构建完成
                    .build() // JavaFile构建完成
                    .writeTo(filer); // 文件生成器开始生成类文件
        }
    }

    /**
     * 赋值临时map存储，用来存放被@Parameter注解的属性集合，生成类文件时遍历
     *
     * @param elements 被 @Parameter 注解的 元素集合
     */
    private void valueOfParameterMap(Set<? extends Element> elements) {
        for (Element element : elements) {
            // messager.printMessage(Diagnostic.Kind.NOTE, "@Parameter注解的元素:: " + element.toString());

            // 注解在属性之上，属性节点父节点是类节点
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
            // 如果map集合中的key：类节点存在，直接添加属性
            if (!tempParameterMap.containsKey(enclosingElement)) {
                List<Element> fields = new ArrayList<>();
                fields.add(element);
                tempParameterMap.put(enclosingElement, fields);
            } else {
                tempParameterMap.get(enclosingElement).add(element);
            }
        }
    }
}
