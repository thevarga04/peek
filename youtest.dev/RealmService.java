package i7.realm;

import i7.GeneralConfig;
import i7.exception.TooManyPasswordResetRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional( propagation = Propagation.REQUIRED, readOnly = false, rollbackFor = Exception.class )
public class RealmService extends GeneralConfig implements Serializable {
  private static final long serialVersionUID = 6802306917810541606L;
  private static final Logger LOGGER = LoggerFactory.getLogger( RealmService.class );
  
  UserService userService;
  PasswordResetRepo passwordResetRepo;
  EmailService emailService;
  
  
  
  public RealmService( UserService userService, PasswordResetRepo passwordResetRepo, EmailService emailService ) {
    this.userService = userService;
    this.passwordResetRepo = passwordResetRepo;
    this.emailService = emailService;
  }
  
  
  
  /**
   * Initiate the password reset process: Generate token and send email up to 3 times per day for defined user
   */
  public void passwordReset( String email ) throws EntityNotFoundException, TooManyPasswordResetRequestsException, MailException {
    User user = userService.getByUsername( email );
    emailService.emailPasswordReset( user, generatePasswordReset( user ) );
  }
  
  
  /**
   * Check number of tokens already generated for this user
   * Generate and persist the token, ++tokens to always get the range over 100_000, so range is 1 .. 3 instead of 0 .. 2
   */
  public PasswordReset generatePasswordReset( User user ) throws TooManyPasswordResetRequestsException {
    int tokens = passwordResetRepo.getTokensForLastDay( now() - 24 * 60 * 60, user );
    if ( tokens > 2 ) {
      LOGGER.info( "passwordReset: too many requests today: {}", tokens );
      throw new TooManyPasswordResetRequestsException( "tokens generated for last day: " + tokens );
    }
    
    int newToken = ThreadLocalRandom.current().nextInt( ++tokens * 100_000, 100_000 + tokens * 100_000 );
    PasswordReset passwordReset = new PasswordReset( newToken, now(), user );
    
    return passwordResetRepo.save( passwordReset );
  }
  
  
  
  /**
   * Change user's password: Get user by token (if valid)
   * Delete expired tokens - don't need to keep old tokens and eliminate checking expiration of the token
   */
  public void passwordChange( int token, String password ) throws EntityNotFoundException {
    int deletedOldTokens = passwordResetRepo.deleteExpiredTokens( now() );
    LOGGER.info( "Deleted {} expired tokens from password_reset table", deletedOldTokens );
    
    PasswordReset passwordReset = passwordResetRepo.findByToken( token ).orElseThrow( EntityNotFoundException::new );
    
    User user = passwordReset.getUser();
    user.setPassword( password );
    LOGGER.info( "Changing {}'s password to: {}", user.getUsername(), password.replaceAll( ".", "." ) );
    
    userService.encodePassword( user );
    userService.saveAccount( user );
    passwordResetRepo.deleteByToken( token );
  }
}