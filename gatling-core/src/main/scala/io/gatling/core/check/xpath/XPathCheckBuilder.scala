/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.check.xpath

import io.gatling.core.check.{ DefaultMultipleFindCheckBuilder, Extractor }
import io.gatling.core.session._

import net.sf.saxon.s9api.XdmNode

trait XPathCheckType

class XPathCheckBuilder(
    path: Expression[String],
    namespaces: Map[String, String],
    xmlParsers: XmlParsers
) extends DefaultMultipleFindCheckBuilder[XPathCheckType, Option[XdmNode], String](displayActualValue = true) {

  override def findExtractor(occurrence: Int): Expression[Extractor[Option[XdmNode], String]] =
    path.map(new XPathFindExtractor(_, namespaces, occurrence, xmlParsers))
  override def findAllExtractor: Expression[Extractor[Option[XdmNode], Seq[String]]] = path.map(new XPathFindAllExtractor(_, namespaces, xmlParsers))
  override def countExtractor: Expression[Extractor[Option[XdmNode], Int]] = path.map(new XPathCountExtractor(_, namespaces, xmlParsers))
}
