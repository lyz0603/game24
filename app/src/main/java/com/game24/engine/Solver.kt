package com.game24.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/**
 * 有理数（分数），用于精确运算，避免浮点误差。
 */
class Rational private constructor(
    val num: Long,
    val den: Long
) {
    init {
        require(den != 0L) { "分母不能为零" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Rational) return false
        return num == other.num && den == other.den
    }

    override fun hashCode(): Int = 31 * num.hashCode() + den.hashCode()

    override fun toString(): String = if (den == 1L) "$num" else "$num/$den"

    operator fun plus(other: Rational): Rational = fromRaw(
        num * other.den + other.num * den,
        den * other.den
    )

    operator fun minus(other: Rational): Rational = fromRaw(
        num * other.den - other.num * den,
        den * other.den
    )

    operator fun times(other: Rational): Rational = fromRaw(
        num * other.num,
        den * other.den
    )

    operator fun div(other: Rational): Rational? {
        if (other.num == 0L) return null
        return fromRaw(num * other.den, den * other.num)
    }

    companion object {
        /** 从整数创建 */
        fun of(value: Long): Rational = Rational(value, 1)

        /** 内部工厂：自动约分并保持分母为正 */
        private fun fromRaw(n: Long, d: Long): Rational {
            val g = gcd(abs(n), abs(d))
            val sign = if (d < 0) -1L else 1L
            return Rational(sign * n / g, abs(d) / g)
        }

        private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
    }
}

/** 四种运算 */
enum class Op(val symbol: String, val apply: (Rational, Rational) -> Rational?) {
    ADD("+", Rational::plus),
    SUB("-", Rational::minus),
    MUL("×", Rational::times),
    DIV("÷", Rational::div),
}

/** 一种括号结构：表达式模板 + 求值函数 */
private data class Pattern(
    val template: String,
    val eval: (a: Rational, b: Rational, c: Rational, d: Rational, o1: Op, o2: Op, o3: Op) -> Rational?
)

/** 5 种二叉树括号结构 */
private val patterns = listOf(
    // 1: ((a op b) op c) op d
    Pattern(
        template = "(({0} {4} {1}) {5} {2}) {6} {3}",
        eval = { a, b, c, d, o1, o2, o3 ->
            o1.apply(a, b)?.let { o2.apply(it, c) }?.let { o3.apply(it, d) }
        },
    ),
    // 2: (a op (b op c)) op d
    Pattern(
        template = "({0} {4} ({1} {5} {2})) {6} {3}",
        eval = { a, b, c, d, o1, o2, o3 ->
            o2.apply(b, c)?.let { o1.apply(a, it) }?.let { o3.apply(it, d) }
        },
    ),
    // 3: a op ((b op c) op d)
    Pattern(
        template = "{0} {4} (({1} {5} {2}) {6} {3})",
        eval = { a, b, c, d, o1, o2, o3 ->
            o2.apply(b, c)?.let { o3.apply(it, d) }?.let { o1.apply(a, it) }
        },
    ),
    // 4: a op (b op (c op d))
    Pattern(
        template = "{0} {4} ({1} {5} ({2} {6} {3}))",
        eval = { a, b, c, d, o1, o2, o3 ->
            o3.apply(c, d)?.let { o2.apply(b, it) }?.let { o1.apply(a, it) }
        },
    ),
    // 5: (a op b) op (c op d)
    Pattern(
        template = "({0} {4} {1}) {5} ({2} {6} {3})",
        eval = { a, b, c, d, o1, o2, o3 ->
            o1.apply(a, b)?.let { left -> o3.apply(c, d)?.let { right -> o2.apply(left, right) } }
        },
    ),
)

/**
 * 24 点求解引擎。
 *
 * 搜索空间：24 排列 × 64 运算符 × 5 括号结构 = 7680 种表达式。
 * 使用 [Dispatchers.Default] 并行计算各排列，[Mutex] 保护共享结果集。
 */
object Solver {

    private val target: Rational = Rational.of(24)

    /**
     * 求解 24 点，返回所有不重复的表达式字符串。
     * 无解时返回空列表。
     */
    suspend fun solve(numbers: List<Int>): List<String> = coroutineScope {
        require(numbers.size == 4) { "必须恰好输入 4 个数字" }

        val ops = Op.entries
        val mutex = Mutex()
        val allResults = mutableSetOf<String>()

        // 生成排列（去重，避免重复数字导致冗余计算）
        val perms = numbers.toList().permutations().distinct()

        perms.map { perm ->
            async(Dispatchers.Default) {
                val local = mutableListOf<String>()
                for (o1 in ops) {
                    for (o2 in ops) {
                        for (o3 in ops) {
                            local.addAll(evaluate(perm, o1, o2, o3))
                        }
                    }
                }
                if (local.isNotEmpty()) {
                    mutex.withLock { allResults.addAll(local) }
                }
            }
        }.forEach { it.await() }

        allResults.toList()
    }

    /** 对给定排列和运算符组合，尝试 5 种括号结构 */
    private fun evaluate(nums: List<Int>, o1: Op, o2: Op, o3: Op): List<String> {
        val a = Rational.of(nums[0].toLong())
        val b = Rational.of(nums[1].toLong())
        val c = Rational.of(nums[2].toLong())
        val d = Rational.of(nums[3].toLong())

        val results = mutableListOf<String>()

        for (p in patterns) {
            val value = p.eval(a, b, c, d, o1, o2, o3)
            if (value != null && value == target) {
                results.add(
                    p.template
                        .replace("{0}", nums[0].toString())
                        .replace("{1}", nums[1].toString())
                        .replace("{2}", nums[2].toString())
                        .replace("{3}", nums[3].toString())
                        .replace("{4}", o1.symbol)
                        .replace("{5}", o2.symbol)
                        .replace("{6}", o3.symbol)
                )
            }
        }

        return results
    }
}

/** 全排列（递归实现） */
fun <T> List<T>.permutations(): List<List<T>> {
    if (size <= 1) return listOf(this.toList())
    val result = mutableListOf<List<T>>()
    for (i in indices) {
        val head = this[i]
        val tail = this.toMutableList().apply { removeAt(i) }
        for (perm in tail.permutations()) {
            result.add(listOf(head) + perm)
        }
    }
    return result
}
