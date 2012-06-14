import java.util.TreeSet;
import java.util.Comparator;


public class Huffman {

	class HuffmanNode{
		char character;
		int count;
		HuffmanNode left;
		HuffmanNode right;
		public HuffmanNode(HuffmanNode left, HuffmanNode right){
			this.count = left.count + right.count;
			this.left = left;
			this.right = right;
		}
		public HuffmanNode(char character, int count){
			this.character = character;
			this.count = count;
		}
	}

	int[] charCounts = new int[255];
	HuffmanNode huffmanTree;
	String[] huffmanCodes = new String[255];
	String infile = "";
	String outfile = "";
	//created by countChars()
	TextFile textFile = null;
	BinaryFile binaryFile = null;
	int textFileSize;

	/*
	 * Specify proper command line arguments to user and exit 
	 */

	private static void printUsage(){
		System.out.println("Usage: java Huffman (-c|-u) [-v] [-f] infile outfile");
		System.exit(1);
	}

	
	public int treeSize(){
		return treeSize(huffmanTree);
	}

	/*
	 * Computes the size of the tree
	 * 1 bit for each internal node
	 * 9 bits for each leaf
	 */

	private int treeSize(HuffmanNode huffmanTree){

		if(huffmanTree.left == null && huffmanTree.right == null){
			return 9;
		} else{
			return 1 + treeSize(huffmanTree.left) + treeSize(huffmanTree.right);			
		}
	}

	public int compressedFileSize(){
		int accumulatedSize = 0;
		for (int i = 0; i < 255; i++){
			accumulatedSize += huffmanCodes[i].length() * charCounts[i];
		}

		//accumulated + 48 bits + size of tree, then turn to bytes
		accumulatedSize += (48 + treeSize(huffmanTree));
		//accumulated size /8 + 1 if accumulatedSize mod 8 does not equal 0
		int fileSizeInBytes = accumulatedSize / 8 + (accumulatedSize % 8 != 0 ? 1 : 0);
		return  fileSizeInBytes;
	}
	/*
	 * count characters in the textfile
	 */

	private void countChars(String filePath){
		textFile = new TextFile(filePath, 'r');
		textFileSize = 0;
		
		while (!textFile.EndOfFile()){
			charCounts[textFile.readChar()]++;
			textFileSize++;
		}
	}

	private void buildTree(){

		TreeSet<HuffmanNode> sortedTrees = new TreeSet<HuffmanNode>(new Comparator<HuffmanNode>(){
			public int compare(HuffmanNode arg0, HuffmanNode arg1) {
				int count1 = arg0.count - arg1.count;
				if(count1 != 0){
					return count1;
				}
				return arg0.hashCode() - arg1.hashCode();
			}
		});

		for (char i = 0; i < charCounts.length; i++){
			if(charCounts[i]>0){
				sortedTrees.add(new HuffmanNode(i, charCounts[i]));
			}
		}

		while (sortedTrees.size() > 1){
			HuffmanNode first = sortedTrees.first();
			HuffmanNode next = sortedTrees.higher(first);
			sortedTrees.add(new HuffmanNode(first, next));
			sortedTrees.remove(first);
			sortedTrees.remove(next);
		}
		huffmanTree = sortedTrees.first();
	}
	
	//helper method to build huffman codes
	public void buildHuffmanCodes(){
		buildHuffmanCodes("", huffmanTree);
	}

	private void buildHuffmanCodes(String codeSoFar, HuffmanNode curNode){
		if(curNode.left == null){
			huffmanCodes[curNode.character] = codeSoFar;
			return;
		}
		buildHuffmanCodes((codeSoFar + '0'), curNode.left);
		buildHuffmanCodes((codeSoFar + '1'), curNode.right);
	}

	public void writeToFile(String outfile){

		binaryFile = new BinaryFile(outfile, 'w');
		binaryFile.writeChar('H');
		binaryFile.writeChar('F');
		writeTree(huffmanTree);
		textFile.rewind();
		while(!textFile.EndOfFile()){
			char currentChar = textFile.readChar();
			String codeString = huffmanCodes[(int)currentChar];
			for(int i = 0; i < codeString.length() -1; i++){
				//write 1 if codeString.charAt(i) is true
				binaryFile.writeBit(codeString.charAt(i) == '1');
			}
		}
		binaryFile.close();
		textFile.close();
	}

