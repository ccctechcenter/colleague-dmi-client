package org.ccctc.colleaguedmiclient.util

import spock.lang.Specification

import java.time.LocalTime

import static org.ccctc.colleaguedmiclient.util.StringUtils.*

class StringUtilsSpec extends Specification{

    static constructorConverage = new StringUtils()

    def "dateFromString"() {
        when:
        def nil = dateFromString(null)
        def plusDays = dateFromString("12")
        def minusDays = dateFromString("-7")
        def error = dateFromString("not-a-number")

        then:
        nil == null
        plusDays == BASE_DATE.plusDays(12)
        minusDays == BASE_DATE.minusDays(7)
        error == null
    }

    def "dateToString"() {
        when:
        def nil = dateToString(null)
        def plusDays = dateToString(BASE_DATE.plusDays(3))
        def minusDays = dateToString(BASE_DATE.minusDays(7))

        then:
        nil == null
        plusDays == "3"
        minusDays == "-7"
    }

    def "timeFromString"() {
        when:
        def nil = timeFromString(null)
        def minusTime = timeFromString("-10") // negatives take absolute value
        def plusTime = timeFromString("10")
        def lotsOfTime = timeFromString("86401") // over one day wraps around
        def error = timeFromString("not-a-number")

        then:
        nil == null
        minusTime == plusTime
        plusTime.toSecondOfDay() == 10
        lotsOfTime.toSecondOfDay() == 1
        error == null
    }

    def "timeToString"() {
        when:
        def nil = timeToString(null)
        def notNil = timeToString(LocalTime.ofSecondOfDay(12))

        then:
        nil == null
        notNil == "12"
    }


    def "split"() {
        when:
        def nil = split(null, "+" as char)
        def split1 = split("a", "+" as char)
        def split2 = split("a+", "+" as char)
        def split3 = split("a+b", "+" as char)
        def split4 = split("a+bc+", "+" as char)
        def split5 = split("a+bc+def", "+" as char)

        then:
        nil == null
        split1 == ["a"]
        split2 == ["a"]
        split3 == ["a", "b"]
        split4 == ["a", "bc"]
        split5 == ["a", "bc", "def"]
    }

    def "computeHash"() {
        when:
        def h1 = computeHash("string" + VM + "more-strings", null)
        def h2 = computeHash("string,more-strings", null)
        def h3 = computeHash("string" + FM + "more-strings", "a secret")

        then:
        h1 == h2
        h1 != null
        h1 != h3
        h3 != null
    }
}
