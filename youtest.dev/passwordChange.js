/**
 * Password Reset UI
 */


// Global variables
let data_div;                                   // Container to generate the data: Password Reset Form
let warning;                                    // Container to generate warning message
let urlPost = "/api/realm/passwordChange";      // POST password change, send the form to the REST api (User with password only)
let token = 0;                                  // token received with and used in url as a parameter
let minPwdLength = 8;                           // minimum password length


// After the page is loaded generate UI to change the password
$(document).ready(function () {
  // Container to upload to
  data_div = document.getElementById( 'data_div' );
  warning = document.getElementById( 'warning' );
  
  // Get token from url
  let url = window.location.href;
  token = parseInt( url.substring( url.lastIndexOf( '/' ) + 1 ) );
  if ( isNaN( token ) ) {
    console.log( "Invalid url or invalid token: " + token );
    return;
  } else {
    console.log( "Token: " + token );
  }
  if ( url.includes( "localhost" ) ) {
    minPwdLength = 1;
  }
  
  
  // <h4 class="form-heading">Password Change</h4>
  let head = document.createElement( "h4" );
  head.setAttribute( "class", "form-heading" );
  head.append( "Change Your Password" );
  
  let pwdLength = document.createElement( "div" );
  pwdLength.id = "pwdLength";
  pwdLength.setAttribute( "class", "small" );
  pwdLength.innerHTML = "Minimum password length is: " + minPwdLength;
  // pwdLength.setAttribute( "style", "display: none" );
  
  let pwd = document.createElement( "input" );
  pwd.id = "pwd";
  pwd.type = "password";
  pwd.name = "password";
  pwd.setAttribute( "class", "form-control" );
  pwd.placeholder = "New Password";
  pwd.autofocus = true;
  pwd.title = "Set New Password";
  
  let confirm = document.createElement( "input" );
  confirm.id = "confirm";
  confirm.type = "password";
  confirm.name = "confirm";
  confirm.setAttribute( "class", "form-control" );
  confirm.placeholder = "Confirm Your New Password";
  confirm.title = "Confirm Your New Password";
  
  let submit = document.createElement( "button" );
  submit.id = "submit";
  submit.type = "button";
  submit.name = "Submit";
  submit.setAttribute( "class", "btn btn-lg btn-primary btn-block" );
  submit.title = "Submit Password Change";
  submit.append( "Submit" );
  submit.onclick = function() { submitPasswordChange() };
  submit.disabled = true;
  
  let div1 = document.createElement( "div" );
  div1.setAttribute( "class", "text-center small mt-3" );
  div1.append( "After you submit password change you will be redirected to the login page." );
  
  let note = document.createElement( "div" );
  note.id = "note";
  note.setAttribute( "class", "text-center text-danger mt-2" );
  note.innerHTML = "Passwords do not match";
  note.setAttribute( "style", "display: none" );
  
  
  data_div.append( head, pwdLength, pwd, confirm, submit, div1, note );
  
  
  // Password and Confirm Password validation
  $(":password").on('keyup', function (){
    let pwd = $( "#pwd" ).val();
    
    if ( pwd !== $( "#confirm" ).val() ) {
      $('#submit').attr('disabled', true);
      document.getElementById("note").style.display = 'block';
    } else {
      $('#submit').removeAttr('disabled');
      document.getElementById("note").style.display = 'none';
    }
    
    if ( pwd.length < minPwdLength ) {
      $('#submit').attr('disabled', true);
    } else {
      $('#submit').removeAttr('disabled');
    }
    
  });
});


function submitPasswordChange() {
  console.log( "Initiate to change user's password ..." );
  
  let form = document.getElementById("form");
  let formData = new FormData( form );
  formData.delete( "confirm" );       // need only password and the token
  
  const xhttp = new XMLHttpRequest();
  xhttp.open( 'POST', urlPost + "/" + token );
  xhttp.send( formData );
  
  xhttp.onreadystatechange = function () {
    if ( this.readyState === 4 ) {
      if ( this.status === 200 ) {
        console.log( "Password Change succeeded, redirecting to the login ..." );
        location.href = "/login";
        
      } else {
        console.log( 'Password change has failed. ' + this.responseText + " Response code: " + this.status );
 
        while ( warning.firstChild )
          warning.removeChild( warning.lastChild );
  
        let note = document.createElement( "div" );
        note.setAttribute( "class", "text-danger mt-2" );
        note.innerHTML = "Password change has failed due: <br />" + this.responseText;
        warning.append( note );
      }
    }
  }
}