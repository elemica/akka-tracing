/**
 * Copyright 2014 the Akka Tracing contributors. See AUTHORS for more details.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.levkhomich.akka.tracing

import scala.util.Random

trait BaseTracingSupport extends Serializable {
  def spanId: Long
  def traceId: Option[Long]
  def parentId: Option[Long]

  def asChildOf(ts: BaseTracingSupport)(implicit tracer: TracingExtensionImpl): this.type

  private[tracing] def setTraceId(newTraceId: Option[Long]): Unit
}

/**
 * Trait to be mixed with messages that should support tracing.
 */
trait TracingSupport extends BaseTracingSupport {

  var spanId = Random.nextLong()
  var traceId: Option[Long] = None
  var parentId: Option[Long] = None

  /**
   * Declares message as a child of another message.
   * @param ts parent message
   * @return child message with required tracing headers
   */
  def asChildOf(ts: BaseTracingSupport)(implicit tracer: TracingExtensionImpl): this.type = {
    tracer.createChildSpan(spanId, ts)
    parentId = Some(ts.spanId)
    traceId = ts.traceId
    this
  }

  def setTraceId(newTraceId: Option[Long]): Unit = {
    traceId = newTraceId
  }

  private[tracing] def init(spanId: Long, traceId: Long, parentId: Option[Long]): Unit = {
    this.spanId = spanId
    this.traceId = Some(traceId)
    this.parentId = parentId
  }

}

class ResponseTracingSupport[T](val msg: T) extends AnyVal {

  /**
   * Declares message as a response to another message.
   * @param request parent message
   * @return unchanged message
   */
  def asResponseTo(request: BaseTracingSupport)(implicit trace: TracingExtensionImpl): T = {
    trace.record(request, "response: " + msg)
    trace.recordServerSend(request)
    msg
  }
}

