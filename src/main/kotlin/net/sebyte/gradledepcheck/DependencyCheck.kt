package net.sebyte.gradledepcheck

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.psi.PsiFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.util.application.runReadAction

private val scope = CoroutineScope(Dispatchers.IO)

private val cache = mutableMapOf<String, Boolean>()

fun checkAndMarkDependency(
    file: PsiFile,
    signature: String,
    mark: (String) -> Unit
) {
    if (cache[signature] == true) {
        mark(signature)
    } else {
        scope.launch {
            delay(1000) // simulate checking the dependency
            cache[signature] = true
            runReadAction {
                DaemonCodeAnalyzer.getInstance(file.project).restart(file)
            }
        }
    }
}
