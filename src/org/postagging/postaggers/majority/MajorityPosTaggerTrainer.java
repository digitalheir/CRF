package org.postagging.postaggers.majority;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.postagging.data.PosTagCorpus;
import org.postagging.data.PosTagCorpusReader;
import org.postagging.postaggers.PosTagger;
import org.postagging.postaggers.PosTaggerTrainer;
import org.postagging.utilities.TaggedToken;
import org.postagging.utilities.PosTaggerException;

/**
 * Trains a {@link MajorityPosTagger} from a given corpus.
 * For each token the trainer counts how many times each tag was assigned to that token in the corpus.
 * Then, for each token, the tag that was the most frequent for that token is considered as the tag to be used in
 * annotating a test example.
 *  
 * @author Asher Stern
 * Date: Nov 4, 2014
 *
 */
public class MajorityPosTaggerTrainer implements PosTaggerTrainer<PosTagCorpus<String,String>>
{

	@Override
	public void train(PosTagCorpus<String,String> corpus)
	{
		processCorpus(corpus);
		
		majorityForToken = calculateMajorityForTokens();
		majorGeneralTag = getMajority(majorityMapGeneralTag);
		
		posTagger = new MajorityPosTagger(majorityForToken, majorGeneralTag);
	}

	@Override
	public PosTagger getTrainedPosTagger()
	{
		if (null==posTagger) {throw new PosTaggerException("Not yet trained.");}
		return posTagger;
	}

	@Override
	public void save(File modelDirectory)
	{
		throw new PosTaggerException("Not implemented");
		
	}

	
	private void addToMaps(String token, String tag)
	{
		if (!(majorityMapPerToken.containsKey(token)))
		{
			majorityMapPerToken.put(token, new LinkedHashMap<String, Integer>());
		}
		Map<String, Integer> tokenMap = majorityMapPerToken.get(token);
		if (tokenMap.containsKey(tag))
		{
			int count = tokenMap.get(tag);
			++count;
			tokenMap.put(tag,count);
		}
		else
		{
			tokenMap.put(tag, 1);
		}
		
		int generalCount = 0;
		if (majorityMapGeneralTag.containsKey(tag))
		{
			generalCount = majorityMapGeneralTag.get(tag);
		}
		majorityMapGeneralTag.put(tag, generalCount+1);
	}
	
	
	private void processSentence(List<? extends TaggedToken<String, String>> sentence)
	{
		for (TaggedToken<String, String> token : sentence)
		{
			addToMaps(token.getToken(),token.getTag());
		}
	}
	
	private void processCorpus(PosTagCorpus<String,String> corpus)
	{
		majorityMapPerToken = new LinkedHashMap<String, Map<String,Integer>>();
		majorityMapGeneralTag = new LinkedHashMap<String, Integer>();
		PosTagCorpusReader<String,String> reader = corpus.iterator();
		
		int index = 0;
		while(reader.hasNext())
		{
			++index;
			List<? extends TaggedToken<String, String>> sentence = reader.next();
			processSentence(sentence);
			if (logger.isDebugEnabled()) {if (0==(index%10000)){logger.debug("Already processed: "+index+" sentences.");} }
		}
	}
	
	private String getMajority(Map<String, Integer> map)
	{
		String ret = null;
		int max = 0;
		for (String str : map.keySet())
		{
			int count = map.get(str);
			if (count > max)
			{
				ret = str;
				max = count;
			}
		}
		return ret;
	}
	
	private Map<String, String> calculateMajorityForTokens()
	{
		Map<String, String> ret = new LinkedHashMap<String, String>();
		for (String token : majorityMapPerToken.keySet())
		{
			String majorTag = getMajority(majorityMapPerToken.get(token));
			ret.put(token, majorTag);
		}
		return ret;
	}
	

	private Map<String ,Map<String, Integer>> majorityMapPerToken;
	private Map<String, Integer> majorityMapGeneralTag;
	
	
	
	private Map<String, String> majorityForToken = null;
	private String majorGeneralTag = null;
	private MajorityPosTagger posTagger = null;
	
	
	private static final Logger logger = Logger.getLogger(MajorityPosTaggerTrainer.class);
}
