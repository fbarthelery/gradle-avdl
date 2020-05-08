/*
 * gradle-avdl is a Gradle plugin to launch and stop Android
 * Virtual devices
 *
 * Copyright (C) 2020 by Frederic-Charles Barthelery.
 *
 * This file is part of gradle-avdl.
 *
 * gradle-avdl is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gradle-avdl is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gradle-avdl.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.geekorum.gradle.avdl.providers.flydroid

import com.geekorum.gradle.avdl.DeviceSetup
import com.geekorum.gradle.avdl.VirtualDeviceDefinition
import groovy.lang.Closure
import org.gradle.kotlin.dsl.invoke

// Unfortunately Groovy Extension modules don't work with gradle
// https://github.com/gradle/gradle/issues/2973
// so we are forced to use use() {}

fun flydroid(deviceDefinition: VirtualDeviceDefinition, fn: Closure<*>): DeviceSetup {
    return deviceDefinition.flydroid {
        fn.delegate = this
        fn()
    }
}