/*
 * Copyright (C) 2018 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import javassist.ClassPool
import javassist.CtClass
import javassist.CtNewMethod
import java.io.File

@Suppress("unused")
class MasqueradeUIInjector(private val app: AppExtension, private val startingClass: String) : Transform() {

    override fun getName() = "MasqueradeUIInjector"

    override fun getInputTypes() = mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun getScopes() = mutableSetOf(
        QualifiedContent.Scope.PROJECT,
        QualifiedContent.Scope.EXTERNAL_LIBRARIES,
        QualifiedContent.Scope.SUB_PROJECTS
    )

    override fun isIncremental() = false

    override fun transform(transformInvocation: TransformInvocation) = with (transformInvocation) {
        // Don't transform anything if this is a test APK; just copy inputs and return
        if (context.variantName.endsWith("AndroidTest")) {
            inputs.forEach {
                it.jarInputs.forEach {
                    val dest = outputProvider.getContentLocation(it.name, it.contentTypes, it.scopes, Format.JAR)
                    when (it.status) {
                        Status.REMOVED -> dest.delete()
                        else -> it.file.copyTo(dest, true)
                    }
                }
                it.directoryInputs.forEach {
                    val dest = outputProvider.getContentLocation(it.name, it.contentTypes, it.scopes, Format.DIRECTORY)
                    it.file.copyRecursively(dest, overwrite = true)
                }
            }
            return
        }

        // Create class pool to hold the project's class paths. This must be populated prior to transforming any classes.
        val classPool = ClassPool.getDefault()

        // A list of project classes and their associated File objects, limited to class files with "instructure" in their path.
        val projectClassFiles = mutableListOf<Pair<CtClass, File>>()

        // Copy inputs to their destinations
        inputs.forEach {

            // We won't transform anything in the jar inputs; we just need to add them to the class pool.
            it.jarInputs.forEach {
                classPool.insertClassPath(it.file.absolutePath)
                val dest = outputProvider.getContentLocation(it.name, it.contentTypes, it.scopes, Format.JAR)
                when (it.status) {
                    Status.REMOVED -> dest.delete()
                    else -> it.file.copyTo(dest, true)
                }
            }

            // Copy input directories, add classes to class pool, and extract classes of interest into projectClassFiles
            it.directoryInputs.forEach {
                val output = outputProvider.getContentLocation(it.name, it.contentTypes, it.scopes, Format.DIRECTORY)
                it.file.copyRecursively(output, overwrite = true)
                output.extractClasses(classPool, projectClassFiles)
            }
        }

        // android.jar is a compile-only dependency so we must manually add it to the class pool
        classPool.insertClassPath(File(app.sdkDirectory, "platforms/${app.compileSdkVersion}/android.jar").absolutePath)

        // Add the imports we'll be using
        classPool.importPackage("android.os.Bundle")
        classPool.importPackage("com.instructure.loginapi.login.util.MasqueradeUI")

        // Only transform subclasses of specific types
        val activityClass = classPool.get("android.app.Activity")
        val dialogFragmentClass = classPool.get("android.app.DialogFragment")
        val compatDialogFragmentClass = classPool.get("android.support.v4.app.DialogFragment")

        val targetClassFiles = projectClassFiles.filter { (cts, _) ->
            cts.subclassOf(activityClass)
                    || cts.subclassOf(dialogFragmentClass)
                    || cts.subclassOf(compatDialogFragmentClass)
        }

        val targetClasses: List<CtClass> = targetClassFiles.unzip().first

        targetClassFiles
            .filter { (cts, _) ->
                // Filter out subclasses so we don't add multiple UIs
                cts.superclass !in targetClasses
            }
            .forEach { (ctClass, src) ->
                // Perform transformation
                try {
                    when {
                        ctClass.subclassOf(activityClass) -> ctClass.transformActivity()
                        ctClass.subclassOf(dialogFragmentClass) -> ctClass.transformDialogFragment()
                        ctClass.subclassOf(compatDialogFragmentClass) -> ctClass.transformDialogFragment()
                        else -> throw UnsupportedOperationException("Transforming classes of type ${ctClass.superclass.name} is unsupported.")
                    }
                    src.writeBytes(ctClass.toBytecode())
                } catch (e: Throwable) {
                    println("Error adding masquerade UI to ${src.nameWithoutExtension}")
                    e.printStackTrace()
                    throw e
                }
            }
    }

    /**
     * Extracts classes with "instructure" in their path to [projectClasses] and adds them to the [classPool]
     */
    private fun File.extractClasses(classPool: ClassPool, projectClasses: MutableList<Pair<CtClass, File>>) {
        if (isDirectory) {
            listFiles().forEach { it.extractClasses(classPool, projectClasses) }
        } else if (isFile && extension == "class" && absolutePath.contains("instructure")) {
            projectClasses += classPool.makeClass(this.inputStream(), false) to this
        }
    }

    /**
     * Transforms this class to show the Masquerade UI whenever the user is masquerading.
     * This only works for subclasses of android.app.Activity.
     */
    private fun CtClass.transformActivity() {
        val method = declaredMethods.find { it.name == "onPostCreate" }
        if (method != null) {
            removeMethod(method)
            method.insertBefore("com.instructure.loginapi.login.util.MasqueradeUI.showMasqueradeNotification(this, $startingClass);")
            addMethod(method)
        } else {
            val newMethod = CtNewMethod.make(
                """
                protected void onPostCreate(android.os.Bundle savedInstanceState) {
                    super.onPostCreate(savedInstanceState);
                    com.instructure.loginapi.login.util.MasqueradeUI.showMasqueradeNotification(this, $startingClass);
                }
                """.trimIndent(), this )
            addMethod(newMethod)
        }
    }

    /**
     * Transforms this class to show the Masquerade UI whenever the user is masquerading.
     * This only works for subclasses of android.app.DialogFragment and android.support.v4.app.DialogFragment.
     */
    private fun CtClass.transformDialogFragment() {
        val method = declaredMethods.find { it.name == "onStart" }
        if (method != null) {
            removeMethod(method)
            method.insertBefore("com.instructure.loginapi.login.util.MasqueradeUI.showMasqueradeNotification(this, $startingClass);")
            addMethod(method)
        } else {
            val newMethod = CtNewMethod.make(
                """
                protected void onStart() {
                    super.onStart();
                    com.instructure.loginapi.login.util.MasqueradeUI.showMasqueradeNotification(this, $startingClass);
                }
                """.trimIndent(), this )
            addMethod(newMethod)
        }
    }

}

