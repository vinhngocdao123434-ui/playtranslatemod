package com.playtranslate.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.playtranslate.Prefs
import com.playtranslate.R
import com.playtranslate.applyEdgeToEdge
import com.playtranslate.applyTheme
import com.playtranslate.language.SourceLangId
import kotlinx.coroutines.launch
import java.util.Locale

class TtsVoiceActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var lang: SourceLangId
    private var selectedVoiceName: String? = null
    
    private var tts: TextToSpeech? = null
    private var engines = listOf<TextToSpeech.EngineInfo>()
    private var locales = listOf<Locale>()
    private var voices = listOf<Voice>()

    private lateinit var spinnerEngine: Spinner
    private lateinit var spinnerLanguage: Spinner
    private lateinit var spinnerVoice: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme(this)
        applyEdgeToEdge(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tts_voice)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(sys.left, sys.top, sys.right, maxOf(sys.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }

        prefs = Prefs(this)
        lang = intent.getStringExtra(EXTRA_LANG)?.let { SourceLangId.fromCode(it) } ?: prefs.sourceLangId
        selectedVoiceName = intent.getStringExtra(EXTRA_INITIAL_VOICE) ?: prefs.ttsVoiceName(lang)

        findViewById<MaterialToolbar>(R.id.toolbar).apply {
            title = "TTS Settings"
            setNavigationOnClickListener { finish() }
        }
        findViewById<View>(R.id.headerVoices).visibility = View.GONE
        findViewById<View>(R.id.voiceRows).visibility = View.GONE

        spinnerEngine = findViewById(R.id.spinnerEngine)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        spinnerVoice = findViewById(R.id.spinnerVoice)

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra(EXTRA_PICKED_VOICE, selectedVoiceName))
            finish()
        }

        TtsSpeedSection.bind(findViewById(R.id.speedSection), prefs) {}
        initEngines()
    }

    private fun initEngines() {
        val tempTts = TextToSpeech(this, null)
        engines = tempTts.engines ?: emptyList()
        val engineNames = engines.map { it.label }
        spinnerEngine.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, engineNames)
        
        val savedEngine = prefs.ttsEnginePackage ?: tempTts.defaultEngine
        val selIdx = engines.indexOfFirst { it.name == savedEngine }.coerceAtLeast(0)
        spinnerEngine.setSelection(selIdx)
        tempTts.shutdown()

        spinnerEngine.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val pkg = engines[position].name
                prefs.ttsEnginePackage = pkg
                loadEngine(pkg)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadEngine(pkg: String) {
        tts?.shutdown()
        tts = TextToSpeech(this, { status ->
            if (status == TextToSpeech.SUCCESS) {
                locales = tts?.availableLanguages?.toList()?.sortedBy { it.displayName } ?: emptyList()
                val langNames = locales.map { it.displayName }
                spinnerLanguage.adapter = ArrayAdapter(this@TtsVoiceActivity, android.R.layout.simple_spinner_dropdown_item, langNames)
                
                val targetLoc = Locale.forLanguageTag(prefs.targetLang)
                val selIdx = locales.indexOfFirst { it.language == targetLoc.language }.coerceAtLeast(0)
                spinnerLanguage.setSelection(selIdx)

                spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                        loadVoices(locales[pos])
                    }
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }
            }
        }, pkg)
    }

    private fun loadVoices(locale: Locale) {
        voices = tts?.voices?.filter { it.locale.language == locale.language }?.sortedBy { it.name } ?: emptyList()
        val voiceNames = voices.map { it.name }
        spinnerVoice.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, voiceNames)
        
        val selIdx = voices.indexOfFirst { it.name == selectedVoiceName }.coerceAtLeast(0)
        spinnerVoice.setSelection(selIdx)

        spinnerVoice.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedVoiceName = voices[pos].name
                prefs.setTtsVoiceName(lang, selectedVoiceName)
                tts?.voice = voices[pos]
                tts?.speak("Test", TextToSpeech.QUEUE_FLUSH, null, "test")
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_LANG = "lang"
        const val EXTRA_INITIAL_VOICE = "initial_voice"
        const val EXTRA_PICKED_VOICE = "picked_voice"
        fun intent(context: Context, lang: SourceLangId, initialVoice: String?): Intent = 
            Intent(context, TtsVoiceActivity::class.java).putExtra(EXTRA_LANG, lang.code).putExtra(EXTRA_INITIAL_VOICE, initialVoice)
    }
}
