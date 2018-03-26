/**
 * Released Under the MIT license
 *
 * Copyright (c) 2009 Joe Gregorio, Tom Zellman
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *   and associated documentation files (the "Software"), to deal in the Software without
 *   restriction, including without limitation the rights to use, copy, modify, merge, publish,
 *   distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 *   Software is furnished to do so, subject to the following conditions. The above copyright
 *   notice and this permission notice shall be included in all copies or substantial portions of
 *   the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 *   BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 *   DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.disney.groovity.servlet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * MIME-Type Parser
 * 
 * This class provides basic functions for handling mime-types. It can handle
 * matching mime-types against a list of media-ranges. See section 14.1 of the
 * HTTP specification [RFC 2616] for a complete explanation.
 * 
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1
 * 
 * A port to Java of Joe Gregorio's MIME-Type Parser:
 * 
 * http://code.google.com/p/mimeparse/
 * 
 * Ported by Tom Zellman &lt;tzellman@gmail.com&gt;.
 * 
 */
public final class TypeNegotiator
{

    /**
     * Parse results container
     */
    protected static class ParseResults
    {
        String type;

        String subType;

        // !a dictionary of all the parameters for the media range
        Map<String, String> params;

        @Override
        public String toString()
        {
            StringBuffer s = new StringBuffer("('" + type + "', '" + subType
                    + "', {");
            for (String k : params.keySet())
                s.append("'" + k + "':'" + params.get(k) + "',");
            return s.append("})").toString();
        }
    }

    /**
     * Carves up a mime-type and returns a ParseResults object
     * 
     * For example, the media range 'application/xhtml;q=0.5' would get parsed
     * into:
     * 
     * ('application', 'xhtml', {'q', '0.5'})
     */
    protected static ParseResults parseMimeType(String mimeType)
    {
        String[] parts = mimeType.split(";");
        ParseResults results = new ParseResults();
        results.params = new HashMap<String, String>();

        for (int i = 1; i < parts.length; ++i)
        {
            String p = parts[i];
            String[] subParts = p.split("=");
            if (subParts.length == 2)
                results.params.put(subParts[0].trim(), subParts[1].trim());
        }
        String fullType = parts[0].trim();

        // Java URLConnection class sends an Accept header that includes a
        // single "*" - Turn it into a legal wildcard.
        if (fullType.equals("*"))
            fullType = "*/*";
        String[] types = fullType.split("/");
        results.type = types[0].trim();
        results.subType = types[1].trim();
        return results;
    }

    /**
     * Carves up a media range and returns a ParseResults.
     * 
     * For example, the media range 'application/*;q=0.5' would get parsed into:
     * 
     * ('application', '*', {'q', '0.5'})
     * 
     * In addition this function also guarantees that there is a value for 'q'
     * in the params dictionary, filling it in with a proper default if
     * necessary.
     * 
     * @param range
     */
    protected static ParseResults parseMediaRange(String range)
    {
        ParseResults results = parseMimeType(range);
        String q = results.params.get("q");
        if(q==null || q.trim().equals("")){
        	results.params.put("q", "1");
        }
        else{
        	float f = Float.parseFloat(q);
            if (f < 0 || f > 1){
                results.params.put("q", "1");
            }
        }
        
        return results;
    }

    /**
     * Structure for holding a fitness/quality combo
     */
    protected static class FitnessAndQuality implements
            Comparable<FitnessAndQuality>
    {
    	int position;
    	
        int fitness;

        float quality;

        String mimeType; // optionally used

        public FitnessAndQuality(int position, int fitness, float quality)
        {
            this.fitness = fitness;
            this.quality = quality;
            this.position=position;
        }

        public int compareTo(FitnessAndQuality o)
        {
            if (fitness == o.fitness)
            {
                if (quality == o.quality)
                    return o.position-position;
                else
                    return quality < o.quality ? -1 : 1;
            }
            else
                return fitness < o.fitness ? -1 : 1;
        }
    }

