package com.neytron.sshcommander.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Контракт дизайна для эпох интерфейса.
 */
interface EraStyle {
    val name: String

    // Фоновые цвета
    val appBackground: Color
    val surfaceBackground: Color
    val textColor: Color

    // Стили кнопок
    val buttonShape: Shape
    val buttonBorder: BorderStroke?
    val buttonBackground: Color
    val buttonTextColor: Color
    val buttonHasBevel: Boolean

    // Стили карточек / панелей
    val cardShape: Shape
    val cardBorder: BorderStroke?
    val cardBackground: Color
    val cardHasBevel: Boolean

    // Стили текстовых полей
    val textFieldShape: Shape
    val textFieldBorder: BorderStroke?
    val textFieldBackground: Color
    val textFieldSunken: Boolean
}

/**
 * Стандартный современный стиль Material 3 (Tokyo Night / Light в зависимости от темы).
 */
object ModernAndroidEra : EraStyle {
    override val name = "Modern Android"
    override val appBackground = Color(0xFF1A1B26)
    override val surfaceBackground = Color(0xFF1F2335)
    override val textColor = Color(0xFFC0CAF5)

    override val buttonShape = RoundedCornerShape(100) // Круглые кнопки Material 3
    override val buttonBorder = null
    override val buttonBackground = Color(0xFF7AA2F7)
    override val buttonTextColor = Color(0xFF1A1B26)
    override val buttonHasBevel = false

    override val cardShape = RoundedCornerShape(12.dp)
    override val cardBorder = null
    override val cardBackground = Color(0xFF1F2335)
    override val cardHasBevel = false

    override val textFieldShape = RoundedCornerShape(4.dp)
    override val textFieldBorder = null
    override val textFieldBackground = Color(0xFF24283B)
    override val textFieldSunken = false
}

/**
 * Классический ретро-стиль Windows 9x.
 */
object Windows9xEra : EraStyle {
    override val name = "Windows 9x"
    override val appBackground = Color(0xFFC0C0C0) // Классический серый фоновый цвет
    override val surfaceBackground = Color(0xFFC0C0C0) // Серый цвет окон
    override val textColor = Color(0xFF000000) // Черный текст

    override val buttonShape = RoundedCornerShape(0.dp) // Абсолютно квадратные
    override val buttonBorder = null 
    override val buttonBackground = Color(0xFFA0A0A0) // Темно-серый для активных элементов
    override val buttonTextColor = Color(0xFF000000) // Всегда черный текст
    override val buttonHasBevel = true

    override val cardShape = RoundedCornerShape(0.dp)
    override val cardBorder = null
    override val cardBackground = Color(0xFFC0C0C0)
    override val cardHasBevel = true

    override val textFieldShape = RoundedCornerShape(0.dp)
    override val textFieldBorder = null
    override val textFieldBackground = Color(0xFFFFFFFF) // Белые поля ввода
    override val textFieldSunken = true
}

/**
 * Современный плоский стиль Windows 10.
 */
object Windows10Era : EraStyle {
    override val name = "Windows 10"
    override val appBackground = Color(0xFFF2F2F2)
    override val surfaceBackground = Color(0xFFFFFFFF)
    override val textColor = Color(0xFF000000)

    override val buttonShape = RoundedCornerShape(0.dp)
    override val buttonBorder = null
    override val buttonBackground = Color(0xFF0078D7) // Windows Blue
    override val buttonTextColor = Color(0xFFFFFFFF)
    override val buttonHasBevel = false

    override val cardShape = RoundedCornerShape(0.dp)
    override val cardBorder = BorderStroke(1.dp, Color(0xFFE5E5E5))
    override val cardBackground = Color(0xFFFFFFFF)
    override val cardHasBevel = false

    override val textFieldShape = RoundedCornerShape(0.dp)
    override val textFieldBorder = BorderStroke(2.dp, Color(0xFF666666))
    override val textFieldBackground = Color(0xFFFFFFFF)
    override val textFieldSunken = false
}

/**
 * Темный плоский стиль Windows 10.
 */
object Windows10DarkEra : EraStyle {
    override val name = "Windows 10 Dark"
    override val appBackground = Color(0xFF171717)
    override val surfaceBackground = Color(0xFF202020)
    override val textColor = Color(0xFFFFFFFF)

    override val buttonShape = RoundedCornerShape(0.dp)
    override val buttonBorder = null
    override val buttonBackground = Color(0xFF0078D7) // Тот же синий
    override val buttonTextColor = Color(0xFFFFFFFF)
    override val buttonHasBevel = false

    override val cardShape = RoundedCornerShape(0.dp)
    override val cardBorder = BorderStroke(1.dp, Color(0xFF333333))
    override val cardBackground = Color(0xFF202020)
    override val cardHasBevel = false

