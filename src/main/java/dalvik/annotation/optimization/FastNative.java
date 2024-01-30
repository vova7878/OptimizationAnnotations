/*
 * Copyright (c) 2024 Vladimir Kozelkov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dalvik.annotation.optimization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An ART runtime built-in optimization for {@code native} methods to speed up JNI transitions:
 * Compared to normal {@code native} methods, {@code native} methods that are annotated with
 * {@literal @}{@code FastNative} use faster JNI transitions from managed code to the native code
 * and back. Calls from a {@literal @}{@code FastNative} method implementation to JNI functions
 * that access the managed heap or call managed code also have faster internal transitions.
 *
 * <p>
 * While executing a {@literal @}{@code FastNative} method, the garbage collection cannot
 * suspend the thread for essential work and may become blocked. Use with caution. Do not use
 * this annotation for long-running methods, including usually-fast, but generally unbounded,
 * methods. In particular, the code should not perform significant I/O operations or acquire
 * native locks that can be held for a long time. (Some logging or native allocations, which
 * internally acquire native locks for a short time, are generally OK. However, as the cost
 * of several such operations adds up, the {@literal @}{@code FastNative} performance gain
 * can become insignificant and overshadowed by potential GC delays.)
 * Acquiring managed locks is OK as it internally allows thread suspension.
 * </p>
 *
 * <p>
 * For performance critical methods that need this annotation, it is strongly recommended
 * to explicitly register the method(s) with JNI {@code RegisterNatives} instead of relying
 * on the built-in dynamic JNI linking.
 * </p>
 *
 * <p>
 * The {@literal @}{@code FastNative} optimization was implemented for system use since
 * Android 8 and became CTS-tested public API in Android 14. Developers aiming for maximum
 * compatibility should avoid calling {@literal @}{@code FastNative} methods on Android 13-.
 * The optimization is likely to work also on Android 8-13 devices (after all, it was used
 * in the system, albeit without the strong CTS guarantees), especially those that use
 * unmodified versions of ART, such as Android 12+ devices with the official ART Module.
 * The built-in dynamic JNI linking is working only in Android 12+, the explicit registration
 * with JNI {@code RegisterNatives} is strictly required for running on Android versions 8-11.
 * The annotation is ignored on Android 7-.
 * </p>
 *
 * <p>
 * <b>Deadlock Warning:</b> As a rule of thumb, any native locks acquired in a
 * {@literal @}{@link FastNative} call (despite the above warning that this is an unbounded
 * operation that can block GC for a long time) must be released before returning to managed code.
 * </p>
 *
 * <p>
 * Say some code does:
 * <code>
 * fast_jni_call_to_grab_a_lock();
 * does_some_java_work();
 * fast_jni_call_to_release_a_lock();
 * </code>
 *
 * <p>
 * This code can lead to deadlocks. Say thread 1 just finishes
 * {@code fast_jni_call_to_grab_a_lock()} and is in {@code does_some_java_work()}.
 * GC kicks in and suspends thread 1. Thread 2 now is in {@code fast_jni_call_to_grab_a_lock()}
 * but is blocked on grabbing the native lock since it's held by thread 1.
 * Now thread suspension can't finish since thread 2 can't be suspended since it's doing
 * FastNative JNI.
 * </p>
 *
 * <p>
 * Normal JNI doesn't have the issue since once it's in native code,
 * it is considered suspended from java's point of view.
 * FastNative JNI however doesn't do the state transition done by JNI.
 * </p>
 *
 * <p>
 * Note that even in FastNative methods you <b>are</b> allowed to
 * allocate objects and make upcalls into Java code. A call from Java to
 * a FastNative function and back to Java is equivalent to a call from one Java
 * method to another. What's forbidden in a FastNative method is blocking
 * the calling thread in some non-Java code and thereby preventing the thread
 * from responding to requests from the garbage collector to enter the suspended
 * state.
 * </p>
 *
 * <p>
 * Has no effect when used with non-native methods.
 * </p>
 */
@Retention(RetentionPolicy.CLASS)  // Save memory, don't instantiate as an object at runtime.
@Target(ElementType.METHOD)
public @interface FastNative {
}
