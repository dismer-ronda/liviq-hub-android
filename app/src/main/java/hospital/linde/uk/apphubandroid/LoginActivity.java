package hospital.linde.uk.apphubandroid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import hospital.linde.uk.apphubandroid.utils.Hospital;
import hospital.linde.uk.apphubandroid.utils.JsonSerializer;
import hospital.linde.uk.apphubandroid.utils.Location;
import hospital.linde.uk.apphubandroid.utils.Role;
import hospital.linde.uk.apphubandroid.utils.Token;
import hospital.linde.uk.apphubandroid.utils.User;
import lombok.Getter;
import lombok.Setter;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity
{
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    @Getter private EditText emailView;
    @Getter private EditText passwordView;
    @Getter private CheckBox checkRemember;

    private View mProgressView;
    private View mLoginFormView;

    @Setter @Getter private static Token token;
    @Setter @Getter private static Role role;
    @Setter @Getter private static Hospital hospital;
    @Setter @Getter private static List<Location> locations;
    @Setter @Getter private static boolean rememberLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences sessionPref = getPreferences(Context.MODE_PRIVATE);
        String email = sessionPref.getString( "email", null );
        String pwd = sessionPref.getString( "password", null );
        rememberLogin = sessionPref.getBoolean( "remember", false );

        System.out.println( "email " + email );
        System.out.println( "password " + pwd );
        System.out.println( "remember " + rememberLogin );

        setContentView(R.layout.activity_login);

        // Set up the login form.
        emailView = (EditText) findViewById(R.id.email);
        if ( rememberLogin )
            emailView.setText( email );

        passwordView = (EditText) findViewById(R.id.password);
        if ( rememberLogin )
            passwordView.setText( pwd );

        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        checkRemember = (CheckBox) findViewById(R.id.checkbox_remember);
        checkRemember.setChecked( rememberLogin );

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button exitButton = (Button) findViewById(R.id.exit);
        exitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onExit();
            }
        });
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    public void onCheckboxClicked(View view) {
        rememberLogin = ((CheckBox) view).isChecked();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        // Handle item selection
        switch ( item.getItemId() )
        {
            case R.id.settings_option:
                startActivity( new Intent( LoginActivity.this, SettingsActivity.class ) );
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }

    private void onExit()
    {
        finishAndRemoveTask();
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin()
    {
        if ( mAuthTask != null )
            return;

        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Reset errors.
        emailView.setError(null);
        passwordView.setError(null);

        // Store values at the time of the login attempt.
        String email = emailView.getText().toString();
        String password = passwordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if ( TextUtils.isEmpty( email ) )
        {
            emailView.setError(getString(R.string.error_field_required));
            focusView = emailView;
            cancel = true;
        }
        if ( TextUtils.isEmpty( password ) )
        {
            passwordView.setError(getString(R.string.error_field_required));
            focusView = passwordView;
            cancel = true;
        }

        if ( cancel )
        {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        }
        else
        {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            mAuthTask = new UserLoginTask(this, email, password);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean>
    {
        public final int ERROR_NONE                     = 0;
        public final int ERROR_INVALID_LOGIN            = 1;
        public final int ERROR_INVALID_PROFILE          = 2;
        public final int ERROR_EXCEPTION_ROLE           = 3;
        public final int ERROR_EXCEPTION_TOKEN          = 4;

        private final String email;
        private final String password;
        private LoginActivity parentActivity;
        private int errorCode;

        UserLoginTask( LoginActivity parentActivity, String email, String password )
        {
            this.parentActivity = parentActivity;
            this.email = email;
            this.password = password;
        }

        private void saveLoginCredentials()
        {
            SharedPreferences sessionPref = parentActivity.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sessionPref.edit();
            editor.putString( "email", email );
            editor.putString( "password", password );
            editor.putBoolean( "remember", rememberLogin );
            editor.commit();
        }

        private Token getLoginToken( String hospitalUrl )
        {
            HttpsURLConnection urlConnection = null;

            try
            {
                URL url = new URL( hospitalUrl + "/api/appusers/login/?include=user" );

                urlConnection = (HttpsURLConnection) url.openConnection();

                SSLContext sc;
                sc = SSLContext.getInstance("TLS");
                sc.init(null, null, new java.security.SecureRandom());

                urlConnection.setSSLSocketFactory(sc.getSocketFactory());
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setReadTimeout(10000);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");

                User user = new User();
                user.setEmail( email) ;
                user.setPassword( password );

                OutputStream output = urlConnection.getOutputStream();
                output.write( JsonSerializer.toJson(user).getBytes("UTF8") );

                urlConnection.connect();

                InputStream in = new BufferedInputStream( urlConnection.getInputStream() );

                BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
                StringBuilder out = new StringBuilder();
                String line;
                while ( (line = reader.readLine()) != null )
                    out.append( line );

                System.out.println( "token " + out.toString() );

                return (Token)JsonSerializer.toPojo( out.toString(), Token.class );
            }
            catch ( Throwable e )
            {
                e.printStackTrace();
                errorCode = ERROR_INVALID_LOGIN;
            }
            finally
            {
                if ( urlConnection != null )
                    urlConnection.disconnect();
            }

            return null;
        }

        private Role getUserRole(String hospitalUrl, Integer roleId, String token)
        {
            HttpsURLConnection urlConnection = null;

            try
            {
                URL url = new URL( hospitalUrl + "/api/Roles/" + roleId + "?access_token=" + token );

                urlConnection = (HttpsURLConnection) url.openConnection();

                SSLContext sc;
                sc = SSLContext.getInstance("TLS");
                sc.init(null, null, new java.security.SecureRandom());

                urlConnection.setSSLSocketFactory(sc.getSocketFactory());
                //urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");

                urlConnection.connect();

                InputStream in = new BufferedInputStream( urlConnection.getInputStream() );

                BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
                StringBuilder out = new StringBuilder();
                String line;
                while ( (line = reader.readLine()) != null )
                    out.append( line );

                System.out.println( "role " + out.toString() );

                return (Role)JsonSerializer.toPojo( out.toString(), Role.class );
            }
            catch ( Throwable e )
            {
                e.printStackTrace();
                errorCode = ERROR_EXCEPTION_ROLE;
            }
            finally
            {
                if ( urlConnection != null )
                    urlConnection.disconnect();
            }

            return null;
        }

        private List<Hospital> getHospitalData(String hospitalUrl, Integer hospitalId, String token)
        {
            HttpsURLConnection urlConnection = null;

            try
            {
                String filter = URLEncoder.encode( "{\"where\":{\"id\":" + hospitalId + "},\"include\":\"configParameters\"}", "UTF8" );

                URL url = new URL( hospitalUrl + "/api/hospitals?filter=" + filter + "&access_token=" + token );

                urlConnection = (HttpsURLConnection) url.openConnection();

                SSLContext sc;
                sc = SSLContext.getInstance("TLS");
                sc.init(null, null, new java.security.SecureRandom());

                urlConnection.setSSLSocketFactory(sc.getSocketFactory());
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");

                urlConnection.connect();

                InputStream in = new BufferedInputStream( urlConnection.getInputStream() );

                BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
                StringBuilder out = new StringBuilder();
                String line;
                while ( (line = reader.readLine()) != null )
                    out.append( line );

                System.out.println( "hospitals " + out.toString() );

                return (List<Hospital>)JsonSerializer.toArrayList( out.toString(), new TypeToken<ArrayList<Hospital>>(){}.getType() );
            }
            catch ( Throwable e )
            {
                e.printStackTrace();
                errorCode = ERROR_EXCEPTION_ROLE;
            }
            finally
            {
                if ( urlConnection != null )
                    urlConnection.disconnect();
            }

            return null;
        }

        private List<Location> getHospitalLocations( String hospitalUrl, Integer hospitalId, String token )
        {
            HttpsURLConnection urlConnection = null;

            try
            {
                String filter = URLEncoder.encode( "{\"where\":{\"hospitalId\":" + hospitalId + ", \"deleted\":0},\"include\":[\"configParameters\"]}", "UTF8" );

                URL url = new URL( hospitalUrl + "/api/locations?filter=" + filter + "&access_token=" + token );

                urlConnection = (HttpsURLConnection) url.openConnection();

                SSLContext sc;
                sc = SSLContext.getInstance("TLS");
                sc.init(null, null, new java.security.SecureRandom());

                urlConnection.setSSLSocketFactory(sc.getSocketFactory());
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");

                urlConnection.connect();

                InputStream in = new BufferedInputStream( urlConnection.getInputStream() );

                BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
                StringBuilder out = new StringBuilder();
                String line;
                while ( (line = reader.readLine()) != null )
                    out.append( line );

                System.out.println( "locations " + out.toString() );

                return (List<Location>)JsonSerializer.toArrayList( out.toString(), new TypeToken<ArrayList<Location>>(){}.getType() );
            }
            catch ( Throwable e )
            {
                e.printStackTrace();
                errorCode = ERROR_EXCEPTION_ROLE;
            }
            finally
            {
                if ( urlConnection != null )
                    urlConnection.disconnect();
            }

            return null;
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences( LoginActivity.this );
            String hospitalUrl = sharedPref.getString( SettingsFragment.SETTINGS_HOSPITAL_URL, "https://iqhospital.io");

            Token token = getLoginToken( hospitalUrl );
            if ( token == null )
                return false;

            Role role = getUserRole( hospitalUrl, token.getUser().getRoleId(), token.getId() );
            if ( role == null )
                return false;
            if ( !role.getName().equals( "HospitalService" ) )
            {
                errorCode = ERROR_INVALID_PROFILE;
                return false;
            }
            List<Hospital> hospitals = getHospitalData( hospitalUrl, token.getUser().getHospitalId(), token.getId() );
            if ( hospitals == null )
                return false;

            List<Location> locations = getHospitalLocations( hospitalUrl, token.getUser().getHospitalId(), token.getId() );
            if ( locations == null )
                return false;

            parentActivity.setToken( token );
            parentActivity.setRole( role );
            parentActivity.setHospital( hospitals.get( 0 ) );
            parentActivity.setLocations( locations );

            saveLoginCredentials();

            return true;
        }

        @Override
        protected void onPostExecute( final Boolean success )
        {
            mAuthTask = null;
            showProgress(false);

            if ( success )
            {
                //finish();

                Intent intent = new Intent( LoginActivity.this, LoggedActivity.class);
                startActivity(intent);
            }
            else
            {
                switch ( errorCode )
                {
                    case ERROR_INVALID_LOGIN:
                        passwordView.setError( getString( R.string.error_incorrect_password ) );
                        passwordView.requestFocus();
                    break;

                    case ERROR_INVALID_PROFILE:
                        emailView.setError( getString( R.string.error_user_not_authorized ) );
                        emailView.requestFocus();
                    break;

                    default:
                    break;
                }
            }
        }

        @Override
        protected void onCancelled()
        {
            mAuthTask = null;
            showProgress( false );
        }
    }

    public static String getWifiSSID()
    {
        String wifiSSID = hospital.getConfigParameters().getWifiSSID();
        if ( wifiSSID == null )
            return " ";

        return wifiSSID;
    }

    public static String getWifiSecurityKey()
    {
        String wifiSecurityKey = hospital.getConfigParameters().getWifiSecurityKey();
        if ( wifiSecurityKey == null )
            return " ";

        return wifiSecurityKey;
    }

    public static String getProxyServer() {
        String proxyServer = hospital.getConfigParameters().getProxyServer();
        if ( proxyServer == null )
            return "";
        return proxyServer;
    }

    public static String getProxyPort() {
        Integer proxyPort = hospital.getConfigParameters().getProxyPort();
        if ( proxyPort == null )
            return "";
        return proxyPort.toString();
    }

    public static String getProxyUser() {
        String proxyUser = hospital.getConfigParameters().getProxyUser();
        if ( proxyUser == null )
            return "";
        return proxyUser;
    }

    public static String getProxyPassword() {
        String proxyPassword = hospital.getConfigParameters().getProxyPassword();
        if ( proxyPassword == null )
            return "";
        return proxyPassword;
    }

}

