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
package org.apache.juneau.http;

/**
 * Represents a parsed <l>If-Match</l> HTTP request header.
 * <p>
 * Only perform the action if the client supplied entity matches the same entity on the server.
 * This is mainly for methods like PUT to only update a resource if it has not been modified since the user last
 * updated it.
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	If-Match: "737060cd8c284d8af7ad3082f209582d"
 * </p>
 *
 * <h6 class='topic'>RFC2616 Specification</h6>
 *
 * The If-Match request-header field is used with a method to make it conditional.
 * A client that has one or more entities previously obtained from the resource can verify that one of those entities
 * is current by including a list of their associated entity tags in the If-Match header field.
 * Entity tags are defined in section 3.11.
 * The purpose of this feature is to allow efficient updates of cached information with a minimum amount of transaction
 * overhead.
 * It is also used, on updating requests, to prevent inadvertent modification of the wrong version of a resource.
 * As a special case, the value "*" matches any current entity of the resource.
 * <p class='bcode'>
 * 	If-Match = "If-Match" ":" ( "*" | 1#entity-tag )
 * </p>
 * <p>
 * If any of the entity tags match the entity tag of the entity that would have been returned in the response to a
 * similar GET request (without the If-Match header) on that resource, or if "*" is given and any current entity exists
 * for that resource, then the server MAY perform the requested method as if the If-Match header field did not exist.
 * <p>
 * A server MUST use the strong comparison function (see section 13.3.3) to compare the entity tags in If-Match.
 * <p>
 * If none of the entity tags match, or if "*" is given and no current entity exists, the server MUST NOT perform the
 * requested method, and MUST return a 412 (Precondition Failed) response.
 * This behavior is most useful when the client wants to prevent an updating method, such as PUT, from modifying a
 * resource that has changed since the client last retrieved it.
 * <p>
 * If the request would, without the If-Match header field, result in anything other than a 2xx or 412 status, then the
 * If-Match header MUST be ignored.
 * <p>
 * The meaning of "If-Match: *" is that the method SHOULD be performed if the representation selected by the origin
 * server (or by a cache, possibly using the Vary mechanism, see section 14.44) exists, and MUST NOT be performed if the
 * representation does not exist.
 * <p>
 * A request intended to update a resource (e.g., a PUT) MAY include an If-Match header field to signal that the request
 * method MUST NOT be applied if the entity corresponding to the If-Match value (a single entity tag) is no longer a
 * representation of that resource.
 * This allows the user to indicate that they do not wish the request to be successful if the resource has been changed
 * without their knowledge.
 * Examples:
 * <p class='bcode'>
 * 	If-Match: "xyzzy"
 * 	If-Match: "xyzzy", "r2d2xxxx", "c3piozzzz"
 * 	If-Match: *
 * </p>
 * The result of a request having both an If-Match header field and either an If-None-Match or an If-Modified-Since
 * header fields is undefined by this specification.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class IfMatch extends HeaderEntityValidatorArray {

	/**
	 * Returns a parsed <code>If-Match</code> header.
	 *
	 * @param value The <code>If-Match</code> header string.
	 * @return The parsed <code>If-Match</code> header, or <jk>null</jk> if the string was null.
	 */
	public static IfMatch forString(String value) {
		if (value == null)
			return null;
		return new IfMatch(value);
	}

	private IfMatch(String value) {
		super(value);
	}
}