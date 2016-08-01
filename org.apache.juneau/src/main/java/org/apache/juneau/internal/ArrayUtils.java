/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.internal;

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Quick and dirty utilities for working with arrays.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class ArrayUtils {

	/**
	 * Appends one or more elements to an array.
	 *
	 * @param <T> The element type.
	 * @param array The array to append to.
	 * @param newElements The new elements to append to the array.
	 * @return A new array with the specified elements appended.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] append(T[] array, T...newElements) {
		if (array == null)
			return newElements;
		if (newElements.length == 0)
			return array;
		T[] a = (T[])Array.newInstance(array.getClass().getComponentType(), array.length + newElements.length);
		for (int i = 0; i < array.length; i++)
			a[i] = array[i];
		for (int i = 0; i < newElements.length; i++)
			a[i+array.length] = newElements[i];
		return a;
	}

	/**
	 * Appends one or more elements to an array.
	 *
	 * @param <T> The element type.
	 * @param array The array to append to.
	 * @param newElements The new elements to append to the array.
	 * @return A new array with the specified elements appended.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] append(T[] array, Collection<T> newElements) {
		assertFieldNotNull(array, "array");
		if (newElements.size() == 0)
			return array;
		T[] a = (T[])Array.newInstance(array.getClass().getComponentType(), array.length + newElements.size());
		for (int i = 0; i < array.length; i++)
			a[i] = array[i];
		int l = array.length;
		for (T t : newElements)
			a[l++] = t;
		return a;
	}

	/**
	 * Combine an arbitrary number of arrays into a single array.
	 *
	 * @param arrays Collection of arrays to combine.
	 * @return A new combined array, or <jk>null</jk> if all arrays are <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] combine(T[]...arrays) {
		assertFieldNotNull(arrays, "arrays");
		int l = 0;
		T[] a1 = null;
		for (T[] a : arrays) {
			if (a1 == null && a != null)
				a1 = a;
			l += (a == null ? 0 : a.length);
		}
		if (a1 == null)
			return null;
		T[] a = (T[])Array.newInstance(a1.getClass().getComponentType(), l);
		int i = 0;
		for (T[] aa : arrays)
			if (aa != null)
				for (T t : aa)
					a[i++] = t;
		return a;
	}

	/**
	 * Creates a new array with reversed entries.
	 *
	 * @param <T> The class type of the array.
	 * @param array The array to reverse.
	 * @return A new array with reversed entries, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] reverse(T[] array) {
		assertFieldNotNull(array, "array");
		Class<T> c = (Class<T>)array.getClass().getComponentType();
		T[] a2 = (T[])Array.newInstance(c, array.length);
		for (int i = 0; i < array.length; i++)
			a2[a2.length-i-1] = array[i];
		return a2;
	}

	/**
	 * Converts the specified array to a <code>Set</code>.
	 * <p>
	 * 	The order of the entries in the set are the same as the array.
	 *
	 * @param <T> The entry type of the array.
	 * @param array The array being wrapped in a <code>Set</code> interface.
	 * @return The new set.
	 */
	public static <T> Set<T> asSet(final T[] array) {
		assertFieldNotNull(array, "array");
		return new AbstractSet<T>() {

			@Override /* Set */
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					int i = 0;

					@Override /* Iterator */
					public boolean hasNext() {
						return i < array.length;
					}

					@Override /* Iterator */
					public T next() {
						if (i >= array.length)
							throw new NoSuchElementException();
						T t = array[i];
						i++;
						return t;
					}

					@Override /* Iterator */
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override /* Set */
			public int size() {
				return array.length;
			}
		};
	}

	/**
	 * Returns an iterator against an array.
	 * This works with any array type (e.g. <code>String[]</code>, <code>Object[]</code>, <code><jk>int</jk>[]</code>, etc...).
	 *
	 * @param array The array to create an iterator over.
	 * @return An iterator over the specified array.
	 */
	public static Iterator<Object> iterator(final Object array) {
		return new Iterator<Object>() {
			int i = 0;
			int length = array == null ? 0 : Array.getLength(array);

			@Override /* Iterator */
			public boolean hasNext() {
				return i < length;
			}

			@Override /* Iterator */
			public Object next() {
				if (i >= length)
					throw new NoSuchElementException();
				return Array.get(array, i++);
			}

			@Override /* Iterator */
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * Converts the specified collection to an array.
	 * Works on both object and primitive arrays.
	 *
	 * @param c The collection to convert to an array.
	 * @param componentType The component type of the collection.
	 * @return A new array.
	 */
	public static <T> Object toArray(Collection<T> c, Class<T> componentType) {
		Object a = Array.newInstance(componentType, c.size());
		Iterator<T> it = c.iterator();
		int i = 0;
		while (it.hasNext())
			Array.set(a, i++, it.next());
		return a;
	}

	/**
	 * Copies the specified array into the specified list.
	 * Works on both object and primitive arrays.
	 *
	 * @param array The array to copy into a list.
	 * @param list The list to copy the values into.
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public static void copyToList(Object array, List list) {
		if (array != null) {
			int length = Array.getLength(array);
			for (int i = 0; i < length; i++)
				list.add(Array.get(array, i));
		}
	}

	/**
	 * Returns <jk>true</jk> if the specified array contains the specified element
	 * 	using the {@link Object#equals(Object)} method.
	 *
	 * @param element The element to check for.
	 * @param array The array to check.
	 * @return <jk>true</jk> if the specified array contains the specified element,
	 * 	<jk>false</jk> if the array or element is <jk>null</jk>.
	 */
	public static <T> boolean contains(T element, T[] array) {
		return indexOf(element, array) != -1;
	}

	/**
	 * Returns the index position of the element in the specified array
	 * 	using the {@link Object#equals(Object)} method.
	 *
	 * @param element The element to check for.
	 * @param array The array to check.
	 * @return The index position of the element in the specified array, or
	 * 	<code>-1</code> if the array doesn't contain the element, or the array or element is <jk>null</jk>.
	 */
	public static <T> int indexOf(T element, T[] array) {
		if (element == null)
			return -1;
		if (array == null)
			return -1;
		for (int i = 0; i < array.length; i++)
			if (element.equals(array[i]))
				return i;
		return -1;
	}

	/**
	 * Converts a primitive wrapper array (e.g. <code>Integer[]</code>) to a primitive array (e.g. <code><jk>int</jk>[]</code>).
	 *
	 * @param o The array to convert.  Must be a primitive wrapper array.
	 * @return A new array.
	 * @throws IllegalArgumentException If object is not a wrapper object array.
	 */
	public static Object toPrimitiveArray(Object o) {
		Class<?> c = o.getClass();
		if (! c.isArray())
			throw new IllegalArgumentException("Cannot pass non-array objects to toPrimitiveArray()");
		int l = Array.getLength(o);
		Class<?> tc = ClassUtils.getPrimitiveForWrapper(c.getComponentType());
		if (tc == null)
			throw new IllegalArgumentException("Array type is not a primitive wrapper array.");
		Object a = Array.newInstance(tc, l);
		for (int i = 0; i < l; i++)
			Array.set(a, i, Array.get(o, i));
		return a;
	}
}