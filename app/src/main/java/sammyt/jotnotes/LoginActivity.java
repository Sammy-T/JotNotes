package sammyt.jotnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private GoogleSignInClient mSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private SignInButton mSignInButton;
    private ProgressBar mLoadingBar;

    private enum SignInView{
        SIGN_IN, LOADING
    }

    private static final int SIGN_IN_REQUEST = 8119;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mSignInButton = findViewById(R.id.sign_in_button);
        mLoadingBar = findViewById(R.id.sign_in_loading);

        mSignInButton.setOnClickListener(new View.OnClickListener() {
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
        mUser = mAuth.getCurrentUser();

        if(mUser != null){
            setVisibleView(SignInView.LOADING);
            updateUserDoc();
        }else{
            setVisibleView(SignInView.SIGN_IN);
        }
    }

    // Switches visibility between the Sign In Button and the Loading Progress Bar
    private void setVisibleView(SignInView currentView){
        switch(currentView){
            case SIGN_IN:
                mLoadingBar.setVisibility(View.GONE);
                mSignInButton.setVisibility(View.VISIBLE);
                break;

            case LOADING:
                mSignInButton.setVisibility(View.GONE);
                mLoadingBar.setVisibility(View.VISIBLE);
                break;
        }
    }

    // Creates or updates the user's Firestore document
    private void updateUserDoc(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", mUser.getDisplayName());

        db.collection("users")
                .document(mUser.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(LOG_TAG, "Document successfully written.");

                        // Proceed to the main activity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(LOG_TAG, "Error writing document.", e);
                        Toast.makeText(LoginActivity.this, "Database Error", Toast.LENGTH_SHORT)
                                .show();

                        setVisibleView(SignInView.SIGN_IN); // Show the Sign In Button
                    }
                });
    }

    private void signIn(){
        setVisibleView(SignInView.LOADING); // Show the Loading Progress Bar

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

                            mUser = mAuth.getCurrentUser();
                            updateUserDoc();

                        }else{
                            Log.e(LOG_TAG, "SignInWithCredential: Failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Sign in failed", Toast.LENGTH_SHORT)
                                    .show();

                            setVisibleView(SignInView.SIGN_IN); // Show the Sign In Button
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
                Log.e(LOG_TAG, "Google sign in failed.", e);
                Toast.makeText(LoginActivity.this, "Google Sign in failed", Toast.LENGTH_SHORT)
                        .show();

                setVisibleView(SignInView.SIGN_IN); // Show the Sign In Button
            }
        }
    }
}
