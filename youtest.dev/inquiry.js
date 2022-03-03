/**
 * Each inquiry represents one question / answer problem a student is supposed to face off.
 * On a inquiry page there is no header as usually, but info related to the exam/test only:
 * Exam name (Schedule Name), Test name, Test Type (MCQ|QA), Inquiry Type (Input|Choose), Question/All, Remaining Time
 */

// Remaining Time (mm:ss)
// UTC Timestamp: Math.floor( Date.now() / 1000 );

// Global variables
let id = 0;                           // Exam / Schedule ID
let data_div;                         // Container to generate the data: Legend, test, buttons
let dto;                              // InquiryDto containing the question and related info
let urlGet    = "/api/exams/inquiry"; // Get first InquiryDto
let urlPost   = "/api/exams/inquiry"; // Send the answer and receive next inquiry
let urlResult = "/exams/result";      // GET, controller, show achieved result
let isTimeOver = false;
let endOfExam = false;
let examID = -1;                      // updated only 1st time
let index = 1;                        // words in a sentence might repeat

// After the page is loaded get InquiryDto, display the UI and refresh header and body after each Submit
$(document).ready(function () {
  // Ignore anonymous requests
  if ( document.getElementById('csrf') == null )
    return;
  
  // Container to upload to
  data_div = document.getElementById( 'data_div' );
  
  // Get the first InquiryDto
  let url = window.location.href;
  id = url.substring( url.lastIndexOf( '/' ) + 1 );
  
  getFirstInquiry( urlGet + "/" + id );
});


function getFirstInquiry( url ) {
  console.log( "Getting the first InquiryDto from: " + url + " ... " );
  
  let xhttp = new XMLHttpRequest();
  xhttp.open( 'GET', url );
  xhttp.send();
  
  xhttp.onreadystatechange = function () {
    if ( this.readyState === 4 ) {
      if ( this.status === 200 ) {
        if ( xhttp.responseText == null || xhttp.responseText.length === 0 ) {
          console.log( "I am sorry, something bad happened, exam cannot start or your exam is over." );
          return;
        }
        
        dto = JSON.parse( xhttp.responseText );
        console.log( "Received InquiryDto of " + xhttp.responseText.length + " chars." );
        
        if ( examID < 1 )                                 // teacher (owner) is always only trying (examID = 0)
          examID = dto.id_exam;
        
        // Now generate the UI alongside with the data
        generateUI();
        
      } else {
        console.log( '### Failed to get the response. Response status code: ' + this.status );
      }
    }
  }
}


// Generate the UI with the data
function generateUI() {
  // The Header
  generateHeader();
  
  // The Countdown
  countDown( false );
  
  // The body
  generateBody();
  
  // Time Is Over
  if ( isTimeOver )
    timeIsOver();
  
  // Submit Button
  if ( ! isTimeOver )
    submitButton();
}


// The Header
function generateHeader() {
  let header_div1 = document.createElement( "div" );
  header_div1.id = "header";
  header_div1.setAttribute( "class", "container" );
  
  let header_div2 = document.createElement( "div" );
  header_div2.setAttribute( "class", "form-row col-12" );
  
  let header_div3E = document.createElement( "div" );
  header_div3E.setAttribute( "class", "form-group col" );
  let header_div3Eb = document.createElement( "b" );
  header_div3Eb.append( dto.examName );
  header_div3E.append( "Exam: ", header_div3Eb );
  
  let header_div3T = document.createElement( "div" );
  header_div3T.setAttribute( "class", "form-group col" );
  let header_div3Tb = document.createElement( "b" );
  header_div3Tb.append( dto.testname );
  header_div3T.append( "Test: ", header_div3Tb );
  
  let header_div3Q = document.createElement( "div" );
  header_div3Q.setAttribute( "class", "form-group col text-right" );
  let header_div3Qb = document.createElement( "b" );
  header_div3Qb.append( dto.inquiry );
  let header_div3RTb = document.createElement( "b" );
  header_div3RTb.id = "remainingTime";
  header_div3Q.append( "Question: ", header_div3Qb, " / " + dto.inquiries, " | ", header_div3RTb );
  
  header_div2.append( header_div3E, header_div3T, header_div3Q );
  header_div1.append( header_div2 );
  data_div.append( header_div1 );
}