    /**
     * Find the best match for a given mimeType against a list of media_ranges
     * that have already been parsed by MimeParse.parseMediaRange(). Returns a
     * tuple of the fitness value and the value of the 'q' quality parameter of
     * the best match, or (-1, 0) if no match was found. Just as for
     * quality_parsed(), 'parsed_ranges' must be a list of parsed media ranges.
     * 
     * @param mimeType
     * @param parsedRanges
     */
    protected static FitnessAndQuality fitnessAndQualityParsed(int position, String mimeType,
            Collection<ParseResults> parsedRanges)
    {
        int bestFitness = -1;
        float bestFitQ = 0;
        ParseResults target = parseMediaRange(mimeType);

        for (ParseResults range : parsedRanges)
        {
            if ((target.type.equals(range.type) || range.type.equals("*") || target.type
                    .equals("*"))
                    && (target.subType.equals(range.subType)
                            || range.subType.equals("*") || target.subType
                            .equals("*")))
            {
                for (String k : target.params.keySet())
                {
                    int paramMatches = 0;
                    if (!k.equals("q") && range.params.containsKey(k)
                            && target.params.get(k).equals(range.params.get(k)))
                    {
                        paramMatches++;
                    }
                    int fitness = (range.type.equals(target.type)) ? 100 : 0;
                    fitness += (range.subType.equals(target.subType)) ? 10 : 0;
                    fitness += paramMatches;
                    if (fitness > bestFitness)
                    {
                        bestFitness = fitness;
                        String q = range.params.get("q");
                        bestFitQ = (q==null || q.equals(""))?0:Float.parseFloat(q);
                    }
                }
            }
        }
        return new FitnessAndQuality(position,bestFitness, bestFitQ);
    }

    /**
     * Find the best match for a given mime-type against a list of ranges that
     * have already been parsed by parseMediaRange(). Returns the 'q' quality
     * parameter of the best match, 0 if no match was found. This function
     * bahaves the same as quality() except that 'parsed_ranges' must be a list
     * of parsed media ranges.
     * 
     * @param mimeType
     * @param parsedRanges
     * @return
     */
    protected static float qualityParsed(int pos, String mimeType,
            Collection<ParseResults> parsedRanges)
    {
        return fitnessAndQualityParsed(pos, mimeType, parsedRanges).quality;
    }

    /**
     * Returns the quality 'q' of a mime-type when compared against the
     * mediaRanges in ranges. For example:
     * 
     * @param pos
     * @param mimeType
     * @param ranges
     */
    public static float quality(int pos, String mimeType, String ranges)
    {
        List<ParseResults> results = new LinkedList<ParseResults>();
        for (String r : ranges.split(","))
            results.add(parseMediaRange(r));
        return qualityParsed(pos, mimeType, results);
    }

    /**
     * Takes a list of supported mime-types and finds the best match for all the
     * media-ranges listed in header. The value of header must be a string that
     * conforms to the format of the HTTP Accept: header. The value of
     * 'supported' is a list of mime-types.
     * 
     * MimeParse.bestMatch(Arrays.asList(new String[]{"application/xbel+xml",
     * "text/xml"}), "text/*;q=0.5,*; q=0.1") 'text/xml'
     * 
     * @param supported
     * @param header
     * @return
     */
    public static String bestMatch(List<String> supported, String header)
    {
        List<ParseResults> parseResults = new LinkedList<ParseResults>();
        List<FitnessAndQuality> weightedMatches = new LinkedList<FitnessAndQuality>();
        for (String r : header.split(","))
            parseResults.add(parseMediaRange(r));

        for(int i=0;i<supported.size();i++){
        	String s = supported.get(i);
            FitnessAndQuality fitnessAndQuality = fitnessAndQualityParsed(i,s,
                    parseResults);
            fitnessAndQuality.mimeType = s;
            weightedMatches.add(fitnessAndQuality);
        }
        Collections.sort(weightedMatches);

        FitnessAndQuality lastOne = weightedMatches
                .get(weightedMatches.size() - 1);
        return lastOne.quality != 0 ? lastOne.mimeType : "";
    }

}