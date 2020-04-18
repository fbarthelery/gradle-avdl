package com.geekorum.gradle.avdl

import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class AvdlExtension @Inject constructor(
        objectFactory: ObjectFactory
) {
     val devices = objectFactory.domainObjectContainer(VirtualDeviceDefinition::class.java)
}
