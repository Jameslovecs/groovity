/*******************************************************************************
 * © 2018 Disney | ABC Television Group
 *
 * Licensed under the Apache License, Version 2.0 (the "Apache License")
 * with the following modification; you may not use this file except in
 * compliance with the Apache License and the following modification to it:
 * Section 6. Trademarks. is deleted and replaced with:
 *
 * 6. Trademarks. This License does not grant permission to use the trade
 *     names, trademarks, service marks, or product names of the Licensor
 *     and its affiliates, except as required to comply with Section 4(c) of
 *     the License and to reproduce the content of the NOTICE file.
 *
 * You may obtain a copy of the Apache License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License with the above modification is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the Apache License for the specific
 * language governing permissions and limitations under the Apache License.
 *******************************************************************************/
package com.disney.http.auth.server;


/**
 * Interface for a verifier used to authenticate and authorize servlet requests; implementations include basic, digest and signature verifiers
 *
 * @author Alex Vigdor
 */
public interface Verifier{
	/**
	 * Given an incoming request, determine whether the request is authorized according to the rules
	 * for the verifier implementation.  If the implementation calls for a WWW-Authenticate challenge
	 * on failure, it should be crafted and attached to the VerifierResult
	 * 
	 * If verification is successful, the verifier should also attach either the original request, or a wrapped request
	 * e.g. to lazily validate content
	 * 
	 * @param request
	 * @throws Exception 
	 */
	public VerifierResult verify(ServerAuthorizationRequest request) throws Exception;
}
