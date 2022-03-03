package i7.service;

import i7.GeneralConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

public class TransformSentenceTest extends GeneralConfig {
  @Autowired
  TestsService testsService;
  
  String text =
    "When I was a child, I spoke like a child, I thought like a child, I reasoned like a child.\n" +
      "When I became a man - I gave up childish ways..\n" +
      "Washington D.C. is a very special city.\n" +
      "Not every capitol is a D.C.!\n" +
      "Love never ends. As for prophecies, they will pass away; as for tongues, they will cease; as for knowledge, it will pass away.\n" +
      "For now we see in a mirror dimly, but then face to face.\n";
  
  
  
  @Test
  void transformSentenceTest() {
    // Sentences
    String[] sentences = text.split( heapTestSentencesSplitRegEx );
    
    // The Implementation
    for( String sentence : sentences ) {
      List<String> words = new ArrayList<>();
      
      // info( sentence );
      String question = testsService.transformSentence( words, sentence, 7, 5, 20 );
      
      // INFO
      info( question );
      info( String.join( " ", words ) );
      info();
    }
  }
  
  
  
  @Test
  void uniqueListOfAllWordsTest() {
    // The Implementation
    List<String> words = testsService.uniqueListOfAllWords( text );
    
    // By Default build as it is, shuffling is performed each time an InquiryDto is assembled
    info( String.join( " ", words ) );
    
    // Sorted
    Collections.sort( words, String.CASE_INSENSITIVE_ORDER );
    info( String.join( " ", words ) );
  }
  
  
  
}