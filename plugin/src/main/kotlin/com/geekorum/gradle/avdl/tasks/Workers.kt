package com.geekorum.gradle.avdl.tasks

import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters


internal interface ProviderWorkerParams : WorkParameters {
    val pluginClass: Property<String>
    // Seems like gradle can't serialize WorkParameters if they are Byte or primitive array
    // like ShortArray. Works fine with Array<Short> so we need to convert it
    val configuration : Property<Array<Short>>

}

internal fun ProviderWorkerParams.setConfiguration(blob: ByteArray) {
    configuration.set(blob.toShortArray().toTypedArray())
}

internal fun ProviderWorkerParams.getConfigurationBlob(): ByteArray {
    return configuration.get().toShortArray().toByteArray()
}


private fun ByteArray.toShortArray(): ShortArray {
    return ShortArray(this.size / 2) {
        val high: Short = (this[it * 2].toInt() shl 8).toShort()
        val low = this[it * 2 + 1].toShort()
        (high + low).toShort()
    }
}

private fun ShortArray.toByteArray(): ByteArray {
    val res = ByteArray(this.size * 2)
    forEachIndexed { i  , v->
        res[ i * 2] = ((v.toInt() and 0xFF00) shr 8).toByte()
        res [ i * 2 + 1 ] = (v.toInt() and 0x00FF).toByte()
    }
    return res
}