    override val textFieldShape = RoundedCornerShape(0.dp)
    override val textFieldBorder = BorderStroke(2.dp, Color(0xFF858585))
    override val textFieldBackground = Color(0xFF1F1F1F)
    override val textFieldSunken = false
}

/**
 * Вспомогательный Modifier для отрисовки тонкой рамки в 1 пиксель (замена 3D эффекта).
 */
fun Modifier.win9xBevel(
    isPressed: Boolean = false,
    isSunken: Boolean = false
): Modifier = drawBehind {
    val w = size.width
    val h = size.height
    val black = Color(0xFF000000)
    val p = 1f 

    // Рисуем простую рамку в 1 пиксель по периметру
    drawLine(black, Offset(0f, 0f), Offset(w, 0f), strokeWidth = p)
    drawLine(black, Offset(0f, 0f), Offset(0f, h), strokeWidth = p)
    drawLine(black, Offset(w, 0f), Offset(w, h), strokeWidth = p)
    drawLine(black, Offset(0f, h), Offset(w, h), strokeWidth = p)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = LocalEraStyle.current.buttonShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val era = LocalEraStyle.current
    val isWin9x = era is Windows9xEra
    val pressed by interactionSource.collectIsPressedAsState()

    val buttonContent = @Composable {
        Button(
            onClick = onClick,
            modifier = modifier.then(if (isWin9x) Modifier.win9xBevel(isPressed = pressed) else Modifier),
            enabled = enabled,
            shape = if (isWin9x) RoundedCornerShape(0.dp) else shape,
            colors = if (isWin9x) {
                ButtonDefaults.buttonColors(
                    containerColor = era.buttonBackground,
                    contentColor = era.buttonTextColor,
                    disabledContainerColor = era.buttonBackground.copy(alpha = 0.55f),
                    disabledContentColor = era.buttonTextColor.copy(alpha = 0.55f)
                )
            } else {
                colors
            },
            border = if (isWin9x) null else border,
            elevation = if (isWin9x) {
                ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            } else {
                ButtonDefaults.buttonElevation()
            },
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            content = {
                val contentModifier = if (isWin9x && pressed) Modifier.offset(1.dp, 1.dp) else Modifier
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    Row(content = content)
                }
            }
        )
    }

    if (isWin9x) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            buttonContent()
        }
    } else {
        buttonContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EraOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = LocalEraStyle.current.buttonShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val era = LocalEraStyle.current
    val isWin9x = era is Windows9xEra
    val pressed by interactionSource.collectIsPressedAsState()

    val buttonContent = @Composable {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.then(if (isWin9x) Modifier.win9xBevel(isPressed = pressed) else Modifier),
            enabled = enabled,
            shape = if (isWin9x) RoundedCornerShape(0.dp) else shape,
            colors = if (isWin9x) {
                ButtonDefaults.buttonColors(
                    containerColor = era.buttonBackground,
                    contentColor = era.buttonTextColor,
                    disabledContainerColor = era.buttonBackground.copy(alpha = 0.55f),
                    disabledContentColor = era.buttonTextColor.copy(alpha = 0.55f)
                )
            } else {
                colors
            },
            border = if (isWin9x) null else border,
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            content = {
                val contentModifier = if (isWin9x && pressed) Modifier.offset(1.dp, 1.dp) else Modifier
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    Row(content = content)
                }
            }
        )
    }

    if (isWin9x) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            buttonContent()
        }
    } else {
        buttonContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EraTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val era = LocalEraStyle.current
    val isWin9x = era is Windows9xEra
    val pressed by interactionSource.collectIsPressedAsState()

    val buttonContent = @Composable {
        TextButton(
            onClick = onClick,
            modifier = modifier.then(if (isWin9x) Modifier.win9xBevel(isPressed = pressed) else Modifier),
            enabled = enabled,
            shape = if (isWin9x) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp),
            colors = if (isWin9x) {
                ButtonDefaults.buttonColors(
                    containerColor = era.buttonBackground,
                    contentColor = era.buttonTextColor,
                    disabledContainerColor = era.buttonBackground.copy(alpha = 0.55f),
                    disabledContentColor = era.buttonTextColor.copy(alpha = 0.55f)
                )
            } else {
                colors
            },
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            content = {
                val contentModifier = if (isWin9x && pressed) Modifier.offset(1.dp, 1.dp) else Modifier
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    Row(content = content)
                }
            }
        )
    }

    if (isWin9x) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            buttonContent()
        }
    } else {
        buttonContent()
    }
}

