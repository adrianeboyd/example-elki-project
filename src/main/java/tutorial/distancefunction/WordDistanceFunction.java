package tutorial.distancefunction;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.language.ColognePhonetic;
import org.apache.commons.lang3.StringUtils;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.unituebingen.sfs.brillmoore.Candidate;
import de.unituebingen.sfs.brillmoore.Misspelling;
import de.unituebingen.sfs.brillmoore.SpellChecker;

public class WordDistanceFunction extends AbstractPrimitiveDistanceFunction<String> {

	private SpellChecker spellchecker;
	private String trainFile = "/home/adriane/research/tools/example-elki-project/rimrott-eagle-spelling.txt";
	
	// probability difference to consider equal
	private int distThreshold = 1000;
	
	// count multiplier for real training examples vs. 
	// minimal all-character insertion/deletion/substitution
	private static int trainingMultiplier = 1000;

	public WordDistanceFunction() {
		// read in files
		List<Misspelling> trainMisspellings = readMisspellings(trainFile);
		Map<String, Double> wordDict = new HashMap<>();
		
		// find all characters in the training data
		Set<Character> allChars = new HashSet<>();
		for (Misspelling m : trainMisspellings) {
			for (char c : m.getSource().toCharArray()) {
				allChars.add(c);
			}
			for (char c : m.getTarget().toCharArray()) {
				allChars.add(c);
			}
		}

		// add a small probability for preserving, inserting, deleting, 
		// or substituting any two characters
		for (char c : allChars) {
			trainMisspellings.add(new Misspelling(String.valueOf(c), String.valueOf(c), 1));
			trainMisspellings.add(new Misspelling(String.valueOf(c), "", 1));
			trainMisspellings.add(new Misspelling("", String.valueOf(c), 1));
			for (char d : allChars) {
				if (c < d) {
					trainMisspellings.add(new Misspelling(String.valueOf(c), String.valueOf(d), 1));
					trainMisspellings.add(new Misspelling(String.valueOf(d), String.valueOf(c), 1));
				}
			}
		}

		// train spell checker
		try {
			spellchecker = new SpellChecker(trainMisspellings, wordDict, 3, 0.8);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	@Override
	public boolean isSymmetric() {
		return false;
	}
	*/

	@Override
	public double distance(String arg0, String arg1) {
		Map<String, Double> singleDict0 = new HashMap<String, Double>();
		singleDict0.put(arg0, 1.0);
		
		Map<String, Double> singleDict1 = new HashMap<String, Double>();
		singleDict1.put(arg1, 1.0);
		
		double prob = distThreshold;
		
		try {
			List<Candidate> candidates0 = spellchecker.getRankedCandidates(arg0, singleDict1);
			List<Candidate> candidates1 = spellchecker.getRankedCandidates(arg1, singleDict0);

			//System.out.println(arg0 + " " + arg1 + " " + candidates0.get(0).getProb() + " " + candidates1.get(0).getProb());

			prob = candidates0.get(0).getProb() + candidates1.get(0).getProb();
			
			prob = Math.min(prob, distThreshold);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return prob;
		
//		int strLevenshtein = StringUtils.getLevenshteinDistance(arg0, arg1);
//
//		ColognePhonetic cpUtil = new ColognePhonetic();
//		int cpLevenshtein = StringUtils.getLevenshteinDistance(cpUtil.encode(arg0), cpUtil.encode(arg1));
//
//		return 0.5 * (double) strLevenshtein + 0.5 * (double) cpLevenshtein;
	}

	public SimpleTypeInformation<? super String> getInputTypeRestriction() {
		return TypeUtil.STRING;
	}

	private static List<Misspelling> readMisspellings(String file) {
		List<Misspelling> misspellings = new ArrayList<Misspelling>();

		// TODO: replace with a CSV reader for robustness?
		// replace with Scanner?
		BufferedReader input;
		try {
			input = new BufferedReader(new FileReader(file));
			String line;
			int lineCount = 1;

			while ((line = input.readLine()) != null) {
				String[] lineParts = line.split("\t");
				String source = null;
				String target = null;
				int count = 1;

				if (lineParts.length < 2) {
					input.close();
					throw new ParseException(line, lineCount);
				} else {
					source = lineParts[0];
					target = lineParts[1];
				}

				if (lineParts.length >= 3) {
					count = Integer.parseInt(lineParts[2]);
				}

				misspellings.add(new Misspelling(source, target, count * trainingMultiplier));
				lineCount++;
			}

			input.close();
		} catch (FileNotFoundException e) {
			System.err.println("The file " + file + " could not be opened.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("The file " + file + " could not be read.");
			e.printStackTrace();
		} catch (ParseException e) {
			System.err.println("The file " + file + " could not be parsed at line " + e.getErrorOffset() + ".  The format is: \n" +
					"misspelling TAB target TAB count");
			e.printStackTrace();
		}

		return misspellings;
	}


}
