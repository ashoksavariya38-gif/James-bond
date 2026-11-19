package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLuminaColors

private sealed interface RichBlock {
  data class Paragraph(val text: String) : RichBlock
  data class Heading(val level: Int, val text: String) : RichBlock
  data class Bullet(val text: String) : RichBlock
  data class CodeBlock(val language: String, val code: String) : RichBlock
  data class MathBlock(val latex: String) : RichBlock
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkdownMathText(
  content: String,
  sourceUrls: List<String> = emptyList(),
  modifier: Modifier = Modifier,
) {
  val luminaColors = LocalLuminaColors.current
  val clipboard = LocalClipboardManager.current
  val uriHandler = LocalUriHandler.current

  val blocks = remember(content) { parseMarkdownAndMath(content) }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    blocks.forEach { block ->
      when (block) {
        is RichBlock.Heading -> {
          val style = when (block.level) {
            1 -> MaterialTheme.typography.titleLarge
            2 -> MaterialTheme.typography.titleMedium
            else -> MaterialTheme.typography.titleSmall
          }
          Text(
            text = renderInlineRichText(block.text, luminaColors.brandBlue, luminaColors.codeSurface),
            style = style.copy(color = MaterialTheme.colorScheme.onSurface),
          )
        }

        is RichBlock.Paragraph -> {
          val annotated = renderInlineRichText(
            text = block.text,
            linkColor = luminaColors.brandCyan,
            inlineCodeBg = luminaColors.codeSurface,
          )
          @Suppress("DEPRECATION")
          ClickableText(
            text = annotated,
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            onClick = { offset ->
              annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                  runCatching { uriHandler.openUri(annotation.item) }
                }
            },
          )
        }

        is RichBlock.Bullet -> {
          Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Text(
              text = "•",
              style = MaterialTheme.typography.bodyLarge.copy(
                color = luminaColors.brandPurple,
                fontWeight = FontWeight.Bold,
              ),
            )
            val annotated = renderInlineRichText(
              text = block.text,
              linkColor = luminaColors.brandCyan,
              inlineCodeBg = luminaColors.codeSurface,
            )
            @Suppress("DEPRECATION")
            ClickableText(
              text = annotated,
              style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
              modifier = Modifier.weight(1f),
              onClick = { offset ->
                annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                  .firstOrNull()?.let { annotation ->
                    runCatching { uriHandler.openUri(annotation.item) }
                  }
              },
            )
          }
        }

        is RichBlock.MathBlock -> {
          Surface(
            color = luminaColors.mathSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, luminaColors.brandPurple.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Functions,
                contentDescription = "Math equation",
                tint = luminaColors.brandPurple,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = formatLatexReadable(block.latex),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
              )
            }
          }
        }

        is RichBlock.CodeBlock -> {
          Surface(
            color = luminaColors.codeSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, luminaColors.borderSubtle),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(luminaColors.borderSubtle.copy(alpha = 0.4f))
                  .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = block.language.ifBlank { "code" }.uppercase(),
                  style = MaterialTheme.typography.labelSmall,
                  color = luminaColors.textSecondary,
                )
                IconButton(
                  onClick = { clipboard.setText(AnnotatedString(block.code)) },
                  modifier = Modifier.size(32.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code block",
                    tint = luminaColors.textSecondary,
                    modifier = Modifier.size(15.dp),
                  )
                }
              }
              Text(
                text = highlightCodeSnippet(block.code, luminaColors.brandCyan, luminaColors.brandPurple, luminaColors.success),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
                  .padding(12.dp),
              )
            }
          }
        }
      }
    }

    if (sourceUrls.isNotEmpty()) {
      Spacer(Modifier.height(2.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        sourceUrls.distinct().take(6).forEach { url ->
          val hostLabel = remember(url) {
            runCatching { java.net.URI(url).host?.removePrefix("www.") ?: url }.getOrDefault(url)
          }
          Surface(
            color = luminaColors.elevatedCard,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, luminaColors.borderSubtle),
            modifier = Modifier.clickable { runCatching { uriHandler.openUri(url) } },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = luminaColors.brandCyan,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = hostLabel.take(28),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open source",
                tint = luminaColors.textSecondary,
                modifier = Modifier.size(12.dp),
              )
            }
          }
        }
      }
    }
  }
}

private fun parseMarkdownAndMath(raw: String): List<RichBlock> {
  val lines = raw.replace("\r\n", "\n").lines()
  val result = mutableListOf<RichBlock>()
  var i = 0
  while (i < lines.size) {
    val line = lines[i]
    val trimmed = line.trim()

    if (trimmed.startsWith("```")) {
      val lang = trimmed.removePrefix("```").trim()
      val codeLines = mutableListOf<String>()
      i++
      while (i < lines.size && !lines[i].trim().startsWith("```")) {
        codeLines.add(lines[i])
        i++
      }
      result.add(RichBlock.CodeBlock(lang, codeLines.joinToString("\n")))
      i++
      continue
    }

    if (trimmed.startsWith("$$")) {
      val singleLine = trimmed.removePrefix("$$")
      if (singleLine.endsWith("$$") && singleLine.length > 2) {
        result.add(RichBlock.MathBlock(singleLine.removeSuffix("$$").trim()))
        i++
        continue
      }
      val mathLines = mutableListOf<String>()
      if (singleLine.isNotBlank()) mathLines.add(singleLine)
      i++
      while (i < lines.size && !lines[i].trim().endsWith("$$")) {
        mathLines.add(lines[i])
        i++
      }
      if (i < lines.size) {
        val last = lines[i].trim().removeSuffix("$$").trim()
        if (last.isNotBlank()) mathLines.add(last)
      }
      result.add(RichBlock.MathBlock(mathLines.joinToString("\n")))
      i++
      continue
    }

    when {
      trimmed.isEmpty() -> Unit
      trimmed.startsWith("### ") -> result.add(RichBlock.Heading(3, trimmed.removePrefix("### ").trim()))
      trimmed.startsWith("## ") -> result.add(RichBlock.Heading(2, trimmed.removePrefix("## ").trim()))
      trimmed.startsWith("# ") -> result.add(RichBlock.Heading(1, trimmed.removePrefix("# ").trim()))
      trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
        result.add(RichBlock.Bullet(trimmed.drop(2).trim()))
      }
      else -> result.add(RichBlock.Paragraph(line))
    }
    i++
  }
  return result
}

