package com.ivianuu.apelabs

import com.ivianuu.essentials.ui.material.DefaultSliderRange
import com.ivianuu.essentials.ui.material.SliderValueConverter
import com.ivianuu.essentials.ui.material.StepPolicy
import com.ivianuu.injekt.Inject
import kotlin.math.absoluteValue

fun <T : Comparable<T>> StepPolicy<T>.stepValue(
  value: T,
  @Inject valueRange: @DefaultSliderRange ClosedRange<T>,
  @Inject converter: SliderValueConverter<T>
): T = with(converter) {
  val steps = this@stepValue(valueRange)
  val stepFractions = (if (steps == 0) emptyList()
  else List(steps + 2) { it.toFloat() / (steps + 1) })
  val stepValues = stepFractions
    .map {
      valueRange.start.toFloat() +
          ((valueRange.endInclusive.toFloat() - valueRange.start.toFloat()) * it)
    }

  val steppedValue = stepValues
    .minByOrNull { (it - value.toFloat()).absoluteValue }
    ?: value.toFloat()

  return steppedValue.toValue()
}
