package cn.yan.gradle.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 * Created by yan on 18-4-24.
 */

class BuildClassFilterPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create(BuildClassFilterExtension.NAME, BuildClassFilterExtension)
        if (project.plugins.hasPlugin(AppPlugin)) {
            AppExtension android = project.extensions.getByType(AppExtension)
            android.registerTransform(new BuildClassFilterTransform(project))
        } else if(project.plugins.hasPlugin(LibraryPlugin)) {
            LibraryExtension android = project.extensions.getByType(LibraryExtension)
            android.registerTransform(new BuildClassFilterTransform(project))
        }
    }
}
