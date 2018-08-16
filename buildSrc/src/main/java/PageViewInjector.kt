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
import javassist.*
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.StringMemberValue
import java.io.File

@Suppress("unused")
class PageViewInjector(private val app: AppExtension) : Transform() {

    override fun getName() = "PageViewInjector"

    override fun getInputTypes() = mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun getScopes() = mutableSetOf(
        QualifiedContent.Scope.PROJECT,
        QualifiedContent.Scope.EXTERNAL_LIBRARIES,
        QualifiedContent.Scope.SUB_PROJECTS
    )

    override fun isIncremental() = false

    override fun transform(transformInvocation: TransformInvocation) = with(transformInvocation) {
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
        classPool.importPackage("com.instructure.canvasapi2.utils.pageview.PageViewEvent")
        classPool.importPackage("com.instructure.canvasapi2.utils.pageview.PageViewUtils")
        classPool.importPackage("android.util.Log")

        // Only transform subclasses of specific types
        val activityClass = classPool.get("android.app.Activity")
        val fragmentClass = classPool.get("android.support.v4.app.Fragment")

        projectClassFiles
            .filter { (cts, _) -> cts.hasAnnotation("com.instructure.canvasapi2.utils.pageview.PageView") }
            .forEach { (ctClass, src) ->
                try {
                    when {
                        ctClass.subclassOf(activityClass) -> ctClass.transformActivity()
                        ctClass.subclassOf(fragmentClass) -> ctClass.transformFragment()
                        else -> throw UnsupportedOperationException("Transforming classes of type ${ctClass.superclass.name} is unsupported.")
                    }
                    src.writeBytes(ctClass.toBytecode())
                } catch (e: Throwable) {
                    println("Error injecting PageView into ${src.nameWithoutExtension}")
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

    private val emptyOnResume =
        """
        protected void onResume() {
            super.onResume();
        }
        """

    private val emptyOnPause =
        """
        protected void onPause() {
            super.onPause();
        }
        """

    private val emptyOnHiddenChanged =
        """
        public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);
        }
        """

    private val emptyUserVisibleHint =
        """
        public void setUserVisibleHint(boolean isVisibleToUser) {
            super.setUserVisibleHint(isVisibleToUser);
        }
        """

    private val emptyViewCreated =
        """
        public void onViewCreated(android.view.View view, android.os.Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
        }
        """

    private val emptyWindowFocus =
        """
        public void onPageViewWindowFocusChanged(boolean hasFocus) { }
        """

    private fun getStartLogic(eventFieldName: String, eventName: String, urlGetterName: String, componentName: String) =
        """
        if ($eventFieldName == null && _getPageViewEventName() == "$eventFieldName") {
            android.util.Log.d("PageView", "Started $eventName in $componentName");
            $eventFieldName = com.instructure.canvasapi2.utils.pageview.PageViewUtils.startEvent("$eventName", $urlGetterName());
        }
        """

    private fun getStopLogic(eventFieldName: String, componentName: String) =
        """
        if ($eventFieldName != null) android.util.Log.d("PageView", "Stopped " + $eventFieldName.getEventName() + " in $componentName");
        com.instructure.canvasapi2.utils.pageview.PageViewUtils.stopEvent($eventFieldName);
        $eventFieldName = null;
        """

    /**
     * Transforms this Fragment to add PageView tracking. This only works for subclasses of android.support.v4.fragment
     */
    private fun CtClass.transformFragment() {
        val annotation = getAnnotation("com.instructure.canvasapi2.utils.pageview.PageView")
        val eventName = annotation?.getString("name") ?: simpleName
        val eventFieldName = addEventField(eventName)
        val urlGetterName = addUrlGetter(eventName)
        addVerificationMethod(eventFieldName)

        val prerequisites = declaredMethods.filter { it.hasAnnotation("com.instructure.canvasapi2.utils.pageview.BeforePageView") }
        val prerequisiteParams = when (prerequisites.size) {
            0 -> ""
            else -> prerequisites.joinToString(prefix = "new String[] {", postfix = "}") { "\"${it.name}\"" }
        }

        val trackerClassName = "com.instructure.canvasapi2.utils.pageview.PageViewVisibilityTracker"
        val visibilityTrackerName = "_pageViewVisibilityTracker_$eventName"
        val trackerFieldDeclaration = "$trackerClassName $visibilityTrackerName = new $trackerClassName($prerequisiteParams);"
        addField(CtField.make(trackerFieldDeclaration, this))

        prerequisites.forEach {
            it.insertAfter(
                """
                if ($visibilityTrackerName.trackCustom("${it.name}", true)) {
                    ${getStartLogic(eventFieldName, eventName, urlGetterName, it.name)}
                }
                """
            )
        }

        // Add logic to onHiddenChanged()
        val onHiddenChangedContent =
            """
            if ($visibilityTrackerName.trackHidden($1)) {
                ${getStartLogic(eventFieldName, eventName, urlGetterName, "onHiddenChanged")}
            } else {
                ${getStopLogic(eventFieldName, "onHiddenChanged")}
            }
            """
        addOrUpdateDeclaredMethod("onHiddenChanged", onHiddenChangedContent, emptyOnHiddenChanged)

        // Add logic to setUserVisibleHint()
        val userVisibleContent =
            """
            if ($visibilityTrackerName.trackUserHint($1)) {
                ${getStartLogic(eventFieldName, eventName, urlGetterName, "setUserVisibleHint")}
            } else {
                ${getStopLogic(eventFieldName, "setUserVisibleHint")}
            }
            """
        addOrUpdateDeclaredMethod("setUserVisibleHint", userVisibleContent, emptyUserVisibleHint)

        // Add logic to onResume()
        val onResumeContent =
            """
            if ($visibilityTrackerName.trackResume(true)) {
                ${getStartLogic(eventFieldName, eventName, urlGetterName, "onResume")}
            }
            """
        addOrUpdateDeclaredMethod("onResume", onResumeContent, emptyOnResume)

        // Add logic to onPause()
        val onPauseContent =
            """
            $visibilityTrackerName.trackResume(false);
            ${getStopLogic(eventFieldName, "onPause")}
            """
        addOrUpdateDeclaredMethod("onPause", onPauseContent, emptyOnPause)

        // Fun fact: In 2010, Satan invented Fragments as a joke. He has since apologized for going too far.
        addInterface(classPool["com.instructure.canvasapi2.utils.pageview.PageViewWindowFocus"])
        val onWindowFocusContent =
            """
            if ($visibilityTrackerName.trackCustom("pageViewWindowFocusChanged", $1)) {
                ${getStartLogic(eventFieldName, eventName, urlGetterName, "windowFocusChanged")}
            } else {
                ${getStopLogic(eventFieldName, "windowFocusChanged")}
            }
            """
        addOrUpdateDeclaredMethod("onPageViewWindowFocusChanged", onWindowFocusContent, emptyWindowFocus)

        val viewCreatedContent = "if ($1 != null) $1.getViewTreeObserver().addOnWindowFocusChangeListener(new com.instructure.canvasapi2.utils.pageview.PageViewWindowFocusListener($0));"
        addOrUpdateDeclaredMethod("onViewCreated", viewCreatedContent, emptyViewCreated)
    }

    /**
     * Transforms this Activity to add PageView tracking. This only works for subclasses of android.app.Activity.
     */
    private fun CtClass.transformActivity() {
        val annotation = getAnnotation("com.instructure.canvasapi2.utils.pageview.PageView")
        val eventName = annotation?.getString("name") ?: simpleName
        val eventFieldName = addEventField(eventName)
        val urlGetterName = addUrlGetter(eventName)
        addVerificationMethod(eventFieldName)

        // Add logic to onResume()
        val onResumeContent = getStartLogic(eventFieldName, eventName, urlGetterName, "onResume")
        addOrUpdateDeclaredMethod("onResume", onResumeContent, emptyOnResume)

        // Add logic to onPause()
        val onPauseContent = getStopLogic(eventFieldName, "onPause")
        addOrUpdateDeclaredMethod("onPause", onPauseContent, emptyOnPause)
    }

    private fun CtClass.getAnnotation(name: String): Annotation? {
        return (classFile.getAttribute(AnnotationsAttribute.invisibleTag) as? AnnotationsAttribute)?.getAnnotation(name)
    }

    private fun CtMethod.getAnnotation(name: String): Annotation? {
        return (methodInfo.getAttribute(AnnotationsAttribute.invisibleTag) as? AnnotationsAttribute)?.getAnnotation(name)
    }

    private fun CtField.getAnnotation(name: String): Annotation? {
        return (fieldInfo.getAttribute(AnnotationsAttribute.invisibleTag) as? AnnotationsAttribute)?.getAnnotation(name)
    }

    private fun Annotation.getString(memberName: String): String? {
        return (getMemberValue(memberName) as? StringMemberValue)?.value
    }

    private fun CtClass.addUrlGetter(eventName: String): String {
        val getterName = "_getEventUrl_$eventName"
        var urlMethodBody: String? = null

        declaredMethods.find { it.hasAnnotation("com.instructure.canvasapi2.utils.pageview.PageViewUrl") }?.let {
            urlMethodBody = "return ${it.name}();"
        }

        if (urlMethodBody == null) {
            val url = getAnnotation("com.instructure.canvasapi2.utils.pageview.PageView")?.getString("url").orEmpty()

            val paramMethods = declaredMethods
                .filter { it.hasAnnotation("com.instructure.canvasapi2.utils.pageview.PageViewUrlParam") }
                .associate {
                    val key = it.getAnnotation("com.instructure.canvasapi2.utils.pageview.PageViewUrlParam")?.getString("name")
                            ?: throw IllegalArgumentException("Invalid PageViewUrlParam name for ${it.name} in $simpleName")
                    key to "${it.name}()"
                }

            val paramFields = declaredFields
                .filter { it.hasAnnotation("com.instructure.canvasapi2.utils.pageview.PageViewUrlParam") }
                .associate {
                    val key = it.getAnnotation("com.instructure.canvasapi2.utils.pageview.PageViewUrlParam")?.getString("name")
                            ?: throw IllegalArgumentException("Invalid PageViewUrlParam name for ${it.name} in $simpleName")
                    key to it.name
                }

            val paramMap = (paramMethods + paramFields).toMutableMap()

            val queryMethods = declaredMethods
                .filter { it.hasAnnotation("com.instructure.canvasapi2.utils.pageview.PageViewUrlQuery") }
                .associate {
                    val key = it.getAnnotation("com.instructure.canvasapi2.utils.pageview.PageViewUrlQuery")?.getString("name")
                            ?: throw IllegalArgumentException("Invalid PageViewUrlParam name for ${it.name} in $simpleName")
                    key to "${it.name}()"
                }

            val queryFields = declaredFields
                .filter { it.hasAnnotation("com.instructure.canvasapi2.utils.pageview.PageViewUrlQuery") }
                .associate {
                    val key = it.getAnnotation("com.instructure.canvasapi2.utils.pageview.PageViewUrlQuery")?.getString("name")
                            ?: throw IllegalArgumentException("Invalid PageViewUrlParam name for ${it.name} in $simpleName")
                    key to it.name
                }

            val queryMap = (queryMethods + queryFields).toMutableMap()

            if (url.isEmpty() && queryMap.isEmpty()) {
                urlMethodBody = "return com.instructure.canvasapi2.utils.ApiPrefs.getFullDomain();"
            } else {
                require(url.count { it == '{' } == url.count { it == '}' }) { "PageView url $url is incorrectly formatted" }
                val params = url.split("/").filter { it.startsWith('{') && it.endsWith('}') }.map { it.trim('{', '}') }

                val paramSwappers = params.map {
                    // Attempt to use getCanvasContext() if there is no getter specified for the 'canvasContext' param
                    if (it == "canvasContext" && paramMap[it] == null) {
                        if (methods.any { it.name == "getCanvasContext" }){
                            return@map """rawUrl = rawUrl.replace("{$it}", getCanvasContext().getContextId().replace("_", "s/"));"""
                        } else {
                            throw IllegalArgumentException( "Missing getter for PageView url parameter '$it' in " +
                                    "$simpleName. Ensure that either a function named 'getCanvasContext()' exists " +
                                    "or that exactly one property or function in $simpleName has the annotation " +
                                    "@PageViewUrlParam(name=\"$it\").")
                        }
                    }
                    val paramCall = paramMap[it]
                            ?: throw IllegalArgumentException("Missing getter for PageView url parameter '$it' in " +
                                    "$simpleName. Ensure that exactly one property or function in $simpleName has the " +
                                    "annotation @PageViewUrlParam(name=\"$it\").")
                    paramMap -= it
                    """rawUrl = rawUrl.replace("{$it}", String.valueOf($paramCall));"""
                }

                val querySwappers = queryMap.map { (queryName, queryGetter) ->
                    if (queryName.isBlank()) throw IllegalArgumentException("'name' cannot be blank for @PageViewUrlQuery annotation on member $queryGetter in $simpleName")
                    val fieldName = "_pageViewQuery_${eventName}_$queryName"

                    """
                    String $fieldName = String.valueOf($queryGetter);
                    if ($fieldName != null && !"null".equals($fieldName) && !$fieldName.isEmpty()) {
                        queries.put("$queryName", $fieldName);
                    }
                    """
                }

                urlMethodBody =
                        """
                        String domain = com.instructure.canvasapi2.utils.ApiPrefs.getFullDomain();

                        String rawUrl = "$url";
                        ${paramSwappers.joinToString("\n")}

                        java.util.HashMap queries = new java.util.HashMap();
                        ${querySwappers.joinToString("\n")}
                        java.lang.StringBuilder queryString = new java.lang.StringBuilder();
                        if (!queries.isEmpty()) {
                            java.util.Iterator keyIterator = queries.keySet().iterator();
                            while (keyIterator.hasNext()) {
                                String key = (String) keyIterator.next();
                                String value = (String) queries.get(key);
                                queryString.append(queryString.length() == 0 ? '?' : '&');
                                queryString.append(key);
                                queryString.append('=');
                                queryString.append(value);
                            }
                        }

                        return com.instructure.canvasapi2.utils.ApiPrefs.getFullDomain() + "/" + rawUrl + queryString.toString();
                        """
            }

            paramMap.forEach { (key, value) ->
                println("    WARNING: Unused PageViewUrlParam '$key' on member $value in $simpleName")
            }
        }

        if (urlMethodBody == null) throw RuntimeException("Unable to create PageView url method in $simpleName")

        val methodString = """
            private String $getterName() {
                $urlMethodBody
            }
            """
        val urlMethod = CtMethod.make(methodString, this)
        addMethod(urlMethod)
        return getterName
    }

    private fun CtClass.addEventField(eventName: String): String {
        val fieldName = "_pageView_$eventName"
        addField(CtField.make("com.instructure.canvasapi2.utils.pageview.PageViewEvent $fieldName = null;", this))
        return fieldName
    }

    private fun CtClass.addVerificationMethod(eventFieldName: String) {
        val eventVerificationMethod = CtNewMethod.make(
            """
            public String _getPageViewEventName() {
                return "$eventFieldName";
            }
            """.trimIndent(), this)
        addMethod(eventVerificationMethod)
    }

    private fun CtClass.addOrUpdateDeclaredMethod(methodName: String, content: String, defaultMethodBody: String) {
        val method = declaredMethods.find { it.name == methodName }
                ?: CtNewMethod.make(defaultMethodBody, this).also { addMethod(it) }
        method.insertBefore(content)
    }

}

