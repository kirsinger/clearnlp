/**
* Copyright 2012 University of Massachusetts Amherst
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
*   
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.googlecode.clearnlp.component;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.utils.IOUtils;

import com.googlecode.clearnlp.classification.model.StringModel;
import com.googlecode.clearnlp.classification.train.StringTrainSpace;
import com.googlecode.clearnlp.classification.vector.StringFeatureVector;
import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.feature.xml.FtrTemplate;
import com.googlecode.clearnlp.feature.xml.FtrToken;
import com.googlecode.clearnlp.feature.xml.JointFtrXml;
import com.googlecode.clearnlp.reader.AbstractColumnReader;
import com.googlecode.clearnlp.util.UTInput;
import com.googlecode.clearnlp.util.pair.StringIntPair;

/**
 * @since 1.3.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
abstract public class AbstractComponent
{
	protected final byte FLAG_LEXICA	= 0;
	protected final byte FLAG_TRAIN		= 1;
	protected final byte FLAG_DECODE	= 2;
	protected final byte FLAG_BOOTSTRAP	= 3;
	protected final byte FLAG_DEVELOP	= 4;
	
	protected StringTrainSpace[]	s_spaces;
	protected StringModel[]			s_models;
	protected JointFtrXml[]			f_xmls;
	protected DEPTree				d_tree;
	protected byte 					i_flag;
	protected int 					t_size;		// size of d_tree
	
//	====================================== CONSTRUCTORS ======================================
	
	/** Constructs a component for collecting lexica. */
	public AbstractComponent(JointFtrXml[] xmls)
	{
		i_flag = FLAG_LEXICA;
		f_xmls = xmls;
	}
	
	/** Constructs a component for training. */
	public AbstractComponent(JointFtrXml[] xmls, StringTrainSpace[] spaces, Object[] lexica)
	{
		i_flag   = FLAG_TRAIN;
		f_xmls   = xmls;
		s_spaces = spaces;
		
		initLexia(lexica);
	}
	
	/** Constructs a component for developing. */
	public AbstractComponent(JointFtrXml[] xmls, StringModel[] models, Object[] lexica)
	{
		i_flag   = FLAG_DEVELOP;
		f_xmls   = xmls;
		s_models = models;

		initLexia(lexica);
	}
	
	/** Constructs a component for decoding. */
	public AbstractComponent(ZipInputStream zin)
	{
		i_flag = FLAG_DECODE;
		
		loadModels(zin);
	}
	
	/** Constructs a component for bootstrapping. */
	public AbstractComponent(JointFtrXml[] xmls, StringTrainSpace[] spaces, StringModel[] models, Object[] lexica)
	{
		i_flag   = FLAG_BOOTSTRAP;
		f_xmls   = xmls;
		s_spaces = spaces;
		s_models = models;
		
		initLexia(lexica);
	}

	/** Initializes lexica used for this component. */
	abstract protected void initLexia(Object[] lexica);

//	====================================== LOAD/SAVE MODELS ======================================

	/** Loads all models of this joint-component. */
	abstract public void loadModels(ZipInputStream zin);

	/** Called by {@link AbstractComponent#loadModels(ZipInputStream)}}. */
	protected void loadFeatureTemplates(ZipInputStream zin, int index) throws Exception
	{
		System.out.println("Loading feature templates.");
		
		BufferedReader fin = new BufferedReader(new InputStreamReader(zin));
		f_xmls[index] = new JointFtrXml(getFeatureTemplates(fin));
	}
	
	protected ByteArrayInputStream getFeatureTemplates(BufferedReader fin) throws IOException
	{
		StringBuilder build = new StringBuilder();
		String line;

		System.out.println("Loading feature templates.");
		
		while ((line = fin.readLine()) != null)
		{
			build.append(line);
			build.append("\n");
		}
		
		return new ByteArrayInputStream(build.toString().getBytes());
	}
	
	/** Called by {@link AbstractComponent#loadModels(ZipInputStream)}}. */
	protected void loadStatisticalModels(ZipInputStream zin, int index) throws Exception
	{
		BufferedReader fin = new BufferedReader(new InputStreamReader(zin));
		s_models[index] = new StringModel(fin);
	}
	
	/** Saves all models of this joint-component. */
	abstract public void saveModels(ZipOutputStream zout);
	
	/** Called by {@link AbstractComponent#saveModels(ZipOutputStream)}}. */
	protected void saveFeatureTemplates(ZipOutputStream zout, String entryName) throws Exception
	{
		int i, size = f_xmls.length;
		BufferedOutputStream fout;
		System.out.println("Saving feature templates.");
		
		for (i=0; i<size; i++)
		{
			zout.putNextEntry(new ZipEntry(entryName+i));
			fout = new BufferedOutputStream(zout);
			IOUtils.copy(UTInput.toInputStream(f_xmls[i].toString()), fout);
			fout.flush();
			zout.closeEntry();
		}
	}
	
	/** Called by {@link AbstractComponent#saveModels(ZipOutputStream)}}. */
	protected void saveStatisticalModels(ZipOutputStream zout, String entryName) throws Exception
	{
		int i, size = s_models.length;
		PrintStream fout;
		
		for (i=0; i<size; i++)
		{
			zout.putNextEntry(new ZipEntry(entryName+i));
			fout = new PrintStream(new BufferedOutputStream(zout));
			s_models[i].save(fout);
			fout.flush();
			zout.closeEntry();			
		}
	}
	
