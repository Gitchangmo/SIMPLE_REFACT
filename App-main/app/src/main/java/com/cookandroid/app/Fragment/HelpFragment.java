package com.cookandroid.app.Fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cookandroid.app.ChatMessage;
import com.cookandroid.app.R;
import com.cookandroid.app.adapter.ChatAdapter;

import org.json.JSONObject;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HelpFragment extends Fragment {

    private EditText inputMessage;
    private TextView buttonSend;
    private RecyclerView recyclerChat;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatList;

    // TODO: 실제 서버 IP로 변경 필요
    private static final String SERVER_URL = "http://YOUR_SERVER_IP:8000/chat";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_help, container, false);

        inputMessage = view.findViewById(R.id.inputMessage);
        buttonSend = view.findViewById(R.id.buttonSend);
        recyclerChat = view.findViewById(R.id.recyclerChat);

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList);
        recyclerChat.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerChat.setAdapter(chatAdapter);

        buttonSend.setOnClickListener(v -> {
            String message = inputMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessageToChatList(message, ChatMessage.TYPE_USER);
                inputMessage.setText("");
                new ChatTask().execute(message);
            }
        });

        return view;
    }

    private void addMessageToChatList(String message, int senderType) {
        chatList.add(new ChatMessage(message, senderType));
        chatAdapter.notifyItemInserted(chatList.size() - 1);
        recyclerChat.scrollToPosition(chatList.size() - 1);
    }

    private class ChatTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String message = params[0];
            try {
                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("message", message);

                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                InputStream is;
                if (responseCode == 200) {
                    is = conn.getInputStream();
                } else {
                    is = conn.getErrorStream();
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                return sb.toString();

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                addMessageToChatList("서버 연결 오류", ChatMessage.TYPE_BOT);
                return;
            }
            try {
                JSONObject responseJson = new JSONObject(result);
                if (responseJson.has("reply")) {
                    String reply = responseJson.getString("reply");
                    addMessageToChatList(reply, ChatMessage.TYPE_BOT);
                } else if (responseJson.has("error")) {
                    addMessageToChatList("오류: " + responseJson.getString("error"), ChatMessage.TYPE_BOT);
                } else {
                    addMessageToChatList("알 수 없는 응답 형식", ChatMessage.TYPE_BOT);
                }
            } catch (Exception e) {
                e.printStackTrace();
                addMessageToChatList("응답 파싱 오류", ChatMessage.TYPE_BOT);
            }
        }
    }
}
