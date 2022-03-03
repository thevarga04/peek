import java.util.*;

/**
 * TODO: Use arrays and rearange characters instead of this overcomplicated approach, please.
 */
public class TextJustification extends Config {
  class Node {
    String word;
    Integer spaces;
  
    public Node( String word, Integer spaces ) {
      this.word = word;
      this.spaces = spaces;
    }
  }
  
  
  /**
   * Format given text - justify to start and end of lines, extra spaces must be evenly distributed.
   * Last line must be justified left with no extra spaces.
   * It is guaranteed that words do not exceed given width.
   */
  List<String> fullJustify( String[] words, int maxWidth ) {
    // Object to return
    LinkedList<String> justified = new LinkedList<>();
    
    
    // Helper variable to build a line of justified words, 1st word in the line does not need any spaces right before it
    LinkedList<Node> line = new LinkedList<>( Arrays.asList( new Node( words[0], 0 ) ) );
    int count = words[0].length();
    
    
    // start looping from 2nd word (because 1st word does not need a space right before it)
    for ( int i = 1; i < words.length; i++ ) {
      if ( words[i].length() < maxWidth - count ) {       // ordinary word in the line, 1 space needed right before it
        line.add( new Node( words[i], 1 ) );
        count += words[i].length() + 1;                   // with the 1 space before each word (except 1st one)
        
      } else {                                            // maxWidth is over, need to justify previous words
        if ( line.size() == 1 )
          justified.add( line.get( 0 ).word + String.join( "", Collections.nCopies( maxWidth - count, " " ) ) );
        else
          justified.add( justify( line, maxWidth, count, true ) );
        
        // 1st word for a new line
        line = new LinkedList<>( Arrays.asList( new Node( words[i], 0 ) ) );
        count = words[i].length();
      }
    }
    // The last line is justified left
    justified.add( justify( line, maxWidth, count, false ) );
    
    
    // Return list of justified lines
    return justified;
  }
  
  
  /**
   * Add spaces to every word (except 1st one)
   * @param count actual length of characters including minimal spaces
   */
  String justify( List<Node> line, int width, int count, boolean center ) {
    if ( center ) {                                       // evenly add spaces to 2nd .. Nth word
      while ( count <= width - line.size() + 1 ) {        // +1, because 1st word has never space prefix
        for ( int i = 1; i < line.size(); i++ )
          line.get( i ).spaces++;
        count += line.size() - 1;                         // -1, because 1st word is always skipped
      }
  
      // Add remaining extra spaces to 2nd .. Nth - 1 word
      if ( count < width )
        for ( int i = 1; i <= ( width - count ); i++ )
          line.get( i ).spaces++;
    }
    
    
    // Convert list of nodes to a line of justified words
    StringBuilder sb = new StringBuilder( width );
    sb.append( line.get( 0 ).word );                      // 1st word
    
    for ( int i = 1; i < line.size(); i++ ) {             // rest of words with spaces as a prefix
      Node node = line.get( i );
      sb.append( String.join( "", Collections.nCopies( node.spaces, " " ) ) ).append( node.word );
    }
    
    
    // Last line must have trailing spaces
    while ( sb.length() < width )
      sb.append( " " );
    
    
    // Return justified words as a string
    return sb.toString();
  }
  
  
  
  List<String> fullJustify2DArray( String[] words, int maxWidth ) {
    // Use helper 2D array then, convert to the object to return
    char[][] array = new char[words.length][maxWidth];
    
    addWord( array[0], words[0], 0 );
    int filled = words[0].length();
    int line = 0;
    List<Integer> indexes = new LinkedList<>();           // tracks beginning of words in the current line except 1st one
    
    
    // DEBUG
    debug( Arrays.toString( array[0] ) + ", filled: " + filled );
    
    
    // Add words to the current line until fulfilled, start from 2nd word
    for ( int i = 1; i < words.length; i++ ) {
      if ( words[i].length() < maxWidth - filled ) {      // ordinary word which fits to the current line
        addWord( array[line], words[i], ++filled );       // ++filled, because every next word needs at least one space
        indexes.add( filled );                            // track beginning of word in the current line
        filled += words[i].length();                      // track how many characters are occupied
        
      } else {                                            // maxWidth is over, need to justify current line
        if ( indexes.size() > 0 )
          justifyLine( array[line], indexes, maxWidth, filled, true );
        
        line++;                                           // next line
        addWord( array[line], words[i], 0 );        // next line starts with this word
        indexes = new LinkedList<>();                     // don't need to track index of 1st word (is always zero)
        filled = words[i].length();
      }
    }
    
    
    // Convert 2D char array to list of strings
    return arrayToList( array );
  }
  
  void addWord( char[] line, String word, int from ) {
    for ( int i = 0; i < word.length(); i++, from++ )
      line[from] = word.charAt( i );
  }
  
  void justifyLine( char[] line, List<Integer> indexes, int width, int filled, boolean center ) {
    if ( center ) {
      // TODO: justify words in the current line ... move them right until max width is reached ...
    }
  }
  
  /**
   * Convert 2D char array to list of strings
   */
  List<String> arrayToList( char[][] array ) {
    LinkedList<String> list = new LinkedList<>();
    for( char[] line : array )
      list.add( String.valueOf( line ) );
    
    return list;
  }
}