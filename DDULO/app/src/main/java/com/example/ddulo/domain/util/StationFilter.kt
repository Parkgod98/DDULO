package com.example.ddulo.domain.util

import com.example.ddulo.domain.model.Station
import kotlin.math.min

class StationFilter(private var source: List<Station> = emptyList()) {

    // 캐시: "역" 제거 + 소문자 변환
    private var preProcessedNames: List<String> = emptyList()

    // 캐시: 초성 추출
    private var preProcessedInitials: List<String> = emptyList()

    fun updateSource(stations: List<Station>) {
        source = stations
        preProcessedNames = stations.map { it.name.removeSuffix("역").lowercase() }
        preProcessedInitials = stations.map { getInitials(it.name.lowercase()) }
    }

    fun filter(query: String): List<Station> {
        if (query.isBlank()) return source

        val lowerQuery = query.lowercase()
        val queryInitials = getInitials(lowerQuery)

        return source
            .mapIndexed { index, station ->
                val name = preProcessedNames[index]
                val initials = preProcessedInitials[index]

                // 매칭 우선순위
                val priority = when {
                    name == lowerQuery -> 0                      // 완전 일치
                    name.startsWith(lowerQuery) -> 1             // 시작 문자열
                    name.contains(lowerQuery) || initials.startsWith(queryInitials) -> 2 // 포함 또는 초성
                    else -> 3
                }

                val distance = levenshtein(lowerQuery, name) // 오타 거리
                Triple(station, priority, distance)
            }
            .filter { it.second < 3 } // 매칭되는 항목만
            .sortedWith(compareBy({ it.second }, { it.third })) // 우선순위 → 오타거리
            .map { it.first }
    }

    // 간단 Levenshtein distance 구현 (오타 보정용)
    private fun levenshtein(s: String, t: String): Int {
        if (s == t) return 0
        if (s.isEmpty()) return t.length
        if (t.isEmpty()) return s.length

        val dp = Array(s.length + 1) { IntArray(t.length + 1) }
        for (i in 0..s.length) dp[i][0] = i
        for (j in 0..t.length) dp[0][j] = j

        for (i in 1..s.length) {
            for (j in 1..t.length) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s.length][t.length]
    }

    // 한글 초성 추출
    private fun getInitials(str: String): String {
        val sb = StringBuilder()
        for (ch in str) {
            if (ch in '가'..'힣') {
                val uni = ch - '가'
                val cho = uni / (21 * 28)
                sb.append(CHOSUNG[cho])
            } else sb.append(ch)
        }
        return sb.toString()
    }

    private val CHOSUNG = arrayOf(
        'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ','ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
    )
}