	public void writeTree(HuffmanNode huffmanTree){

		if(huffmanTree.left != null){
			binaryFile.writeBit(false);
			writeTree(huffmanTree.left);
			writeTree(huffmanTree.right);
		} else{
			binaryFile.writeBit(true);
			binaryFile.writeChar(huffmanTree.character);
		}
		return;

	}
	/*
	 * logic for uncompression:
	 * open binary file in read
	 * open textfile in write
	 * check to see if signature matches
	 * read tree recursively
	 */

	public void fileUncompression(String infile, String outfile){
		binaryFile = new BinaryFile(outfile, 'r');
		textFile = new TextFile(outfile, 'w');
		//check signature
		String sig = "" + binaryFile.readChar() + binaryFile.readChar();
		if(!sig.equals("HF")){
			return;
		}
		huffmanTree = readTree();
		//write characters to textfile
		while(!binaryFile.EndOfFile()){
			char curChar = readCharFromFile(huffmanTree);
			textFile.writeChar(curChar);
		}

	}

	public HuffmanNode readTree(){
		if(binaryFile.readBit()){
			return new HuffmanNode(binaryFile.readChar(), 0);
		}
		return new HuffmanNode(readTree(), readTree());
	}

	public char readCharFromFile(HuffmanNode hf){
		if(hf.left == null){
			return hf.character;
		} 
		if(binaryFile.readBit()){
			return readCharFromFile(hf.right);
		}
		return readCharFromFile(hf.left);
	}
	
	public void printTree(){
		System.out.println("=============HUFFMAN TREE===============");
		printTree(huffmanTree, 0);
	}

	private void printTree(HuffmanNode hf, int offset){
		for(int i = 0; i < offset; i++){
			System.out.print("  ");
		}
		if(hf.left == null){
			System.out.println(hf.character);
			return;
		}
		//print indication of interior node
		System.out.println("node");
		printTree(hf.left, offset + 1);
		printTree(hf.right, offset);
	}
	
	public void printFrequencies(){
		System.out.println("==========CHARACTER FREQUENCIES=========");
		for(int i = 0; i < 255; i++){
			if(charCounts[i] == 0){
				continue;
			}
			System.out.println(i + " " + charCounts[i]);
		}
	}
	
	public void printHuffmanCodes(){
		System.out.println("=============HUFFMAN CODES=============");
		for(int i = 0; i < 255; i++){
			if(huffmanCodes[i] == null){
				continue;
			}
			System.out.println((char) i + " " + huffmanCodes[i]);
		}
	}
	
	public boolean isCompressible(){
		return textFileSize > treeSize();
	}

	public static void main(String[] args){
		String infile = "";
		String outfile = "";
		boolean compressionFlag = false;
		boolean verbose = false;
		boolean forced = false;
		if(args.length < 3 || args.length > 5){
			printUsage();
		}
		if (args[0].equals("-c")){
			compressionFlag = true;
		}
		else if (!args[0].equals("-u")){
			printUsage();
		}

		if (args.length > 3 ){
			if (args[1].equals("-v")){
				verbose = true;
			}
			else if (args[1].equals("-f")){
				forced = true;
			}
			else {
				printUsage();
			}
		}
		if (args.length > 4){
			if (args[2].equals("-v")){
				if (verbose)
					printUsage();
				verbose = true;
			}
			else if (args[2].equals("-f")){
				if (forced)
					printUsage();
				forced = true;
			}
			else {
				printUsage();
			}
		}
		infile = args[args.length-2];
		outfile = args[args.length-1];

		if (infile.startsWith("-")){
			printUsage();
		}
		if (outfile.startsWith("-d")){
			printUsage();
		}
		Huffman huffman = new Huffman();
		if (compressionFlag){
			huffman.countChars(infile);
			huffman.buildTree();
			huffman.buildHuffmanCodes();
			if(huffman.isCompressible() || forced){
				huffman.writeToFile(outfile);
			}
			if(verbose){
				huffman.printFrequencies();
				huffman.printTree();
				huffman.printHuffmanCodes();
			}
		} else{
			huffman.fileUncompression(infile, outfile);
			if(verbose){
				huffman.printTree();
			}	
		}
	}
}