// Function to update remaining time every second
function countDown( shutdown ) {
  let countDownDate = moment( dto.started * 1000 ).add( dto.timelimit, 'm' ).toDate().getTime();
  
  // Update the count down every second
  let updateCountDown = setInterval(
    function () {
      if ( shutdown ) {
        clearInterval( updateCountDown );
        return;
      }
      
      
      let instantNow = new Date().getTime();
      let remainingSec = Math.floor( ( countDownDate - instantNow ) / 1000 );
      let minutes = Math.floor( remainingSec / 60 );
      let seconds = remainingSec - minutes * 60;
      
      // Display the Count Down, in red for last 10%
      let remainingTime = document.getElementById( "remainingTime" );
      if ( remainingTime == null ) {
        clearInterval( updateCountDown );
        return;
      }
      
      remainingTime.innerHTML = minutes + "m " + seconds + "s";
      if ( remainingSec < dto.timelimit * 6 )
        document.getElementById( "remainingTime" ).className = "has-error";
      
      // After time is over stop the countdown, show the conclusion and disable submit button
      if ( remainingSec < 1 ) {
        isTimeOver = true;
        
        let divOver = document.getElementById("TimeIsOver");
        let divSubmit = document.getElementById("SubmitDiv");
        if ( divOver != null )
          divOver.style.display = "block";
        else
          timeIsOver();
  
        if ( divSubmit != null )
          divSubmit.style.display = "none";
  
        clearInterval( updateCountDown );
      }
    }
  );
}

// The Body
function generateBody() {
  let div1 = document.createElement("div");
  div1.id = "body";
  div1.setAttribute("class", "container" );
  
  let card1 = document.createElement("div");
  card1.setAttribute("class", "card mt-2" );
  
  let card2 = document.createElement("div");
  card2.setAttribute("class", "card m-2" );
  
  let body1 = document.createElement("div");
  body1.setAttribute("class", "card-body" );
  
  // The Picture
  if ( dto.picturename.length > 0 ) {
    let img_div1 = document.createElement( "div" );
    img_div1.setAttribute( "class", "text-center mb-2 mx-0 px-0" );
  
    let imageUrl = "/api/file/" + dto.picturename;
    let image = document.createElement('img');
    image.src = imageUrl;
    image.id = "picture";
    image.onload = function () {
      // console.log( this.width + " x " + this.height );
      if ( this.width > 980 )
        image.width = "980";
    }
    
    img_div1.append( image );
    body1.append( img_div1 );
  }
  
  // Additional text from the test (test's quill)
  if ( dto.testText.length > 0 ) {
    let testHtml = document.createElement( "div" );
    testHtml.setAttribute( "class", "py-0 my-0" );
    testHtml.innerHTML = dto.testHtml;
    body1.append( testHtml );
  }
  
  // Question might be plain or html
  let question_container = document.createElement("div");
  question_container.id = "question_container";
  
  let question = document.createElement("div");
  question.id = "question";
  question.setAttribute("class", "card-header" );
  question.innerHTML = dto.questionHtml == null ? dto.question : dto.questionHtml;
  question_container.append( question );
  
  
  // Input or Options:
  // Heap options are badges
  // MCQ Options are checkboxes
  // QA Options are radio
  let body2 = document.createElement( "div" );
  body2.setAttribute( "class", "card-body" );
  
  // Input type is also an option (either Heap or QA Test, but for Heap Test the question and option_div need to exists beforehand)
  // BTW MCQ cannot be of type input (was: && dto.testType === "QA")
  
  if ( dto.inquiryType === "Input" && dto.testType !== "HEAP" ) {
    // BTW QA Test must contain one and only one option
    let answer_input = document.createElement("input");
    answer_input.setAttribute("class", "form-control input-qa");
    answer_input.placeholder = "Your Answer";
    answer_input.autofocus = true;
    answer_input.id = "answer." + dto.options[0].id;
    answer_input.name = "answer." + dto.options[0].id;
  
    body2.append( answer_input );
  
  } else {
    // For Heap Test the option_div need to exists beforehand
    if ( dto.testType === "HEAP" ) {            // Heap Test is using words as clickable badges
      let option_div = document.createElement( "div" );
      option_div.id = "option_div";
      option_div.setAttribute( "class", "row ml-2" );
      body2.append( option_div );
      
    } else {
      for ( let option of dto.options ) {
        let option_div = document.createElement( "div" );
        option_div.setAttribute( "class", "row ml-2" );
    
        let option_label = document.createElement( "label" );
        let option_input = document.createElement("input");
        option_input.setAttribute( "type", dto.testType === "MCQ" ? "checkbox" : "radio" );
        option_input.setAttribute( "class", "radio_random" );
        option_input.id = "option." + option.id;
        option_input.name = "option" + ( dto.testType === "MCQ" ? ( "." + option.id ) : "" );
        option_input.value = option.id;
    
        let option_b = document.createElement( "b" );
        option_b.setAttribute( "class", "mr-1" );
        option_b.append( " " );
    
        let option_span = document.createElement( "span" );
        option_span.append( option.answer );
    
        // option_label.append( option_input, option_selected, option_b, option_span );
        option_label.append( option_input, option_b, option_span );
        option_div.append( option_label );
        body2.append( option_div );
      }
    }
  }
  
  card2.append( body1, question_container, body2 );
  card1.append( card2 );
  div1.append( card1 );
  data_div.append( div1 );
  
  // For Heap Test the question and the option_div need to exists beforehand
  if ( dto.testType === "HEAP" ) {
    if ( dto.inquiryType === "Choose" ) {
      // Heap Test of type Choose is using words as clickable badges
      for ( let word of dto.words ) {
        addWordToOptionDiv( word );
      }
    } else {
      // For Heap Test of Input type need to loop through the question and replace every placeholder with the input field
      transformQuestion();
    }
  }
}

