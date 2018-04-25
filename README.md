# android-build-filter-gradle-plugin

[![](https://jitpack.io/v/yanbober/android-build-filter-gradle-plugin.svg)](https://jitpack.io/#yanbober/android-build-filter-gradle-plugin)

一个快捷移除 Android 构建中 Jar 包、AAR 包、构建冗余 class 文件的 Gradle 插件。

# 配置

在相应 gradle 文件添加如下仓库。

```gradle
buildscript {
    repositories {
        maven {
            if (project.properties.useLocal == 'true') {
                url uri('../repo')
            } else {
                url 'https://jitpack.io'
            }
        }
    }

    dependencies {
        classpath 'com.github.yanbober:android-build-filter-gradle-plugin:1.0.1'
    }
}
```

在对应模块 build.gradle 文件应用插件。

```gradle
apply plugin: 'buildfilter'

//或者

apply plugin: 'android.build.filter.gradle'
```

# 使用

将想要移除的 class 文件添加到对应配置中，如下：
```
apply plugin: 'buildfilter'

buildClassFilter {
    sourceExcludes = [
            'cn/yan/gradle/plugin/BuildConfig.class'
    ]

    jarExcludes = [ 'a.class', 'b.class' ]
}
```

其中 class 全限定描述名支持标准正则匹配。
sourceExcludes 中配置你项目中源码生成或者 IDE 自动生成的 class 文件(譬如 BuildConfig)。
jarExcludes 中配置你项目依赖的 aar 或者 jar 包中你想移除的 class 文件。

# 校验与追踪

按照如上配置后进行项目构建编译，编译后你可以在你配置 buildClassFilter 的模块的对应构建输出目录中找到校验文件。默认输出为当前 module 下 build 目录，你可以在 build/buildClassFilter/ 目录下找到 build-class-filter-report.md 文件。

build-class-filter-report.md 文件格式如下：
```
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

# 插件应用场景

- 由于历史原因等情况原本多个 module 包名一致，现在放一起使用又不想重命名包名，所以对于安卓 lib module 来说就会存在构建合并时提醒 BuildConfig 等 class 重复问题，使用 sourceExcludes 即可轻松解决。

- 引用外部第三方 SDK，譬如有些 SDK 内部多打包了一个同包名 BuildConfig 等文件，我们想用该 SDK 就会冲突，笨办法就是解压 SDK 删除文件再把 SDK 压缩使用，而先进的办法就是使用 jarExcludes 即可轻松解决。

- 有空了补

# 注意

- 混淆问题。

- 由于所有的 class exclude 操作是构建生成 class 文件后删除的，所以即便 class 不存在也能打包成功，所以你需要非常清楚自己在干什么，一定要将构建校验 report 文件查看被删除的 class 是不是你期望的。