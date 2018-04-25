/**
 * MIT License
 *
 * Copyright (c) 2018 yanbo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package cn.yan.gradle.plugin

import com.google.common.io.Files
import org.gradle.api.Project

class BuildClassFilterConfig {
    private static final String BUILD_REPORT_DIR = "buildClassFilter"
    private static final String BUILD_REPORT_FILE = "build-class-filter-report.md"

    private Project mProject

    List<String> configJarExcludeList
    List<String> configSourceExcludeList

    List<String> realJarExcludeList
    List<String> realSourceExcludeList

    BuildClassFilterConfig(Project project) {
        this.mProject = project
        this.realJarExcludeList = new ArrayList<>()
        this.realSourceExcludeList = new ArrayList<>()

        this.configSourceExcludeList = new ArrayList<>()
        this.configJarExcludeList = new ArrayList<>()
    }

    void done() {
        File file = new File(mProject.buildDir.path + File.separator + BUILD_REPORT_DIR + File.separator + BUILD_REPORT_FILE)
        if (file.exists()) {
            file.delete()
        }
        Files.createParentDirs(file)
        file.createNewFile()
        file.withWriter("UTF-8") { writer ->
            String projectInfo = String.format("## Report gradle project is %s.\n\n", mProject.name)
            writer.write(projectInfo)

            writer.write("## SourceExcludes config is:\n")
            configSourceExcludeList.forEach {  pattern ->
                writer.write(pattern + "\n")
            }
            writer.write("\n")

            writer.write("## SourceExcludes remove file is:\n")
            realSourceExcludeList.forEach {  source ->
                writer.write(source + "\n")
            }
            writer.write("\n")

            writer.write("## JarExcludes config is:\n")
            configJarExcludeList.forEach {  pattern ->
                writer.write(pattern + "\n")
            }
            writer.write("\n")

            writer.write("## JarExcludes remove file is:\n")
            realJarExcludeList.forEach {  jar ->
                writer.write(jar + "\n")
            }
            writer.write("\n")
        }
    }
}