// For Heap Test of Input type need to loop through the question and replace every placeholder with the input field
function transformQuestion() {
  let question = document.getElementById( "question" );
  
  let theQuestion = "";
  let sentence = question.innerHTML;
  let length = sentence.length;
  let from = -1;
  let to = -1;
  let i = 0;
  while ( i < length ) {
    let char = sentence.charAt( i );
    if ( char === "_" ) {
      if ( from < 0 ) {
        from = i;
        to = i;
      } else {
        to = i;
      }
    } else {
      if ( from > -1 ) {
        let width = to - from + 1;
        let input = document.createElement( "input" );
        input.id = "word." + index;
        input.name = "word." + index;
        input.title = "Type the word here";
        input.type = "text";
        input.maxLength = width;
        input.setAttribute( "class", "form-control-sm mx-1 px-1" );
        input.setAttribute( "style", "width: " + ( 15 + width * 10 ) + "px; font-weight: bold; border: 1px solid #a7a8aa" );
        theQuestion += input.outerHTML;         // the input replacing the placeholder
        theQuestion += char;                    // current trailing character (after the placeholder)
        index++;                                // increment the index
        from = -1;                              // reset marker index
      } else {
        theQuestion += char;
      }
    }
    i++;
  }
  question.innerHTML = "";
  question.innerHTML = theQuestion;
}

// For HeapTest add a word as an option
function addWordToOptionDiv( word ) {
  let option_div = document.getElementById( "option_div" );
  
  let button = document.createElement( "button" );
  button.id = "id." + index + "." + word;
  button.name = "id." + index + "." + word;
  button.title = word;
  button.type = "button";
  button.setAttribute( "class", "badge-pill badge-heap px-2 mr-3" );
  button.setAttribute( "style", "cursor: pointer; font-size: 1rem" );
  button.onclick = function(){ transformWord( this ) };
  button.append( word );
  option_div.append( button );
  index++;
}

