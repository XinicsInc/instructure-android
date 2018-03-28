/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package com.instructure.pandautils.utils

import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.widget.Toast
import com.instructure.pandautils.R
import java.io.Serializable
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Show a toast with a default length of Toast.LENGTH_SHORT */
fun Fragment.toast(messageResId: Int, length: Int = Toast.LENGTH_SHORT) { if(context != null) Toast.makeText(context, messageResId, length).show() }

fun Fragment.getModuleItemId(): Long? {
    return arguments?.getLong(Const.MODULE_ITEM_ID, -1)?.takeUnless { it < 0 } ?: parentFragment?.getModuleItemId()
}

/**
 * Dismisses an existing instance of the specified DialogFragment class. This only works for
 * dialogs tagged with the class's simple name.
 */
inline fun <reified D : DialogFragment> FragmentManager.dismissExisting() {
    (findFragmentByTag(D::class.java.simpleName) as? D)?.dismissAllowingStateLoss()
}

fun <T : Fragment> T.withArgs(argBlock: Bundle.() -> Unit): T {
    val args = arguments ?: Bundle()
    argBlock(args)
    arguments = args
    return this
}

/** Gets the fragment's existing args bundle if it exists, or creates and attaches a new bundle if it doesn't */
val Fragment.nonNullArgs: Bundle
    get() = arguments ?: Bundle().apply { this@nonNullArgs.arguments = this }

val Fragment.isTablet: Boolean
    get() = context.resources.getBoolean(R.bool.is_device_tablet)

/** Convenience delegates for fragment arguments */
class IntArg(val default: Int = 0) : ReadWriteProperty<Fragment, Int> {
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: Int) = thisRef.nonNullArgs.putInt(property.name, value)
    override fun getValue(thisRef: Fragment, property: KProperty<*>) = thisRef.arguments?.getInt(property.name, default) ?: default
}

class BooleanArg(val default: Boolean = false) : ReadWriteProperty<Fragment, Boolean> {
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: Boolean) = thisRef.nonNullArgs.putBoolean(property.name, value)
    override fun getValue(thisRef: Fragment, property: KProperty<*>) = thisRef.arguments?.getBoolean(property.name, default) ?: default
}

class LongArg(val default: Long = 0L, val key: String? = null) : ReadWriteProperty<Fragment, Long> {
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: Long) = thisRef.nonNullArgs.putLong(key ?: property.name, value)
    override fun getValue(thisRef: Fragment, property: KProperty<*>) = thisRef.arguments?.getLong(key ?: property.name, default) ?: default
}

class FloatArg(val default: Float = 0f) : ReadWriteProperty<Fragment, Float> {
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: Float) = thisRef.nonNullArgs.putFloat(property.name, value)
    override fun getValue(thisRef: Fragment, property: KProperty<*>) = thisRef.arguments?.getFloat(property.name, default) ?: default
}

class DoubleArg(val default: Double = 0.0) : ReadWriteProperty<Fragment, Double> {
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: Double) = thisRef.nonNullArgs.putDouble(property.name, value)
    override fun getValue(thisRef: Fragment, property: KProperty<*>) = thisRef.arguments?.getDouble(property.name, default) ?: default
}

class StringArg(val default: String = "") : ReadWriteProperty<Fragment, String> {
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: String) = thisRef.nonNullArgs.putString(property.name, value)
    override fun getValue(thisRef: Fragment, property: KProperty<*>) = thisRef.arguments?.getString(property.name, default) ?: default
}

class NullableStringArg : ReadWriteProperty<Fragment, String?> {
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: String?) = thisRef.nonNullArgs.putString(property.name, value)
    override fun getValue(thisRef: Fragment, property: KProperty<*>) = thisRef.arguments?.getString(property.name)
}

class ParcelableArg<T : Parcelable>(val default: T? = null) : ReadWriteProperty<Fragment, T> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T = thisRef.arguments?.getParcelable(property.name) ?: default ?: throw IllegalStateException("Parcelable arg '${property.name}' has not been set!")
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) = thisRef.nonNullArgs.putParcelable(property.name, value)
}

class NullableParcelableArg<T : Parcelable>(val default: T? = null, val key: String? = null) : ReadWriteProperty<Fragment, T?> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T? = thisRef.arguments?.getParcelable(key ?: property.name) ?: default
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T?) = thisRef.nonNullArgs.putParcelable(key ?: property.name, value)
}

class ParcelableArrayListArg<T : Parcelable>(val default: ArrayList<T> = arrayListOf()) : ReadWriteProperty<Fragment, ArrayList<T>> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): ArrayList<T> = thisRef.arguments?.getParcelableArrayList(property.name) ?: default
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: ArrayList<T>) = thisRef.nonNullArgs.putParcelableArrayList(property.name, value)
}

@Suppress("UNCHECKED_CAST")
class SerializableArg<T : Serializable>(val default: T) : ReadWriteProperty<Fragment, T> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T = thisRef.arguments?.getSerializable(property.name) as? T ?: default
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) = thisRef.nonNullArgs.putSerializable(property.name, value)
}

@Suppress("UNCHECKED_CAST")
class NullableSerializableArg<T : Serializable> : ReadWriteProperty<Fragment, T?> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T? = thisRef.arguments?.getSerializable(property.name) as? T
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T?) = thisRef.nonNullArgs.putSerializable(property.name, value)
}

@Suppress("UNCHECKED_CAST")
class BlindSerializableArg<T : Any?>(val default: T? = null) : ReadWriteProperty<Fragment, T?> {
    var cache: T? = null
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T? = cache ?: thisRef.arguments?.getSerializable(property.name) as? T ?: default
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T?) {
        cache = value
        thisRef.nonNullArgs.putSerializable(property.name, value as? Serializable)
    }
}

@Suppress("UNCHECKED_CAST")
class SerializableListArg<T : Serializable>(val default: List<T>) : ReadWriteProperty<Fragment, List<T>> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): List<T> = thisRef.arguments?.getSerializable(property.name) as? List<T> ?: default
    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: List<T>) = thisRef.nonNullArgs.putSerializable(property.name, ArrayList(value))
}
