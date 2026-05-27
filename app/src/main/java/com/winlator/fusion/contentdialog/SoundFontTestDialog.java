package com.winlator.fusion.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.winlator.fusion.R;
import com.winlator.fusion.core.GeneralComponents;
import com.winlator.fusion.widget.SimplePianoKeyboard;
import com.winlator.fusion.midi.MidiHandler;

import cn.sherlock.com.sun.media.sound.SF2Soundbank;

public class SoundFontTestDialog extends ContentDialog {
    private MidiHandler midiHandler = null;

    public SoundFontTestDialog(Context context, String soundfont) {
        super(context, R.layout.soundfont_test_dialog);
        setIcon(R.drawable.icon_piano);
        setTitle(soundfont);

        String soundfontPath = GeneralComponents.getDefinitivePath(GeneralComponents.Type.SOUNDFONT, context, soundfont);

        try {
            SF2Soundbank soundbank = new SF2Soundbank(new java.io.File(soundfontPath));
            midiHandler = new MidiHandler();
            midiHandler.setSoundBank(soundbank);
            midiHandler.start();
        } catch (Exception e) {
            midiHandler = null;
        }

        final boolean[] midiReady = {midiHandler != null};

        int[] channel = {0};
        final Spinner sInstrument = findViewById(R.id.SInstrument);
        Spinner sChannel = findViewById(R.id.SChannel);
        sChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                channel[0] = position == 1 ? 9 : 0;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        String[] instrumentNames = new String[128];
        for (int i = 0; i < 128; i++) instrumentNames[i] = "Instrument " + (i + 1);
        sInstrument.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, instrumentNames));

        SimplePianoKeyboard pianoKeyboard = findViewById(R.id.SimplePianoKeyboard);
        pianoKeyboard.setOnKeyListener(new SimplePianoKeyboard.OnKeyListener() {
            @Override
            public void onKeyDown(int index) {
            }

            @Override
            public void onKeyUp(int index) {
            }
        });

        findViewById(R.id.LLBottomBar).setVisibility(View.GONE);
    }

    @Override
    public void dismiss() {
        if (midiHandler != null) {
            midiHandler.stop();
            midiHandler = null;
        }
        super.dismiss();
    }
}
