package net.sebyte.gradledepcheck

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.callName
import org.jetbrains.kotlin.idea.extensions.gradle.SCRIPT_PRODUCTION_DEPENDENCY_STATEMENTS
import org.jetbrains.kotlin.idea.groovy.inspections.KotlinGradleInspectionVisitor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.plugins.groovy.codeInspection.BaseInspection
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression

private const val DEPENDENCIES_CALL_NAME = "dependencies"

private fun String.withoutQuotes(): String = if (length > 2) substring(1, length - 1) else ""

class KotlinDependencyInspection : AbstractKotlinInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): PsiElementVisitor = object : KtVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
            super.visitCallExpression(expression)

            val dependenciesCall = expression.getStrictParentOfType<KtCallExpression>() ?: return
            if (dependenciesCall.callName() != DEPENDENCIES_CALL_NAME) return
            if (expression.callName() !in SCRIPT_PRODUCTION_DEPENDENCY_STATEMENTS) return

            val signature = expression.dependencySignature
            checkAndMarkDependency(expression.containingFile, signature) {
                holder.registerProblem(expression, it, ProblemHighlightType.WARNING)
            }
        }

        private val KtCallExpression.dependencySignature: String
            get() = valueArguments
                .mapNotNull { it.getArgumentExpression() }
                .filterIsInstance<KtStringTemplateExpression>()
                .joinToString(":") { it.text.withoutQuotes() }
    }
}

class GroovyDependencyInspection : BaseInspection() {
    override fun buildVisitor(): BaseInspectionVisitor = object : KotlinGradleInspectionVisitor() {
        override fun visitCallExpression(callExpression: GrCallExpression) {
            super.visitCallExpression(callExpression)

            val dependenciesCall = callExpression.getStrictParentOfType<GrMethodCall>() ?: return
            if (dependenciesCall.invokedExpression.text != DEPENDENCIES_CALL_NAME) return
            val referenceExpression = callExpression.getChildrenOfType<GrReferenceExpression>().firstOrNull() ?: return
            if (referenceExpression.text !in SCRIPT_PRODUCTION_DEPENDENCY_STATEMENTS) return

            val signature = callExpression.dependencySignature
            if (signature != null) checkAndMarkDependency(callExpression.containingFile, signature) {
                registerError(callExpression, it, emptyArray(), ProblemHighlightType.WARNING)
            }
        }

        private val GrCallExpression.dependencySignature: String?
            get() = argumentList?.allArguments
                ?.joinToString(":") { it.text.withoutQuotes() }
    }
}
