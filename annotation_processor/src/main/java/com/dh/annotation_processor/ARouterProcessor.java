package com.dh.annotation_processor;

import com.dh.annotation.ARouter;
import com.dh.annotation.model.RouterBean;
import com.dh.annotation_processor.utils.Constants;
import com.dh.annotation_processor.utils.EmptyUtils;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

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
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * @author 86351
 * @date 2020/9/24
 * @description
 */
// AutoService则是固定的写法，加个注解即可
// 通过auto-service中的@AutoService可以自动生成AutoService注解处理器，用来注册
// 用来生成 META-INF/services/javax.annotation.processing.Processor 文件
@AutoService(Processor.class)
// 允许/支持的注解类型，让注解处理器处理
@SupportedAnnotationTypes({Constants.AROUTER_ANNOTATION_TYPES})
// 指定JDK编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
// 注解处理器接收的参数
@SupportedOptions({Constants.MODULE_NAME, Constants.APT_PACKAGE})
public class ARouterProcessor extends AbstractProcessor {
    // 操作Element工具类 (类、函数、属性都是Element)
    private Elements elementUtils;

    // type(类信息)工具类，包含用于操作TypeMirror的工具方法
    private Types typeUtils;

    // Messager用来报告错误，警告和其他提示信息
    private Messager messager;

    // 文件生成器 类/资源，Filter用来创建新的源文件，class文件以及辅助文件
    private Filer filer;

    // 子模块名，如：app/order/personal。需要拼接类名时用到（必传）ARouter$$Group$$order
    private String moduleName;

    // 包名，用于存放APT生成的类文件
    private String packageNameForAPT;

    // 临时map存储，用来存放路由组Group对应的详细Path类对象，生成路由路径类文件时遍历
    // key:组名"app", value:"app"组的路由路径"ARouter$$Path$$app.class"
    private Map<String, List<RouterBean>> tempPathMap = new HashMap<>();

    // 临时map存储，用来存放路由Group信息，生成路由组类文件时遍历
    // key:组名"app", value:类名"ARouter$$Path$$app.class"
    private Map<String, String> tempGroupMap = new HashMap<>();

    /**
     * // 该方法主要用于一些初始化的操作，通过该方法的参数ProcessingEnvironment
     * 可以获取一些列有用的工具类
     *
     * @param processingEnv ProcessingEnvironment对象
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();

        // 通过processingEnv获取传递的参数
        Map<String, String> options = processingEnv.getOptions();
        if (!EmptyUtils.isEmpty(options)) {
            moduleName = options.get(Constants.MODULE_NAME);
            packageNameForAPT = options.get(Constants.APT_PACKAGE);
            // 有坑：Diagnostic.Kind.ERROR，异常会自动结束，不像安卓中Log.e
            messager.printMessage(Diagnostic.Kind.NOTE, "moduleName:: " + moduleName);
            messager.printMessage(Diagnostic.Kind.NOTE, "packageNameForAPT:: " + packageNameForAPT);
        }

        // 必传参数判空（乱码问题：添加java控制台输出中文乱码）
        if (EmptyUtils.isEmpty(moduleName) || EmptyUtils.isEmpty(packageNameForAPT)) {
            throw new RuntimeException("注解处理器需要的参数moduleName或者packageName为空，请在对应build.gradle配置参数");
        }
    }

    /**
     * 相当于main函数，开始处理注解
     * 注解处理器的核心方法，处理具体的注解，生成Java文件
     *
     * @param annotations 使用了支持处理注解的节点集合
     * @param roundEnv    当前或是之前的运行环境,可以通过该对象查找的注解
     * @return 表示后续处理器不会再处理（已经处理完成）
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 有类上使用了@ARouter注解
        if (!EmptyUtils.isEmpty(annotations)) {
            // 获取所有@ARouter注解的元素集合
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(ARouter.class);
            if (!EmptyUtils.isEmpty(elements)) {
                try {
                    parseElements(elements);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 坑：必须写返回值，表示处理@ARouter注解完成
            return true;
        }
        return false;
    }

    /**
     * 解析所有被@ARouter注解的类元素
     *
     * @param elements
     */
    private void parseElements(Set<? extends Element> elements) throws IOException {
        // 通过Element工具类，获取Activity、Callback类型
        TypeElement activityType = elementUtils.getTypeElement(Constants.ACTIVITY);
        // 显示类信息（获取被注解节点，类节点）这里也叫自描述 Mirror
        TypeMirror activityMirror = activityType.asType();

        for (Element element : elements) {
            // 获取每个元素类信息，用于比较
            TypeMirror typeMirror = element.asType();
            messager.printMessage(Diagnostic.Kind.NOTE, "遍历类元素信息:: " + typeMirror.toString());

            // 获取每个类上的@ARouter注解中的注解值
            ARouter aRouter = element.getAnnotation(ARouter.class);

            // 路由的详细信息封装到实体类
            RouterBean bean = new RouterBean.Builder()
                    .setGroup(aRouter.group())
                    .setPath(aRouter.path())
                    .setElement(element)
                    .build();

            // 高级判断：ARouter注解仅能用在类之上，并且是规定的Activity
            // 类型工具类方法isSubtype，相当于instance一样
            if (typeUtils.isSubtype(typeMirror, activityMirror)) {
                bean.setType(RouterBean.Type.ACTIVITY);
            } else {
                // 不匹配抛出异常，这里谨慎使用！考虑维护问题
                throw new RuntimeException("@ARouter注解目前仅限用于Activity类之上");
            }

            // 赋值临时map存储，用来存放路由组Group对应的详细Path类对象
            valueOfPathMap(bean);
        }

        // ARouterLoadGroup和ARouterLoadPath类型，用来实现类文件时的实现接口
        // 组接口
        TypeElement groupLoadType = elementUtils.getTypeElement(Constants.AROUTE_GROUP);
        // 路径接口
        TypeElement pathLoadType = elementUtils.getTypeElement(Constants.AROUTE_PATH);

        // 1、生成路由的详细path类文件，如ARouter$$Path$$app
        createPathFile(pathLoadType);

        // 2、生成路由的组文件（没有Path类文件，取不到类文件，不能进行下一步），如：ARouter$$Group$$app
        createGroupFile(groupLoadType, pathLoadType);
    }

