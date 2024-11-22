package com.example.testnav3

import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.write
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

fun <T : Any> encodeToSavedState(serializer: SerializationStrategy<T>, serializersModule: SerializersModule, value: T): SavedState =
    savedState().apply { SavedStateEncoder(this, serializersModule)
        .encodeSerializableValue(serializer, value) }

inline fun <reified T : Any> encodeToSavedState(serializable: T): SavedState {
    return encodeToSavedState(serializer<T>(), EmptySerializersModule(), serializable)
}

fun <T : Any> decodeFromSavedState(
    deserializer: DeserializationStrategy<T>,
    serializersModule: SerializersModule,
    savedState: SavedState
): T {
    return SavedStateDecoder(savedState, serializersModule).decodeSerializableValue(deserializer)
}

inline fun <reified T : Any> decodeFromSavedState(savedState: SavedState): T =
    decodeFromSavedState(serializer<T>(), EmptySerializersModule(), savedState)


private class SavedStateEncoder(private val savedState: SavedState, override val serializersModule: SerializersModule) : AbstractEncoder() {
    private var key: String = ""

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        false

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        // The key will be property names for classes by default and can be modified with
        // `@SerialName`. The key for collections will be decimal integer Strings ("0",
        // "1", "2", ...).
        key = descriptor.getElementName(index)
        return true
    }

    override fun encodeBoolean(value: Boolean) {
        savedState.write { putBoolean(key, value) }
    }

    override fun encodeByte(value: Byte) {
        savedState.write { putInt(key, value.toInt()) }
    }

    override fun encodeShort(value: Short) {
        savedState.write { putInt(key, value.toInt()) }
    }

    override fun encodeInt(value: Int) {
        savedState.write { putInt(key, value) }
    }

    override fun encodeLong(value: Long) {
        savedState.write { putLong(key, value) }
    }

    override fun encodeFloat(value: Float) {
        savedState.write { putFloat(key, value) }
    }

    override fun encodeDouble(value: Double) {
        savedState.write { putDouble(key, value) }
    }

    override fun encodeChar(value: Char) {
        savedState.write { putChar(key, value) }
    }

    override fun encodeString(value: String) {
        savedState.write { putString(key, value) }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        savedState.write { putInt(key, index) }
    }

    override fun encodeNull() {
        savedState.write { putNull(key) }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        // We flatten single structured object at root to prevent encoding to a
        // SavedState containing only one SavedState inside. For example, a
        // `Pair(3, 5)` would become `{"first" = 3, "second" = 5}` instead of
        // `{{"first" = 3, "second" = 5}}`, which is more consistent but less
        // efficient.
        return if (key == "") {
            this
        } else {
            SavedStateEncoder(
                savedState = savedState().also { child -> savedState.write { putSavedState(key, child) } },
                serializersModule = serializersModule
            )
        }
    }
}

private class SavedStateDecoder(
    private val savedState: SavedState,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    private var key: String = ""
    private var index = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (index == savedState.read { size() }) return CompositeDecoder.DECODE_DONE
        key = descriptor.getElementName(index)
        return index++
    }

    override fun decodeBoolean(): Boolean = savedState.read { getBoolean(key) }

    override fun decodeByte(): Byte = savedState.read { getInt(key).toByte() }

    override fun decodeShort(): Short = savedState.read { getInt(key).toShort() }

    override fun decodeInt(): Int = savedState.read { getInt(key) }

    override fun decodeLong(): Long = savedState.read { getLong(key) }

    override fun decodeFloat(): Float = savedState.read { getFloat(key) }

    override fun decodeDouble(): Double = savedState.read { getDouble(key) }

    override fun decodeChar(): Char = savedState.read { getChar(key) }

    override fun decodeString(): String = savedState.read { getString(key) }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = savedState.read { getInt(key) }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        if (key == "") {
            this
        } else {
            SavedStateDecoder(
                savedState = savedState.read { getSavedState(key) },
                serializersModule = serializersModule
            )
        }

    // We don't encode NotNullMark so this will actually read either a `null` from
    // `encodeNull()` or a value from other encode functions.
    override fun decodeNotNullMark(): Boolean = savedState.read { !isNull(key) }
}