//	====================================== GETTERS/SETTERS ======================================
	
	/** @return all training spaces of this joint-components. */
	public StringTrainSpace[] getTrainSpaces()
	{
		return s_spaces;
	}
	
	/** @return all models of this joint-components. */
	public StringModel[] getModels()
	{
		return s_models;
	}
	
	/** @return all objects containing lexica. */
	abstract public Object[] getLexica();
	
	/** @return gold-standard tags. */
	abstract public Object[] getGoldTags();
	
//	====================================== PROCESS ======================================

	/** Counts the number of correctly classified labels. */
	abstract public void countAccuracy(int[] counts);
	
	/** Process this joint-component. */
	abstract public void process(DEPTree tree);
	
	protected void countAccuracyDEP(int[] counts, StringIntPair[] gHeads)
	{
		StringIntPair p;
		DEPNode node;
		int i;
		
		counts[0] += t_size - 1;
		
		for (i=1; i<t_size; i++)
		{
			node = d_tree.get(i);
			p    = gHeads[i];
			
			if (node.isHead(d_tree.get(p.i)))
			{
				counts[2]++;
				
				if (node.isLabel(p.s))
					counts[1]++;
			}
			
			if (node.isLabel(p.s))
				counts[3]++;
		}
	}
	
//	====================================== FEATURE EXTRACTION ======================================

	/** @return a field of the specific feature token (e.g., lemma, pos-tag). */
	abstract protected String getField(FtrToken token);
	
	/** @return multiple fields of the specific feature token (e.g., lemma, pos-tag). */
	abstract protected String[] getFields(FtrToken token);
	
	/** @return a feature vector using the specific feature template. */
	protected StringFeatureVector getFeatureVector(JointFtrXml xml)
	{
		StringFeatureVector vector = new StringFeatureVector();
		
		for (FtrTemplate template : xml.getFtrTemplates())
			addFeatures(vector, template);
		
		return vector;
	}

	/** Called by {@link AbstractComponent#getFeatureVector(JointFtrXml)}. */
	private void addFeatures(StringFeatureVector vector, FtrTemplate template)
	{
		FtrToken[] tokens = template.tokens;
		int i, size = tokens.length;
		
		if (template.isSetFeature())
		{
			String[][] fields = new String[size][];
			String[]   tmp;
			
			for (i=0; i<size; i++)
			{
				tmp = getFields(tokens[i]);
				if (tmp == null)	return;
				fields[i] = tmp;
			}
			
			addFeatures(vector, template.type, fields, 0, "");
		}
		else
		{
			StringBuilder build = new StringBuilder();
			String field;
			
			for (i=0; i<size; i++)
			{
				field = getField(tokens[i]);
				if (field == null)	return;
				
				if (i > 0)	build.append(AbstractColumnReader.BLANK_COLUMN);
				build.append(field);
			}
			
			vector.addFeature(template.type, build.toString());			
		}
    }
	
	/** Called by {@link AbstractComponent#getFeatureVector(JointFtrXml)}. */
	private void addFeatures(StringFeatureVector vector, String type, String[][] fields, int index, String prev)
	{
		if (index < fields.length)
		{
			for (String field : fields[index])
			{
				if (prev.isEmpty())
					addFeatures(vector, type, fields, index+1, field);
				else
					addFeatures(vector, type, fields, index+1, prev + AbstractColumnReader.BLANK_COLUMN + field);
			}
		}
		else
			vector.addFeature(type, prev);
	}
	
/*	protected List<List<Pair<String,StringFeatureVector>>> l_instances;
	
	public void addTrainInstances()
	{
		int i, size = l_instances.size();
		
		for (i=0; i<size; i++)
			for (Pair<String,StringFeatureVector> p : l_instances.get(i))
				s_spaces[i].addInstance(p.o1, p.o2);
	}
	
	public void initTrainInstances(int modelSize)
	{
		l_instances = new ArrayList<List<Pair<String,StringFeatureVector>>>(modelSize);
		
		int i; for (i=0; i<modelSize; i++)
			l_instances.add(new ArrayList<Pair<String,StringFeatureVector>>());
	}*/
}