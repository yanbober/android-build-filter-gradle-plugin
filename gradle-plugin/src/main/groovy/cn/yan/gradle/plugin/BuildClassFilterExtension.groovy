package cn.yan.gradle.plugin
/**
 * Created by yan on 18-4-24.
 */

class BuildClassFilterExtension {
    public static final String NAME = "buildClassFilter"

    List<String> jarExcludes = new ArrayList<>()
    List<String> sourceExcludes = new ArrayList<>()
}
