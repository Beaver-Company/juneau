// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.html;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.xml.*;

/**
 * Parses text generated by the {@link HtmlSerializer} class back into a POJO model.
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Content-Type</code> types: <code>text/html</code>
 *
 * <h5 class='section'>Description:</h5>
 *
 * See the {@link HtmlSerializer} class for a description of the HTML generated.
 * <p>
 * This class is used primarily for automated testing of the {@link HtmlSerializer} class.
 */
public class HtmlParser extends XmlParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, all default settings.*/
	public static final HtmlParser DEFAULT = new HtmlParser(PropertyStore.create());


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final HtmlParserContext ctx;

	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public HtmlParser(PropertyStore propertyStore) {
		super(propertyStore, "text/html", "text/html+stripped");
		this.ctx = createContext(HtmlParserContext.class);
	}

	@Override /* CoreObject */
	public HtmlParserBuilder builder() {
		return new HtmlParserBuilder(propertyStore);
	}

	@Override /* Parser */
	public HtmlParserSession createSession(ParserSessionArgs args) {
		return new HtmlParserSession(ctx, args);
	}
}
