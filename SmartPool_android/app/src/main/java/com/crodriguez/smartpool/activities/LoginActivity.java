package com.crodriguez.smartpool.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crodriguez.smartpool.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/*
* Clase para iniciar sesión mediante:
*   - Correo electrónico y contraseña
*   - Cuenta de Facebook
*   - Cuenta de Google
* También es utilizada para dar de alta a un usuario y recuperar la contraseña de un usuario dado.
*/

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private static final int RC_SIGN_IN_GOOGLE = 9001;
    private static final int RC_SIGN_IN_FACEBOOK = 64206 ;
    private static final int RC_SIGN_IN = 1;
    private static final int RC_SIGN_UP = 2;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private CallbackManager mCallbackManager;
    private SignInButton mGoogleLoginButton;
    private GoogleApiClient mGoogleApiClient;
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private TextView nlcOlvidar;
    private TextView nlcNewAccount;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if(user != null){
                goMainActivity();
                Log.i(TAG,"autstate");
            }
            }
        };
        /* *************************************
         *              FACEBOOK               *
         ***************************************/
        mCallbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = findViewById(R.id.btn_facebook);
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleAccessToken(loginResult.getAccessToken(),1,null);
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
                Toast.makeText(getApplicationContext(),"Se canceló la operación", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                Toast.makeText(getApplicationContext(),"Error al iniciar sesión", Toast.LENGTH_SHORT).show();
            }
        });

        /* *************************************
         *               GOOGLE                *
         ***************************************/

        /* Load the Google login button */
        mGoogleLoginButton = (SignInButton) findViewById(R.id.btn_google);
        mGoogleLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInGoogle();
            }
        });

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(LoginActivity.this,"Error al conectar con google", Toast.LENGTH_SHORT);
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        /* *************************************
         *         EMAIL Y PASSWORD            *
         ***************************************/
        // Set up the login form.
        final Button btn_sesion = (Button) findViewById(R.id.btn_iniciosesion);

        mEmailView = (AutoCompleteTextView) findViewById(R.id.edt_email);
        mPasswordView = (EditText) findViewById(R.id.edt_pass);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    comprobarCampos(RC_SIGN_IN);
                    return true;
                }
                return false;
            }
        });


        btn_sesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comprobarCampos(RC_SIGN_IN);
            }
        });

        /* *************************************
         *               SING UP               *
         ***************************************/
        nlcNewAccount = (TextView) findViewById(R.id.nlc_newaccount);
        nlcNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comprobarCampos(RC_SIGN_UP);
            }
        });

        /* *************************************
         *         RECOVER PASSWORD            *
         ***************************************/

        nlcOlvidar = (TextView) findViewById(R.id.nlc_olvidarpass);
        nlcOlvidar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogo();
            }
        });

    }

    private void signInGoogle() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN_GOOGLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
    }

    public void onStop(){
        super.onStop();
        try {
            mAuth.removeAuthStateListener(authStateListener);

        }catch (Exception e){
            Log.i(TAG,"error onStop");
        }
    }

//    Método para comprobar que los campos de texto estan rellenados correctamente
    private void comprobarCampos(int tp) {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.


            if(!TextUtils.isEmpty(mEmailView.getText().toString()) && !TextUtils.isEmpty(mPasswordView.getText().toString())) {
                progress = ProgressDialog.show(LoginActivity.this, "Cargando...", "Por favor espera!");  //show a progress dialog
                switch (tp){
                    case 1:iniciarSesion();
                            break;
                    case 2:registro();
                            break;
                }
            }
            else
                Toast.makeText(this, "Rellene los campos de registro", Toast.LENGTH_SHORT).show();
        }
    }

//    Función para dar de alta a un usuario
    private void registro() {

        mAuth.createUserWithEmailAndPassword(mEmailView.getText().toString(), mPasswordView.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();
                            try {
                                progress.dismiss();
                            }catch (Exception e){}
                        }else{
                            Log.d(TAG, "Iniciando sesión con el usuario nuevo");
                            iniciarSesion();
                        }

                    }
                });

    }

//    Función para iniciar la sesion de un usuario dado mediante email y contraseña
    private void iniciarSesion(){

        Task<AuthResult> resultado = mAuth.signInWithEmailAndPassword(mEmailView.getText().toString(), mPasswordView.getText().toString());
        while (!resultado.isComplete()) {

        }
        if (resultado.isSuccessful()) {
            try {
               progress.dismiss();

            }catch (Exception e){}
            goMainActivity();
        } else {
            try {
                progress.dismiss();

            }catch (Exception e){}
            mPasswordView.setError(getString(R.string.error_incorrect_password));
            mPasswordView.requestFocus();
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 5;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN_GOOGLE) {
            Log.d(TAG, "handle: requestcode");
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                Log.d(TAG, "handle:succes requestcode");
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                progress = ProgressDialog.show(LoginActivity.this, "Cargando...", "Por favor espera!");  //show a progress dialog
                handleAccessToken(null,0,account);
            }
        }else if(requestCode == RC_SIGN_IN_FACEBOOK){
            progress = ProgressDialog.show(LoginActivity.this, "Cargando...", "Por favor espera!");  //show a progress dialog

            // Pass the activity result back to the Facebook SDK
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }

    }

    private void handleAccessToken(AccessToken token, int tipo, GoogleSignInAccount acc) {
        Log.d(TAG, "handle:" + acc);
        AuthCredential credential;
        switch (tipo){
            case 0:
                Log.d(TAG, "handleGoogleAccessToken:" + acc);
                credential = GoogleAuthProvider.getCredential(acc.getIdToken(), null);
                break;
            case 1:
                Log.d(TAG, "handleFacebookAccessToken:" + token);
                credential = FacebookAuthProvider.getCredential(token.getToken());
                break;
             default:
                 credential=null;
                 break;
        }

        if (credential!=null) {

            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            progress.dismiss();
                            goMainActivity();
                            //   updateUI(user);
                            Log.i("prueba", "handle succes");
                        } else {
                            progress.dismiss();
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());

                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
//                            updateUI(null);
                        }
                        }
                    });
        }

    }

//  Dialogo para solicitar la recuperación de la contraseña de
//  un usuario mediante el correo electrónico
    protected void dialogo() {

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        View mView = getLayoutInflater().inflate(R.layout.dialog, null);
        final EditText mPasswordold = (EditText) mView.findViewById(R.id.etPasswordold);
        final EditText mPassword = (EditText) mView.findViewById(R.id.etPasswordnew);
        final EditText mPasswordrep = (EditText) mView.findViewById(R.id.etPasswordrep);
        final TextView titulo = (TextView) mView.findViewById(R.id.dlgTitulo);
        titulo.setText("Recuperar Contraseña");
        Button btnGuardar = (Button) mView.findViewById(R.id.btn_guardar);
        btnGuardar.setText("Enviar");
        mPasswordrep.setVisibility(View.INVISIBLE);
        mPasswordold.setVisibility(View.INVISIBLE);
        mPassword.setHint("email");
        mPassword.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailAddress = mPassword.getText().toString();
                Toast.makeText(getApplicationContext(),emailAddress,Toast.LENGTH_LONG).show();

                mAuth.sendPasswordResetEmail(emailAddress)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    dialog.dismiss();
                                    Log.d(TAG, "Email sent.");
                                }
                            }
                        });
            }

        });


    }

//  Método para redigir a la pantalla principal
    private void goMainActivity() {
        Intent intent= new Intent(this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

}
