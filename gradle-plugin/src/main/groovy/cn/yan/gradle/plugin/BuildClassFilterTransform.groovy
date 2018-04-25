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

import com.android.build.api.transform.*
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.google.common.collect.Sets
import com.google.common.io.ByteStreams
import org.gradle.api.Project
import java.util.regex.Pattern
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BuildClassFilterTransform extends Transform {
    private static final String NAME = "BuildClassFilter"

    private Project mProject
    private BuildClassFilterConfig mConfig

    BuildClassFilterTransform(Project project) {
        mProject = project
        mConfig = new BuildClassFilterConfig(project)
        initAfterEvaluate(project)
    }

    private void initAfterEvaluate(Project project) {
        project.afterEvaluate {
            BuildClassFilterExtension extension = project.extensions.getByName(BuildClassFilterExtension.NAME)
            if (extension != null) {
                if (extension.jarExcludes != null) {
                    mConfig.configJarExcludeList = extension.jarExcludes.stream()
                            .filter { !it.isAllWhitespace() }
                            .collect()
                }

                if (extension.sourceExcludes != null) {
                    mConfig.configSourceExcludeList = extension.sourceExcludes.stream()
                            .filter { !it.isAllWhitespace() }
                            .collect()
                }
            }
        }
    }

    @Override
    String getName() {
        return NAME
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        if (mProject.plugins.hasPlugin(AppPlugin)) {
            return TransformManager.SCOPE_FULL_PROJECT
        } else if (mProject.plugins.hasPlugin(LibraryPlugin)) {
            return Sets.immutableEnumSet(QualifiedContent.Scope.PROJECT)
        }
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        TransformOutputProvider outputProvider = transformInvocation.outputProvider
        if (!transformInvocation.isIncremental()) {
            outputProvider.deleteAll()
        }

        transformInvocation.inputs.forEach { TransformInput input ->
            input.jarInputs.forEach { JarInput jarInput ->
                File targetJar = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                excludeFilesFromJar(jarInput.file, targetJar)
            }

            input.directoryInputs.forEach { DirectoryInput directoryInput ->
                File targetDir = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                if (targetDir.exists()) {
                    FileUtils.deleteDirectoryContents(targetDir)
                }
                targetDir.mkdirs()

                List<String> targetList = new ArrayList<>()
                getDeepDirFileList(directoryInput.file, targetList)
                excludeFilesFromDirectory(directoryInput.file.absolutePath, targetList, mConfig.configSourceExcludeList)

                FileUtils.copyDirectory(directoryInput.file, targetDir)
            }
        }

        mConfig.done()
    }

    private void getDeepDirFileList(File dir, List<String> targetList) {
        File[] dirList = dir.listFiles()
        for (File indexFile : dirList) {
            if (indexFile.isDirectory()) {
                getDeepDirFileList(indexFile, targetList)
            } else {
                targetList.add(indexFile.absolutePath)
            }
        }
    }

    private boolean matchPatternFile(List<String> patternList, String file) {
        if (patternList == null || file == null) {
            return false
        }

        for (String pattern: patternList) {
            if (Pattern.compile(pattern).matcher(file).matches()) {
                return true
            }
        }
        return false
    }

    private void excludeFilesFromJar(File srcJar, File targetJar) {
        if (targetJar.exists()) {
            targetJar.delete()
        }
        targetJar.createNewFile()

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(srcJar))
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(targetJar))
        def zipEntry = null
        while ((zipEntry = zipInputStream.nextEntry) != null) {
            if (matchPatternFile(mConfig.configJarExcludeList, zipEntry.name)) {
                mConfig.realJarExcludeList.add(srcJar.name+" --> remove -> "+zipEntry.name)
                continue
            }
            zipOutputStream.putNextEntry(zipEntry)
            ByteStreams.copy(zipInputStream, zipOutputStream)
            zipOutputStream.closeEntry()
        }
        zipInputStream.close()
        zipOutputStream.close()
    }

    private void excludeFilesFromDirectory(String dirAbsolutePath, List<String> dirFileList, List<String> patternList) {
        dirFileList.forEach {
            String relativePath = it.substring(dirAbsolutePath.length()+1)
            if (matchPatternFile(patternList, relativePath)) {
                File delFile = new File(it)
                delFile.delete()
                mConfig.realSourceExcludeList.add(delFile.path)
            }
        }
    }
}
