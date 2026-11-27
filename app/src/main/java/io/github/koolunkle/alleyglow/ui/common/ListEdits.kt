package io.github.koolunkle.alleyglow.ui.common

/** max개가 차 있으면 그대로 */
fun <T> List<T>.plusUpTo(item: T, max: Int): List<T> = if (size < max) this + item else this

fun <T> List<T>.withoutIndex(index: Int): List<T> = filterIndexed { i, _ -> i != index }
