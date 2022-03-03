package i7.service;

import i7.GeneralConfig;
import i7.entity.*;
import i7.model.*;
import i7.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Transactional( propagation = Propagation.REQUIRED, readOnly = false, rollbackFor = Exception.class )
public class TestsService extends GeneralConfig implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LoggerFactory.getLogger( TestsService.class );
  
  @Autowired
  TestsRepo testsRepo;
  @Autowired
  McqTestsRepo mcqTestsRepo;
  @Autowired
  QaTestsRepo qaTestsRepo;
  @Autowired
  HeapTestRepo heapTestRepo;
  @Autowired
  SchedulesRepo schedulesRepo;
  @Autowired
  ModelService modelService;          // JPA Interface Projection service
  
  
  
  // ModelValidator
  public Optional<Tests> findByTeacherAndTestname( String teacher, String testname ) {
    LOGGER.info( "# findByTeacherAndTestname: {} {}", teacher, testname );
    return testsRepo.findByTeacherAndTestname( teacher, testname );
  }
  
  
  
  /**
   * View/Edit MCQ Test
   */
  public McqTestDto getMcqTestToViewOrEdit( int id, String teacher ) {
    LOGGER.info( "# loadMcqTest {} {}", teacher, id );
    
    // The entity, hence it contains its MCQs, need other MCQs as well (to be able to add them into the test)
    McqTests test = mcqTestsRepo.getByIdAndTeacher( id, teacher ).orElse( null );
    if ( test == null )
      return null;
    
    // Get simple list of all MCQs with number of options and 1|0 if included
    List<McqIncluded> mcqQuestions = modelService.getAllMcqAndIfIncluded( id, teacher );
  
    // Need schedules to determine if test is scheduled, if yes, do not allow to delete or modify it
    List<Schedules> scheduleNames = schedulesRepo.mcqTestScheduledIn( id );
    
    // Entity -> Dto + MCQ Questions to return McqTestDto
    return mcqTests2Dto( test, mcqQuestions, scheduleNames );
  }
  
  /**
   * View/Edit QA Test
   */
  public QaTestDto getQaTestToViewOrEdit( int id, String teacher ) {
    LOGGER.info( "# loadQaTest {} {}", teacher, id );
    
    QaTests test = qaTestsRepo.getByIdAndTeacher( id, teacher ).orElse( null );
    if ( test == null )
      return null;
  
    // Need schedules to determine if test is scheduled, if yes, do not allow to delete or modify it
    List<Schedules> schedules = schedulesRepo.qaTestScheduledIn( id );
  
    // Entity -> Dto (with Qas -> QA)
    return qaTests2Dto( test, schedules, true );
  }
  
  /**
   * View/Edit Heap Test
   */
  public HeapTestDto getHeapTestToEdit( int id, String teacher ) {
    LOGGER.info( "getHeapTestToEdit {} {}", id, teacher );
    
    HeapTests test = heapTestRepo.getByIdAndTeacher( id, teacher ).orElse( null );
    if ( test == null )
      return null;
    
    // Need schedules to determine if test is scheduled, if yes, do not allow to delete or modify it
    List<Schedules> schedules = schedulesRepo.heapTestScheduledIn( id );
    
    // Entity -> Dto
    return new HeapTestDto( test.getId(), test.getTestname(), test.getPicturename(), test.getText(), test.getHtml(), test.getOps(), test.getAccess(), test.getWordsToHide(), test.getLengthFrom(), test.getLengthTo(), test.isSourceSentence(), test.getSentences(), test.getTotal(), schedules );
  }
  
  /**
   * Return HeapTestDto to create a brand-new copy of it
   */
  public HeapTestDto getHeapTestToCopy( int id, String teacher ) {
    LOGGER.info( "getHeapTestToCopy {} {}", id, teacher );
    
    HeapTests test = heapTestRepo.getByIdAndTeacher( id, teacher ).orElse( null );
    if ( test == null )
      return null;
    
    // Entity -> Dto
    return new HeapTestDto( 0, test.getTestname(), test.getPicturename(), test.getText(), test.getHtml(), test.getOps(), test.getAccess(), test.getWordsToHide(), test.getLengthFrom(), test.getLengthTo(), test.isSourceSentence(), test.getSentences(), test.getTotal(), new ArrayList<>() );
  }
  
  
  
  /**
   * Return MCQ Test with ID = 0 to create a new copy of it and blank schedules
   */
  public McqTestDto getMcqTestToCopy( int id, String teacher ) {
    LOGGER.info( "# loadMcqTest {} {}", teacher, id );
  
    // The entity, hence it contains its MCQs, need other MCQs as well (to be able to add them into the test)
    McqTests t = mcqTestsRepo.getByIdAndTeacher( id, teacher ).orElse( null );
    if ( t == null )
      return null;
  
    // Get MCQ Questions with info if included
    List<McqIncluded> mcqQuestions = modelService.getAllMcqAndIfIncluded( id, teacher );
    
    // Entity -> Dto + MCQ Questions to return McqTestDto, blank schedules
    return mcqTests2Dto( t, mcqQuestions, null );
  }
  
  
  
  /**
   * Return QA Test with ID = 0 to create a new copy of it and blank schedules
   */
  public QaTestDto getQaTestToCopy( int id, String teacher ) {
    LOGGER.info( "# loadQaTest {} {}", teacher, id );
  
    QaTests test = qaTestsRepo.getByIdAndTeacher( id, teacher ).orElse( null );
    if ( test == null ) return null;
    
    // Entity -> Dto (with Qas -> QA), blank schedules
    return qaTests2Dto( test, new ArrayList<>(), true );
  }
  
  
  
  /**
   * Save QA Test
   */
  public QaTests saveQaTest( QaTestDto dto, String teacher ) {
    LOGGER.info( "# saveQaTest {}", dto );
    
    // QAs are passed as is: keep valid and unique QAs only = ignore incomplete and duplicates
    LinkedHashSet<QA> setQA = dto.getQaList().stream().filter( QA::isValid ).collect( Collectors.toCollection( LinkedHashSet::new ) );
    
    // QA -> Qas, maintain the order of QA(s)
    Set<Qas> qasSet = setQA.stream().map( qa -> new Qas( qa.getId(), qa.getQuestion(), qa.getAnswer() ) ).collect( Collectors.toCollection( LinkedHashSet::new ) );
    LOGGER.info( "{}", qasSet.stream().map( Qas::toString ).collect( Collectors.joining(", ") ) );
    
    // QaTestDto -> QaTests  (id should be 0 for new Test); QDH: access: Private
    QaTests qaTests = new QaTests( dto.getId(), teacher, dto.getTestname(), dto.getPicturename(), dto.getText(), dto.getHtml(), dto.getOps(), "Private", qasSet );
    LOGGER.info( "{}", qaTests );
    
    // Persists QA Test with QAs
    return qaTestsRepo.save( qaTests );
  }
  
  
  /**
   * Save Heap Test
   */
  public HeapTests saveHeapTest( HeapTestDto dto, String teacher ) {
    LOGGER.info( "# saveHeapTest {}", dto );
    
    // Dto -> Entity
    HeapTests heapTests = new HeapTests( dto.getId(), teacher, dto.getTestname(), dto.getPicturename(), dto.getText(), dto.getHtml(), dto.getOps(), "Private", dto.getWordsToHide(), dto.getLengthFrom(), dto.getLengthTo(), dto.isSourceSentence(), dto.getSentences(), dto.getTotal() );
    LOGGER.info( "{}", heapTests );
    
    return heapTestRepo.save( heapTests );
  }
  
  
  
  
  
  public void save( TestDtoOld dto ) {
    LOGGER.info( "# save {}", dto );

    // Keep valid and unique QAs only (ignore incomplete and duplicate data in a form)
    LinkedHashSet<QA> setQA = dto.getQaList().stream().filter( QA::isValid ).collect( Collectors.toCollection( LinkedHashSet::new ) );

    // QA -> QAs, maintain the order of QA(s)
    Set<QAsOld> qAsSet = setQA.stream().map( qa -> new QAsOld( qa.getId(), qa.getQuestion(), qa.getAnswer() ) ).collect( Collectors.toCollection( LinkedHashSet::new ) );
    LOGGER.info( qAsSet.stream().map( QAsOld::toString ).collect( Collectors.joining(", ") ) );

    // TestDto -> Tests   (id should be 0 for new Test)
    Tests tests = new Tests( dto.getId(), dto.getTeacher(), dto.getTestname(), dto.getTesttext(), dto.getAccess(), qAsSet );
    LOGGER.info( tests.toString() );

    // Persist Test with QAs
    testsRepo.save( tests );
  }
  
  
  /**
   * Rest's actions: Private | Share | Public | Delete :: Copy is implemented in the controller
   */
  public int updateAccessByTeacherAndId( String access, String teacher, int id ) {
    LOGGER.info( "# updateAccess {} {} {}", id, access, teacher );
    int updated = 0;
    
    try {
      updated = testsRepo.updateAccessByTeacherAndId( access, teacher, id );
    } catch ( Exception e ) {
      LOGGER.error( e.getMessage() );
    }
    return updated;
  }
  
  /**
   * Rest's actions: Private | Share | Public | Delete :: Copy is implemented in the controller
   * + Extra check: Refuse to delete test if is scheduled.
   */
  public int deleteByTeacherAndId( String teacher, int id ) {
    LOGGER.info( "# delete {} {}", id, teacher );
    int deleted = 0;
    
    try {
      deleted = testsRepo.deleteByTeacherAndId( teacher, id );
    } catch ( Exception e ) {
      LOGGER.error( e.getMessage() );
    }
    return deleted;
  }
  
  
  /**
   * Delete either MCQ or QA Test with the check if is not scheduled
   */
  public int deleteByTeacherAndId( String teacher, int id, TestType testType ) {
    LOGGER.info( "# delete {} test {} {}", testType, id, teacher );
    int deleted = 0;
    
    try {
      if ( testType.equals( TestType.HEAP ) )
        deleted = heapTestRepo.deleteByIdAndTeacher( id, teacher ); // Fancier to use first id than teacher (id is shorter)
      
      else if ( testType.equals( TestType.MCQ ) )
        deleted = mcqTestsRepo.deleteByTeacherAndId( teacher, id );
      
      else
        deleted = qaTestsRepo.deleteByTeacherAndId( teacher, id );
      
    } catch ( Exception e ) {
      LOGGER.error( e.getMessage() );
    }
    return deleted;
  }
  
  
  
  /**
   * MCQ Test -> McqTestDto to View/Edit/Copy MCQ Test, needs brief 'McqIncluded' and list of schedules where used
   */
  public McqTestDto mcqTests2Dto ( McqTests t, List<McqIncluded> included, List<Schedules> scheduleNames ) {
    return new McqTestDto(
      t.getId(), t.getTestname(), t.getPicturename(), t.getText(), t.getHtml(), t.getOps(), t.getAccess()
      , included
      , scheduleNames
    );
  }
  
  /**
   * MCQ Entity -> McqTestDto for exam (needs McqDto with options, but does not need brief 'McqIncluded' nor Schedules)
   */
  public McqTestDto mcqTests2Dto ( McqTests mcqTests, boolean ordered ) {
    List<McqDto> mcqList = new ArrayList<>();
    for ( Mcqs m : mcqTests.getMcqs() )
      mcqList.add( new McqDto( m.getId(), m.getText(), m.getHtml(), m.getOps(), m.getMcqOptionsSet(), null, ordered ) );
  
    // TODO: ManyToMany mcq_tests_questions (Set<Mcqs>) does not capture order defined in UI
    if ( ordered )
      mcqList.sort( Comparator.comparingInt( McqDto::getId ) );
    else
      Collections.shuffle( mcqList );
    
    McqTestDto mcqTestDto = mcqTests2Dto( mcqTests, null, null );
    mcqTestDto.setMcqDtoList( mcqList );        // Exam and inquiry needs all the data: Test, MCQ and Options
  
    return mcqTestDto;
  }
  
  
  /**
   * List of schedules is needed to determine if the test can be edited
   */
  public QaTestDto qaTests2Dto( QaTests t, List<Schedules> schedules, boolean ordered ) {
    List<QA> qaList = new ArrayList<>();
    for ( Qas qas : t.getQasSet() )
      qaList.add( new QA( qas.getId(), qas.getQuestion(), qas.getAnswer() ) );
  
    if ( ordered )
      qaList.sort( Comparator.comparingInt( QA::getId ) );          // to View/Edit/Copy QA Test
    else
      Collections.shuffle( qaList );                                // to inquiry (examination) determines order of questions
    LOGGER.info( qaList.stream().map( QA::toString ).collect( Collectors.joining( ", " ) ) );
    
    return new QaTestDto( t.getId(), t.getTestname(), t.getPicturename(), t.getText(), t.getHtml(), t.getOps(), t.getAccess(), qaList, schedules );
  }
  
  
  /**
   * InquiryDto needs list of options if QA Test is scheduled as a type Choose (up to scheduled inquiries)
   * Returning list does not need to be shuffled here, because InquiryDto will do that in its constructor
   */
  public List<Option> qaList2OptionList( TestDto dto, QA qa ) {
    List<Option> list = new ArrayList<>();
    list.add( new Option( qa.getId(), qa.getAnswer(), true ) );     // correct answer must be included, but don't worry client sees blinded options only
    
    if ( dto instanceof QaTestDto && dto.getInquiryType().equals( InquiryType.CHOOSE.getType() ) ) {
      List<QA> qaList = ( (QaTestDto) dto ).getQaList();
      Collections.shuffle( qaList );                                // always need random options to pick from
      
      // Generally answers might be very similar (like many yes/no) or unique,
      // but list of options might contain only unique values up to number of inquiries
      Map<String, Integer> map = new HashMap<>();
      for( QA entry : qaList )
        map.put( entry.getAnswer(), entry.getId() );                // unique answers
  
      // Need to make sure that at least two options are present
      // TODO: Is it necessary to enforce in the UI to schedule at least 2 questions if inquiry type is Choose? ditto MCQ - at least 1 correct and 1 incorrect option
      int inquiries = Math.min( map.size(), Math.max( 2, dto.getInquiries() ) );
      int i = 1;                                                    // correct answer is already included
      for ( Map.Entry<String, Integer> entry : map.entrySet() ) {
        if ( i < inquiries ) {
          if ( ! entry.getKey().equals( qa.getAnswer() ) ) {        // don't add the same answer twice
            list.add( new Option( entry.getValue(), entry.getKey() ) );
            i++;
          }
        } else {
          break;
        }
      }
      return list;
      
    } else
      return list;
  }
  
  
  /**
   * Heap test need to transform sentence to a question and assembly list of words,
   * BTW student can never receive sentence in its original correct form.
   * Show must go on, even if there isn't enough eligible words or if there isn't at least one.
   * If inquiry type is Input, this list of words will not be used, but question still must be transformed.
   * If inquiry type is Choose and source is not sentence, only unique words will be sent to a student.
   * question is transformed form of the sentence. Example: Not every ____ is a D.C.. (D.C. is a special word, should never be hidden)
   *
   */
  public String transformSentence( List<String> words, String sentence, int wordsToHide, int lengthFrom, int lengthTo ) {
    StringBuilder question = new StringBuilder( sentence );
    
    // Add extra white space at the end to avoid checking the end of the sentence.
    question.append( " " );
    
    // Transform all non-special words or
    // transform only up to specified number of words and consider defined preferred lengths first
    if ( wordsToHide > 2000 ) {
      transformWords( words, question, 0, sentence.length(), 0, 2048, 2048, true, true );
      
    } else {
      int transformed = words.size();
      while ( transformed < wordsToHide ) {
        int from = ThreadLocalRandom.current().nextInt( 0, sentence.length() / 2 );
        transformWords( words, question, from, sentence.length(), lengthFrom, lengthTo, 0, false, true );
        
        if ( words.size() == transformed )
          transformWords( words, question, 0, sentence.length(), lengthFrom, lengthTo, 0, true, true );
        
        
        // Try without limits if didn't find suitable word
        if ( words.size() == transformed )
          transformWords( words, question, from, sentence.length(), 0, 2048, 0, false, true );
        
        if ( words.size() == transformed )
          transformWords( words, question, 0, sentence.length(), 0, 2048, 0, true, true );
        
        
        // Need to let it go, if didn't transformed new word after all
        if ( words.size() == transformed )
          break;
        else
          transformed = words.size();
      }
    }
    
    // Remove extra space from the end of the question
    question.deleteCharAt( question.length() - 1 );
    
    return question.toString();
  }
  
  /**
   * Transform either one word (resp. as defined by a limit) or all eligible words
   */
  private void transformWords( List<String> words, StringBuilder question, int from, int to, int min, int max, int limit, boolean started, boolean replace ) {
    int i = from;
    int special = 0;                  // index of special char, skip words having special char inside
    
    while ( from < to ) {
      char c = question.charAt( i );
      if ( Character.isWhitespace( c ) ) {
        if ( started && special < i ) {                             // need to ignore words with special char inside
          int length = ( special == 0 ? i : special ) - from;       // need to not include last special char if present
    
          if ( length > 0 && length >= min && length <= max ) {     // need to consider defined preferred lengths
            words.add( question.substring( from, from + length ) );
            if ( replace )
              question.replace( from, from + length, String.join( "", Collections.nCopies( length, "_" ) ) );
      
            if ( words.size() > limit ) // for specified number of words need to transform one word at the time (due randomization)
              return;
          }
        }
        from = i + 1;                 // next word will start from the next position
        special = 0;                  // reset index of special char
        started = true;               // now we can start transformation process
      } else {
        if ( started && specialCharacters.indexOf( c ) > -1 )       // first need to find the start of a word
          special += i;
      }
      i++;
    }
  }
  
  
  /**
   * If all words are used, list will be unique and will not change every time (stored in HttpSession)
   */
  public List<String> uniqueListOfAllWords( String sentences ) {
    Set<String> allWords = new HashSet<>();
    
    for ( String sentence : sentences.split( heapTestSentencesSplitRegEx ) ) {
      StringBuilder question = new StringBuilder( sentence );
      question.append( " " );                                       // Add extra white space at the end
      List<String> words = new ArrayList<>();
      transformWords( words, question, 0, sentence.length(), 0, 2048, 2048, true, false );
      
      allWords.addAll( words );
    }
    
    // By Default build as it is, shuffling is performed each time an InquiryDto is assembled
    return new ArrayList<>( allWords );
  }
  
  
  
}