package com.lenovo.quantum.test.shell.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lenovo.quantum.test.shell.models.ToolCategory
import com.lenovo.quantum.test.shell.models.ToolDescriptor
//import sun.tools.jconsole.LabeledComponent.layout

@Composable
fun CategoryHeader(title: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = rememberVectorPainter(Icons.Default.List),
            contentDescription = null
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        CountBadge(text = "$count tools")
    }
}

@Composable
fun ToolCard(
    tool: ToolDescriptor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            // Top row: icon + name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = rememberVectorPainter(Icons.Default.Build),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    tool.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(10.dp))

            // Description
            Text(
                tool.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )

            Spacer(Modifier.weight(1f))

            // Footer: category label + args badge
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Default.MenuBook),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        tool.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                CountBadge(text = "args: ${tool.argsCount}")
            }
        }
    }
}

@Composable
fun CountBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Simple adaptive grid that lays out cards in rows based on a minimum cell size.
 * Works on Android & Desktop without extra libs.
 */
@Composable
fun <T> AdaptiveCardGrid(
    items: List<T>,
    minCellSize: Dp,
    itemContent: @Composable (T) -> Unit,
    rowSpacing: Dp = 16.dp,
    colSpacing: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Layout(
        content = {
            items.forEach { item ->
                Box { itemContent(item) }
            }
        },
        modifier = modifier.fillMaxWidth()
    ) { measurables, constraints ->
        val maxWidth = constraints.maxWidth.toFloat()
        val minPx = minCellSize.roundToPx()
        val spacingPx = colSpacing.roundToPx()

        // compute columns that fit
        val cols = maxOf(1, ((maxWidth + spacingPx) / (minPx + spacingPx)).toInt())
        val cellWidth = ((maxWidth - spacingPx * (cols - 1)) / cols).toInt()

        val placeables = measurables.map { measurable ->
            val c = constraints.copy(
                minWidth = 0,
                maxWidth = cellWidth
            )
            measurable.measure(c)
        }

        val rowHeights = mutableListOf<Int>()
        var i = 0
        while (i < placeables.size) {
            val row = placeables.subList(i, minOf(i + cols, placeables.size))
            rowHeights += row.maxOf { it.height }
            i += cols
        }

        val height = rowHeights.sum() + rowSpacing.roundToPx() * (rowHeights.size - 1).coerceAtLeast(0)
        layout(constraints.maxWidth, height.coerceAtLeast(0)) {
            var y = 0
            var index = 0
            rowHeights.forEach { rowH ->
                var x = 0
                repeat(cols) {
                    if (index < placeables.size) {
                        val p = placeables[index]
                        p.placeRelative(x, y)
                        x += cellWidth + spacingPx
                        index++
                    }
                }
                y += rowH + rowSpacing.roundToPx()
            }
        }
    }
}