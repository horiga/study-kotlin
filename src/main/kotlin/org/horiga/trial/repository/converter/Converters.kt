package org.horiga.trial.repository.converter

import org.horiga.trial.repository.SampleType
import org.horiga.trial.repository.TypeValue
import org.slf4j.LoggerFactory
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ConverterBuilder
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import java.nio.ByteBuffer
import java.util.UUID

object Converters {

    val log = LoggerFactory.getLogger(Converters::class.java)!!
    
    fun generateUUIDToByteArrayConverters() =
        ConverterBuilder.writing(UUID::class.java, ByteArray::class.java) { uuid ->
            ByteBuffer.wrap(ByteArray(16)).apply {
                this.putLong(uuid.mostSignificantBits)
                this.putLong(uuid.leastSignificantBits)
            }.array()
        }.andReading { bytes ->
            ByteBuffer.wrap(bytes)?.let { Pair(it.long, it.long) }?.let { (high, low) -> UUID(high, low) }
        }.converters

    // FIXME: doesn't work...
    @ReadingConverter
    class StringToEnumValueConverter<T>(
        val type: Class<T>
    ) : Converter<String, T> where T : Enum<*>, T : TypeValue {
        override fun convert(source: String): T? {
            log.info("[Custom reading converter] src={}", source)
            return type.enumConstants.find { it.enumValue == source }
        }
    }

    // works fine.
    @WritingConverter
    class EnumValueToStringConverter<T>(
        val type: Class<T>
    ) : Converter<T, String> where T : Enum<*>, T : TypeValue {
        override fun convert(source: T): String {
            log.info("[Custom writing converter] src={}", source)
            return source.enumValue
        }
    }

    fun <T> enumTypeConverters(klass: Class<T>): Set<Any> where T : Enum<*>, T : TypeValue =
        setOf(StringToEnumValueConverter(klass), EnumValueToStringConverter(klass))

    fun converters(): MutableList<Any> = mutableListOf<Any>().apply {
        addAll(generateUUIDToByteArrayConverters())
        addAll(SampleType.converters())
    }
}