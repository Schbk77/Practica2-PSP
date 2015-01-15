package com.example.serj.cleverbot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.serj.cleverbot.chatterbotapi.ChatterBot;
import com.example.serj.cleverbot.chatterbotapi.ChatterBotFactory;
import com.example.serj.cleverbot.chatterbotapi.ChatterBotSession;
import com.example.serj.cleverbot.chatterbotapi.ChatterBotType;

import java.util.ArrayList;
import java.util.Locale;

public class Principal extends Activity implements TextToSpeech.OnInitListener, AdapterView.OnItemSelectedListener {

    /**********************************************************************************************/
    /**************************************VARIABLES***********************************************/
    /**********************************************************************************************/

    private ChatterBotFactory factory;
    private ChatterBot bot;
    private ChatterBotSession botsession;
    private boolean sintetizador = false;
    private TextToSpeech tts;
    private String dictado, respuesta;
    private ArrayList<String> conversacion;
    private int tono = 1, velocidad = 1, idioma = 0;
    private Adaptador ad;
    private ListView lv;
    private static int TTS_CHECK = 1;
    private static int DICTADO = 2;

    /**********************************************************************************************/
    /****************************************ON...*************************************************/
    /**********************************************************************************************/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TTS_CHECK) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                tts = new TextToSpeech(Principal.this, this);
            } else {
                checkTTS();
            }
        } else if (requestCode == DICTADO){
            if(resultCode == RESULT_OK){
                ArrayList<String> textos = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                dictado = textos.get(0);
                conversacion.add(dictado);
                ad.notifyDataSetChanged();
                lv.setSelection(ad.getCount()-1);
                pensar();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);
        initComponents();
        initBot();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS){
            sintetizador = true;
            tts.setLanguage(Locale.getDefault());
        } else {
            sintetizador = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return ajustes();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkTTS();
        if(tts != null){
            setPitch();
            setSpeed();
            setLanguage();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        liberarRecursos();
    }

    /**********************************************************************************************/
    /******************************************HILOS***********************************************/
    /**********************************************************************************************/

    class Hilo extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                respuesta = botsession.think(dictado);
                conversacion.add(respuesta);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            ad.notifyDataSetChanged();
            lv.setSelection(ad.getCount()-1);
            reproducir();
        }
    }

    /**********************************************************************************************/
    /***********************************MÉTODOS AUXILIARES*****************************************/
    /**********************************************************************************************/

    public boolean ajustes() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        final View ajustesTTS = inflater.inflate(R.layout.ajustes, null);
        builder.setTitle(getString(R.string.ajustesTTS));
        builder.setView(ajustesTTS);

        final Spinner spinnertono = (Spinner)ajustesTTS.findViewById(R.id.spinnertono);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.pitchvalues, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnertono.setAdapter(adapter);
        spinnertono.setOnItemSelectedListener(this);
        spinnertono.setSelection(tono);
        final Spinner spinnervelocidad = (Spinner)ajustesTTS.findViewById(R.id.spinnervelocidad);
        adapter = ArrayAdapter.createFromResource(this, R.array.speedvalues, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnervelocidad.setAdapter(adapter);
        spinnervelocidad.setOnItemSelectedListener(this);
        spinnervelocidad.setSelection(velocidad);
        final Spinner spinneridioma = (Spinner)ajustesTTS.findViewById(R.id.spinneridioma);
        adapter = ArrayAdapter.createFromResource(this, R.array.languagevalues, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinneridioma.setAdapter(adapter);
        spinneridioma.setOnItemSelectedListener(this);
        spinneridioma.setSelection(idioma);

        builder.setPositiveButton(R.string.aceptar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setPitch();
                setSpeed();
                setLanguage();
            }
        });
        builder.setNegativeButton(R.string.cancelar, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        return true;
    }

    private void checkTTS(){
        Intent intent = new Intent();
        intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(intent, TTS_CHECK);
    }

    public void dictar(View v){
        String language  = getString(R.string.espanol);
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        switch (idioma){
            case 0:
                language = getString(R.string.espanol);
                break;
            case 1:
                language = getString(R.string.ingles);
                break;
            case 2:
                language = getString(R.string.frances);
                break;
        }
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, language);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language);
        i.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.habla);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);
        startActivityForResult(i, DICTADO);
    }

    private void initBot() {
        try {
            factory= new ChatterBotFactory();
            bot = factory.create(ChatterBotType.CLEVERBOT);
            botsession = bot.createSession();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() {
        conversacion = new ArrayList<String>();
        lv = (ListView)findViewById(R.id.lvLista);
        ad = new Adaptador(this, R.layout.listadetalle, conversacion);
        lv.setAdapter(ad);
    }

    private void liberarRecursos() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    public void pensar() {
        Hilo hilo = new Hilo();
        hilo.execute();
    }

    public void reproducir() {
        if(sintetizador){
            tts.speak(respuesta, TextToSpeech.QUEUE_FLUSH, null);
        }else{
            Toast.makeText(this, R.string.notts, Toast.LENGTH_SHORT).show();
        }
    }

    public void setLanguage(){
        switch (idioma){
            case 0:{
                tts.setLanguage(new Locale("es", "ES"));
                break;
            }
            case 1: {
                tts.setLanguage(Locale.ENGLISH);
                break;
            }
            case 2: {
                tts.setLanguage(Locale.FRENCH);
                break;
            }
        }
    }

    public void setPitch(){
        switch (tono){
            case 0:{
                tts.setPitch((float) 2.0);
                break;
            }
            case 1: {
                tts.setPitch((float) 1.0);
                break;
            }
            case 2: {
                tts.setPitch((float) 0.5);
                break;
            }
        }
    }

    public void setSpeed(){
        switch (velocidad){
            case 0:{
                tts.setSpeechRate((float) 2.0);
                break;
            }
            case 1: {
                tts.setSpeechRate((float) 1.0);
                break;
            }
            case 2: {
                tts.setSpeechRate((float) 0.5);
                break;
            }
        }
    }

    /**********************************************************************************************/
    /***********************************OnItemSelectedListener*************************************/
    /**********************************************************************************************/

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.spinnertono: {
                tono = position;
                break;
            }
            case R.id.spinnervelocidad: {
                velocidad = position;
                break;
            }
            case R.id.spinneridioma: {
                idioma = position;
                break;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Método para hacer algo en caso de no seleccionar ningún item del spinner
    }
}