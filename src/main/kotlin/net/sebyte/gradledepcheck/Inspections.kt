package net.sebyte.gradledepcheck

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.callName
import org.jetbrains.kotlin.idea.extensions.gradle.SCRIPT_PRODUCTION_DEPENDENCY_STATEMENTS
import org.jetbrains.kotlin.idea.groovy.inspections.GradleHeuristicHelper
import org.jetbrains.kotlin.idea.groovy.inspections.KotlinGradleInspectionVisitor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.plugins.groovy.codeInspection.BaseInspection
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
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

            val dependencyText = expression.dependencyText
            holder.registerProblem(expression, dependencyText, ProblemHighlightType.WARNING)
        }

        private val KtCallExpression.dependencyText: String
            get() = valueArguments
                .mapNotNull { it.getArgumentExpression() }
                .filterIsInstance<KtStringTemplateExpression>()
                .joinToString(":") { it.text.withoutQuotes() }
    }
}

class GroovyDependencyInspection : BaseInspection() {
    override fun buildVisitor(): BaseInspectionVisitor = object : KotlinGradleInspectionVisitor() {
        override fun visitClosure(closure: GrClosableBlock) {
            super.visitClosure(closure)

            val dependenciesCall = closure.getStrictParentOfType<GrMethodCall>() ?: return
            if (dependenciesCall.invokedExpression.text != DEPENDENCIES_CALL_NAME) return

            val dependencies = GradleHeuristicHelper.findStatementWithPrefixes(
                closure, SCRIPT_PRODUCTION_DEPENDENCY_STATEMENTS
            )
            for (dependency in dependencies) {
                visitDependency(dependency)
            }
        }

        private fun visitDependency(dependency: GrCallExpression) {
            val dependencyText = dependency.dependencyText
            if (dependencyText != null) registerError(
                dependency, dependencyText, emptyArray(), ProblemHighlightType.WARNING
            )
        }

        private val GrCallExpression.dependencyText: String?
            get() = argumentList?.allArguments
                ?.joinToString(":") { it.text.withoutQuotes() }
    }
}
