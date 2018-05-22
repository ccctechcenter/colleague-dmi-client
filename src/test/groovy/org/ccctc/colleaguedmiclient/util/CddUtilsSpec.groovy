package org.ccctc.colleaguedmiclient.util

import org.ccctc.colleaguedmiclient.model.CddEntry
import spock.lang.Specification

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static org.ccctc.colleaguedmiclient.util.CddUtils.*

class CddUtilsSpec extends Specification {

    def "convertFromValue - date"() {
        setup:
        def dateType = CddEntry.builder().informConversionString("D4/").build()
        def notDateType = CddEntry.builder().build()

        def localDate = LocalDate.of(2010, 3, 11)
        def localDateTime = LocalDateTime.of(2010, 3, 11, 10, 45, 22)
        def date = new SimpleDateFormat("yyyy-MM-dd").parse("2010-3-11")
        def intDate = 25
        def strDate = "25"

        when:
        def ld1 = convertFromValue(localDate, dateType)
        def ldt1 = convertFromValue(localDateTime, dateType)
        def d1 = convertFromValue(date, dateType)
        def i1 = convertFromValue(intDate, dateType)
        def s1 = convertFromValue(strDate, dateType)

        then:
        ld1 == StringUtils.dateToString(localDate)
        ld1 == ldt1
        ld1 == d1
        i1 == s1
        i1.toString() == StringUtils.dateToString(StringUtils.BASE_DATE.plusDays(intDate))

        when:
        def ld2 = convertFromValue(localDate, notDateType)
        def ldt2 = convertFromValue(localDateTime, notDateType)
        def d2 = convertFromValue(date, notDateType)
        def i2 = convertFromValue(intDate, dateType)
        def s2 = convertFromValue(strDate, dateType)

        then:
        ld2 == localDate.toString()
        ldt2 == localDateTime.toString()
        d2 == date.toString()
        i2 == intDate.toString()
        s2 == strDate
    }

    def "convertFromValue - time"() {
        setup:
        def timeType = CddEntry.builder().informConversionString("MTS").build()
        def notTimeType = CddEntry.builder().build()

        def localTime = LocalTime.of(10, 45, 22)
        def localDateTime = LocalDateTime.of(2010, 3, 11, 10, 45, 22)
        def date = new SimpleDateFormat("HH:mm:ss").parse("10:45:22")
        def intTime = 10 * 60 * 60 + 45 * 60 + 22 // 10:45:22 in seconds
        def strTime = intTime.toString()

        when:
        def lt1 = convertFromValue(localTime, timeType)
        def ldt1 = convertFromValue(localDateTime, timeType)
        def d1 = convertFromValue(date, timeType)
        def i1 = convertFromValue(intTime, timeType)
        def s1 = convertFromValue(strTime, timeType)

        then:
        lt1 == StringUtils.timeToString(localTime)
        lt1 == ldt1
        lt1 == d1
        lt1 == i1
        lt1 == s1

        when:
        def lt2 = convertFromValue(localTime, notTimeType)
        def ldt2 = convertFromValue(localDateTime, notTimeType)
        def d2 = convertFromValue(date, notTimeType)
        def i2 = convertFromValue(intTime, notTimeType)
        def s2 = convertFromValue(strTime, notTimeType)

        then:
        lt2 == localTime.toString()
        ldt2 == localDateTime.toString()
        d2 == date.toString()
        i2 == intTime.toString()
        s2 == strTime.toString()
    }

    def "convertFromValue - decimal"() {
        setup:
        def decimalType = CddEntry.builder().informConversionString("MD25,").build()
        def notDecimalType = CddEntry.builder().build()

        def string = "99.2345"
        def bigDecimal = new BigDecimal("99.2345")
        def aDouble = (Double)99.2345
        def aFloat = (Float)99.2345
        def aInteger = (Integer)99
        def aLong = (Long)99

        when:
        def s1 = convertFromValue(string, decimalType)
        def b1 = convertFromValue(bigDecimal, decimalType)
        def d1 = convertFromValue(aDouble, decimalType)
        def f1 = convertFromValue(aFloat, decimalType)
        def i1 = convertFromValue(aInteger, decimalType)
        def l1 = convertFromValue(aLong, decimalType)

        then:
        s1 == "9923450"
        s1 == b1
        // rounding issues with these data types ... they're all slightly off
        (s1 as Integer) >= ((d1 as Integer) - 1) && (s1 as Integer) <= ((d1 as Integer) + 1)
        (s1 as Integer) >= ((f1 as Integer) - 1) && (s1 as Integer) <= ((f1 as Integer) + 1)
        // integer / long
        i1 == "9900000"
        i1 == l1

        when:
        def s2 = convertFromValue(string, notDecimalType)
        def b2 = convertFromValue(bigDecimal, notDecimalType)
        def d2 = convertFromValue(aDouble, notDecimalType)
        def f2 = convertFromValue(aFloat, notDecimalType)
        def i2 = convertFromValue(aInteger, notDecimalType)
        def l2 = convertFromValue(aLong, notDecimalType)

        then:
        s2 == string
        b2 == bigDecimal.toString()
        d2 == aDouble.toString()
        f2 == aFloat.toString()
        i2 == aInteger.toString()
        l2 == aLong.toString()
    }

    def "convertFromValue - integer"() {
        setup:
        def integerType = CddEntry.builder().informConversionString("MD0").build()

        def string = "99"
        def bigInteger = new BigInteger("99")
        def aInteger = (Integer)99
        def aLong = (Long)99
        def bigDecimal = new BigDecimal("99.2345")
        def aDouble = (Double)99.2345
        def aFloat = (Float)99.2345

        when:
        def s1 = convertFromValue(string, integerType)
        def b1 = convertFromValue(bigDecimal, integerType)
        def bi1 = convertFromValue(bigInteger, integerType)
        def d1 = convertFromValue(aDouble, integerType)
        def f1 = convertFromValue(aFloat, integerType)
        def i1 = convertFromValue(aInteger, integerType)
        def l1 = convertFromValue(aLong, integerType)

        then:
        s1 == b1
        s1 == bi1
        s1 == d1
        s1 == f1
        s1 == i1
        s1 == l1
    }

    def "convertFromValue - boolean"() {
        setup:
        def cddEntry = CddEntry.builder().build()

        when:
        def b1 = convertFromValue((Boolean) true, cddEntry)
        def b2 = convertFromValue((Boolean) false, cddEntry)
        def b3 = convertFromValue((Boolean) null, cddEntry)

        then:
        b1 == "Y"
        b2 == "N"
        b3 == ""
    }

    def "convertFromValue - NumberFormatException"() {
        setup:
        def dateType = CddEntry.builder().informConversionString("D4").build()
        def timeType = CddEntry.builder().informConversionString("MT").build()

        when: convertFromValue(-1, timeType)
        then: thrown NumberFormatException

        when: convertFromValue("9000000", timeType)
        then: thrown NumberFormatException

        when: convertFromValue("abc", timeType)
        then: thrown NumberFormatException

        when: convertFromValue("abc", dateType)
        then: thrown NumberFormatException



    }
}
