package com.example.tglobehelper;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tglobehelper.features.CallbackBundle;
import com.example.tglobehelper.features.OpenFileDialog;
import com.example.tglobehelper.features.SaveFileDialog;
import com.example.tglobehelper.features.UserTable;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private final static String TAG = "NotepadActivity";
    private final static int OPEN_FILE_DIALOG_ID = 101;
    private final static int SAVE_FILE_DIALOG_ID = 102;

    private final static int MSG_PROGRESS_DIALOG = 10;
    private final static int MSG_PROGRESS_DIALOG_DISSMISS = 11;
    private final static String EXTRA_STRING = "extra_string";

    private final static String FIRST_COMMENT = "//Q_Range,Q_Type,Q_Type2,Q_TotalAns,Q_Help,rght_cnt,wrong_cnt,reserved\n";
    private final static String FIRST_8Bytes = "DW\t0,0,0,0,0,0,0,0\n";
    private final static String SUBLINE1_COMMENT = ";Right answer";
    private final static String SUBLINE2_COMMENT = ";Help answer";
    private final static String SUBLINE3_COMMENT = ";???";
    //private String currentPath = "/sdcard/TGlobe/";
    private final static String OPEN_PATH = "/sdcard/TGlobe/";
    private final static String SAVE_PATH = "/sdcard/TGlobe/converted/";
    private String currentFileName = "Table.txt";

    //private final static String FILTER_PATTERN = "\\w{10,}";
    private final static String FILTER_PATTERN = "SPI_SN\\d{4}";
    private final static String USER_END_WORD = "SPI_SN_END";
    private final static String USER_DIVIDER = ",\t";

    private final static String[] FIRST_COMMENTS =
            {
                    "Q_Range",
                    "Q_Type",
                    "Q_Type2",
                    "Q_TotalAns",
                    "Q_Help",
                    "rght_cnt",
                    "wrong_cnt",
                    "reserved"
            };

    private String[] NotExistSoundList;

    private int[] firstlineData = {0,0,0,0,0,0,0,0};

    //view components
    private Button mBtnFile,mBtnEdit,mBtnOpen,mBtnSave;
    //private EditText mEtContent;
    private TextView mTvSource,mTvTarget;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    /**/
        NotExistSoundList = new String[UserTable.ALL_SOUNDS.length-UserTable.EXIST_SOUNDS.length];
        boolean isExist;
        int indexAllSnd=0;
        for(int i =0;i<NotExistSoundList.length;i++){

            for(int j=indexAllSnd;j<UserTable.ALL_SOUNDS.length;j++){
                isExist = false;
                String allSndStr = UserTable.ALL_SOUNDS[j];
                for(String exSndStr:UserTable.EXIST_SOUNDS){
                    if(allSndStr.equals(exSndStr)){
                        isExist = true;
                        break;
                    }
                }
                if(!isExist) {
                    NotExistSoundList[i] = allSndStr;
                    indexAllSnd = j+1;
                    break;
                }
            }
        }

        setupViewComponents();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            /**/
            switch(msg.what){
                case MSG_PROGRESS_DIALOG:
                    mProgressDialog.setMax(msg.arg1);
                    mProgressDialog.setProgress(msg.arg2);
                    break;
                case MSG_PROGRESS_DIALOG_DISSMISS:
                    mTvSource.setText(msg.getData().getString(EXTRA_STRING));
                    mProgressDialog.dismiss();
                    break;
            }
        }
    };

    private void setupViewComponents(){
        mBtnFile = (Button) findViewById(R.id.buttonFile);
        mBtnEdit = (Button) findViewById(R.id.buttonEdit);
        mBtnOpen = (Button) findViewById(R.id.buttonOpen);
        mBtnSave = (Button) findViewById(R.id.buttonSave);
        //mEtContent = (EditText) findViewById(R.id.EditTextContent);
        mTvSource = (TextView)findViewById(R.id.textViewSource);
        mTvSource.setTextColor(Color.GRAY);
        mTvTarget = (TextView)findViewById(R.id.textViewTarget);
        mTvTarget.setTextColor(Color.GREEN);

        mTvTarget.setText(resortTable(NotExistSoundList));
        mBtnFile.setOnClickListener(mBtnOCL);
        mBtnOpen.setOnClickListener(mBtnOCL);
        mBtnSave.setOnClickListener(mBtnOCL);
    }

    private View.OnClickListener mBtnOCL = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getId()==mBtnFile.getId()){
                //showDialog(OPEN_FILE_DIALOG_ID);
                createFilePopupWindow();
            }else if(v.getId() == mBtnOpen.getId()){
                showDialog(OPEN_FILE_DIALOG_ID);
            }else if(v.getId() == mBtnSave.getId()){
                //saveFile();
                //
                //batchConvertFiles(OPEN_PATH);
                mProgressDialog = new ProgressDialog(MainActivity.this);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        batchConvertFiles(OPEN_PATH);
                    }
                }).start();

            }
            /*
            if( !(v instanceof EditText)){
                InputMethodManager manager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
            }*/
        }
    };

    private void batchConvertFiles(String path){
        //List<File> list = null;
        StringBuilder stringBuilder = new StringBuilder();

            File[] files = null;
            try{
                files = new File(path).listFiles();
            }
            catch(Exception e){
                files = null;
            }
            if(files==null){
                // 璁块棶鍑洪敊
                Toast.makeText(this, "No rights to access!", Toast.LENGTH_SHORT).show();
                return;
            }
        /*
            if(list != null){
                list.clear();
            }
            else{
                list = new ArrayList<File>(files.length);
            }
            */
        int totalCnt = 0;
        int successCnt = 0;

            for(File file: files){
                Message msg = new Message();
                msg.what = MSG_PROGRESS_DIALOG;
                msg.arg1 = files.length;
                msg.arg2 = totalCnt;
                mHandler.sendMessage(msg);
                if(file.isFile() && file.getName().toLowerCase().contains("txt")){
                    totalCnt++;
                    //mTvSource.setText(Integer.toString(totalCnt) + "/" + Integer.toString(files.length));
                    String newFileName = file.getName().substring(0,file.getName().indexOf('.'));
                    String suffix = file.getName().substring(file.getName().indexOf('.'),file.getName().length());
                    String newPath = SAVE_PATH+"_"+newFileName+suffix;
                    if(convertFile(file,newPath))
                        successCnt++;
                        stringBuilder.append(newPath).append("\n");
                    }
            }
        stringBuilder.append(Integer.toString(successCnt) + "/" + Integer.toString(totalCnt) + " converted.\n");
        Message msg = new Message();
        msg.what = MSG_PROGRESS_DIALOG_DISSMISS;
        Bundle data = new Bundle();
        data.putString(EXTRA_STRING,stringBuilder.toString());
        msg.setData(data);
        mHandler.sendMessage(msg);

        //mTvSource.setText(stringBuilder.toString());

    }
    private boolean convertFile(File file, String path){

        try {
            FileOutputStream out = new FileOutputStream(new File(path));
            byte[] data = convertTextFile(file).toString().getBytes("UTF-8");
            if(data.length>0){
                //Toast.makeText(getApplicationContext(),"Saved to "+path,Toast.LENGTH_LONG).show();
                out.write(data);
                Log.v(TAG,"Saved to "+path);
            }else{
                Log.v(TAG,"Failed save to "+path);
            }
            out.close();
        } catch (IOException e) {
            Log.v(TAG,"Failed save to "+path);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void saveFile(String path){
        try {
            FileOutputStream out = new FileOutputStream(new File(path));
            byte[] data = mTvTarget.getText().toString().getBytes("UTF-8");
            if(data.length>0){
                //Toast.makeText(getApplicationContext(),"Saved to "+path,Toast.LENGTH_LONG).show();
                out.write(data);
                Log.v(TAG,"Saved to "+path);
            }else{
                Log.v(TAG,"Failed save to "+path);
            }
            out.close();
        } catch (IOException e) {
            Log.v(TAG,"Failed save to "+path);
            e.printStackTrace();
        }
    }

    private void saveFile(){
        String newFileName = currentFileName.substring(0,currentFileName.indexOf('.'));
        String suffix = currentFileName.substring(currentFileName.indexOf('.'),currentFileName.length());
        String path = SAVE_PATH+"_"+newFileName+suffix;
        try {
            FileOutputStream out = new FileOutputStream(new File(path));
            byte[] data = mTvTarget.getText().toString().getBytes("UTF-8");
            if(data.length>0){
                Toast.makeText(getApplicationContext(),"Saved to "+path,Toast.LENGTH_LONG).show();
                out.write(data);
            }else{
                Toast.makeText(getApplicationContext(),"Save failed",Toast.LENGTH_LONG).show();
            }
            out.close();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),"Save failed",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void createFilePopupWindow(){
        PopupMenu popupMenu = new PopupMenu(getApplicationContext(),mBtnFile);
        popupMenu.inflate(R.menu.file_popupmenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.file_menu_open:
                        showDialog(OPEN_FILE_DIALOG_ID);
                        break;
                    case R.id.file_menu_save:
                        String str = mTvTarget.getText().toString();
                        if(mTvTarget.getText().toString() != null || mTvTarget.getText().toString().length() > 0){
                            showDialog(SAVE_FILE_DIALOG_ID);
                        }else{
                            Toast.makeText(getApplicationContext(),"Content is empty!",Toast.LENGTH_LONG).show();
                        }
                        break;
                    case R.id.file_menu_exit:
                        finish();
                        break;
                }
                return true;
            }
        });
        popupMenu.show();

    }

    private String resortTable(String[] table){
        String result="";
        for(int i=0;i<table.length;i++){
            if(i%16==0)result += "\n";
            result += "_"+table[i] + "_,";
        }

        return result;
    }

    private SpannableStringBuilder highLightWord(String str){
        SpannableStringBuilder builder = new SpannableStringBuilder(str);

        CharacterStyle span = null;
        Pattern p = Pattern.compile(FILTER_PATTERN);
        Matcher m = p.matcher(str);
        while (m.find()){
            span = new ForegroundColorSpan(Color.RED);
            builder.setSpan(span,m.start(),m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }
    private String convertTextFile(String path){
        File file = new File(path);
        return convertTextFile(file);
    }

    private String convertTextFile(File file){
        //File file = new File(path);
        StringBuilder stringbuilder = new StringBuilder();
        //stringbuilder.append(FIRST_COMMENT);
        //stringbuilder.append(FIRST_8Bytes);
        String string = null;
        int semicolonCnt  = 0;
        int partCnt = 0;
        int lineCnt = 0;
        int subLineCnt = 0;
        int lineOfSet = 0;
        BufferedReader reader;
        boolean isHelpExist = false;
        boolean isSubLine = false;

        Pattern pattern = Pattern.compile(FILTER_PATTERN);
        try{
            reader = new BufferedReader(new FileReader(file));
            while((string=reader.readLine()) != null){
                if(!string.contains(";") && (string.indexOf(';')<5)){
                    semicolonCnt = 0;
                    int wordCnt = 0;
                    StringBuilder b = new StringBuilder();
                    Matcher m = pattern.matcher(string);
                    String str = "";
                    int subLineIndex = string.indexOf(' ');
                    isSubLine = (subLineIndex<5 && subLineIndex>-1);
                    if(isSubLine){
                        subLineCnt ++;
                        if(subLineCnt == 2)isHelpExist=true;
                    }else{
                        subLineCnt=0;
                    }
                    while (m.find()){
                        str = string.substring(m.start(),m.end());
                        b.append(str);
                        wordCnt++;
                        if(wordCnt<4)b.append(USER_DIVIDER);
                    }
                    for(int i=wordCnt;i<4;i++){
                        b.append(USER_END_WORD);
                        if(i<3)b.append(USER_DIVIDER);
                    }
                    string = b.toString();

                    lineCnt++;

                    if(isSubLine){
                        switch(subLineCnt){
                            case 1:
                                stringbuilder.append("DW\t\t"+string+SUBLINE1_COMMENT+"\n");
                                break;
                            case 2:
                                stringbuilder.append("DW\t\t"+string+SUBLINE2_COMMENT+"\n");
                                break;
                            default:
                                stringbuilder.append("DW\t\t"+string+SUBLINE3_COMMENT+"\n");
                        }

                    }
                    else stringbuilder.append("DW\t"+string+"\n");
                }else{
                    if(semicolonCnt == 0) {
                        stringbuilder.append(";Part\t" + partCnt+"\n");
                        if(partCnt==1){
                            lineOfSet = subLineCnt + 1;
                            firstlineData[5] = lineCnt;
                        }
                        partCnt++;
                        lineCnt = 0;
                    }
                    semicolonCnt++;
                }
            }
            firstlineData[6] = lineCnt;
            reader.close();

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }

        if(isHelpExist)firstlineData[4]=1;

        String result = "";

        //////////////////////////////////////////////
        //Header
        //////////////////////////////////////////////
        /*
        result += ";" + file.getName() + "\n";
        for(int i=0;i<8;i++){
            result += ";" + FIRST_COMMENTS[i] +"\n";
            result += "DW\t" + Integer.toString(firstlineData[i]) + "\n";
        }*/
        //////////////////////////////////////////////
        result +=stringbuilder.toString();
        result += "\n//line of set is: "+lineOfSet;

        return result;
    }
    /*
    private String convertTextFile(File file){
        //File file = new File(path);
        StringBuilder stringbuilder = new StringBuilder();
        //stringbuilder.append(FIRST_COMMENT);
        //stringbuilder.append(FIRST_8Bytes);
        String string = null;
        int semicolonCnt  = 0;
        int partCnt = 0;
        int lineCnt = 0;
        int subLineCnt = 0;
        BufferedReader reader;
        boolean isHelpExist = false;
        boolean isSubLine = false;

        Pattern pattern = Pattern.compile(FILTER_PATTERN);
        try{
            reader = new BufferedReader(new FileReader(file));
            while((string=reader.readLine()) != null){
                //System.out.println(str);

                //if(!string.contains(";")){
                if(!string.contains(";") && (string.indexOf(';')<5)){
                    semicolonCnt = 0;
                    int wordCnt = 0;
                    StringBuilder b = new StringBuilder();
                    Matcher m = pattern.matcher(string);
                    String str = "";
                    int subLineIndex = string.indexOf(' ');
                    isSubLine = (subLineIndex<5 && subLineIndex>-1);
                    if(isSubLine){
                        subLineCnt ++;
                        if(subLineCnt == 2)isHelpExist=true;
                    }else{
                        subLineCnt=0;
                    }
                    while (m.find()){
                        str = string.substring(m.start(),m.end());
                        b.append(str);
                        wordCnt++;
                        if(wordCnt<4)b.append(USER_DIVIDER);
                    }
                    for(int i=wordCnt;i<4;i++){
                        b.append(USER_END_WORD);
                        if(i<3)b.append(USER_DIVIDER);
                    }
                    string = b.toString();

                    lineCnt++;

                    if(isSubLine){
                        switch(subLineCnt){
                            case 1:
                                stringbuilder.append("DW\t\t"+string+SUBLINE1_COMMENT+"\n");
                                break;
                            case 2:
                                stringbuilder.append("DW\t\t"+string+SUBLINE2_COMMENT+"\n");
                                break;
                            default:
                                stringbuilder.append("DW\t\t"+string+SUBLINE3_COMMENT+"\n");
                        }

                    }
                    else stringbuilder.append("DW\t"+string+"\n");
                }else{
                    if(semicolonCnt == 0) {
                        stringbuilder.append(";Part\t" + partCnt+"\n");
                        if(partCnt==1)firstlineData[5] = lineCnt;
                        partCnt++;
                        lineCnt = 0;
                    }
                    semicolonCnt++;
                }
            }
            firstlineData[6] = lineCnt;
            reader.close();

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }

        if(isHelpExist)firstlineData[4]=1;

        String result = ";" + file.getName() + "\n";
        for(int i=0;i<8;i++){
            result += ";" + FIRST_COMMENTS[i] +"\n";
            result += "DW\t" + Integer.toString(firstlineData[i]) + "\n";
        }
        result +=stringbuilder.toString();
        return result;
    }*/

    private String readTxtFile(String path){
        File file = new File(path);
        String  str = null;
        try {
            FileInputStream inputStream = new FileInputStream(file);
            int length = inputStream.available();
            byte[] buffer = new byte[length];
            while(inputStream.read(buffer) !=-1);
            inputStream.close();
            str = new String(buffer,"UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return str;
    }
    @Override
    protected Dialog onCreateDialog(int id) {
        if(id == OPEN_FILE_DIALOG_ID){
            Map<String,Integer> images = new HashMap<String, Integer>();
            images.put(OpenFileDialog.sParent,R.drawable.filedialog_folder_up);
            images.put(OpenFileDialog.sRoot,R.drawable.filedialog_root);
            images.put(OpenFileDialog.sFolder,R.drawable.filedialog_folder);
            images.put("wav",R.drawable.filedialog_wavfile);
            images.put(OpenFileDialog.sEmpty,R.drawable.filedialog_file);
            //images.put("bin",R.drawable.filedialog_file);
            Dialog dialog = OpenFileDialog.createDialog(id,this,"Open File",new CallbackBundle(){

                        @Override
                        public void callback(Bundle bundle) {
                            String filepath = bundle.getString(OpenFileDialog.EXTRA_STRING_PATH);
                            currentFileName = bundle.getString(OpenFileDialog.EXTRA_STRING_NAME);
                            Log.v(TAG,"path= "+ filepath);
                            Log.v(TAG,"currentFileName= "+ currentFileName);
                            //currentPath = filepath.substring(0,filepath.lastIndexOf("/")+1);
                            String txt = readTxtFile(filepath);

                            if(txt != null){
                                mTvSource.setText(txt);
                                mTvTarget.setText(highLightWord(convertTextFile(filepath)));
                            }
                        }
                    },"",images,OPEN_PATH
            );
            return dialog;
        }else if(id == SAVE_FILE_DIALOG_ID){
            Map<String,Integer> images = new HashMap<String, Integer>();
            images.put(SaveFileDialog.sParent,R.drawable.filedialog_folder_up);
            images.put(SaveFileDialog.sRoot,R.drawable.filedialog_root);
            images.put(SaveFileDialog.sFolder,R.drawable.filedialog_folder);
            images.put("wav",R.drawable.filedialog_wavfile);
            images.put(SaveFileDialog.sEmpty,R.drawable.filedialog_file);
            //images.put("bin",R.drawable.filedialog_file);
            String newFileName = currentFileName.substring(0,currentFileName.indexOf('.'));
            String suffix = currentFileName.substring(currentFileName.indexOf('.'),currentFileName.length());
            Dialog dialog = SaveFileDialog.createDialog(id,this,"Save File",new CallbackBundle(){
                        @Override
                        public void callback(Bundle bundle) {
                            String path = bundle.getString(SaveFileDialog.EXTRA_STRING_PATH);
                            try {
                                FileOutputStream out = new FileOutputStream(new File(path));
                                byte[] data = mTvTarget.getText().toString().getBytes("UTF-8");
                                if(data.length>0){
                                    Toast.makeText(getApplicationContext(),"Saved",Toast.LENGTH_LONG).show();
                                    out.write(data);
                                }else{
                                    Toast.makeText(getApplicationContext(),"Save failed",Toast.LENGTH_LONG).show();
                                }
                                out.close();
                            } catch (IOException e) {
                                Toast.makeText(getApplicationContext(),"Save failed",Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }
                    },"",images,SAVE_PATH
            );
            return dialog;

        }
        return null;
    }
}
