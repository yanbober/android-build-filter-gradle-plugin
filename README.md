# android-build-filter-gradle-plugin

[![](https://jitpack.io/v/yanbober/android-build-filter-gradle-plugin.svg)](https://jitpack.io/#yanbober/android-build-filter-gradle-plugin)

一个小众需求下移除 Android 构建中 Jar 包、AAR 包、构建冗余 class 文件的 Gradle 插件。

## 配置

在相应 gradle 文件添加如下仓库。

```gradle
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
        }
    }

    dependencies {
        classpath 'com.github.yanbober:android-build-filter-gradle-plugin:1.0.1'
    }
}

apply plugin: 'buildfilter'
//或者
apply plugin: 'android.build.filter.gradle'
```

## 使用

将想要移除的 class 文件添加到对应配置中，如下：
```gradle
apply plugin: 'buildfilter'

buildClassFilter {
    sourceExcludes = [
            'YOUR CLASS', '**/BuildConfig*'
    ]

    jarExcludes = [ 'cn/sina/sdk/BuildConfig.class' ]
}
```

其中 class 全限定描述名支持标准正则匹配。
sourceExcludes 中配置你项目中源码生成或者 IDE 自动生成的 class 文件(譬如 BuildConfig)。
jarExcludes 中配置你项目依赖的 aar 或者 jar 包中你想移除的 class 文件。

## 校验与追踪

按照如上配置后进行项目构建编译，编译后你可以在你配置 buildClassFilter 的模块的对应构建输出目录中找到校验文件。默认输出为当前 module 下 build 目录，你可以在 build/buildClassFilter/ 目录下找到 build-class-filter-report.md 文件。

build-class-filter-report.md 文件格式如下：
```md
## Report gradle project is demolib1.

//你在 gradle 配置的删除源码生成的 class 正则列表
## SourceExcludes config is:
cn/yan/gradle/plugin/BuildConfig.class

//依据正则在打包前被删掉的 class 列表
## SourceExcludes remove file is:
/home/yan/github/android-build-filter-gradle-plugin/demolib1/build/intermediates/classes/debug/cn/yan/gradle/plugin/BuildConfig.class

//你在 gradle 配置的删除 jar 或者 aar 中的 class 正则列表
## JarExcludes config is:

//依据正则在打包前被删掉的 class 列表
## JarExcludes remove file is:

```

## 应用场景

- 引用外部第三方 SDK，譬如有些 SDK 内部多打包了一个同包名 BuildConfig 等 class 文件，我们想用该 SDK 就会冲突，笨办法就是解压 SDK 删除文件再把 SDK 压缩使用，而先进的办法就是使用 jarExcludes 即可轻松解决。

- 有时候我们有一个 jar 或者 aar 包想修改其中某个类（该类可以从 SDK 摘出来或者其关联访问接口都是 public 的）达到偷换概念的目的，一种办法就是运行时反射，另一种办法就是把 jar 或者 aar 包中要替换的 class 文件（包括内部类）通过 jarExcludes 删除，然后在你项目中创建与 jarExcludes 中完全一致的包目录结构和 java 文件，对原 class 保持接口一致的情况下不用反射就可以随意修改其方法实现逻辑了。

## 注意

- 由于所有的 class exclude 操作是构建生成 class 文件后删除的，所以即便 class 不存在也能打包成功，所以你需要非常清楚自己在干什么，一定要看下构建校验 report 文件中被删除的 class 是不是你期望的或者删除的 class 对别的 class 有无影响。

- 对于 jar 或者 aar 包中 class 的替换操作一定要清楚自己在干什么，建议一般修改方法实现逻辑，谨慎修改了类成员属性名，除非你能清楚知道其他地方没有反射这个属性。

## 拓展知识

与该插件类似的其实还有许多其他 android 构建相关的常见棘手问题（比较多见的都是 duplicate xxx 问题），所以既然想说明该插件的用处则就顺带提下相关其他知识。

### 1 自定义过滤参与构建的源码

Android Gradle 插件提供的常用构建源码配置 extension 如下：

```gradle
//执行 gradlew sourceSets 查看构建源码

android {
  ...
  sourceSets {
    main {
        //设置参与构建java源文件
        AndroidSourceDirectorySet java

        //设置参与构建aidl源文件
        AndroidSourceDirectorySet aidl

        //设置参与构建assets源文件
        AndroidSourceDirectorySet assets

        The Android Assets directory for this source set.

        //设置参与构建jni源文件
        AndroidSourceDirectorySet jni

        //设置参与构建jni lib源文件
        AndroidSourceDirectorySet jniLibs

        //设置参与构建清单源文件
        AndroidSourceFile manifest

        //设置参与构建renderscript源文件
        AndroidSourceDirectorySet renderscript

        //设置参与构建res源文件
        AndroidSourceDirectorySet res

        //设置参与构建的 java resource资源源文件
        AndroidSourceDirectorySet resources

        //其他属性
        ......
    }

    //......
    androidTest {
        ...
    }
  }
}
```

如上列出了参与 apk 构建的源码主要配置项，你可以发现很多配置想都是 AndroidSourceDirectorySet 类型的，而 AndroidSourceDirectorySet 是 PatternFilterable 接口的子接口，所以这些类型的配置项都支持如下列表方法：

```
Set<String> getIncludes();

Set<String> getExcludes();

PatternFilterable setIncludes(Iterable<String> var1);

PatternFilterable setExcludes(Iterable<String> var1);

PatternFilterable include(String... var1);

PatternFilterable include(Iterable<String> var1);

PatternFilterable include(Spec<FileTreeElement> var1);

PatternFilterable include(Closure var1);

PatternFilterable exclude(String... var1);

PatternFilterable exclude(Iterable<String> var1);

PatternFilterable exclude(Spec<FileTreeElement> var1);

PatternFilterable exclude(Closure var1);

String getName();

AndroidSourceDirectorySet srcDir(Object var1);

AndroidSourceDirectorySet srcDirs(Object... var1);

AndroidSourceDirectorySet setSrcDirs(Iterable<?> var1);

FileTree getSourceFiles();

PatternFilterable getFilter();

List<ConfigurableFileTree> getSourceDirectoryTrees();

Set<File> getSrcDirs();
```

从上面方法名字和 Groovy 语法就能看出我们对于构建源码完全可以自己决定哪些参与构建，哪些不参与构建。

譬如我们不想让模块的 BuildConfig 类参与构建（BuildConfig 是构建自动生成的），如下写法：

```gradle
android {
    sourceSets {
        main {
             java.exclude("cn/yan/gradle/plugin/BuildConfig.java")
        }
    }
}
```

### 2 自定义过滤打包到 APK 的文件

除了从源头上自定义构建源文件外还可以在最终打包（听清楚，是最终）时指定哪些文件不添加到最终的压缩包（.apk）中。

packagingOptions 官方默认的配置如下：
```
Pick first: none
Merge: /META-INF/services/**
Exclude:
/META-INF/LICENSE
/META-INF/LICENSE.txt
/META-INF/NOTICE
/META-INF/NOTICE.txt
/LICENSE
/LICENSE.txt
/NOTICE
/NOTICE.txt
/META-INF/*.DSA (all DSA signature files)
/META-INF/*.EC (all EC signature files)
/META-INF/*.SF (all signature files)
/META-INF/*.RSA (all RSA signature files)
/META-INF/maven/** (all files in the maven meta inf directory)
**/.svn/** (all .svn directory contents)
**/CVS/** (all CVS directory contents)
**/SCCS/** (all SCCS directory contents)
**/.* (all UNIX hidden files)
**/.*/** (all contents of UNIX hidden directories)
**/*~ (temporary files)
**/thumbs.db
**/picasa.ini
**/about.html
**/package.html
**/overview.html
**/_*
**/_*/**
```

我们构建发生这一类冲突时可以通过此类操作解决，譬如：
```gradle
packagingOptions {
    pickFirsts = ['**/libTest.so']
    merges = ['YOUR FILE']
    //譬如一个jar中包含同包名AndroidManifest.xml然后被我们引用就会报AndroidManifest错误，可以exclude操作
    excludes = ['**/classes.dex', '**/AndroidManifest.xml']
}
```

### 3 自定义 lib 参与构建依赖传递

使用 Gradle 我们常见的 lib 依赖方式如下：
```gradle
//批量依赖jar包
compile fileTree(include: ['*.jar'], dir: 'libs')

//依赖一些jar包
compile files('libs/test.jar')

//依赖远程仓库aar或jar包
compile 'com.xxx:zzzz:1.0.0'

//依赖子module工程
compile project(':libmodule')

//依赖本地aar包，需要在repositories中配置flatDir存放本地aar的目录
compile(name: 'testlib', ext: 'aar')
```

依赖常用的基本类型有 provided、compile、compileOnly、api、implementation， provided 和 compileOnly 只编译不打包；其他类型正常情况下如果是主工程则默认会打包；如果是 lib 工程则其依赖的 jar 包默认是远程不会被打包，而本地会被打包，如果依赖的是 aar 包则默认本地远程都不会被打包进去。

一般可以通过 exclude 操作来移除构建依赖，或者通过 transitive 设置不传递依赖，如下案例：
```
dependencies {
     compile ("com.aa:vv:1.0.0") {
        exclude group: 'pp', module: 'tt'
     }
     compile 'com.pp:tt:1.0.0'
 }
```
```
dependencies {
     compile ("com.aa:vv:1.0.0") {
        transitive = false
     }
     compile 'com.pp:tt:1.0.0'
     //other sub depends
 }
```

### 4 自定义 lib 资源合并规则

对于 res 资源想要避免冲突我们可以在构建脚本中添加 resourcePrefix 进行前缀支持，这样就可以避免。

对于 AndroidManifest.xml 的合并冲突可以采取 tools:replace 操作。

### 5 其他

有人可能会说为什么 Java EE 等项目构建遇见多个 jar 包中包含同样包名和文件名的类且只用其中之一时构建不会报错，而 Android 中却会报错？其实原理很简单，Java EE 等项目构建两个 jar 包时会依据指定的优先级顺序合并，第二个 jar 与第一个 jar 冲突的文件会被忽略，所以在 Java EE 等项目中出现该问题时自己要明白保谁谁就得放在前面。而在 Android 中之所以构建会报错是因为 Android 报错是出现在构建的 dex 阶段，也就是将多个 class 合并 dex 时会 merge 冲突，所以对于 Android 这种冲突时要么选择移除处理，要么选择 jarjar 改名，要么还有一个我想到的骚操作就是把冲突 jar 包单独打一个 dex，然后在其他 dex 中用不同的 DexClassLoader （ClassLoader 隔离特性）加载进来反射调用，这样就避开了一个打入一个 dex 冲突的问题。

总之一句话 **珍爱构建，远离本地 jar 包依赖，多用远程仓库依赖。** 无论如何冒昧的修改第三方 SDK 文件是有风险的，除非你明确知道其影响面，否则还是与对方技术支持沟通最为靠谱良策。

### 6 Android APK 构建流程图

概览图
<div><img src=".images/build-process.png""></div>

详细图
<div><img src=".images/Android-Build-Process.png"></div>