function timeIsOver() {
  let div1 = document.createElement( "div" );
  div1.setAttribute( "class", "mt-2" );
  div1.id = "TimeIsOver";
  
  let div21 = document.createElement( "div" );
  div21.setAttribute( "class", "has-error text-center" );
  
  let div22 = document.createElement( "div" );
  div22.setAttribute( "class", "text-center" );
  
  let tio_a = document.createElement( "a" );
  tio_a.title = "View achieved exam score result.";
  
  let tio_b = document.createElement( "b" );
  tio_b.append( "here" );
  tio_a.append( tio_b );
  
  if ( examID === 0 ) {                                   // teacher (owner) is only trying ...
    tio_a.href = "/";
    tio_a.setAttribute( "class", "text-success" );
    div21.append( "End of the exam, click ", tio_a, " to go home." );
  } else {
    tio_a.href = urlResult + "/" + examID;
    div21.append( "Exam Time Limit Is Over" );
    div22.append( "Click ", tio_a, " to see your score." );
  }
  
  div1.append( div21, div22 );
  data_div.append( div1 );
}


function submitButton() {
  let div_submit = document.createElement( "div" );
  div_submit.id = "SubmitDiv"
  div_submit.setAttribute( "class", "text-center my-2" );
  
  let b_submit = document.createElement( "button" );
  b_submit.id = "SubmitAnswer";
  b_submit.title = "Submit & Next";
  b_submit.onclick = function () { SubmitAndNext() };
  b_submit.setAttribute( "class", "btn btn-outline-success mx-3" );
  b_submit.setAttribute( "type", "button" );
  let b_submit_i = document.createElement( "i" );
  b_submit_i.setAttribute( "class", "far fa-check-circle mr-2" );
  let b_submit_text = document.createTextNode( "Submit & Next" );
  b_submit.append( b_submit_i, b_submit_text );
  
  
  div_submit.append( b_submit );
  data_div.append( div_submit );
}