    /**
     * 生成路由Group对应的详细的Path文件，如：ARouter$$Path$$app
     *
     * @param pathLoadType ARouterLoadPath接口信息
     */
    private void createPathFile(TypeElement pathLoadType) throws IOException {
        if (EmptyUtils.isEmpty(tempPathMap)) {
            return;
        }

        // 方法的返回值Map<String, RouterBean>
        TypeName methodReturn = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(RouterBean.class)
        );

        // 遍历分组，每一个分组创建一个路径类文件，如：ARouter$$Path$$app
        for (Map.Entry<String, List<RouterBean>> entry : tempPathMap.entrySet()) {
            // 方法配置：public Map<String, RouterBean> loadPath() {
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Constants.PATH_METHOD_NAME) // 方法名
                    .addAnnotation(Override.class) // 注解
                    .addModifiers(Modifier.PUBLIC) // 修饰符
                    .returns(methodReturn); // 返回值


            // 不循环的部分Map<String, RouterBean> pathMap = new HashMap<>();
            methodBuilder.addStatement("$T<$T, $T> $N = new $T<>()",
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ClassName.get(RouterBean.class),
                    Constants.PATH_PARAMETER_NAME,
                    ClassName.get(HashMap.class));

            // 一个分组，如：ARouter$$Path$$app。有很多详细路径信息，如：/app/MainActivity、/app/OtherActivity
            List<RouterBean> routerBeans = entry.getValue();
            for (RouterBean bean : routerBeans) {
                // 方法内容的循环部分
                /**
                 * pathMap.put("/order/OrderMainActivity",
                 *                 RouterBean.create(RouterBean.Type.ACTIVITY, OrderMainActivity.class,
                 *                         "/order/OrderMainActivity", "order"));
                 */
                methodBuilder.addStatement("$N.put($S, $T.create($T.$L, $T.class, $S, $S))",
                        Constants.PATH_PARAMETER_NAME, // pathMap
                        bean.getPath(), // "/app/MainActivity"
                        ClassName.get(RouterBean.class),
                        ClassName.get(RouterBean.Type.class),
                        bean.getType(), // 枚举ACTIVITY
                        ClassName.get((TypeElement) bean.getElement()), // MainActivity.class
                        bean.getPath(), // "/app/MainActivity"
                        bean.getGroup()); // app
            }
            // 遍历过后：返回return pathMap
            methodBuilder.addStatement("return $N", Constants.PATH_PARAMETER_NAME);

            // 最终生成的类文件名，如ARouter$$Path$$app
            String resultClassName = Constants.PATH_FILE_NAME + entry.getKey();
            messager.printMessage(Diagnostic.Kind.NOTE, "APT生成路由Path类文件：" +
                    packageNameForAPT + "." + resultClassName);

            // 生成类文件，如ARouter$$Path$$app
            JavaFile.builder(packageNameForAPT, // 包路径
                    TypeSpec.classBuilder(resultClassName) // 类名
                            .addSuperinterface(ClassName.get(pathLoadType)) // 实现接口
                            .addModifiers(Modifier.PUBLIC) // 修饰符
                            .addMethod(methodBuilder.build()) // 方法
                            .build())
                    .build()
                    .writeTo(filer);