@Composable
fun EraCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val era = LocalEraStyle.current
    val isWin9x = era is Windows9xEra
    val boxSize = 18.dp
    val color = if (isWin9x) era.buttonBackground else Color(0xFF1976D2)
    val textColor = if (isWin9x) era.buttonTextColor else Color.Black

    Box(
        modifier = modifier
            .size(boxSize)
            .then(if (isWin9x) Modifier.win9xBevel(isPressed = checked) else Modifier)
            .background(if (checked) color else era.surfaceBackground)
            .border(1.dp, if (isWin9x) Color.Black else Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(0.dp))
            .clickable(enabled = enabled && onCheckedChange != null) { onCheckedChange?.invoke(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun EraSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val era = LocalEraStyle.current
    val isWin9x = era is Windows9xEra
    val trackWidth = 38.dp
    val thumbSize = 16.dp
    val trackColor = if (checked) era.buttonBackground else era.surfaceBackground
    val thumbColor = if (checked) era.buttonTextColor else Color.White

    val trackShape = if (isWin9x) RoundedCornerShape(0.dp) else RoundedCornerShape(50)
    val knobShape = if (isWin9x) RoundedCornerShape(0.dp) else RoundedCornerShape(50)

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(22.dp)
            .then(if (isWin9x) Modifier.win9xBevel(isPressed = false, isSunken = !checked) else Modifier)
            .background(trackColor, shape = trackShape)
            .border(1.dp, if (isWin9x) Color.Black else Color.Gray.copy(alpha = 0.3f), trackShape)
            .clickable(enabled = enabled && onCheckedChange != null) { onCheckedChange?.invoke(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(thumbSize)
                .background(thumbColor, shape = knobShape)
                .border(1.dp, if (isWin9x) Color.Black else Color.LightGray, knobShape)
        )
    }
}

@Composable
fun EraSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true
) {
    val era = LocalEraStyle.current
    val isWin9x = era is Windows9xEra

    // Use platform Material Slider for non-Win9x eras to preserve native look & behavior
    if (!isWin9x) {
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = modifier
        )
        return
    }

    // Custom Win9x slider (simple visual + pointer handling)
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .padding(vertical = 6.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .height(24.dp)
            .pointerInput(enabled, valueRange, steps) {
                            // Only react to pointer press + drag (no hover)
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val downChange = event.changes.firstOrNull { it.changedToDown() } ?: continue

                                    // compute inner bounds (account for horizontal padding)
                                    val paddingPx = 8.dp.toPx()
                                    val innerWidth = (trackWidthPx - paddingPx * 2f).coerceAtLeast(1f)
                                    val knobSizePx = 16.dp.toPx()

                                    fun posToValue(posX: Float): Float {
                                        val localX = (posX - paddingPx).coerceIn(0f, innerWidth)
                                        val ratio = (localX / innerWidth).coerceIn(0f, 1f)
                                        var selected = valueRange.start + (valueRange.endInclusive - valueRange.start) * ratio
                                        if (steps > 0) {
                                            val stepSize = (valueRange.endInclusive - valueRange.start) / (steps + 1)
                                            val rounded = ((selected - valueRange.start) / stepSize).roundToInt() * stepSize + valueRange.start
                                            selected = rounded.coerceIn(valueRange.start, valueRange.endInclusive)
                                        }
                                        return selected
                                    }

                                    // set value on initial press (tap)
                                    onValueChange(posToValue(downChange.position.x))

                                    val pointerId = downChange.id
                                    // handle drag until release
                                    while (true) {
                                        val ev2 = awaitPointerEvent()
                                        val change = ev2.changes.firstOrNull { it.id == pointerId } ?: continue
                                        if (!change.pressed) break
                                        change.consume()
                                        onValueChange(posToValue(change.position.x))
                                    }
                                }
                            }
                        }
    ) {
        val strokeColor = era.buttonBackground
        val trackColor = era.surfaceBackground
        val knobSize = 16.dp

        val paddingPx = with(density) { 8.dp.toPx() }
        val innerWidth = (trackWidthPx - paddingPx * 2f).coerceAtLeast(1f)
        val knobSizePx = with(density) { knobSize.toPx() }

        // compute ratio and clamp knob inside inner area
        val ratio = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
                val maxOffset = (innerWidth - knobSizePx).coerceAtLeast(0f)
                val knobOffset = (ratio * innerWidth).coerceIn(0f, maxOffset)

        val trackShape = RoundedCornerShape(0.dp)
        val knobShape = RoundedCornerShape(0.dp)

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .then(Modifier.win9xBevel(isSunken = true))
                .background(trackColor, shape = trackShape)
                .border(1.dp, Color.Black, trackShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((knobOffset).toInt(), 0) }
                .size(knobSize)
                .background(strokeColor, shape = knobShape)
                .border(1.dp, Color.Black, knobShape)
        )
    }
}

