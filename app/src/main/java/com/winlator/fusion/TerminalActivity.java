package com.winlator.fusion;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.winlator.fusion.R;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.ProcessHelper;
import com.winlator.fusion.xenvironment.ImageFs;
import com.winlator.fusion.xenvironment.RootFS;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class TerminalActivity extends AppCompatActivity {
    private TextView outputTextView;
    private EditText commandInput;
    private Button executeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        outputTextView = findViewById(R.id.outputTextView);
        commandInput = findViewById(R.id.commandInput);
        executeButton = findViewById(R.id.executeButton);

        // Set execute permissions for binaries
        RootFS rootFS = RootFS.find(this);
        if (rootFS != null && rootFS.isValid()) {
            setExecutePermissionsForBinaries(rootFS.getRootDir());
        }

        executeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String command = commandInput.getText().toString();
                if (!command.isEmpty()) {
                    executeCommand(command);
                }
            }
        });
    }

    private void setExecutePermissionsForBinaries(File rootDir) {
        File binDir = new File(rootDir, "usr/bin");
        if (binDir.isDirectory()) {
            File[] files = binDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    FileUtils.chmod(file, 0755);
                }
            }
        }
    }

    private void executeCommand(String command) {
        if (command.equals("bash") || command.equals("dash") || command.equals("sh")) {
            outputTextView.append("\n$ " + command + "\nInteractive shells are unsupported.\n");
            return;
        }

        try {
            RootFS rootFS = RootFS.find(this);
            String rootPath = rootFS.getRootDir().getPath();
            String[] env = new String[]{
                "HOME=" + rootPath + "/home/xuser",
                "PATH=" + rootPath + "/usr/bin:/usr/sbin:/bin:/sbin",
                "LD_LIBRARY_PATH=" + rootPath + "/usr/lib",
                "TERM=xterm"
            };

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.environment().put("HOME", rootPath + "/home/xuser");
            pb.environment().put("PATH", rootPath + "/usr/bin:/usr/sbin:/bin:/sbin");
            pb.environment().put("LD_LIBRARY_PATH", rootPath + "/usr/lib");
            pb.environment().put("TERM", "xterm");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();

            outputTextView.append("\n$ " + command + "\n" + output.toString());
        } catch (Exception e) {
            outputTextView.append("\n$ " + command + "\nError: " + e.getMessage() + "\n");
        }
        commandInput.setText("");
    }
}