// Submit the answer and get next inquiry
function SubmitAndNext() {
  // Never allow to submit the form after timelimit has passed
  if ( isTimeOver )
    return;
  
  // Was this the last inquiry?
  if ( dto.inquiry === dto.inquiries )
    endOfExam = true;
  
  let form = document.getElementById("form");
  let formData = new FormData( form );
  
  // Instead of enriching the form with additional details, enrich received dto with client's input
  if ( dto.inquiryType == "Input" ) {
    if ( dto.testType === "HEAP" ) {
      // The response to HeapTest question need to be assembled from original question and all submitted words
      let question = document.getElementById( "question" );
      let response = "";
      
      let array = question.innerHTML.split( "<input " );
      for ( let e of array ) {
        if ( e.startsWith( "id=" ) ) {          // the reset always starts with the word id (might ends by a text)
          let wid = e.substring( 4, e.indexOf( '"', 9 ) );
          let word = formData.get( wid );
          if ( word === null || word.length === 0 ) {
            response += "_";
          } else {
            response += formData.get( wid );    // word value as submitted by a user
          }
          
          let tail = e.lastIndexOf( ">" );
          if ( tail > -1 )
            response += e.substring( tail + 1 );// tailing text between or after the last placeholder
          
        } else {
          response += e;                        // part before 1st placeholder
        }
      }
      dto.options[0].response = response;
      
    } else {
      for ( let entry of formData.entries() ) {
        let answer_id = parseInt( entry[0].substring( entry[0].indexOf( "answer." ) + 7 ), 10 );
    
        for ( let option of dto.options ) {
          if ( option.id === answer_id ) {
            option.response = entry[1];
          }
        }
      }
    }
  } else {                                      // InquiryType: Choose
    if ( dto.testType === "HEAP" ) {
      // HeapTest answer is assembled in question.textContent
      let question = document.getElementById( "question" );
      dto.options[0].response = question.textContent;
      
    } else {
      // QA option is a radio ("option":"ID"), MCQ a checkbox ("option.ID1":"ID1", "option.ID2":ID2", ...)
      for ( let entry of formData.entries() ) {
        // let option_id = parseInt( entry[0].substring( entry[0].lastIndexOf( "option" ) + 7 ), 10 );
        for ( let option of dto.options ) {
          if ( option.id == entry[1] ) {          // entry[1] is a string
            option.selected = true;
          }
        }
      }
    }
  }
  
  // Get rid of unnecessary data and send the dto to rest api
  dto.examName = "";
  dto.testname = "";
  dto.testText = "";
  dto.testHtml = "";
  
  
  // DEBUG :: Semantic Validation
  // console.log( JSON.stringify( Object.fromEntries( formData ) ) );
  // console.log( JSON.stringify( dto ) );
  
  
  let csrfToken = document.getElementById('csrf');                  // Cross-Site Request Forgery (CSRF) + .antMatchers( HttpMethod.POST, "/api/**" ).authenticated()
  let xhttp = new XMLHttpRequest();
  xhttp.open( 'POST', urlPost + "/" + id );
  xhttp.setRequestHeader('x-csrf-token', csrfToken.value);             // xhttp.withCredentials = true; not needed for the same site
  xhttp.setRequestHeader( 'Content-Type', 'application/json' );  // xhttp.setRequestHeader( "Content-Type", "application/x-www-form-urlencoded" );
  xhttp.send( JSON.stringify( dto ) );
  
  
  // The output
  xhttp.onreadystatechange = function () {
    if ( this.readyState === 4 ) {
      if ( this.status === 200 ) {
        console.log( "Response saved, expecting new data or the end" );
        
        if ( endOfExam && ( xhttp.responseText == null || xhttp.responseText.length === 0 ) ) {
          console.log( "End of the exam, show the end message with the link to see the results." );
          
          // Show the end message and link to see the results.
          examEnd();
          
          
        } else if ( xhttp.responseText == null || xhttp.responseText.length === 0 ) {
          console.log( "I am sorry, something bad happened, exam cannot continue. :-(" );
          
        } else {
          console.log( "Everything is fine, re-generating UI with new data ..." );
          
          // New InquiryDto
          dto = JSON.parse( xhttp.responseText );
          console.log( "Received InquiryDto of " + xhttp.responseText.length + " chars." );
  
          // Cleanup the page, but TODO: do not download the same file again
          while ( data_div.firstChild )
            data_div.removeChild( data_div.lastChild );
          
          generateUI();
        }
      } else if ( this.status === 204 ) {
        console.log( "I am sorry, something bad happened, exam cannot continue due: " + this.status );
    
      } else {
        console.log( "I am sorry, something very bad happened, exam cannot continue due: " + this.status );
        
      }
    }
  }
}


// Goodbye UI & message (inspired by examBegin)
function examEnd() {
  // Stop the countdown
  countDown( true );
  
  // Cleanup the page
  while ( data_div.firstChild )
    data_div.removeChild( data_div.lastChild );
  
  
  let div1 = document.createElement("div");
  div1.setAttribute("class", "mb-4");
  
  let div2 = document.createElement("div");
  div2.setAttribute("class", "card border-left-info shadow");
  
  let div3 = document.createElement("div");
  div3.setAttribute("class", "card-body");
  
  let div4 = document.createElement("div");
  div4.setAttribute("class", "row align-items-center");
  
  let div5 = document.createElement("div");
  div5.setAttribute("class", "col mx-3 my-2");
  
  let div6 = document.createElement("div");
  div6.setAttribute("class", "row align-items-center");
  
  let div7 = document.createElement("div");
  div7.setAttribute("class", "col text-center");
  
  let tio_a = document.createElement( "a" );
  tio_a.title = "View achieved exam score result.";
  tio_a.href = urlResult + "/" + examID;
  
  let tio_b = document.createElement( "b" );
  tio_b.append( "here" );
  tio_a.append( tio_b );
  if ( examID === 0 ) {                                   // teacher (owner) is only trying ...
    tio_a.href = "/";
    tio_a.setAttribute( "class", "text-success" );
    div7.append( "End of the exam, click ", tio_a, " to go home." );
  } else {
    div7.append( "You just accomplished the exam, click ", tio_a, " to see your score." );
  }
  
  
  // The Card Assembly
  div6.append( div7 );
  div5.append( div6 );
  div4.append( div5 );
  div3.append( div4 );
  div2.append( div3 );
  div1.append( div2 );
  data_div.append( div1 );
}


