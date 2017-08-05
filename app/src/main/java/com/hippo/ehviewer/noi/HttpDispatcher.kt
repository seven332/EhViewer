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

import java.util.ArrayDeque
import java.util.concurrent.Executor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/*
 * Created by Hippo on 2017/8/1.
 */

class HttpDispatcher(
    private val maxRequests: Int = 64,
    private val maxRequestsPerHost: Int = 5,
    private val executor: Executor = ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, SynchronousQueue<Runnable>())
) {

  private val lowRequests = ArrayDeque<HttpRequest>()
  private val mediumRequests = ArrayDeque<HttpRequest>()
  private val highRequests = ArrayDeque<HttpRequest>()
  private val runningRequests = ArrayDeque<HttpRequest>()
  private val runningRequestsPerHost = hashMapOf<String, Int>()

  /** Returns `true` if the request added. **/
  private fun tryAddingRequestForHost(host: String): Boolean {
    val requestsPerHost = runningRequestsPerHost[host] ?: 0
    if (requestsPerHost < maxRequestsPerHost) {
      runningRequestsPerHost[host] = requestsPerHost + 1
      return true
    } else {
      return false
    }
  }

  private fun deleteRequestForHost(host: String) {
    val requestsPerHost = runningRequestsPerHost[host]
    if (requestsPerHost == null || requestsPerHost <= 0) {
      error("Requests per host is invalid in finished(): $requestsPerHost")
    } else if (requestsPerHost == 1) {
      runningRequestsPerHost.remove(host)
    } else {
      runningRequestsPerHost[host] = requestsPerHost - 1
    }
  }

  /** Return `true` if the request doesn't allow duplicate url and duplicate url is running **/
  private fun checkDuplicateUrl(request: HttpRequest): Boolean {
    if (request.concurrentDuplicateUrl) return false

    val url = request.url()
    return runningRequests.any { it.url() == url }
  }

  fun enqueue(request: HttpRequest) {
    synchronized(this) {
      if (runningRequests.size < maxRequests &&
          tryAddingRequestForHost(request.url().host()) &&
          !checkDuplicateUrl(request)) {
        runningRequests.add(request)
        executor.execute(request)
      } else {
        when (request.priority) {
          Priority.LOW -> lowRequests
          Priority.MEDIUM -> mediumRequests
          Priority.HIGH -> highRequests
        }.add(request)
      }
    }
  }

  fun cancel(request: HttpRequest) {
    request.cancel()

    synchronized(this) {
      if (!runningRequests.contains(request)) {
        when (request.priority) {
          Priority.LOW -> lowRequests
          Priority.MEDIUM -> mediumRequests
          Priority.HIGH -> highRequests
        }.remove(request)
      }
    }
  }

  fun finished(request: HttpRequest) {
    synchronized(this) {
      runningRequests.remove(request)
      deleteRequestForHost(request.url().host())

      // Promote request
      if (runningRequests.size >= maxRequests) return
      arrayOf(highRequests, mediumRequests, lowRequests).forEach { requests ->
        val iterator = requests.iterator()
        while (iterator.hasNext()) {
          val req = iterator.next()

          if (tryAddingRequestForHost(req.url().host()) && !checkDuplicateUrl(req)) {
            iterator.remove()
            runningRequests.add(req)
            executor.execute(req)
          }

          if (runningRequests.size >= maxRequests) return
        }
      }
    }
  }
}
