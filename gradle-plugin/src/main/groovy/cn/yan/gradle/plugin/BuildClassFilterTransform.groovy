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
/**
 * Created by yan on 18-4-24.
 */

class BuildClassFilterTransform extends Transform {
    private static final String NAME = "BuildClassFilterTransform"
    private List<Pattern> mJarExcludeList
    private List<Pattern> mSourceExcludeList
    private Project mProject

    BuildClassFilterTransform(Project project) {
        mProject = project
        project.afterEvaluate {
            BuildClassFilterExtension extension = project.extensions.getByName(BuildClassFilterExtension.NAME)
            if (extension == null) {
                return
            }

            if (extension.jarExcludes != null) {
                System.out.println(project.name+":jarExcludes==============="+extension.jarExcludes)
                mJarExcludeList = extension.jarExcludes.stream()
                        .filter { !it.isAllWhitespace() }
                        .map { Pattern.compile(it) }
                        .collect()
            }

            if (extension.sourceExcludes != null) {
                System.out.println(project.name+":sourceExcludes==============="+extension.sourceExcludes)
                mSourceExcludeList = extension.sourceExcludes.stream()
                        .filter { !it.isAllWhitespace() }
                        .map { Pattern.compile(it) }
                        .collect()
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
//        if (mJarExcludeList == null || mJarExcludeList.size() == 0) {
//            super.transform(transformInvocation)
//            return
//        }
        println("transform---------------------------------------------")

        TransformOutputProvider outputProvider = transformInvocation.outputProvider
        if (!transformInvocation.isIncremental()) {
            outputProvider.deleteAll()
        }

        transformInvocation.inputs.forEach { TransformInput input ->
            input.jarInputs.forEach { JarInput jarInput ->
                File targetJar = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                System.out.println("-2--------------------src="+jarInput.file.path+", target="+targetJar.path)
                excludeFilesFromJar(jarInput.file, targetJar)
            }

            input.directoryInputs.forEach { DirectoryInput directoryInput ->
                File targetDir = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                if (targetDir.exists()) {
                    FileUtils.deleteDirectoryContents(targetDir)
                }
                targetDir.mkdirs()
                System.out.println(mProject.name+"--->2>>----------directoryInput="+directoryInput.file.path)

                List<String> targetList = new ArrayList<>()
                getDeepDirFileList(directoryInput.file, targetList)
                excludeFilesFromDirectory(directoryInput.file.absolutePath, targetList, mSourceExcludeList)

                FileUtils.copyDirectory(directoryInput.file, targetDir)
            }
        }
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

    private boolean matchPatternFile(List<Pattern> patternList, String file) {
        if (patternList == null || file == null) {
            return false
        }

        for (Pattern pattern: patternList) {
            if (pattern.matcher(file).matches()) {
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
            if (matchPatternFile(mJarExcludeList, zipEntry.name)) {
                System.out.println(mProject.name+":jar skip("+zipEntry.name+")----"+srcJar.absolutePath)
                continue
            }
            zipOutputStream.putNextEntry(zipEntry)
            ByteStreams.copy(zipInputStream, zipOutputStream)
            zipOutputStream.closeEntry()
        }
        zipInputStream.close()
        zipOutputStream.close()
    }

    private void excludeFilesFromDirectory(String dirAbsolutePath, List<String> dirFileList, List<Pattern> patternList) {
        dirFileList.forEach {
            String relativePath = it.substring(dirAbsolutePath.length()+1)
//            System.out.println("relativePath..."+relativePath)
            if (matchPatternFile(patternList, relativePath)) {
                File delFile = new File(it)
                delFile.delete()
                System.out.println(mProject.name+":delete--------------"+delFile.absolutePath)
            }
        }
    }
}