// HeapTest's words are buttons which are put into a question instead of placeholders (back and forth)
function transformWord( word ) {
  console.log( "Adding " + word.id + " ..." );
  let question = document.getElementById( "question" );
  
  // Find 1st available placeholder in the question
  let first = -1;
  let last  = -1;
  let i = 0;
  let sentence = question.innerHTML;
  let length = sentence.length;
  while ( i < length ) {
    let char = sentence.charAt( i );
    if ( char === "_" ) {
      if ( first < 0 ) {
        first = i;
        last = i;
      } else {
        last = i;
      }
    } else {
      if ( first > -1 && last > -1 )
        break;
    }
    i++;
  }
  // Stop adding more than possible
  if ( first < 0 && last < 0 )
    return;
  
  // Create word as a button to be put into the question
  index++;
  let button = document.createElement( "button" );
  button.id = "idx." + index + "." + word.title;
  button.name = word.title;
  button.title = word.title;
  button.type = "button";
  button.setAttribute( "class", "btn btn-sm badge-heap py-0 my-0 px-2" );
  button.setAttribute( "style", "cursor: pointer" );
  button.append( word.title );
  
  // Replace 1st available placeholder in the question with selected word
  let part1 = ( first === 0 ) ? "" : sentence.substring( 0, first );
  let part2 = ( last + 1 === length ) ? "" : sentence.substring( last + 1, length );
  
  // // Remove existing question
  // let question_container = document.getElementById( "question_container" );
  // while ( question_container.firstChild )
  //   question_container.removeChild( question_container.lastChild );
  //
  // // Add new form of the question
  // let new_question = document.createElement("div");
  // new_question.id = "question";
  // new_question.setAttribute("class", "card-header" );
  // new_question.innerHTML = part1 + button.outerHTML + part2;
  // question_container.append( new_question );
  question.innerHTML = "";
  question.innerHTML = part1 + button.outerHTML + part2;
  
  // Need to (re)register events of all already existing buttons
  let buttons = document.getElementById( question.id ).children;
  for( let i = 0; i < buttons.length; i++ ) {
    buttons[i].addEventListener( "click", function () { withdrawWord( buttons[i].id ) } );
  }
  
  // INFO
  // console.log( question.innerHTML );
  // console.log( question.textContent );      // value to send back for a validation
}


// User can withdraw any word from HeapTest's question
function withdrawWord( word_id ) {
  console.log( "Withdrawing " + word_id + " ..." );
  
  // Get button's position to restore its placeholder
  let question = document.getElementById( "question" );
  let sentence = question.innerHTML;
  
  let button = document.getElementById( word_id );
  let length = button.title.length;
  let position = sentence.indexOf( "<button id=\"" + word_id );
  
  // Remove button from the question
  document.getElementById( word_id ).remove();
  
  // Reload question to add placeholder to it after button removal
  question = document.getElementById( "question" );
  sentence = question.innerHTML;
  
  // INFO
  // console.log( sentence );
  
  let part1 = ( position === 0 ) ? "" : sentence.substring( 0, position );
  let part2 = ( position + 1 === length ) ? "" : sentence.substring( position );
  question.innerHTML = part1 + "_".repeat( length ) + part2;
  
  // INFO
  // console.log( document.getElementById( "question" ).innerHTML );
  
  // Need to (re)register events of all already existing buttons
  let buttons = document.getElementById( "question" ).children;
  for( let i = 0; i < buttons.length; i++ ) {
    buttons[i].addEventListener( "click", function () { withdrawWord( buttons[i].id ) } );
  }
}




