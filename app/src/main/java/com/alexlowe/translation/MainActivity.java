package com.alexlowe.translation;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private Locale currentSpokenLang = Locale.US;

    private Locale locSpanish = new Locale("es", "MX");
    private Locale locRussian = new Locale("ru", "RU");
    private Locale locPortuguese = new Locale("pt", "BR");
    private Locale locDutch = new Locale("nl", "NL");

    private Locale[] languages = {locDutch, Locale.FRENCH, Locale.GERMAN, Locale.ITALIAN, locPortuguese,
                    locRussian, locSpanish};

    private TextToSpeech textToSpeech;
    private Spinner languageSpinner;
    private int spinnerIndex = 0;

    private String[] translationsArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        languageSpinner = (Spinner) findViewById(R.id.lang_spinner);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSpokenLang = languages[position];
                spinnerIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        textToSpeech = new TextToSpeech(this, this);
    }

    //releases tts if it's running when the app is killed
    @Override
    protected void onDestroy() {
        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Calls for the AsyncTask to execute when the translate button is clicked
    public void onTranslateClick(View view) {
        EditText translateEditText = (EditText) findViewById(R.id.translateEditText);

        if (isNetAvail()) {
            if (!isEmpty(translateEditText)) {
                Toast.makeText(this, "Getting translations...", Toast.LENGTH_SHORT).show();

                new SaveTheFeed().execute();
                translateEditText.setText("");
            } else {
                Toast.makeText(this, "Enter Words to Translate", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this,"Network is unavailable", Toast.LENGTH_LONG).show();
        }
    }

    protected boolean isEmpty(EditText editText) {
        // return (editText != null); my idea
        return editText.getText().toString().trim().length() == 0;
    }

    private boolean isNetAvail() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if(networkInfo != null && networkInfo.isConnected()){
            isAvailable = true;
        }
        return isAvailable;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS){
            int result = textToSpeech.setLanguage(currentSpokenLang);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(this, "Text to Speech Failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void readTheText(View view) {
        textToSpeech.setLanguage(currentSpokenLang);
        if(translationsArray.length >= 9){
            textToSpeech.speak(translationsArray[spinnerIndex + 4], TextToSpeech.QUEUE_FLUSH, null);
        }else{
            Toast.makeText(this, "Translate Text First", Toast.LENGTH_SHORT).show();
        }
    }


    // Allows you to perform background operations without locking up the user interface
    // until they are finished
    // The void part is stating that it doesn't receive parameters, it doesn't monitor progress
    // and it won't pass a result to onPostExecute
    private class SaveTheFeed extends AsyncTask<Void, Void, Void> {

        String jsonString = "";
        String result = "";
        String wordsToTrans;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            EditText translateEditText = (EditText) findViewById(R.id.translateEditText);
            wordsToTrans = translateEditText.getText().toString();
            wordsToTrans = wordsToTrans.replace(" ", "+");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Client used to grab data from a provided URL
            DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
            // Provide the URL for the post request
            HttpPost httpPost = new HttpPost("http://newjustin.com/translateit.php?action=translations&english_words=" + wordsToTrans);
            // Define that the data expected is in JSON format
            httpPost.setHeader("Content-Type", "application/json");
            // Allows you to input a stream of bytes from the URL
            InputStream inputStream = null;

            try {
                // The client calls for the post request to execute and sends the results back
                HttpResponse response = httpClient.execute(httpPost);
                // Holds the message sent by the response
                HttpEntity entity = response.getEntity();
                // Get the content sent
                inputStream = entity.getContent();

                // A BufferedReader is used because it is efficient
                // The InputStreamReader converts the bytes into characters
                // My JSON data is UTF-8 so I read that encoding
                // 8 defines the input buffer size

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);

                StringBuilder sb = new StringBuilder();

                String line = null;

                // readLine reads all characters up to a \n and then stores them
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }

                jsonString = sb.toString();

                JSONObject jObject = new JSONObject(jsonString);

                JSONArray jArray = jObject.getJSONArray("translations");

                outputTranslations(jArray);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            TextView translationTextView = (TextView) findViewById(R.id.translationTextView);

            translationTextView.setMovementMethod(new ScrollingMovementMethod());

            //just want translation, not the language names
            String stringOfTrans = result.replaceAll("\\w+\\s:", "#");

            //breaks long string into array, separates by #
            translationsArray = stringOfTrans.split("#");

            translationTextView.setText(result);
        }

        protected void outputTranslations(JSONArray jsonArray) {
            String[] languages = {"arabic", "chinese", "danish", "dutch",
                    "french", "german", "italian", "portuguese", "russian",
                    "spanish"};
            try {
                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject translationObject = jsonArray.getJSONObject(i);

                    result = result + languages[i] + ": " + translationObject.getString(languages[i]) +
                    "\n";
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    public void ExceptSpeechInput(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        //telling recognizer how you want it to interpret speech
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //telling it to use english
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        //message fro prompt
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_input_phrase));

        try{
            startActivityForResult(intent, 100);
        }catch (ActivityNotFoundException e){
            Toast.makeText(this, getString(R.string.stt_not_supported), Toast.LENGTH_SHORT).show();
        }
    }

    protected void OnActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == 100 && data != null && resultCode == RESULT_OK){
            ArrayList<String> spokenText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            EditText wordsEntered = (EditText) findViewById(R.id.translateEditText);
            wordsEntered.setText(spokenText.get(0));
        }
    }




}
