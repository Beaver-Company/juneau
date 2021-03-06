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
package org.apache.juneau.rest.test;

import static org.junit.Assert.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

public class OptionsWithoutNlsTest extends RestTestcase {

	private static String URL = "/testOptionsWithoutNls";
	private RestClient client = TestMicroservice.DEFAULT_CLIENT;

	//====================================================================================================
	// Should get to the options page without errors
	//====================================================================================================
	@Test
	public void testOptions() throws Exception {
		RestCall r = client.doOptions(URL + "/testOptions");
		Swagger o = r.getResponse(Swagger.class);
		assertNotNull(o.getInfo());
	}

	//====================================================================================================
	// Missing resource bundle should cause {!!x} string.
	//====================================================================================================
	@Test
	public void testMissingResourceBundle() throws Exception {
		RestCall r = client.doGet(URL + "/testMissingResourceBundle");
		String o = r.getResponse(String.class);
		assertEquals("{!!bad}", o);
	}
}
