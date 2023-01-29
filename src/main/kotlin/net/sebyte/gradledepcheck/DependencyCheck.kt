package net.sebyte.gradledepcheck

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.psi.PsiFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.util.application.runReadAction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private val scope = CoroutineScope(Dispatchers.IO)

private val cache = mutableMapOf<String, Boolean>()

private const val BASE_URL = "http://127.0.0.1:8080/check"

private val HttpURLConnection.body: String
    get() {
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.readText()
    }

private fun makeRequest(signature: String): String {
    val url = URL("$BASE_URL/$signature")
    val con = url.openConnection() as HttpURLConnection
    con.requestMethod = "GET"
    try {
        return con.body
    } finally {
        con.disconnect()
    }
}


private fun checkSignature(signature: String): Boolean =
    makeRequest(signature) == "Vulnerable!"

fun checkAndMarkDependency(
    file: PsiFile,
    signature: String,
    mark: (String) -> Unit
) {
    if (cache[signature] == true) {
        mark("This dependency is vulnerable! ($signature)")
    } else {
        scope.launch {
            cache[signature] = async { checkSignature(signature) }.await()
            runReadAction {
                DaemonCodeAnalyzer.getInstance(file.project).restart(file)
            }
        }
    }
}
