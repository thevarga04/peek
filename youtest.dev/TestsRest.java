package i7.api;

import i7.GeneralConfig;
import i7.entity.HeapTests;
import i7.entity.McqTests;
import i7.entity.QaTests;
import i7.entity.Welcomes;
import i7.model.*;
import i7.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static i7.model.Action.*;

@RestController
@RequestMapping( "/api/tests" )
public class TestsRest extends GeneralConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger( TestsRest.class );
  
  TestsService testsService;          // TESTS: Input/Choose(radio) :: MCQ (multiple choice) :: Commotions
  WelcomesService welcomesService;    // welcome messages - deny to create new schedule when there is not any welcome message
  SchedulesService schedulesService;  // schedules - deny to delete or edit a test when is scheduled
  HttpSession httpSession;            // Never Start with a brand new Test until user click Cancel
  ModelService modelService;          // JPA Interface Projection service
  McqsService mcqsService;

  
  public TestsRest( TestsService testsService, WelcomesService welcomesService, SchedulesService schedulesService, HttpSession httpSession, ModelService modelService, McqsService mcqsService ) {
    this.testsService = testsService;
    this.welcomesService = welcomesService;
    this.schedulesService = schedulesService;
    this.httpSession = httpSession;
    this.modelService = modelService;
    this.mcqsService = mcqsService;
  }
  
  
  /**
   * Provide list of both test types as: id, testname, type, access, questions, schedules
   */
  @GetMapping("/getTestsList")
  public List<TestsList> getTestsList( Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
    
    return modelService.getTestsList( principal.getName() );
  }
  
  
  /**
   * QA/MCQ Test actions: Change access/privacy: Private | Share | Public or Delete.
   * Copy and Edit is implemented through the controller and both have separate flows
   */
  @GetMapping( "/{id}" )
  public ResponseEntity<Integer> lifeCycle( @PathVariable("id") int id, @RequestParam("action") Action action, @RequestParam("type") TestType testType, Principal principal ) {
    int outcome = 0;
    
    if ( action == PRIVATE || action == SHARE || action == PUBLIC )
      outcome = testsService.updateAccessByTeacherAndId( action.getAccess(), principal.getName(), id );
    
    else if ( action == DELETE )
      outcome = testsService.deleteByTeacherAndId( principal.getName(), id, testType );
    
    if ( outcome == 0 )
      return new ResponseEntity<>( outcome, HttpStatus.NOT_IMPLEMENTED );
    
    return new ResponseEntity<>( outcome, HttpStatus.OK );
  }
  
  
  
  // *** MCQ and QA Tests: Create New, Edit, Save ***
  /**
   * New MCQ Test is going to be created      (QA Test does not need anything beforehand)
   */
  @GetMapping( value = "/newMcqTest")
  public ResponseEntity<McqTestDto> newMcqTest( Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
    
    // Get Available Questions
    List<McqIncluded> mcqQuestions = modelService.getAllMcqAndIfIncluded( 0, principal.getName() );
    if ( mcqQuestions == null )
      return ResponseEntity.status( HttpStatus.NO_CONTENT ).body( null );
    else if ( mcqQuestions.size() == 0 )
      return ResponseEntity.status( HttpStatus.NO_CONTENT ).body( null );
    
    McqTestDto dto = new McqTestDto();
    dto.setMcqQuestions( mcqQuestions );
    
    return ResponseEntity.ok( dto );
  }
  
  
  /**
   * Return the MCQ Test to View/Edit, need list of available and included question as well as schedule names where used (if any)
   */
  @GetMapping( value = "/getMcqTestToEdit/{id}")
  public ResponseEntity<McqTestDto> getMcqTestToEdit( @PathVariable("id") int id, Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
  
    McqTestDto dto = testsService.getMcqTestToViewOrEdit( id, principal.getName() );
    if ( dto == null )
      return ResponseEntity.status( HttpStatus.NO_CONTENT ).body( null );
    else
      return ResponseEntity.ok( dto );
  }
  
  /**
   * Return the QA Test to View/Edit, need QAs and schedule names where used (if any)
   */
  @GetMapping( value = "/getQaTestToEdit/{id}")
  public ResponseEntity<QaTestDto> getQaTestToEdit( @PathVariable("id") int id, Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
  
    QaTestDto dto = testsService.getQaTestToViewOrEdit( id, principal.getName() );
    System.out.println( dto );
    if ( dto == null )
      return ResponseEntity.status( HttpStatus.NO_CONTENT ).body( null );
    else
      return ResponseEntity.ok( dto );
  }
  
  /**
   * Return the Heap Test Dto to View/Edit
   */
  @GetMapping( value = "/getHeapTestToEdit/{id}")
  public ResponseEntity<HeapTestDto> getHeapTestToEdit( @PathVariable("id") int id, Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
  
    HeapTestDto dto = testsService.getHeapTestToEdit( id, principal.getName() );
    if ( dto == null )
      return ResponseEntity.status( HttpStatus.NO_CONTENT ).body( null );
    else
      return ResponseEntity.ok( dto );
  }
  
  /**
   * Return HeapTestDto to create a brand-new copy of it
   */
  @GetMapping( value = "/getHeapTestToCopy/{id}")
  public ResponseEntity<HeapTestDto> getHeapTestToCopy( @PathVariable("id") int id, Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
    
    HeapTestDto dto = testsService.getHeapTestToCopy( id, principal.getName() );
    if ( dto == null )
      return ResponseEntity.status( HttpStatus.NO_CONTENT ).body( null );
    else
      return ResponseEntity.ok( dto );
  }
  
  
  /**
   * Return MCQ Test to create a brand new copy of it
   */
  @GetMapping( value = "/getMcqTestToCopy/{id}")
  public ResponseEntity<McqTestDto> getMcqTestToCopy( @PathVariable("id") int id, Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
    
    McqTestDto dto = testsService.getMcqTestToCopy( id, principal.getName() );
    if ( dto == null )
      return ResponseEntity.status( HttpStatus.NO_CONTENT ).body( null );
    else {
      dto.setId( 0 );                 // Reset the ID (Qas IDs must be set to 0 in UI)
      return ResponseEntity.ok( dto );
    }
  }
  
  /**
   * Return QA Test to create a brand-new copy of it
   */
  @GetMapping( value = "/getQaTestToCopy/{id}")
  public ResponseEntity<QaTestDto> getQaTestToCopy( @PathVariable("id") int id, Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
  
    QaTestDto dto = testsService.getQaTestToCopy( id, principal.getName() );
    if ( dto == null )
      return ResponseEntity.status( HttpStatus.NO_CONTENT ).body( null );
    else {
      dto.setId( 0 );                 // Reset the ID (Qas IDs must be set to 0 in UI)
      return ResponseEntity.ok( dto );
    }
  }
  
  
  
  /**
   * Save MCQ Test, data integrity of returned data is checked on the client side.
   */
  @PostMapping( value = "/saveMcqTest" )
  public ResponseEntity<McqTestDto> saveMcqTest( McqTestDto dto, Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
    
    McqTests mcqTests = mcqsService.saveMcqTest( dto, principal.getName() );
    return ResponseEntity.ok( new McqTestDto( mcqTests.getId() ) );
  }
  
  /**
   * Save QA Test, data integrity of returned data is checked on the client side.
   */
  @PostMapping(value = "/saveQaTest")
  public ResponseEntity<QaTestDto> saveQaTest( QaTestDto dto, Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
  
    QaTests qaTests = testsService.saveQaTest( dto, principal.getName() );
    return ResponseEntity.ok( new QaTestDto( qaTests.getId() ) );
  }
  
  /**
   * Save Heap Test, data integrity of returned data is checked on the client side.
   */
  @PostMapping(value = "/saveHeapTest")
  public ResponseEntity<HeapTestDto> saveHeapTest( HeapTestDto dto, Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
  
    HeapTests heapTests = testsService.saveHeapTest( dto, principal.getName() );
    return ResponseEntity.ok( new HeapTestDto( heapTests.getId() ) );
  }
  
  
  
  // TODO: Create distinct rest controller for welcome messages
  // <<< Welcome Messages >>>
  /**
   * Give the teacher his messages so he can manage them: list them, add/create, view, edit, delete, copy
   */
  @GetMapping("/messages")
  public List<WelcomesAreScheduled> getMessages( Principal principal ){
    if ( principal == null ) return null;                           // ignore hijacking
    
    return modelService.getWelcomesAreScheduled( principal.getName() );
  }
  
  
  /**
   * Return the Message to View / Edit it with the list of schedules where used
   */
  @GetMapping("/message/{id}")
    public ResponseEntity<List<WelcomeDto>> getMessage( @PathVariable("id") int id, Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
    
    Welcomes welcomes = welcomesService.getByIdAndTeacher( id, principal.getName() );
    if ( welcomes == null )
      return new ResponseEntity<>( null, HttpStatus.NOT_IMPLEMENTED );
  
    // Get the list of Schedules where used
    List<String> names = schedulesService.getScheduleNamesForWelcome( id, principal.getName() );
    
    // The output
    WelcomeDto dto = new WelcomeDto( welcomes.getId(), welcomes.getMessage(), names );
    
    return new ResponseEntity<>( Collections.singletonList( dto ), HttpStatus.OK );
  }
  
  
  /**
   * Save just Created, Edited or Copied Message. Copied and New message has 0 ID and must be different than existing ones.
   */
  @PostMapping( value = "/messageSave" )
  public ResponseEntity<HttpStatus> messageSave( @RequestBody Welcomes welcomes, Principal principal ) {
    if ( principal == null ) return null;                 // ignore hijacking
    
    // re-set the teacher to avoid hijacking
    welcomes.setTeacher( principal.getName() );
    
    // Persist the data
    if ( welcomes.getMessage() != null && welcomes.getMessage().length() > 0 )
      if ( welcomesService.save( welcomes ) != null )
        return new ResponseEntity<>( HttpStatus.OK );
    
    return new ResponseEntity<>( HttpStatus.NOT_MODIFIED );
  }
  
  
  /**
   * Delete Welcome Message from the list of messages
   */
  // TODO: This needs to check if such message is not used in any test
  @GetMapping("/messageDelete/{id}")
  public ResponseEntity<Integer> deleteMessages( @PathVariable("id") int id, Principal principal ) {
    if ( principal == null ) return null;                           // ignore hijacking
    
    int removed = welcomesService.deleteByIdAndTeacher( id, principal.getName() );
    if ( removed != 1 )
      return new ResponseEntity<>( removed, HttpStatus.NOT_MODIFIED );
  
    return new ResponseEntity<>( removed, HttpStatus.OK );
  }
  
  
  
  
  
}