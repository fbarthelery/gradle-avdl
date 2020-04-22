package com.geekorum.gradle.avdl

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class AvdlExtension @Inject constructor(
        private val project: Project,
        objectFactory: ObjectFactory
) {
     val devices = objectFactory.domainObjectContainer(
             VirtualDeviceDefinition::class.java
     ) { name ->
          VirtualDeviceDefinition(project, name)
     }
}
