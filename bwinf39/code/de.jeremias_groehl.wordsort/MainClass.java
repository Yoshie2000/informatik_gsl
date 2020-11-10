import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainClass {
	
	//------------------------------------------------vars------------------------------------------------------------//
	
	private File mainFile;
	
	private ArrayList<String> wordsAndGaps = new ArrayList<>();
	
	private JeresHashMap gaps = new JeresHashMap();
	
	private List<List<String>> wordsFromLetters = new ArrayList<>();
	
	private JeresHashMap sortedWords = new JeresHashMap();
	
	private Pattern criteriaForWord = Pattern.compile("[\\wüöäÜÄÖ]+");
	
	// a backup file if the FileChooser is closed and the mainFile is null
	private String pathToBackupFile = "res/backups/backup.txt";
	
	//list of solved gaps that need to be deleted
	private List<Integer> gapsToDelete = new ArrayList<>();
	
	private StringBuilder sourceText = new StringBuilder();
	private StringBuilder outputText = new StringBuilder();
	
	
	//----------------------------------------------main--------------------------------------------------------------//
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		JFrame frame = new JFrame("WordSort");
		frame.setSize(600, 200);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
		frame.add(panel);
		
		panel.setLayout(new MigLayout("", "[]20[]", "[][]"));
		
		JTextArea sourceTextArea = new JTextArea(1, 30);
		sourceTextArea.setLineWrap(true);
		sourceTextArea.setWrapStyleWord(true);
		
		JButton browseButton = new JButton("Select source file!");
		
		JTextArea outputTextArea = new JTextArea(1, 30);
		outputTextArea.setLineWrap(true);
		outputTextArea.setWrapStyleWord(true);
		JLabel outputInfoLabel = new JLabel("Output text:");
		
		panel.add(browseButton, "left, sg 1");
		panel.add(sourceTextArea, "right, sg 2, pushx, growx, pushy, growy, wrap");
		
		panel.add(outputInfoLabel, "left, sg 1");
		panel.add(outputTextArea, "right, sg 2, pushx, growx, pushy, growy, split");
		
		frame.setVisible(true);
		
		List<Image> icons = new ArrayList<>();
		icons.add(Toolkit.getDefaultToolkit().getImage("res/desktop_icons/WordSort_16x16.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage("res/desktop_icons/WordSort_32x32.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage("res/desktop_icons/WordSort_128x128.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage("res/desktop_icons/WordSort_512x512.png"));
		frame.setIconImages(icons);
		
		browseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainClass mainClass = new MainClass();
				
				mainClass.selectFile();
				
				mainClass.extractWords();
				
				sourceTextArea.setText(mainClass.sourceText.toString());
				
				mainClass.extractGapsFromWords();
				
				mainClass.sortFromLetterCount();
				
				mainClass.sortWords();
				
				mainClass.fillInWords();
				
				outputTextArea.setText(mainClass.outputText.toString());
				
				frame.setLocationRelativeTo(null);
				frame.pack();
			}
		});
	}
	
	//-----------------------------------------------logic------------------------------------------------------------//
	
	private void fillInWords() {
		try {
			
			BufferedReader reader;
			
			reader = initReader();
			
			StringBuilder text = new StringBuilder();
			
			String nextLine = reader.readLine();
			String nextLineBuffer = "";
			
			while (nextLine.contains("_")) {
				
				Matcher word = criteriaForWord.matcher(nextLine);
				
				int matcherEndBuffer = 0;
				int wordIndex = 0;
				while (word.find()) {
					String wordToPrint = "";
					String readWord = nextLine.substring(word.start(), word.end());
					//checks if word is a gap
					if (readWord.contains("_")) {
						wordToPrint = sortedWords.get(wordIndex);
						wordIndex++;
					} else {
						break;
					}
					
					String wordAddendum = nextLine.substring(matcherEndBuffer, word.start());
					matcherEndBuffer = word.end();
					
					text.append(wordAddendum).append(wordToPrint);
				}
				
				if (nextLine.contains("_")) {
					nextLineBuffer = nextLine;
				}
				
				nextLine = reader.readLine();
				
				if (! nextLine.contains("_")) {
					
					int endPosition = nextLineBuffer.length();
					String lastPunctuation = nextLineBuffer.substring(matcherEndBuffer, endPosition);
					
					text.append(lastPunctuation);
				}
			}
			
			
			outputText = text;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private BufferedReader initReader() throws FileNotFoundException {
		BufferedReader reader;
		
		//checks if the mainFile the reader is trying to read == null, if it is then use a backup file
		if (mainFile != null) {
			reader = new BufferedReader(new FileReader(mainFile));
		} else {
			//shows error dialog
			String message = "No file selected!";
			JOptionPane.showMessageDialog(null, message, "Error!", JOptionPane.ERROR_MESSAGE);
			
			//uses backup file
			reader = new BufferedReader(new FileReader(pathToBackupFile));
			mainFile = new File(pathToBackupFile);
		}
		return reader;
	}
	
	//-----------------------------------------------------done-------------------------------------------------------//
	
	private void sortWords() {
		gapsToDelete = new ArrayList<>();
		
		//iterates through all gaps and tries to solve them
		for (Integer gapKey : gaps.keySet()) {
			
			//gathers information about gap: String value, length and what letters
			// are contained in it at which position
			String gap = gaps.get(gapKey);
			int length = gap.length();
			HashMap<Integer, String> gapLetters = detectLetters(gap);
			
			//creates list of possible words that fit the length and the position of the given letters
			List<String> possibleWords = wordsFromLetters.get(length);
			
			//if there are no letters in the gap:
			if (gapLetters.keySet().size() == 0) {
				
				// and the options for a possible word are just one,
				// add the first option to the list of sorted words
				if (possibleWords.size() == 1) {
					String word = possibleWords.get(0);
					wordFound(word, gapKey);
					
					//and there is more than one possible word, this is unsolvable and remains to be sorted
				} else {
					sortedWords.put(gapKey, gap);
				}
				
			}
			//if there are letters in the gap:
			else {
				
				//creates list for words that match the letters and the letterCount of the gap
				List<String> fittingWords = new ArrayList<>();
				
				//iterates through every word that has the required number of letters
				possibleWords.forEach(word -> {
					//detects all letters in the current possible word
					HashMap<Integer, String> wordLetters = detectLetters(word);
					
					//sets up boolean for the case that all of the letters match
					boolean allMatch = true;
					//iterates through every letter in the gap
					for (Integer gapLettersKey : gapLetters.keySet()) {
						//buffers the letter of the current possible word and the gap
						String wordLetter = wordLetters.get(gapLettersKey);
						String gapLetter = gapLetters.get(gapLettersKey);
						
						//checks if the current letter of the word is identical with the equivalent letter
						//of the gap, if not then it sets allMatch to false and breaks
						if (! wordLetter.equals(gapLetter)) {
							allMatch = false;
							break;
						}
					}
					
					//checks if all the information on letters matches, and if yes it adds it to the
					// list of fitting words
					if (allMatch) {
						fittingWords.add(word);
					}
				});
				
				//if there is only one fitting word, this handles the adding to sortedWords, flags the
				// gap for deletion and removes word from wordsFromLetters
				if (fittingWords.size() == 1) {
					String word = fittingWords.get(0);
					wordFound(word, gapKey);
					
				} else {
					//if there is more than one fitting word, it compares the fitting words, to
					// see if they are identical
					boolean areIdentical = false;
					//compares every word with every word
					for (int i = 0; i < fittingWords.size(); i++) {
						for (int j = i + 1; j < fittingWords.size(); j++) {
							String word1 = fittingWords.get(i);
							String word2 = fittingWords.get(j);
							
							//if one word is different than the other, it sets areIdentical to false
							//and breaks
							if (! word1.equals(word2)) {
								areIdentical = false;
								break;
							} else {
								areIdentical = true;
							}
							
						}
						if (! areIdentical)
							break;
					}
					
					//if all the words are identical, it doesn't matter which word is chosen, so the first one will do
					if (areIdentical) {
						String word = fittingWords.get(0);
						wordFound(word, gapKey);
					} else {
						sortedWords.put(gapKey, gap);
					}
				}
				
				
			}
		}
		
		//deletes all the solved gaps that were flagged for deletion
		for (int deleteKey : gapsToDelete) {
			gaps.remove(deleteKey);
		}
		
		//as long as there are gaps left to solve, this method will recursively start,
		//until all gaps are solved
		int sizeOfGaps = gaps.keySet().size();
		while (sizeOfGaps > 0) {
			
			sortWords();
			
			sizeOfGaps = gaps.keySet().size();
		}
	}
	
	private void wordFound(String word, Integer gapKey) {
		//buffers the word length
		int length = word.length();
		
		//adds word to sortedWords, removes word from wordsFromLetters, flags gap for deletion
		sortedWords.put(gapKey, word);
		wordsFromLetters.get(length).remove(word);
		gapsToDelete.add(gapKey);
	}
	
	private HashMap<Integer, String> detectLetters(String str) {
		//creates local buffer HashMap for return value
		HashMap<Integer, String> detectedLetters = new HashMap<>();
		
		//splits input String into characters
		String[] splitStr = str.split("");
		
		//iterates through characters, if it detects any other character than an underscore,
		// it saves that to the HashMap
		for (int i = 0; i < splitStr.length; i++) {
			if (! splitStr[i].equals("_")) {
				detectedLetters.put(i, splitStr[i]);
			}
		}
		
		//returns local buffer of HashMap
		//Example: str = __i_e__
		//detectedLetters = [3=i, 5=e]
		return detectedLetters;
	}
	
	public void sortFromLetterCount() {
		//iterates through every detected word (there are no gaps present anymore!)
		for (String word : wordsAndGaps) {
			
			//saves the length of the word in local variable
			int length = word.length();
			
			//if the ArrayList isn't big enough, it's made to the required size
			while (wordsFromLetters.size() <= length) {
				wordsFromLetters.add(wordsFromLetters.size(), new ArrayList<>());
			}
			
			//adds the word to the list at the appropriate place
			wordsFromLetters.get(length).add(word);
		}
	}
	
	private void extractGapsFromWords() {
		//iterates through all words and gaps collected
		for (int i = 0; i < wordsAndGaps.size(); i++) {
			String word = wordsAndGaps.get(i);
			
			//adds every detected gap to list of gaps, then deletes gap from wordsAndGaps
			if (word.contains("_")) {
				gaps.add(word);
				wordsAndGaps.remove(i);
				i--;
			} else {
				//if there is no gap detected anymore, the method returns
				return;
			}
		}
	}
	
	private void extractWords() {
		try {
			//creates new BufferedReader
			BufferedReader reader;
			reader = initReader();
			
			//iterates through the file line by line
			String nextLine = "";
			while ((nextLine = reader.readLine()) != null) {
				
				//creates a matcher for the pattern which defines what a word is
				Matcher word = criteriaForWord.matcher(nextLine);
				
				//if the line contains an underscore, add that line to the source text
				if (nextLine.contains("_")) {
					sourceText.append(nextLine);
				}
				
				//while there is another word, it adds it to the list of words and gaps
				while (word.find()) {
					String word_str = nextLine.substring(word.start(), word.end());
					wordsAndGaps.add(word_str);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void selectFile() {
		//instantiates local buffer for new main file
		File file;
		
		//set LookAndFeel of UIManager to the system-wide default
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		//instantiates a JFileChooser with a filter for text files and opens it
		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"text files", "txt");
		fileChooser.setFileFilter(filter);
		fileChooser.showOpenDialog(null);
		
		//sets the value of the local buffer file to the selected file and updates mainFile
		file = fileChooser.getSelectedFile();
		mainFile = file;
	}
}
