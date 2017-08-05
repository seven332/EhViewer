/*
 * Copyright 2017 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.noi

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.Executor
import kotlin.test.assertEquals

/*
 * Created by Hippo on 2017/8/2.
 */

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class HttpDispatcherTest {

  private val executor = RecordingExecutor()
  private val dispatcher = HttpDispatcher(maxRequests = 3, maxRequestsPerHost = 2, executor = executor)
  private val noi = Noi(context = RuntimeEnvironment.application, dispatcher = dispatcher)

  @Test
  fun testMaxRequests() {
    val expected = mutableListOf<Runnable>()

    val request1 = noi.http("http://a/1")
    request1.asResponse().subscribe()
    expected.add(request1)
    executor.assertRequests(expected)

    val request2 = noi.http("http://b/1")
    request2.asResponse().subscribe()
    expected.add(request2)
    executor.assertRequests(expected)

    val request3 = noi.http("http://c/1")
    request3.asResponse().subscribe()
    expected.add(request3)
    executor.assertRequests(expected)

    val request4 = noi.http("http://d/1")
    request4.asResponse().subscribe()
    executor.assertRequests(expected)

    executor.finish(request2)
    expected.remove(request2)
    expected.add(request4)
    executor.assertRequests(expected)

    val request5 = noi.http("http://e/1")
    request5.asResponse().subscribe()
    executor.assertRequests(expected)
  }

  @Test
  fun testMaxRequestsPerHost() {
    val expected = mutableListOf<Runnable>()

    val request1 = noi.http("http://a/1")
    request1.asResponse().subscribe()
    expected.add(request1)
    executor.assertRequests(expected)

    val request2 = noi.http("http://a/2")
    request2.asResponse().subscribe()
    expected.add(request2)
    executor.assertRequests(expected)

    val request3 = noi.http("http://a/3")
    request3.asResponse().subscribe()
    executor.assertRequests(expected)

    executor.finish(request2)
    expected.remove(request2)
    expected.add(request3)
    executor.assertRequests(expected)

    val request4 = noi.http("http://a/4")
    request4.asResponse().subscribe()
    executor.assertRequests(expected)
  }

  @Test
  fun testPriority() {
    val expected = mutableListOf<Runnable>()

    val request1 = noi.http("http://a/1")
    request1.asResponse().subscribe()
    expected.add(request1)
    executor.assertRequests(expected)

    val request2 = noi.http("http://a/2")
    request2.asResponse().subscribe()
    expected.add(request2)
    executor.assertRequests(expected)

    val request3 = noi.http("http://a/3")
    request3.asResponse().subscribe()
    executor.assertRequests(expected)

    val request4 = noi.http("http://a/3")
    request4.priority(Priority.HIGH).asResponse().subscribe()
    executor.assertRequests(expected)

    executor.finish(request2)
    expected.remove(request2)
    expected.add(request4)
    executor.assertRequests(expected)
  }

  @Test
  fun testCancel() {
    val expected = mutableListOf<Runnable>()

    val request1 = noi.http("http://a/1")
    request1.asResponse().subscribe()
    expected.add(request1)
    executor.assertRequests(expected)

    val request2 = noi.http("http://a/2")
    request2.asResponse().subscribe()
    expected.add(request2)
    executor.assertRequests(expected)

    val request3 = noi.http("http://a/3")
    request3.asResponse().subscribe()
    executor.assertRequests(expected)

    dispatcher.cancel(request3)
    executor.finish(request2)
    expected.remove(request2)
    executor.assertRequests(expected)
  }

  @Test
  fun testConcurrentDuplicateUrl() {
    val expected = mutableListOf<Runnable>()

    val request1 = noi.http("http://a/1")
    request1.concurrentDuplicateUrl(false).asResponse().subscribe()
    expected.add(request1)
    executor.assertRequests(expected)

    val request2 = noi.http("http://a/1")
    request2.concurrentDuplicateUrl(false).asResponse().subscribe()
    executor.assertRequests(expected)

    val request3 = noi.http("http://a/1")
    request3.concurrentDuplicateUrl(false).asResponse().subscribe()
    executor.assertRequests(expected)

    executor.finish(request1)
    expected.remove(request1)
    expected.add(request2)
    executor.assertRequests(expected)

    executor.finish(request2)
    expected.remove(request2)
    expected.add(request3)
    executor.assertRequests(expected)

    executor.finish(request3)
    expected.remove(request3)
    executor.assertRequests(expected)
  }

  private inner class RecordingExecutor : Executor {

    private val list = mutableListOf<Runnable?>()

    override fun execute(command: Runnable?) {
      list.add(command)
    }

    fun assertRequests(list: List<Runnable?>) {
      assertEquals(list, this.list)
    }

    fun finish(request: HttpRequest) {
      list.remove(request)
      dispatcher.finished(request)
    }
  }
}
