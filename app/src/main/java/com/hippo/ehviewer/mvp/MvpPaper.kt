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

package com.hippo.ehviewer.mvp

import android.content.Context
import android.os.Bundle
import android.support.annotation.CallSuper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.Disposables
import io.reactivex.exceptions.Exceptions

/*
 * Created by Hippo on 2017/7/13.
 */

abstract class MvpPaper<This : Any>(
    private val logic: MvpLogic<This>
) : MvpUi {

  companion object {
    private const val INIT = 0
    const val CREATE = 1
    const val ATTACH = 2
    const val START = 3
    const val RESUME = 4
    const val PAUSE = 5
    const val STOP = 6
    const val DETACH = 7
    const val DESTROY = 8
  }

  lateinit var inflater: LayoutInflater
    private set
  lateinit var context: Context
    private set

  lateinit var view: View
    protected set

  private var step = INIT
  private var lifecycleHandler: LifecycleHandler? = null
  val lifecycle by lazy {
    val lifecycleHandler = LifecycleHandler(step)
    this.lifecycleHandler = lifecycleHandler
    lifecycleHandler.observable
  }

  /**
   * Creates the paper.
   */
  internal fun create(inflater: LayoutInflater, container: ViewGroup) {
    this.inflater = inflater
    this.context = inflater.context

    onCreate(inflater, container)
    step = CREATE
    lifecycleHandler?.emit(step)

    @Suppress("UNCHECKED_CAST")
    logic.attach(this as This)
  }

  /**
   * Attaches the paper.
   */
  internal fun attach() {
    onAttach()
    step = ATTACH
    lifecycleHandler?.emit(step)
  }

  /**
   * Starts the paper.
   */
  internal fun start() {
    onStart()
    step = START
    lifecycleHandler?.emit(step)
  }

  /**
   * Resumes the paper.
   */
  internal fun resume() {
    onResume()
    step = RESUME
    lifecycleHandler?.emit(step)
  }

  /**
   * Pauses the paper.
   */
  internal fun pause() {
    onPause()
    step = PAUSE
    lifecycleHandler?.emit(step)
  }

  /**
   * Stops the paper.
   */
  internal fun stop() {
    onStop()
    step = STOP
    lifecycleHandler?.emit(step)
  }

  /**
   * Detaches the paper.
   */
  internal fun detach() {
    onDetach()
    step = DETACH
    lifecycleHandler?.emit(step)
  }

  /**
   * Destroys the paper.
   */
  internal fun destroy() {
    onDestroy()
    step = DESTROY
    lifecycleHandler?.emit(step)

    logic.detach()
  }

  /**
   * Save state of the paper.
   */
  internal fun saveState(outState: Bundle) {
    onSaveState(outState)
  }

  /**
   * Restore state of the paper.
   */
  internal fun restoreState(savedState: Bundle) {
    onRestoreState(savedState)
  }

  /**
   * Called when the paper created.
   */
  @CallSuper
  protected open fun onCreate(inflater: LayoutInflater, container: ViewGroup) {}

  /**
   * Called when the paper attached.
   */
  @CallSuper
  protected open fun onAttach() {}

  /**
   * Called when the paper started.
   */
  @CallSuper
  protected open fun onStart() {}

  /**
   * Called when the paper resumed.
   */
  @CallSuper
  protected open fun onResume() {}

  /**
   * Called when the paper paused.
   */
  @CallSuper
  protected open fun onPause() {}

  /**
   * Called when the paper stopped.
   */
  @CallSuper
  protected open fun onStop() {}

  /**
   * Called when the paper detached.
   */
  @CallSuper
  protected open fun onDetach() {}

  /**
   * Called when the paper destroyed.
   */
  @CallSuper
  protected open fun onDestroy() {}

  /**
   * Called when the paper state saved.
   */
  @CallSuper
  protected open fun onSaveState(outState: Bundle) {}

  /**
   * Called when the paper state restored.
   */
  @CallSuper
  protected open fun onRestoreState(savedState: Bundle) {}


  private class LifecycleHandler(var step: Int) {

    private val emitters = mutableListOf<ObservableEmitter<Int>>()

    val observable : Observable<Int> by lazy {
      Observable.create<Int> {
        if (step != DESTROY) {
          emitters.add(it)
          it.setDisposable(Disposables.fromAction { emitters.remove(it) })
          emitMissingStep(it)
        } else {
          it.setDisposable(Disposables.disposed())
        }
      }
    }

    private fun emitMissingStep(emitter: ObservableEmitter<Int>) {
      if (step in CREATE until DESTROY) emit(emitter, CREATE)
      if (step in ATTACH until DETACH) emit(emitter, ATTACH)
      if (step in START until STOP) emit(emitter, START)
      if (step in RESUME until PAUSE) emit(emitter, RESUME)
    }

    fun emit(step: Int) {
      this.step = step
      emitters.forEach { emit(it, step) }

      if (step == DESTROY) {
        emitters.clear()
      }
    }

    private fun emit(emitter: ObservableEmitter<Int>, step: Int) {
      if (!emitter.isDisposed) {
        try {
          emitter.onNext(step)
        } catch (t: Throwable) {
          Exceptions.throwIfFatal(t)
          emitter.onError(t)
        }
      }
    }
  }
}
