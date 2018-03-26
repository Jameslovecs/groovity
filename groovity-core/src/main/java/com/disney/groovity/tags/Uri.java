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
package com.disney.groovity.tags;

import groovy.lang.Closure;

import java.net.URI;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;

import com.disney.groovity.GroovityConstants;
import com.disney.groovity.Taggable;
import com.disney.groovity.doc.Attr;
import com.disney.groovity.doc.Tag;
/**
 * Used to programmatically construct parameterized URLs
 * <p>
 * uri( <ul>	
 *	<li><b>base</b>: 
 *	The base URL to use,</li>	
 *	<li><i>var</i>: 
 *	The variable name to store the resulting constructed URL,</li>	
 *	<li><i>path</i>: 
 *	The path to use on for the constructed URL, will replace the path if any on the base URL,</li>
 *	</ul>{
 *	<blockquote>// Zero or more g:param tags defining query string parameters for the resulting URL</blockquote>
 * 	});
 *	
 *	<p><b>returns</b> the constructed URL
 *	
 *	<p>Sample
 *	<pre>
 *	&lt;~ &lt;g:uri var=&quot;myUri&quot; base=&quot;http://www.disney.com&quot; path=&quot;/api/characters&quot;&gt;
 *		&lt;g:param name=&quot;characterName&quot; value=&quot;Mickey &amp; Minnie&quot;/&gt;
 *	&lt;/g:uri&gt;
 *	&lt;a href=&quot;${myUri}&quot;&gt;Find Mickey &amp; Minnie&lt;/a&gt; ~&gt;
 *	</pre>	
 * 
 * @author Alex Vigdor
 */ 
@Tag(
		info="Used to programmatically construct parameterized URLs",
		body="Zero or more g:param tags defining query string parameters for the resulting URL",
		sample="<~ <g:uri var=\"myUri\" base=\"http://www.disney.com\" path=\"/api/characters\">\n" + 
				"	<g:param name=\"characterName\" value=\"Mickey & Minnie\"/>\n" + 
				"</g:uri>\n" + 
				"<a href=\"${myUri}\">Find Mickey & Minnie</a> ~>",
		returns = "the constructed URL",
		attrs = {
		         @Attr(name="base",info="The base URL to use", required=true),
		         @Attr(name=GroovityConstants.VAR,info="The variable name to store the resulting constructed URL", required=false),
		         @Attr(name="path",info="The path to use on for the constructed URL, will replace the path if any on the base URL", required=false)
		}
)
public class Uri implements Taggable{
	public static String CURRENT_URI_BUILDER = INTERNAL_BINDING_PREFIX+"uriBuilder";
	
	@SuppressWarnings("rawtypes")
	public Object tag(Map attributes, Closure body) throws Exception {
		Object base = resolve(attributes,"base");
		if(base==null){
			throw new RuntimeException("Uri tag requires 'base' attribute");
		}
		Object var = resolve(attributes,VAR);
		Object path = resolve(attributes,"path");
		URIBuilder builder;
		try {
			builder = new URIBuilder(base.toString());
			bind(body,CURRENT_URI_BUILDER, builder);
			if(path!=null){
				builder.setPath(path.toString());
			}
			body.call();
			URI built = builder.build();
			if(var!=null){
				bind(body,var.toString(), built);
			}
			return built;
		}
		finally{
			unbind(body,CURRENT_URI_BUILDER);
		}
	}

}