private fun renderInlineRichText(
  text: String,
  linkColor: androidx.compose.ui.graphics.Color,
  inlineCodeBg: androidx.compose.ui.graphics.Color,
): AnnotatedString {
  val formatted = formatLatexReadable(text)
  return buildAnnotatedString {
    val tokenRegex = Regex("""(\*\*[^*]+\*\*|`[^`]+`|\[[^\]]+\]\(https?://[^)]+\)|https?://\S+)""")
    var cursor = 0
    for (match in tokenRegex.findAll(formatted)) {
      if (match.range.first > cursor) {
        append(formatted.substring(cursor, match.range.first))
      }
      val token = match.value
      when {
        token.startsWith("**") && token.endsWith("**") -> {
          withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(token.removeSurrounding("**"))
          }
        }
        token.startsWith("`") && token.endsWith("`") -> {
          withStyle(
            SpanStyle(
              fontFamily = FontFamily.Monospace,
              background = inlineCodeBg,
              fontSize = 13.sp,
            )
          ) {
            append(" ${token.removeSurrounding("`")} ")
          }
        }
        token.startsWith("[") && token.contains("](") -> {
          val label = token.substringAfter("[").substringBefore("]")
          val url = token.substringAfter("](").substringBefore(")")
          pushStringAnnotation(tag = "URL", annotation = url)
          withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
            append(label)
          }
          pop()
        }
        token.startsWith("http") -> {
          val cleanUrl = token.trimEnd('.', ',', ')', ';')
          pushStringAnnotation(tag = "URL", annotation = cleanUrl)
          withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
            append(cleanUrl)
          }
          pop()
        }
      }
      cursor = match.range.last + 1
    }
    if (cursor < formatted.length) {
      append(formatted.substring(cursor))
    }
  }
}

private fun highlightCodeSnippet(
  code: String,
  keywordColor: androidx.compose.ui.graphics.Color,
  typeColor: androidx.compose.ui.graphics.Color,
  stringColor: androidx.compose.ui.graphics.Color,
): AnnotatedString = buildAnnotatedString {
  val regex = Regex("""("[^"\n]*"|'[^'\n]*'|\b(fun|val|var|class|def|return|if|else|for|while|import|const|let|function|async|await)\b|\b[A-Z][A-Za-z0-9_]*\b)""")
  var cursor = 0
  for (match in regex.findAll(code)) {
    if (match.range.first > cursor) append(code.substring(cursor, match.range.first))
    val tok = match.value
    when {
      tok.startsWith("\"") || tok.startsWith("'") ->
        withStyle(SpanStyle(color = stringColor)) { append(tok) }
      tok in setOf("fun", "val", "var", "class", "def", "return", "if", "else", "for", "while", "import", "const", "let", "function", "async", "await") ->
        withStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold)) { append(tok) }
      else ->
        withStyle(SpanStyle(color = typeColor)) { append(tok) }
    }
    cursor = match.range.last + 1
  }
  if (cursor < code.length) append(code.substring(cursor))
}

private fun formatLatexReadable(raw: String): String {
  var out = raw
  // Strip inline $...$ delimiters cleanly while keeping equation readable
  out = out.replace(Regex("""\$([^$]+)\$""")) { it.groupValues[1] }
  val replacements = listOf(
    "\\frac{" to "(",
    "}{" to ")/(",
    "\\sqrt{" to "√(",
    "\\sum" to "∑",
    "\\int" to "∫",
    "\\prod" to "∏",
    "\\infty" to "∞",
    "\\pi" to "π",
    "\\alpha" to "α",
    "\\beta" to "β",
    "\\gamma" to "γ",
    "\\delta" to "δ",
    "\\Delta" to "Δ",
    "\\theta" to "θ",
    "\\lambda" to "λ",
    "\\mu" to "μ",
    "\\sigma" to "σ",
    "\\omega" to "ω",
    "\\leq" to "≤",
    "\\geq" to "≥",
    "\\neq" to "≠",
    "\\approx" to "≈",
    "\\times" to "×",
    "\\cdot" to "·",
    "\\pm" to "±",
    "\\rightarrow" to "→",
    "\\Rightarrow" to "⇒",
    "\\leftarrow" to "←",
    "\\partial" to "∂",
    "\\nabla" to "∇",
  )
  replacements.forEach { (k, v) -> out = out.replace(k, v) }
  return out
}
