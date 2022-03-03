/**
 * Password Reset Request UI
 */

// Global variables
let data_div;                                   // Container to generate the data: Password Reset Form
let warning;                                    // Container to generate warning message
let url    = "/api/realm/passwordReset";        // GET password reset, send email as a parameter


// After the page is loaded generate UI with semantic task (the equation)
$(document).ready(function () {
  // Container to upload to
  data_div = document.getElementById( 'data_div' );
  warning = document.getElementById( 'warning' );
  
  // <h4 class="form-heading">Password Reset</h4>
  let head = document.createElement( "h4" );
  head.setAttribute( "class", "form-heading" );
  head.append( "Password Reset" );
  
  // <input name="task" type="text" className="form-control" placeholder="14 + 3 * 2 = " autoFocus="true"/>
  let task = document.createElement( "input" );
  task.id = "task";
  task.type = "text";
  task.name = "task";
  task.setAttribute( "class", "form-control" );
  task.placeholder = placeholder();
  task.autofocus = true;
  task.title = "Input the result for given equation.";
  
  // <input name="email" type="text" className="form-control" placeholder="Email Address"/>
  let email = document.createElement( "input" );
  email.id = "email";
  email.type = "email";
  email.name = "email";
  email.setAttribute( "class", "form-control" );
  email.placeholder = "Email Address";
  
  // <button class="btn btn-lg btn-primary btn-block" type="submit">Submit</button>
  let submit = document.createElement( "button" );
  submit.id = "submit";
  submit.type = "button";
  submit.name = "Submit";
  submit.setAttribute( "class", "btn btn-lg btn-primary btn-block" );
  submit.title = "Submit password reset";
  submit.append( "Submit" );
  submit.onclick = function() { submitPasswordReset() };
  
  let div1 = document.createElement( "div" );
  div1.setAttribute( "class", "text-center small mt-3" );
  div1.append( "You can request password reset max 3 times per day for the same account." );
  
  
  data_div.append( head, task, email, submit, div1 );
});



function placeholder() {
  let n1 = Math.floor( Math.random() * 9 ) + 1;
  let n2 = Math.floor( Math.random() * 8 ) + 2;
  let n3 = Math.floor( Math.random() * 7 ) + 3;
  
  return n1 + " + " + n2 + " * " + n3 + " =";
}



function submitPasswordReset() {
  console.log( "Validate task and submit password reset ..." );
  
  let question = document.getElementById( "task" ).placeholder;
  let value = parseInt( document.getElementById( "task" ).value, 10 );
  
  let result = parseInt( question.substr( 0, 1 ), 10 );
  let n2 = parseInt( question.substr( 4, 1 ), 10 );
  let n3 = parseInt( question.substr( 8, 1 ), 10 );
  result += n2 * n3;
  
  // Ignore incompetent attempt and display a note the the user for 3 seconds.
  if ( result !== value ) {
    invalidAttempt( "task", "Sorry, but that isn't the right <b>answer</b>." );
    return;
  }
  
  // Reject invalid email value
  let email = document.getElementById( "email" ).value;
  if ( ! validEmail( email ) ) {
    invalidAttempt( "email", "Sorry, but you entered an <b>invalid email address</b>." );
    return;
  }
  console.log( "Good job, you solved the equation and input valid email address!" );
  
  
  // Send the email for password reset
  const xhttp = new XMLHttpRequest();
  xhttp.open( 'GET', url + "?email=" + encodeURIComponent( email ) );
  xhttp.send();
  
  // If the response is ok show the note only
  xhttp.onreadystatechange = function () {
    if ( this.readyState === 4 ) {
      if ( this.status === 200 ) {
        console.log( "Password Reset was initiated ..." );
        
        while ( data_div.firstChild )
          data_div.removeChild( data_div.lastChild );
        
        let note = document.createElement( "div" );
        note.setAttribute( "class", "text-center" );
        note.innerHTML = "Check your Inbox or Spam folders for a mail<br />containing recovery token.";
        data_div.append( note );
    
      } else {
        console.log( 'Password reset has failed. ' + this.responseText + " Response code: " + this.status );
      }
    }
  }
}


function invalidAttempt( element, message ) {
  let input = document.getElementById( element );
  input.className = "form-control text-danger";
  
  let submit = document.getElementById("submit");
  submit.className = "btn btn-lg btn-secondary btn-block";
  submit.disabled = true;
  
  let note = document.createElement( "div" );
  note.setAttribute( "class", "text-center text-danger mt-2" );
  note.innerHTML = message;
  warning.append( note );
  
  setTimeout( function () {
    submit.className = "btn btn-lg btn-primary btn-block";
    submit.disabled = false;
  
    input.className = "form-control";
    input.value = '';
    
    while ( warning.firstChild )
      warning.removeChild( warning.lastChild );
  }, 3000 );
}


function validEmail( mail ) {
  let pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-z]{2,6}$";
  return /^[a-zA-Z0-9]+[a-zA-Z0-9._~-]*[a-zA-Z0-9]@[a-zA-Z0-9]+[a-zA-Z0-9._~-]*[a-zA-Z0-9]+$/.test( mail );
  // return /^([^\x00-\x20\x22\x28\x29\x2c\x2e\x3a-\x3c\x3e\x40\x5b-\x5d\x7f-\xff]+|\x22([^\x0d\x22\x5c\x80-\xff]|\x5c[\x00-\x7f])*\x22)(\x2e([^\x00-\x20\x22\x28\x29\x2c\x2e\x3a-\x3c\x3e\x40\x5b-\x5d\x7f-\xff]+|\x22([^\x0d\x22\x5c\x80-\xff]|\x5c[\x00-\x7f])*\x22))*\x40([^\x00-\x20\x22\x28\x29\x2c\x2e\x3a-\x3c\x3e\x40\x5b-\x5d\x7f-\xff]+|\x5b([^\x0d\x5b-\x5d\x80-\xff]|\x5c[\x00-\x7f])*\x5d)(\x2e([^\x00-\x20\x22\x28\x29\x2c\x2e\x3a-\x3c\x3e\x40\x5b-\x5d\x7f-\xff]+|\x5b([^\x0d\x5b-\x5d\x80-\xff]|\x5c[\x00-\x7f])*\x5d))*$/.test( mail );
}