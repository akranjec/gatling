/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.recorder.http.handler

import scala.collection.JavaConversions.asScalaBuffer

import org.jboss.netty.channel.{ Channel, ChannelHandlerContext, ExceptionEvent, MessageEvent, SimpleChannelHandler }
import org.jboss.netty.handler.codec.http.{ DefaultHttpRequest, HttpRequest }

import com.ning.http.util.Base64
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.http.HeaderNames
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.util.URIHelper

object ClientRequestHandler {
	def buildRequestWithRelativeURI(request: HttpRequest) = {

		val (_, pathQuery) = URIHelper.splitURI(request.getUri)
		val newRequest = new DefaultHttpRequest(request.getProtocolVersion, request.getMethod, pathQuery)
		newRequest.setChunked(request.isChunked)
		newRequest.setContent(request.getContent)
		for (header <- request.headers.entries) newRequest.headers.add(header.getKey, header.getValue)
		newRequest
	}
}

abstract class ClientRequestHandler(proxy: HttpProxy) extends SimpleChannelHandler with StrictLogging {

	var _serverChannel: Option[Channel] = None

	override def messageReceived(ctx: ChannelHandlerContext, event: MessageEvent) {

		event.getMessage match {
			case request: HttpRequest =>
				proxy.outgoingHost.map { _ =>
					for {
						username <- proxy.outgoingUsername
						password <- proxy.outgoingPassword
					} {
						val proxyAuth = "Basic " + Base64.encode((username + ":" + password).getBytes)
						request.headers.set(HeaderNames.PROXY_AUTHORIZATION, proxyAuth)
					}
				}.getOrElse(request.headers.remove("Proxy-Connection")) // remove Proxy-Connection header if it's not significant

				propagateRequest(ctx, request)

				proxy.controller.receiveRequest(request)

			case unknown => logger.warn(s"Received unknown message: $unknown")
		}
	}

	def propagateRequest(requestContext: ChannelHandlerContext, request: HttpRequest)

	override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
		logger.error("Exception caught", e.getCause)
		ctx.getChannel.close
		_serverChannel.map(_.close)
	}
}