            // 非常重要一步！路径文件生成出来了，才能赋值路由组tempGroupMap
            tempGroupMap.put(entry.getKey(), resultClassName);
        }
    }

    /**
     * 生成路由组文件，如：ARouter$$Group$$app
     *
     * @param groupLoadType
     * @param pathLoadType
     */
    private void createGroupFile(TypeElement groupLoadType, TypeElement pathLoadType) throws IOException {
        // 判断是否有需要生成的类文件
        if (EmptyUtils.isEmpty(tempGroupMap) || EmptyUtils.isEmpty(tempPathMap)) return;

        // 方法的返回值Map<String, Class<? extends ARouterLoadPath>>
        TypeName methodReturn = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                // 第二个参数：Class<? extends ARouterLoadPath>
                // 某某Class是否属于ARouterLoadPath接口的实现类
                ParameterizedTypeName.get(
                        ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(pathLoadType))
                )
        );

        // 方法配置：public Map<String, Class<? extends ARouterLoadPath>> loadGroup() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Constants.GROUP_METHOD_NAME) // 方法名
                .addAnnotation(Override.class) // 重写注解
                .addModifiers(Modifier.PUBLIC) // public修饰符
                .returns(methodReturn); // 方法返回值

        // Map<String, Class<? extends ARouterLoadPath>> groupMap = new HashMap<>();
        methodBuilder.addStatement("$T<$T, $T> $N = new $T<>()",
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(pathLoadType))),
                Constants.GROUP_PARAMETER_NAME,
                HashMap.class);

        // 方法内容配置
        for (Map.Entry<String, String> entry : tempGroupMap.entrySet()) {
            // 类似String.format("hello %s net163 %d", "net", 163)通配符
            // groupMap.put("main", ARouter$$Path$$app.class);
            methodBuilder.addStatement("$N.put($S, $T.class)",
                    Constants.GROUP_PARAMETER_NAME, // groupMap.put
                    entry.getKey(),
                    // 类文件在指定包名下
                    ClassName.get(packageNameForAPT, entry.getValue()));
        }

        // 遍历之后：return groupMap;
        methodBuilder.addStatement("return $N", Constants.GROUP_PARAMETER_NAME);

        // 最终生成的类文件名
        String resultClassName = Constants.GROUP_FILE_NAME + moduleName;
        messager.printMessage(Diagnostic.Kind.NOTE, "APT生成路由组Group类文件：" +
                packageNameForAPT + "." + resultClassName);

        // 生成类文件：ARouter$$Group$$app
        JavaFile.builder(packageNameForAPT, // 包名
                TypeSpec.classBuilder(resultClassName) // 类名
                        .addSuperinterface(ClassName.get(groupLoadType)) // 实现ARouterLoadGroup接口
                        .addModifiers(Modifier.PUBLIC) // public修饰符
                        .addMethod(methodBuilder.build()) // 方法的构建（方法参数 + 方法体）
                        .build()) // 类构建完成
                .build() // JavaFile构建完成
                .writeTo(filer); // 文件生成器开始生成类文件
    }

    /**
     * 赋值临时map存储，用来存放路由组Group对应的详细类path对象，
     * 生成路由 路径类文件时遍历
     *
     * @param bean 路由详细信息，最终实体封装类
     */
    private void valueOfPathMap(RouterBean bean) {
        if (checkRouterPath(bean)) {
            messager.printMessage(Diagnostic.Kind.NOTE, "RouterBean:: " + bean.toString());

            List<RouterBean> routerBeans = tempPathMap.get(bean.getGroup());
            // 如果从Map中找不到key为：bean.getGroup()的数据，就新建List集合再添加进Map
            if (EmptyUtils.isEmpty(routerBeans)) {
                routerBeans = new ArrayList<>();
                routerBeans.add(bean);
                tempPathMap.put(bean.getGroup(), routerBeans);
            } else {
                // 如果找到了key，就直接将实体对象加入临时集合
                routerBeans.add(bean);
            }

        } else {
            messager.printMessage(Diagnostic.Kind.ERROR, "@ARouter注解未按规范书写，/app/MainActivity");
        }
    }

    /**
     * 校验@ARouter注解的值，如果group未填写就从必填项path中截取数据
     *
     * @param bean 路由详细信息，最终实体封装类
     * @return
     */
    private boolean checkRouterPath(RouterBean bean) {
        String group = bean.getGroup();
        String path = bean.getPath();

        // @ARouter注解中的path值，必须要以 / 开头（模仿阿里ARouter规范）
        if (EmptyUtils.isEmpty(path) || !path.startsWith("/")) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "@ARouter注解未按规范书写, 如: /app/MainActivity, 错误的path:: " + path);
            return false;
        }

        // 比如开发者代码为：path = "/MainActivity"，最后一个 / 符号必然在字符串第1位
        if (path.lastIndexOf("/") == 0) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "@ARouter注解未按规范书写, 如: /app/MainActivity, 错误的path:: " + path);
            return false;
        }

        // 从第一个 / 到第二个 / 中间截取，如：/app/MainActivity 截取出 app 作为group
        String resultGroup = path.substring(1, path.indexOf("/", 1));

        // 比如开发者/MainActivity/MainActivity
        if (EmptyUtils.isEmpty(resultGroup) || resultGroup.contains("/")) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "@ARouter注解未按规范书写, 如: /app/MainActivity, 错误的path:: " + path);
            return false;
        }

        // @ARouter注解中的group有赋值情况
        if (!EmptyUtils.isEmpty(group) && !group.equals(moduleName)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "@ARouter注解中的group值必须和当前的Module名相同");
            return false;
        } else {
            bean.setGroup(resultGroup);
        }

        return true;
    }
}
