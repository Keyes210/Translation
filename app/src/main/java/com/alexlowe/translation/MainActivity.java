package com.alexlowe.translation;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends Activity implements
        TextToSpeech.OnInitListener {

    // Define the spoken language we wish to use
    // You must install all of these on your phone for text to speech
    // Settings - Language & Input - Text-to-speech output -
    // Preferred Engine Settings - Install voice data
    private Locale currentSpokenLang = Locale.US;

    // Create the Locale objects for languages not in Android Studio
    private Locale locSpanish = new Locale("es", "MX");
    private Locale locRussian = new Locale("ru", "RU");
    private Locale locPortuguese = new Locale("pt", "BR");
    private Locale locDutch = new Locale("nl", "NL");

    // Stores all the Locales in an Array so they are easily found
    private Locale[] languages = {locDutch, Locale.FRENCH, Locale.GERMAN, Locale.ITALIAN,
            locPortuguese, locRussian, locSpanish};

    // Synthesizes text to speech
    private TextToSpeech textToSpeech;

    // Spinner for selecting the spoken language
    private Spinner languageSpinner;

    // Currently selected language in Spinner
    private int spinnerIndex = 0;

    // Will hold all translations
    private String[] arrayOfTranslations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        languageSpinner = (Spinner) findViewById(R.id.lang_spinner);

        // When the Spinner is changed update the currently selected language
        // to speak in
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long l) {
                currentSpokenLang = languages[index];

                // Store the selected Spinner index for use elsewhere
                spinnerIndex = index;

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        textToSpeech = new TextToSpeech(this, this);

    }

    // When the app closes shutdown text to speech
    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    // Calls for the AsyncTask to execute when the translate button is clicked
    public void onTranslateText(View view) {

        EditText translateEditText = (EditText) findViewById(R.id.words_edit_text);

        // If the user entered words to translate then get the JSON data
        if(!isEmpty(translateEditText)){

            Toast.makeText(this, "Getting Translations",
                    Toast.LENGTH_LONG).show();

            // Calls for the method doInBackground to execute
            new SaveTheFeed().execute();

        } else {

            // Post an error message if they didn't enter words
            Toast.makeText(this, "Enter Words to Translate",
                    Toast.LENGTH_SHORT).show();

        }

    }

    // Check if the user entered words to translate
    // Returns false if not empty
    protected boolean isEmpty(EditText editText){

        // Get the text in the EditText convert it into a string, delete whitespace
        // and check length
        return editText.getText().toString().trim().length() == 0;

    }

    // Initializes text to speech capability
    @Override
    public void onInit(int status) {
        // Check if TextToSpeech is available
        if (status == TextToSpeech.SUCCESS) {

            int result = textToSpeech.setLanguage(currentSpokenLang);

            // If language data or a specific language isn't available error
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language Not Supported", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Text To Speech Failed", Toast.LENGTH_SHORT).show();
        }

    }

    // Speaks the selected text using the correct voice for the language
    public void readTheText(View view) {

        // Set the voice to use
        textToSpeech.setLanguage(currentSpokenLang);

        // Check that translations are in the array
        if (arrayOfTranslations.length >= 9){

            // There aren't voices for our first 3 languages so skip them
            // QUEUE_FLUSH deletes previous text to read and replaces it
            // with new text
            textToSpeech.speak(arrayOfTranslations[spinnerIndex+4], TextToSpeech.QUEUE_FLUSH, null);

        } else {

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
            EditText translateEditText = (EditText) findViewById(R.id.words_edit_text);
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

        @Override
        protected void onPostExecute(Void aVoid) {
            TextView translationTextView = (TextView) findViewById(R.id.translate_text_view);

            translationTextView.setMovementMethod(new ScrollingMovementMethod());

            //just want translation, not the language names
            String stringOfTrans = result.replaceAll("\\w+\\s:", "#");

            //breaks long string into array, separates by #
            arrayOfTranslations = stringOfTrans.split("#");

            translationTextView.setText(result);
        }
    }

    // Converts speech to text
    public void ExceptSpeechInput(View view) {

        // Starts an Activity that will convert speech to text
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // Use a language model based on free-form speech recognition
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // Recognize speech based on the default speech of device
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        // Prompt the user to speak
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_input_phrase));

        try{

            startActivityForResult(intent, 100);

        } catch (ActivityNotFoundException e){

            Toast.makeText(this, getString(R.string.stt_not_supported_message), Toast.LENGTH_LONG).show();

        }

    }

    // The results of the speech recognizer are sent here
    protected void onActivityResult(int requestCode, int resultCode, Intent data){

        // 100 is the request code sent by startActivityForResult
        if((requestCode == 100) && (data != null) && (resultCode == RESULT_OK)){

            // Store the data sent back in an ArrayList
            ArrayList<String> spokenText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            EditText wordsEntered = (EditText) findViewById(R.id.words_edit_text);

            // Put the spoken text in the EditText
            wordsEntered.setText(spokenText.get(0));

        }

    }


}

