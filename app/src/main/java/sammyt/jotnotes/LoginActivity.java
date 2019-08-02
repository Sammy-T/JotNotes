package sammyt.jotnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private GoogleSignInClient mSignInClient;
    private FirebaseAuth mAuth;

    private static final int SIGN_IN_REQUEST = 8119;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SignInButton signInButton = findViewById(R.id.sign_in_button);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient w/ the specified options
        mSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart(){
        super.onStart();

        // Check if the user is signed in (not null)
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null){
            proceedToMain();
        }
    }

    private void proceedToMain(){
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
    }

    private void signIn(){
        Intent signInIntent = mSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, SIGN_IN_REQUEST);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account){
        Log.d(LOG_TAG, "FirebaseAuthWithGoogle: " + account.getId());

        // Get a Firebase credential & sign in with it
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Log.d(LOG_TAG, "SignInWithCredential: Success");
                            proceedToMain();

                        }else{
                            Log.w(LOG_TAG, "SignInWithCredential: Failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Sign in failed", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SIGN_IN_REQUEST){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try{
                // Google Sign In was successful, authenticate w/ Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);

            }catch(ApiException e){
                Log.w(LOG_TAG, "Google sign in failed.", e);
                Toast.makeText(LoginActivity.this, "Google Sign in failed", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }
}
